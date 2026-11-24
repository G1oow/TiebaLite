import com.android.build.gradle.internal.api.BaseVariantOutputImpl

import java.security.KeyStore
import java.security.MessageDigest
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.com.squareup.wire)
}

// Hilt 聚合任务 hiltJavaCompileDebug 的 provider 在 Codeberg runner 下无值
// ("Cannot query the value of this provider", 任务图解析失败), 关闭聚合任务规避
hilt {
    enableAggregatingTask = false
}

val sha: String? = System.getenv("GITHUB_SHA")
val isCI: String? = System.getenv("CI")
val isSelfBuild = isCI.isNullOrEmpty() || !isCI.equals("true", ignoreCase = true)
// 版本号与签名口令原先由 SweetProperty 生成；该插件已停更、新版要求 JDK 21，
// 这里直接读 application.properties / keystore.properties
fun loadProperties(fileName: String): Properties = Properties().apply {
    val file = rootDir.resolve(fileName)
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val appProperties = loadProperties("application.properties")
val keystoreProperties = loadProperties("keystore.properties")
val applicationVersionCode = appProperties.getProperty("versionCode").toInt()
var applicationVersionName = appProperties.getProperty("versionName")
val isPerVersion = appProperties.getProperty("isPreRelease").toBoolean()
if (isPerVersion) {
    applicationVersionName +=
        "-${appProperties.getProperty("preReleaseName")}.${appProperties.getProperty("preReleaseVer")}"
}
if (!isSelfBuild && !sha.isNullOrEmpty()) {
    applicationVersionName += "+${sha.substring(0, 7)}"
}

// ==== 正式发布版签名（固定） ====
// 历史发布包全部由仓库内 app/tblite_debug.jks（PKCS12，alias tiebalite）签名。
// 换 key 会让老用户无法覆盖升级，因此把「发布签名」固定成这一份 keystore：
//   - storeFile / keyAlias 写死，任何机器、任何 CI 都指向同一把 key；
//   - 口令优先取 keystore.properties，其次取环境变量
//     TBLITE_KEYSTORE_PASSWORD / TBLITE_KEY_PASSWORD；
//   - **绝不回落到本机 debug.keystore**——debug key 每台机器随机生成，
//     正是历史上「同版本号不同签名」的根源。release 构建在签名缺失 / 指纹不符时直接失败
//     （见 checkReleaseSigning 任务）。
val pinnedKeystore = rootDir.resolve("app/tblite_debug.jks")
val pinnedKeyAlias = "tiebalite"
val pinnedStorePassword: String? = run {
    val propertiesFile = rootDir.resolve("keystore.properties")
    if (propertiesFile.exists() && keystoreProperties.getProperty("keystore.file", "").isNotBlank()) {
        keystoreProperties.getProperty("keystore.password")
    } else {
        System.getenv("TBLITE_KEYSTORE_PASSWORD")
    }
}
val pinnedKeyPassword: String? = run {
    val propertiesFile = rootDir.resolve("keystore.properties")
    if (propertiesFile.exists() && keystoreProperties.getProperty("keystore.file", "").isNotBlank()) {
        keystoreProperties.getProperty("keystore.key.password")
    } else {
        System.getenv("TBLITE_KEY_PASSWORD")
    }
}

/** 发布签名证书的 SHA-256 指纹（app/tblite_debug.jks）。改它就等于换签名，必须同步更新。 */
val pinnedReleaseCertSha256 =
    "F7E02F10C7C9E83B943F8DEEE786B270330416C710CF5CFBCE73027AD19DA759"

wire {
    sourcePath {
        srcDir("src/main/protos")
    }

    kotlin {
        android = true
    }
}

android {
    // 2026 版 AndroidX（hilt-navigation-compose 1.4、compose 1.13 等）要求 compileSdk >= 37.1
    // AGP 9 用 compileSdk + compileSdkMinor 表达 37.1（旧 DSL 的 compileSdk 只接受网络版本号 Int）
    compileSdk = 37
    compileSdkMinor = 1
    defaultConfig {
        // 2026 版 AndroidX（navigation 2.10 / compose 1.13 / datastore）已把 minSdk 抬到 24
        minSdk = 24
        //noinspection OldTargetApi
        targetSdk = 36
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
        versionCode = applicationVersionCode
        versionName = applicationVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        manifestPlaceholders["is_self_build"] = "$isSelfBuild"
    }
    buildFeatures {
        compose = true
    }
    composeCompiler {
        // 2.4 起 strong skipping 已默认开启，该 flag 本身被标记为错误，这里不再显式设置
        includeSourceInformation = false
        // 原先把这两个参数塞进 kotlinOptions 的 freeCompilerArgs（-P plugin:...），2.4 已禁用该 DSL
        // 指标 / 报告是诊断产物，默认关闭。
        // 原因：AGP 9 传给 Compose 插件的 moduleName 形如 "TiebaLite:app_debug"（带冒号），插件把它和
        // 文件名拼成 ".../compose_metrics/debug/TiebaLite:app_debug-module.json"。Windows 把冒号当
        // 「文件名:NTFS 数据流」分隔符：轻则指标被塞进 0 字节 TiebaLite 文件的 ADS（等于没产出），
        // 重则 createNewFile 抛 IOException: Invalid file path，整个 Kotlin 编译器崩掉
        // （IrGenerationExtensionException）。需要时用 -PenableComposeMetrics=true 打开。
        if (project.providers.gradleProperty("enableComposeMetrics").isPresent) {
            metricsDestination = layout.buildDirectory.dir("compose_metrics")
            reportsDestination = layout.buildDirectory.dir("compose_metrics")
        }
        // compose_stability_configuration.txt 被本地 gitignore，CI checkout 后缺失；
        // 文件不存在时不要设置，避免插件拿到不存在的路径
        if (rootDir.resolve("compose_stability_configuration.txt").exists()) {
            stabilityConfigurationFiles.set(
                listOf(
                    rootProject.layout.projectDirectory.file("compose_stability_configuration.txt")
                )
            )
        }
    }
    // ==== 正式发布版签名（固定） ====
    // 声明见文件开头「正式发布版签名」区块；此处只把解析结果接进 AGP。

    signingConfigs {
        if (pinnedKeystore.exists()) {
            create("config") {
                storeFile = pinnedKeystore
                storePassword = pinnedStorePassword
                keyAlias = pinnedKeyAlias
                keyPassword = pinnedKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }
    buildFeatures {
        buildConfig = true
    }

    // 本地 JVM 单元测试：org.json 等 android.jar stub 默认抛异常，放行返回默认值
    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    defaultConfig {
        buildConfigField("String", "SIGN_MAINTAINER", "\"qaqmin\"")
        buildConfigField("String", "SIGN_REPO", "\"https://github.com/qaqmin/TiebaLite\"")
        buildConfigField("String", "SIGN_PROFILE", "\"https://github.com/qaqmin\"")
    }

    // release 必须用固定签名：缺 keystore/口令时由 checkReleaseSigning 任务在构建期直接失败，
    // 而不是像以前那样悄悄回落到 debug key（那会让每个构建机产出不同签名的同名包）。
    // debug 保持 Android 默认 debug 签名，便于本地与其他应用共存。
    buildTypes {
        all {
            if (name == "release") {
                signingConfig = signingConfigs.findByName("config")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
            isJniDebuggable = false
            multiDexEnabled = true
            // 应用内更新 mock 源：release 恒为空（mock 地址绝不进 release 路径）
            buildConfigField("String", "MOCK_UPDATE_API_ROOT", "\"\"")
        }
        debug {
            // 本地端到端验收用：adb reverse 后指向 python -m http.server 的 mock 源
            buildConfigField("String", "MOCK_UPDATE_API_ROOT", "\"http://localhost:8000/\"")
        }
    }
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_11
        sourceCompatibility = JavaVersion.VERSION_11
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "DebugProbesKt.bin"
        }
    }
    namespace = "com.huanchengfly.tieba.post"
    applicationVariants.configureEach {
        val variant = this
        outputs.configureEach {
            val fileName =
                "${variant.buildType.name}-${applicationVersionName}(${applicationVersionCode}).apk"

            (this as BaseVariantOutputImpl).outputFileName = fileName
        }
        kotlin.sourceSets {
            getByName(variant.name) {
                kotlin.srcDir("build/generated/ksp/${variant.name}/kotlin")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

// ==== 正式发布版签名校验 ====
// 把「keystore SHA-256 指纹」当成发布契约来守：
//   - assembleRelease / bundleRelease 前置依赖 checkReleaseSigning；
//   - keystore 缺失 / 口令缺失 / 证书指纹不符 → 构建直接失败；
//   - 指纹不符时报错打印实际值与期望值，便于确认是换 key 还是环境串了。

tasks.register("checkReleaseSigning") {
    group = "verification"
    description = "校验正式发布版签名 keystore 存在且证书指纹符合发布契约"

    val keystoreFile = pinnedKeystore
    val storePasswordProvider = pinnedStorePassword
    val keyAliasProvider = pinnedKeyAlias
    val expectedSha256 = pinnedReleaseCertSha256

    doLast {
        if (!keystoreFile.exists()) {
            throw GradleException(
                "正式发布版签名缺失：${keystoreFile.path} 不存在。" +
                    "该 keystore 是历史发布包的签名，必须随仓库分发；" +
                    "请恢复文件，或改用 keystore.properties 指定替代 keystore（并同步更新指纹契约）。"
            )
        }
        if (storePasswordProvider.isNullOrEmpty()) {
            throw GradleException(
                "正式发布版签名口令缺失：请提供 keystore.properties" +
                    "（keystore.password / keystore.key.password），" +
                    "或设置环境变量 TBLITE_KEYSTORE_PASSWORD / TBLITE_KEY_PASSWORD。"
            )
        }
        val store = KeyStore.getInstance(keystoreFile, storePasswordProvider.toCharArray())
        keystoreFile.inputStream().use { input ->
            store.load(input, storePasswordProvider.toCharArray())
        }
        val certificate = store.getCertificate(keyAliasProvider)
            ?: throw GradleException("签名 keystore 中不存在 alias「$keyAliasProvider」。")
        val actualSha256 = MessageDigest.getInstance("SHA-256")
            .digest(certificate.encoded)
            .joinToString("") { "%02X".format(it) }
        if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
            throw GradleException(
                "正式发布版签名证书指纹不匹配，构建已中止。\n" +
                    "  期望: $expectedSha256\n" +
                    "  实际: $actualSha256\n" +
                    "若这是有意换 key，请同步更新 app/build.gradle.kts 中的 pinnedReleaseCertSha256，" +
                    "并知悉：已安装的老用户将无法覆盖升级。"
            )
        }
        logger.lifecycle("Release signing OK: SHA-256 = $actualSha256")
    }
}

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn("checkReleaseSigning")
}

dependencies {
    //Local Files
//    implementation fileTree(include: ["*.jar"], dir: "libs")

    implementation(libs.net.swiftzer.semver.semver)
    implementation(libs.godaddy.color.picker)

    implementation(libs.airbnb.lottie)
    implementation(libs.airbnb.lottie.compose)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui.compose)

    implementation(libs.androidx.navigation.compose)

    api(libs.wire.runtime)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.accompanist.drawablepainter)
    implementation(libs.accompanist.insets.ui)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.placeholder.material)
    implementation(libs.accompanist.navigation.material)

    implementation(libs.sketch.core)
    implementation(libs.sketch.compose)
    implementation(libs.sketch.ext.compose)
    implementation(libs.sketch.gif)
    implementation(libs.sketch.okhttp)

    implementation(libs.zoomimage.compose.sketch)

    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    runtimeOnly(libs.compose.runtime.tracing)
    implementation(libs.compose.animation)
    implementation(libs.compose.animation.graphics)
    implementation(libs.compose.material)
    implementation(libs.compose.material.icons.core)
    // Optional - Add full set of material icons
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.material3)
    // Android Studio Preview support
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // UI Tests
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugRuntimeOnly(libs.compose.ui.test.manifest)

    implementation(libs.androidx.constraintlayout.compose)

    implementation(libs.github.oaid)

    implementation(libs.org.jetbrains.annotations)

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    //AndroidX
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.window)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.swiperefreshlayout)

    //Test
    testImplementation(libs.junit.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestRuntimeOnly(libs.androidx.test.runner)

    //Glide
    implementation(libs.glide.core)
    ksp(libs.glide.ksp)
    implementation(libs.glide.okhttp3.integration)

    implementation(libs.google.material)

    implementation(libs.okhttp3.core)
    implementation(libs.retrofit2.core)
    implementation(libs.retrofit2.converter.wire)

    implementation(libs.google.gson)
    implementation(libs.org.litepal.android.kotlin)
    implementation(libs.com.jaredrummler.colorpicker)

    implementation(libs.github.matisse)
    implementation(libs.xx.permissions)
    implementation(libs.com.gyf.immersionbar.immersionbar)

    implementation(libs.com.github.yalantis.ucrop)

    implementation(libs.appcenter.analytics)
    implementation(libs.appcenter.crashes)
    implementation(libs.appcenter.distribute)
}
