# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **ConnectSDK for Android** - a Kotlin-based SDK that provides native Android integration for the Connect Auth web platform. The SDK wraps a WebView-based authentication flow with native Android components, OAuth handling via Chrome Custom Tabs, and bidirectional JavaScript-Kotlin communication.

**Package**: `xyz.connect.sdk`
**Min SDK**: Android API 21 (Android 5.0+)
**Language**: Kotlin 1.9+
**Build**: Gradle 8.2+ with JDK 17

## Essential Commands

### Build and Test
```bash
# Build the SDK module
./gradlew :connectsdk:build

# Run unit tests
./gradlew :connectsdk:test

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest

# Build everything
./gradlew build

# Clean build
./gradlew clean
```

### Demo App
```bash
# Build and install demo app on device/emulator
./gradlew installDebug

# Build debug APK (output: app/build/outputs/apk/debug/app-debug.apk)
./gradlew assembleDebug
```

### Development
```bash
# View logcat for debugging
adb logcat | grep -E "ConnectDemo|ConnectSDK"

# List connected devices
adb devices
```

## Architecture

### High-Level Flow

```
Host App → ConnectSDK.configureAuth() → ConnectAuthSession → [Intent] → WebViewActivity
                                                                              ↓
                                                    [Coordinates 3 managers] ←┘
                                                              ↓
                        ┌─────────────────────────┬────────────────────┬──────────────────┐
                        ↓                         ↓                    ↓                  ↓
              WebViewMessageHandler    WebViewOAuthManager    WebViewLoadingManager    WebView
              (JS↔Kotlin bridge)       (Chrome Custom Tabs)   (Loading UI)            (Connect Web)
```

### Core Components

1. **ConnectSDK** (`ConnectSDK.kt`) - Public API entry point (singleton object)
2. **ConnectAuthSession** (`auth/ConnectAuthSession.kt`) - Session lifecycle manager
3. **WebViewActivity** (`ui/WebViewActivity.kt`) - Main container that coordinates three managers
4. **WebViewMessageHandler** (`ui/WebViewMessageHandler.kt`) - JavaScript↔Kotlin message bus
   - Inbound: `@JavascriptInterface` for JS→Kotlin
   - Outbound: `evaluateJavascript()` for Kotlin→JS
   - Message types: `page-ready`, `content-ready`, `navigate`, `close`, `error`, `event`, `deposit`
5. **WebViewOAuthManager** (`ui/WebViewOAuthManager.kt`) - Handles OAuth via Chrome Custom Tabs
   - Uses custom URL scheme: `connectsdk-oauth://callback`
   - Intent filter catches OAuth redirect
6. **WebViewLoadingManager** (`ui/WebViewLoadingManager.kt`) - Animated loading state (3-dot animation)

### Key Design Patterns

- **Composite Manager Pattern**: WebViewActivity coordinates specialized managers (message, OAuth, loading)
- **Delegate Pattern**: Used throughout for loose coupling (see `WebViewMessageHandler.Delegate`)
- **Factory Pattern**: `ConnectSDK.configureAuth()` creates sessions
- **Intent-Based Communication**: Session config passed via Intent extras, callbacks via static map keyed by sessionId

### Thread Safety

All UI operations must run on main thread. JavaScript bridge methods (`@JavascriptInterface`) may be called on background threads, so always post to main thread:

```kotlin
@JavascriptInterface
fun postMessage(message: String) {
    webView.post {
        // Process on main thread
    }
}
```

### OAuth Implementation

OAuth uses Chrome Custom Tabs (NOT WebView) for security and SSO preservation:
1. Web sends navigate message with `mobileTarget="oauth"`
2. Chrome Custom Tabs launched with OAuth URL
3. User authenticates
4. Provider redirects to `connectsdk-oauth://callback`
5. Intent filter catches callback in WebViewActivity
6. Parse parameters and send result back to WebView via MessageHandler

