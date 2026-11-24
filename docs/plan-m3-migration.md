# Material 3 迁移执行台账

本文是 [AGENTS.md](../AGENTS.md) §5「UI 大方向：Material 3 + 悬浮栏」的**执行台账**：阶段划分、每阶段的改动范围、验收门禁与进度。

- 规范条文（红线、约定）以 AGENTS.md 为准，本文不重复规定，只回答三个问题：**下一步做什么、做到什么算完成、现在在哪**。
- 每次动手前先看本文；改完把「进度台账」和对应阶段的「状态」更新掉。

---

## 0. 决策基线

| # | 决策 | 状态 |
|---|---|---|
| D1 | **顶栏收起时整条一起滑出**（状态栏底色带 + 标题行 + 次级槽页签/搜索框），**不要求**「推荐 / 热榜」在收起后仍停在状态栏下沿 | 用户确认（放宽旧的「页签常驻」验收） |
| D2 | 收起后状态栏区域直接显示滚动内容：无 scrim、无残留色带 | 用户确认保留 |
| D3 | 栏报告给父级的布局高度恒定，位移只发生在绘制层 | 用户确认保留（AGENTS.md §6-3） |
| D4 | 每阶段都要**浅色 / 深色两套主题各回归一次** | 用户确认保留 |
| D5 | 配色唯一来源 `ExtendedColors`，M3 侧由 `TiebaLiteTheme` 映射（已落地，见阶段 1） | 已实现 |
| D6 | 下拉刷新换成 M3 `PullToRefreshBox`，**接受**指示器观感变化 | 用户确认 |

---

## 1. 现状快照（2026-11-24，阶段 2a 之后）

| 指标 | 数值 |
|---|---|
| 业务 `.kt` 文件总数 | 395 |
| 仍 import M2 core（不含 icons）的文件 | **99** |
| import M3 的文件 | **12** |
| `MaterialTheme.typography.*` 引用 | 138 处 / 57 文件 |
| `MaterialTheme.colors.*` 引用 | 54 处 |

M2 存量热点（按文件数，`import` 计数）：

| M2 组件 | 文件数 | 说明 |
|---|---|---|
| `Text` | 73 | 机械替换；文字样式改名见阶段 1 的映射表 |
| `MaterialTheme` | 66 | 主要是读 `typography` / `colors`，需逐处映射到 M3 名字空间 |
| `Icon` | 34 | 机械替换（`material.icons.*` 不受影响，M2/M3 共用） |
| `pullrefresh.*` | **0** | 阶段 3a 已统一为 `ui/widgets/compose/PullToRefresh.kt` 的 `PullRefresh`（M3 `PullToRefreshBox`），17 个调用点全部迁完 |
| `IconButton` | 14 | 机械替换 |
| `ProvideTextStyle` / `LocalContentColor` | 12 / 12 | M3 有对应物；`LocalContentAlpha` 系列 M3 已移除，改显式 alpha |
| `Surface` | 10 | 注意 `tonalElevation` 会染色，已有 `surfaceTint = Transparent` 兜底 |
| `Tab` / `TabRow` | 8 | M3 是 `PrimaryTabRow` / `SecondaryTabRow` / `ScrollableTabRow` |
| `TopAppBar` | 1 | 阶段 2b 已清掉主入口的 2 处，只剩 `LogPage.kt` 直接引用，随阶段 5 设置类一起清 |
| `Scaffold` / `ScaffoldState` | **0** | 阶段 2a 已清空 |

---

## 2. 已完成

### 阶段 0 — 5.0.0 发版（已完成）

版本号 `5.0.0 / 500000`，AI_CHANGELOG + 四语言 README 三处同步；`:app:checkReleaseSigning` + `:app:assembleRelease` 通过；tag `v5.0.0` 已发布，线上 APK 证书 SHA-256 与契约一致，可与 v4.0.65 互相覆盖升级。

