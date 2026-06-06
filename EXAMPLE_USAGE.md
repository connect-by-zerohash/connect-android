# Connect SDK - Example Usage

## Complete Implementation Example

### 1. Add Dependency

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("xyz.connect:connect-sdk:1.0.0")
}
```

### 2. MainActivity Implementation

```kotlin
package com.example.myapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import xyz.connect.sdk.ConnectError
import xyz.connect.sdk.ConnectSDK
import xyz.connect.sdk.Environment
import xyz.connect.sdk.GenericEvent
import xyz.connect.sdk.Theme
import xyz.connect.sdk.auth.AuthCallbacks
import xyz.connect.sdk.auth.ConnectAuthSession
import xyz.connect.sdk.auth.DepositEvent

class MainActivity : AppCompatActivity() {

    private var authSession: ConnectAuthSession? = null

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup UI
        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            startConnectAuth()
        }
    }

    private fun startConnectAuth() {
        // Get JWT token (from your backend)
        val jwt = getJWTToken()

        if (jwt.isBlank()) {
            Toast.makeText(this, "JWT token required", Toast.LENGTH_SHORT).show()
            return
        }

        // Configure authentication session
        authSession = ConnectSDK.configureAuth(
            jwt = jwt,
            environment = Environment.PRODUCTION,
            theme = Theme.SYSTEM,
            callbacks = object : AuthCallbacks {
                override fun onClose() {
                    Log.d(TAG, "Connect session closed")
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Session closed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    authSession = null
                }

                override fun onError(error: ConnectError) {
                    Log.e(TAG, "Connect error: ${error.message}")
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Error: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    // Handle specific error types
                    when (error) {
                        is ConnectError.NetworkError -> {
                            // Handle network error
                            Log.e(TAG, "Network issue detected")
                        }
                        is ConnectError.AuthenticationError -> {
                            // Handle auth error (e.g., invalid JWT)
                            Log.e(TAG, "Authentication failed")
                        }
                        is ConnectError.OAuthError -> {
                            // Handle OAuth error
                            Log.e(TAG, "OAuth flow failed")
                        }
                        else -> {
                            // Handle other errors
                            Log.e(TAG, "Unknown error")
                        }
                    }
                }

                override fun onEvent(event: GenericEvent) {
                    Log.d(TAG, "Connect event: ${event.type}")

                    // Access event data
                    val eventData = buildString {
                        append("Event: ${event.type}\n")
                        event.getString("message")?.let { append("Message: $it\n") }
                        event.getBool("success")?.let { append("Success: $it\n") }
                    }

                    Log.d(TAG, eventData)
                }

                override fun onDeposit(event: DepositEvent) {
                    Log.d(TAG, "Deposit received: ${event.depositId}")

                    runOnUiThread {
                        if (event.success) {
                            Toast.makeText(
                                this@MainActivity,
                                "Deposit successful: ${event.depositId}",
                                Toast.LENGTH_LONG
                            ).show()

                            // Process deposit
                            processDeposit(event)
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Deposit failed: ${event.status}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        )

        // Present the authentication session
        authSession?.present(this)
    }

    private fun processDeposit(deposit: DepositEvent) {
        Log.d(TAG, """
            Deposit Details:
            - ID: ${deposit.depositId}
            - Status: ${deposit.status}
            - Success: ${deposit.success}
            - Asset: ${deposit.assetId}
            - Network: ${deposit.networkId}
            - Amount: ${deposit.amount}
        """.trimIndent())

        // Send to your backend
        // sendDepositToBackend(deposit)
    }

    private fun getJWTToken(): String {
        // TODO: Get JWT from your backend
        // This is just a placeholder
        return "your-jwt-token-here"
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel session if still active
        authSession?.cancel()
    }
}
```

### 3. Layout File

```xml
<!-- res/layout/activity_main.xml -->
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="16dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Connect SDK Example"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="32dp" />

    <Button
        android:id="@+id/btnConnect"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Connect Account"
        android:textSize="18sp"
        android:paddingHorizontal="32dp"
        android:paddingVertical="16dp" />

</LinearLayout>
```

### 4. AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.AppCompat.Light">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

## Advanced Examples

### Using Different Environments

```kotlin
// Production environment
val prodSession = ConnectSDK.configureAuth(
    jwt = jwt,
    environment = Environment.PRODUCTION,
    theme = Theme.SYSTEM,
    callbacks = callbacks
)

// Sandbox environment for testing
val sandboxSession = ConnectSDK.configureAuth(
    jwt = jwt,
    environment = Environment.SANDBOX,
    theme = Theme.SYSTEM,
    callbacks = callbacks
)
```

### Custom Theme Selection

```kotlin
// Force dark mode
val darkSession = ConnectSDK.configureAuth(
    jwt = jwt,
    environment = Environment.PRODUCTION,
    theme = Theme.DARK,
    callbacks = callbacks
)

// Force light mode
val lightSession = ConnectSDK.configureAuth(
    jwt = jwt,
    environment = Environment.PRODUCTION,
    theme = Theme.LIGHT,
    callbacks = callbacks
)

// Follow system theme (default)
val systemSession = ConnectSDK.configureAuth(
    jwt = jwt,
    environment = Environment.PRODUCTION,
    theme = Theme.SYSTEM,
    callbacks = callbacks
)
```

### Handling Session Lifecycle

```kotlin
class MyActivity : AppCompatActivity() {
    private var session: ConnectAuthSession? = null

    fun startSession() {
        session = ConnectSDK.configureAuth(...)
        session?.present(this)
    }

    fun cancelSession() {
        session?.cancel()
        session = null
    }

    fun isSessionActive(): Boolean {
        return session?.isActive() ?: false
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelSession()
    }
}
```

### Processing Generic Events

```kotlin
override fun onEvent(event: GenericEvent) {
    when (event.type) {
        "account_linked" -> {
            val accountId = event.getString("accountId")
            Log.d(TAG, "Account linked: $accountId")
        }
        "connection_status" -> {
            val isConnected = event.getBool("connected") ?: false
            Log.d(TAG, "Connection status: $isConnected")
        }
        "progress_update" -> {
            val progress = event.getInt("progress") ?: 0
            Log.d(TAG, "Progress: $progress%")
        }
        else -> {
            Log.d(TAG, "Unknown event: ${event.type}")
        }
    }
}
```

### Error Handling Best Practices

```kotlin
override fun onError(error: ConnectError) {
    when (error) {
        is ConnectError.NetworkError -> {
            // Show retry dialog
            showRetryDialog("Network error. Please check your connection.")
        }
        is ConnectError.AuthenticationError -> {
            // Refresh JWT token
            refreshJWTAndRetry()
        }
        is ConnectError.ConfigurationError -> {
            // Log to analytics
            logErrorToAnalytics(error)
        }
        is ConnectError.OAuthError -> {
            // Show user-friendly message
            showMessage("OAuth authentication failed. Please try again.")
        }
        is ConnectError.WebViewError -> {
            // Report to crash reporting service
            reportToCrashlytics(error)
        }
        is ConnectError.UnknownError -> {
            // Generic error handling
            showMessage("An unexpected error occurred.")
        }
    }
}
```

## Testing

### Unit Testing Example

```kotlin
class ConnectSDKTest {
    @Test
    fun testEnvironmentValues() {
        assertEquals("production", Environment.PRODUCTION.toWebValue())
        assertEquals("sandbox", Environment.SANDBOX.toWebValue())
    }

    @Test
    fun testThemeValues() {
        assertEquals("light", Theme.LIGHT.toWebValue())
        assertEquals("dark", Theme.DARK.toWebValue())
        assertEquals("system", Theme.SYSTEM.toWebValue())
    }

    @Test
    fun testDepositEventParsing() {
        val json = JSONObject().apply {
            put("depositId", "123")
            put("success", true)
            put("amount", "100.00")
        }

        val event = DepositEvent.fromJSON(json)
        assertEquals("123", event.depositId)
        assertTrue(event.success)
        assertEquals("100.00", event.amount)
    }
}
```

## Common Issues

### Issue: JWT Token Expired

```kotlin
override fun onError(error: ConnectError) {
    if (error is ConnectError.AuthenticationError) {
        // Refresh JWT and restart session
        refreshJWTToken { newJwt ->
            startNewSession(newJwt)
        }
    }
}
```

### Issue: Activity Lifecycle

```kotlin
// Always cancel session in onDestroy
override fun onDestroy() {
    super.onDestroy()
    authSession?.cancel()
}
```

### Issue: Multiple Sessions

```kotlin
// Ensure only one session at a time
private var currentSession: ConnectAuthSession? = null

fun startNewSession(jwt: String) {
    // Cancel existing session
    currentSession?.cancel()

    // Create new session
    currentSession = ConnectSDK.configureAuth(...)
    currentSession?.present(this)
}
```

## Integration with Backend

```kotlin
// Example: Get JWT from your backend
private fun fetchJWTToken(callback: (String?) -> Unit) {
    // Using Retrofit or similar
    apiService.getConnectJWT().enqueue(object : Callback<JWTResponse> {
        override fun onResponse(call: Call<JWTResponse>, response: Response<JWTResponse>) {
            val jwt = response.body()?.token
            callback(jwt)
        }

        override fun onFailure(call: Call<JWTResponse>, t: Throwable) {
            Log.e(TAG, "Failed to fetch JWT", t)
            callback(null)
        }
    })
}

// Example: Send deposit to backend
private fun sendDepositToBackend(deposit: DepositEvent) {
    val request = DepositRequest(
        depositId = deposit.depositId,
        assetId = deposit.assetId,
        networkId = deposit.networkId,
        amount = deposit.amount
    )

    apiService.submitDeposit(request).enqueue(object : Callback<DepositResponse> {
        override fun onResponse(call: Call<DepositResponse>, response: Response<DepositResponse>) {
            Log.d(TAG, "Deposit submitted successfully")
        }

        override fun onFailure(call: Call<DepositResponse>, t: Throwable) {
            Log.e(TAG, "Failed to submit deposit", t)
        }
    })
}
```
