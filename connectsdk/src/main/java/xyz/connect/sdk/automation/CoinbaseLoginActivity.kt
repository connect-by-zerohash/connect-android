package xyz.connect.sdk.automation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CompletableDeferred
import java.net.URI

/**
 * SDK-owned visible WebView that renders the Coinbase login page — the Android
 * counterpart of iOS `presentModalWebView` (ModalViewController) for `auth.login`.
 *
 * The scraping WebView is owned by the SDK, NOT the host app (per the iOS model:
 * a separate automation WebView talks to Coinbase; the host app only triggers it
 * and communicates over the bridge).
 *
 * Parity with iOS `Coinbase.login` + `ModalViewController` + `ModalAutoClose`:
 * - **documentStart injection** ([loginModalJS] = hide-social + prefer-password):
 *   Android has no WKUserScript documentStart hook, so we inject on every
 *   [WebViewClient.onPageStarted] (and again on finish as a backstop). The
 *   scripts are idempotent and self-persist (CSS rule + MutationObserver), so
 *   they survive the login SPA's client-side re-renders.
 * - **passkey-only auto-close** ([passkeyOnlyJS]): polled every
 *   [PROBE_INTERVAL_MS]; after [PROBE_REQUIRED_HITS] consecutive positive reads
 *   the modal closes with outcome `passkey-only` (iOS `.conditionMet`).
 * - **timeout** ([TIMEOUT_MS] ceiling): closes with outcome `timeout`.
 * - **success / user-closed**: redirect to [SUCCESS_HOST] == success (iOS
 *   `successHosts`); closing before that == user-closed.
 *
 * Sign in with Apple is hidden by [loginModalJS] (unsupported on Android), so the
 * `appleid.apple.com` stay-open host iOS tolerates is never reached here.
 *
 * Never exported — launched only in-process from [present].
 */
class CoinbaseLoginActivity : AppCompatActivity() {

    private var webView: WebView? = null
    private var done = false

    private val handler = Handler(Looper.getMainLooper())
    private var probeHits = 0

