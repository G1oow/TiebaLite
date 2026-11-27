# DebugLog

本文件记录本仓库**已定位并修复的 bug**：现象、复现、定位过程、根因、修复内容与验证证据。

与其它文档的分工：

| 文档 | 回答什么 |
|---|---|
| `debuglog.md`（本文） | 这个 bug 当时长什么样、怎么找到的、改了什么、怎么证明修好了 |
| `AI_CHANGELOG.md` | 面向用户的发版说明（CI 按 tag 抽取） |
| `docs/plan-m3-migration.md` | Material 3 迁移的阶段划分、门禁与进度 |
| `AGENTS.md` | 开发与发版规范（含「修复 bug 必须回填本文」的约定） |

维护约定：

1. 一个 bug 一条，ID 形如 `DBG-00X`，编号升序、新的追加在文末，顶部索引表同步。
2. 每条至少含：现象、复现、定位、根因、修复、验证、涉及文件、遗留。没有的内容写「无」，不要省略小节。
3. 验证必须可复核：设备、分辨率、具体坐标 / 像素 / MD5 / 命令输出。禁止用「看起来正常」代替证据。
4. 状态只有三种：`已修复`、`已修复（未提交）`、`部分修复`。提交后补 commit hash。

---

## 环境基线

| 项 | 值 |
|---|---|
| 设备 | `emulator-5560`（`adb devices` 唯一在线设备） |
| 屏幕 | 1080 × 2400，density 420（**1dp = 2.625px**：80dp = 210px、128dp = 336px、40dp = 105px） |
| 变体 | `debug`（不混淆），`:app:assembleDebug` 产物 41.5MB |
| 构建 | `F:\Android\gradle-dist\gradle-9.9.2\bin\gradle.bat --no-daemon`，`JAVA_HOME=F:\Android\jdk`、`GRADLE_USER_HOME=F:\Android\gradle-home` |
| 提取证据 | `adb shell screencap` + `adb shell uiautomator dump`，PowerShell 逐像素 / 逐行统计 |

**2026-11-26 起（DBG-009 及之后）换了机器环境**，像素数字不能与上表混用：

| 项 | 值 |
|---|---|
| 设备 | `emulator-5554`（AVD `Pixel_7a`，API 37） |
| 屏幕 | 1080 × 2400，density 420；**状态栏 / 挖孔 118px**（上表那台是 63px），所以顶栏整体 = 118 + 147 = **265px** |
| 构建 | 仓库自带 `.\gradlew.bat --no-daemon`，`JAVA_HOME=F:\Android\jbr`（Android Studio 自带 JBR，JDK 25）、`GRADLE_USER_HOME=F:\Android\gradle-home`、`ANDROID_HOME=F:\androidsdk` |
| adb | `F:\androidsdk\platform-tools\adb.exe` |

---

## 索引

