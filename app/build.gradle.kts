plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sscrribble.nightdim"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sscrribble.nightdim"
        minSdk = 31 // Extra dim(reduce bright colors)은 Android 12부터
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 개인용: 별도 키 없이 설치 가능하도록 debug 키로 서명
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
