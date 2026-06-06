package xyz.connect.sdk.recovery

import org.json.JSONObject
import xyz.connect.sdk.AppCallbacks
import xyz.connect.sdk.CallbackHandler
import xyz.connect.sdk.ConnectError
import xyz.connect.sdk.GenericEvent
import xyz.connect.sdk.withdrawal.WithdrawalEvent

/**
 * Recovery-specific callbacks extending base AppCallbacks
 */
interface RecoveryCallbacks : AppCallbacks {
    /**
     * Called when a withdrawal is completed during the recovery flow
     */
    fun onWithdrawal(event: WithdrawalEvent) {}
}

/**
 * Handler that converts raw data to typed recovery events
 */
internal class RecoveryCallbackHandler(
    private val callbacks: RecoveryCallbacks
) : CallbackHandler {

    override fun handleClose() {
        callbacks.onClose()
    }

    override fun handleError(code: String?, message: String, data: JSONObject?) {
        val error = ConnectError.fromWebError(code, message)
        callbacks.onError(error)
    }

    override fun handleEvent(type: String, data: JSONObject?) {
        val event = GenericEvent(type, data)
        callbacks.onEvent(event)
    }

    override fun handleWithdrawal(data: JSONObject?) {
        val event = WithdrawalEvent.fromJSON(data)
        callbacks.onWithdrawal(event)
    }
}