### 阶段 1 — M3 主题桥接（已完成，`7883d9af`，随 v5.0.0 发布）

| 文件 | 内容 |
|---|---|
| `ui/common/theme/compose/ColorScheme.kt` | `material3ColorScheme()`：`ExtendedColors` → M3 `ColorScheme`；`surfaceTint = Transparent`；`surfaceContainer*` 按背景/卡片/芯片三档填充 |
| `ui/common/theme/compose/Type.kt` | `Material3Typography`：`h6→titleLarge`(20sp/32sp)、`body1→bodyLarge`、`subtitle1→titleMedium`、`button→labelLarge`，其余沿用 M3 默认（本就同值） |
| `ui/common/theme/compose/Shape.kt` | `Material3Shapes`：small/medium/large 与 M2 同值，`extraLarge = 28dp` 与胶囊同族 |
| `ui/common/theme/compose/Theme.kt` | `TiebaLiteTheme` 在 M2 `MaterialTheme` 内层再包一层 M3 `MaterialTheme`，两层读同一份 `ExtendedColors` |

验收证据：AMOLED 深色 + 浅色（清新蓝）两套下，M3 基线色 `#6750A4` / `#EADDFF` / `#D0BCFF` / `#E8DEF8` 全图零命中；弹层底色取主题色（深色 `#000000` / 浅色 `#FFFFFF`），拖拽条为中性灰。

### 阶段 2a — MyScaffold（已完成，`601e5c8a`）

| 改动 | 内容 |
|---|---|
| `ui/widgets/compose/Scaffold.kt` | M2 类型全换 M3：`ScaffoldState`→`SnackbarHostState`、M2 `SnackbarHost`、`SwipeToDismiss`→`SwipeToDismissBox`、`FabPosition` 换包 |
| 同上 | 删除整条抽屉分支（M2 `Scaffold` + 8 个 drawer 参数）：21 个调用点无人使用；同时删 `contentColor` 与 `isFloatingActionButtonDocked` |
| 同上 | **保留悬浮 overlay 布局，不套 M3 `Scaffold`**（M3 Scaffold 会为栏预留布局空间，违反 §5.3 / §6） |
| 6 个页面 | `ForumPage` / `HistoryPage` / `ThreadPage` / `ThreadStorePage` 改 `remember { SnackbarHostState() }`；`LoginPage` / `ForumThreadListPage` 换 M3 的 `SnackbarDuration` / `SnackbarResult` / `SnackbarHostState` |

踩坑记录：M3 `showSnackbar` 第三个位置参数是 `withDismissAction: Boolean`（不是 M2 的 `duration`），位置传参必须改成具名。

验收证据：`compileDebugKotlin` / `assembleDebug` 通过；发现页 / 帖子页 / 浏览记录页渲染正常；Snackbar 底色 `#CFCFCF`（主题反色面，非 M3 基线 `#313033`）；上滑可关闭。

### 阶段 2b — `Toolbar.kt`（已完成，`fb07d191`）

| 改动 | 内容 |
|---|---|
| `TitleCentredToolbar` | 去掉 M2 的 content 槽 `TopAppBar`（它本质只是 `Surface` + 空 `Column` 包装），保留原有居中行布局；标题样式改用 M3 `titleLarge`（与 M2 `h6` 同值） |
| `Toolbar` | M3 `TopAppBar` + `TopAppBarDefaults.topAppBarColors(...)`；`expandedHeight = 56.dp` 对齐 M2 栏高；**`windowInsets = WindowInsets(0, 0, 0, 0)`**，否则 M3 自带状态栏 inset 会与 `TopAppBarContainer` 手画的底色带叠加 |
| `AccountNavIcon` | M2 `DropdownMenuItem` 的 content 槽 → M3 `text` 槽；`contentPadding` 显式 16dp / 8dp，抵消 M3 默认的 12dp 左右边距 |
| 内联组件 | `Text` / `Icon` / `IconButton` / `MaterialTheme` / `ProvideTextStyle` 全换 M3 |
| `TextFields.kt` | `ProvideContentColor` 升级为 M2 + M3 双写：同时提供 M2 `LocalContentColor` / `LocalContentAlpha` 与 M3 `LocalContentColor` |
| `Toolbar.kt` | 新增私有 `ProvideTopBarTitleStyle`：同时给两套 `LocalTextStyle`，否则页面传入的 M2 `Text` 标题会掉样式（h6 + Bold → body1） |

