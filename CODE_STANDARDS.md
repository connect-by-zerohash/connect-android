# ConnectSDK Android - Code Standards

> Essential coding standards for the ConnectSDK project.

---

## Naming Conventions

### Classes, Interfaces, Objects
Use **PascalCase**:

```kotlin
class WebViewMessageHandler { }
data class AuthCallbacks { }
enum class Environment { }
interface WebViewMessageHandlerDelegate { }
```

### Variables, Functions, Parameters
Use **camelCase**:

```kotlin
val jwt: String
var isActive: Boolean
fun sendMessageToPage(type: String, data: Map<String, Any>) { }
```

### Interface Naming
Listeners end with `Listener`:

```kotlin
interface WebViewMessageHandlerListener { }
```

### Interface Method Naming

**Pattern 1:** `onObjectAction`

```kotlin
fun onPageReady(handler: WebViewMessageHandler)
fun onClose(handler: WebViewMessageHandler)
```

**Pattern 2:** `onAction`

```kotlin
fun onError(data: Map<String, Any>, jsonString: String)
fun onEvent(event: GenericEvent)
```

### Callback Properties
Use lambda properties for callbacks:

```kotlin
var onClose: (() -> Unit)? = null
var onError: ((ErrorEvent) -> Unit)? = null
var onEvent: ((GenericEvent) -> Unit)? = null
```

---

## Access Control

### Use Explicit Access Modifiers
Always specify access level:

```kotlin
public class ConnectSDK { }           // Public API
internal class WebViewController { }  // Internal to module
private val jwt: String               // Private to file/class
```

### Order
1. `public` - External API
2. `internal` - Within module (default in Kotlin)
3. `protected` - Subclasses only
4. `private` - Within class/file

**Example:**

```kotlin
class MyClass {
    // Properties
    var listener: MyListener? = null
    private var webView: WebView? = null
    private val jwt: String
    private val theme: String
}
```

---

## Documentation

### Public APIs Must Be Documented
Use KDoc (`/**`) for documentation:

```kotlin
/**
 * Configures an auth session that can be presented later
 * Configure while fetching JWT, then present instantly for optimal UX
 *
 * @param jwt JWT token for authentication
 * @param environment Environment to use (defaults to production)
 * @param theme UI theme (defaults to system)
 * @param callbacks Optional callbacks for auth events
 * @return A ConnectAuthSession that can be presented when ready
 */
public fun configureAuth(
    jwt: String,
    environment: Environment = Environment.PRODUCTION,
    theme: Theme = Theme.SYSTEM,
    callbacks: AuthCallbacks = AuthCallbacks()
): ConnectAuthSession {
```

---

## Types

### Enums for Constants

```kotlin
enum class Environment(val value: String) {
    SANDBOX("sandbox"),
    PRODUCTION("production")
}

enum class Theme {
    LIGHT,
    DARK,
    SYSTEM
}
```

### Sealed Classes for State
Use sealed classes for restricted hierarchies:

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}
```

---

## File Organization

### Directory Structure

```
connectsdk/src/main/java/com/example/connectsdk/
├── core/
│   ├── ConnectSDK.kt
│   └── ConnectSDKTypes.kt
├── auth/
│   ├── AuthTypes.kt
│   ├── ConnectAuthSession.kt
│   └── OAuthHandler.kt
├── ui/
│   ├── components/
│   │   ├── WebViewMessageHandler.kt
│   │   ├── WebViewOAuthManager.kt
│   │   └── WebViewLoadingManager.kt
│   ├── activities/
│   │   ├── WebViewActivity.kt
│   │   └── SubActivity.kt
│   └── theme/
│       └── ThemeHelper.kt
└── internal/
    └── Constants.kt
```

### File Naming
- **PascalCase:** `WebViewMessageHandler.kt`
- **Match main type:** File name = main class/interface name
- **Helpers end with "Helper":** `ThemeHelper.kt`
- **Managers end with "Manager":** `WebViewOAuthManager.kt`
- **Types end with "Types":** `AuthTypes.kt`

---

## Kotlin Best Practices

### Use Data Classes
For simple data holders:

```kotlin
data class AuthCallbacks(
    val onSuccess: ((String) -> Unit)? = null,
    val onError: ((ErrorEvent) -> Unit)? = null
)
```

### Null Safety
Prefer safe calls and elvis operator:

```kotlin
val length = text?.length ?: 0
webView?.loadUrl(url)
```

### Use `when` Instead of `if-else` Chains

```kotlin
when (environment) {
    Environment.SANDBOX -> loadSandboxConfig()
    Environment.PRODUCTION -> loadProductionConfig()
}
```

### Companion Objects for Static Members

```kotlin
class ConnectSDK {
    companion object {
        private const val TAG = "ConnectSDK"

        fun getInstance(): ConnectSDK {
            // Implementation
        }
    }
}
```

---

## Quick Reference

| Element | Convention | Example |
|---------|-----------|---------|
| Class/Interface | PascalCase | `WebViewMessageHandler` |
| Variable/Function | camelCase | `sendMessageToPage` |
| Interface | PascalCase + Listener | `WebViewMessageHandlerListener` |
| Enum | PascalCase | `Environment` |
| Enum constant | UPPER_SNAKE_CASE | `PRODUCTION`, `SANDBOX` |
| Callback property | on + action | `onClose`, `onError` |
| Interface method | onAction | `onClose`, `onError` |
| File name | PascalCase.kt | `WebViewMessageHandler.kt` |
| Access control | Explicit | `public`, `internal`, `private` |
| Documentation | /** */ | `/** Description */` |
