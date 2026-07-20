package xyz.connect.sdk

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.connect.sdk.auth.OAuthHandler
import xyz.connect.sdk.ui.WebViewMessageHandler
import xyz.connect.sdk.ui.WebViewOAuthManager

/**
 * Unit tests for WebView security hardening.
 *
 * Covers:
 *   F-004 — postMessage uses exact origin, never wildcard '*'
 *   F-009 — JavaScript bridge origin validation (legacy @JavascriptInterface path)
 *   F-010 — External browser URL scheme allowlist
 *   F-003 — UUID validation on OAuth callback connectionId
 *   ConnectAllowList — host matching logic (port of iOS PR #24)
 *   Environment.webHost — sandbox vs production routing (port of iOS PR #24)
 */
class WebViewSecurityTest {

    // -------------------------------------------------------------------------
    // F-004: postMessage target origin
    // -------------------------------------------------------------------------

    @Test
    fun `TARGET_ORIGIN is not the wildcard asterisk`() {
        assertNotEquals(
            "postMessage target origin must not be '*'",
            "*",
            WebViewMessageHandler.TARGET_ORIGIN
        )
    }

    @Test
    fun `TARGET_ORIGIN is exactly the production SDK origin`() {
        assertEquals(
            "TARGET_ORIGIN must be the canonical SDK origin",
            "https://sdk.connect.xyz",
            WebViewMessageHandler.TARGET_ORIGIN
        )
    }

    // -------------------------------------------------------------------------
    // F-009: JavaScript bridge origin validation (legacy fallback path)
    // -------------------------------------------------------------------------

    private val prodHost = "sdk.connect.xyz"
    private val sandboxHost = "sdk.sandbox.connect.xyz"

    @Test
    fun `exact production origin is accepted`() {
        assertTrue(WebViewMessageHandler.isAllowedOrigin("https://sdk.connect.xyz", prodHost))
    }

    @Test
    fun `production origin with path is accepted`() {
        assertTrue(WebViewMessageHandler.isAllowedOrigin("https://sdk.connect.xyz/mobile/#auth", prodHost))
    }

    @Test
    fun `sandbox origin is accepted when sandbox host configured`() {
        assertTrue(WebViewMessageHandler.isAllowedOrigin("https://sdk.sandbox.connect.xyz", sandboxHost))
    }

    @Test
    fun `sandbox origin is rejected when production host configured`() {
        assertFalse(WebViewMessageHandler.isAllowedOrigin("https://sdk.sandbox.connect.xyz", prodHost))
    }

    @Test
    fun `production origin is rejected when sandbox host configured`() {
        assertFalse(WebViewMessageHandler.isAllowedOrigin("https://sdk.connect.xyz", sandboxHost))
    }

    @Test
    fun `other subdomain of connect xyz is rejected`() {
        assertFalse(WebViewMessageHandler.isAllowedOrigin("https://app.connect.xyz/path", prodHost))
    }

    @Test
    fun `null URL is rejected`() {
        assertFalse(WebViewMessageHandler.isAllowedOrigin(null, prodHost))
    }

    @Test
    fun `blank URL is rejected`() {
        assertFalse(WebViewMessageHandler.isAllowedOrigin("", prodHost))
    }

    @Test
    fun `attacker domain pretending to be subdomain is rejected`() {
        assertFalse(WebViewMessageHandler.isAllowedOrigin("https://evil.connect.xyz.attacker.com", prodHost))
    }

    @Test
    fun `attacker domain pretending to contain host is rejected`() {
        assertFalse(WebViewMessageHandler.isAllowedOrigin("https://connect.xyz.evil.com", prodHost))
    }

    @Test
    fun `http scheme is rejected — scheme must be https`() {
        assertFalse(WebViewMessageHandler.isAllowedOrigin("http://sdk.connect.xyz", prodHost))
    }

    @Test
    fun `unrelated host on https is rejected`() {
        assertFalse(WebViewMessageHandler.isAllowedOrigin("https://attacker.com", prodHost))
    }

    @Test
    fun `javascript scheme is rejected`() {
        assertFalse(WebViewMessageHandler.isAllowedOrigin("javascript:alert(1)", prodHost))
    }

    @Test
    fun `file scheme is rejected`() {
        assertFalse(WebViewMessageHandler.isAllowedOrigin("file:///data/local/tmp/payload.html", prodHost))
    }

    // -------------------------------------------------------------------------
    // Error payload wire contract: {errorCode, reason} (zerohash-sdk ErrorPayload).
    // A missing key must not surface as an empty message.
    // -------------------------------------------------------------------------

    @Test
    fun `errorCode and reason are read from the payload`() {
        val data = JSONObject().apply {
            put("errorCode", "auth_error")
            put("reason", "Your session expired, please try again.")
        }
        val (code, message) = WebViewMessageHandler.extractErrorCodeAndMessage(data)
        assertEquals("auth_error", code)
        assertEquals("Your session expired, please try again.", message)
    }

    @Test
    fun `missing reason falls back to Unknown error, not blank`() {
        val data = JSONObject().apply { put("errorCode", "unknown_error") }
        val (_, message) = WebViewMessageHandler.extractErrorCodeAndMessage(data)
        assertEquals("Unknown error", message)
    }

    @Test
    fun `null data yields null code and Unknown error message`() {
        val (code, message) = WebViewMessageHandler.extractErrorCodeAndMessage(null)
        assertNull(code)
        assertEquals("Unknown error", message)
    }

    // -------------------------------------------------------------------------
    // F-010: URL scheme allowlist for external browser navigation
    // -------------------------------------------------------------------------

    @Test
    fun `https URL is allowed`() {
        assertTrue(WebViewOAuthManager.isAllowedUrl("https://example.com/path"))
    }

