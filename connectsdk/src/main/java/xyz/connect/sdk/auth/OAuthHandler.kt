package xyz.connect.sdk.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import xyz.connect.sdk.BuildConfig

/**
 * Manages OAuth authentication flows using Chrome Custom Tabs.
 *
 * ## Architecture context
 *
 * zerohash uses a **confidential-client OAuth architecture**: the authorization
 * code → token exchange happens entirely server-side inside connection-service,
 * which holds the `client_secret`.  The Android SDK never sees the raw
 * authorization code; connection-service validates the OAuth callback, performs
 * the token exchange, and then redirects to the web app's oauth-callback page
 * which in turn fires `connectsdk-oauth://callback?connectionId=<uuid>`.
 *
 * ## Why PKCE is not added here (F-003)
 *
 * PKCE (RFC 7636) is designed for **public clients** that perform the token
 * exchange on-device (e.g., a native app calling the token endpoint directly).
 * It protects against an intercepted authorization code being exchanged by a
 * third party.  Here the token exchange is handled server-side by
 * connection-service using `client_secret` — a stronger form of client
 * authentication.  Adding a `code_challenge` from the SDK without a matching
 * `code_verifier` from connection-service would break the token exchange if the
 * OAuth provider enforces PKCE when the parameter is present.  Additionally,
 * the OAuth authorization URL is already constructed by connection-service,
 * which appends its own `state` token; prepending another `state` from the SDK
 * creates a duplicate parameter that breaks connection-service's own CSRF
 * protection.
 *
 * ## Why state is not validated here (F-005)
 *
 * CSRF protection via `state` is already implemented server-side:
 * connection-service generates a 256-bit random `state` token
 * (`GenerateState()` in `utils.go`), stores it against the pending connection
 * in MongoDB, and verifies it when the OAuth provider redirects back.  The
 * final redirect to `connectsdk-oauth://callback` only carries `connectionId`
 * — the `state` is fully consumed by the backend and never forwarded to the
 * SDK.  Validating a `state` that is never present in the callback would
 * unconditionally reject every OAuth response.
 *
 * ## Scheme-hijacking mitigation (F-003)
 *
 * The primary mitigation for custom-scheme hijacking is that the callback
 * only delivers a `connectionId` (not an authorization code or tokens).
 * Connection-service has already completed the token exchange before this
 * callback fires.  A malicious app that intercepts the intent receives only a
 * reference UUID; it cannot exchange that for credentials without proper
 * authentication against the zerohash API.
 *
 * Additional defense in depth from the post-merge review:
 *  - The exported intent-filter lives on [OAuthCallbackActivity], not on
 *    [xyz.connect.sdk.ui.WebViewActivity], so the JWT-bearing activity is not
 *    reachable from external apps.
 *  - This handler validates that `connectionId` is a well-formed UUID before
 *    forwarding it — a malicious app firing the intent cannot inject arbitrary
 *    strings.
 *  - Provider-supplied `error_description` is sanitised to the RFC 6749 §5.2
 *    error-code allowlist before being surfaced to integrators.
 *
 * F-007: OAuth URLs and connection IDs are only logged in debug builds.
 */
