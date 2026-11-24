# AGENTS.md

本文件是本仓库的**开发与发版规范**，面向在本仓库中工作的 AI agent 与贡献者。
与代码注释冲突时，以本文件为准；本文件未覆盖的部分按现有代码惯例执行。

---

## 1. 项目概览

TiebaLite 是一个第三方贴吧 Android 客户端。

| 项 | 值 |
|---|---|
| 语言 / UI | Kotlin + Jetpack Compose；**Material 3 为目标形态**（见第 5 节），存量仍有 Material 2 |
| 构建 | Gradle 9.9.2 wrapper + AGP 9.5.2 + Kotlin 2.4.21 |
| compileSdk / targetSdk / minSdk | 37.1 / 36 / 24 |
| applicationId | `com.huanchengfly.tieba.post` |
| 依赖管理 | Gradle version catalog（`gradle/libs.versions.toml`），依赖用 `libs.xxx` 引用 |
| DI | Hilt + KSP |
| 架构 | MVVM：`arch/BaseViewModel` + `UiState/UiIntent/UiEvent`，页面级 `pageViewModel()` |
| 全局事件 | `arch/GlobalEvent.kt` 的 `GlobalEventFlow` + `onGlobalEvent<T>`（**不要在页面里直接 collect**） |
| 版本号 | `application.properties` 的 `versionCode` / `versionName`（发版时手改） |

主要目录：

```
app/src/main/java/com/huanchengfly/tieba/post/
├── arch/            # Activity/ViewModel 基类、全局事件、Compose 辅助
├── ui/common/       # 主题、prefs 框架、windowsizeclass（窗口尺寸类）
├── ui/page/         # 各页面（main / forum / thread / settings …）
├── ui/widgets/compose/  # 自绘 Compose 组件（Scaffold/Toolbar/Lazy 列表/弹窗…）
└── utils/           # 账号、主题、图片、DataStore 封装（AppPreferencesUtils）
```

仓库内与本规范配套的文档：

| 文档 | 用途 | 维护时机 |
|---|---|---|
| `AGENTS.md` | 开发与发版规范（本文件） | 规范变更时 |
| `debuglog.md` | **调试记录**：每个已修 bug 的现象、复现、定位过程、根因、修复与验证证据 | 修完一个 bug 追加一条 |
| `docs/plan-m3-migration.md` | Material 3 迁移执行台账（阶段 / 门禁 / 进度） | 每阶段改完 |
| `AI_CHANGELOG.md` | 面向用户的发版说明（CI 按 tag 抽取） | 发版时 |

---

## 2. 本地构建与验证

构建工具链不依赖 Android Studio，可直接用命令行 Gradle：

```powershell
$env:JAVA_HOME="F:\Android\jdk"
$env:GRADLE_USER_HOME="F:\Android\gradle-home"
$env:ANDROID_HOME="F:\Android\sdk"
$env:ANDROID_SDK_ROOT="F:\Android\sdk"

# 快速编译校验（改完 Kotlin 必跑，约 1-4 分钟）
& F:\Android\gradle-dist\gradle-9.9.2\bin\gradle.bat --no-daemon :app:compileDebugKotlin --console=plain

# 发版前：签名校验（需要口令，见第 7 节）
& F:\Android\gradle-dist\gradle-9.9.2\bin\gradle.bat --no-daemon :app:checkReleaseSigning --console=plain

# 完整 release 构建
& F:\Android\gradle-dist\gradle-9.9.2\bin\gradle.bat --no-daemon :app:assembleRelease --console=plain
```

