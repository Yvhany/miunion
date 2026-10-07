plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.miunion.app"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.miunion.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "2.0.0"
    }

    signingConfigs {
        // 发布签名（keystore 不入库，见 .gitignore）
        create("release") {
            storeFile = file("${rootProject.projectDir}/release.keystore")
            storePassword = "miunion2026"
            keyAlias = "miunion"
            keyPassword = "miunion2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            // 2.0.0 起发布 64 位（arm64-v8a / armeabi-v7a 合并为 armv8 版本仅保留 64 位）
            ndk {
                abiFilters += setOf("arm64-v8a")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation("org.jetbrains.compose.runtime:runtime-android:1.12.1")
    implementation("org.jetbrains.compose.foundation:foundation-android:1.12.1")
    implementation("org.jetbrains.compose.material3:material3-android:1.12.0-alpha03")
    implementation("org.jetbrains.compose.ui:ui-android:1.12.1")
    implementation(libs.androidx.activity.compose)
    implementation("androidx.biometric:biometric:1.1.0")
    implementation(libs.miuix.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.icons)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
}