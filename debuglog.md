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

### 验证

| 项 | 值 |
|---|---|
| 构建 | `:app:compileDebugKotlin`、`:app:assembleDebug` 均 `BUILD SUCCESSFUL`；`adb install -r` `Success`（`lastUpdateTime` 与 APK 一致） |
| 修前（按住下滑 60px） | 同屏节点统一 **+187px** = 滚动 ≈40 + 凭空 **147px**（147px = 56dp 标题行） |
| 修后（同一手法） | 「回复 55」+39px、「全部」+44px = 手指 60px − touch slop，**无额外位移** |
| 吸顶页签位置 | 栏收起 / 展开下均为 `y=84`（状态栏 63 + 8dp 21），不随栏动画变 |

### 涉及文件

`ThreadPage.kt`。

### 遗留

同款写法仍在 `SubPostsPage`、`SearchUserPage`、`SearchForumPage`、`ForumSearchPostPage`、`HistoryListPage`（吸顶页签）与 `ForumPage`（页头 `followingPadding`）——同样把栏动画兑成布局高度，本次未改，待定是否一并按方案 B 处理。

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