- `local.properties` 需含 `sdk.dir`（已被 `.gitignore` 忽略，不要提交）。
- 本机没有 `gradle` 命令、`keytool` 不在 PATH 时，用上面的绝对路径。
- **验证以实际命令输出为准**：`BUILD SUCCESSFUL` 才算通过；不要用「看起来对」代替编译。
- **Windows 盘符约束（Glide 5 + KSP2）**：`GRADLE_USER_HOME` 必须与工程在同一个盘符，否则
  `:app:kspDebugKotlin` 直接失败：`IllegalArgumentException: this and base files have different roots`
  （Glide 的 KSP 处理器读依赖 jar 内的 `GlideIndexer_*` 时计算相对路径；KSP1 在 KSP 2.3.14 已移除，
  没有退回 KSP1 的开关）。Android Studio 默认用 `C:\Users\<user>\.gradle`，需在
  Settings → Build, Execution, Deployment → Build Tools → Gradle → **Gradle user home** 改成
  `F:\Android\gradle-home`（与上面的命令行一致）；或设置用户环境变量 `GRADLE_USER_HOME` 后重启 IDE。
- **Compose 编译器指标默认关闭**：AGP 9 传给 Compose 插件的 `moduleName` 带冒号（`TiebaLite:app_debug`），
  `metricsDestination` / `reportsDestination` 会把它拼进文件名；Windows 把冒号当 NTFS 数据流分隔符——
  轻则指标被写进 0 字节 `TiebaLite` 的 ADS（等于没产出），重则 `IOException: Invalid file path`
  让整个 Kotlin 编译器崩掉。所以默认不设置这两项；需要时 `-PenableComposeMetrics=true`
  （Windows 上打开仍会踩冒号问题，属 AGP / Compose 插件侧 bug，不在本仓库修）。

### 2.1 调试热重载（Live Edit）

**Live Edit 是 IDE 级（application）设置，不随仓库下发。** 它由 Android Studio 的
`LiveEditApplicationConfiguration` 持有：`LiveEditApplicationConfiguration.getInstance()` 走
`ApplicationManager.getApplication().getService(...)`，state 名 `LiveEditConfiguration`、storage
`other.xml`，落盘在 `%APPDATA%\Google\AndroidStudio<版本>\options\other.xml`。工程内的
`.idea/other.xml` 是 **project 级**文件，这个组件不会读它——写进去不生效（本仓库曾那样做，现已移除）。

2026.x 里该开关默认就开着（`compose.deploy.live.edit.deploy.enable.default=COMPLETE`），所以
`options/other.xml` 里通常看不到 `mode`——等于默认值时不落盘。查看/修改位置：

    Settings → Editor → Compose Live Edit（中文界面：设置 → 编辑器 → Compose Live Edit）
    「Live Edit」= 启用，「None」= 关闭；触发方式选「Push Edits Automatically」

- 部署：用 Android Studio 的 Run / Debug 部署 `debug` 变体。**不要用 `adb install` 验证热重载**——
  Live Edit 走的是 IDE 与运行中进程的直连通道，手动装包只会看到旧字节码。
- 前提：`debugImplementation(libs.compose.ui.tooling)`（已在 `app/build.gradle.kts`）、设备 API 30+；
  `debug` 变体不混淆，可以直接热替换。
- 用法：改 `@Composable` 代码并保存，改动会自动推送到运行中的进程；改方法签名、改非 Compose 逻辑等
  场景 IDE 会提示需要重新部署。
- 它不是验收手段：Live Edit 只加速本地看效果，交付前仍须跑 `:app:compileDebugKotlin`。
- 改回手动触发：同一设置页把触发方式换成「Push Edits Manually on Save」（`ON_SAVE`）或
  「Push Edits Manually」（`ON_HOTKEY`，动作 `Compose.Live.Edit.Refresh`）。

---

## 3. 代码规范

### 3.1 通用

- 只改与任务直接相关的文件与行为；不做顺手重构、格式化、依赖升级。
- 保持文件编码（UTF-8）与既有换行风格（CRLF）不变。
- 注释解释**意图、约束、取舍**；不复述代码。中文注释，术语保留英文原文。
- 新增字符串走 `app/src/main/res/values/strings.xml`，命名沿用 `title_` / `summary_` / `button_` / `toast_` / `desc_` 前缀。
- 常量优先 `private val` 放在使用处附近；跨模块复用的放 `ui/common/` 下对应工具文件。

### 3.2 Compose

