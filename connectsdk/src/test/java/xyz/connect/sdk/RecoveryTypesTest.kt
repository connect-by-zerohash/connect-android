package xyz.connect.sdk

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.connect.sdk.recovery.RecoveryCallbacks
import xyz.connect.sdk.recovery.RecoveryCallbackHandler
import xyz.connect.sdk.withdrawal.WithdrawalEvent

class RecoveryTypesTest {

    // MARK: - RecoveryCallbacks tests

    @Test
    fun `recoveryCallbacks onWithdrawal default is no-op`() {
        val callbacks = object : RecoveryCallbacks {
            override fun onClose() {}
            override fun onError(error: ConnectError) {}
            override fun onEvent(event: GenericEvent) {}
        }
        callbacks.onWithdrawal(WithdrawalEvent(null, null, false, null, null, null, null))
    }

    @Test
    fun `recoveryCallbacks onWithdrawal fires when overridden`() {
        var called = false
        val callbacks = object : RecoveryCallbacks {
            override fun onClose() {}
            override fun onError(error: ConnectError) {}
            override fun onEvent(event: GenericEvent) {}
            override fun onWithdrawal(event: WithdrawalEvent) { called = true }
        }
        callbacks.onWithdrawal(WithdrawalEvent(null, null, false, null, null, null, null))
        assertTrue(called)
    }

    // MARK: - RecoveryCallbackHandler tests

    @Test
    fun `recoveryCallbackHandler handleClose fires onClose`() {
        var called = false
        val callbacks = object : RecoveryCallbacks {
            override fun onClose() { called = true }
            override fun onError(error: ConnectError) {}
            override fun onEvent(event: GenericEvent) {}
        }
        RecoveryCallbackHandler(callbacks).handleClose()
        assertTrue(called)
    }

    @Test
    fun `recoveryCallbackHandler handleError fires onError`() {
        var receivedError: ConnectError? = null
        val callbacks = object : RecoveryCallbacks {
            override fun onClose() {}
            override fun onError(error: ConnectError) { receivedError = error }
            override fun onEvent(event: GenericEvent) {}
        }
        RecoveryCallbackHandler(callbacks).handleError("AUTH_ERROR", "Unauthorized", null)
        assertTrue(receivedError is ConnectError.AuthenticationError)
    }

    @Test
    fun `recoveryCallbackHandler handleEvent fires onEvent`() {
        var receivedEvent: GenericEvent? = null
        val callbacks = object : RecoveryCallbacks {
            override fun onClose() {}
            override fun onError(error: ConnectError) {}
            override fun onEvent(event: GenericEvent) { receivedEvent = event }
        }
        RecoveryCallbackHandler(callbacks).handleEvent("recovery.started", null)
        assertEquals("recovery.started", receivedEvent?.type)
    }

    @Test
    fun `recoveryCallbackHandler handleWithdrawal fires onWithdrawal`() {
        var receivedEvent: WithdrawalEvent? = null
        val callbacks = object : RecoveryCallbacks {
            override fun onClose() {}
            override fun onError(error: ConnectError) {}
            override fun onEvent(event: GenericEvent) {}
            override fun onWithdrawal(event: WithdrawalEvent) { receivedEvent = event }
        }
        val status = JSONObject().apply { put("value", "processed") }
        val data = JSONObject().apply {
            put("withdrawalId", "wit-recovery-001")
            put("status", status)
        }
        RecoveryCallbackHandler(callbacks).handleWithdrawal(data)
        assertEquals("wit-recovery-001", receivedEvent?.withdrawalId)
        assertTrue(receivedEvent?.success == true)
    }

    @Test
    fun `recoveryCallbackHandler handleDeposit is no-op`() {
        var onWithdrawalCalled = false
        val callbacks = object : RecoveryCallbacks {
            override fun onClose() {}
            override fun onError(error: ConnectError) {}
            override fun onEvent(event: GenericEvent) {}
            override fun onWithdrawal(event: WithdrawalEvent) { onWithdrawalCalled = true }
        }
        RecoveryCallbackHandler(callbacks).handleDeposit(JSONObject())
        assertFalse(onWithdrawalCalled)
    }
}
