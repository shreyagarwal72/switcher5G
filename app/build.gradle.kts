plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.app.switcher5g"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.app.switcher5g"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.0.5"
    }

    signingConfigs {
        create("release") {
            val ksFile = rootProject.file("app/release.keystore")
            val storePass = System.getenv("RELEASE_KEYSTORE_PASSWORD") ?: project.findProperty("RELEASE_KEYSTORE_PASSWORD")?.toString()
            val alias = System.getenv("RELEASE_KEY_ALIAS") ?: project.findProperty("RELEASE_KEY_ALIAS")?.toString()
            val keyPass = System.getenv("RELEASE_KEY_PASSWORD") ?: project.findProperty("RELEASE_KEY_PASSWORD")?.toString()

            if (!ksFile.exists()) {
                ksFile.parentFile?.mkdirs()
                runCatching {
                    ProcessBuilder(
                        "keytool", "-genkeypair", "-v",
                        "-keystore", ksFile.absolutePath,
                        "-alias", "switcher5g",
                        "-keyalg", "RSA",
                        "-keysize", "2048",
                        "-validity", "10000",
                        "-storepass", "switcher5gpass",
                        "-keypass", "switcher5gpass",
                        "-dname", "CN=Switcher5G, OU=Mobile, O=OpenSource, L=City, ST=State, C=US"
                    ).start().waitFor()
                }
            }

            if (ksFile.exists()) {
                storeFile = ksFile
                storePassword = if (!storePass.isNullOrEmpty()) storePass else "switcher5gpass"
                keyAlias = if (!alias.isNullOrEmpty()) alias else "switcher5g"
                keyPassword = if (!keyPass.isNullOrEmpty()) keyPass else storePassword
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }

    buildFeatures {
        compose = true
        aidl = true
    }
}

// Same Kotlin/Compose toolchain as Petal Browser (Kotlin 2.0.21 + Compose BOM 2026.06.01 +
// Material 3 1.5.0-alpha17): pin the Kotlin runtime libs to the compiler version so newer
// transitive dependencies can't pull in a stdlib the compiler can't read.
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.0.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:2.0.21")
        force("org.jetbrains.kotlin:kotlin-reflect:2.0.21")
    }
}

// Petal disables the AAR metadata check for the same dependency set (the alpha Compose/Material
// artifacts declare a newer AGP/compileSdk minimum than the toolchain enforces).
tasks.configureEach {
    if (name.startsWith("check") && name.endsWith("AarMetadata")) {
        enabled = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Compose BOM keeps all Compose artifacts on matching versions
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.5.0-alpha17") // Expressive APIs: ContainedLoadingIndicator, CircularWavyProgressIndicator, HorizontalFloatingToolbar, MaterialShapes
    implementation("androidx.graphics:graphics-shapes:1.0.1") // RoundedPolygon for MaterialShapes (same as Petal)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Shizuku — lets us run TelephonyManager's hidden network-mode APIs as `shell`,
    // which passes the MODIFY_PHONE_STATE check without device root.
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    // Unlocks reflective access to hidden/SystemApi framework methods on API 28+
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
