package xyz.connect.sdk

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.connect.sdk.withdrawal.WithdrawalCallbacks
import xyz.connect.sdk.withdrawal.WithdrawalCallbackHandler
import xyz.connect.sdk.withdrawal.WithdrawalEvent

class WithdrawalTypesTest {

    // MARK: - WithdrawalEvent.fromJSON tests

    @Test
    fun `withdrawalEvent fromJSON parses withdrawalId`() {
        val data = JSONObject().apply { put("withdrawalId", "wit-123") }
        val event = WithdrawalEvent.fromJSON(data)
        assertEquals("wit-123", event.withdrawalId)
    }

    @Test
    fun `withdrawalEvent fromJSON returns null withdrawalId when absent`() {
        val event = WithdrawalEvent.fromJSON(JSONObject())
        assertNull(event.withdrawalId)
    }

    @Test
    fun `withdrawalEvent fromJSON success true for processed status`() {
        val status = JSONObject().apply { put("value", "processed") }
        val data = JSONObject().apply { put("status", status) }
        val event = WithdrawalEvent.fromJSON(data)
        assertTrue(event.success)
        assertEquals("processed", event.status)
    }

    @Test
    fun `withdrawalEvent fromJSON success true for PROCESSED uppercase`() {
        val status = JSONObject().apply { put("value", "PROCESSED") }
        val data = JSONObject().apply { put("status", status) }
        val event = WithdrawalEvent.fromJSON(data)
        assertTrue(event.success)
    }

    @Test
    fun `withdrawalEvent fromJSON success false for pending status`() {
        val status = JSONObject().apply { put("value", "pending") }
        val data = JSONObject().apply { put("status", status) }
        val event = WithdrawalEvent.fromJSON(data)
        assertFalse(event.success)
        assertEquals("pending", event.status)
    }

    @Test
    fun `withdrawalEvent fromJSON success false when status absent`() {
        val event = WithdrawalEvent.fromJSON(JSONObject())
        assertFalse(event.success)
        assertNull(event.status)
    }

    @Test
    fun `withdrawalEvent fromJSON parses assetId`() {
        val data = JSONObject().apply { put("assetId", "BTC") }
        val event = WithdrawalEvent.fromJSON(data)
        assertEquals("BTC", event.assetId)
    }

    @Test
    fun `withdrawalEvent fromJSON returns null assetId when absent`() {
        val event = WithdrawalEvent.fromJSON(JSONObject())
        assertNull(event.assetId)
    }

    @Test
    fun `withdrawalEvent fromJSON parses networkId`() {
        val data = JSONObject().apply { put("networkId", "bitcoin") }
        val event = WithdrawalEvent.fromJSON(data)
        assertEquals("bitcoin", event.networkId)
    }

    @Test
    fun `withdrawalEvent fromJSON parses amount`() {
        val data = JSONObject().apply { put("amount", "0.5") }
        val event = WithdrawalEvent.fromJSON(data)
        assertEquals("0.5", event.amount)
    }

    @Test
    fun `withdrawalEvent fromJSON handles null data`() {
        val event = WithdrawalEvent.fromJSON(null)
        assertNull(event.withdrawalId)
        assertNull(event.status)
        assertFalse(event.success)
        assertNull(event.assetId)
        assertNull(event.networkId)
        assertNull(event.amount)
        assertNull(event.rawData)
    }

    // MARK: - WithdrawalCallbacks tests

    @Test
    fun `withdrawalCallbacks onWithdrawal default is no-op`() {
        val callbacks = object : WithdrawalCallbacks {
            override fun onClose() {}
            override fun onError(error: ConnectError) {}
            override fun onEvent(event: GenericEvent) {}
        }
        // Should not throw — default implementation is no-op
        callbacks.onWithdrawal(WithdrawalEvent(null, null, false, null, null, null, null))
    }

    @Test
    fun `withdrawalCallbacks onWithdrawal fires when overridden`() {
        var called = false
        val callbacks = object : WithdrawalCallbacks {
            override fun onClose() {}
            override fun onError(error: ConnectError) {}
            override fun onEvent(event: GenericEvent) {}
            override fun onWithdrawal(event: WithdrawalEvent) { called = true }
        }
        callbacks.onWithdrawal(WithdrawalEvent(null, null, false, null, null, null, null))
        assertTrue(called)
    }

    // MARK: - WithdrawalCallbackHandler tests

    @Test
    fun `withdrawalCallbackHandler handleClose fires onClose`() {
        var called = false
        val callbacks = object : WithdrawalCallbacks {
            override fun onClose() { called = true }
            override fun onError(error: ConnectError) {}
            override fun onEvent(event: GenericEvent) {}
        }
        WithdrawalCallbackHandler(callbacks).handleClose()
        assertTrue(called)
    }

    @Test
    fun `withdrawalCallbackHandler handleError fires onError`() {
        var receivedError: ConnectError? = null
        val callbacks = object : WithdrawalCallbacks {
            override fun onClose() {}
            override fun onError(error: ConnectError) { receivedError = error }
            override fun onEvent(event: GenericEvent) {}
        }
        WithdrawalCallbackHandler(callbacks).handleError("NETWORK_ERROR", "No connection", null)
        assertTrue(receivedError is ConnectError.NetworkError)
    }

    @Test
    fun `withdrawalCallbackHandler handleEvent fires onEvent`() {
        var receivedEvent: GenericEvent? = null
        val callbacks = object : WithdrawalCallbacks {
            override fun onClose() {}
            override fun onError(error: ConnectError) {}
            override fun onEvent(event: GenericEvent) { receivedEvent = event }
        }
        WithdrawalCallbackHandler(callbacks).handleEvent("withdrawal.submitted", null)
        assertEquals("withdrawal.submitted", receivedEvent?.type)
    }

    @Test
    fun `withdrawalCallbackHandler handleWithdrawal fires onWithdrawal`() {
        var receivedEvent: WithdrawalEvent? = null
        val callbacks = object : WithdrawalCallbacks {
            override fun onClose() {}
            override fun onError(error: ConnectError) {}
            override fun onEvent(event: GenericEvent) {}
            override fun onWithdrawal(event: WithdrawalEvent) { receivedEvent = event }
        }
        val status = JSONObject().apply { put("value", "processed") }
        val data = JSONObject().apply {
            put("withdrawalId", "wit-001")
            put("status", status)
        }
        WithdrawalCallbackHandler(callbacks).handleWithdrawal(data)
        assertEquals("wit-001", receivedEvent?.withdrawalId)
        assertTrue(receivedEvent?.success == true)
    }

    @Test
    fun `withdrawalCallbackHandler handleDeposit is no-op`() {
        var onWithdrawalCalled = false
        val callbacks = object : WithdrawalCallbacks {
            override fun onClose() {}
            override fun onError(error: ConnectError) {}
            override fun onEvent(event: GenericEvent) {}
            override fun onWithdrawal(event: WithdrawalEvent) { onWithdrawalCalled = true }
        }
        // Deposit events must be silently dropped in the withdrawal flow
        WithdrawalCallbackHandler(callbacks).handleDeposit(JSONObject())
        assertFalse(onWithdrawalCalled)
    }
}
