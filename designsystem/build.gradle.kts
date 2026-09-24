import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    id("maven-publish")
}

// 公開面盤點：把「沒寫修飾符、預設 public」的宣告全部列成警告，
// 清完之後再改成 explicitApi() 讓它變成編譯錯誤。
kotlin {
    explicitApiWarning()

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        publishLibraryVariants("release")
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.foundation)
            api(compose.ui)
            api(compose.material3)
            api(project(":designsystem-tokens"))
            implementation(libs.haze)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

group = "top.hasiy"
// 2.0.0：CMP 化伴隨破壞性 API 變更（@StringRes 參數改 String、paletteNameRes 移除），見
// designsystem-cmp-migration-plan.md §5。KMP 的 maven-publish 會自動建立各 target 的
// publication，group/version 直接沿用這裡的專案屬性。
version = "2.0.0"

android {
    namespace = "top.hasiy.designsystem"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // 作為 SDK 對外發佈時需要打包 source / javadoc
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    // androidInstrumentedTest 的依赖走 AGP 的 androidTestImplementation 配置：
    // KotlinDependencyHandler 里的 platform(Any) 在 Kotlin 2.1 已按错误处理。
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
