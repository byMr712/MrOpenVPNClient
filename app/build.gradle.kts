plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mropenovpn.client"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mropenovpn.client"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/release.jks")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
        }

        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(project(":vpnlib"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

val apkVersion = "v${android.defaultConfig.versionName}"

tasks.register("copyApkToRoot") {
    group = "build"
    description = "Copy the built APKs to the project root"
    doLast {
        copy {
            from(layout.buildDirectory.dir("outputs/apk/debug"))
            from(layout.buildDirectory.dir("outputs/apk/release"))
            include("*.apk")
            into(rootProject.layout.projectDirectory)
            rename { name ->
                if (name.contains("debug")) "MrOpenVPNClient-$apkVersion-debug.apk"
                else "MrOpenVPNClient-$apkVersion.apk"
            }
        }
    }
}

tasks.matching { it.name in setOf("assembleDebug", "assembleRelease") }.configureEach {
    finalizedBy("copyApkToRoot")
}
