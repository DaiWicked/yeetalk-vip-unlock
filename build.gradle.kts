plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9 内置 Kotlin 支持：再声明 org.jetbrains.kotlin.android 会报错
    alias(libs.plugins.kotlin.compose) apply false
}
