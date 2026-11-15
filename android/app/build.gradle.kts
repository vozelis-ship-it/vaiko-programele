plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Versija rašoma android/version.txt (pvz. 1.0.3). versionCode skaičiuojamas: 1.0.3 → 10003.
val appVersionName: String = rootProject.file("version.txt").readText().trim()
val appVersionCode: Int = appVersionName.split(".").map { it.toInt() }.let { (a, b, c) -> a * 10000 + b * 100 + c }

// Pasirašymo raktas imamas iš aplinkos kintamųjų (GitHub Actions). Be jų release APK lieka nepasirašytas.
val ksFile: String? = System.getenv("ANDROID_KEYSTORE_FILE")

android {
    namespace = "lt.mokausi.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "lt.mokausi.app"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (ksFile != null) {
            create("release") {
                storeFile = file(ksFile)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (ksFile != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core:1.13.1")   // FileProvider (atnaujinimo diegimui)
}