**为什么不给 `TitleCentredToolbar` 套 M3 `TopAppBar`**：M3 单行栏强制 `heightIn(min = expandedHeight)`（默认 64dp）且标题槽自带 start inset；居中标题栏的高度由内容决定（48dp 的头像 / 图标），套上去栏会变高、标题会偏——与本阶段「观感不变」冲突，所以只换容器内的组件、保留行布局。

踩坑记录：

1. `topAppBarColors` 必须显式传 container / title / navigationIcon / actionIcon 四个色，否则回落到 M3 基线的 `surfaceContainer` / `onSurfaceVariant`。
2. 两套 Material 的 `LocalTextStyle`、`LocalContentColor` 是**不同**的 `CompositionLocal`：只换 M3 一侧，页面传入的 M2 组件读不到（标题回落 body1、图标回落 onSurface）。迁移期由上面两个桥接函数兜住，页面全部迁到 M3 后删掉 M2 那一半。

验收证据（同一台 `emulator-5554`，改动前 / 改动后 APK 各跑同一套脚本，容忍 8/255）：

- `:app:compileDebugKotlin`、`:app:assembleDebug` → `BUILD SUCCESSFUL`；本文件无新增编译告警。
- 静态渲染逐像素比对，顶栏区域差异**只有状态栏时钟**：
  - 浏览记录页（`TitleCentredToolbar` + 页签，浅色）：顶栏区 275 / 496800，bbox `x[95..113] y[16..42]`；
  - 贴吧页（`Toolbar` + 页签，浅色）：顶栏 + 页签区 255 / 669600，bbox 同上；整页差异全在列表内容（两次抓取之间贴子更新）；
  - 主题选择页（`TitleCentredToolbar`，纯黑 AMOLED）：全页 177 / 2592000，bbox 同上。
- 无障碍树 bounds 逐行比对：浏览记录页、主题选择页 **14 行完全一致**；贴吧页仅列表条目变化，标题 `[231,288][389,363]`、页签 `[42,480][110,529]` 一致。
- 栏收起行为（贴吧页，3 次上滑 / 3 次下滑）：收起后页签 `y480 → 249`、标题移出语义树；滑回后 `y288 / y480`，与改动前 APK 完全一致（重复跑一次复现；首次 73px 偏差是动画未落定）。

---

## 3. 待做阶段

### 阶段 2c — `Tabs.kt`

`Tab` / `TabRow` / `ScrollableTabRow` → M3 `PrimaryTabRow` / `SecondaryTabRow` / `ScrollableTabRow`；`TabRowDefaults.tabIndicatorOffset`（M3 已废弃）改 `SecondaryIndicator`。影响主界面「推荐/热榜」与吧页分类页签。

### 阶段 2d — 基础件：`Dialogs.kt` / `Texts.kt` / `Buttons.kt` / `FeedCard.kt`

`Text` / `Icon` / `Surface` / `TextButton` / `ButtonDefaults` / `OutlinedTextField` / `TextFieldDefaults` / `ProvideTextStyle` / `LocalContentColor` 换 M3。约束：`Dialogs.kt` 对外仍只暴露项目自己的 `Dialog` / `ConfirmDialog` / `AlertDialog` / `PromptDialog`（AGENTS.md §3.2）。

### 阶段 2e — 其它 M2 小件

