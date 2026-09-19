plugins {
    id("com.android.application") version "8.9.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    // Kotlin 2.x ships the Compose compiler as a Gradle plugin (replaces composeOptions.kotlinCompilerExtensionVersion)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
