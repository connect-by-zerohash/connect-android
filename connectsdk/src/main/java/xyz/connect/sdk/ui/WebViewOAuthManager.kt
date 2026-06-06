package xyz.connect.sdk.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import xyz.connect.sdk.BuildConfig
import xyz.connect.sdk.auth.OAuthHandler

/**
 * Manages external navigation and OAuth flows.
 *
 * Routes to:
 * - handleOAuthFlow(): Uses OAuthHandler for OAuth authentication
 * - openInExternalBrowser(): Opens external links in browser
 *
 * Security hardening:
 * - F-010: Only https (and optionally http) URL schemes are permitted when
 *   opening a URL in the external browser.  Dangerous schemes such as
 *   javascript:, file://, content:// and intent:// are rejected.
 * - F-007: URLs are not logged in release builds.
 */
class WebViewOAuthManager(
    private val activity: Activity,
    private val oauthHandler: OAuthHandler
) {
    companion object {
        private const val TAG = "WebViewOAuthManager"

        /**
         * F-010: Allowlist of URL schemes that may be opened in the external browser.
         * Everything else (javascript:, file://, content://, intent://, etc.) is rejected.
         */
        internal val ALLOWED_SCHEMES = setOf("https", "http")

        /**
         * Returns true if [url] has an allowed scheme.
         *
         * Uses [java.net.URI] instead of [android.net.Uri] so this pure helper
         * can be called from JVM unit tests without Android framework stubs.
         * The [openInExternalBrowser] caller still uses [android.net.Uri] when
         * constructing the actual Intent after this gate passes.
         */
        internal fun isAllowedUrl(url: String): Boolean {
            if (url.isBlank()) return false
            return try {
                val scheme = java.net.URI(url).scheme?.lowercase() ?: return false
                scheme in ALLOWED_SCHEMES
            } catch (e: Exception) {
                false
            }
        }
    }

    interface Delegate {
        fun onOAuthSuccess(connectionId: String?)
        fun onOAuthError(error: String)
        fun onOAuthCancel()
    }

    var delegate: Delegate? = null

    /**
     * Handle navigation based on mobile target.
     */
    fun handleNavigation(url: String, mobileTarget: String?) {
        when (mobileTarget) {
            "oauth" -> handleOAuthFlow(url)
            "external" -> openInExternalBrowser(url)
            else -> {
                Log.w(TAG, "Unknown mobileTarget: $mobileTarget")
                openInExternalBrowser(url)
            }
        }
    }

    private fun handleOAuthFlow(url: String) {
        // F-007: Do not log the full OAuth URL
        if (BuildConfig.DEBUG) Log.d(TAG, "Starting OAuth flow")

        oauthHandler.startOAuthFlow(url, object : OAuthHandler.OAuthCallback {
            override fun onSuccess(connectionId: String?) {
                // F-007: Connection ID not logged in release builds
                if (BuildConfig.DEBUG) Log.d(TAG, "OAuth success")
                delegate?.onOAuthSuccess(connectionId)
            }

            override fun onError(error: String) {
                Log.e(TAG, "OAuth error: $error")
                delegate?.onOAuthError(error)
            }

            override fun onCancel() {
                if (BuildConfig.DEBUG) Log.d(TAG, "OAuth cancelled")
                delegate?.onOAuthCancel()
            }
        })
    }

    /**
     * Open URL in external browser.
     *
     * F-010: Validates scheme against [ALLOWED_SCHEMES] before dispatching the
     * intent.  Unsafe schemes (javascript:, file://, intent://, etc.) are
     * rejected and reported as an error to the delegate.
     */
    private fun openInExternalBrowser(url: String) {
        // F-010: Scheme validation
        if (!isAllowedUrl(url)) {
            Log.w(TAG, "Blocked navigation to URL with disallowed scheme")
            delegate?.onOAuthError("Navigation blocked: URL scheme not permitted")
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity.startActivity(intent)
            if (BuildConfig.DEBUG) Log.d(TAG, "Opened external URL")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open external URL", e)
            delegate?.onOAuthError("Failed to open URL: ${e.message}")
        }
    }

    /**
     * Handle OAuth callback intent.
     */
    fun handleOAuthCallback(intent: Intent?): Boolean {
        return oauthHandler.handleCallback(intent)
    }
}
