import com.android.build.gradle.internal.api.BaseVariantOutputImpl

import java.security.KeyStore
import java.security.MessageDigest
plugins {
    autowire(libs.plugins.com.android.application)
    autowire(libs.plugins.kotlin.android)
    autowire(libs.plugins.kotlin.serialization)
    autowire(libs.plugins.kotlin.parcelize)
    autowire(libs.plugins.kotlin.compose.compiler)
    autowire(libs.plugins.hilt.android)
    autowire(libs.plugins.kotlin.ksp)
    autowire(libs.plugins.com.squareup.wire)
}

// Hilt 聚合任务 hiltJavaCompileDebug 的 provider 在 Codeberg runner 下无值
// ("Cannot query the value of this provider", 任务图解析失败), 关闭聚合任务规避
hilt {
    enableAggregatingTask = false
}

val sha: String? = System.getenv("GITHUB_SHA")
val isCI: String? = System.getenv("CI")
val isSelfBuild = isCI.isNullOrEmpty() || !isCI.equals("true", ignoreCase = true)
val applicationVersionCode = property.versionCode
var applicationVersionName = property.versionName
val isPerVersion = property.isPreRelease
if (isPerVersion) {
    applicationVersionName += "-${property.preReleaseName}.${property.preReleaseVer}"
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
    if (propertiesFile.exists() && property.keystore.file.isNotBlank()) {
        property.keystore.password
    } else {
        System.getenv("TBLITE_KEYSTORE_PASSWORD")
    }
}
val pinnedKeyPassword: String? = run {
    val propertiesFile = rootDir.resolve("keystore.properties")
    if (propertiesFile.exists() && property.keystore.file.isNotBlank()) {
        property.keystore.key.password
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
    compileSdk = 36
    defaultConfig {
        applicationId = "com.huanchengfly.tieba.post"
        minSdk = 21
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
        enableStrongSkippingMode = true
        includeSourceInformation = false
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
    kotlinOptions {
        jvmTarget = "11"
        val composeArgs = listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" + project.buildDir.absolutePath + "/compose_metrics",
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" + project.buildDir.absolutePath + "/compose_metrics"
        )
        // compose_stability_configuration.txt 被本地 gitignore, CI checkout 后缺失;
        // 缺文件时插件 Provider 无值 -> "Cannot query the value of this provider" (09-16 CI 全红根因之二)
        freeCompilerArgs += if (rootDir.resolve("compose_stability_configuration.txt").exists())
            composeArgs + listOf(
                "-P", "plugin:androidx.compose.compiler.plugins.kotlin:stabilityConfigurationPath=" +
                    project.rootDir.absolutePath + "/compose_stability_configuration.txt"
            )
        else composeArgs
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

    implementation(net.swiftzer.semver.semver)
    implementation(godaddy.color.picker)

    implementation(airbnb.lottie)
    implementation(airbnb.lottie.compose)

    implementation(kotlinx.serialization.json)
    implementation(kotlinx.collections.immutable)

    implementation(androidx.media3.exoplayer)
    implementation(androidx.media3.ui)

    implementation(androidx.navigation.compose)

    api(wire.runtime)

    implementation(hilt.android)
    ksp(hilt.compiler)
    implementation(androidx.hilt.navigation.compose)
    ksp(androidx.hilt.compiler)

    implementation(accompanist.drawablepainter)
    implementation(accompanist.insets.ui)
    implementation(accompanist.systemuicontroller)
    implementation(accompanist.placeholder.material)
    implementation(accompanist.navigation.material)

    implementation(sketch.core)
    implementation(sketch.compose)
    implementation(sketch.ext.compose)
    implementation(sketch.gif)
    implementation(sketch.okhttp)

    implementation(zoomimage.compose.sketch)

    implementation(compose.bom)
    androidTestImplementation(compose.bom)

    runtimeOnly(compose.runtime.tracing)
    implementation(compose.animation)
    implementation(compose.animation.graphics)
    implementation(compose.material)
    implementation(compose.material.icons.core)
    // Optional - Add full set of material icons
    implementation(compose.material.icons.extended)
    implementation(compose.ui.util)
//    implementation "androidx.compose.material3:material3"

    // Android Studio Preview support
    implementation(compose.ui.tooling.preview)
    debugImplementation(compose.ui.tooling)

    // UI Tests
    androidTestImplementation(compose.ui.test.junit4)
    debugRuntimeOnly(compose.ui.test.manifest)

    implementation(androidx.constraintlayout.compose)

    implementation(github.oaid)

    implementation(org.jetbrains.annotations)

    implementation(kotlin.stdlib)
    implementation(kotlin.reflect)

    implementation(kotlinx.coroutines.core)
    implementation(kotlinx.coroutines.android)

    implementation(androidx.lifecycle.runtime)
    implementation(androidx.lifecycle.viewmodel)
    implementation(androidx.lifecycle.viewmodel.compose)

    //AndroidX
    implementation(androidx.activity)
    implementation(androidx.activity.compose)
    implementation(androidx.appcompat)
    implementation(androidx.annotation)
    implementation(androidx.browser)
    implementation(androidx.constraintlayout)
    implementation(androidx.core)
    implementation(androidx.core.splashscreen)
    implementation(androidx.datastore.preferences)
    implementation(androidx.gridlayout)
    implementation(androidx.palette)
    implementation(androidx.window)
    implementation(androidx.startup.runtime)

    //Test
    testImplementation(junit.junit)
    androidTestImplementation(androidx.test.core)
    androidTestImplementation(androidx.test.ext.junit)
    androidTestImplementation(androidx.test.rules)
    androidTestImplementation(androidx.test.espresso.core)
    androidTestRuntimeOnly(androidx.test.runner)

    //Glide
    implementation(glide.core)
    ksp(glide.ksp)
    implementation(glide.okhttp3.integration)

    implementation(google.material)

    implementation(okhttp3.core)
    implementation(retrofit2.core)
    implementation(retrofit2.converter.wire)

    implementation(google.gson)
    implementation(org.litepal.android.kotlin)
    implementation(com.jaredrummler.colorpicker)

    implementation(github.matisse)
    implementation(xx.permissions)
    implementation(com.gyf.immersionbar.immersionbar)

    implementation(com.github.yalantis.ucrop)

    implementation(appcenter.analytics)
    implementation(appcenter.crashes)
    implementation(appcenter.distribute)
}