    @Test
    fun `http URL is allowed`() {
        assertTrue(WebViewOAuthManager.isAllowedUrl("http://example.com/path"))
    }

    @Test
    fun `javascript scheme is blocked`() {
        assertFalse(WebViewOAuthManager.isAllowedUrl("javascript:alert(document.cookie)"))
    }

    @Test
    fun `file scheme is blocked`() {
        assertFalse(WebViewOAuthManager.isAllowedUrl("file:///etc/passwd"))
    }

    @Test
    fun `content scheme is blocked`() {
        assertFalse(WebViewOAuthManager.isAllowedUrl("content://com.android.contacts/contacts"))
    }

    @Test
    fun `intent scheme is blocked`() {
        assertFalse(WebViewOAuthManager.isAllowedUrl("intent://scan/#Intent;scheme=zxing;end"))
    }

    @Test
    fun `tel scheme is blocked`() {
        assertFalse(WebViewOAuthManager.isAllowedUrl("tel:+15550001234"))
    }

    @Test
    fun `sms scheme is blocked`() {
        assertFalse(WebViewOAuthManager.isAllowedUrl("sms:+15550001234"))
    }

    @Test
    fun `blank URL is blocked`() {
        assertFalse(WebViewOAuthManager.isAllowedUrl(""))
    }

    @Test
    fun `malformed URL is blocked`() {
        assertFalse(WebViewOAuthManager.isAllowedUrl("not a url at all !@#"))
    }

    // -------------------------------------------------------------------------
    // F-003: OAuth callback connectionId UUID validation
    // -------------------------------------------------------------------------

    @Test
    fun `valid lowercase UUID connectionId is accepted`() {
        assertTrue(OAuthHandler.isValidUuid("d3b07384-d9a8-4c1e-9a4e-5b1b6b1f4e9c"))
    }

    @Test
    fun `valid uppercase UUID connectionId is accepted`() {
        assertTrue(OAuthHandler.isValidUuid("D3B07384-D9A8-4C1E-9A4E-5B1B6B1F4E9C"))
    }

    @Test
    fun `non-UUID connectionId is rejected`() {
        assertFalse(OAuthHandler.isValidUuid("not-a-uuid"))
    }

    @Test
    fun `UUID with trailing junk is rejected`() {
        assertFalse(
            OAuthHandler.isValidUuid("d3b07384-d9a8-4c1e-9a4e-5b1b6b1f4e9c<script>")
        )
    }

    @Test
    fun `null connectionId is rejected`() {
        assertFalse(OAuthHandler.isValidUuid(null))
    }

    @Test
    fun `blank connectionId is rejected`() {
        assertFalse(OAuthHandler.isValidUuid("   "))
    }

    @Test
    fun `arbitrary error_description is collapsed to oauth_error`() {
        val sanitized = OAuthHandler.sanitizeOAuthError(
            "user-identifying error detail from provider"
        )
        assertEquals("oauth_error", sanitized)
    }

    @Test
    fun `standard OAuth error codes pass through`() {
        assertEquals("access_denied", OAuthHandler.sanitizeOAuthError("access_denied"))
        assertEquals("invalid_grant", OAuthHandler.sanitizeOAuthError("invalid_grant"))
    }

    @Test
    fun `null OAuth error becomes oauth_error`() {
        assertEquals("oauth_error", OAuthHandler.sanitizeOAuthError(null))
    }

    // -------------------------------------------------------------------------
    // ConnectAllowList — port of iOS PR #24 host-matching logic
    // -------------------------------------------------------------------------

    private val defaultList = ConnectAllowList.DEFAULT

    @Test
    fun `exact host entry is permitted`() {
        assertTrue(ConnectAllowList(listOf("connect.xyz")).contains("connect.xyz"))
    }

    @Test
    fun `subdomain of listed entry is permitted`() {
        assertTrue(defaultList.contains("sdk.connect.xyz"))
        assertTrue(defaultList.contains("sdk.sandbox.connect.xyz"))
    }

    @Test
    fun `sibling domain sharing suffix is rejected`() {
        // "evilconnect.xyz" must not match entry "connect.xyz"
        assertFalse(defaultList.contains("evilconnect.xyz"))
    }

    @Test
    fun `suffix-attack domain is rejected`() {
        // "connect.xyz.attacker.com" must not match entry "connect.xyz"
        assertFalse(defaultList.contains("connect.xyz.attacker.com"))
    }

    @Test
    fun `host matching is case-insensitive`() {
        assertTrue(ConnectAllowList(listOf("connect.xyz")).contains("SDK.Connect.XYZ"))
    }

    @Test
    fun `zerohash com domain is in default list`() {
        assertTrue(defaultList.contains("zerohash.com"))
        assertTrue(defaultList.contains("api.zerohash.com"))
    }

    @Test
    fun `unrelated domain is rejected`() {
        assertFalse(defaultList.contains("attacker.com"))
    }

    @Test
    fun `empty allow list rejects everything`() {
        val empty = ConnectAllowList(emptyList())
        assertFalse(empty.contains("sdk.connect.xyz"))
    }

    // -------------------------------------------------------------------------
    // Environment.webHost — port of iOS PR #24 environment-aware routing
    // -------------------------------------------------------------------------

    @Test
    fun `production environment uses sdk connect xyz`() {
        assertEquals("sdk.connect.xyz", Environment.PRODUCTION.webHost)
    }

    @Test
    fun `sandbox environment uses sdk sandbox connect xyz`() {
        assertEquals("sdk.sandbox.connect.xyz", Environment.SANDBOX.webHost)
    }

    @Test
    fun `production and sandbox hosts are different`() {
        assertNotEquals(Environment.PRODUCTION.webHost, Environment.SANDBOX.webHost)
    }
}
