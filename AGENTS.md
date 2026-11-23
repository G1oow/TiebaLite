# AGENTS.md

本文件是本仓库的**开发与发版规范**，面向在本仓库中工作的 AI agent 与贡献者。
与代码注释冲突时，以本文件为准；本文件未覆盖的部分按现有代码惯例执行。

---

## 1. 项目概览

TiebaLite 是一个第三方贴吧 Android 客户端。

| 项 | 值 |
|---|---|
| 语言 / UI | Kotlin + Jetpack Compose（Material 2，`androidx.compose.material`） |
| 构建 | Gradle 8.9 wrapper + AGP 8.6.1 + Kotlin 2.0.21 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 21 |
| applicationId | `com.huanchengfly.tieba.post` |
| 依赖管理 | SweetDependency（`gradle/sweet-dependency/`），`libs.xxx` 由插件生成 |
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

---

## 2. 本地构建与验证

构建工具链不依赖 Android Studio，可直接用命令行 Gradle：

```powershell
$env:JAVA_HOME="F:\Android\jdk"
$env:GRADLE_USER_HOME="F:\Android\gradle-home"
$env:ANDROID_HOME="F:\Android\sdk"
$env:ANDROID_SDK_ROOT="F:\Android\sdk"

# 快速编译校验（改完 Kotlin 必跑，约 1-4 分钟）
& F:\Android\gradle-dist\gradle-8.13\bin\gradle.bat --no-daemon :app:compileDebugKotlin --console=plain

# 发版前：签名校验（需要口令，见第 5 节）
& F:\Android\gradle-dist\gradle-8.13\bin\gradle.bat --no-daemon :app:checkReleaseSigning --console=plain

# 完整 release 构建
& F:\Android\gradle-dist\gradle-8.13\bin\gradle.bat --no-daemon :app:assembleRelease --console=plain
```

- `local.properties` 需含 `sdk.dir`（已被 `.gitignore` 忽略，不要提交）。
- 本机没有 `gradle` 命令、`keytool` 不在 PATH 时，用上面的绝对路径。
- **验证以实际命令输出为准**：`BUILD SUCCESSFUL` 才算通过；不要用「看起来对」代替编译。

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

---

## 5. 顶栏 / 底栏自动隐藏规范

实现在 `ui/widgets/compose/BarsAutoHide.kt`，核心是 `BarsAutoHideState` + `Modifier.barsAutoHide(edge)`。

**硬性约束（改动前务必读完）**

1. **不消费滚动**：`nestedScrollConnection` 的 pre/post 回调一律返回 `Offset.Zero`，只「观察」已消费的位移方向。**禁止**把用户的拖拽增量拿去做栏动画——那会给滚动叠加加速度，手感直接变差。这是本功能的设计红线。
2. **独立动画**：方向累计超过阈值后，用固定时长 tween 播完收起/展开，与手指位移解耦；松手后仍会滑到底。
3. **状态在 Activity 级共享**：`LocalBarsAutoHideState` 由 `BaseComposeActivity` 提供，`MyScaffold` 在每个新页面进入时调用 `show()` 复位。
4. **系统手势热区必须留在 `barsAutoHide` 之外**：底栏的手势 inset 占位（`bottomBarInsetsHeight()` / `bottomBarInsetsPadding()` / `navigationBarsPadding()`）要放在被隐藏内容的**外层**，否则收起后触摸会落到内容上。
5. 新增底栏时，写法固定为：

```kotlin
Column(modifier = Modifier.background(bg)) {          // 背景常驻
    Column(modifier = Modifier.barsAutoHide(BarEdge.Bottom)) {
        /* 真正会滑动隐藏的内容 */
    }
    Spacer(modifier = Modifier.navigationBarsPadding())  // 手势热区，不参与隐藏
}
```

6. 用户可在「自定义设置 → 顶栏 / 底栏跟随滑动隐藏」关闭该行为（`AppPreferencesUtils.hideBarsOnScroll`）。关闭后 `onScroll` 直接返回，不做任何动画。

---

## 6. 发版规范

### 6.1 版本号与变更记录

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

### 6.2 签名（固定发布签名）

- 发布包**只能**用仓库内 `app/tblite_debug.jks`（PKCS12，alias `tiebalite`）签名。它是历史所有发布包的签名，换 key 会导致老用户无法覆盖升级。
- 证书 SHA-256 指纹是发布契约：

  ```
  F7E02F10C7C9E83B943F8DEEE786B270330416C710CF5CFBCE73027AD19DA759
  ```

  期望值写在 `app/build.gradle.kts` 的 `pinnedReleaseCertSha256`，由 `:app:checkReleaseSigning` 在构建期校验；CI 还会用 `apksigner` 复核产出的 APK。**指纹不符 = 构建失败**，不要绕过。
- 口令来源（按优先级）：`keystore.properties` → 环境变量 `TBLITE_KEYSTORE_PASSWORD` / `TBLITE_KEY_PASSWORD`。
- `debug` build type 仍用 Android 默认 debug 签名，与发布包互不干扰；**禁止**再把 release 回落成 debug 签名（历史上正是这样造成了同版本号不同签名）。
- `app/tblite_debug.jks` 与其口令本来就是公开的：它只保证包可持续升级，不是防伪手段，无需额外加密或转移。

### 6.3 发布流程

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

### 6.4 禁止事项

- 不要手动上传 APK 到 Release（会被下一次工作流覆盖，且可能签错）。
- 不要修改 `pinnedReleaseCertSha256` 却不通知用户——那意味着全体用户必须卸载重装。
- 不要把 `keystore.properties`、`local.properties`、任何口令提交进仓库。

---

## 7. 交付前检查清单

- [ ] `:app:compileDebugKotlin` 通过
- [ ] 发版相关改动：`:app:checkReleaseSigning` 通过
- [ ] 新增字符串已进 `strings.xml`，无硬编码文案
- [ ] 新增设置项已在 `AppPreferencesUtils` 声明，键名与 UI `key` 一致
- [ ] 响应式改动走 `AdaptiveLayout`，未引入新的百分比宽度魔法数字
- [ ] 栏隐藏改动未消费滚动增量，手势热区仍在隐藏范围之外
- [ ] `git status` 中无意外文件（`local.properties`、构建产物、`.kotlin/sessions/` 等）
