package xyz.connect.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import xyz.connect.sdk.internal.Base64Util
import xyz.connect.sdk.internal.JwtValidator

/**
 * Unit tests for [JwtValidator] and [Base64Util].
 *
 * Covers F-006 remediation: structural check, expiry, and `alg: none` rejection.
 */
class JwtValidatorTest {

    private fun buildJwt(
        alg: String = "HS256",
        payload: String,
        signature: String = "sig"
    ): String {
        val headerJson = """{"alg":"$alg","typ":"JWT"}"""
        val h = Base64Util.urlSafeEncode(headerJson.toByteArray(Charsets.UTF_8))
        val p = Base64Util.urlSafeEncode(payload.toByteArray(Charsets.UTF_8))
        return "$h.$p.$signature"
    }

    private fun nowSeconds() = System.currentTimeMillis() / 1000L

    @Test
    fun `empty string is rejected`() {
        val result = JwtValidator.validate("")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("blank") == true)
    }

    @Test
    fun `blank string is rejected`() {
        val result = JwtValidator.validate("   ")
        assertTrue(result.isFailure)
    }

    @Test
    fun `single segment is rejected`() {
        val result = JwtValidator.validate("onlyone")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("3 segments") == true)
    }

    @Test
    fun `two segments are rejected`() {
        assertTrue(JwtValidator.validate("header.payload").isFailure)
    }

    @Test
    fun `four segments are rejected`() {
        assertTrue(JwtValidator.validate("a.b.c.d").isFailure)
    }

    @Test
    fun `segment with invalid base64url characters is rejected`() {
        assertTrue(JwtValidator.validate("hea der.pay load.sig").isFailure)
    }

    @Test
    fun `alg none is rejected`() {
        val jwt = buildJwt(alg = "none", payload = """{"sub":"u"}""")
        val r = JwtValidator.validate(jwt)
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull()?.message?.contains("none", ignoreCase = true) == true)
    }

    @Test
    fun `alg NONE uppercase is rejected`() {
        val jwt = buildJwt(alg = "NONE", payload = """{"sub":"u"}""")
        assertTrue(JwtValidator.validate(jwt).isFailure)
    }

    @Test
    fun `well-formed signed JWT without exp is accepted`() {
        val jwt = buildJwt(payload = """{"sub":"user123","iss":"connect"}""")
        assertTrue(JwtValidator.validate(jwt).isSuccess)
    }

    @Test
    fun `signed JWT with future exp is accepted`() {
        val futureExp = nowSeconds() + 3600
        val jwt = buildJwt(payload = """{"sub":"u","exp":$futureExp}""")
        assertTrue(JwtValidator.validate(jwt).isSuccess)
    }

    @Test
    fun `JWT inside skew tolerance is accepted`() {
        val skewExp = nowSeconds() - 20
        val jwt = buildJwt(payload = """{"sub":"u","exp":$skewExp}""")
        assertTrue(JwtValidator.validate(jwt).isSuccess)
    }

    @Test
    fun `expired JWT is rejected`() {
        val pastExp = nowSeconds() - 3600
        val jwt = buildJwt(payload = """{"sub":"u","exp":$pastExp}""")
        val r = JwtValidator.validate(jwt)
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull()?.message?.contains("expired") == true)
    }

    @Test
    fun `JWT with non-JSON payload is rejected`() {
        val h = Base64Util.urlSafeEncode("""{"alg":"HS256"}""".toByteArray())
        val p = Base64Util.urlSafeEncode("this-is-not-json".toByteArray())
        assertTrue(JwtValidator.validate("$h.$p.sig").isFailure)
    }

    // -------------------------------------------------------------------------
    // Base64Util — strict-decode behaviour
    // -------------------------------------------------------------------------

    @Test
    fun `Base64Util roundtrip is exact`() {
        val original = "hello-world-32-bytes-of-text-data!".toByteArray()
        val encoded = Base64Util.urlSafeEncode(original)
        assertTrue(original.contentEquals(Base64Util.urlSafeDecode(encoded)))
    }

    @Test
    fun `Base64Util rejects invalid character`() {
        try {
            Base64Util.urlSafeDecode("abc!def")
            fail("Expected IllegalArgumentException for invalid character")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Invalid Base64URL") == true)
        }
    }

    @Test
    fun `Base64Util rejects length mod 4 of 1`() {
        try {
            Base64Util.urlSafeDecode("abcde") // length 5 → mod 4 == 1
            fail("Expected IllegalArgumentException for invalid length")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Invalid Base64URL length") == true)
        }
    }

    @Test
    fun `Base64Util accepts empty input`() {
        assertEquals(0, Base64Util.urlSafeDecode("").size)
    }
}
