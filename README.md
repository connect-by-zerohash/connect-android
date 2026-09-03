# ConnectSDK for Android

![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-purple.svg)
![Platform](https://img.shields.io/badge/Platform-Android%205.0%2B-green.svg)
[![Maven Central](https://img.shields.io/maven-central/v/xyz.connect/connect-android)](https://central.sonatype.com/artifact/xyz.connect/connect-android)

A Kotlin SDK for seamless integration with the [Connect](https://docs.zerohash.com/docs/connect) product.

The SDK exposes three apps that can be presented from your Android application:

- **Auth** — onboarding, KYC, and deposit flow
- **Recovery** — account recovery flow with terminal withdrawal
- **Withdrawal** — standalone withdrawal flow

## Features

- **Three Connect apps** — Auth, Recovery, and Withdrawal exposed through a single SDK
- **Secure OAuth2/OIDC Authentication** — OAuth flows handled by Chrome Custom Tabs, with an SDK-owned callback receiver
- **Configurable host allow-list** — restrict the hosts the embedded WebView is allowed to navigate to or load resources from
- **Theme Support** — Light, dark, and system theme options to match your app's design
- **Real-time Event Callbacks** — Typed callbacks for each app flow
- **Multiple Environments** — Sandbox and production environments
- **Type-Safe** — Full Kotlin type safety with a sealed `ConnectError` hierarchy

## Requirements

- Android 5.0+ (API 21)
- Kotlin 1.9+
- Gradle 8.2+

## Installation

### Maven Central

The SDK is published to Maven Central as `xyz.connect:connect-android`. A
standard Android project already resolves it, so usually there is nothing to add
to `settings.gradle.kts`. If your repositories block does not list
`mavenCentral()` yet:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

Then add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("xyz.connect:connect-android:1.2.0")
}
```

Or with the Groovy DSL, in `build.gradle`:

```groovy
dependencies {
    implementation 'xyz.connect:connect-android:1.2.0'
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

### Obtain a JWT Token

Before presenting any of the apps, you'll need to obtain a JWT token from
your backend. This token authenticates the end user with the Connect
platform.

> **Note:** For detailed instructions on obtaining JWT tokens, please refer to the [Connect documentation](https://docs.zerohash.com/docs/connect).

### OAuth callback

OAuth flows are driven by Chrome Custom Tabs and return to the SDK via the
`connectsdk-oauth://callback` custom scheme. The intent filter that
receives the callback is declared in the SDK's own `AndroidManifest.xml`,
so **no additional manifest configuration is required in the host app**.

### (Optional) Configure the host allow-list

The SDK ships with a built-in allow-list that permits navigations and
resource loads to `connect.xyz`, `zerohash.com`, and their subdomains.
You can supply your own list — for example to add a partner-hosted domain,
or to limit the SDK to a subset of hosts — via `ConnectAllowList`. Host
matching is exact or via dot-suffix subdomain.

```kotlin
val allowList = ConnectAllowList(listOf(
    "connect.xyz",
    "zerohash.com",
    "partner.example.com"
))
```

If you don't pass `allowList`, the SDK uses `ConnectAllowList.DEFAULT`.

## Usage

### Auth

The Auth app handles onboarding, KYC, and the deposit flow. Use
`onDeposit` to react to deposit events.

```kotlin
import androidx.appcompat.app.AppCompatActivity
import xyz.connect.sdk.ConnectSDK
import xyz.connect.sdk.ConnectError
import xyz.connect.sdk.Environment
import xyz.connect.sdk.GenericEvent
import xyz.connect.sdk.Theme
import xyz.connect.sdk.auth.AuthCallbacks
import xyz.connect.sdk.auth.ConnectAuthSession
import xyz.connect.sdk.auth.DepositEvent

class AuthActivity : AppCompatActivity() {

    private var authSession: ConnectAuthSession? = null

    fun startAuthTapped() {
        val callbacks = object : AuthCallbacks {
            override fun onClose() { println("Auth closed") }

            override fun onError(error: ConnectError) {
                println("Auth error: ${error.message}")
            }

            override fun onEvent(event: GenericEvent) {
                println("Auth event: ${event.type}")
            }

            override fun onDeposit(deposit: DepositEvent) {
                if (deposit.success) {
                    println("Deposit ${deposit.depositId ?: "?"} processed")
                } else {
                    println("Deposit status: ${deposit.status ?: "unknown"}")
                }
            }
        }

        authSession = ConnectSDK.configureAuth(
            jwt = "your-jwt-token",
            environment = Environment.PRODUCTION,
            theme = Theme.SYSTEM,
            callbacks = callbacks
        )

        authSession?.present(this)
    }
}
```

### Recovery

The Recovery app drives the account-recovery experience and emits a
withdrawal event when the recovering user completes the terminal
withdrawal step.

```kotlin
import androidx.appcompat.app.AppCompatActivity
import xyz.connect.sdk.ConnectSDK
import xyz.connect.sdk.ConnectError
import xyz.connect.sdk.Environment
import xyz.connect.sdk.GenericEvent
import xyz.connect.sdk.Theme
import xyz.connect.sdk.recovery.ConnectRecoverySession
import xyz.connect.sdk.recovery.RecoveryCallbacks
import xyz.connect.sdk.withdrawal.WithdrawalEvent

class RecoveryActivity : AppCompatActivity() {

    private var recoverySession: ConnectRecoverySession? = null

    fun startRecoveryTapped() {
        val callbacks = object : RecoveryCallbacks {
            override fun onClose() { println("Recovery closed") }

            override fun onError(error: ConnectError) {
                println("Recovery error: ${error.message}")
            }

            override fun onEvent(event: GenericEvent) {
                println("Recovery event: ${event.type}")
            }

            override fun onWithdrawal(withdrawal: WithdrawalEvent) {
                if (withdrawal.success) {
                    println("Recovery withdrawal ${withdrawal.withdrawalId ?: "?"} processed")
                } else {
                    println("Recovery withdrawal status: ${withdrawal.status ?: "unknown"}")
                }
            }
        }

        recoverySession = ConnectSDK.configureRecovery(
            jwt = "your-jwt-token",
            environment = Environment.PRODUCTION,
            theme = Theme.SYSTEM,
            callbacks = callbacks
        )

        recoverySession?.present(this)
    }
}
```

### Withdrawal

The Withdrawal app is the standalone withdrawal flow. It shares the
`WithdrawalEvent` payload with Recovery.

```kotlin
import androidx.appcompat.app.AppCompatActivity
import xyz.connect.sdk.ConnectSDK
import xyz.connect.sdk.ConnectError
import xyz.connect.sdk.Environment
import xyz.connect.sdk.GenericEvent
import xyz.connect.sdk.Theme
import xyz.connect.sdk.withdrawal.ConnectWithdrawalSession
import xyz.connect.sdk.withdrawal.WithdrawalCallbacks
import xyz.connect.sdk.withdrawal.WithdrawalEvent

class WithdrawalActivity : AppCompatActivity() {

    private var withdrawalSession: ConnectWithdrawalSession? = null

    fun startWithdrawalTapped() {
        val callbacks = object : WithdrawalCallbacks {
            override fun onClose() { println("Withdrawal closed") }

            override fun onError(error: ConnectError) {
                println("Withdrawal error: ${error.message}")
            }

            override fun onEvent(event: GenericEvent) {
                println("Withdrawal event: ${event.type}")
            }

            override fun onWithdrawal(withdrawal: WithdrawalEvent) {
                if (withdrawal.success) {
                    println("Withdrawal ${withdrawal.withdrawalId ?: "?"} processed")
                    println("Asset: ${withdrawal.assetId ?: "N/A"}")
                    println("Network: ${withdrawal.networkId ?: "N/A"}")
                    println("Amount: ${withdrawal.amount ?: "N/A"}")
                } else {
                    println("Withdrawal status: ${withdrawal.status ?: "unknown"}")
                }
            }
        }

        withdrawalSession = ConnectSDK.configureWithdrawal(
            jwt = "your-jwt-token",
            environment = Environment.PRODUCTION,
            theme = Theme.SYSTEM,
            callbacks = callbacks
        )

        withdrawalSession?.present(this)
    }
}
```

## API Reference

### ConnectSDK

The main entry point for the SDK. All three configure methods follow the
same shape; only the callbacks interface and the returned session type
differ.

#### `configureAuth(jwt, environment, theme, allowList, callbacks)`

Configures an Auth session that can be presented later. Returns a
`ConnectAuthSession`.

#### `configureRecovery(jwt, environment, theme, allowList, callbacks)`

Configures a Recovery session that can be presented later. Returns a
`ConnectRecoverySession`.

#### `configureWithdrawal(jwt, environment, theme, allowList, callbacks)`

Configures a Withdrawal session that can be presented later. Returns a
`ConnectWithdrawalSession`.

**Shared parameters:**

| Parameter     | Type                                                            | Default                    | Description                                                        |
| ------------- | --------------------------------------------------------------- | -------------------------- | ------------------------------------------------------------------ |
| `jwt`         | `String`                                                        | —                          | JWT token authenticating the end user                              |
| `environment` | `Environment`                                                   | `Environment.PRODUCTION`   | `SANDBOX` or `PRODUCTION`                                          |
| `theme`       | `Theme`                                                         | `Theme.SYSTEM`             | `LIGHT`, `DARK`, or `SYSTEM`                                       |
| `allowList`   | `ConnectAllowList`                                              | `ConnectAllowList.DEFAULT` | Hosts the WebView may navigate to / load resources from            |
| `callbacks`   | `AuthCallbacks` / `RecoveryCallbacks` / `WithdrawalCallbacks`   | —                          | App-specific event callbacks                                       |

### Session types

All three session types (`ConnectAuthSession`, `ConnectRecoverySession`,
`ConnectWithdrawalSession`) share the same lifecycle:

#### `present(activity: Activity): ConnectSession?`

Presents the UI from the specified activity. Returns the created
`ConnectSession`, or `null` if the session has already been presented or
the JWT failed validation. When JWT validation fails, `onError` is invoked
with a `ConnectError.ConfigurationError`.

#### `cancel()`

Cancels the session if it is active, and triggers the `onClose` callback.

#### `isActive(): Boolean`

Returns `true` while the session is running.

### Types

#### Environment

```kotlin
enum class Environment {
    SANDBOX,     // Testing environment
    PRODUCTION   // Production environment
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

#### ConnectAllowList

```kotlin
class ConnectAllowList(val hosts: List<String>) {
    companion object {
        /** Default allow-list shipped with the SDK: connect.xyz + zerohash.com */
        val DEFAULT: ConnectAllowList
    }
}
```

Host matching is exact, or via dot-suffix subdomain — `connect.xyz`
matches `sdk.connect.xyz` but not `evilconnect.xyz`.

#### AuthCallbacks

```kotlin
interface AuthCallbacks : AppCallbacks {
    fun onDeposit(event: DepositEvent)
}
```

#### RecoveryCallbacks

```kotlin
interface RecoveryCallbacks : AppCallbacks {
    fun onWithdrawal(event: WithdrawalEvent) {}
}
```

#### WithdrawalCallbacks

```kotlin
interface WithdrawalCallbacks : AppCallbacks {
    fun onWithdrawal(event: WithdrawalEvent) {}
}
```

Each of the above extends the base `AppCallbacks`:

```kotlin
interface AppCallbacks {
    fun onClose()
    fun onError(error: ConnectError)
    fun onEvent(event: GenericEvent)
}
```

## Callbacks and Events

See all callback payloads in the
[Connect documentation](https://docs.zerohash.com/docs/front-end-implementation-guide#shared-callbacks).

### onDeposit (Auth only)

Called when a deposit event occurs during the Auth flow.

`onDeposit` is a **status, not an outcome**. It also fires while account matching
is verifying, and can arrive more than once for the same deposit, so read the
outcome off `success` rather than treating the call itself as completion.

```kotlin
deposit.depositId              // String? - Unique deposit identifier
deposit.status                 // String? - "CONFIRMED", "PROCESSED", "PENDING", "FAILED", ...
deposit.success                // Boolean - True at "CONFIRMED" or "PROCESSED", unless
                               //           account matching is PENDING/INVALID/ERROR
deposit.assetId                // String? - Asset ticker (BTC, ETH, USDC, etc.)
deposit.networkId              // String? - Network/chain used
deposit.amount                 // String? - Amount deposited
deposit.accountMatchingStatus  // String? - "PENDING", "VALID", "INVALID", "ERROR"
deposit.accountMatchingReason  // String? - Why account matching failed
deposit.rawData                // JSONObject? - Raw event data
```

A successful deposit reports `CONFIRMED` on most platforms and `PROCESSED` on
platforms running zerohash with auto-convert; both count as success.

### onWithdrawal (Recovery and Withdrawal)

Called when a withdrawal event occurs during the Recovery or Withdrawal flow.

```kotlin
withdrawal.withdrawalId  // String? - Unique withdrawal identifier
withdrawal.status        // String? - Current withdrawal status
withdrawal.success       // Boolean - True at "CONFIRMED" or "PROCESSED"
withdrawal.assetId       // String? - Asset ticker (BTC, ETH, USDC, etc.)
withdrawal.networkId     // String? - Network/chain used
withdrawal.amount        // String? - Amount withdrawn
withdrawal.rawData       // JSONObject? - Raw event data
```

### onError

Called when an error occurs during any of the flows. `ConnectError` is a
sealed class — branch on its subtype to react to specific failure modes.

```kotlin
sealed class ConnectError : Exception() {
    data class NetworkError(override val message: String) : ConnectError()
    data class AuthenticationError(override val message: String) : ConnectError()
    data class ConfigurationError(override val message: String) : ConnectError()
    data class WebViewError(override val message: String) : ConnectError()
    data class OAuthError(override val message: String) : ConnectError()
    data class UnknownError(override val message: String) : ConnectError()
}
```

### onEvent

Called for generic events during the flow. [Documentation](https://docs.zerohash.com/docs/front-end-implementation-guide#shared-callbacks).

```kotlin
event.type                    // String - Event type identifier
event.data                    // JSONObject? - Event data
event.getString("key")        // String? - Get string value
event.getInt("key")           // Int? - Get integer value
event.getBool("key")          // Boolean? - Get boolean value
event.getObject("key")        // JSONObject? - Get nested object
event.getDouble("key")        // Double? - Get double value
```

### onClose

Called when the session is closed by the user or programmatically via
`cancel()`.

## Themes and Customization

### Setting Theme

The SDK supports three theme options across all three apps:

```kotlin
// Light theme
ConnectSDK.configureAuth(jwt = token, theme = Theme.LIGHT, callbacks = callbacks)

// Dark theme
ConnectSDK.configureAuth(jwt = token, theme = Theme.DARK, callbacks = callbacks)

// System theme (default) — matches device settings
ConnectSDK.configureAuth(jwt = token, theme = Theme.SYSTEM, callbacks = callbacks)
```

### Theme Behavior

- **`Theme.SYSTEM`** — Automatically switches between light and dark based on device settings
- **`Theme.LIGHT`** — Forces light theme regardless of device settings
- **`Theme.DARK`** — Forces dark theme regardless of device settings

The theme applies to the WebView content, status bar, and navigation
appearance.

## Contact

For additional support or questions about the Connect platform:
- [Technical Support](https://zerohash.com/)
- [Documentation](https://docs.zerohash.com/docs/connect)

## License

Licensed under the zerohash Android Wrapper License — a proprietary license.
See [`LICENSE`](LICENSE) for the full terms. Questions: legal@zerohash.com.