`picker/*`、`Switch.kt`、`Avatars.kt`、`TextFields.kt`：清 `ContentAlpha` / `LocalContentAlpha`（M3 已移除）→ 用 `ExtendedTheme.colors.textSecondary/textDisabled` 或显式 `copy(alpha = …)`。

### 阶段 3 — 主界面 + 下拉刷新统一

**3a 下拉刷新统一（已完成）**

1. `ui/widgets/compose/PullToRefresh.kt` 的 `PullRefresh` 封装 M3 `PullToRefreshBox`，**一次封装、逐页替换**；已替换 17 个调用点（首页 / 吧页 / 用户资料页 + 推荐 / 关注 / 热榜 / 话题榜 / 通知 / 我的 / 搜索三页 / 帖子 / 收藏 / 吧内搜索 / 用户赞吧 / 用户发贴）。
2. 指示器位置由封装统一计算：偏移取 `LocalBarsContentPadding` 的 top（= `MyScaffold` 实测顶栏完整高度），因此落在栏下沿；`maxDistance` 固定为指示器容器直径 40dp，避免栏下多空一截。
3. 删掉旧的 `PullToRefreshLayout` 自绘实现、`PullRefreshOverlayState` / `LocalPullRefreshOverlay` 与 `MyScaffold` 里的 overlay 绘制；`Scaffold.kt` 不再参与下拉刷新。
4. 回归证据（emulator-5560，1080×2400 @420dpi）：推荐页下拉时帧间差仅指示器区域 `[486,382]-[594,492]`（动态栏底 336px）；热榜 `[484,394]-[592,498]`（同栏底）；首页 `[486,256]-[594,362]`（首页栏底 210px）；消息 / 我的 / 吧页下拉均有反馈；改动前推荐页与热榜页下拉全程截图 MD5 不变（零反馈）。

**3b 主界面导航（待办）**

1. `NavigationComponents.kt`：M2 `NavigationRail` / `NavigationRailItem` / `BottomNavigationItem` → M3 `NavigationRail` / `NavigationBar`；大屏导航形态仍由 `MainPage` 统一决定（AGENTS.md §4-4）。
2. `ExplorePage` 次级槽已加「只看已关注的吧」开关（推荐页签右侧 `IconButton`，选中态用 `primary` 着色），顶栏其余部分仍走 `Toolbar` 的 M2 页签，随阶段 2c 一起迁。

观感变化已获授权（D6）：指示器由 M2 圆形下拉变为 M3 容器形变样式。

### 阶段 4 — 帖子页 / 吧页

`ThreadPage.kt`（混用最重的文件）→ `ForumPage` / `ForumSearchPostPage` / `ForumThreadListPage` / `SubPostsPage` / `ReplyPage`。阶段 3 的 `PullRefresh` 封装在这里直接复用。

### 阶段 5 — 设置类

`ui/common/prefs` 的 `PrefsScreen.kt` / `PrefsListItem.kt` 先改（所有设置页都经过它们），再逐页：`CustomSettingsPage` / `HabitSettingsPage` / `AppThemePage` / `AboutPage` / `LogPage` / `MoreSettingsPage` / `OKSignSettingsPage` / `BlockSettingsPage` / `BlockListPage` / `AccountManagePage`。

### 阶段 6 — 依赖收尾

目标不是「依赖清零」，而是「新代码零 M2 + 残留有明确豁免清单」。现实约束：`accompanist-navigation-material`、`accompanist-placeholder-material` 绑定 M2，不换实现就无法移除；`androidx.compose.material:material-icons-*` 与 M3 共用，不需要拆。

---

## 4. 每阶段统一门禁（DoD）

1. `:app:compileDebugKotlin` → `BUILD SUCCESSFUL`（AGENTS.md §2）。
2. 涉及页面：浅色 + 深色各一张截图，肉眼比对「观感不变」。
3. 栏相关改动额外验 AGENTS.md §6 四条：不消费滚动增量、独立动画、只改位置不改尺寸、背景随栏位移。
4. 响应式只用 `AdaptiveLayout`，不新增百分比宽度（§4）。
5. 新增文案进 `strings.xml`，不硬编码。
6. 一阶段一提交，Conventional Commits，中文说明；提交信息里写清验证证据。

