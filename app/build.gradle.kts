plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.parengmark.fueltracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.parengmark.fueltracker"
        minSdk = 26
        targetSdk = 34
        versionCode = 7
        versionName = "0.7.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Populated by the GitHub Actions workflow from repo secrets at build time.
        // Locally (Android Studio), release builds without these env vars will
        // simply fall back to being unsigned -- use the debug build for local testing.
        create("release") {
            val ksPath = System.getenv("RELEASE_KEYSTORE_PATH")
            if (ksPath != null) {
                storeFile = file(ksPath)
                storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (System.getenv("RELEASE_KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
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
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.3")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Room (local, offline-first database)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Phase 2: on-device receipt OCR (no network call, no cloud billing needed)
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // In-app QR code for sharing the download link
    implementation("com.google.zxing:core:3.5.3")

    // Phase 2: background daily check for due maintenance reminders
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// --- Version badge on the launcher icon --------------------------------------
// Draws "v0.7"-style text in a 3x5 pixel font as vector rectangles, so the
// installed icon always shows the build's versionName. Generated into
// build/ before every build and layered over the icon foreground by
// res/drawable/ic_launcher_foreground_badged.xml.

val versionBadgeResDir: File = layout.buildDirectory.dir("generated/versionBadge/res").get().asFile

fun versionBadgeXml(versionName: String): String {
    val font = mapOf(
        '0' to listOf("111", "101", "101", "101", "111"),
        '1' to listOf("010", "110", "010", "010", "111"),
        '2' to listOf("111", "001", "111", "100", "111"),
        '3' to listOf("111", "001", "111", "001", "111"),
        '4' to listOf("101", "101", "111", "001", "001"),
        '5' to listOf("111", "100", "111", "001", "111"),
        '6' to listOf("111", "100", "111", "101", "111"),
        '7' to listOf("111", "001", "001", "001", "001"),
        '8' to listOf("111", "101", "111", "101", "111"),
        '9' to listOf("111", "101", "111", "001", "111"),
        'v' to listOf("000", "101", "101", "101", "010"),
        '.' to listOf("0", "0", "0", "0", "1")
    )
    // "0.7.0" -> "v0.7"; "0.7.2" -> "v0.7.2"
    val parts = versionName.split(".").filter { it.isNotEmpty() }
    val shown = if (parts.size == 3 && parts[2] == "0") parts.take(2) else parts
    val text = ("v" + shown.joinToString(".")).filter { it in font.keys }

    val columns = text.sumOf { font.getValue(it)[0].length } + (text.length - 1)
    val px = minOf(2.4, 40.0 / columns)
    val textWidth = columns * px
    val cx = 54.0
    val cy = 71.0
    val pillW = textWidth + 7.0
    val pillH = 5 * px + 6.0
    fun f(v: Double) = String.format(java.util.Locale.US, "%.2f", v)

    val pill = run {
        val l = cx - pillW / 2
        val t = cy - pillH / 2
        val r = cx + pillW / 2
        val b = cy + pillH / 2
        val rad = 4.0
        "M${f(l + rad)},${f(t)}H${f(r - rad)}A${f(rad)},${f(rad)} 0,0 1,${f(r)},${f(t + rad)}" +
            "V${f(b - rad)}A${f(rad)},${f(rad)} 0,0 1,${f(r - rad)},${f(b)}" +
            "H${f(l + rad)}A${f(rad)},${f(rad)} 0,0 1,${f(l)},${f(b - rad)}" +
            "V${f(t + rad)}A${f(rad)},${f(rad)} 0,0 1,${f(l + rad)},${f(t)}Z"
    }
    val glyphs = StringBuilder()
    var x = cx - textWidth / 2
    val y0 = cy - 2.5 * px
    for (c in text) {
        val g = font.getValue(c)
        g.forEachIndexed { row, bits ->
            bits.forEachIndexed { col, bit ->
                if (bit == '1') {
                    val gx = x + col * px
                    val gy = y0 + row * px
                    glyphs.append("M${f(gx)},${f(gy)}h${f(px)}v${f(px)}h${f(-px)}Z")
                }
            }
        }
        x += (g[0].length + 1) * px
    }
    return """<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by app/build.gradle.kts from versionName "$versionName". Do not edit. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="#FFB300" android:pathData="$pill" />
    <path android:fillColor="#1A1A1A" android:pathData="$glyphs" />
</vector>
"""
}

val generateVersionBadge = tasks.register("generateVersionBadge") {
    val versionName = android.defaultConfig.versionName ?: "0"
    val xml = versionBadgeXml(versionName)
    val outFile = File(versionBadgeResDir, "drawable/ic_version_badge.xml")
    inputs.property("badgeXml", xml)
    outputs.file(outFile)
    doLast {
        outFile.parentFile.mkdirs()
        outFile.writeText(xml)
    }
}

android.sourceSets.getByName("main").res.srcDir(versionBadgeResDir)

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(generateVersionBadge)
}
