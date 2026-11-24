pluginManagement {
    repositories {
        // 只用官方源（本机与 CI 均走代理），不再使用国内镜像：
        // 镜像对新版插件常常只有元数据、没有 jar，命中后不会回落到其它源
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        // LitePal-Kotlin 与 immersionbar 只在该仓库发布，官方源上没有（必须保留）
        maven("https://verve.jfrog.io/artifactory/verve-gradle-release/")
        // Android_CN_OAID 的传递依赖（华为/荣耀广告标识）只在这些厂商官方仓库发布
        maven("https://developer.huawei.com/repo/")
        maven("https://developer.hihonor.com/repo/")
    }
}

rootProject.name = "TiebaLite"
include(":app")
