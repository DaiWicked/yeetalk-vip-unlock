pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Xposed API（de.robv.android.xposed:api）只在官方仓库发布
        maven("https://api.xposed.info/")
    }
}

rootProject.name = "YeeTalkVipUnlocker"
include(":module")