---

## 5. 风险与回滚

| 风险 | 应对 |
|---|---|
| M3 组件自带 `tonalElevation` 染色 | `ColorScheme.surfaceTint` 已固定 `Transparent`；新组件如仍偏色，显式传 `tonalElevation = 0.dp` |
| M3 `TopAppBar` / `Scaffold` 自带 inset 与项目手工 inset 叠加 | 一律显式 `windowInsets = WindowInsets(0)`，inset 由页面根部统一处理（§4-6） |
| 位置参数语义变化（如 `showSnackbar` 第三位） | 迁移时统一改**具名参数**，不靠位置猜 |
| 大范围替换导致观感漂移 | 每阶段独立提交、独立验收；出问题只回滚该阶段提交，不动其它阶段 |
| Windows 构建环境差异 | 见 AGENTS.md §2：Gradle user home 必须与工程同盘（Glide+KSP2）；Compose 指标默认关闭（冒号路径） |

已知问题（阶段 2b 验证时发现）与后续处理：

| 现象 | 证据 | 结论 |
|---|---|---|
| `floatingBarColor()` 的 `compositeOver` 参数顺序反了：`color.compositeOver(overlay)` 是「不透明栏色盖在 12% 白上」，提亮被完全遮住，纯黑主题下顶栏 / 底栏实际仍是 `#000000` | 纯黑主题顶栏截图 `x=1000` 列 `y0..420` 全为 `0,0,0`，注释写的 `#1F1F1F` 未出现 | **评估后不修**：改成 `overlay.compositeOver(color)` 会让深色 / 纯黑主题的顶栏与底栏整体变亮一档（`#000000` → `#1F1F1F`），用户 2026-11 明确不接受该观感变化。已回退代码，只把误导性注释改成与实现一致的说明 |
| 打开「设置 / 关于 / 主题选择 / 浏览记录」等 push 路由后整页只剩一行工具栏：标题落在 `y≈1194`，正文与页签都不在语义树里；帖子页也只剩顶栏 + 底栏胶囊 | `TopAppBarContainer` 临时日志：首页 `topBarHeight=80dp/210px`（正常），push 页 `topBarHeight=914.3dp/2400px`（= 整屏） | **已修**（`9affea67`/`fb07d191` 引入的回归）：`TitleCentredToolbar` 的标题行用了 `Box(Modifier.fillMaxSize())`，顶栏在浮层里拿到松约束时会撑成整屏高，把正文挤出屏幕。改为定高 `TopBarRowHeight = 56.dp`（`Modifier.fillMaxWidth().height(...)`）。修复后设置 / 关于 / 主题选择 / 浏览记录 / 帖子页均正常渲染 |

---

## 6. 进度台账

- [x] 阶段 0 — 5.0.0 发版
- [x] 阶段 1 — M3 主题桥接（`7883d9af`）
- [x] 阶段 2a — `MyScaffold`（`601e5c8a`）
- [x] 阶段 2b — `Toolbar.kt`（`fb07d191`）
- [ ] 阶段 2c — `Tabs.kt`
- [ ] 阶段 2d — `Dialogs` / `Texts` / `Buttons` / `FeedCard`
- [ ] 阶段 2e — 其它 M2 小件
- [x] 阶段 3a — 下拉刷新统一（`PullRefresh` + M3 `PullToRefreshBox`，17 个调用点；下拉指示器移入栏下沿）
- [ ] 阶段 3b — 主界面导航 M2 → M3
- [ ] 阶段 4 — 帖子页 / 吧页
- [ ] 阶段 5 — 设置类
- [ ] 阶段 6 — 依赖收尾