| ID | 日期 | 现象 | 状态 |
|---|---|---|---|
| [DBG-001](#dbg-001-全站下拉刷新无可视反馈--提示被顶栏遮住) | 2026-11-24 | 全站下拉刷新无反馈（推荐 / 热榜）或提示压在标题行上（首页 / 吧页） | 已修复（未提交） |
| [DBG-002](#dbg-002-只看已关注的吧占据推荐流首行) | 2026-11-24 | 「只看已关注的吧」作为列表首项占掉一整行，未并入顶栏 | 已修复（未提交） |
| [DBG-003](#dbg-003-个人主页滚动后顶栏消失--状态栏下方空白带) | 2026-11-24 | 个人主页滚动后头像 / ID / 返回 / 黑名单上滑消失，状态栏下方留一条空白带 | 已修复（未提交） |
| [DBG-004](#dbg-004-个人主页页签与首条内容之间-290px-空白) | 2026-11-24 | 个人主页页签与首条内容之间一大块空白（用户截图红框） | 已修复（未提交） |
| [DBG-005](#dbg-005-刷新结果提示更新了-n-条动态被顶栏盖住) | 2026-11-25 | 刷新后「更新了 N 条动态」提示落在顶栏底下，看不到 | 已修复（未提交） |
| [DBG-006](#dbg-006-帖子详情吸顶页签随栏动画伸缩-内容被推着走-147px) | 2026-11-25 | 帖子详情滑动时内容多位移一截（吸顶页签的顶部留白随栏收起比例伸缩） | 已修复（未提交） |
| [DBG-007](#dbg-007-关于页内容被顶栏切掉应用图标上半截看不见) | 2026-11-25 | 「关于」页应用图标被顶栏盖掉大半，页面像被裁掉一截 | 已修复（未提交） |
| [DBG-008](#dbg-008-帖子详情-2dp-分隔线滚动时抖动闪烁) | 2026-11-25 | 帖子页主楼下方那条 2dp 分隔线在滚动时自己抖 / 闪 1–2px | 已修复（未提交） |
| [DBG-009](#dbg-009-日志页内容被悬浮顶栏盖住) | 2026-11-26 | 「关于 → 查看日志」有日志时最早几条被顶栏盖住 | 已修复（`64741ec6`） |
| [DBG-010](#dbg-010-帖子详情主楼下方的分隔线按用户要求删除) | 2026-11-26 | 帖子页主楼底部那条线随滚动晃动，用户要求删除 | 已修复（`bc145e34`） |
| [DBG-011](#dbg-011-帖子详情吸顶页签被展开的顶栏盖住) | 2026-11-26 | 帖子页回拉后顶栏出现，把吸顶的「回复 / 全部 / 只看楼主」整条盖住 | 已修复（`bc145e34`） |
| [DBG-012](#dbg-012-顶部-inset-只取-statusbars两处按窗口百分比取宽) | 2026-11-26 | 5 处顶部避让只算 statusBars；个人主页 / 错误页按窗口百分比取宽 | 已修复 |
| [DBG-013](#dbg-013-通知权限请求撞上已销毁的-activity-崩溃) | 2026-11-26 | 切主题 / 快速进出页面偶发 `IllegalStateException: The activity has been destroyed` | 已修复（未提交） |
| [DBG-014](#dbg-014-个人主页页签下方的短横条整体右移) | 2026-11-26 | 个人主页页签下方 16dp 短横条整体右移约 16dp | 已修复（未提交） |
| [DBG-018](#dbg-018-吧内页滑动时列表被顶栏动画附加下滑加速度并截断底部) | 2026-11-26 | 吧内页滑动存在下滑加速度，且顶栏收起后列表底部被截断 147px | 已修复（`758dc764`） |
| [DBG-019](#dbg-019-首页动态消息标题左侧头像未与正文左侧对齐) | 2026-11-26 | 「首页」等主界面顶栏头像左沿停在 4dp，未与页面 16dp 左边界对齐 | 已修复（`5409199b`） |
| [DBG-020](#dbg-020-切页视差动画导致透明背景页透出前一页内容) | 2026-11-26 | 路由切换时新旧两页短暂重叠，画面透出前一页内容 | 已修复（`36c29960`） |
| [DBG-021](#dbg-021-消息页空态箱子图标与文字左偏错位) | 2026-11-26 | 「消息」页空态箱子图标与「这里什么都没有」整体左偏约 55px 未居中 | 已修复（`810bdff8`） |
| [DBG-022](#dbg-022-切换页面与列表滑动偶发掉帧) | 2026-11-26 | 切 Tab 与滑动信息流偶发掉帧，且旧版高刷逻辑未提交生效 | 已修复（`5a793138`） |
| [DBG-023](#dbg-023-退出帖子返回列表页后自动回到顶端) | 2026-11-26 | 从帖子返回吧内页/列表页时，列表直接弹回最顶端丢失阅读位置 | 已修复（`810bdff8`） |
| [DBG-024](#dbg-024-个人主页头像显示慢及帖子刷新与图片加载慢) | 2026-11-26 | 个人主页大头像先显灰圈等待；多图与帖子刷新慢 | 已修复（`18f61267`） |
| [DBG-025](#dbg-025-0-关注时关注页正文全空无任何提示) | 2026-11-26 | 用户 0 关注时进入「动态 → 关注」，正文区完全空白 | 已修复（`18f61267`） |
| [DBG-026](#dbg-026-动态页切换关注推荐热榜时页面与页签胶囊上下跳动) | 2026-11-26 | 「动态」页在「关注 / 推荐 / 热榜」间切换时，页签胶囊与页面正文上下跳动重新定位 | 已修复（`92e4a73e`） |
| [DBG-027](#dbg-027-帖子详情评论与楼中楼左右边距双重叠加过宽) | 2026-11-26 | 帖子详情页中主贴、评论与楼中楼离屏幕左右边界过远、明显偏中心 | 已修复（`e743ab61`） |
| [DBG-028](#dbg-028-搜索页顶栏遮挡页签不居中帖子卡片未走样式及搜吧分组条多余空隙) | 2026-11-26 | 搜索页历史/联想被顶栏遮挡、「搜贴」页签不居中、搜贴卡片未应用卡片样式、搜吧「相关吧」与页签间多出空隙 | 已修复（未提交） |

---

## DBG-001 全站下拉刷新无可视反馈 / 提示被顶栏遮住

**状态**：已修复（未提交）

### 现象

- 「动态 → 推荐」「动态 → 热榜」：下拉全程**没有任何视觉反馈**，松手约 2s 后内容突然变化。
- 首页、csgo 吧：有反馈，但「下拉即可刷新 / 松手刷新」文字压在标题行顶部，看起来像浮错位置的字。

### 复现

进页面 → 从内容区 `DOWN 540 700`，分 10 步 `MOVE` 到 `540 1360` → 途中截图 `pull`、松手后截图 `release` → 与 `idle` 比对。

### 定位

1. 核对设备上的包：`dumpsys package` 的 `lastUpdateTime=2026-11-24 18:43`，与 HEAD 不符（是 `a0486dd8` revert 之前的旧包）。**重新 `assembleDebug` + `install -r` 后才开始实测**，否则所有结论都不可信。
2. `grep 'SwipeRefresh|PullRefresh|RefreshIndicator'`：两套实现并存 —— 自绘 `PullToRefreshLayout`（首页 / 吧页 / 用户资料页）+ M2 `Modifier.pullRefresh` + `PullRefreshIndicator`（14 页）。
3. 读 `PullToRefresh.kt` 与 `Scaffold.kt`：自绘页把提示交给 `MyScaffold` 的 overlay，偏移取 `topBarInsetsHeight()`（只含状态栏）；M2 页的指示器 `align(TopCenter)` 对齐到内容区顶边。
4. 方向已由台账确定：`docs/plan-m3-migration.md` 的 D6「统一到 M3 `PullToRefreshBox`，接受指示器观感变化」。

### 根因

顶栏改成不透明 overlay 后正文从 `y=0` 起铺满整屏（AGENTS.md §5.3）：

- M2 页的指示器落在 `y≈0–105px`，被「状态栏底色带（0–62px）+ 标题行」整块盖住 → 零反馈。
- 自绘页的 overlay 偏移只算了状态栏 24dp，没有算顶栏完整高度 → 提示贴着状态栏下沿、悬在标题行上方。

### 修复

| 文件 | 改动 |
|---|---|
| `ui/widgets/compose/PullToRefresh.kt` | 重写为唯一封装 `PullRefresh`（M3 `PullToRefreshBox`）。指示器槽位 `padding(top = LocalBarsContentPadding.current.calculateTopPadding())`，即 `MyScaffold` 实测的顶栏完整高度；`maxDistance = 40.dp`（M3 `SpinnerContainerSize` 未公开，按视觉常量固定），避免栏下多空一截；配色取 `ExtendedTheme.colors.pullRefreshIndicator` / `primary` |
| `ui/widgets/compose/Scaffold.kt` | 删除 `PullRefreshOverlayState` / `LocalPullRefreshOverlay` 与 overlay 绘制（-19 行），`MyScaffold` 不再参与下拉刷新 |
| 17 个调用点 | 首页 / 吧页 / 用户资料页 + 推荐 / 关注 / 热榜 / 话题榜 / 通知 / 我的 / 搜索三页 / 帖子 / 收藏 / 吧内搜索 / 用户赞吧 / 用户发贴，全部改为 `PullRefresh { }`，并清掉 `androidx.compose.material.pullrefresh.*` 死 import（现全仓 0 引用） |

### 验证

| 页面 | 修前 | 修后 |
|---|---|---|
| 推荐 | idle / pull / release 三帧 **MD5 完全相同** | 帧差仅 `[486,382]–[594,492]`（动态栏底 336px） |
| 热榜 | 三帧 MD5 完全相同 | `[484,394]–[592,498]`（栏底 336px） |
| 首页 | 提示在 `[477,71]–[602,102]`（栏高 210px，压标题） | `[486,256]–[594,362]`（栏底 210px） |
| 消息 / 我的 / csgo 吧 | — | 下拉均有帧差，指示器在栏下方 |
| 帖子页 | — | 有反馈，但位移来自页头折叠（`enablePullRefresh = hasPrevious \|\| DESC` 门控），属既有设计 |

`compileDebugKotlin` / `assembleDebug` 均 `BUILD SUCCESSFUL`；新包 `install Success`，并用 `pm path` 拉回 APK 校验 dex 内含 `PullRefresh` 字样，确认装的是新构建。

### 涉及文件

`PullToRefresh.kt`、`Scaffold.kt`、`HomePage.kt`、`ForumPage.kt`、`UserProfilePage.kt`、`PersonalizedPage.kt`、`ConcernPage.kt`、`HotPage.kt`、`HotTopicListPage.kt`、`NotificationsListPage.kt`、`UserPage.kt`、`SearchForumPage.kt`、`SearchThreadPage.kt`、`SearchUserPage.kt`、`ForumSearchPostPage.kt`、`ThreadPage.kt`、`ThreadStorePage.kt`、`UserLikeForumPage.kt`、`UserPostPage.kt`、`ForumThreadListPage.kt`（只清 import）。

### 遗留

- 推荐页自动刷新浮层「更新了 N 条动态」仍用 `padding(top = 72.dp)`（`PersonalizedPage.kt:236`），动态栏高 128dp，它仍会被栏遮住。同类问题但属另一元素，未改，待确认。
- 搜索三页、收藏页、用户赞吧 / 发贴页未逐页上机下拉（代码路径与已验证页一致）；未做浅色主题截图。

---

## DBG-002 「只看已关注的吧」占据推荐流首行

**状态**：已修复（未提交）

### 现象

过滤开关是推荐流列表的第 0 项，随列表滚动，占据一整行（实测 `[42,357]–[1038,535]`，约 68dp）。用户要求把它并入动态页顶栏。

### 定位

- `grep 'personalized_followed_only'` → `PersonalizedPage.kt` 的 `item(key = "followed_only_filter")` + `FollowedOnlyFilter`（M2 `Row` + `Switch`）。
- 顶栏在 `ExplorePage.kt`：`Toolbar`（头像 + 「动态」+ 搜索）+ 次级槽 `TabRow`（`76.dp * pages.size` 居中）。
- 状态链路：`PersonalizedUiState.followedOnly` + `SetFollowMode` 意图 → 写 pref `personalized_followed_only` 并重拉首页。
- 落位方案由用户在三个选项里选定：**页签行右侧的可选中图标，仅「推荐」页签显示**。

### 根因

功能实现时把开关当成了列表内容，没有接到 `Toolbar` 的次级槽；同时 `ExplorePage` 与 `PersonalizedViewModel` 之间没有共享状态的通道。

### 修复

| 文件 | 改动 |
|---|---|
| `ui/page/main/explore/ExplorePage.kt` | 次级槽改 `Box`：`TabRow` 仍居中，行尾加 `IconButton`（`FilterAlt` 实心 + `primary` 着色 = 开，`FilterAltOff` + `onTopBar` = 关），仅 `personalized` 页签显示；状态读 `rememberPreferenceAsState(booleanPreferencesKey("personalized_followed_only"))`，点按写 `appPreferences.personalizedFollowedOnly` 后 `emitGlobalEvent(GlobalEvent.Refresh("personalized"))` |
| `ui/page/main/explore/personalized/PersonalizedPage.kt` | 删除列表首项与 `FollowedOnlyFilter`，连带清理 `FeedList` 的 `followedOnly` / `onSetFollowedOnly` 参数与页面侧采集 |
| `ui/page/main/explore/personalized/PersonalizedViewModel.kt` | 删除随之失去引用的 `SetFollowMode` 意图 / `PartialChange` / `UiState.followedOnly`（-77 行）；过滤逻辑仍由 `Refresh` / `LoadMore` 读 pref 决定 |

选择「走 pref + 全局刷新事件」而不是在 `ExplorePage` 里共享 `PersonalizedViewModel`：后者会让 `pageViewModel()` 重复注册公共事件收集，Toast 会弹两次。

### 验证

| 检查 | 结果 |
|---|---|
| 推荐页可见 | `content-desc="只看已关注的吧"` @ `[975,242]–[1038,305]`，页签仍居中 |
| 切到关注 / 热榜 | 隐藏；切回推荐重新出现 |
| 点按切态 | 图标主色像素 824 → 0、白色像素 0 → 771，同时列表重拉 |
| 进程重启后 | 主色像素仍 824，说明 pref 读写闭环 |

### 过程中的自伤与纠正

按行区间删除 `SetFollowMode` 时连带删掉了 `Agree` 的 intent 注册行，编译前 grep `Agree` 发现并补回；另有一次区间删除漏掉一处 `)`，编译器报 `PersonalizedPage.kt:110 Expecting ')'`，补回括号后通过。

### 涉及文件

`ExplorePage.kt`、`PersonalizedPage.kt`、`PersonalizedViewModel.kt`。

### 遗留

无。`personalized_followed_only` 在设置页没有独立入口（只有顶栏图标），符合当前需求。

---

## DBG-003 个人主页滚动后顶栏消失 + 状态栏下方空白带

**状态**：已修复（未提交）

### 现象

用户反馈「页面中还保留着之前 M3 顶栏的遮罩，导致画面中有一块部分完全不显示内容」，并要求「下滑时头像 / ID / 返回 / 黑名单保持不动」。实测滚动后：顶栏整条上滑消失，状态栏下方留一条 **147px 纯底色空白带**。

### 定位

1. `BarsAutoHide.kt`：`onScroll` 累计同向滚动超过阈值就 `animateTo(1f)`，`TopAppBarContainer` 只把栏做 `graphicsLayer` 位移（布局高度不变）。
2. `UserProfilePage.kt`：`PullRefresh` 内的 `Column(Modifier.padding(paddingValues))` 仍按栏高 80dp / 210px 留位 —— 栏滑走后这块空间没有任何内容。
3. 证据：滚动两次后语义树里 `返回` / 昵称 / `屏蔽选项` 全部消失；截图 `y=63..210` 采样全为 `0,0,0`。

### 根因

栏「跟随滑动隐藏」的设计前提是「栏滑走后露出的就是滚动内容」（AGENTS.md §5.3）。个人主页的正文顶部是固定头部而不是滚动内容，于是 padding 留下的空间成了永久空白；而页头本身是导航标识，本来也不该被上滑带走。

### 修复

| 文件 | 改动 |
|---|---|
| `ui/widgets/compose/BarsAutoHide.kt` | 增加页面级常驻计数 `pinCount` 与 `pin()` / `unpin()`；`pin()` 立即 `show()`；`onScroll` 在 `pinCount > 0` 时直接返回。计数器而非布尔值：栏状态是 Activity 级共享，同一时刻可能多个页面持有 |
| `ui/page/user/UserProfilePage.kt` | 在普通 / 大屏两套布局的公共父级 `UserProfileContent` 里 `DisposableEffect` 成对调用 `pin()` / `unpin()` |
| `AGENTS.md` §6 | 新增第 9 条，写明这类页面的正确做法与「栏滑走 → 空白带」的成因 |

### 验证

| 检查 | 结果 |
|---|---|
| 滚动 2 次 / 4 次后 | `返回` @ `[43,105]–[106,168]`、昵称 @ `[264,95]–[445,179]`、`屏蔽选项` @ `[975,105]–[1038,168]` 均原位 |
| 页签位置 | `贴子 23` / `关注吧 7` @ `y=249–273`，紧贴栏下沿 |
| 空白带 | 逐行统计非底色像素，连续无内容段均 < 15 行（只剩正常内边距） |
| pin 释放 | 进入帖子页滚动，顶栏仍会自动收起 |
| 下拉刷新 | 本页仍出现指示器 @ `[488,306]–[592,410]` |

### 涉及文件

`BarsAutoHide.kt`、`UserProfilePage.kt`、`AGENTS.md`。

### 遗留

指示器在这一页会轻微压到页签行下沿（栏只到 210px，页签属页面正文）。属瞬时状态，未处理；如需也落到页签下方，需给 `PullRefresh` 增加可选顶部额外偏移。

---

## DBG-004 个人主页页签与首条内容之间 290px 空白

**状态**：已修复（未提交）

### 现象

修复 DBG-003 后，用户仍反馈截图红框区域没有内容：页签与首条帖子之间一大块空白。

### 定位

1. 语义树量尺寸：页签文字 `贴子 1133` 占 `y=1068..1117`，首条帖子作者名在 `y=1407` → 间隙 **290px**，与 210px 的顶栏高度接近，先怀疑重复避让。
2. 读 `UserPostPage.kt` / `UserLikeForumPage.kt`：列表走 `MyLazyColumn` 默认 `avoidTopBar = true`，会把 `LocalBarsContentPadding` 的顶栏高度加进 `contentPadding` —— 而外层 `Column` 已经用 `padding(paddingValues)` 避让过一次，于是同一份 210px 被算了两遍。

### 根因

避让顶栏这件事在同一个页面里发生了两次：父级 `padding` 一次，子列表 `contentPadding` 又一次。`MyLazyColumn` 的 `avoidTopBar` 注释里本来就写了这个约束，但个人主页的三个内层列表没传。

### 修复

| 文件 | 改动 |
|---|---|
| `ui/page/user/post/UserPostPage.kt` | 新增 `avoidTopBar: Boolean = true`，透传到 `UserPostList` → `MyLazyColumn(avoidTopBar = …)` |
| `ui/page/user/likeforum/UserLikeForumPage.kt` | 同上 |
| `ui/page/user/UserProfilePage.kt` | 普通 / 大屏 × 帖子 / 动态 / 关注吧 共 6 个调用点传 `avoidTopBar = false` |

默认值保持 `true`，这两个列表只在个人主页被调用，独立路由行为不变。

### 验证

| 项 | 修前 | 修后 |
|---|---|---|
| 页签文字 | `y=1068..1117` | `y=1056..1105` |
| 首条内容 | 作者名 `y=1407` | 头像从 `y=1185` 起 |
| 页签 → 首条间隙 | ≈290px | ≈80px（正常内边距） |
| 页签 → 首条之间的空白（逐行扫描） | 290px 无内容 | 63px（≈24dp，条目自身内边距；210px 的重复占位已消失） |
| 下拉刷新 | — | 仍生效（帧差为指示器区域） |

`:app:assembleDebug` `BUILD SUCCESSFUL`，新包已安装。

### 涉及文件

`UserPostPage.kt`、`UserLikeForumPage.kt`、`UserProfilePage.kt`。

### 遗留

「关注吧」页签在测试账号上为空态（`这里什么都没有`），空间按同一路径修正但无真实数据对照，需非空账号复核。

---

## DBG-005 刷新结果提示「更新了 N 条动态」被顶栏盖住

**状态**：已修复（未提交）

### 现象

「动态 → 推荐」下拉刷新成功后，预期的「更新了 N 条动态」胶囊提示看不到（用户反馈「被顶栏遮罩遮住」）。列表本身已滚到顶部、计数也在涨，只是提示不可见。

### 复现

「动态」页签 → 推荐 → 从内容区下拉刷新 → 刷新成功后 2s 内用 `uiautomator dump` 抓语义树，找 `text="更新了…"` 的节点与 `bounds`。

### 定位

1. `R.string.toast_feed_refresh` 只有一处使用：`PersonalizedPage.kt` 的 `RefreshTip`（`Box` + `align(TopCenter)` + `padding(top = 72.dp)`）。
2. 语义树量栏：标题「动态」`y=94..178`、页签「关注/推荐/热榜」`y=247..299` → 顶栏完整高度（状态栏底色带 62px + 56dp 标题行 + 48dp 页签槽）≈ **336px = 128dp**。
3. `72dp = 189px` 的偏移整段落在栏体 `0..336px` 内；而 `MyScaffold` 把 `topBar` 画在内容**之后**、栏底是 `floatingBarColor(topBar)` 不透明纯色 → 提示被整块盖住。

### 根因

顶栏是悬浮 overlay、正文从 `y=0` 起铺满整屏（AGENTS.md §5.3），提示却用**写死的 72dp** 而不是 `MyScaffold` 实测的栏高。栏高随次级槽（页签 / 搜索框）变化，写死的偏移必然在带页签的页面上失效。

### 修复

| 文件 | 改动 |
|---|---|
| `ui/page/main/explore/personalized/PersonalizedPage.kt` | `RefreshTip` 偏移改为 `LocalBarsContentPadding.current.calculateTopPadding() + RefreshIndicatorContainerSize + 12.dp`（栏下沿 + 下拉指示器槽 + 间距），KDoc 说明为何不能写死偏移 |
| `ui/widgets/compose/PullToRefresh.kt` | `RefreshIndicatorContainerSize`（40dp）由 `private` 改为 `internal`，供页面级浮层复用同一份指示器槽高，避免两处各写一个 40dp |

预留指示器槽是为了「2s 内二次下拉」时不出现圆形指示器压在提示上：M3 的指示器槽停在栏下沿、占 40dp，且指示器层绘制在内容之上。

### 验证

| 项 | 值 |
|---|---|
| 构建 | `:app:compileDebugKotlin` / `:app:assembleDebug` 均 `BUILD SUCCESSFUL` |
| 安装 | `adb install -r 'debug-5.1.0(501000).apk'` → `Success` |
| 提示文本 bounds（修后实测） | `[380,494][700,555]`；胶囊外沿上沿 = `494 - 21px(8dp 内边距)` = **473px** |
| 栏底 | ≈336px（同一份语义树量得），间距 **137px ≈ 52dp** = 40dp 指示器槽 + 12dp |
| 修前（按代码算） | 72dp = 189px，落在栏体 0..336px 内 → 不可见 |

### 涉及文件

`PersonalizedPage.kt`、`PullToRefresh.kt`。

### 遗留

「二次下拉时指示器不与提示重叠」由预留 40dp 槽位保证，未在设备上构造二次下拉实测。

---
## DBG-006 帖子详情吸顶页签随栏动画伸缩，内容被推着走 147px

**状态**：已修复（未提交）

### 现象

帖子详情页上下滑动时，内容除了跟手滚动还会额外上下"跑"一截；观感是「帖子详情里多了一块会移动的空间」，一滑就整段内容跟着动，松手还会再补一段。

### 复现

进任意帖子 → 手指按住下滑约 60px（越过 touch slop，触发顶栏显隐）→ 用 `uiautomator dump` 对比同一批节点（楼层作者名 / `回复 N` 行）的 `bounds.y`。

### 定位

1. 先排除自绘位移：帖子页链路 `MyScaffold → PullRefresh → LoadMoreLayout → MyLazyColumn` 没有任何内容位移（M3 `PullToRefreshBox` 只位移指示器；`LoadMoreLayout` 只 `offset` 指示器；`BarsAutoHide` 只移动栏）。按住下拉 600px 时全部节点 `bounds` 不变，向上拖 400px 时按滚动正常位移 —— 说明下拉方向没有布局位移，与「某次更新后多出一块空间」的描述不吻合。
2. 比对版本：`git diff f02a10ad..HEAD -- ThreadPage.kt` 里唯一与间距相关的改动，是吸顶页签 `Row` 多了 `.padding(top = stickyHeaderTopInset())`（a0486dd8 引入，随 v5.1.0 发布）；v5.0.0 时该 `Row` 只有 `.padding(8.dp)`。
3. `stickyHeaderTopInset()` = `statusBar + (barHeight - statusBar) * (1 - hiddenFraction)`，读的是顶栏收起动画的进度值 —— 它把「栏的 220ms 绘制动画」变成了列表项**布局高度**在 63..210px 之间来回伸缩。

### 根因

用布局高度（padding）去跟随栏的收起比例，于是每次滑动触发栏显隐，列表里就多出 / 少掉最多 56dp（147px）的空间，后面的内容被这截空间推着走。违反 AGENTS.md §6.3「栏只改位置、不改尺寸」。

### 修复

| 文件 | 改动 |
|---|---|
| `ui/page/thread/ThreadPage.kt` | 吸顶页签 `Row` 的 `padding(top = stickyHeaderTopInset())` 改为 `padding(top = topBarInsetsHeight())`（`statusBars ∪ displayCutout`，常量，与栏收起比例无关）；`import` 同步替换；注释写明为何不能跟随 `hiddenFraction`，以及「顶栏展开时这条页签藏在栏后面」的既定代价（方案 B） |
| `ui/page/subposts/SubPostsPage.kt`、`ui/page/search/user/SearchUserPage.kt`、`ui/page/search/forum/SearchForumPage.kt`、`ui/page/forum/searchpost/ForumSearchPostPage.kt`、`ui/page/history/list/HistoryListPage.kt` | 同款吸顶页签（共 7 处）按同一方案 B 改为 `topBarInsetsHeight()`，`import` 同步替换 |
| `ui/page/forum/ForumPage.kt` | 页头跟随栏改成**绘制位移**：`followingPadding.top` 固定为栏的完整高度 `LocalTopBarHeight`，再用 `graphicsLayer { translationY = -((barHeight - statusBar) * hiddenFraction) }` 跟随。视觉与原来一致（栏展开时页头贴栏下沿、收起后贴状态栏下），但布局高度恒定 |
| `ui/widgets/compose/BarsAutoHide.kt` | 删掉 `stickyHeaderTopInset()`（全部调用点已改完，留着就是「拿动画值算布局高度」的样板）；`hiddenFraction` 的 KDoc 改为「只允许在绘制层读」 |

### 验证

| 项 | 值 |
|---|---|
| 构建 | `:app:compileDebugKotlin`、`:app:assembleDebug` 均 `BUILD SUCCESSFUL`；`adb install -r` `Success`（`lastUpdateTime` 与 APK 一致） |
| 修前（按住下滑 60px） | 同屏节点统一 **+187px** = 滚动 ≈40 + 凭空 **147px**（147px = 56dp 标题行） |
| 修后（同一手法） | 「回复 55」+39px、「全部」+44px = 手指 60px − touch slop，**无额外位移** |
| 吸顶页签位置 | 栏收起 / 展开下均为 `y=84`（状态栏 63 + 8dp 21），不随栏动画变 |
| 修后复测（本轮这批改动装上后，帖子页同手法） | 「回复 161」+39px，同屏其他节点同样 **+39px**，栏显隐不再产生 147px 位移 |
| 吧页（`ForumPage` 改绘制位移后） | 进页面时页头「图拉丁吧」`y=252` = 栏底 210 + 16dp(42)，与改前一致；上滑 300px 后页签「最新」-391px（滚动 + 页头折叠），无崩溃 |

### 涉及文件

`ThreadPage.kt`、`SubPostsPage.kt`、`SearchUserPage.kt`、`SearchForumPage.kt`、`ForumSearchPostPage.kt`、`HistoryListPage.kt`、`ForumPage.kt`、`BarsAutoHide.kt`。

### 遗留

无。同类写法（含 `stickyHeaderTopInset()` 本身）已全部清除；吧页「页头跟随栏」是既定设计，仍保留跟随，只把实现从布局高度换成绘制位移。

---

## DBG-007 关于页内容被顶栏切掉（应用图标上半截看不见）

**状态**：已修复（未提交）

### 现象

「关于」页顶部应用图标被不透明顶栏盖掉大半、只剩下半截，整页内容看起来像从标题栏底下开始被裁掉一截。

### 复现

我 → 关于 → `uiautomator dump` 量节点：顶栏「关于」标题 `y=99..174`、返回键 `y=106..169`，栏底沿 ≈210px；而「贴吧 Lite」标题在 `y=347`。

### 定位

1. 页面结构：`MyScaffold { Column(fillMaxSize().verticalScroll()) { Spacer(32.dp); Image(88.dp); … } }`。
2. `MyScaffold` 的 content lambda 是 `(PaddingValues) -> Unit`，`PaddingValues` = 实测栏高（顶栏 80dp = 210px）。这里写成 `) { Column(...) }`，**把参数丢掉了**。
3. 栏是 overlay、正文从 `y=0` 起铺满整屏（AGENTS.md §5.3），于是顶部 32dp 留白把图标推到 `84..315`，其中 `84..210` 正落在栏体内部 → 被整块盖住；全页内容也比应有位置高了 210px。

### 根因

页面没吃 `MyScaffold` 下发的栏占位。栏不占布局，避让必须由页面自己用 `padding` / `contentPadding` 完成，否则内容一律从屏幕顶端开始。

### 修复

| 文件 | 改动 |
|---|---|
| `ui/page/settings/about/AboutPage.kt` | content lambda 取 `paddingValues`，`Column` 在 `fillMaxSize()` 之后、`verticalScroll` 之前加 `.padding(paddingValues)`——滚动视口整体退到栏下方 |

### 验证

| 项 | 修前 | 修后 |
|---|---|---|
| 「贴吧 Lite」标题 | `y=347..422` | `y=557..632`（+210 = 顶栏 80dp） |
| 应用图标实际位置 | `84..315`，其中 `84..210` 被栏盖住 | `294..525`，全部在栏下沿（210）之外 |
| 截图像素（图标区 470,300–610,520） | — | 高饱和像素 **50%**、11 种颜色 → 图标确实画出来了 |
| 截图像素（栏标题行 200,120–900,190） | — | 高饱和 **0%** → 栏区域干净、无内容透出 |
| 页面是否需要滚动 | 内容到 1403 结束 | 内容到 1613 结束，仍在一屏内 |
| 构建 | — | `:app:assembleDebug` `BUILD SUCCESSFUL`，`adb install -r` `Success` |

### 涉及文件

`AboutPage.kt`。

### 遗留

无。

---

## DBG-008 帖子详情 2dp 分隔线滚动时抖动 / 闪烁

**状态**：已修复（未提交）

### 现象

帖子详情页滚动时，主楼与「回复 N / 全部 / 只看楼主」页签之间那条 2dp 分隔线自己在上下抖 / 闪 1–2px，像没对齐像素。**位置本身不对**（它属于主楼 item 的尾部，不跟随页签）。

### 复现

帖子页滚到主楼末尾、页签进入视口（「全部」`y≈896`）→ 截图逐行采样。

### 定位

1. `VerticalDivider(thickness = 2.dp)` 的实现是 `Divider`（`Box` + `background`）；2dp × 2.625（420dpi）= **5.25px**，上下边缘落在小数像素上。
2. 滚动 / fling 时每帧的小数位置都在变，抗锯齿覆盖率随之变化：相邻帧里「几行是全色、几行是半覆盖」来回变 → 视觉上就是这条线在抖。
3. 位置、尺寸、父级都没有任何动画或位移（帖子页唯一的 `animateContentSize` 在加载更多指示器上，与它无关），所以不是布局在动，是**逐帧重新栅格化**。

### 根因

非整像素高度的实心细线，在会移动的容器里被逐帧重新栅格化，边缘覆盖率不稳定。

### 修复

| 文件 | 改动 |
|---|---|
| `ui/page/thread/ThreadPage.kt` | 新增私有 `PixelSnappedDivider`：**位置、padding、2dp 高度全都不变**，只在绘制层把高度取整（`floor(2dp.toPx())` = 5px）并按 `positionInWindow` 的小数部分把绘制起点吸附到整像素；颜色仍取 `ExtendedTheme.colors.divider`。原 `VerticalDivider(thickness = 2.dp)` 调用点替换为它 |

### 验证

| 项 | 结果 |
|---|---|
| 构建 / 装机 | `:app:assembleDebug` `BUILD SUCCESSFUL`；`adb install -r` `Success` |
| 滚动位置 1 | 线段 `y=824..828`：**恰好 5 行**纯色 `rgb(30,30,30)`，无半覆盖行 |
| 再滚 +43px | `y=849..853`：仍恰好 5 行同色 |
| 再滚 +37px | `y=869..873`：仍恰好 5 行同色 |
| 相对位置 | 页签「全部」`y=896` 时线在它上方 68px，与改动前一致（布局未动） |

### 涉及文件

`ThreadPage.kt`。

### 遗留

其它位置的 `VerticalDivider(thickness = 2.dp)`（推荐流卡片分隔线等）仍是原实现，同样可能抖，本次未改。

**2026-11-26 更新**：这条线已按用户要求整条删除，`PixelSnappedDivider` 不再存在，见 DBG-010。

---

## DBG-009 日志页内容被悬浮顶栏盖住

**状态**：已修复（`64741ec6`）

### 现象

「关于 → 查看日志」有日志时，最早的几条压在顶栏底下看不到；日志少的时候整页像是空的。

### 复现

登录 → 任意帖子点「评论一番」→ 输入几个字符（触发存草稿，写入 `AppLog`，不发送）→ 返回 →「我 → 关于 → 查看日志」。日志只在内存里，重装 / 杀进程后要重新产生。

### 定位

1. `uiautomator dump` 量节点：顶栏标题行 `y=154..229`，顶栏整体占 `0..265`（状态栏 118 + 标题行 56dp = 147）。
2. 改动前首条日志时间戳 `[32,5][192,35]`、第二条 `[32,75][192,105]`，第三条整条不在语义树里（被栏遮住），第四条只露出 `y=265..275` 的 10px —— 列表是从 `y=0` 排起的。
3. `LogPage` 的 `MyScaffold { }` 内容没用传入的 `PaddingValues`，列表是原生 `LazyColumn`；`MyScaffold` 的正文铺满整屏、栏是 overlay（AGENTS.md §5.3），不避让就一定被盖。与 DBG-007 同一类。

### 根因

页面绕过了 `MyLazyColumn`，没有任何一层替它合并 `LocalBarsContentPadding`。

### 修复

| 文件 | 改动 |
|---|---|
| `ui/page/settings/about/LogPage.kt` | `LazyColumn` → `MyLazyColumn`（按 `LocalBarsContentPadding` 自动加 `contentPadding`）；4 处硬编码文案改 `stringResource` |
| `ui/page/settings/about/AboutPage.kt`、`ui/page/main/user/UserPage.kt` | 「查看日志」「查看个人主页」改 `stringResource` |
| `res/values/strings.xml` | 新增 `title_app_log` / `desc_copy_log` / `desc_clear_log` / `title_no_log` / `button_view_log` / `title_view_profile` |

### 验证

| 项 | 结果 |
|---|---|
| 构建 | `:app:compileDebugKotlin`、`:app:assembleDebug` → `BUILD SUCCESSFUL` |
| 有日志 | 首条时间戳 `[32,5][192,35]` → `[32,270][192,300]`（栏底 265 + 行内 2dp），之后每 70px 一条，7 条全部在语义树里 |
| 空日志 | 「暂无日志」`[456,1170][624,1231]`、标题与三个按钮位置与改动前一致，`logcat -b crash` 为空 |
| 关于页 | 9 个节点 bounds 与改动前逐行一致 |

### 涉及文件

`LogPage.kt`、`AboutPage.kt`、`UserPage.kt`、`strings.xml`。

### 遗留

- 「查看个人主页」菜单项只在登录了多个账号时出现，未上机点开。
- `AboutPage.kt` 的「无法打开链接」「项目地址」、`UserPage.kt` 的「切换到: …」「回帖内容已自动保存」仍是硬编码，本次未改。

---

## DBG-010 帖子详情主楼下方的分隔线按用户要求删除

**状态**：已修复（`bc145e34`）

### 现象

用户反馈：帖子详情主楼底部有一条会跟着滚动晃动的线条，要求删掉。对应主楼与「回复 N / 全部 / 只看楼主」页签之间那条 2dp 分隔线（DBG-008 修过一次抖动的同一条）。

### 复现

帖子页滚到主楼末尾、页签进入视口 → 按住手指逐像素拖动（`input motionevent`，步长 1–2px、每步停 0.5s）并逐帧截图。

### 定位

1. 改动前这条线是 5 行纯色 `#F5F5F5`（浅色主题），位于「回复 N」文字顶边上方 161..165px。
2. 8 步逐像素拖动里，线始终恰好 5 行、与页签文字 1:1 同步移动（文字顶 `1925 → 1917`，线 `1760..1764 → 1752..1756`）。**这种停住再截图的手法没有复现出晃动**——惯性滚动中的逐帧表现没有量到，不能据此断言 DBG-008 之后它已经不抖。
3. 用户明确要求删除，不再继续追抖动原因。

### 根因

未定位（见上）。按需求删除该元素。

### 修复

| 文件 | 改动 |
|---|---|
| `ui/page/thread/ThreadPage.kt` | 删掉主楼 item 末尾的 `PixelSnappedDivider(...)` 调用、`PixelSnappedDivider` 本身，以及只为它服务的 6 个 import |

### 验证

| 项 | 结果 |
|---|---|
| 构建 | `:app:assembleDebug` → `BUILD SUCCESSFUL` |
| 线是否还在 | 页签文字顶 `y=1436` 时，`x=540` 列 `y=1255..1431` 全为底色 `#FFFFFF`，`y=1254` 往上是深色像素（应为主楼里的图片，未核对）；原位置（文字上方 161..165px）没有 `#F5F5F5` |

### 涉及文件

`ThreadPage.kt`。

### 遗留

- 主楼与页签之间的间距随之少了 10dp（线 2dp + 它的 `padding(bottom = 8.dp)`）。这是按代码推出来的，没有在同一个帖子上量前后差。
- 只在浅色主题下看过。

---

## DBG-011 帖子详情吸顶页签被展开的顶栏盖住

**状态**：已修复（`bc145e34`）

### 现象

帖子页往下看一段后回拉、停住，顶栏滑出来，把吸顶的「回复 N / 全部 / 只看楼主」整条盖住，要再往上滑让栏收起才看得到、点得到。

### 复现

帖子页上滑到页签吸顶（栏已收起）→ 手指回拉约 60px 并停住。

### 定位

1. 改动前，吸顶 + 栏收起：「回复 N」`[42,139][158,188]`，正常。
2. 改动前，吸顶 + 栏展开：「回复 N」不在语义树里，「全部」只剩 `[760,109][886,118]` 一条 9px 的残边；顶栏占 `0..265`，页签盒在 `0..209`，整条在栏底下。
3. 这是 DBG-006 留下的已知代价（当时的注释原话：「顶栏展开时这条吸顶页签藏在栏后面，收起后才露出来」）：为了不让布局高度跟着栏动画伸缩，页签的留白固定成状态栏高度，不再跟栏。

### 根因

`stickyHeader` 粘的是列表视口顶边 `y=0`，顶栏是 overlay、不占布局；页签自己没有任何随栏移动的量。

### 修复

| 文件 | 改动 |
|---|---|
| `ui/page/thread/ThreadPage.kt` | 页签外层 `Box` 加 `graphicsLayer { translationY = … }`：位移 =（栏高 − 状态栏高）×（1 − `hiddenFraction`）− 页签在视口里的自然位置，下限 0。吸顶时自然位置为 0，被推到栏下沿；还在列表流里、离顶边超过一个栏行高时位移为 0 |
| 同上 | 同一个 `Box` 加 `zIndex(1f)`：列表只在吸顶时把它画在最上层，刚离开吸顶、仍被推着的那一段里它是普通 item，会被后一楼盖住 |
| 同上 | key 提成常量 `ThreadHeaderKey`，供页签查自己在 `layoutInfo` 里的位置 |

布局高度没有动：`hiddenFraction` 与 `layoutInfo` 都只在 `graphicsLayer` 块里读（AGENTS.md §6.3）。

### 验证

第一版只有位移、没有 `zIndex`，实测在过渡段（回拉 180..240px）页签从语义树里消失、截图里是下一楼的内容盖在上面；补 `zIndex` 后重测如下。

| 项 | 结果 |
|---|---|
| 构建 | `:app:assembleDebug` → `BUILD SUCCESSFUL` |
| 吸顶 + 栏收起 | 「回复 N」`[42,139][158,188]`，与改动前一致 |
| 吸顶 + 栏展开 | 「回复 N」`[42,286][158,335]`（= 139 + 栏行高 147），「全部」`[760,265][886,374]`，栏底 265 以下完整可见 |
| 回拉全过程 | 按住手指 18 步 × 30px：第 2–10 步文字顶恒为 286；第 11 步起 `290 → 320 → … → 500`，与手指 1:1。每一步页签都在语义树里；第 2–18 步页签盒内 `x=300` / `x=540` 两列采样全为底色（第 1 步栏还没出来，没有采样） |
| 被推着时可点 | 点「只看楼主」后它的文字色由 `#66` 变 `#21`（选中）、「全部」变 `#66`，列表重载；再点「全部」两者换回 |
| 崩溃 | 全程 `logcat -b crash` 为空 |

### 涉及文件

`ThreadPage.kt`。

### 遗留

- 用同一种吸顶写法（`padding(top = topBarInsetsHeight())`）的还有 `SubPostsPage`、`HistoryListPage`、`SearchForumPage`、`SearchUserPage`、`ForumSearchPostPage`。按代码推断它们在栏展开时同样会被盖住，但没有上机确认，本次也没有改。
- 只在浅色主题、竖屏、Compact 宽度下测过；深色主题与宽屏 `Container` 收窄下没有上机。
- 栏的 220ms 收起 / 展开动画过程中页签是否与栏严格同步，没有逐帧量（只量了停住后的位置）。

---
## DBG-012 顶部 inset 只取 statusBars、两处按窗口百分比取宽

**状态**：已修复

### 现象

两处与 AGENTS.md 冲突的写法，都不是用户报上来的显性故障：

1. 5 处页面根 / 侧栏头部用裸的 `statusBarsPadding()` 做顶部避让（AGENTS.md §4-6 要求 `safeDrawing.only(Top)`）。
   刘海 / 挖孔高于状态栏的机型上，内容会被压住一截。
2. 2 处按窗口百分比取宽：个人主页 Expanded 两列容器用 `fillMaxWidth(0.75f)`、错误页用 `0.9f / 0.5f`
   （AGENTS.md §4-2 明确禁止）。窗口越宽行宽越失控。

### 复现

1. 需要刘海高于状态栏的机型才看得出差异——本机模拟器 `statusBars` 与 `displayCutout` 都是 118px，
   所以**这一项在这台设备上观感不变**，改动属于对齐规范与防回归。
2. 百分比宽度可复现：`adb shell wm size 2400x3000`（914dp 宽，Expanded）后进个人主页。

### 定位

1. `statusBarsPadding()` 只读 `WindowInsets.statusBars`；`WindowInsets.safeDrawing` 的 Top 另外并入了
   `captionBar` 与 `displayCutout`，才是 §4-6 定义的顶栏 inset。
2. 百分比写法的宽度随窗口线性增长：914dp 宽时 `0.75 × 914 ≈ 686dp` 且不封顶；
   错误页 `0.5 × 914 ≈ 457dp`。

### 根因

历史实现各自就地取了 inset / 比例，没有走 `ui/utils/BottomBarInsets.kt` 与
`ui/common/windowsizeclass/AdaptiveLayout.kt` 这两个唯一出口。

### 修复

| 文件 | 改动 |
|---|---|
| `ui/utils/BottomBarInsets.kt` | `topBarInsetsHeight()` 改读 `safeDrawing.only(WindowInsetsSides.Top)`；新增等值的 `Modifier.topBarInsetsPadding()`（同时消费该 inset） |
| `ui/page/main/NavigationComponents.kt`（抽屉头部、NavigationRail）、`ui/page/main/user/UserPage.kt`、`ui/page/reply/ReplyPage.kt` | 5 处 `.statusBarsPadding()` → `.topBarInsetsPadding()` |
| `ui/page/user/UserProfilePage.kt` | Expanded 两列容器 `.fillMaxWidth(0.75f)` → `.widthIn(max = AdaptiveLayout.ContentMaxWidth).fillMaxWidth()` |
| `ui/widgets/compose/Errors.kt` | 非 Compact 由 `fillMaxWidth(0.5f)` 改为「水平 16dp 留白 + `widthIn(max = AdaptiveLayout.DialogMaxWidth)` + `fillMaxWidth()`」；Compact 保留原 90% 留边（屏宽有限，不会失控） |

### 验证

设备 `emulator-5554`，浅色。

| 项 | 结果 |
|---|---|
| 构建 | `:app:compileDebugKotlin`、`:app:assembleDebug` → `BUILD SUCCESSFUL` |
| Compact（1080×2400 / 420dpi） | 动态页签 `关注[303,302][378,354] 推荐[502,302][577,354] 热榜[701,302][776,354]`，与改动前逐字一致 |
| Expanded（`wm size 2400x3000` = 914×1143dp） | 个人主页左列首元素 `[98,610][262,673]`，即内容从 x≈98 开始 = (2400 − 840dp×2.625)/2，说明收窄到 `ContentMaxWidth` 并居中；改动前的公式会从 x=300 开始（`0.75 × 2400 = 1800` 宽） |
| 错误页（Expanded，深链无效吧名） | 「出错了」`[1137,1470][1263,1531]`，中心 x=1200 = 屏宽一半，居中正常；无崩溃 |
| 崩溃 | 0 |

### 涉及文件

见上表 5 个文件。

### 遗留

- `Errors.kt` 的内容块宽度无法从语义树读出（内部元素都是 wrap content），只验证了居中与不崩溃；
  容器宽度是否恰好等于 560dp 没有直接量到。
- 刘海高于状态栏的机型没有实机可测，第 1 项改动在这一维度上属于「按规范改、未实测」。
- `PbContentRender` / `FeedCard` / `Search` 里按窗口宽度分档的媒体比例（0.5 / 0.6 / 0.8）**没有改**：
  那里表达的是「图片占卡片宽度的比例」，卡片本身已被 `Container` 收窄到 840dp，
  不是规范禁止的「窗口百分比当宽度上限」。

---

## DBG-013 通知权限请求撞上已销毁的 Activity 崩溃

**状态**：已修复（未提交）

### 现象

启动 / 切主题 / 快速进出页面时偶发闪退，`logcat` 里是：

```
FATAL EXCEPTION: main
java.lang.IllegalStateException: The activity has been destroyed,
    please manually determine the status of the activity
    at com.hjq.permissions.PermissionChecker.checkActivityStatus(PermissionChecker.java:51)
    at com.hjq.permissions.XXPermissions.request(XXPermissions.java:169)
    at com.huanchengfly.tieba.post.utils.PermissionRequester.start(PermissionUtils.kt:439)
    at com.huanchengfly.tieba.post.MainActivityV2.requestNotificationPermission(MainActivityV2.kt:312)
    at com.huanchengfly.tieba.post.MainActivityV2.onStart$lambda$1(MainActivityV2.kt:346)
    at android.os.Handler.handleCallback(Handler.java:1095)
```

另一种变体的首行是 `The activity has been finishing`，堆栈相同。

### 复现

前置：登录状态下撤销通知权限（`pm revoke ... POST_NOTIFICATIONS`），这样每次 `onStart` 都会真的去请求权限。

```powershell
adb shell cmd uimode night yes; Start-Sleep -Milliseconds 500
adb shell cmd uimode night no;  Start-Sleep -Milliseconds 500
# 连按 10 次，然后 adb logcat -b crash -d
```

`MainActivityV2` 的 `configChanges` 不含 `uiMode`，每次切换都会重建 Activity。
0.5s 间隔连切 10 次，在**修复前**的包上稳定复现（第一轮即崩）。

> 3 秒间隔切 4 次**不**复现——必须让下一次销毁落在上一次 `onStart` 之后 100ms 的窗口里。
> 早期几次偶发崩溃（12:41 / 12:49 / 15:24）是快速连续操作（dump + 点击 + 深链）时撞上的。

### 定位

1. `onStart()` 末尾原本是 `handler.postDelayed({ requestNotificationPermission() }, 100)`。
2. `handler` 是 Activity 的普通字段，`postDelayed` 的 Runnable **不随 Activity 销毁取消**。
3. 若 Activity 在这 100ms 内被销毁或进入 finishing，Runnable 仍会执行，`XXPermissions.request()`
   内的 `PermissionChecker.checkActivityStatus` 直接抛 `IllegalStateException`。
4. `git blame` 显示这三行自 2022-09-24 起就没动过（`7cf6a8680`），不是本次迁移引入的。

### 根因

权限请求挂在一个与 Activity 生命周期无关的 Handler 上，延迟窗口内 Activity 结束就会炸。

### 修复

| 文件 | 改动 |
|---|---|
| `MainActivityV2.kt` | `handler.postDelayed` → `lifecycleScope.launch { delay(...); if (lifecycle 至少 RESUMED && !isFinishing && !isDestroyed) requestNotificationPermission() }`；协程随 lifecycle 取消，再补一次「是否还在前台」判断 |
| 同上 | 删掉只此一处使用的 `handler` 字段与 `android.os.Handler` / `android.os.Looper` 两个 import；延后时长提成 `NotificationPermissionRequestDelay = 100L` |

行为未变：仍是 `onStart` 后 100ms 请求一次，仍只在「已登录 + API 33+」时请求。

### 验证

设备 `emulator-5554`，`pm revoke` 后未授权状态。

| 项 | 修复前（5.1.2 基线包） | 修复后 |
|---|---|---|
| 0.5s × 10 次切深浅色 | **第 1 轮即 FATAL**，堆栈同上 | 连跑 3 轮，`logcat -b crash` **0 行** |
| 权限弹窗是否仍出现 | 出现 | 出现（`Allow 贴吧 Lite to send you notifications?`，说明请求确实发出去了） |
| 构建 | — | `:app:compileDebugKotlin`、`:app:assembleDebug` → `BUILD SUCCESSFUL`，本文件无新增告警 |

### 涉及文件

`MainActivityV2.kt`。

### 遗留

- 仍是每次 `onStart` 都尝试请求（与改造前一致）。若已永久拒绝，系统不会弹窗，但每次仍会走一遍
  `XXPermissions` 流程；要不要加「只请求一次」的标记没有改。
- 只在 API 37 模拟器上验证；API 33~36 的权限对话框时序没有覆盖。

---

## DBG-014 个人主页页签下方的短横条整体右移

**状态**：已修复（未提交）

### 现象

用户反馈：个人主页「贴子 / 回复 / 关注吧」页签下方那条 16dp 短横条与文字不对齐，整体偏右。

### 复现

登录 → 「我」→ 点顶部资料卡进个人主页 → 截图量「贴子 0」文字框与线条的水平中心。

### 定位

1. 量到的数据：文字 `[42,1111][138,1160]`（中心 90），线条 `x=112..151`（中心 131.5）→ **偏右 41.5px ≈ 15.8dp**。
   线条宽度 42px = 16dp ✓，垂直位置正常，只有 x 不对。
2. 加临时日志打印 M3 传进来的数据：

   ```
   pos=L0.0.dp,W68.57143.dp,C36.571426.dp, L68.57143.dp,W68.57143.dp,C36.571426.dp, ...
   chosen=69 maxW=96 maxH=126
   ```

   `chosen=69`px 与"居中于 Tab（宽 68.57dp）"完全吻合，说明**公式没错**；但实际画在 112px。
3. 关键在 `maxW=96`：`SecondaryScrollableTabRow` 给指示器的**容器只有 96px 宽（= 选中页签的内容宽，即文字宽）**，
   而整行第一个 Tab 宽 180px。容器原点也因此落在内容左边缘（`(180 − 96) / 2 = 42px`）。
4. 于是 `placeRelative(x = 69)` 变成屏幕 `42 + 69 = 111` px —— 与实测 112 吻合。

### 根因

在可滚动页签栏里，M3 把指示器容器收窄到「选中页签的内容区」并以其左边缘为原点；
用 `TabPosition` 的**整行绝对坐标**去 `placeRelative`，就会整体多走 `(tabWidth − contentWidth) / 2`。
固定页签栏（`SecondaryTabRow`）的容器是整行，所以只有个人主页 / 吧页这类可滚动页签栏才暴露。

### 修复

| 文件 | 改动 |
|---|---|
| `ui/widgets/compose/Tabs.kt` | `PagerTabIndicator` 不再自己按 `TabPosition` 算 x：改为 `Modifier.tabIndicatorOffset(pagerState.currentPage, matchContentSize = true)` 让 M3 定位到内容区，内部 `Box(contentAlignment = BottomCenter)` 居中一条 16dp × 3dp 短横条（底部仍留 8dp） |
| 同上 | 删掉随之不再使用的 `Constraints` / `lerp` import，补 `fillMaxHeight` / `width` |

代价：跨页切换改成 M3 自带的平滑过渡，不再逐帧跟随手指（M3 页签栏的标准行为）。

### 验证

设备 `emulator-5554`（1080×2400 @420dpi），深色主题。「偏差」= 线条中心 − 页签文字中心。

| 页面 | 页签 | 修复前 | 修复后 |
|---|---|---|---|
| 个人主页（可滚动） | 贴子 | 中心 131.5 vs 文字 90 → **+41.5px** | `x=69..110` 中心 90 → **0px** |
| 同上 | 回复 | 未单独量 | 中心 270 → **0px** |
| 同上 | 关注吧 | 未单独量 | 中心 466 vs 467 → **−1px** |
| 同上（回点贴子） | 贴子 | — | 中心 90 → **0px** |
| 吧页（可滚动） | 最新 / 精华 | — | **0px / 0px** |
| 动态（固定） | 推荐 | — | 中心 540 → **0px** |
| 消息（固定） | 回复我的 | — | 中心 180 → **0px** |
| 浏览记录（固定） | 贴子记录 | — | **−2px** |
| 屏蔽词管理（固定） | 黑名单 | — | **0px** |
| 搜索结果（固定 + 菜单） | 搜吧 | — | **−1px** |

线条宽度全部 42px = 16dp；±1~2px 是 16dp（42px，奇数）除不尽的取整，肉眼不可见。
`compileDebugKotlin` / `assembleDebug` → `BUILD SUCCESSFUL`，全程 `logcat -b crash` 为空。

### 涉及文件

`ui/widgets/compose/Tabs.kt`。

### 遗留

- 滑动 pager 时短横条不再逐帧跟手，改为过半页后平滑过渡到新页签（M3 `tabIndicatorOffset` 自带动画）。
- 只量了静止状态；滑动过程中的中间位置没有逐帧比对。
- 只在深色主题下量了（当时模拟器停在深色），浅色主题未重量。

---


## DBG-015 草稿箱第一条草稿被悬浮顶栏盖住

**状态**：已修复（未提交）

### 现象

草稿箱有两三条草稿时，第一条永远顶在屏幕最上方：吧名/正文被状态栏与标题行切成两半，右侧删除按钮
只剩一条 12px 的缝可以点；列表再怎么滑，首条也回不到顶栏下方。

### 根因

`ui/page/settings/about/DraftPage.kt` 直接用裸 `androidx.compose.foundation.lazy.LazyColumn`：

```kotlin
LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp))
```

`MyScaffold` 的栏是悬浮 overlay，正文铺满整屏（AGENTS.md §5.3），顶部避让只能靠
`LocalBarsContentPadding` → 列表的 `contentPadding`（见 `MyLazyColumn`）。裸 `LazyColumn` 不读这个占位，
`Modifier.padding` 又只是缩视口——8dp 就是全部留白，首条必然落在顶栏底下。与 DBG-009（日志页）同一类问题。

### 修复

换成 `MyLazyColumn`（`ui/widgets/compose/LazyLists.kt`，内部把外层/本层栏高合并进 `contentPadding`）：
水平 16dp 仍用 `Modifier.padding`（不影响纵向避让），垂直 8dp 改走 `contentPadding`。
空态（「暂无草稿」居中 `Column`）不装列表，不需要改。

### 验证

设备 `emulator-5554`（1080×2400 @420dpi），草稿 2 条（`csgo` / `抗压背锅`），`uiautomator dump` 取 bounds。
「修复前」用临时 `MyLazyColumn(..., avoidTopBar = false)` 复现旧行为（此时顶栏占位被跳过，与裸 `LazyColumn` 等价），
量完立即改回。

| 节点 | 修复前 | 修复后 |
|---|---|---|
| 首条正文 `csgo` | `[84,74][162,117]` | `[84,339][162,382]` |
| 首条删除按钮 | `[902,106][965,118]`（只剩 12px） | `[902,371][965,434]`（完整 63px） |
| 第二条正文 `抗压背锅` | `[84,330][228,382]` | `[84,595][228,647]` |

顶栏标题「草稿箱」`[461,154][619,229]`，顶栏整块占 0..265px（状态栏 118px + 标题行 147px）。
首条正文上移量 339 − 74 = **265px**，正好等于顶栏高度——即内容已完整退到顶栏下方。
`compileDebugKotlin` / `installDebug` → `BUILD SUCCESSFUL`，`logcat -b crash` 全程为空。

### 涉及文件

`ui/page/settings/about/DraftPage.kt`。

### 遗留

- 页面内文案（「草稿箱」「暂无草稿」「已删除」「删除失败」…）仍是硬编码，未资源化（不在本次范围）。
- 只验证了有草稿的情形；1 条草稿时的底栏避让没有单独量。

## DBG-016 发版后旧版本用户启动时没有更新弹窗

**状态**：已修复（未提交）

### 现象

5.1.3 发布后，仍装 5.1.2 的用户启动应用完全看不到「发现新版本」提示；关于页手动检查却正常。

### 复现

AVD 上把 `application.properties` 临时改成 `versionName=5.1.2` / `versionCode=501004`
（编号抬高以便覆盖安装），装包后用真机网络（非 mock）分两组启动：

| 组 | 预置条件 | 结果 |
|---|---|---|
| A | `update_last_check_time` = 5 分钟前 | **无弹窗**（连检查都没发） |
| B | 删掉 `update_last_check_time` | **弹窗正常**：发现新版本 / v5.1.3 / 本次忽略 / 立即更新 |

B 组说明网络、版本比对、弹窗渲染都没问题，问题出在「什么情况下才允许检查 / 才继续提示」。

### 根因

三处静默路径，任一命中都表现为「启动不提示」：

1. `UpdateManager.check` 对 `STARTUP` 有 **6 小时节流**（`CHECK_INTERVAL_MS`）：6 小时内成功查过一次，
   新版本发布后的启动会直接 `return@launch`，连请求都不发。
2. `UpdateAvailableDialog` 的 `onDismiss` 直接调 `ignoreCurrentVersion()`：弹窗在启动瞬间弹出，
   用户误触外部或按返回就把**该版本永久静音**（写 `update_ignored_tag`），之后再也不会提示。
3. 只传了 `cancelable = !force`（管返回键），`cancelableOnTouchOutside` 仍是默认 `true`：
   **强更弹窗也能点外部关掉**。

### 修复

- 节流 `6h → 2min`（`STARTUP_CHECK_MIN_INTERVAL_MS`）：只用来挡同一会话里的重复触发
  （Activity 重建、切换主题重组），冷启动每次都真正检查。请求带 ETag，304 不计入 GitHub 匿名限额。
- 关弹窗不再等于忽略：`onDismiss = null`，只有「本次忽略」按钮写 `update_ignored_tag`。
- 补 `cancelableOnTouchOutside = !force`，强更点外部也关不掉。

### 验证

设备 `emulator-5554`，debug 包报告 `versionName=5.1.2`，真实网络：

| 步骤 | 结果 |
|---|---|
| `update_last_check_time` = 5 分钟前 → 启动 | 弹窗出现（修复前同条件下无弹窗） |
| 按返回关闭弹窗 | 关闭；`update_ignored_tag` 仍是 `v9.9.9`（未被写成 v5.1.3） |
| 关闭后再启动 | 弹窗再次出现（不再被永久静默） |

`logcat -b crash` 全程为空；`BUILD SUCCESSFUL`。

### 涉及文件

`update/UpdateManager.kt`、`update/UpdateDialog.kt`。

### 遗留

- 启动检查失败仍保持静默（不打扰用户），因此「网络不通」在用户侧依旧表现为不提示；
  这次只修掉了「明明能查却不查 / 关掉就再也不提示」。
- 只在 AVD + 宿主机网络下验证，未覆盖国内网络访问 `api.github.com` 失败的情形。

---

## DBG-017 纯黑主题下悬浮底栏外圈有一条亮描边

**状态**：已修复（未提交）

### 现象

纯黑（AMOLED）主题下，胶囊底栏四周有一圈很淡但可辨的亮线；页面本身是纯黑，这条线把栏「框」了出来。

### 根因

`FloatingBottomBar` 无条件画 1dp 描边（深色主题 8%~12% 白）。纯黑主题页面底色 `#000000`、
胶囊底色 `#171717`，这条描边落在胶囊边缘就成了一圈亮线。灰暗主题（`grey_dark`）里页面与胶囊都是灰面，
描边是需要的，不能一刀切去掉。

### 修复

- `FloatingBars.kt` 新增 `rememberPureBlackTheme()`：`theme == amoled_dark` 或 `background == Color.Black`
  （后者兜住「动态取色」路径——那条路径下 `theme` 会变成 `dynamic`）。
- `floatingBarBorderColor(..., pureBlack)` 纯黑时返回 `Color.Transparent`；
  `FloatingBottomBar` 见到透明色就不再叠 `Modifier.border`，少画一层。

### 验证

设备 `emulator-5554`（1080×2400 @420dpi），取底栏左边缘 `(42,2226)` 与胶囊中部 `(540,2226)`：

| 主题 | 边缘 `(42,2226)` | 胶囊 `(540,2226)` | 结论 |
|---|---|---|---|
| 纯黑 `amoled_dark`（修复后） | `#171717` | `#171717` | 边缘与胶囊同色，无描边 |
| 纯黑（修复前） | `#292929` | `#171717` | 边缘偏亮 = 那条线 |
| 深邃灰 `grey_dark`（未受影响） | `#404040` | `#303030` | 边缘仍亮一档，描边保留 |

`BUILD SUCCESSFUL`，`logcat -b crash` 为空。

### 涉及文件

`ui/widgets/compose/FloatingBars.kt`。

### 遗留

- 只处理了胶囊外描边；样式 A 的**选中滑块**仍有 1dp 主色描边（当作「当前在哪」的强调），
  若纯黑也要求去掉，需要再确认。
- 顶栏不在本次范围。

---

## DBG-018 吧内页滑动时列表被顶栏动画附加下滑加速度并截断底部

**状态**：已修复（`758dc764`）

### 现象

在贴吧吧内页（如 `csgo吧`）上下滚动帖子列表时，顶栏收起/展开瞬间整块列表会额外猛窜一截（体感有明显下滑加速度），且顶栏收起后列表最底端会被裁掉一段空白。

### 复现

`emulator-5554`（1080×2400 @420dpi）打开 `csgo吧` → 向下滚动使顶栏收起 → 观察列表滑动手感并 dump 语义树最大 `bottom` 坐标（仅 `2253px`，比屏幕高度 `2400px` 少 `147px = 56dp`）。

### 定位

1. 读 `ForumPage.kt`（666–738 行）：虽然 `ForumToolbar` 复用了公共 `Toolbar` / `TopAppBarContainer`，但 `ForumPage` 正文外层自己套了 `Box(Modifier.padding(top = barHeight).graphicsLayer { translationY = -(followable * hiddenFraction).toPx() }.nestedScroll(headerNestedScrollConnection))`，把吧头、`ScrollableTabRow` 与整个 `HorizontalPager` 帖子列表都打包在这个 `Box` 内。
2. 对比动态页 `ExplorePage.kt`：动态页把页签直接放进 `Toolbar` 次级槽，`HorizontalPager` 铺满整屏、无外层 `padding(top)` 与 `graphicsLayer`，列表靠 `MyLazyColumn(avoidTopBar = true)` 避让顶栏。

### 根因

1. `ForumPage` 将包含整个 `HorizontalPager` 帖子列表的外层 `Box` 绑定到了顶栏的 `hiddenFraction`（220ms 动画），导致顶栏收起/展开时整块列表被强行叠加 `±147px / 220ms` 绘制位移，体感即滑动加速度。
2. 该外层 `Box` 同时在布局上减去了 `barHeight = 265px`，再向上 `translationY = -147px`，直接把列表可视底边抬高 `147px`，导致屏幕底部截断在 `y = 2253`。
3. `headerNestedScrollConnection.onPreScroll` 消费滚动并逐帧修改 `Box.height(containerHeight)`，且超界时返回整个 `available.copy(x = 0f)`，进一步叠加顿挫。

### 修复

- `ForumPage.kt`：对齐 `ExplorePage` 悬浮顶栏架构——将「最新 / 精华」`ScrollableTabRow` 移入 `ForumToolbar` 次级槽（配色统一取 `ExtendedTheme.colors.onTopBar`，适配彩色顶栏主题）；删除外层 `followingPadding`、`graphicsLayer { translationY }` 及 `headerNestedScrollConnection`，让 `HorizontalPager` 铺满整屏；`LoadingPlaceholder` 同步把假页签移入顶栏次级槽。
- `ForumThreadListPage.kt`：恢复 `MyLazyColumn` 默认 `avoidTopBar = true`；将 `ForumHeader` 作为首个列表 `item(key = "ForumHeader")`、精华页 `GoodClassifyTabs` 作为 `item(key = "GoodClassifyTabs")` 随列表 1:1 滚动，不再在列外被顶栏遮挡。

### 验证

`emulator-5554`（1080×2400 @420dpi），`:app:assembleDebug` `BUILD SUCCESSFUL` 后安装实测 `csgo吧`：

| 检查项 | 修复前 | 修复后 |
|---|---|---|
| 慢速上划 300px（1200ms 消惯性）参照项位移 | 触发顶栏动画附加 `-147px` | `CSGO吧 官方水楼` `y=767 → 487`（`Δy = -280px`，1:1 扣除 20px touchSlop） |
| 慢速回拉 300px（1200ms）参照项位移 | 触发顶栏动画附加 `+147px` | `csgo吧` `y=862 → 1145`（`Δy = +283px`，1:1 严格跟手） |
| 顶栏收起后残留 | `最新`/`精华` 停在 `y=157` | `返回`/`最新`/`精华` 整块滑出屏幕，零残留 |
| 顶栏收起后列表最大 `bottom` | `2253px`（截断 147px） | `2400px`（直达屏底） |
| 精华页签 `GoodClassifyTabs` 落点 | — | `y=667..712`，完整位于顶栏与吧头下方，无遮挡 |
| 崩溃数（`logcat -b crash`） | — | `0` |

### 涉及文件

`ui/page/forum/ForumPage.kt`、`ui/page/forum/threadlist/ForumThreadListPage.kt`。

### 遗留

无。

---

## DBG-019 首页/动态/消息标题左侧头像未与正文左侧对齐

**状态**：已修复（`5409199b`）

### 现象

「首页」（以及共用 `accountNavIconIfCompact()` 的「动态」「消息」）顶栏标题左侧的用户头像偏左贴边，未与下方搜索框、标签、吧名等页面 UI 的左侧对齐。

### 复现

`emulator-5554`（1080×2400 @420dpi）打开首页并 dump 节点 / 截图测像素：
- 搜索框左沿 `x = 42px`（16dp）、吧名「图拉丁」`x = 42px`（16dp）
- 顶栏头像 `长按切换账号`：`[11,144][106,239]`（像素左沿 `x = 11px` = 4.2dp，偏左 12dp）

### 定位

读 `Toolbar.kt:90-195`（`AccountNavIcon`）：函数体顶层先写了 `if (spacer) Spacer(modifier = Modifier.width(12.dp))`，再渲染 `Image` / `LongClickMenu`。

### 根因

迁移到 M3 `TopAppBar`（`fb07d191`）后，`navigationIcon` 槽容器由 M2 的横向 `Row` 变为 M3 的 **`Box`**（自带 `start = 4.dp`）。放在 `Box` 内的同级 `Spacer(12.dp)` 与头像重叠在 `(0, 0)`，完全没有把头像推向右，头像停在 `4.dp = 11px`；同时标题文字与头像仅相距 M3 默认的 `4.dp`（11px）。

### 修复

`Toolbar.kt` 的 `AccountNavIcon` 去掉独立 `Spacer(12.dp)`，改为用外层 `Box(modifier = if (spacer) Modifier.padding(start = 12.dp, end = 8.dp) else Modifier)` 包裹已登录/未登录两分支：
- `spacer = true`（Compact 顶栏：首页 / 动态 / 消息）：`4dp + 12dp = 16dp`，头像左沿对齐正文 `16dp` 基准线，右侧留 `8dp`（叠加 M3 标题 `4dp` 共 `12dp`）避免头像紧贴标题。
- `spacer = false`（大屏 `NavigationRail` / `NavigationDrawer`）：`Modifier` 保持无边距，不受影响。

### 验证

`emulator-5554`（1080×2400 @420dpi）实测：

| 页面 | 修复前头像 bounds | 修复后头像 bounds（像素左沿） | 标题左沿 | 页面 UI 左沿 |
|---|---|---|---|---|
| 首页 | `[11,144][106,239]` | `[43,144][138,239]`（`x = 43px`） | `170px` | 搜索框 `42px` / 吧名 `42px` |
| 动态 | `[11,144][106,239]` | `[43,144][138,239]` | `170px` | 帖子正文 `42px` |
| 消息 | `[11,144][106,239]` | `[43,144][138,239]` | `170px` | — |

`BUILD SUCCESSFUL`，`logcat -b crash` 为空。

### 涉及文件

`ui/widgets/compose/Toolbar.kt`。

### 遗留

无。

---

## DBG-020 切页视差动画导致透明背景页透出前一页内容

**状态**：已修复（`36c29960`）

### 现象

在页面间跳转或返回（如首页 ↔ 吧内页）的转场过程中，新页面上会短暂叠出前一页的画面内容。

### 复现

`emulator-5554`（1080×2400 @420dpi）点击任意贴吧进入吧内页或按返回键，观察 300ms 转场过渡帧。

### 定位

1. Grep `backgroundColor = Color.Transparent`：全站 22 个页面（含 `MainPage`、`ForumPage`、`SettingsPage` 等）统一把背景交给最外层 `TranslucentThemeBackground`，页面自身 `MyScaffold` 为全透明。
2. 读 `MainActivityV2.kt` 的 `NavHost` 转场配置：`exitTransition` / `popEnterTransition` 使用了 25% 视差偏移（`targetOffset = { it / 4 }`）并叠加 `fadeIn` / `fadeOut`。

### 根因

当新页整屏滑入（`100% → 0%`）而旧页只退让 `25%`（`0% → -25%`）时，两页有 `75%` 的重叠期；由于新页背景透明，底层旧页直接透过新页显示，形成叠影。

### 修复

`MainActivityV2.kt` 去掉 25% 视差与淡入淡出，恢复整屏严丝合缝并排滑入/滑出（0 重叠），保留先快后慢减速曲线 `CubicBezierEasing(0.25f, 1f, 0.5f, 1f)`（前推 `320ms`、返回 `280ms`）。

### 验证

`emulator-5554`（540×1200 录屏抽帧），首页 → `csgo吧` → 返回：
- 前推时首页底栏由 `[21, 518]` 干净滑出左沿 `[0, 137]` 至完全消失，无重叠叠影；
- `:app:assembleDebug` `BUILD SUCCESSFUL`，`logcat -b crash` 为 `0`。

### 涉及文件

`MainActivityV2.kt`。

### 遗留

无。

---

## DBG-021 消息页空态箱子图标与文字左偏错位

**状态**：已修复（`810bdff8`）

### 现象

进入「消息」页空列表时，Lottie 空箱子动画与下方「这里什么都没有」文字整体偏左，未对齐屏幕中线。

### 复现

在 `emulator-5554`（1080×2400 @420dpi，中线 x=540px）进入「消息 → 回复我的」空态，抓 `uiautomator dump` 与 `screencap` 计算非纯黑像素包围盒。

### 定位

- `NotificationsListPage.kt:99` 将 `TipScreen(...)` 放进 `MyLazyColumn`（默认 `horizontalAlignment = Alignment.Start`）的 `item` 中且未传 `fillMaxWidth()`。
- `Errors.kt:62` 中 `TipScreen` 外层 `Column` 仅接收调用方传入的 `modifier`（默认 `Modifier`），未自带 `.fillMaxWidth()`；而内层 `Column` 使用 `Modifier.fillMaxWidth(0.9f)`（972px）。

### 根因

`TipScreen` 外层 `Column` 在左对齐的父容器内被子项收缩到 `0.9 × 1080 = 972px` 并贴左（`x = 0..972`），其中心落在 `486px`，相对 `540px` 屏幕中线整体左偏 `54px`。

### 修复

- `Errors.kt`：`TipScreen` 外层 `Column` 增加 `.fillMaxWidth().then(modifier)`，同时给图片容器 `Column` 补 `horizontalAlignment = Alignment.CenterHorizontally`。

### 验证

设备 `emulator-5554`（1080×2400）：
- 修复前：箱子 bbox `(328, 530, 642, 862)` 中心 `485.0px`；文字「这里什么都没有」bounds `[302,969][670,1044]`、像素中心 `485.0px`。
- 修复后：箱子 bbox `(382, 502, 696, 862)` 中心 **`539.0px`**；文字 bounds **`[356,969][724,1044]`**（中点 `540.0px`）、像素中心 **`539.0px`**。

### 涉及文件

- `app/src/main/java/com/huanchengfly/tieba/post/ui/widgets/compose/Errors.kt`

### 遗留

无。

---

## DBG-022 切换页面与列表滑动偶发掉帧

**状态**：已修复（`5a793138`）

### 现象

主界面切换 Tab 与滑动动态信息流时偶发明显掉帧，切页卡顿尤甚。

### 复现

在 `emulator-5554` 执行 5 次动态信息流快速上滑与 6 次主界面 4-Tab 切换，通过 `adb shell dumpsys gfxinfo com.huanchengfly.tieba.post` 采集帧耗时。

### 定位

1. `DataStore.kt:78`：`rememberPreferenceAsState` 含多余的 `LaunchedEffect(state.value) { dataStore.edit { it[key] = state.value } }`，在只读偏好组件首次组合时拿 `defaultValue` 写盘；而 `FeedCard` 与 `FeedListDivider` 在每个列表 item 内部直接调用 `rememberFeedCardStyle()`，列表每滑出一个新卡片就并发写 DataStore 并广播全局 Flow 重组。
2. `Toolbar.kt:419` / `FloatingBars.kt:189` / `NavigationComponents.kt:458,558`：在组合阶段直接解包 `barsState?.hiddenFraction`、`indicatorOffsetPx`、`labelAlpha`，导致动画期间每帧整棵顶栏/底栏子树重组。
3. `LazyLoad.kt:109`：`ProvideShouldLoad(shouldLoad = it == state.currentPage)` 在跨 Tab 动画切页途经中间页时中途拉起中间页的 `LazyLoad`。
4. `EmoticonUtil.kt:122` 与 `FeedCard.kt:276`：`emoticonString` 每次调用在主线程重新 `Pattern.compile` 且 `ThreadContent` 未 `remember`。
5. `Images.kt:243,324`：`NetworkImage` 每张可见图滚动每帧在 `onGloballyPositioned` 写 `mutableStateOf(Offset.Zero)`；`Avatars.kt` 未 `remember` `DisplayRequest`；`UserPage.kt` / `ForumPage.kt` 每次重组从 `assets` 读 `bebas.ttf`。
6. `MainActivityV2.kt:382`：原刷新率代码仅改 `window.attributes.preferredDisplayModeId` 未赋值回 `window.attributes`，实际未向 `WindowManager` 提交。

### 根因

列表与切页热路径叠加了「只读偏好反向写盘广播重组」「动画值在组合期读取」「每帧图片坐标写 Compose State」「跨页途经触发懒加载」「主线程反复编译正则/加载字体」以及「窗口刷新率未提交」多重阻塞。

### 修复

- `DataStore.kt`：移除 `rememberPreferenceAsState` 的写盘 `LaunchedEffect`；修复 `rememberPreferenceAsMutableState` 仅在调用方主动赋值时写盘。
- `BaseComposeActivity.kt` / `FeedCardStyle.kt` / `FloatingBars.kt`：引入 Activity 级 `LocalFeedCardStyle` 与 `LocalFloatingBarStyle`，列表项 O(1) 读取零订阅。
- `Toolbar.kt` / `FloatingBars.kt` / `NavigationComponents.kt`：将 `hiddenFraction` / `indicatorOffsetPx` / `labelAlpha` 推迟至 `graphicsLayer` 绘制层读取；`BarsAutoHide.kt` 不再将 `enabled` 作为重建 state 的 key。
- `LazyLoad.kt`：`ProvideShouldLoad` 改用 `it == state.settledPage`。
- `EmoticonUtil.kt` / `FeedCard.kt`：预编译 `PATTERN_ALL` + 无 `#(` 快速路径，`ThreadContent` 缓存 `AnnotatedString`。
- `Images.kt` / `Avatars.kt` / `UserPage.kt` / `ForumPage.kt`：`NetworkImage` 用非 State 容器存边界且仅 `enablePreview` 注册；缓存 `DisplayRequest` 与 `bebas.ttf` 字体。
- `RefreshRateUtil.kt` / `BaseComposeActivity.kt` / `MainActivityV2.kt` / `CustomSettingsPage.kt`：移除未提交的旧高刷代码，改为完整多刷新率自适应与多档位实时切换（`auto` / `max` / 设备支持的各具体 Hz）。

### 验证

设备 `emulator-5554`（1080×2400，debug 包，`dumpsys gfxinfo` 同脚本复测）：
- **Tab 切换**：Janky frames 从 `16.67%`（legacy `50.00%`）降至 **`4.81%`**（legacy **`6.42%`**）；P50/P90/P95 从 `19ms / 32ms / 57ms` 降至 **`15ms / 18ms / 25ms`**；Missed Vsync 从 `1` 降至 **`0`**；Slow issue draw commands 从 `14` 降至 **`3`**。
- **动态信息流滚动**：P99 从 `400ms` 降至 **`109ms`**；P90/P95 从 `48ms / 77ms` 降至 **`44ms / 65ms`**；Slow bitmap uploads 从 `3` 降至 **`0`**；GPU P50 从 `10ms` 降至 **`5ms`**。
- **刷新率实时提交**：在「设置 → 个性化 → 屏幕刷新率」选「最高刷新率（60 Hz）」后，`dumpsys window windows` 立即出现 `preferredRefreshRate=60.000004 preferredDisplayMode=1`；切回「跟随系统（自适应）」后立即归零。

### 涉及文件

- `app/src/main/java/com/huanchengfly/tieba/post/DataStore.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/arch/BaseComposeActivity.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/MainActivityV2.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/utils/RefreshRateUtil.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/utils/AppPreferencesUtils.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/utils/EmoticonUtil.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/widgets/compose/FeedCardStyle.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/widgets/compose/FeedCard.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/widgets/compose/FloatingBars.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/widgets/compose/Toolbar.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/widgets/compose/BarsAutoHide.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/widgets/compose/LazyLoad.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/widgets/compose/Images.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/widgets/compose/Avatars.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/page/main/NavigationComponents.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/page/main/user/UserPage.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/ForumPage.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/page/settings/custom/CustomSettingsPage.kt`
- `app/src/main/res/values/strings.xml`

### 遗留

- 模拟器仅暴露单档 `60Hz` 模式；90Hz / 120Hz / 144Hz / LTPO 多档切换逻辑已按 `Display.supportedModes`（过滤同物理分辨率 + `alternativeRefreshRates`）适配，真机多高刷屏建议实测确认。

---

## DBG-023 退出帖子返回列表页后自动回到顶端

**状态**：已修复（`810bdff8`）

### 现象


### 复现

在 `emulator-5554` 进入「图拉丁吧」上滑 4 屏 → 记下当前可见帖子 → 点进帖子 → 按返回退出 → 观察吧内页位置直接回到顶部（`LV4 图拉丁P3` / 吧规）。

### 定位

- `arch/Extensions.kt:49` 的 `Flow<T>.collectPartialAsState(prop1, initial)` 用 `produceState(initialValue = initial)` + `.flowOn(Dispatchers.IO)` 收集状态。
- 几乎所有页面的 `viewModel.uiState` 都是 `StateFlow<T>`。页面因跳转帖子被 `NavHost` 移出前台组合后，返回时 `StateFlow.value` 明明仍持有完整的 `forumInfo` 与 `threadList`，但 `collectPartialAsState` 首帧无视 `StateFlow.value` 强行吐出 `initial`（`null` / `persistentListOf()`）。
- 在 `ForumPage` 中，首帧 `forumInfo == null` 触发 `StateScreen(isEmpty = true)` 把 `MyScaffold` + `HorizontalPager` + `ForumThreadListPage` 切换为 `LoadingPlaceholder` 销毁重建；在列表页中，首帧 0 条数据也会让 `LazyListState` 把恢复的 `firstVisibleItemIndex` clamp 成 `0`。
- 另 `MyLazyColumn` / `MyLazyVerticalGrid` 的 `onGlobalEvent<GlobalEvent.ScrollToTop>` 未过滤 `LocalShouldLoad.current`，在 `beyondViewportPageCount = 1` 下双击顶栏会把隔壁隐藏 Tab 一并滚回顶。

### 根因

`collectPartialAsState` 未对 `StateFlow` 同步读取当前值，导致页面从返回栈恢复的首帧被 `initial`（空值）覆盖，摧毁 `StateScreen` 子树并把 `LazyListState` 滚动索引清零。

### 修复

- `arch/Extensions.kt`：`collectPartialAsState` 在上游是 `StateFlow<T>` 时，首帧同步以 `prop1.get(this.value)` 作为 `initialValue`，并直接收集不再绕 `flowOn(Dispatchers.IO)`。
- `ui/widgets/compose/LazyLists.kt`：`ScrollToTop` 全局事件增加 `filter = { LocalShouldLoad.current }`。

### 验证

设备 `emulator-5554`（1080×2400），图拉丁吧上滑 4 屏后进帖再返回：
- 修复前：进帖前停在 `求助。。。请问我这个配置想把显卡升级到4070s`，退出帖子后弹回顶部 `LV4 图拉丁P3` / `【图拉丁吧吧规】`。
- 修复后：进帖前 `sata固态...` `[69,0][1011,221]`、`有没有大哥救一下...` `[69,499][1011,795]`、`小白求助贴 | AM4升级板U` `[69,1076][1011,1312]`；退出帖子后三者坐标完全一致（`[69,0][1011,221]`、`[69,499][1011,795]`、`[69,1076][1011,1312]`），0 像素偏移。

### 涉及文件

- `app/src/main/java/com/huanchengfly/tieba/post/arch/Extensions.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/widgets/compose/LazyLists.kt`

### 遗留

无。

---

## DBG-024 个人主页头像显示慢及帖子刷新与图片加载慢

**状态**：已修复（`18f61267`）

### 现象

1. 进入「个人主页」时 96dp 头像先显示灰色占位圆较长时间才加载出来。
2. 贴吧多图流与帖子详情图片加载慢、逐张排队；帖子列表刷新慢且弱网重复下拉易卡死等待。

### 复现

实测 `http://tb.himg.baidu.com/sys/portrait/item/...` 明文 HTTP 偶发 8s 超时，而 `https://himg.bdimg.com/sys/portrait/item/...` 仅 52ms；另 `portraith` 高清头像约 105KB（普通头像 4.8KB 的 22 倍）且不命中全站普通头像缓存。检查网络层发现 Retrofit 7 个客户端与 Sketch/Glide 均使用 OkHttp 默认 `maxRequestsPerHost = 5` 与 60s 超时。

### 定位

- `StringUtil.kt:150-167`：头像使用明文 `http://tb.himg.baidu.com`（无 HTTP/2 多路复用，易超时）。
- `UserProfilePage.kt:807`：96dp 头像直接拉 `getBigAvatarUrl`，无已缓存普通头像垫底。
- `App.kt:795` / `RetrofitTiebaApi.kt:355-414` / `OkHttpLibraryGlideModule.java:23`：未配置自定义 `Dispatcher`，同域名并发被 OkHttp 锁死在 5；API 超时 60s。
- `PbPageRepository.kt` / `FrsPageRepository.kt` / `PersonalizedRepository.kt` / `PersonalizedViewModel.kt`：多处 `userList.first/find` O(N²) 查找；`FrsPageRepository.kt:50` 每次刷新吧内页还在 Flow 中同步执行 `AppCache.put(..., response.toString())`（巨型 Protobuf 字符串序列化且全仓无人读取）。
- 列表与帖子 ViewModel 刷新均用 `flatMapConcat`，弱网旧请求未结束时新下拉被串行堵塞。

### 根因

头像域名协议陈旧且大图无缓存小图渐进垫底；全站 OkHttp 单域名默认 5 并发导致贴吧同域名多图与 API 严重排队；数据层 O(N²) 扫描、无用同步大字符串写盘与 `flatMapConcat` 串行阻塞拖慢刷新。

### 修复

- `StringUtil.kt`：头像域名升级为 `https://himg.bdimg.com`。
- `Avatars.kt` / `UserProfilePage.kt`：新增 `AvatarWithPreview`，个人主页 96dp 大头像先用已缓存的普通头像 0ms 瞬显垫底，再平滑叠加高清 `portraith` 大图。
- `NetworkConfig.kt` / `App.kt` / `RetrofitTiebaApi.kt` / `OkHttpLibraryGlideModule.java` / `AppPreferencesUtils.kt` / `MoreSettingsPage.kt`：统一管理 API 与图片共享 `Dispatcher`、`ConnectionPool` 与动态超时（默认总并发 64、单域名并发 16、超时 15s），并在「设置 → 更多」提供实时生效的「最大并发请求数」「单域名最大并发数」「网络请求超时」设置项。
- `PbPageRepository.kt` / `FrsPageRepository.kt` / `PersonalizedRepository.kt` / `PersonalizedViewModel.kt`：改用 `HashMap`/`HashSet` O(N) 关联，移除无人读取的 `FrsPageRepository` `AppCache.put(..., response.toString())`。
- `PersonalizedViewModel.kt` / `ForumThreadListViewModel.kt` / `ThreadViewModel.kt` / `ConcernViewModel.kt` / `HotViewModel.kt`：刷新与重载流由 `flatMapConcat` 改为 `flatMapLatest`。

### 验证

- `:app:assembleDebug` 编译通过（`BUILD SUCCESSFUL in 1m 15s`）。
- 设备 `emulator-5554`：进入「个人主页」头像立即清晰呈现（中心采样 1169 种真实 RGB 颜色）；进入「设置 → 更多」完整显示「最大并发请求数（64 个并发）」「单域名最大并发数（16 个并发）」「网络请求超时（15 秒）」。

### 涉及文件

- `app/src/main/java/com/huanchengfly/tieba/post/utils/StringUtil.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/widgets/compose/Avatars.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/page/user/UserProfilePage.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/utils/NetworkConfig.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/App.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/api/retrofit/RetrofitTiebaApi.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/components/glide/OkHttpLibraryGlideModule.java`
- `app/src/main/java/com/huanchengfly/tieba/post/utils/AppPreferencesUtils.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/page/settings/more/MoreSettingsPage.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/repository/PbPageRepository.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/repository/FrsPageRepository.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/repository/PersonalizedRepository.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/page/main/explore/personalized/PersonalizedViewModel.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/threadlist/ForumThreadListViewModel.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/page/thread/ThreadViewModel.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/page/main/explore/concern/ConcernViewModel.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/page/main/explore/hot/HotViewModel.kt`
- `app/src/main/res/values/strings.xml`

### 遗留

无。

---
## DBG-025 0 关注时「关注」页正文全空无任何提示

**状态**：已修复（`18f61267`）

### 现象

当登录账号 0 关注（或关注流返回 0 条动态）时，进入「动态 → 关注」页签，页面只剩顶栏与底栏，中间完全空白且无任何提示或刷新按钮。

### 复现

在 `emulator-5554` 使用 0 关注账号 `G1oow` 切至「动态 → 关注」，`uiautomator dump` 仅含顶栏与底栏节点，正文区无任何元素。

### 定位

- `ConcernPage.kt:65` 直接渲染 `PullRefresh -> LoadMoreLayout -> MyLazyColumn`，未套 `StateScreen` 也没有空态与错误态分支。
- `ConcernViewModel.kt:172` 未保存 `error`，且未过滤无 `threadList` 的不可渲染占位条目（`ConcernPage` 只渲染 `item.threadList != null`）。
- `ConcernPage.kt:81` 列表 `key` 仍为 `${item.recommendType}_${item.threadList?.id}`，与 `ConcernViewModel.distinctById()` 的 `${recommendType}_${postData?.id}_${threadList?.id}` 不一致。

### 根因

`ConcernPage` 缺失空态/加载态/错误态页面承接，且未在状态层过滤不可渲染条目。

### 修复

- `ConcernViewModel.kt`：`ConcernUiState` 增加 `error` 字段；`Refresh`/`LoadMore` 过滤掉 `threadList == null` 的不可渲染项。
- `ConcernPage.kt`：外层接入 `StateScreen`，空态使用 `TipScreen` 展示空箱子 Lottie 动画、「暂无关注动态」标题、「关注感兴趣的用户后，TA 们的动态会显示在这里」说明与「刷新」按钮；补齐错误态 `ErrorScreen`、`LoadMoreLayout` 的 `loadEnd = !hasMore` / `isEmpty` 与对齐的列表 `key`。
- `strings.xml`：新增 `title_concern_empty` / `summary_concern_empty`。

### 验证

设备 `emulator-5554`（1080×2400，中线 540px），0 关注账号进入「动态 → 关注」：
- 修复前：正文区 0 节点，完全空白。
- 修复后：居中展示空箱子动画 + `暂无关注动态` `[383,1307][698,1382]`（中点 `540.5px`）+ `关注感兴趣的用户后，TA 们的动态会显示在这里` `[96,1403][984,1525]`（中点 `540.0px`）+ `刷新` `[503,1567][577,1619]`（中点 `540.0px`）。

### 涉及文件

- `app/src/main/java/com/huanchengfly/tieba/post/ui/page/main/explore/concern/ConcernPage.kt`
- `app/src/main/java/com/huanchengfly/tieba/post/ui/page/main/explore/concern/ConcernViewModel.kt`
- `app/src/main/res/values/strings.xml`

### 遗留

无。


---

## DBG-026 动态页切换「关注 / 推荐 / 热榜」时页面与页签胶囊上下跳动

**状态**：已修复（未提交）

### 现象

在「动态」页切到或切离「推荐」页签（「关注」↔「推荐」↔「热榜」）时，顶栏胶囊页签位置上下跳动约 13px，同时下方列表正文整体跟着上下重排跳动。

### 根因

`ExplorePage.kt` 中 `ExplorePageTab` 的外层 `Box` 未固定高度（仅加了 `.padding(vertical = 6.dp)`），而行尾的「只看已关注的吧」`IconButton`（48dp 高）只在 `showFollowedOnlyToggle`（「推荐」页）时挂载：
- 「推荐」页次级槽高度 = `48dp + 12dp = 60dp`；
- 「关注 / 热榜」页次级槽高度 = `38dp（胶囊）+ 12dp = 50dp`。

切页瞬间顶栏高度在 `60dp` 与 `50dp` 间跳变，触发 `TopAppBarContainer` 高度变化和 `MyScaffold` 重新计算 `LocalBarsContentPadding`，造成胶囊与列表同步上下重排。

### 修复

- `app/src/main/java/com/huanchengfly/tieba/post/ui/page/main/explore/ExplorePage.kt`：将 `ExplorePageTab` 外层 `Box` 改为固定 `.height(50.dp)`（同时包住 48dp `IconButton` 与 38dp 胶囊页签），消除切换页签时的顶栏高度抖动。

### 验证

- `:app:assembleDebug` → `BUILD SUCCESSFUL`
- `emulator-5554`（1080×2400）实测三页切换 UI 树坐标完全一致（0px 偏移）：
  - 推荐页：`关注 [327,307][397,356]`、`推荐 [506,307][576,356]`、`热榜 [685,307][755,356]`
  - 关注页：`关注 [327,307][397,356]`、`推荐 [506,307][576,356]`、`热榜 [685,307][755,356]`
  - 热榜页：`关注 [327,307][397,356]`、`推荐 [506,307][576,356]`、`热榜 [685,307][755,356]`

---

## DBG-027 帖子详情、评论与楼中楼左右边距双重叠加过宽

**状态**：已修复（`e743ab61`）

### 现象

在帖子详情页与楼中楼独立详情页，1 楼主贴正文、2+ 楼评论与楼中楼离屏幕左右边缘过远（视觉上向中心挤窄），与「回复 N」吸顶页签左边界（16dp）明显不对齐。

### 根因

`ThreadPage.kt` 与 `SubPostsPage.kt` 内部均调用了 `FeedCard.kt` 的 `Card(...)`，而该 `Card` 默认自带 `PaddingValues(horizontal = 16.dp)` 与 `padding(vertical = 16.dp)`；外层 `ThreadFloorSurface` 已按样式统一加上了 `16.dp`（通栏/1 楼）或 `12dp + 14dp = 26dp`（浮卡），导致水平边距双重叠加至 `32dp`（84px）乃至 `42dp`（110px）。

### 修复

- `app/src/main/java/com/huanchengfly/tieba/post/ui/page/thread/ThreadPage.kt`：`PostCard` 内去掉多余 `Card(...)` 包装，改由 `Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp))` 承载 `header` 与 `content`，外层边距全部由 `ThreadFloorSurface` 单一负责。
- `app/src/main/java/com/huanchengfly/tieba/post/ui/page/subposts/SubPostsPage.kt`：`SubPostItem` 同步去掉内部多余 `Card(...)` 包装，消除双重内缩。
- `app/src/main/java/com/huanchengfly/tieba/post/ui/widgets/compose/ThreadCommentStyle.kt`：补齐通栏/1 楼竖向留白（`vertical = 12.dp / 13.dp`）。

### 验证

- `:app:assembleDebug` → `BUILD SUCCESSFUL`
- `emulator-5554`（1080×2400）实测 UI 树坐标：
  - 1 楼主贴标题与正文左边界 `x = 42px`（精确 `16dp`，与「回复 26」`x = 42px` 对齐，修复前为 `84px`）
  - 2+ 楼评论作者头像左沿 `x = 42px`，楼中楼文字右沿扩展至 `x = 1006px`（修复前左右各浪费额外 `42px`）

---

## DBG-028 搜索页顶栏遮挡、页签不居中、帖子卡片未走样式及搜吧分组条多余空隙

**状态**：已修复（未提交）

### 现象

1. 「搜索」页的历史记录与联想列表上半部分被悬浮顶栏遮挡。
2. 搜索后「搜吧 / 搜贴 / 搜人」胶囊页签中，「搜贴」文字偏左不居中，且下拉筛选挤在胶囊内观感不佳。
3. 搜索结果「搜贴」列表仍显示旧版默认通栏卡片，未随全局「帖子卡片样式」（柔和浮卡 / 色调胶囊卡 / 极简通栏流 / 原版样式）变化。
4. 「搜吧」（及「搜人」）结果页的「精准匹配 / 相关吧」分组条没有紧贴页签栏，中间多出约 45dp 空白。

### 复现

进入「搜索」页观察历史记录与输入联想；搜索 `csgo` 或无精确匹配词 `tieba123`，依次查看「搜吧」「搜贴」「搜人」三页及排序弹窗。

### 定位

1. `SearchPage.kt` 的 `MyScaffold` 采用悬浮 overlay 顶栏，但历史页 `Column(verticalScroll(...))` 未消费 `paddingValues`，联想列表 `SearchSuggestionList` 使用原生 `LazyColumn` 未避让 `LocalBarsContentPadding`。
2. `SegmentedTabClickMenu` 外层 `ClickMenu` 的 `Box` 未传播最小约束（`propagateMinConstraints = false`），导致等宽页签的 `Constraints.fixed` 丢失且右侧箭头使文字不对称；用户进一步要求将「搜贴」排序筛选移出页签胶囊。
3. `Search.kt` 的 `SearchThreadList` / `SearchThreadItem` 硬编码 `VerticalDivider` 与老版 `Card(...)`，未接入 `FeedCardStyle`（`StyledFeedCardBody` / `FeedListDivider`）。
4. `SearchForumPage.kt` 与 `SearchUserPage.kt` 的 `MyLazyColumn` 已按完整顶栏高度设置 `contentPadding.top`，但 `stickyHeader` 内部又叠加 `.padding(top = topBarInsetsHeight())`，造成顶部静止时多出 1 个状态栏高度（45dp / 118px）空白。

### 修复

- `app/src/main/java/com/huanchengfly/tieba/post/ui/page/search/SearchPage.kt`：
  - 搜索历史 `Column` 在 `verticalScroll` 之后应用 `.padding(paddingValues)`，`SearchSuggestionList` 改为 `MyLazyColumn(Modifier.fillMaxSize())`，并在回到空关键词时复位 `LocalBarsAutoHideState`。
  - `SearchTabRow` 固定 50dp 高度；三个页签统一使用纯文字 `SegmentedTab`，将「搜贴」排序筛选移至页签行最左侧独立 `Sort` 图标按钮（仅「搜贴」页显示，点击弹出原 `ListSinglePicker` 排序菜单）。
- `app/src/main/java/com/huanchengfly/tieba/post/ui/widgets/compose/Menu.kt` & `Tabs.kt`：
  - 为 `ClickMenu` 补齐可选 `propagateMinConstraints` 参数，`SegmentedTabClickMenu` 开启约束传播并采用左右等宽对称占位（惠及吧内页「最新▾」等调用点）。
- `app/src/main/java/com/huanchengfly/tieba/post/ui/widgets/compose/FeedCard.kt` & `Search.kt`：
  - 将 `StyledFeedCardBody` 开放为 `internal`（接受 `timeString: String`），`SearchThreadItem` 与 `SearchThreadList` 接入 `rememberFeedCardStyle()` 与 `FeedListDivider`（同时覆盖全局搜贴与吧内搜贴）。
- `app/src/main/java/com/huanchengfly/tieba/post/ui/widgets/compose/Search.kt`、`SearchForumPage.kt`、`SearchUserPage.kt`：
  - 移除 `stickyHeader` 内部的 `.padding(top = topBarInsetsHeight())`，新增 `Modifier.searchStickyHeaderFollowTopBar` 在绘制层随顶栏展开比例平滑定位，且两级吸顶条交接时前一条平滑让位淡出。
- `app/src/main/res/values/strings.xml`：新增 `desc_search_sort` 无障碍文案。

### 验证

- `:app:assembleDebug` → `BUILD SUCCESSFUL`（`debug-5.1.8(501008).apk`）。
- `emulator-5554`（Pixel 7a，1080×2400 @420dpi，纯黑主题 + 柔和浮卡样式）实测 UI 树与像素：
  - **搜索历史 / 联想**：顶栏底边 `y = 286px`；「搜索历史」`[42,328][891,389]`、首条联想 `csgologo` `[42,307][105,370]`，0px 遮挡。
  - **三页签居中与独立排序按钮**：切换三页期间坐标恒定无跳动——「搜吧」`[318,328][386,377]`、「搜贴」`[507,328][575,377]`（水平间距均精确 `189px`，文字中心 `x = 541px`）、「搜人」`[696,328][764,377]`；仅在「搜贴」页左侧显示排序按钮 `[45,323][103,381]`，点击在左侧正常弹出「新贴在前 / 旧贴在前 / 相关度」菜单。
  - **搜贴卡片样式**：卡片外边缘 `x = 10` 为 `#000000` 纯黑底，卡片内 `x = 40 / 950` 为 `#0D0D0D` 柔和浮卡底色（`y = 480 / 520 / 600` 采样一致）。
  - **搜吧 / 搜人分组条**：无精确匹配时「相关吧」位于 `[84,449][177,494]`（有精确匹配时「推荐」同为 `[84,449][146,494]`），紧贴页签栏底边（原多 118px 掉至 `y ≈ 568px`）。

### 涉及文件

`SearchPage.kt`、`SearchForumPage.kt`、`SearchUserPage.kt`、`Search.kt`、`FeedCard.kt`、`Menu.kt`、`Tabs.kt`、`strings.xml`。

### 遗留

无。

---
## 附录 A：调试环境与手法

这几轮踩过的坑，复用时按此执行可少走弯路。

### A.1 「怎么改了没效果」先怀疑包

- `adb shell dumpsys package com.huanchengfly.tieba.post | Select-String lastUpdateTime` 与本地 APK 时间戳对照。
- 更硬的校验：`pm path` 取设备上的 APK 拉回本地，在 `classes*.dex` 里搜特征字符串（如 `PullRefresh` / `pinCount`）。DBG-001 一开始就是因为在旧包上测，得出的「修了也没用」是假的。
- Live Edit 只用于本地看效果，验收必须走重新构建安装。

### A.2 别让管道杀掉构建

`gradle ... 2>&1 | Select-String ... | Select-Object -First N` 会在取够行数后关闭管道，PowerShell 提前终止上游进程 —— 构建被打断但 `$LASTEXITCODE` 看起来正常，随后 `install` 装的是上一次的旧 APK。**改为重定向到日志文件再筛选**：

```powershell
& $gradle --no-daemon :app:assembleDebug --console=plain *> $env:PI_SCRATCH_DIR\build.log
Select-String -Path $env:PI_SCRATCH_DIR\build.log -Pattern 'BUILD SUCCESSFUL|BUILD FAILED|^e:'
```

### A.3 手势与像素判定

1. 先取两帧一致基线（间隔 0.9s 各截一次，MD5 相同才继续），避免动画未停导致误判。
2. 下拉用 `input motionevent DOWN/MOVE.../UP` 分段注入，比 `input swipe` 更容易在途中截图。
3. 判定「有没有反馈」不能只看 MD5，要看**变化包围盒落在哪**：逐像素按阈值（RGB 差之和 > 6）求 `minX/minY/maxX/maxY`，再与栏底坐标（80dp=210px、128dp=336px）比较。
4. 找「空白块」用逐行统计：每行采样非底色像素数，连续 15 行以上为 0 即为空白带。

### A.4 语义树定位 Compose 元素

`adb shell uiautomator dump` + 正则提 `text=` / `content-desc=` / `bounds` 即可量出栏高、页签位置与间隙高度；`Icon` 的 `contentDescription`、未登录头像的 `contentDesc` 都能拿到，可直接验证「按钮在不在、在哪」。注意 `NAF="true"` 的节点可能没有 text，要用 content-desc 匹配。

### A.5 adb 偶发断连

`cannot connect to daemon at tcp:5037` 时先 `adb start-server`，再重跑同一批命令；不要基于断连期间的截图下结论。
