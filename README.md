<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_new_round.png" alt="TiebaLite Logo" width="96" height="96">

# 贴吧 Lite · TiebaLite

**轻量化、无广告的第三方百度贴吧 Android 客户端**

基于 Kotlin 与 Jetpack Compose 构建，采用 Material 3 悬浮栏交互与自适应大屏布局。

[![Latest Release](https://img.shields.io/github/v/release/G1oow/TiebaLite?style=flat-square&color=2563eb)](https://github.com/G1oow/TiebaLite/releases/latest)
[![Build Status](https://img.shields.io/github/actions/workflow/status/G1oow/TiebaLite/build.yml?branch=main&style=flat-square)](https://github.com/G1oow/TiebaLite/actions/workflows/build.yml)
[![Android API](https://img.shields.io/badge/API-24%2B-3ddc84?style=flat-square)](app/build.gradle.kts)
[![Target Android](https://img.shields.io/badge/Android-16%20Target-1a73e8?style=flat-square)](app/build.gradle.kts)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue?style=flat-square)](LICENSE)

[核心特性](#核心特性) • [安装与使用](#安装与使用) • [技术栈与架构](#技术栈与架构) • [本地构建](#本地构建) • [相关文档](#相关文档与致谢)

</div>

> [!IMPORTANT]
> 本软件为**非官方**百度贴吧客户端，与百度公司无任何关联。源码及构建产物仅供学习与技术交流使用，严禁用于商业用途；使用本软件产生的风险与后果由使用者自行承担。

---

## 核心特性

### 界面与交互
- **Material 3 与悬浮栏架构**：顶栏与胶囊式悬浮底栏均以 overlay 浮于内容之上，正文铺满整屏；向下滑动时整块平移动画收起、不消费滚动手势，支持在设置中切换「阴影层次 / 色调分层」底栏样式或关闭自动隐藏。
- **自适应响应式布局**：基于 WindowSizeClass 区分 Compact / Medium / Expanded 窗口尺寸，手机端使用悬浮胶囊底栏，平板与折叠屏自动切换为 `NavigationRail` 或常驻抽屉，正文与弹窗居中收窄，支持全面屏与刘海 / 挖孔安全区避让。
- **个性化定制**：支持跟随系统深浅色、纯黑 AMOLED 暗色与多套主题配色、自定义字体大小、三种内容密度（紧凑 / 标准 / 舒适）、自定义启动页与桌面图标切换。

### 浏览与社区体验
- **纯净无广告**：无开屏广告、无信息流推广、无追踪埋点，仅与贴吧服务及 GitHub Release 更新接口通信。
- **全功能看帖**：支持只看楼主、倒序浏览、楼中楼带图评论完整渲染、收藏页浏览偏好继承、列表滚动位置记忆以及「只看已关注的吧」动态过滤。
- **草稿箱与回帖**：回帖与楼中楼回复自动落盘保存，草稿箱展示吧名、内容预览与时间，点击可直达原帖对应楼层。
- **媒体与离线体验**：Media3 ExoPlayer 驱动视频播放（支持 `http → https` 自动升级），看图器支持缩放与跨页多选批量保存；弱网或断网时支持读取离线缓存列表。

### 签到、账号与排障
- **全量一键签到与定时自动签到**：分页拉取全部关注贴吧（支持 3000+ 吧全覆盖）并在通知栏实时展示进度；定时签到基于精准闹钟在 Doze 休眠态下按时唤醒并自动预约次日任务。
- **多账号与屏蔽管理**：支持多账号快速切换、关键词与用户屏蔽管理。
- **应用内更新与日志面板**：内置 GitHub Releases 更新检查与直链下载；关于页提供实时应用日志查看与一键复制，便于提交问题时附带诊断上下文。

---

## 安装与使用

### 下载安装

前往 [GitHub Releases](https://github.com/G1oow/TiebaLite/releases/latest) 下载最新正式版 APK 安装。

- **系统要求**：Android 7.0（API 24）及以上，支持 `arm64-v8a` 与 `armeabi-v7a` 架构，已适配 Android 16（API 36）。
- **无缝覆盖升级**：所有正式 Release 均使用固定发布签名（证书 SHA-256 指纹在构建期与产物阶段双重校验），升级新版本可直接覆盖安装，无需卸载旧版。
- **版本记录**：各版本变更详情请查看 [AI_CHANGELOG.md](AI_CHANGELOG.md)。

### 使用建议

> [!TIP]
> **全量签到与定时签到**：系统后台限制可能导致长耗时签到被中途终止。建议在系统设置中将「贴吧 Lite」的电池优化策略设为**不限制**（允许后台运行）。

> [!NOTE]
> **反馈 Bug**：提交 [GitHub Issue](https://github.com/G1oow/TiebaLite/issues) 时，可在应用内进入 **设置 → 关于 → 查看日志**，复制关键操作日志一并附上，有助于快速定位问题。

---

## 技术栈与架构

| 维度 | 选型 |
| --- | --- |
| 语言与构建 | Kotlin 2.4.21 · Gradle 9.9.2 · AGP 9.5.2 · Version Catalog |
| UI 框架 | Jetpack Compose（BOM `2026.11.01`）· Material 3（增量迁移中） |
| 架构模式 | MVVM（`BaseViewModel` + `UiState / UiIntent / UiEvent` + `GlobalEventFlow`） |
| 依赖注入 | Hilt 2.60.1 + KSP |
| 网络与协议 | Retrofit 3 · OkHttp 5 · Square Wire 7（Protobuf）· Kotlinx Serialization |
| 媒体与图片 | AndroidX Media3 ExoPlayer · Sketch 3 · ZoomImage · Glide 5 |
| 本地存储 | Jetpack DataStore Preferences · LitePal |

### 主要目录结构

```text
TiebaLite/
├── app/
│   ├── build.gradle.kts              # 应用构建配置与固定发布签名校验任务
│   └── src/main/
│       ├── protos/                   # 贴吧客户端 Wire Protobuf 定义
│       └── java/com/huanchengfly/tieba/post/
│           ├── api/                  # 贴吧网络接口与 Retrofit / Wire 封装
│           ├── arch/                 # Activity / ViewModel 基类与全局事件总线
│           ├── services/             # 一键签到前台服务等后台组件
│           ├── ui/
│           │   ├── common/           # 主题桥接、偏好设置框架、WindowSizeClass 自适应工具
│           │   ├── page/             # 主界面、贴吧、帖子、子楼、搜索、设置等页面
│           │   └── widgets/compose/  # MyScaffold、悬浮顶底栏、自动隐藏与列表封装
│           ├── update/               # GitHub Releases 更新检查与下载器
│           └── utils/                # 账号、主题、图片与 AppPreferencesUtils
├── docs/plan-m3-migration.md         # Material 3 迁移阶段与门禁台账
├── gradle/libs.versions.toml         # 依赖版本清单
├── application.properties            # versionCode / versionName 配置
├── AGENTS.md                         # 开发、架构与发版规范
└── debuglog.md                       # 历史缺陷定位与验证记录
```

---

## 本地构建

### 环境要求

- **JDK 17+**（可使用 Android Studio 内置 JBR）
- **Android SDK**：`compileSdk = 37.1`、`targetSdk = 36`、`minSdk = 24`
- 推荐使用项目自带的 Gradle Wrapper（`./gradlew` / `.\gradlew.bat`，Gradle 9.9.2）

### 构建步骤

1. 克隆仓库并在根目录准备 `local.properties`（配置 `sdk.dir` 指向本地 Android SDK 目录，或设置 `ANDROID_HOME` / `ANDROID_SDK_ROOT` 环境变量）。
2. 执行构建或测试命令：

```bash
# 快速校验 Kotlin 编译
./gradlew :app:compileDebugKotlin

# 构建 Debug APK
./gradlew :app:assembleDebug

# 运行本地 JVM 单元测试
./gradlew :app:testDebugUnitTest
```

3. 构建 Release APK：

Release 包固定使用仓库内 `app/tblite_debug.jks`（alias `tiebalite`）签名，且 `:app:assembleRelease` 会先运行 `:app:checkReleaseSigning` 校验证书 SHA-256 指纹。构建前需通过 `keystore.properties` 或环境变量 `TBLITE_KEYSTORE_PASSWORD`、`TBLITE_KEY_PASSWORD` 提供签名口令（配置方式见 [app/build.gradle.kts](app/build.gradle.kts) 与 [AGENTS.md](AGENTS.md)）：

```bash
# 校验发布签名配置
./gradlew :app:checkReleaseSigning

# 构建 Release APK
./gradlew :app:assembleRelease
```

构建产物输出在 `app/build/outputs/apk/` 目录。

> [!WARNING]
> **Windows 构建提示**：由于 Glide 5 的 KSP2 处理器需要计算相对路径，`GRADLE_USER_HOME` 必须与本仓库位于**同一盘符**，否则 `:app:kspDebugKotlin` 会因跨盘符路径报错。请勿将 `local.properties` 或 `keystore.properties` 提交到版本控制。

---

## 相关文档与致谢

### 仓库文档

- [AGENTS.md](AGENTS.md) — 项目架构约定、Material 3 / 悬浮栏规范、响应式准则与发版流程
- [docs/plan-m3-migration.md](docs/plan-m3-migration.md) — Material 3 迁移执行台账与验收门禁
- [AI_CHANGELOG.md](AI_CHANGELOG.md) — 面向用户的版本发布说明
- [debuglog.md](debuglog.md) — 已修复缺陷的现象、根因与回归验证记录
- [CREDITS.md](CREDITS.md) — 原作者与第三方开源库致谢名单

### 致谢与参考

- 本项目源自 **[HuanCheng65](https://github.com/HuanCheng65)** 原创并维护至 2024 年的 TiebaLite 项目，全部原始架构与核心工作归原作者所有，特此致敬。
- 相关生态项目：[Starry-OvO/aiotieba](https://github.com/Starry-OvO/aiotieba) · [n0099/tbclient.protobuf](https://github.com/n0099/tbclient.protobuf)