    /** hide-social + prefer-password, read once (idempotent; re-injected per nav). */
    private val loginModalJS: String by lazy {
        asset("automation/auth-hide-social.js") + "\n" + asset("automation/auth-prefer-password.js")
    }
    private val passkeyOnlyJS: String by lazy { asset("automation/auth-passkey-only.js") }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val wv = WebView(this)
        webView = wv
        setContentView(wv)

        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            // Defense-in-depth: deny local-file/content access (defaults true
            // below API 30; minSdk 21). Only login.coinbase.com is loaded.
            allowFileAccess = false
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            // No popup support: every popup-based provider (Google/Apple/passkey)
            // is hidden by auth-hide-social.js, and the embedded modal has no
            // onCreateWindow handler. Leaving multi-window OFF (the default) means a
            // stray window.open navigates in-frame instead of dead-tapping a popup
            // we can't present. (iOS keeps popups for Apple, which it supports; we
            // don't, so we intentionally diverge here.)
        }

        // Shared, persistent cookies — the login session set here must be visible
        // to the offscreen status/balance runs (process-wide CookieManager).
        android.webkit.CookieManager.getInstance().setAcceptCookie(true)
        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)

        wv.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                // documentStart-equivalent: inject before/as the page renders so
                // unsupported buttons are hidden and Password 2FA auto-advances.
                view?.evaluateJavascript(loginModalJS, null)
                // Coinbase redirects to www.coinbase.com once authenticated.
                if (!done && hostOf(url) == SUCCESS_HOST) {
                    Log.d(TAG, "login success (redirect to $SUCCESS_HOST)")
                    finishWith("success")
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                // Backstop in case onPageStarted ran before documentElement existed.
                view?.evaluateJavascript(loginModalJS, null)
                // Also check success here (iOS checks on didFinish): covers a
                // success surfaced without a fresh onPageStarted host change.
                if (!done && hostOf(url) == SUCCESS_HOST) {
                    Log.d(TAG, "login success (finished on $SUCCESS_HOST)")
                    finishWith("success")
                }
            }
        }
        wv.webChromeClient = WebChromeClient()
        wv.loadUrl(LOGIN_URL)

        scheduleTimeout()
        startPasskeyOnlyProbe()
    }

    /**
     * Polls [passkeyOnlyJS] while the modal is open; closes with `passkey-only`
     * after [PROBE_REQUIRED_HITS] consecutive positive reads. Evaluates first
     * then re-posts, so the interval is the confirm gap between reads. A transient
     * half-rendered DOM resets the streak (mirrors iOS ModalAutoClose).
     */
    private fun startPasskeyOnlyProbe() {
        val tick = object : Runnable {
            override fun run() {
                if (done) return
                webView?.evaluateJavascript(passkeyOnlyJS) { result ->
                    if (done) return@evaluateJavascript
                    if (result == "true") {
                        probeHits++
                        if (probeHits >= PROBE_REQUIRED_HITS) {
                            Log.d(TAG, "passkey-only detected; closing modal")
                            finishWith("passkey-only")
                            return@evaluateJavascript
                        }
                    } else {
                        probeHits = 0
                    }
                    handler.postDelayed(this, PROBE_INTERVAL_MS)
                }
            }
        }
        handler.postDelayed(tick, PROBE_INTERVAL_MS)
    }

    /** Force-closes with `timeout` if the user neither completes nor dismisses. */
    private fun scheduleTimeout() {
        handler.postDelayed({
            if (!done) {
                Log.d(TAG, "login modal timed out after ${TIMEOUT_MS}ms")
                finishWith("timeout")
            }
        }, TIMEOUT_MS)
    }

    private fun finishWith(outcome: String) {
        if (done) return
        done = true
        handler.removeCallbacksAndMessages(null)
        pending?.complete(outcome)
        pending = null
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        // If the modal is torn down before a success redirect, the user backed out.
        if (!done) {
            done = true
            pending?.complete("user-closed")
            pending = null
        }
        webView?.destroy()
        webView = null
    }

    // Graceful: a missing/unreadable asset (packaging regression) injects empty JS
    // (a no-op) rather than crashing the modal from a WebViewClient callback.
    private fun asset(path: String): String =
        runCatching { assets.open(path).bufferedReader().use { it.readText() } }
            .getOrElse { Log.e(TAG, "missing automation asset: $path", it); "" }

    private fun hostOf(url: String?): String =
        runCatching { URI(url).host ?: "" }.getOrDefault("")

    companion object {
        private const val TAG = "ZHAutomation"
        const val LOGIN_URL = "https://login.coinbase.com/signin"
        private const val SUCCESS_HOST = "www.coinbase.com"

        // Mirror iOS: 300s modal ceiling; passkey-only probe every 100ms, 2 hits.
        private const val TIMEOUT_MS = 300_000L
        private const val PROBE_INTERVAL_MS = 100L
        private const val PROBE_REQUIRED_HITS = 2

        /**
         * In-flight login completion. One modal at a time (matches the UX); set by
         * [present], completed by the activity with the outcome string. ponytail:
         * a single global is fine — there's never more than one login modal up.
         */
        internal var pending: CompletableDeferred<String>? = null

        /** Launch the login modal and suspend until it closes; returns the outcome. */
        suspend fun present(activity: Activity): String {
            // One login modal at a time. auth.login is non-coalescable, so two
            // overlapping requests would otherwise clobber `pending` and strand the
            // first waiter. Mirrors the single-session withdraw guard.
            if (pending?.isCompleted == false) {
                throw PlatformException("login already in progress")
            }
            val deferred = CompletableDeferred<String>()
            pending = deferred
            activity.startActivity(Intent(activity, CoinbaseLoginActivity::class.java))
            return deferred.await()
        }
    }
}
