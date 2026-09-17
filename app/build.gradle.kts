plugins {
    alias(libs.plugins.android.application)
}

// Keep version metadata independent of a local Git executable. GitHub Actions
// supplies GITHUB_RUN_NUMBER for monotonically increasing CI builds, while a
// local build uses the checked-in fallback values below.
val ciBuildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
    ?.takeIf { it > 0 }
    ?: 1
val releaseVersion = System.getenv("LUMALIGHT_VERSION")?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: "1.0.1"

android {
    namespace = "com.azlan.lumalight"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.azlan.lumalight"
        minSdk = 23
        targetSdk = 34
        versionCode = ciBuildNumber
        versionName = releaseVersion
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASS")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASS")
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (!System.getenv("KEYSTORE_PATH").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
