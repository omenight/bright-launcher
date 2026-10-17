import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release key comes from keystore.properties locally or BRIGHT_* variables in CI, the APK stays unsigned without either
val keystore = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.reader()?.use(::load)
}
fun signingValue(key: String, env: String): String? = keystore.getProperty(key) ?: System.getenv(env)
val releaseStoreFile = signingValue("storeFile", "BRIGHT_STORE_FILE")

android {
    namespace = "com.omenight.bright"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.omenight.bright"
        minSdk = 23
        targetSdk = 37
        versionCode = 3
        versionName = "0.1.2"
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = signingValue("storePassword", "BRIGHT_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "BRIGHT_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "BRIGHT_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Lets a debug build sit next to the release one on the same box
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        // Drops the dozens of library translations Bright never shows, add a code here with each new values-xx
        localeFilters += listOf("en", "ru")
        // Lists the languages in the system per-app language picker on Android 13+
        generateLocaleConfig = true
    }

    buildFeatures {
        compose = true
    }

    dependenciesInfo {
        // Keeps the APK reproducible for F-Droid / IzzyOnDroid
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.profileinstaller)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.tv.material)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
