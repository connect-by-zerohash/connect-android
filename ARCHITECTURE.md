# Connect SDK for Android - Architecture Documentation

## Overview

The Connect SDK for Android is a Kotlin-based wrapper that provides native Android integration for the Connect Auth web platform. This document details the architecture, design patterns, and implementation decisions.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        Host App                              │
│                  (Your Android Application)                  │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ ConnectSDK.configureAuth()
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    ConnectAuthSession                        │
│              (Session Lifecycle Manager)                     │
│   - Stores JWT, environment, theme                           │
│   - Creates WebViewActivity via Intent                       │
│   - Manages session state                                    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ Intent with extras
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    WebViewActivity                           │
│              (Main WebView Container)                        │
│   - Hosts WebView with JavaScript enabled                   │
│   - Coordinates three managers                               │
│   - Handles theme and status bar                             │
└──────────┬──────────────┬──────────────┬────────────────────┘
           │              │              │
           │              │              │
           ▼              ▼              ▼
┌──────────────┐ ┌─────────────┐ ┌─────────────────┐
│   Loading    │ │  Message    │ │     OAuth       │
│   Manager    │ │  Handler    │ │    Manager      │
└──────────────┘ └──────┬──────┘ └────────┬────────┘
                        │                  │
                        │ @JavascriptInterface  │ Chrome Custom Tabs
                        ▼                  ▼
            ┌────────────────────────────────┐
            │   Connect Web Auth Platform    │
            │   (https://sdk.connect.xyz)    │
            └────────────────┬───────────────┘
                             │
                             ▼
                   ┌──────────────────┐
                   │ OAuth Providers  │
                   │  (Google, etc)   │
                   └──────────────────┘
```

## Core Components

### 1. ConnectSDK (Public API)

**Location**: `ConnectSDK.kt`

**Responsibility**: Single entry point for SDK configuration

**Pattern**: Factory/Builder pattern

```kotlin
object ConnectSDK {
    fun configureAuth(
        jwt: String,
        environment: Environment = Environment.PRODUCTION,
        theme: Theme = Theme.SYSTEM,
        callbacks: AuthCallbacks
    ): ConnectAuthSession
}
```

**Design Decision**: Static object (Kotlin `object`) provides simple, stateless API without requiring initialization.

### 2. ConnectAuthSession (Session Manager)

**Location**: `auth/ConnectAuthSession.kt`

**Responsibility**: Manages authentication session lifecycle

**Key Features**:
- Validates JWT before presentation
- Creates Intent for WebViewActivity
- Prevents multiple presentations
- Provides session state management

**Pattern**: Session facade pattern

```kotlin
class ConnectAuthSession {
    fun present(activity: Activity)
    fun cancel()
    fun isActive(): Boolean
}
```

**Design Decision**: Use Intent-based communication to decouple session management from UI presentation, following Android best practices.

### 3. WebViewActivity (Main Container)

**Location**: `ui/WebViewActivity.kt`

**Responsibility**: Hosts WebView and coordinates managers

**Architecture**: Composite manager pattern

```kotlin
class WebViewActivity : AppCompatActivity(),
    WebViewMessageHandler.Delegate,
    WebViewOAuthManager.Delegate,
    WebViewLoadingManager.Delegate {

    private lateinit var webView: WebView
    private lateinit var messageHandler: WebViewMessageHandler
    private lateinit var oauthManager: WebViewOAuthManager
    private lateinit var loadingManager: WebViewLoadingManager
}
```

**Design Decision**: Delegate pattern separates concerns:
- **LoadingManager**: UI state (loading/content/error)
- **MessageHandler**: JS↔Kotlin communication
- **OAuthManager**: External navigation and OAuth flows

This prevents a "god object" and makes each component testable in isolation.

### 4. WebViewMessageHandler (JS Bridge)

**Location**: `ui/WebViewMessageHandler.kt`

**Responsibility**: Bidirectional JavaScript↔Kotlin communication

**Pattern**: Message bus pattern

**Inbound Messages** (JavaScript → Kotlin):
```javascript
window.NativeAndroid.postMessage(JSON.stringify({
    type: "page-ready",
    data: {}
}))
```

**Outbound Messages** (Kotlin → JavaScript):
```kotlin
webView.evaluateJavascript("window.postMessage({type: 'jwt', data: {...}}, '*')")
```

**Message Types**:
- `page-ready`: Web loaded, send JWT/config
- `content-ready`: Hide loading, show content
- `navigate`: Handle navigation (OAuth, in-app, external)
- `close`: Close session
- `error`: Error event
- `event`: Generic event
- `deposit`: Deposit completion

**Design Decision**: Use `@JavascriptInterface` for JavaScript→Kotlin and `evaluateJavascript` for Kotlin→JavaScript. This is the standard Android approach and works reliably across all Android versions (API 21+).

### 5. WebViewOAuthManager (OAuth Handler)

**Location**: `ui/WebViewOAuthManager.kt`

**Responsibility**: Manage OAuth flows and external navigation

**Pattern**: Strategy pattern (different handling per target)

**OAuth Implementation**: Chrome Custom Tabs

```kotlin
class WebViewOAuthManager {
    fun handleNavigation(url: String, mobileTarget: String?) {
        when (mobileTarget) {
            "oauth" -> handleOAuthFlow(url)
            "external" -> openInExternalBrowser(url)
        }
    }
}
```

**Design Decision**: Use Chrome Custom Tabs instead of WebView for OAuth because:
- Preserves SSO cookies
- Trusted UI (user sees real browser)
- Better security
- Standard Android practice

**OAuth Callback Flow**:
1. Custom Tabs launched with OAuth URL
2. User authenticates
3. Redirect to `connectsdk-oauth://callback`
4. Intent filter catches callback
5. Parse parameters from URL
6. Send result to WebView via MessageHandler

### 6. WebViewLoadingManager (Loading UI)

**Location**: `ui/WebViewLoadingManager.kt`

**Responsibility**: Animated loading state

**Features**:
- Three-dot animation (yellow gradient)
- Fade and translation effects
- Theme-aware colors
- Error state with retry
- Smooth transitions

**Pattern**: State machine (loading → content → error)

**Design Decision**: Custom animated view instead of ProgressBar provides branded experience consistent with iOS version.

## Design Patterns

### 1. Delegate Pattern

Used throughout for loose coupling:

```kotlin
interface WebViewMessageHandler.Delegate {
    fun onContentReady()
    fun onNavigate(url: String, mobileTarget: String?)
    fun onClose()
}
```

**Benefits**:
- Decouples components
- Makes testing easier
- Follows Android best practices

### 2. Callback Pattern

For async event handling:

```kotlin
interface AuthCallbacks {
    fun onClose()
    fun onError(error: ConnectError)
    fun onEvent(event: GenericEvent)
    fun onDeposit(event: DepositEvent)
}
```

**Benefits**:
- Familiar Android pattern
- Type-safe
- Easy to extend

### 3. Factory Pattern

For session creation:

```kotlin
object ConnectSDK {
    fun configureAuth(...): ConnectAuthSession
}
```

**Benefits**:
- Simple API
- Hides construction complexity
- Allows future customization

### 4. Composite Manager Pattern

WebViewActivity coordinates specialized managers:

```kotlin
class WebViewActivity {
    private val messageHandler: WebViewMessageHandler
    private val oauthManager: WebViewOAuthManager
    private val loadingManager: WebViewLoadingManager
}
```

**Benefits**:
- Single Responsibility Principle
- Each manager independently testable
- Clear separation of concerns

## Threading Model

### Main Thread Operations

All UI operations run on main thread:

```kotlin
webView.post {
    delegate?.onContentReady()
}
```

### JavaScript Bridge Thread

`@JavascriptInterface` methods may be called on background thread, so we post to main thread:

```kotlin
@JavascriptInterface
fun postMessage(message: String) {
    webView.post {
        // Process on main thread
    }
}
```

**Design Decision**: Always post UI updates to main thread to avoid crashes. Android UI toolkit is not thread-safe.

## Data Flow

### Initialization Flow

```
User calls ConnectSDK.configureAuth()
    ↓
Creates ConnectAuthSession
    ↓
User calls session.present(activity)
    ↓
Creates Intent with extras (JWT, environment, theme, sessionId)
    ↓
Stores callbackHandler in static map (keyed by sessionId)
    ↓
Starts WebViewActivity
    ↓
WebViewActivity retrieves callbackHandler
    ↓
Creates WebView + managers
    ↓
Loads Connect web platform URL
```

### Message Flow (Web → Native)

```
JavaScript calls window.NativeAndroid.postMessage()
    ↓
@JavascriptInterface method receives JSON string
    ↓
Parse JSON to extract type and data
    ↓
Route based on type:
    - navigate → WebViewOAuthManager
    - error/event/deposit → AuthCallbackHandler
    - close → Close activity
    ↓
Callback handler converts to typed events
    ↓
Host app receives typed callback
```

### OAuth Flow

```
Web sends navigate message with mobileTarget="oauth"
    ↓
WebViewOAuthManager.handleOAuthFlow()
    ↓
OAuthHandler creates Chrome Custom Tabs Intent
    ↓
User authenticates in Chrome Custom Tabs
    ↓
OAuth provider redirects to connectsdk-oauth://callback
    ↓
Android Intent filter catches callback
    ↓
WebViewActivity.onNewIntent()
    ↓
OAuthHandler parses URL parameters
    ↓
WebViewOAuthManager.onOAuthSuccess/Error
    ↓
MessageHandler sends result back to web
    ↓
Web continues flow with connectionId
```

## Error Handling

### Error Type Hierarchy

```kotlin
sealed class ConnectError {
    data class NetworkError(message: String)
    data class AuthenticationError(message: String)
    data class ConfigurationError(message: String)
    data class WebViewError(message: String)
    data class OAuthError(message: String)
    data class UnknownError(message: String)
}
```

**Design Decision**: Sealed class provides exhaustive when statements and type-safe error handling.

### Error Propagation

```
Web error → WebViewMessageHandler
    ↓
AuthCallbackHandler.handleError()
    ↓
ConnectError.fromWebError(code, message)
    ↓
AuthCallbacks.onError(error)
    ↓
Host app handles error
```

## Theme Support

### Theme Detection

```kotlin
private fun shouldUseDarkMode(theme: String): Boolean {
    return when (theme) {
        "dark" -> true
        "light" -> false
        "system" -> {
            val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            nightMode == Configuration.UI_MODE_NIGHT_YES
        }
        else -> false
    }
}
```

### Theme Application

- **Status bar**: Color + light/dark icons
- **WebView background**: Dark (#111113) or white
- **Loading UI**: Adapted text and button colors
- **Navigation**: Theme-aware toolbar

**Design Decision**: Match system conventions for seamless integration with host app.

## Memory Management

### Callback Handler Lifecycle

```kotlin
companion object {
    private val callbackHandlers = mutableMapOf<String, AuthCallbackHandler>()

    fun setCallbackHandler(sessionId: String, handler: AuthCallbackHandler)
    private fun getCallbackHandler(sessionId: String): AuthCallbackHandler?
}
```

**Design Decision**: Use static map with sessionId key to pass callbacks across Activity boundaries. Remove from map when retrieved to prevent memory leaks.

### WebView Cleanup

```kotlin
override fun onDestroy() {
    super.onDestroy()
    loadingManager.stopAnimation()
    oauthHandler.clear()
    webView.destroy()
}
```

**Design Decision**: Explicitly clean up WebView resources to prevent memory leaks.

## Security Considerations

### JWT Handling

- JWT passed via Intent extras (in-process)
- Not logged or persisted
- Validated before use

### OAuth Security

- Chrome Custom Tabs (not WebView) for OAuth
- Custom URL scheme with specific host
- Parameters parsed and validated
- No persistence of OAuth tokens

### WebView Security

- JavaScript enabled only for Connect domain
- Mixed content handled appropriately
- No file access enabled
- UserAgent not modified (anti-fingerprinting)

## Comparison: iOS vs Android

| Aspect | iOS (Swift) | Android (Kotlin) | Rationale |
|--------|-------------|------------------|-----------|
| **JS Bridge** | WKScriptMessageHandler | @JavascriptInterface | Platform standard |
| **OAuth** | ASWebAuthenticationSession | Chrome Custom Tabs | Best practice for each platform |
| **Container** | UIViewController + UINavigationController | Activity | Different navigation models |
| **Threading** | @MainActor | runOnUiThread() | Different concurrency models |
| **Package** | Swift Package Manager | Gradle/Maven | Platform tooling |
| **Callbacks** | Closure-based | Interface-based | Idiomatic to each language |

## Future Enhancements

### Potential Improvements

1. **Jetpack Compose UI**: Migrate loading UI to Compose
2. **Coroutines**: Use Kotlin Coroutines for async operations
3. **ViewModel**: Add ViewModel for better lifecycle management
4. **Navigation Component**: Use Navigation Component instead of raw Intents
5. **Kotlin Multiplatform**: Share business logic with iOS
6. **WebSocket Support**: Real-time communication alternative to polling
7. **Offline Support**: Cache and offline operation handling
8. **Analytics**: Built-in event tracking
9. **Error Recovery**: Automatic retry mechanisms
10. **Testing**: Comprehensive unit and UI tests

## Testing Strategy

### Unit Tests

- Environment/Theme enum value mapping
- Error type conversion
- Event parsing (DepositEvent, GenericEvent)
- URL parameter parsing

### Integration Tests

- WebView loading
- JavaScript bridge communication
- OAuth callback handling
- Theme application

### UI Tests

- Complete auth flow
- Error handling
- Loading states
- Theme switching

## Performance Considerations

### WebView Performance

- Hardware acceleration enabled
- Appropriate cache mode
- Minimal JavaScript injection

### Memory Usage

- Single WebView instance
- Proper cleanup in onDestroy
- No memory leaks from callbacks

### Startup Time

- Lazy initialization of components
- Minimal work in onCreate
- Fast Intent-based navigation

## Conclusion

The Connect SDK for Android provides a robust, production-ready wrapper for the Connect Auth platform. The architecture emphasizes:

- **Separation of concerns** through manager pattern
- **Type safety** with sealed classes and interfaces
- **Platform conventions** following Android best practices
- **Maintainability** with clear component boundaries
- **Testability** through delegation and dependency injection
- **Consistency** mirroring iOS architecture where appropriate

This design allows for easy extension, thorough testing, and seamless integration into Android applications.
