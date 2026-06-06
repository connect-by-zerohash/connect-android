plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "xyz.connect.sdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            // F-008: Enable R8/ProGuard shrinking and obfuscation so that
            // internal class names and method names are renamed in the
            // release AAR, making reverse-engineering significantly harder.
            isMinifyEnabled = true
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
        viewBinding = true
        // Required for BuildConfig.DEBUG guards (F-007)
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.browser:browser:1.7.0") // Chrome Custom Tabs for OAuth
    // WebViewCompat.addWebMessageListener with allowedOriginRules — provides
    // per-frame origin filtering on the JS↔Kotlin bridge (replaces the
    // top-frame-only check that addJavascriptInterface allows).
    implementation("androidx.webkit:webkit:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // JSON parsing handled by org.json (bundled with Android) — Gson removed
    // after WebViewMessageHandler was migrated to JSONObject (reduces AAR size
    // and eliminates an unnecessary dependency from partner apps).

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20231013")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            // groupId / artifactId are advisory — JitPack overrides them with
            // `com.github.connect-by-zerohash` / `connect-android` (derived
            // from the public mirror's GitHub path) when it resolves the
            // artifact for customers. Local `publishToMavenLocal` runs (and
            // ad-hoc Sonatype usage if we ever pivot to Maven Central) still
            // see these values.
            groupId = "xyz.connect"
            artifactId = "connect-sdk"

            // Single source of truth = git tag. JitPack forwards the resolved
            // tag via -PSDK_VERSION=$VERSION (see jitpack.yml); fall back to
            // a SNAPSHOT marker for local `publishToMavenLocal` runs so the
            // build doesn't fail when the property is absent.
            version = (findProperty("SDK_VERSION") as? String) ?: "0.0.0-SNAPSHOT"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
