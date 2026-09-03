plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    // Maven Central publishing; applied only under -PmavenCentralRelease (below).
    id("com.vanniktech.maven.publish") version "0.30.0" apply false
}

android {
    namespace = "xyz.connect.sdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        // SDK version at runtime for the automation bridge's core.ping reply.
        buildConfigField(
            "String",
            "SDK_VERSION",
            "\"${(findProperty("SDK_VERSION") as? String) ?: "0.0.0-SNAPSHOT"}\"",
        )
    }

    buildTypes {
        release {
            // F-008 originally enabled R8 minify on the library to obscure
            // internals. That was wrong for an SDK: R8 ran against the
            // LIBRARY's proguard-rules.pro (which kept ConnectSDKTypes**
            // and AuthTypes** wildcards but missed top-level enums like
            // Environment, Theme, ConnectError, GenericEvent,
            // ConnectAuthSession, DepositEvent), so those public types
            // got renamed to a.c, a.e, etc. inside the AAR — consumers
            // couldn't reference them by their documented names.
            //
            // The consumer-rules.pro (packaged as proguard.txt in the
            // AAR) is correct (`-keep public class xyz.connect.sdk.** { *; }`)
            // but it applies to the CONSUMER's R8 pass — too late, by
            // then the AAR's classes are already renamed.
            //
            // RE resistance was never a real win anyway since the public
            // mirror at connect-by-zerohash/connect-android is open source;
            // anyone wanting to read the SDK can clone it.
            //
            // Libraries should ship un-minified bytecode + a consumer-rules.pro
            // telling consumers' R8 what to keep when minifying the final
            // app. Reverting to that pattern.
            isMinifyEnabled = false
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

// Artifact version comes from the git tag via -PSDK_VERSION; SNAPSHOT locally.
val sdkVersion = (findProperty("SDK_VERSION") as? String) ?: "0.0.0-SNAPSHOT"

// -PmavenCentralRelease: signed Central Portal publish (workflow only).
// Default (no flag): unsigned maven-publish path below, used by JitPack.
if (project.hasProperty("mavenCentralRelease")) {
    apply(plugin = "com.vanniktech.maven.publish")

    configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
        // Publishes and releases in one step — no manual promote in the Portal UI.
        publishToMavenCentral(
            com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL,
            automaticRelease = true,
        )
        signAllPublications()

        coordinates("xyz.connect", "connect-android", sdkVersion)
        configure(com.vanniktech.maven.publish.AndroidSingleVariantLibrary("release"))

        // url/scm point at the public mirror. All fields required by Central.
        pom {
            name.set("Connect Android SDK")
            description.set("Connect SDK for Android — drop-in native integration for the Connect Auth, Recovery, and Withdrawal flows.")
            url.set("https://github.com/connect-by-zerohash/connect-android")
            // Proprietary license; `name` matches the LICENSE file heading.
            licenses {
                license {
                    name.set("zerohash Android Wrapper License")
                    url.set("https://github.com/connect-by-zerohash/connect-android/blob/main/LICENSE")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("zerohash")
                    name.set("zerohash")
                    email.set("security@zerohash.com")
                }
            }
            scm {
                url.set("https://github.com/connect-by-zerohash/connect-android")
                connection.set("scm:git:https://github.com/connect-by-zerohash/connect-android.git")
                developerConnection.set("scm:git:ssh://git@github.com/connect-by-zerohash/connect-android.git")
            }
        }
    }
} else {
    publishing {
        publications {
            create<MavenPublication>("release") {
                // Advisory locally; JitPack overrides with
                // com.github.connect-by-zerohash / connect-android.
                groupId = "xyz.connect"
                artifactId = "connect-android"
                version = sdkVersion

                afterEvaluate {
                    from(components["release"])
                }
            }
        }
    }
}
