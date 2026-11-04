plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.lcs.yeetalk"
    compileSdk = 37
    // 本机仅装了 36.0.0；显式指定可避免 AGP 自动下载 37.x 到系统盘
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.lcs.yeetalk"
        minSdk = 27
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // hook 依赖类名/方法名稳定，且需要反射，此处不做混淆
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/AL2.0",
            "META-INF/LGPL2.1",
            "META-INF/*.kotlin_module",
            "DebugProbesKt.bin"
        )
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    // 运行时由 LSPosed 提供，不打进 APK。
    // 用仓库内 libs/xposed-api-82.jar（25 KB），避免每次构建都去 api.xposed.info 拉取。
    compileOnly(files("../libs/xposed-api-82.jar"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.ripple)

    // Liquid Glass
    implementation(libs.backdrop)
    implementation(libs.kyant.shapes)
}
