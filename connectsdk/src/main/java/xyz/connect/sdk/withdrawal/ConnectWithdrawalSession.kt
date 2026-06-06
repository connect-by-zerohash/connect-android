package xyz.connect.sdk.withdrawal

import android.app.Activity
import android.content.Intent
import android.util.Log
import xyz.connect.sdk.BuildConfig
import xyz.connect.sdk.ConnectAllowList
import xyz.connect.sdk.ConnectApp
import xyz.connect.sdk.ConnectError
import xyz.connect.sdk.ConnectSession
import xyz.connect.sdk.Environment
import xyz.connect.sdk.Theme
import xyz.connect.sdk.internal.JwtValidator
import xyz.connect.sdk.ui.WebViewActivity

/**
 * Manages the lifecycle of a withdrawal session.
 *
 * Responsible for:
 * - Validating JWT before presenting the session (F-006)
 * - Storing JWT, environment, theme, and callbacks
 * - Creating and launching WebViewActivity
 * - Cleaning up callback handler on activity-start failure (F-013)
 */
class ConnectWithdrawalSession internal constructor(
    private val jwt: String,
    private val environment: Environment,
    private val theme: Theme,
    private val allowList: ConnectAllowList = ConnectAllowList.DEFAULT,
    private val callbacks: WithdrawalCallbacks
) {
    private var session: ConnectSession? = null
    private var hasPresented = false

    companion object {
        private const val TAG = "ConnectWithdrawalSession"
        private const val PATH = "/mobile/#withdraw"
    }

    /**
     * Present the withdrawal session.
     *
     * @param activity The activity to launch from
     * @return The created ConnectSession, or null if already presented or JWT is invalid
     */
    fun present(activity: Activity): ConnectSession? {
        if (hasPresented) {
            Log.w(TAG, "Session already presented")
            return null
        }

        // F-006: Validate JWT structure and expiry before proceeding
        val validationResult = JwtValidator.validate(jwt)
        if (validationResult.isFailure) {
            val msg = validationResult.exceptionOrNull()?.message ?: "JWT validation failed"
            Log.e(TAG, "JWT validation failed: $msg")
            callbacks.onError(ConnectError.ConfigurationError(msg))
            return null
        }

        hasPresented = true

        val newSession = ConnectSession(app = ConnectApp.WITHDRAWAL)
        session = newSession

        newSession.setOnCloseCallback {
            callbacks.onClose()
        }

        val callbackHandler = WithdrawalCallbackHandler(callbacks)

        val url = "https://${environment.webHost}$PATH"

        val intent = Intent(activity, WebViewActivity::class.java).apply {
            putExtra(WebViewActivity.EXTRA_URL, url)
            putExtra(WebViewActivity.EXTRA_JWT, jwt)
            putExtra(WebViewActivity.EXTRA_ENVIRONMENT, environment.toWebValue())
            putExtra(WebViewActivity.EXTRA_THEME, theme.toWebValue())
            putExtra(WebViewActivity.EXTRA_SESSION_ID, newSession.id)
            putExtra(WebViewActivity.EXTRA_WEB_HOST, environment.webHost)
            putStringArrayListExtra(
                WebViewActivity.EXTRA_ALLOW_HOSTS,
                ArrayList(allowList.hosts)
            )
        }

        WebViewActivity.setCallbackHandler(newSession.id, callbackHandler)

        try {
            if (BuildConfig.DEBUG) Log.d(TAG, "Starting WebViewActivity")
            activity.startActivity(intent)
        } catch (e: Exception) {
            // F-013: Remove stale handler on activity-start failure
            WebViewActivity.removeCallbackHandler(newSession.id)
            Log.e(TAG, "Failed to start WebViewActivity", e)
            callbacks.onError(
                ConnectError.UnknownError("Failed to open Withdrawal: ${e.message}")
            )
        }

        return newSession
    }

    fun cancel() {
        session?.close()
        session = null
    }

    fun isActive(): Boolean = session?.isActive() ?: false
}
