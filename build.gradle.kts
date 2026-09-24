// Top-level build file. AGP / Kotlin 版本統一由 gradle/libs.versions.toml 管理。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.binary.compatibility.validator)
}

// SDK 對外的公開面以 api 目錄下的 dump 為準；app 是驗收用範例，不列入。
apiValidation {
    ignoredProjects.add("app")
    ignoredClasses.add("top.hasiy.designsystem.BuildConfig")
}