# 📦 Connect SDK - Integration Guide

Technical documentation for integrating the Connect SDK into your Android application.

---

## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
  - [Option 1: Gradle Dependency (Recommended)](#option-1-gradle-dependency-recommended)
  - [Option 2: Local Module](#option-2-local-module)
- [Project Configuration](#project-configuration)
  - [Gradle Setup](#gradle-setup)
  - [AndroidManifest.xml](#androidmanifestxml)
  - [Permissions](#permissions)
- [Basic Integration](#basic-integration)
  - [Minimal Example](#minimal-example)
  - [Handling Callbacks](#handling-callbacks)
- [Advanced Integration](#advanced-integration)
  - [Custom Error Handling](#custom-error-handling)
  - [Event Processing](#event-processing)
  - [Session Management](#session-management)
- [Configuration Options](#configuration-options)
  - [Environment](#environment)
  - [Theme](#theme)
  - [JWT Token](#jwt-token)
- [ProGuard Configuration](#proguard-configuration)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)

---

## Requirements

### Minimum Requirements

- **Android SDK**: API 21 (Android 5.0 Lollipop) or higher
- **Target SDK**: API 34 (Android 14) recommended
- **Kotlin**: 1.9.0 or higher
- **Gradle**: 8.0 or higher
- **Java**: JDK 17 or higher

### Dependencies

The SDK includes these dependencies automatically:
- `androidx.appcompat:appcompat:1.6.1`
- `androidx.browser:browser:1.7.0` (for Chrome Custom Tabs)
- `org.json:json:20230227`

---

## Installation

### Option 1: Gradle Dependency (Recommended)

Add the Connect SDK to your app's `build.gradle.kts` or `build.gradle`:

**Kotlin DSL (`build.gradle.kts`):**
```kotlin
dependencies {
    implementation("xyz.connect:connect-sdk:1.0.0")
}
```

**Groovy (`build.gradle`):**
```groovy
dependencies {
    implementation 'xyz.connect:connect-sdk:1.0.0'
}
```

### Option 2: Local Module

If you need to customize the SDK or use it as a local module:

1. Copy the `connectsdk` folder to your project root
2. Include it in `settings.gradle.kts`:

```kotlin
include(":connectsdk")
```

3. Add dependency in your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":connectsdk"))
}
```

---

## Project Configuration

### Gradle Setup

Ensure your project's `build.gradle.kts` has the required repositories:

```kotlin
// Root build.gradle.kts
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.20")
    }
}
```

In your app's `build.gradle.kts`:

```kotlin
android {
    namespace = "com.yourcompany.yourapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yourcompany.yourapp"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
```

### AndroidManifest.xml

The SDK's manifest will be automatically merged with your app's manifest. However, verify these permissions are present:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Required permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".YourApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.YourApp">

        <!-- Your activities -->
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!--
            Note: The SDK's WebViewActivity and OAuth callback
            intent-filter are included automatically from the SDK's manifest
        -->

    </application>

</manifest>
```

### Permissions

The SDK requires these permissions (automatically included):

| Permission | Purpose | Required |
|------------|---------|----------|
| `INTERNET` | Load WebView content and API calls | Yes |
| `ACCESS_NETWORK_STATE` | Check network connectivity | Yes |

---

## Basic Integration

### Minimal Example

The simplest integration requires just a few lines of code:

```kotlin
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import xyz.connect.sdk.ConnectSDK
import xyz.connect.sdk.Environment
import xyz.connect.sdk.Theme
import xyz.connect.sdk.auth.AuthCallbacks
import xyz.connect.sdk.auth.ConnectAuthSession
import xyz.connect.sdk.ConnectError
import xyz.connect.sdk.GenericEvent
import xyz.connect.sdk.auth.DepositEvent

class MainActivity : AppCompatActivity() {

    private var authSession: ConnectAuthSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize and present Connect session
        startConnectSession()
    }

    private fun startConnectSession() {
        // Configure authentication session
        authSession = ConnectSDK.configureAuth(
            jwt = getJwtToken(), // Your JWT from backend
            environment = Environment.PRODUCTION,
            theme = Theme.SYSTEM,
            callbacks = object : AuthCallbacks {
                override fun onClose() {
                    // User closed the session
                }

                override fun onError(error: ConnectError) {
                    // Handle error
                }

                override fun onEvent(event: GenericEvent) {
                    // Handle generic events
                }

                override fun onDeposit(event: DepositEvent) {
                    // Handle deposit completion
                }
            }
        )

        // Present the session
        authSession?.present(this)
    }

    private fun getJwtToken(): String {
        // TODO: Get JWT from your backend
        return "your-jwt-token"
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel session if still active
        authSession?.cancel()
    }
}
```

### Handling Callbacks

Implement all callback methods to handle different events:

```kotlin
private fun createCallbacks(): AuthCallbacks {
    return object : AuthCallbacks {
        override fun onClose() {
            // Session closed by user
            Log.d(TAG, "Connect session closed")
            authSession = null

            // Update UI
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Session closed", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onError(error: ConnectError) {
            // Error occurred during session
            Log.e(TAG, "Connect error: ${error.message}")

            when (error) {
                is ConnectError.NetworkError -> {
                    // Handle network issues
                    showError("Network error: ${error.message}")
                }
                is ConnectError.AuthenticationError -> {
                    // Handle auth failures (invalid JWT, expired, etc)
                    showError("Authentication failed: ${error.message}")
                    // Maybe refresh JWT and retry
                }
                is ConnectError.OAuthError -> {
                    // OAuth flow failed
                    showError("OAuth error: ${error.message}")
                }
                is ConnectError.ConfigurationError -> {
                    // SDK misconfiguration
                    showError("Configuration error: ${error.message}")
                }
                is ConnectError.WebViewError -> {
                    // WebView loading issues
                    showError("WebView error: ${error.message}")
                }
                is ConnectError.UnknownError -> {
                    // Unexpected error
                    showError("Unknown error: ${error.message}")
                }
            }
        }

        override fun onEvent(event: GenericEvent) {
            // Generic events from the platform
            Log.d(TAG, "Event received: ${event.type}")

            // Access event data
            val message = event.getString("message")
            val success = event.getBool("success") ?: false

            // Process event based on type
            when (event.type) {
                "user_action" -> handleUserAction(event)
                "navigation" -> handleNavigation(event)
                else -> Log.d(TAG, "Unhandled event type: ${event.type}")
            }
        }

        override fun onDeposit(event: DepositEvent) {
            // Deposit completed
            Log.d(TAG, "Deposit event: ${event.depositId}")

            if (event.success) {
                // Deposit successful
                val message = """
                    Deposit Successful!
                    ID: ${event.depositId}
                    Asset: ${event.assetId}
                    Network: ${event.networkId}
                    Amount: ${event.amount}
                """.trimIndent()

                showSuccess(message)

                // Update your backend
                notifyBackendOfDeposit(event)
            } else {
                // Deposit failed
                showError("Deposit failed: ${event.status}")
            }
        }
    }
}
```

---

## Advanced Integration

### Custom Error Handling

Implement robust error handling with retry logic:

```kotlin
class ConnectManager(private val activity: AppCompatActivity) {

    private var authSession: ConnectAuthSession? = null
    private var retryCount = 0
    private val maxRetries = 3

    fun startSession(jwt: String) {
        authSession = ConnectSDK.configureAuth(
            jwt = jwt,
            environment = Environment.PRODUCTION,
            theme = Theme.SYSTEM,
            callbacks = createCallbacksWithRetry()
        )

        authSession?.present(activity)
    }

    private fun createCallbacksWithRetry(): AuthCallbacks {
        return object : AuthCallbacks {
            override fun onClose() {
                retryCount = 0
                authSession = null
            }

            override fun onError(error: ConnectError) {
                when (error) {
                    is ConnectError.NetworkError -> {
                        if (retryCount < maxRetries) {
                            retryCount++
                            Log.d(TAG, "Retrying... ($retryCount/$maxRetries)")

                            // Wait and retry
                            Handler(Looper.getMainLooper()).postDelayed({
                                refreshJwtAndRetry()
                            }, 2000)
                        } else {
                            showError("Network error after $maxRetries attempts")
                        }
                    }
                    is ConnectError.AuthenticationError -> {
                        // JWT expired or invalid, refresh and retry once
                        if (retryCount == 0) {
                            retryCount++
                            refreshJwtAndRetry()
                        } else {
                            showError("Authentication failed")
                        }
                    }
                    else -> {
                        showError(error.message ?: "Unknown error")
                    }
                }
            }

            override fun onEvent(event: GenericEvent) {
                // Handle events
            }

            override fun onDeposit(event: DepositEvent) {
                // Handle deposits
            }
        }
    }

    private fun refreshJwtAndRetry() {
        // Fetch new JWT from your backend
        fetchJwtFromBackend { newJwt ->
            if (newJwt != null) {
                startSession(newJwt)
            } else {
                showError("Failed to refresh authentication")
            }
        }
    }

    private fun fetchJwtFromBackend(callback: (String?) -> Unit) {
        // TODO: Implement your JWT refresh logic
        // Example:
        // yourApi.refreshJwt().enqueue(object : Callback<JwtResponse> {
        //     override fun onResponse(call: Call<JwtResponse>, response: Response<JwtResponse>) {
        //         callback(response.body()?.jwt)
        //     }
        //     override fun onFailure(call: Call<JwtResponse>, t: Throwable) {
        //         callback(null)
        //     }
        // })
    }
}
```

### Event Processing

Process and route different event types:

```kotlin
class EventProcessor {

    fun processEvent(event: GenericEvent) {
        when (event.type) {
            "user_authenticated" -> handleAuthentication(event)
            "account_linked" -> handleAccountLink(event)
            "transfer_initiated" -> handleTransfer(event)
            "connection_established" -> handleConnection(event)
            else -> Log.d(TAG, "Unknown event: ${event.type}")
        }
    }

    private fun handleAuthentication(event: GenericEvent) {
        val userId = event.getString("user_id")
        val provider = event.getString("provider")

        Log.d(TAG, "User authenticated via $provider: $userId")

        // Update your app state
        // Store user info
        // Navigate to next screen
    }

    private fun handleAccountLink(event: GenericEvent) {
        val accountId = event.getString("account_id")
        val accountType = event.getString("account_type")
        val linked = event.getBool("linked") ?: false

        if (linked) {
            Log.d(TAG, "Account linked: $accountId ($accountType)")
            // Update UI, sync with backend
        }
    }

    private fun handleTransfer(event: GenericEvent) {
        val transferId = event.getString("transfer_id")
        val amount = event.getString("amount")
        val currency = event.getString("currency")

        Log.d(TAG, "Transfer initiated: $transferId - $amount $currency")
        // Track transfer, update UI
    }

    private fun handleConnection(event: GenericEvent) {
        val connectionId = event.getString("connection_id")
        val institution = event.getString("institution")

        Log.d(TAG, "Connection established with $institution: $connectionId")
        // Store connection info
    }
}
```

### Session Management

Manage session lifecycle properly:

```kotlin
class SessionManager(private val context: Context) {

    private var currentSession: ConnectAuthSession? = null
    private val prefs = context.getSharedPreferences("connect_prefs", Context.MODE_PRIVATE)

    fun startSession(activity: AppCompatActivity, jwt: String) {
        // Cancel existing session if any
        cancelSession()

        // Create new session
        currentSession = ConnectSDK.configureAuth(
            jwt = jwt,
            environment = getEnvironment(),
            theme = getTheme(),
            callbacks = createCallbacks()
        )

        // Save session start time
        prefs.edit().putLong("session_start", System.currentTimeMillis()).apply()

        // Present session
        currentSession?.present(activity)
    }

    fun cancelSession() {
        currentSession?.cancel()
        currentSession = null
        clearSessionData()
    }

    fun isSessionActive(): Boolean {
        return currentSession?.isActive() == true
    }

    fun getSessionDuration(): Long {
        val startTime = prefs.getLong("session_start", 0)
        return if (startTime > 0) {
            System.currentTimeMillis() - startTime
        } else {
            0
        }
    }

    private fun getEnvironment(): Environment {
        val envString = prefs.getString("environment", "production")
        return when (envString) {
            "sandbox" -> Environment.SANDBOX
            else -> Environment.PRODUCTION
        }
    }

    private fun getTheme(): Theme {
        val themeString = prefs.getString("theme", "system")
        return when (themeString) {
            "light" -> Theme.LIGHT
            "dark" -> Theme.DARK
            else -> Theme.SYSTEM
        }
    }

    private fun clearSessionData() {
        prefs.edit().remove("session_start").apply()
    }

    private fun createCallbacks(): AuthCallbacks {
        return object : AuthCallbacks {
            override fun onClose() {
                cancelSession()
            }

            override fun onError(error: ConnectError) {
                // Handle error
            }

            override fun onEvent(event: GenericEvent) {
                // Handle event
            }

            override fun onDeposit(event: DepositEvent) {
                // Handle deposit
            }
        }
    }
}

// Usage in Activity:
class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)

        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            val jwt = getJwtToken()
            sessionManager.startSession(this, jwt)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            sessionManager.cancelSession()
        }
    }
}
```

---

## Configuration Options

### Environment

Choose between production and sandbox environments:

```kotlin
// Production (default)
ConnectSDK.configureAuth(
    jwt = jwt,
    environment = Environment.PRODUCTION,
    // ...
)

// Sandbox for testing
ConnectSDK.configureAuth(
    jwt = jwt,
    environment = Environment.SANDBOX,
    // ...
)

// Dynamic environment based on build type
val environment = if (BuildConfig.DEBUG) {
    Environment.SANDBOX
} else {
    Environment.PRODUCTION
}
```

### Theme

Control the visual theme:

```kotlin
// System theme (default - follows device settings)
theme = Theme.SYSTEM

// Light theme
theme = Theme.LIGHT

// Dark theme
theme = Theme.DARK

// Allow user to choose
val theme = when (userPreference) {
    "light" -> Theme.LIGHT
    "dark" -> Theme.DARK
    else -> Theme.SYSTEM
}
```

### JWT Token

Best practices for JWT management:

```kotlin
class JwtManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveJwt(jwt: String) {
        prefs.edit().putString("jwt", jwt).apply()
    }

    fun getJwt(): String? {
        return prefs.getString("jwt", null)
    }

    fun clearJwt() {
        prefs.edit().remove("jwt").apply()
    }

    fun isJwtValid(jwt: String): Boolean {
        // Basic validation
        if (jwt.isBlank()) return false

        // Check JWT structure (3 parts separated by dots)
        val parts = jwt.split(".")
        if (parts.size != 3) return false

        // TODO: Add expiration check by decoding payload
        // This requires a JWT library or manual Base64 decoding

        return true
    }

    suspend fun fetchFreshJwt(): String? {
        // TODO: Implement your backend call
        // Example:
        // return withContext(Dispatchers.IO) {
        //     try {
        //         val response = yourApi.getJwt().execute()
        //         response.body()?.jwt
        //     } catch (e: Exception) {
        //         null
        //     }
        // }
        return null
    }
}

// Usage:
class MainActivity : AppCompatActivity() {

    private lateinit var jwtManager: JwtManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        jwtManager = JwtManager(this)

        lifecycleScope.launch {
            val jwt = jwtManager.getJwt() ?: jwtManager.fetchFreshJwt()

            if (jwt != null && jwtManager.isJwtValid(jwt)) {
                startConnect(jwt)
            } else {
                showError("Failed to get valid JWT")
            }
        }
    }

    private fun startConnect(jwt: String) {
        // Save for future use
        jwtManager.saveJwt(jwt)

        // Start Connect session
        val authSession = ConnectSDK.configureAuth(
            jwt = jwt,
            // ...
        )
        authSession.present(this)
    }
}
```

---

## ProGuard Configuration

If using ProGuard/R8, add these rules to `proguard-rules.pro`:

```proguard
# Connect SDK
-keep class xyz.connect.sdk.** { *; }
-keepclassmembers class xyz.connect.sdk.** { *; }

# JavaScript Interface
-keepclassmembers class xyz.connect.sdk.ui.WebViewMessageHandler {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep callback interfaces
-keep interface xyz.connect.sdk.auth.AuthCallbacks { *; }
-keep class xyz.connect.sdk.auth.DepositEvent { *; }
-keep class xyz.connect.sdk.GenericEvent { *; }
-keep class xyz.connect.sdk.ConnectError { *; }

# Kotlin
-keep class kotlin.Metadata { *; }

# Gson (if using for JSON parsing in your callbacks)
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
```

Your app's `build.gradle.kts`:

```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

---

## Testing

### Unit Testing

Mock the SDK for unit tests:

```kotlin
class ConnectSessionTest {

    @Mock
    private lateinit var mockSession: ConnectAuthSession

    @Mock
    private lateinit var mockCallbacks: AuthCallbacks

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `test session creation with valid JWT`() {
        val jwt = "valid.jwt.token"

        // Mock SDK behavior
        `when`(mockSession.isActive()).thenReturn(true)

        // Verify session is active
        assertTrue(mockSession.isActive())
    }

    @Test
    fun `test callback on deposit success`() {
        val depositEvent = DepositEvent(
            depositId = "dep_123",
            status = "completed",
            success = true,
            assetId = "USDC",
            networkId = "ethereum",
            amount = "100.00",
            rawData = null
        )

        // Trigger callback
        mockCallbacks.onDeposit(depositEvent)

        // Verify callback was called
        verify(mockCallbacks, times(1)).onDeposit(any())
    }
}
```

### Integration Testing

Test the actual SDK integration:

```kotlin
@RunWith(AndroidJUnit4::class)
class ConnectIntegrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testConnectSessionLaunch() {
        activityRule.scenario.onActivity { activity ->
            val jwt = "test.jwt.token"

            // Configure session
            val session = ConnectSDK.configureAuth(
                jwt = jwt,
                environment = Environment.SANDBOX,
                theme = Theme.LIGHT,
                callbacks = object : AuthCallbacks {
                    override fun onClose() {}
                    override fun onError(error: ConnectError) {}
                    override fun onEvent(event: GenericEvent) {}
                    override fun onDeposit(event: DepositEvent) {}
                }
            )

            // Present session
            session.present(activity)

            // Verify WebViewActivity is launched
            Intents.intended(hasComponent(WebViewActivity::class.java.name))
        }
    }
}
```

---

## Troubleshooting

### Issue: WebView not loading

**Symptoms:** Blank screen, WebView doesn't load content

**Solutions:**
1. Check internet permission in manifest
2. Verify JWT is valid
3. Check network connectivity
4. Enable WebView debugging:

```kotlin
if (BuildConfig.DEBUG) {
    WebView.setWebContentsDebuggingEnabled(true)
}
```

Then inspect via Chrome DevTools: `chrome://inspect`

### Issue: OAuth callback not working

**Symptoms:** OAuth flow opens but doesn't return to app

**Solutions:**
1. Verify intent filter in merged manifest:
```bash
./gradlew :app:processDebugManifest
# Check: app/build/intermediates/merged_manifests/debug/AndroidManifest.xml
```

2. Ensure the callback scheme is correct: `connectsdk-oauth://callback`

3. Test the intent filter:
```bash
adb shell am start -W -a android.intent.action.VIEW -d "connectsdk-oauth://callback?code=test"
```

### Issue: Callbacks not firing

**Symptoms:** No response from SDK callbacks

**Solutions:**
1. Verify you're keeping a reference to the session:
```kotlin
// Wrong - session gets garbage collected
fun start() {
    val session = ConnectSDK.configureAuth(...)
    session.present(this)
} // session destroyed here

// Correct - keep reference
class MainActivity {
    private var session: ConnectAuthSession? = null

    fun start() {
        session = ConnectSDK.configureAuth(...)
        session?.present(this)
    }
}
```

2. Check Activity lifecycle - callbacks run on UI thread

### Issue: ProGuard strips SDK classes

**Symptoms:** App crashes in release build with ClassNotFoundException

**Solution:** Add ProGuard rules as shown in [ProGuard Configuration](#proguard-configuration)

### Issue: SSL Certificate errors

**Symptoms:** WebView shows certificate error

**Solution:** The SDK handles SSL for Connect domains. If you see errors:
1. Check device date/time is correct
2. Verify network isn't intercepting SSL (corporate proxy)
3. For development only, you can bypass (not recommended for production)

### Issue: Memory leaks

**Symptoms:** App memory grows, Activity not released

**Solution:** Always cancel session in onDestroy:
```kotlin
override fun onDestroy() {
    super.onDestroy()
    authSession?.cancel()
    authSession = null
}
```

---

## Support

For additional help:
- **Documentation**: https://docs.connect.xyz
- **GitHub Issues**: [repository-url]
- **Email**: support@connect.xyz

---

## Next Steps

- Review [README.md](README.md) for SDK overview
- Check [QUICKSTART.md](QUICKSTART.md) to run the demo app
- See example implementations in the `app/` module

---

**Last Updated:** 2024-01-14
