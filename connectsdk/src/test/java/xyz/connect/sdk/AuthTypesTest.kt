package xyz.connect.sdk

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.connect.sdk.auth.AuthCallbackHandler
import xyz.connect.sdk.auth.AuthCallbacks
import xyz.connect.sdk.auth.DepositEvent

/**
 * Unit tests for [DepositEvent.fromJSON], which parses the `deposit` bridge
 * payload. The status and the account-matching validation both arrive nested and
 * `success` is derived rather than sent, so the derivation has to track the web
 * SDK's own success rule or hosts see a failure on a good deposit.
 */
class AuthTypesTest {

    private fun payload(
        statusValue: String? = null,
        validationStatus: String? = null,
        validationReason: String? = null
    ) = JSONObject()
        .put("depositId", "dep-1")
        .put("assetId", "USDC")
        .put("networkId", "ethereum")
        .put("amount", "3100")
        .apply {
            if (statusValue != null) {
                put(
                    "status",
                    JSONObject()
                        .put("value", statusValue)
                        .put("details", "some detail")
                        .put("occurredAt", "2026-09-01T00:00:00Z")
                )
            }
            if (validationStatus != null) {
                put(
                    "accountMatchingValidation",
                    JSONObject().put("status", validationStatus).apply {
                        if (validationReason != null) put("reason", validationReason)
                    }
                )
            }
        }

    // MARK: - DepositEvent.fromJSON parsing

    @Test
    fun `depositEvent fromJSON flattens the nested status`() {
        val event = DepositEvent.fromJSON(payload("CONFIRMED"))

        assertEquals("dep-1", event.depositId)
        assertEquals("CONFIRMED", event.status)
        assertEquals("USDC", event.assetId)
        assertEquals("ethereum", event.networkId)
        assertEquals("3100", event.amount)
    }

    @Test
    fun `depositEvent fromJSON handles null data`() {
        val event = DepositEvent.fromJSON(null)

        assertNull(event.depositId)
        assertNull(event.status)
        assertFalse(event.success)
        assertNull(event.assetId)
        assertNull(event.networkId)
        assertNull(event.amount)
        assertNull(event.accountMatchingStatus)
        assertNull(event.accountMatchingReason)
        assertNull(event.rawData)
    }

    /**
     * The status object is what carries the value; a flat `status` string is a
     * different payload shape and must not be read as one.
     */
    @Test
    fun `depositEvent fromJSON ignores a flat status string`() {
        val event = DepositEvent.fromJSON(JSONObject().put("status", "CONFIRMED"))

        assertNull(event.status)
        assertFalse(event.success)
    }

    // MARK: - success derivation

    /**
     * The regression this guards: the web SDK shows "Deposit successful" at
     * CONFIRMED on the default path, so comparing against "processed" alone
     * reported a failure for every successful deposit.
     */
    @Test
    fun `depositEvent fromJSON success true for confirmed status`() {
        assertTrue(DepositEvent.fromJSON(payload("CONFIRMED")).success)
        assertTrue(DepositEvent.fromJSON(payload("confirmed")).success)
    }

    /** Platforms running zerohash with auto-convert succeed at PROCESSED instead. */
    @Test
    fun `depositEvent fromJSON success true for processed status`() {
        assertTrue(DepositEvent.fromJSON(payload("PROCESSED")).success)
        assertTrue(DepositEvent.fromJSON(payload("processed")).success)
    }

    @Test
    fun `depositEvent fromJSON success false while non-terminal or failed`() {
        for (value in listOf("PENDING", "FAILED", "ACCOUNT_VALIDATION_FAILED")) {
            assertFalse(value, DepositEvent.fromJSON(payload(value)).success)
        }
    }

    @Test
    fun `depositEvent fromJSON success false when status absent`() {
        val event = DepositEvent.fromJSON(payload())

        assertFalse(event.success)
        assertNull(event.status)
    }

    /**
     * Web checks account matching before the status, so a deposit still
     * verifying shows the verifying screen even once the status reads terminal.
     */
    @Test
    fun `depositEvent fromJSON success false while account matching is pending`() {
        assertFalse(DepositEvent.fromJSON(payload("CONFIRMED", "PENDING")).success)
        assertFalse(DepositEvent.fromJSON(payload("PROCESSED", "PENDING")).success)
    }

    /** INVALID and ERROR both send web to the deposit-failed screen. */
    @Test
    fun `depositEvent fromJSON success false when account matching rejects`() {
        for (validation in listOf("INVALID", "ERROR")) {
            assertFalse(validation, DepositEvent.fromJSON(payload("CONFIRMED", validation)).success)
        }
    }

    @Test
    fun `depositEvent fromJSON success true when account matching is valid`() {
        assertTrue(DepositEvent.fromJSON(payload("CONFIRMED", "VALID")).success)
    }

    /**
     * Web falls through to the status check for any matching value it does not
     * recognise, rather than treating it as a failure.
     */
    @Test
    fun `depositEvent fromJSON success true for an unknown account matching status`() {
        assertTrue(DepositEvent.fromJSON(payload("CONFIRMED", "SKIPPED")).success)
    }

    // MARK: - account-matching fields

    @Test
    fun `depositEvent fromJSON keeps the account matching reason`() {
        val event = DepositEvent.fromJSON(payload("CONFIRMED", "INVALID", "name mismatch"))

        assertEquals("INVALID", event.accountMatchingStatus)
        assertEquals("name mismatch", event.accountMatchingReason)
    }

    @Test
    fun `depositEvent fromJSON tolerates an absent validation`() {
        val event = DepositEvent.fromJSON(payload("CONFIRMED"))

        assertNull(event.accountMatchingStatus)
        assertNull(event.accountMatchingReason)
    }

    // MARK: - AuthCallbackHandler

    @Test
    fun `authCallbackHandler handleDeposit fires onDeposit`() {
        var receivedEvent: DepositEvent? = null
        val callbacks = object : AuthCallbacks {
            override fun onClose() {}
            override fun onError(error: ConnectError) {}
            override fun onEvent(event: GenericEvent) {}
            override fun onDeposit(event: DepositEvent) { receivedEvent = event }
        }

        AuthCallbackHandler(callbacks).handleDeposit(payload("CONFIRMED"))

        assertEquals("dep-1", receivedEvent?.depositId)
        assertTrue(receivedEvent?.success == true)
    }

    @Test
    fun `authCallbackHandler handleError fires onError`() {
        var receivedError: ConnectError? = null
        val callbacks = object : AuthCallbacks {
            override fun onClose() {}
            override fun onError(error: ConnectError) { receivedError = error }
            override fun onEvent(event: GenericEvent) {}
            override fun onDeposit(event: DepositEvent) {}
        }

        AuthCallbackHandler(callbacks).handleError("NETWORK_ERROR", "No connection", null)

        assertTrue(receivedError is ConnectError.NetworkError)
    }
}