- 组件优先复用 `ui/widgets/compose/` 里已有的封装（`MyScaffold`、`Toolbar`、`TitleCentredToolbar`、`MyLazyColumn`、`MyLazyVerticalGrid`、`Container`），不要直接搭一套新的。
- 页面顶级用 `MyScaffold`（它提供 `LocalSnackbarHostState` 与栏隐藏重置），只有需要 Material 原生 `Scaffold` 特殊参数时才例外。
- 列表统一走 `MyLazyColumn` / `MyLazyVerticalGrid`：它们已内置「滚到顶部」全局事件响应与宽屏居中收窄。
- 弹窗统一用 `ui/widgets/compose/Dialogs.kt` 的 `Dialog` / `ConfirmDialog` / `AlertDialog` / `PromptDialog`，不要直接用 `androidx.compose.ui.window.Dialog`。
- 状态读取用 `collectPartialAsState`，避免整页重组。
- `@Composable` 函数保持无副作用；副作用放 `LaunchedEffect` / `DisposableEffect`。
- **Material 版本：新组件用 M3**（`androidx.compose.material3`）；M2 只修不加，且不要在同一组合函数里混用两套。详见第 5 节。

### 3.3 状态与事件

- 新增全局事件加到 `GlobalEvent`；页面用 `onGlobalEvent<T> { }` 消费（该实现已保证只有一个 collector，不要改回 `DisposableEffect`）。
- 持久化设置项加到 `AppPreferencesUtils`（`DataStoreDelegates`），键名用 snake_case 字符串常量；页面侧用 `rememberPreferenceAsState` 或 `SwitchPref(key = "...")` 读取，**键名必须与属性名一致**（`DataStoreDelegates` 默认用属性名做 key）。

---

## 4. 界面自适应（响应式）规范

窗口尺寸类的唯一来源是 `arch/BaseComposeActivity.LocalWindowSizeClass`（由 `calculateWindowSizeClass` 提供，随窗口尺寸变化重组）。断点：

| 类别 | 宽度 | 高度 |
|---|---|---|
| Compact | < 600dp | < 480dp |
| Medium | 600–840dp | 480–900dp |
| Expanded | >= 840dp | >= 900dp |

规则：

1. **不要自己写 `widthSizeClass` 分支的魔法数字**。需要「宽屏限制内容宽度」时用 `ui/common/windowsizeclass/AdaptiveLayout.kt`：
   - `AdaptiveLayout.ContentMaxWidth`（840dp）：单列正文上限；
   - `AdaptiveLayout.DialogMaxWidth`（560dp）：居中对话框上限；
   - `WindowSizeClass.contentMaxWidth` / `isCompactWidth` / `isMediumWidth` / `isExpandedWidth` / `isCompactHeight`。
2. **不要用窗口百分比当宽度上限**（`fillMaxWidth(0.75f)`）。窗口越宽内容越宽，平板上行宽会失控。用绝对上限 + 居中。
3. 单列正文用 `Container { }` 包裹即可获得「宽屏居中 + 收窄」；`fluid = true` 表示明确不要限制。
4. 大屏导航形态由 `MainPage` 统一决定：Compact → 底部导航，Medium → NavigationRail，Expanded → 常驻抽屉（书本姿态折叠屏回落为 Rail）。不要在各页面重复判断。
5. 横向分栏（列表 + 详情）属于架构级改动，需要单独评审；不要在局部页面临时拼。
6. **全面屏 / 安全区**：顶栏的 inset 取 `WindowInsets.safeDrawing.only(Top)`（statusBars ∪ displayCutout），底栏取 `bottomBarInsetsHeight()`（navigationBars ∪ safeGestures ∪ displayCutout）；页面根部已统一做横向 `safeDrawing` 避让。不要退回只用 `statusBars`，刘海/挖孔会压住内容。

---

## 5. UI 大方向：Material 3 + 悬浮栏

这是本仓库 UI 的**长期主线**。新代码按此执行，存量页面按此逐步迁移；与其他章节冲突时，以本节的「方向」为准则、以其他章节的「硬性约束」为红线。

