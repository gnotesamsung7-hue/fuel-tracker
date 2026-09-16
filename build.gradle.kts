// Top-level build file: declares plugin versions used by modules below,
// applied with `apply false` here so the `app` module can apply them.
//
// Versions below (AGP 8.7.2, Kotlin 2.0.21, compileSdk/targetSdk 34 in
// app/build.gradle.kts) were picked as known-stable, well-documented releases
// rather than whatever is bleeding-edge as you read this -- this project was
// generated in an environment with no access to Maven/Google's repos, so
// nothing here could be test-built or verified against the latest available
// versions. Android Gradle Plugin 9.x is out and has real breaking changes
// (new DSL, built-in Kotlin support replacing this plugin block entirely) --
// worth reading AGP's release notes before upgrading rather than just bumping
// the version number. Android Studio's own Upgrade Assistant is the safest
// way to move this project forward when you're ready.
plugins {
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.kapt") version "2.0.21" apply false
}
