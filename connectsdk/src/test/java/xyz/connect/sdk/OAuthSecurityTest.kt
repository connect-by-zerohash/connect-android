package xyz.connect.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.connect.sdk.internal.Base64Util
import java.security.MessageDigest

/**
 * Tests for the OAuth security infrastructure shared with the JWT validator.
 *
 * ## Why state/PKCE are not tested here
 *
 * zerohash uses a confidential-client OAuth architecture: connection-service
 * performs the authorization-code → token exchange server-side using
 * client_secret.  CSRF protection via `state` is implemented entirely inside
 * connection-service (GenerateState / FindConnectionByState in Go); the SDK
 * receives only a connectionId in the final callback and never sees the raw
 * authorization code or the state token.
 *
 * Adding SDK-level state or PKCE parameters would create duplicate `state`
 * query parameters (breaking connection-service's own CSRF check) and
 * unsatisfied code_challenge requirements that would fail the backend token
 * exchange.  The pen test findings F-003 and F-005 are mitigated at the
 * connection-service layer, not the mobile SDK layer.
 *
 * ## What is tested here
 *
 * [Base64Util] is used by both [xyz.connect.sdk.internal.JwtValidator] (payload
 * decode) and is the canonical Base64URL implementation in the SDK.  These
 * tests verify the encoding/decoding quality properties that both use sites
 * depend on, including the RFC 7636 Appendix B test vector.
 */
class OAuthSecurityTest {

    // -------------------------------------------------------------------------
    // Helper — mirrors the S256 challenge computation for test assertions
    // -------------------------------------------------------------------------

    private fun challengeFor(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64Util.urlSafeEncode(hash)
    }

    // -------------------------------------------------------------------------
    // Base64Util encoding quality (used by JwtValidator and PKCE if ever added)
    // -------------------------------------------------------------------------

    @Test
    fun `32 bytes encodes to 43 base64url characters without padding`() {
        // ceil(32 * 4/3) rounded down to nearest group, no '=' padding
        val encoded = Base64Util.urlSafeEncode(ByteArray(32))
        assertEquals(43, encoded.length)
    }

    @Test
    fun `encoded output contains only base64url characters`() {
        val bytes = java.security.SecureRandom().generateSeed(32)
        val encoded = Base64Util.urlSafeEncode(bytes)
        val base64UrlPattern = Regex("^[A-Za-z0-9_\\-]+$")
        assertTrue(
            "Base64URL output must not contain '+', '/', or '=' characters",
            base64UrlPattern.matches(encoded)
        )
    }

    @Test
    fun `urlSafeDecode is the inverse of urlSafeEncode`() {
        val original = java.security.SecureRandom().generateSeed(32)
        val roundTripped = Base64Util.urlSafeDecode(Base64Util.urlSafeEncode(original))
        assertTrue(original.contentEquals(roundTripped))
    }

    @Test
    fun `S256 challenge is deterministic for the same verifier`() {
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        assertEquals(challengeFor(verifier), challengeFor(verifier))
    }

    @Test
    fun `different inputs produce different S256 challenges`() {
        val c1 = challengeFor(Base64Util.urlSafeEncode(ByteArray(32) { 0x00 }))
        val c2 = challengeFor(Base64Util.urlSafeEncode(ByteArray(32) { 0xFF.toByte() }))
        assertFalse(c1 == c2)
    }

    /**
     * RFC 7636 Appendix B test vector — validates the SHA-256 + Base64URL
     * pipeline end-to-end against the published specification.
     *
     * verifier  = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
     * challenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
     */
    @Test
    fun `S256 pipeline matches RFC 7636 Appendix B test vector`() {
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val expectedChallenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
        assertEquals(expectedChallenge, challengeFor(verifier))
    }
}
