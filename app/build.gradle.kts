plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.app.switcher5g"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.app.switcher5g"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.2"
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
    }

    buildFeatures {
        compose = true
        aidl = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Compose BOM keeps all Compose artifacts on matching versions
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.4.0-alpha05") // Unlocks ExperimentalMaterial3ExpressiveApi and MaterialShapes.Cookie12Sided
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Shizuku — lets us run TelephonyManager's hidden network-mode APIs as `shell`,
    // which passes the MODIFY_PHONE_STATE check without device root.
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    // Unlocks reflective access to hidden/SystemApi framework methods on API 28+
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
