import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.android.application)
}

/** Runs a git command and returns trimmed stdout, or "" if git/the repo isn't available. */
fun runGit(vararg args: String): String {
    return try {
        val out = ByteArrayOutputStream()
        exec {
            commandLine(listOf("git") + args)
            standardOutput = out
            isIgnoreExitValue = true
        }
        out.toString().trim()
    } catch (e: Exception) {
        ""
    }
}

// versionCode = total commit count: monotonically increasing as long as history
// is linear-ish, which is all this solo/small-team repo needs.
val gitCommitCount = runGit("rev-list", "--count", "HEAD").toIntOrNull() ?: 1
// versionName = nearest tag (e.g. "v1.2.0", or "v1.2.0-4-gabc1234" for commits
// past the last tag). Falls back to "0.0.0-dev" so a fresh clone with no tags
// (or a shallow CI checkout) still builds instead of crashing Gradle.
val gitDescribe = runGit("describe", "--tags", "--always", "--dirty").ifBlank { "0.0.0-dev" }

android {
    namespace = "com.azlan.lumalight"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.azlan.lumalight"
        minSdk = 23
        targetSdk = 34
        versionCode = gitCommitCount
        versionName = gitDescribe
    }

    signingConfigs {
        create("release") {
            // Populated from GitHub Actions secrets (KEYSTORE_PATH points at the
            // keystore decoded from the base64'd KEYSTORE_B64 secret - see
            // release.yml). Left unconfigured for local builds without secrets,
            // in which case the release build type below skips signing.
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