### 5.1 Material 3 是目标形态

- 目标形态是 Jetpack Compose **Material 3**（`androidx.compose.material3`）。Material 2（`androidx.compose.material`）进入**只修不加**状态：存量组件继续可用，但不新增 M2 组件、不再给 M2 组件加功能。
- **新写 / 重写的 UI 一律用 M3。** 改到某个页面时，把该页面已用到的 M2 组件换成 M3 等价物算本次改动范围（`Scaffold` / `TopAppBar` / `NavigationBar` / `Surface` / `Card` / `Text` / `Icon` / `Button` / `TextField` 等）。
- **禁止在同一个组合函数里混用 M2 与 M3 的同名组件**：两套 `MaterialTheme`、两套 `Scaffold` 语义会让测量与配色出现两个来源。需要跨边界取值时走 5.2 的桥接。
- 配色唯一来源仍是 `ui/common/theme/compose` 的 `ExtendedTheme` / `ExtendedColors`（来自贴吧主题 attr 与调色板）。M3 侧读 `MaterialTheme.colorScheme` 时，由 `TiebaLiteTheme` 把 ExtendedColors 映射成 `ColorScheme`；**不要**在页面里另造一套颜色常量。
- 迁移顺序按页面热度推进：主框架（`MyScaffold` / 栏 / 导航）→ 主界面 → 帖子页与吧页 → 设置类页面。每一步都必须保持可编译、可运行，不留半迁移的死代码。
- 执行台账见 `docs/plan-m3-migration.md`：阶段划分、每阶段的改动范围与验收门禁、当前进度都在那里。动手前先看它，改完同步更新。

### 5.2 迁移期的桥接约定

- 主题：`TiebaLiteTheme` 同时提供 M3 的 `colorScheme/typography/shapes` 与既有的 `ExtendedTheme`，两者读同一个配色来源；未迁移的 M2 组件继续读 `ExtendedTheme`。
- 尺寸：间距、圆角、触控目标优先取 M3 的 `MaterialTheme.shapes` 与 4dp 栅格；不要引入新的散落魔法数字。
- 组件选择：能用 M3 预设就不自绘（`NavigationBar` / `NavigationBarItem` / `Surface` / `TopAppBar` 等）；自绘只保留项目特有视觉（例如胶囊悬浮栏的定位与隐藏行为）。

### 5.3 栏（顶栏 / 底栏）一律是悬浮 overlay

栏的**布局语义**固定为「浮在内容之上」，不是「占掉一块布局空间」：

1. 页面内容铺满整屏，栏以 overlay 叠在上面；内容通过 `PaddingValues`（列表用 `contentPadding`）避让栏的高度。
2. 栏隐藏时只做**绘制位移**、不改布局高度：**顶栏整条（状态栏底色带 + 标题行 + 次级槽页签 / 搜索框）作为一整块滑出屏幕**，收起后顶栏区域没有任何残留；底栏（含胶囊、外边距、系统手势区）同样整块滑出。**不允许**留下空白条、也不允许留半截被系统栏压住的栏。**次级槽不要求常驻**：收起后「推荐 / 热榜」等页签不必停在状态栏下沿——整条一起滑出是本仓库的既定行为（2026-11 用户确认，勿按「页签常驻」的旧验收标准改回去）。
3. 顶栏不需要单独的遮罩 / scrim：底色画在被位移的节点之内，随栏一起滑走，收起后状态栏区域直接显示滚动内容。
4. **底栏统一胶囊式悬浮**：圆角、浮在内容之上，与屏幕左右留 16dp、与底部只留手势区 + 6dp 的间距，不使用「贴边整条栏」的形态。M3 下用 `Surface`（`shape` + `shadowElevation`/`tonalElevation`）承载；主界面、帖子页、子楼页、屏蔽词页等所有底栏共用 `ui/widgets/compose/FloatingBars.kt` 的 `FloatingBottomBar`，保持同一视觉族。
5. 栏的隐藏实现仍须满足第 6 节的硬性约束（不消费滚动、独立动画、只改位置不改尺寸、状态 Activity 级共享）。

