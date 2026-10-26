plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 서명 키는 GitHub Secrets에서 받아요 (매번 같은 키여야 워치에 덮어 설치돼요)
val ksPath: String? = System.getenv("KEYSTORE_PATH")

android {
    namespace = "io.github.korassaultrifle.schoolalarm"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.korassaultrifle.schoolalarm"
        minSdk = 30
        targetSdk = 34
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1.0"
    }

    signingConfigs {
        if (ksPath != null) {
            create("own") {
                storeFile = file(ksPath)
                storePassword = System.getenv("KEYSTORE_PASS")
                keyAlias = "schoolalarm"
                keyPassword = System.getenv("KEYSTORE_PASS")
                storeType = "pkcs12"
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (ksPath != null) signingConfig = signingConfigs.getByName("own")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
