package xyz.connect.sdk.ui

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONObject
import xyz.connect.sdk.BuildConfig
import xyz.connect.sdk.CallbackHandler
import java.net.URI

/**
 * JavaScript↔Kotlin communication bridge.
 *
 * Two reception paths, both routed through [dispatchMessage]:
 *
 * 1. **Preferred — WebMessageListener (API 24+ with up-to-date WebView).**
 *    [WebViewActivity] registers this handler via
 *    `WebViewCompat.addWebMessageListener` with [targetOrigin] as the only
 *    allowed-origin rule.  Origin filtering happens **per-frame** inside the
 *    WebView runtime, so a cross-origin iframe cannot reach the handler even
 *    if it tries to call `NativeAndroid.postMessage`.  Messages arriving on
 *    this path are flagged `originVerified=true`.
 *
 * 2. **Fallback — `@JavascriptInterface` (older WebView builds).**  The
 *    [postMessage] method below is exposed on the WebView via
 *    `addJavascriptInterface`.  In this mode the SDK can only validate the
 *    **top-frame** URL (cached on the main thread via [onPageLoaded]); a
 *    same-origin (or cross-origin) iframe inside the top page could
 *    technically call the bridge.  This path emits a warning at SDK init time.
 *
 * The [allowedHost] drives both the inbound origin check and the outbound
 * `window.postMessage` target — it is set per-session from
 * [Environment.webHost] so sandbox sessions talk to `sdk.sandbox.connect.xyz`
 * and production sessions talk to `sdk.connect.xyz`.
 *
 * Port of the environment-aware trusted-origin check.
 */