---

## 6. 顶栏 / 底栏自动隐藏规范

实现在 `ui/widgets/compose/BarsAutoHide.kt`（`BarsAutoHideState` + `LocalBarsAutoHideState`）与 `ui/widgets/compose/FloatingBars.kt`（`FloatingBottomBar`）；顶栏的位移在 `Toolbar.kt` 的 `TopAppBarContainer` 内实现（不再提供 `Modifier.barsAutoHide`）。

**硬性约束（改动前务必读完）**

1. **不消费滚动**：`nestedScrollConnection` 的 pre/post 回调一律返回 `Offset.Zero`，只「观察」已消费的位移方向。**禁止**把用户的拖拽增量拿去做栏动画——那会给滚动叠加加速度，手感直接变差。这是本功能的设计红线。
2. **独立动画**：方向累计超过阈值后，用固定时长 tween 播完收起/展开，与手指位移解耦；松手后仍会滑到底。
3. **只改位置、不改尺寸**：栏报告给父级的布局高度必须恒等于栏的完整高度，只把栏平移出去。高度一旦跟着动画变化，`MyScaffold` 算出的内容内边距每帧都变，列表被反复重新测量——用户感受到的就是「页面突然长高」，滚动会跟着抖。
4. **栏的背景必须跟着一起位移**：背景要画在被位移的节点之内，否则栏滑走后背景仍盖着内容，看起来像「收起后留了一条空白」。
5. **状态在 Activity 级共享**：`LocalBarsAutoHideState` 由 `BaseComposeActivity` 提供，`MyScaffold` 在每个新页面进入时调用 `show()` 复位。
6. **底栏的手势区属于被隐藏的整块**：`bottomBarInsetsHeight()` 占位要放在同一位移容器内一起滑走。留在外面的话，收起后会残留一条底色带（正是「没完全收起」的观感来源）；滑走之后该区域的触摸交给页面内容。
7. 新增底栏时**直接复用胶囊容器**，不要另写位移逻辑：

```kotlin
FloatingBottomBar(color = floatingBarColor(ExtendedTheme.colors.bottomBar)) {
    /* 胶囊内容：图标按钮 / 输入框 */
}
```

`FloatingBottomBar` 内部已实现「实测整块高度 → `graphicsLayer` 位移 → 手势区跟随滑走」，栏的布局高度恒定（内容内边距据此计算），只有绘制位移随动画变化。

8. 用户可在「自定义设置 → 顶栏 / 底栏跟随滑动隐藏」关闭该行为（`AppPreferencesUtils.hideBarsOnScroll`）。关闭后 `onScroll` 直接返回，不做任何动画。

9. 页面级常驻：页头本身承担导航标识时（个人主页的头像 / ID / 返回 / 黑名单），进入页面用 `LocalBarsAutoHideState.current?.pin()`、离开时 `unpin()`（`DisposableEffect` 成对调用）。**不要**让这类页面的栏跟随滚动收起——正文仍按栏高留了 padding，栏滑走后状态栏下方会留下一条「有高度、没内容」的空白带。计数器语义，多个页面可同时持有。

---

## 7. 发版规范

### 7.1 版本号与变更记录

发版提交必须同时改这三处，缺一不可：

1. `application.properties`：`versionCode`（+1）、`versionName`（如 `4.0.64`）；预览版改 `isPreRelease=true` + `preReleaseName` + `preReleaseVer`。
2. `AI_CHANGELOG.md`：在**顶部**新增 `## vX.Y.Z` 段，结构为

   ```markdown
   ## v4.0.64

   ### 🇨🇳 中文

   <一句话概述>

   - 修复：…
   - 新增：…
   ```

   CI 会按 tag 精确匹配该标题抽取发布说明，标题格式错误会导致 Release 说明为空。
3. 各语言 README（`README.md` / `README.zh-CN.md` / `README.ja.md` / `README.ko.md`）的更新日志段落。

