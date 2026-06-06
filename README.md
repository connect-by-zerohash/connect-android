# ConnectSDK for Android

![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-purple.svg)
![Platform](https://img.shields.io/badge/Platform-Android%205.0%2B-green.svg)
[![JitPack](https://jitpack.io/v/connect-by-zerohash/connect-android.svg)](https://jitpack.io/#connect-by-zerohash/connect-android)

A Kotlin SDK for seamless integration with the [Connect](https://docs.zerohash.com/docs/connect) product.

## Features

- **Secure OAuth2/OIDC Authentication** - Industry-standard authentication flow via Chrome Custom Tabs
- **Theme Support** - Light, dark, and system theme options to match your app's design
- **Android 5.0+ Support** - Compatible with API 21 (Android 5.0) and later versions
- **Real-time Event Callbacks** - Comprehensive event handling for the deposit flow
- **Multiple Environments** - Support for both sandbox and production environments
- **Type-Safe** - Full Kotlin type safety with comprehensive error handling

## Requirements

- Android 5.0+ (API 21)
- Kotlin 1.9+
- Gradle 8.2+

## Installation

### JitPack

Add the JitPack repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Then add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.connect-by-zerohash:connect-android:1.0.0")
}
```

### Local Development

Clone the repository and include it as a module:

```kotlin
// settings.gradle.kts
include(":connectsdk")
project(":connectsdk").projectDir = File("path/to/connect-android/connectsdk")

// app/build.gradle.kts
dependencies {
    implementation(project(":connectsdk"))
}
```

## Getting Started

### Import the SDK

```kotlin
import xyz.connect.sdk.ConnectSDK
```

### Obtain JWT Token

Before using the SDK, you'll need to obtain a JWT token from your backend. This token authenticates your app with the Connect platform.

> 📘 **Note:** For detailed instructions on obtaining JWT tokens, please refer to [your company's authentication documentation](#).

### Basic Configuration

```kotlin
// Configure the auth session
val authSession = ConnectSDK.configureAuth(
    jwt = "your-jwt-token",
    environment = Environment.PRODUCTION,  // or Environment.SANDBOX for testing
    theme = Theme.SYSTEM                   // matches device theme
)
```

## Usage

### Basic Example

Here's a simple example to get you started with ConnectSDK:

```kotlin
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import xyz.connect.sdk.ConnectSDK
import xyz.connect.sdk.auth.AuthCallbacks

class MainActivity : AppCompatActivity() {

    private var authSession: ConnectAuthSession? = null

    fun authenticateButtonClicked() {
        // Configure auth session with minimal setup
        authSession = ConnectSDK.configureAuth(
            jwt = "your-jwt-token",
            callbacks = object : AuthCallbacks {
                override fun onDeposit(event: DepositEvent) {
                    // Handle successful deposit
                    if (event.success) {
                        println("Deposit successful!")
                        println("Deposit ID: ${event.depositId ?: "N/A"}")
                    }
                }
            }
        )

        // Present the authentication UI
        authSession?.present(this)
    }
}
```

### Complete Example

Here's a comprehensive example showcasing all available features and callbacks:

```kotlin
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import xyz.connect.sdk.ConnectSDK
import xyz.connect.sdk.Environment
import xyz.connect.sdk.Theme
import xyz.connect.sdk.auth.AuthCallbacks
import xyz.connect.sdk.auth.DepositEvent
import xyz.connect.sdk.ConnectError
import xyz.connect.sdk.GenericEvent

class AuthenticationActivity : AppCompatActivity() {

    private var authSession: ConnectAuthSession? = null

    fun startAuthentication() {
        // Configure callbacks for all events
        val callbacks = object : AuthCallbacks {
            override fun onClose() {
                // Handle session closure
                Log.d("Connect", "Authentication session closed")
                handleSessionClosed()
            }

            override fun onError(error: ConnectError) {
                // Handle errors with detailed information
                Log.e("Connect", "Error occurred: ${error.message}")
                Log.e("Connect", "Error code: ${error.code}")

                // Access additional error data if needed
                error.data["additionalInfo"]?.let {
                    Log.e("Connect", "Additional info: $it")
                }

                showErrorAlert(error.message)
            }

            override fun onEvent(event: GenericEvent) {
                // Handle generic events
                Log.d("Connect", "Event received: ${event.type}")

                // Access event data using convenience methods
                event.getString("userId")?.let { userId ->
                    Log.d("Connect", "User ID: $userId")
                }

                event.getBool("verified")?.let { isVerified ->
                    Log.d("Connect", "Verification status: $isVerified")
                }
            }

            override fun onDeposit(deposit: DepositEvent) {
                // Handle deposit completion
                Log.d("Connect", "Deposit event received")

                if (deposit.success) {
                    // Deposit was successful
                    Log.d("Connect", "✅ Deposit successful!")
                    Log.d("Connect", "Deposit ID: ${deposit.depositId ?: "N/A"}")
                    Log.d("Connect", "Asset: ${deposit.assetId ?: "N/A"}")
                    Log.d("Connect", "Network: ${deposit.networkId ?: "N/A"}")
                    Log.d("Connect", "Amount: ${deposit.amount ?: "N/A"}")

                    handleSuccessfulDeposit(deposit)
                } else {
                    // Deposit failed or is pending
                    Log.d("Connect", "⏳ Deposit status: ${deposit.status ?: "unknown"}")
                }

                // Access raw data if needed
                Log.d("Connect", "Raw deposit data: ${deposit.rawData}")
            }
        }

        // Configure auth session with all options
        authSession = ConnectSDK.configureAuth(
            jwt = getJWTToken(),
            environment = if (isDevelopment()) Environment.SANDBOX else Environment.PRODUCTION,
            theme = getUserPreferredTheme(),
            callbacks = callbacks
        )

        // Present the authentication UI
        authSession?.present(this)?.let { session ->
            Log.d("Connect", "Session ID: ${session.id}")
            Log.d("Connect", "Session created at: ${session.createdAt}")

            // You can check session state
            if (session.isActive) {
                Log.d("Connect", "Session is active")
            }
        }
    }

    // Helper methods
    private fun getJWTToken(): String {
        // Fetch JWT from your backend
        return "your-jwt-token"
    }

    private fun isDevelopment(): Boolean {
        return BuildConfig.DEBUG
    }

    private fun getUserPreferredTheme(): Theme {
        // Return user's theme preference
        // This example returns system theme
        return Theme.SYSTEM
    }

    private fun handleSessionClosed() {
        // Clean up after session closes
        authSession = null
    }

    private fun handleSuccessfulDeposit(deposit: DepositEvent) {
        // Navigate to success screen or update UI
        // This is called when a deposit is successfully processed
    }

    private fun showErrorAlert(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    // Cancel the session if needed
    fun cancelAuthentication() {
        authSession?.cancel()
        authSession = null
    }
}
```

## API Reference

### ConnectSDK

The main entry point for the SDK.

#### `configureAuth(jwt, environment, theme, callbacks)`

Configures an authentication session that can be presented later.

**Parameters:**
- `jwt: String` - JWT token for authentication
- `environment: Environment` - Target environment (default: `Environment.PRODUCTION`)
  - `Environment.SANDBOX` - For testing and development
  - `Environment.PRODUCTION` - For production use
- `theme: Theme` - UI theme (default: `Theme.SYSTEM`)
  - `Theme.LIGHT` - Light theme
  - `Theme.DARK` - Dark theme
  - `Theme.SYSTEM` - Follows device theme
- `callbacks: AuthCallbacks` - Event callbacks (default: empty callbacks)

**Returns:** `ConnectAuthSession` - A configured session ready to be presented

### ConnectAuthSession

Manages the authentication session lifecycle.

#### `present(activity)`

Presents the authentication UI from the specified activity.

**Parameters:**
- `activity: Activity` - The activity to present from

**Returns:** `ConnectSession?` - The active session if presentation succeeds

#### `cancel()`

Cancels the authentication session if it's active.

#### `isActive`

A boolean property indicating whether the session is currently active.

### Types and Enums

#### Environment

```kotlin
enum class Environment {
    SANDBOX,    // Testing environment
    PRODUCTION  // Production environment
}
```

#### Theme

```kotlin
enum class Theme {
    LIGHT,   // Light theme
    DARK,    // Dark theme
    SYSTEM   // Follows device theme setting
}
```

#### AuthCallbacks

Interface containing all available callback handlers:

```kotlin
interface AuthCallbacks {
    fun onClose()
    fun onError(error: ConnectError)
    fun onEvent(event: GenericEvent)
    fun onDeposit(event: DepositEvent)
}
```

## Callbacks and Events

See all callback payloads in our [documentation](https://docs.zerohash.com/docs/front-end-implementation-guide#shared-callbacks)

### onDeposit

Called when a deposit event occurs during the authentication flow.

**DepositEvent Properties:**

```kotlin
deposit.depositId    // String? - Unique deposit identifier
deposit.status       // String? - Current deposit status
deposit.success      // Boolean - Whether the deposit was successful
deposit.assetId      // String? - Asset ticker (BTC, ETH, USDC, etc.)
deposit.networkId    // String? - Network/chain used
deposit.amount       // String? - Amount deposited
deposit.rawData      // JSONObject? - Raw event data
```

### onError

Called when an error occurs during the authentication process.

**ConnectError Properties:**

```kotlin
error.code        // String - Error code
error.message     // String - Human-readable error message
error.data        // Map<String, Any> - Additional error details
error.timestamp   // Long - When the error occurred (Unix timestamp)
```

### onEvent

Called for generic events during the authentication flow. [Documentation](https://docs.zerohash.com/docs/front-end-implementation-guide#shared-callbacks)

**GenericEvent Properties:**

```kotlin
event.type                    // String - Event type identifier
event.data                    // Map<String, Any> - Event data
event.getString("key")        // String? - Get string value
event.getInt("key")           // Int? - Get integer value
event.getBool("key")          // Boolean? - Get boolean value
event.getObject("key")        // Map<String, Any>? - Get nested object
event.getDouble("key")        // Double? - Get double value
```

### onClose

Called when the authentication session is closed by the user or programmatically.

## Themes and Customization

### Setting Theme

The SDK supports three theme options:

```kotlin
// Light theme
val session = ConnectSDK.configureAuth(jwt = token, theme = Theme.LIGHT)

// Dark theme
val session = ConnectSDK.configureAuth(jwt = token, theme = Theme.DARK)

// System theme (default) - automatically matches device settings
val session = ConnectSDK.configureAuth(jwt = token, theme = Theme.SYSTEM)
```

### Theme Behavior

- **`Theme.SYSTEM`** - Automatically switches between light and dark based on device settings
- **`Theme.LIGHT`** - Forces light theme regardless of device settings
- **`Theme.DARK`** - Forces dark theme regardless of device settings

The theme affects the WebView content, status bar, and navigation appearance.

## Contact

For additional support or questions about the Connect platform:
- [Technical Support](https://zerohash.com/)
- [Documentation](https://docs.zerohash.com/docs/connect)