**Exception — Coinbase automation login (Apple):** The scraping login WebView
(`automation/CoinbaseLoginActivity.kt`) supports "Sign in with Apple", which
Coinbase drives via a `window.open` popup on `appleid.apple.com` that returns
its result to `window.opener`. This popup is NOT handled by Chrome Custom Tabs
(a separate browser has no `window.opener` link back to the login page).
Instead the login WebView enables `setSupportMultipleWindows(true)` and its
`WebChromeClient.onCreateWindow` delegates to `automation/AuthPopupWindow.kt`,
a provider-agnostic host that presents the popup in a child WebView sharing the
process-wide cookie jar (the Android counterpart of iOS `PopupWebViewController`).
`AuthPopupWindow` carries no provider specifics, so a future Kraken login reuses
it unchanged.

## Code Standards

### Naming Conventions
- **Classes/Interfaces**: PascalCase (`WebViewMessageHandler`)
- **Functions/Variables**: camelCase (`sendMessageToPage`)
- **Constants**: UPPER_SNAKE_CASE
- **Interface Delegates**: End with `Delegate` or `Listener`
- **Callback methods**: Start with `on` (`onError`, `onClose`)

### Documentation
Public APIs MUST have KDoc comments with `@param`, `@return`, and usage examples.

### Access Modifiers
Always use explicit access modifiers:
- `public` - External API (SDK consumers)
- `internal` - Within SDK module (default in Kotlin)
- `private` - Within class/file

### File Organization
```
connectsdk/src/main/java/xyz/connect/sdk/
├── ConnectSDK.kt                    # Public API
├── ConnectSDKTypes.kt               # Public types/enums
├── auth/                            # Auth session management
│   ├── AuthTypes.kt
│   ├── ConnectAuthSession.kt
│   └── OAuthHandler.kt
├── ui/                              # UI components
│   ├── WebViewActivity.kt
│   ├── WebViewMessageHandler.kt
│   ├── WebViewOAuthManager.kt
│   └── WebViewLoadingManager.kt
└── internal/                        # Internal utilities
    └── Constants.kt
```

## Module Structure

- **connectsdk/** - Main SDK module (library)
- **app/** - Demo application showing SDK integration
- **Root build.gradle.kts** - Project-level configuration
- **settings.gradle.kts** - Includes both modules (`:connectsdk`, `:app`)

When making changes to the SDK, rebuild with `./gradlew :connectsdk:build`. The demo app depends on the SDK via `implementation(project(":connectsdk"))`.

## Important Implementation Details

### Callback Lifecycle
Callbacks are passed across Activity boundaries via a static map in `WebViewActivity`:
```kotlin
companion object {
    private val callbackHandlers = mutableMapOf<String, AuthCallbackHandler>()
}
```
The sessionId keys this map. Handlers are removed after retrieval to prevent memory leaks.

### WebView Security
- JavaScript enabled only for Connect domain
- No file access
- Uses Chrome Custom Tabs for OAuth (not WebView)
- JWT never logged or persisted

### Theme System
Three themes: `Theme.LIGHT`, `Theme.DARK`, `Theme.SYSTEM`
- Affects WebView background, status bar, and loading UI
- System theme reads from `Configuration.UI_MODE_NIGHT_MASK`

## Testing Strategy

Unit tests should cover:
- Environment/Theme enum value mapping
- Error type conversion
- Event parsing (DepositEvent, GenericEvent)
- URL parameter parsing

Integration tests should cover:
- WebView loading
- JavaScript bridge communication
- OAuth callback handling

No test files currently exist in the repository.

## Publishing

The SDK is configured for Maven publication in `connectsdk/build.gradle.kts`:
```kotlin
publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "xyz.connect"
            artifactId = "connect-sdk"
            version = "1.0.0"
        }
    }
}
```

## Additional Documentation

For deeper understanding, refer to:
- **ARCHITECTURE.md** - Detailed architecture documentation with diagrams and data flows
- **CODE_STANDARDS.md** - Comprehensive coding standards
- **README.md** - Public-facing SDK documentation with usage examples
- **CONTRIBUTING.md** - Contribution guidelines and development setup