提交信息用 Conventional Commits，例如 `feat(ui): …` / `fix(thread): …` / `chore: version bump to v4.0.64 …`。

### 7.2 签名（固定发布签名）

- 发布包**只能**用仓库内 `app/tblite_debug.jks`（PKCS12，alias `tiebalite`）签名。它是历史所有发布包的签名，换 key 会导致老用户无法覆盖升级。
- 证书 SHA-256 指纹是发布契约：

  ```
  F7E02F10C7C9E83B943F8DEEE786B270330416C710CF5CFBCE73027AD19DA759
  ```

  期望值写在 `app/build.gradle.kts` 的 `pinnedReleaseCertSha256`，由 `:app:checkReleaseSigning` 在构建期校验；CI 还会用 `apksigner` 复核产出的 APK。**指纹不符 = 构建失败**，不要绕过。
- 口令来源（按优先级）：`keystore.properties` → 环境变量 `TBLITE_KEYSTORE_PASSWORD` / `TBLITE_KEY_PASSWORD`。
- `debug` build type 仍用 Android 默认 debug 签名，与发布包互不干扰；**禁止**再把 release 回落成 debug 签名（历史上正是这样造成了同版本号不同签名）。
- `app/tblite_debug.jks` 与其口令本来就是公开的：它只保证包可持续升级，不是防伪手段，无需额外加密或转移。

### 7.3 发布流程

发布由 `.github/workflows/build.yml` 完成，两种入口：

**A. 手动触发（推荐日常使用）**

```
Actions → "Build & Release APK" → Run workflow
  tag:        留空 = 用 v{versionName}；或填 v4.0.64
  publish:    勾选则构建成功后自动建/更新 GitHub Release
  prerelease: 勾选则标记为预发布
```

**B. 推送 tag 自动发布**

```bash
git tag v4.0.64 && git push origin v4.0.64
```

工作流行为：

- 校验签名证书（Gradle 任务）→ 构建 release APK → `apksigner` 复核 APK 签名 → 上传 artifact → 发布 Release（说明取自 `AI_CHANGELOG.md` 对应段落）。
- Release 已存在时**更新并覆盖资产**，不会先删后建（避免半程状态）。
- tag 含 `-ai` 时自动标记 prerelease。
- 同一 ref 的并发运行会自动取消旧的一次。

发版后自检：

1. Release 页有 APK，且说明非空、版本号正确。
2. 下载的 APK 能被 `apksigner verify --print-certs` 验过且指纹为上文契约值。
3. 覆盖安装旧版本成功（签名一致）。

### 7.4 禁止事项

- 不要手动上传 APK 到 Release（会被下一次工作流覆盖，且可能签错）。
- 不要修改 `pinnedReleaseCertSha256` 却不通知用户——那意味着全体用户必须卸载重装。
- 不要把 `keystore.properties`、`local.properties`、任何口令提交进仓库。

---

## 8. 交付前检查清单

- [ ] `:app:compileDebugKotlin` 通过
- [ ] 发版相关改动：`:app:checkReleaseSigning` 通过
- [ ] 新增字符串已进 `strings.xml`，无硬编码文案
- [ ] 新增设置项已在 `AppPreferencesUtils` 声明，键名与 UI `key` 一致
- [ ] 响应式改动走 `AdaptiveLayout`，未引入新的百分比宽度魔法数字
- [ ] 栏隐藏改动未消费滚动增量，手势热区仍在隐藏范围之外
- [ ] UI 改动：新组件用 M3，且符合第 5 节的迁移方向（未新增 M2 组件、未混用两套 MaterialTheme）
- [ ] 栏改动：顶栏/底栏是 overlay，隐藏时整块滑出、不留空白条；底栏为胶囊式悬浮
- [ ] `git status` 中无意外文件（`local.properties`、构建产物、`.kotlin/sessions/` 等）
- [ ] 修复的 bug 已在 `debuglog.md` 追加一条（现象 / 根因 / 修复 / 验证证据），状态与提交对应