class OAuthHandler(
    private val activity: Activity
) {
    companion object {
        private const val TAG = "OAuthHandler"
        private const val OAUTH_CALLBACK_SCHEME = "connectsdk-oauth"
        private const val OAUTH_CALLBACK_HOST = "callback"
        const val REQUEST_CODE_OAUTH = 1001

        // RFC 4122 UUID — 8-4-4-4-12 hex chars
        private val UUID_REGEX = Regex(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        )

        // RFC 6749 §5.2 standard OAuth 2.0 error codes
        private val STANDARD_OAUTH_ERRORS = setOf(
            "invalid_request",
            "unauthorized_client",
            "access_denied",
            "unsupported_response_type",
            "invalid_scope",
            "server_error",
            "temporarily_unavailable",
            "invalid_grant",
            "unsupported_grant_type",
            "invalid_client"
        )

        /**
         * Whether [value] is a syntactically well-formed UUID. Exposed for unit tests.
         */
        internal fun isValidUuid(value: String?): Boolean {
            if (value.isNullOrBlank()) return false
            return UUID_REGEX.matches(value)
        }

        /**
         * Map a raw provider error to a known OAuth 2.0 error code, falling back
         * to a generic `oauth_error` if unrecognised.  Prevents provider-supplied
         * error_description text (which may include identifying detail) from
         * leaking through the SDK to the integrator's app.
         */
        internal fun sanitizeOAuthError(rawError: String?): String {
            if (rawError.isNullOrBlank()) return "oauth_error"
            val normalized = rawError.trim().lowercase()
            return if (normalized in STANDARD_OAUTH_ERRORS) normalized else "oauth_error"
        }
    }

    interface OAuthCallback {
        fun onSuccess(connectionId: String?)
        fun onError(error: String)
        fun onCancel()
    }

    private var currentCallback: OAuthCallback? = null

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Start OAuth flow with Chrome Custom Tabs.
     *
     * The authorization URL is passed through unchanged — connection-service
     * has already appended the `state` token and any other required parameters
     * to the URL before the web app forwarded it to the SDK via the
     * `navigate` message.
     *
     * @param url      OAuth authorization URL constructed by connection-service
     * @param callback Callback for OAuth results
     */
    fun startOAuthFlow(url: String, callback: OAuthCallback) {
        currentCallback = callback

        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()

            // F-007: Do NOT log the full OAuth URL in release builds
            if (BuildConfig.DEBUG) Log.d(TAG, "Starting OAuth flow")

            customTabsIntent.launchUrl(activity, Uri.parse(url))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start OAuth flow", e)
            currentCallback = null
            callback.onError("Failed to open OAuth URL: ${e.message}")
        }
    }

    /**
     * Handle OAuth callback from redirect.
     *
     * Validates scheme and host of the incoming intent, then extracts the
     * `connectionId` from query parameters or fragment.  Rejects non-UUID
     * connectionIds (F-003 review) and sanitises provider-supplied errors to
     * standard OAuth 2.0 codes (F-013 review).
     *
     * @param intent Intent containing the callback URL
     * @return true if the intent was handled, false otherwise
     */
    fun handleCallback(intent: Intent?): Boolean {
        val data = intent?.data ?: return false
        val callback = currentCallback ?: return false

        if (data.scheme != OAUTH_CALLBACK_SCHEME || data.host != OAUTH_CALLBACK_HOST) {
            return false
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "OAuth callback received")

        try {
            // Query parameters first (Authorization Code flow)
            var connectionId = data.getQueryParameter("connectionId")
            var error = data.getQueryParameter("error")

            // Fragment fallback (Implicit flow)
            if (connectionId == null && error == null) {
                val fragment = data.fragment
                if (fragment != null) {
                    val params = parseFragment(fragment)
                    connectionId = params["connectionId"]
                    error = params["error"]
                }
            }

            currentCallback = null

            when {
                error != null -> {
                    Log.e(TAG, "OAuth error received")
                    callback.onError(sanitizeOAuthError(error))
                }
                connectionId != null -> {
                    if (!isValidUuid(connectionId)) {
                        Log.e(TAG, "OAuth callback rejected: connectionId is not a valid UUID")
                        callback.onError("invalid_callback")
                    } else {
                        if (BuildConfig.DEBUG) Log.d(TAG, "OAuth success")
                        callback.onSuccess(connectionId)
                    }
                }
                else -> {
                    if (BuildConfig.DEBUG) Log.d(TAG, "OAuth cancelled or incomplete")
                    callback.onCancel()
                }
            }

            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse OAuth callback", e)
            currentCallback = null
            callback.onError("Failed to parse OAuth response: ${e.message}")
            return true
        }
    }

    /**
     * Clear current state (e.g., on activity destroy).
     */
    fun clear() {
        currentCallback = null
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun parseFragment(fragment: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        fragment.split("&").forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                params[parts[0]] = Uri.decode(parts[1])
            }
        }
        return params
    }
}
