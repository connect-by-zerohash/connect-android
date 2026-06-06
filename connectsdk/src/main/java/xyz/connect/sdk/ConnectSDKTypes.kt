package xyz.connect.sdk

import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Identifies the Connect app type
 */
enum class ConnectApp {
    AUTH,
    RECOVERY,
    WITHDRAWAL
}

/**
 * Theme configuration for the SDK UI
 */
enum class Theme {
    LIGHT,
    DARK,
    SYSTEM;

    fun toWebValue(): String = when (this) {
        LIGHT -> "light"
        DARK -> "dark"
        SYSTEM -> "system"
    }
}

/**
 * Environment configuration (sandbox or production)
 */
enum class Environment {
    SANDBOX,
    PRODUCTION;

    fun toWebValue(): String = when (this) {
        SANDBOX -> "sandbox"
        PRODUCTION -> "production"
    }

    /**
     * Host of the embedded Connect web app for this environment.
     *
     * Single source of truth shared by the session base-URL builders and the
     * WebView's trusted-origin check.  Sandbox traffic is routed to
     * `sdk.sandbox.connect.xyz`; production traffic to `sdk.connect.xyz`.
     *
     */
    internal val webHost: String
        get() = when (this) {
            SANDBOX -> "sdk.sandbox.connect.xyz"
            PRODUCTION -> "sdk.connect.xyz"
        }
}

/**
 * Represents an active Connect session with lifecycle management.
 *
 * F-012: [_isActive] uses [AtomicBoolean] and [close] uses compareAndSet so
 * that concurrent calls to [close] are safe — the cleanup branch executes
 * exactly once regardless of how many threads call it simultaneously.
 */
class ConnectSession internal constructor(
    val id: String = UUID.randomUUID().toString(),
    val app: ConnectApp
) {
    // F-012: AtomicBoolean prevents data races on isActive
    private val _isActive = AtomicBoolean(true)
    private var onCloseCallback: (() -> Unit)? = null

    /**
     * Check if the session is currently active.
     */
    fun isActive(): Boolean = _isActive.get()

    /**
     * Close the session and trigger cleanup.
     *
     * Thread-safe: the callback is invoked at most once even if [close] is
     * called concurrently from multiple threads.
     */
    fun close() {
        // compareAndSet returns true only for the one thread that flips true→false
        if (_isActive.compareAndSet(true, false)) {
            onCloseCallback?.invoke()
        }
    }

    internal fun setOnCloseCallback(callback: () -> Unit) {
        onCloseCallback = callback
    }
}

/**
 * Comprehensive error types for Connect SDK
 */
sealed class ConnectError : Exception() {
    data class NetworkError(override val message: String) : ConnectError()
    data class AuthenticationError(override val message: String) : ConnectError()
    data class ConfigurationError(override val message: String) : ConnectError()
    data class WebViewError(override val message: String) : ConnectError()
    data class OAuthError(override val message: String) : ConnectError()
    data class UnknownError(override val message: String) : ConnectError()

    companion object {
        /**
         * Convert web error codes to ConnectError types
         */
        fun fromWebError(code: String?, message: String): ConnectError {
            return when (code) {
                "NETWORK_ERROR" -> NetworkError(message)
                "AUTH_ERROR", "INVALID_TOKEN" -> AuthenticationError(message)
                "CONFIG_ERROR" -> ConfigurationError(message)
                "OAUTH_ERROR" -> OAuthError(message)
                else -> UnknownError(message)
            }
        }
    }
}

/**
 * Base callback interface for all Connect apps
 */
interface AppCallbacks {
    /**
     * Called when the session is closed by user or programmatically
     */
    fun onClose()

    /**
     * Called when an error occurs during the session
     */
    fun onError(error: ConnectError)

    /**
     * Called for generic events from the web application
     */
    fun onEvent(event: GenericEvent)
}

/**
 * Internal protocol for handling callbacks with raw data
 */
internal interface CallbackHandler {
    fun handleClose()
    fun handleError(code: String?, message: String, data: JSONObject?)
    fun handleEvent(type: String, data: JSONObject?)
    fun handleDeposit(data: JSONObject?) {}
    fun handleWithdrawal(data: JSONObject?) {}
}

/**
 * Generic event wrapper with convenience accessors
 */
data class GenericEvent(
    val type: String,
    val data: JSONObject?
) {
    /**
     * Get a string value from the event data
     */
    fun getString(key: String): String? = data?.optString(key)

    /**
     * Get an integer value from the event data
     */
    fun getInt(key: String): Int? = data?.optInt(key)

    /**
     * Get a boolean value from the event data
     */
    fun getBool(key: String): Boolean? = data?.optBoolean(key)

    /**
     * Get a nested object from the event data
     */
    fun getObject(key: String): JSONObject? = data?.optJSONObject(key)

    /**
     * Get a double value from the event data
     */
    fun getDouble(key: String): Double? = data?.optDouble(key)
}
