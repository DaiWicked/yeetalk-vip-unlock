import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

android {
    namespace = "com.lcs.yeetalk"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lcs.yeetalk"
        minSdk = 27
        targetSdk = 36
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
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // 运行时由 LSPosed 提供，不打进 APK
    compileOnly(libs.xposed.api)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.animation)
    implementation(compose.material3)
    implementation(compose.ui)

    // Liquid Glass
    implementation(libs.backdrop)
    implementation(libs.kyant.shapes)
}