internal class WebViewMessageHandler(
    private val webView: WebView,
    private val jwt: String,
    private val environment: String,
    private val theme: String,
    private val callbackHandler: CallbackHandler,
    /** Trusted host for this session — derived from [Environment.webHost]. */
    private val allowedHost: String = "sdk.connect.xyz"
) {
    companion object {
        private const val TAG = "WebViewMessageHandler"
        const val INTERFACE_NAME = "NativeAndroid"

        /**
         * Fallback constant kept for backward-compat with ProGuard rules and
         * tests that don't supply an explicit environment.  Prefer
         * [WebViewMessageHandler.targetOrigin] for runtime use.
         */
        const val TARGET_ORIGIN = "https://sdk.connect.xyz"

        /**
         * Strict origin check used by the legacy [postMessage]
         * `@JavascriptInterface` path.
         *
         * Requires the page URL's scheme to be `https` and its host to match
         * [allowedHost] exactly (case-insensitive).  This check is also used in
         * unit tests to validate the allow-list logic in isolation.
         *
         * The WebMessageListener path does NOT call into here — the framework
         * enforces the same rule via `allowedOriginRules`.
         *
         * Port of `WebViewMessageHandler.userContentController` host check
         */
        internal fun isAllowedOrigin(url: String?, allowedHost: String): Boolean {
            if (url.isNullOrBlank()) return false
            return try {
                val uri = URI(url)
                val scheme = uri.scheme?.lowercase()
                val host = uri.host?.lowercase()
                scheme == "https" && host == allowedHost.lowercase()
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * The exact postMessage target origin for this session.
     * Used both for outbound messages and as the WebMessageListener origin rule.
     */
    val targetOrigin: String get() = "https://$allowedHost"

    interface Delegate {
        fun onContentReady()
        fun onNavigate(url: String, mobileTarget: String?)
        fun onSessionClose()
    }

    var delegate: Delegate? = null

    /**
     * Cached URL of the last fully-loaded top-frame page.
     *
     * Written by [onPageLoaded] (main thread, called from
     * [WebViewActivity.onPageFinished] and at session-start).  Read by the
     * legacy `@JavascriptInterface` path which runs on a background thread —
     * the @Volatile annotation guarantees cross-thread visibility.
     */
    @Volatile
    private var currentPageUrl: String? = null

    internal fun onPageLoaded(url: String?) {
        currentPageUrl = url
    }

    /**
     * Legacy `@JavascriptInterface` entry point. Reached only when the device's
     * WebView does not support per-frame origin filtering via
     * `WebViewCompat.addWebMessageListener` (otherwise [WebViewActivity] does
     * not register this object via `addJavascriptInterface`).
     *
     * Runs on a background thread.
     */
    @JavascriptInterface
    fun postMessage(message: String) {
        if (!isAllowedOrigin(currentPageUrl, allowedHost)) {
            Log.w(TAG, "Message rejected: origin not allowed")
            return
        }
        dispatchMessage(message)
    }

    /**
     * Entry point for messages whose origin has already been verified by the
     * WebView framework (WebMessageListener with allowedOriginRules).
     */
    internal fun handleVerifiedMessage(message: String) {
        dispatchMessage(message)
    }

    private fun dispatchMessage(message: String) {
        try {
            val json = JSONObject(message)
            val type = json.optString("type")
            val data = json.optJSONObject("data")

            if (BuildConfig.DEBUG) Log.d(TAG, "Received message type: $type")

            when (type) {
                "page-ready" -> handlePageReady()
                "content-ready" -> handleContentReady()
                "navigate" -> handleNavigate(data)
                "close" -> handleClose()
                "error" -> handleError(data)
                "event" -> handleEvent(data)
                "deposit" -> handleDeposit(data)
                "withdrawal" -> handleWithdrawal(data)
                else -> Log.w(TAG, "Unknown message type: $type")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message", e)
        }
    }

    private fun handlePageReady() {
        sendJWT()
        sendConfig()
    }

    private fun handleContentReady() {
        webView.post {
            delegate?.onContentReady()
        }
    }

    private fun handleNavigate(data: JSONObject?) {
        val url = data?.optString("url") ?: return
        val mobileTarget = data.optString("mobileTarget")

        webView.post {
            delegate?.onNavigate(url, mobileTarget)
        }
    }

    private fun handleClose() {
        webView.post {
            callbackHandler.handleClose()
            delegate?.onSessionClose()
        }
    }

    private fun handleError(data: JSONObject?) {
        val code = data?.optString("code")
        val message = data?.optString("message") ?: "Unknown error"

        webView.post {
            callbackHandler.handleError(code, message, data)
        }
    }

    private fun handleEvent(data: JSONObject?) {
        val eventType = data?.optString("type") ?: "unknown"

        webView.post {
            callbackHandler.handleEvent(eventType, data)
        }
    }

    private fun handleDeposit(data: JSONObject?) {
        webView.post {
            callbackHandler.handleDeposit(data)
        }
    }

    private fun handleWithdrawal(data: JSONObject?) {
        webView.post {
            callbackHandler.handleWithdrawal(data)
        }
    }

    private fun sendJWT() {
        val jwtMessage = JSONObject().apply {
            put("token", jwt)
            put("env", environment)
        }
        sendMessageToWeb("jwt", jwtMessage)
    }

    private fun sendConfig() {
        val configMessage = JSONObject().apply {
            put("theme", theme)
        }
        sendMessageToWeb("config", configMessage)
    }

    fun sendOAuthSuccess(connectionId: String?) {
        val oauthMessage = JSONObject().apply {
            put("success", true)
            connectionId?.let { put("connectionId", it) }
        }
        sendMessageToWeb("oauth-result", oauthMessage)
    }

    fun sendOAuthError(error: String) {
        val oauthMessage = JSONObject().apply {
            put("success", false)
            put("error", error)
        }
        sendMessageToWeb("oauth-result", oauthMessage)
    }

    /**
     * Outbound message — uses exact [targetOrigin] (never wildcard).
     */
    private fun sendMessageToWeb(type: String, data: JSONObject) {
        val message = JSONObject().apply {
            put("type", type)
            put("data", data)
        }

        val script = "window.postMessage(${message}, '$targetOrigin');"

        webView.post {
            webView.evaluateJavascript(script) { result ->
                if (BuildConfig.DEBUG) Log.d(TAG, "Sent message type: $type, result: $result")
            }
        }
    }
}
