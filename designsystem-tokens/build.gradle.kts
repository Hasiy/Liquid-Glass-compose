/*
 * Copyright 2026 FitDash contributors.
 */

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}
group = "top.hasiy"
version = "1.0.0"

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}