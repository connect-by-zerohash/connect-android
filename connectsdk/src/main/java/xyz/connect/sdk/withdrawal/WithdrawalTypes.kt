package xyz.connect.sdk.withdrawal

import org.json.JSONObject
import xyz.connect.sdk.AppCallbacks
import xyz.connect.sdk.CallbackHandler
import xyz.connect.sdk.ConnectError
import xyz.connect.sdk.GenericEvent

/**
 * Withdrawal-specific callbacks extending base AppCallbacks
 */
interface WithdrawalCallbacks : AppCallbacks {
    /**
     * Called when a withdrawal is completed during the withdrawal flow
     */
    fun onWithdrawal(event: WithdrawalEvent) {}
}

/**
 * Withdrawal event with parsed fields
 *
 * Used by both the Withdrawal and Recovery flows since both can emit withdrawal events.
 */
data class WithdrawalEvent(
    val withdrawalId: String?,
    val status: String?,
    val success: Boolean,
    val assetId: String?,
    val networkId: String?,
    val amount: String?,
    val rawData: JSONObject?
) {
    companion object {
        /**
         * Parse withdrawal event from JSON data
         */
        fun fromJSON(data: JSONObject?): WithdrawalEvent {
            val statusObj = data?.optJSONObject("status")
            val statusValue = statusObj?.optString("value")
            return WithdrawalEvent(
                withdrawalId = data?.optString("withdrawalId"),
                status = statusValue,
                success = statusValue?.lowercase() == "processed",
                assetId = data?.optString("assetId"),
                networkId = data?.optString("networkId"),
                amount = data?.optString("amount"),
                rawData = data
            )
        }
    }
}

/**
 * Handler that converts raw data to typed withdrawal events
 */
internal class WithdrawalCallbackHandler(
    private val callbacks: WithdrawalCallbacks
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
