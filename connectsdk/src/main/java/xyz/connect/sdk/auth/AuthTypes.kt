package xyz.connect.sdk.auth

import org.json.JSONObject
import xyz.connect.sdk.AppCallbacks
import xyz.connect.sdk.CallbackHandler
import xyz.connect.sdk.ConnectError
import xyz.connect.sdk.GenericEvent

/**
 * Auth-specific callbacks extending base AppCallbacks
 */
interface AuthCallbacks : AppCallbacks {
    /**
     * Called when a deposit is completed
     */
    fun onDeposit(event: DepositEvent)
}

/**
 * Error event wrapper with structured data
 */
data class ErrorEvent(
    val code: String?,
    val message: String,
    val data: JSONObject?,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Deposit event with parsed fields
 */
data class DepositEvent(
    val depositId: String?,
    val status: String?,
    val success: Boolean,
    val assetId: String?,
    val networkId: String?,
    val amount: String?,
    val rawData: JSONObject?
) {
    companion object {
        private fun JSONObject.optStringOrNull(key: String): String? =
            if (has(key) && !isNull(key)) getString(key) else null

        /**
         * Parse deposit event from JSON data.
         *
         * `status` arrives as an object (`{ value, details, occurredAt }`) and
         * there is no flat `success` field, so both are derived from
         * `status.value`. Matches
         * how [WithdrawalEvent] and connect-ios parse the same shape.
         */
        fun fromJSON(data: JSONObject?): DepositEvent {
            val statusValue = data?.optJSONObject("status")?.optStringOrNull("value")
            return DepositEvent(
                depositId = data?.optStringOrNull("depositId"),
                status = statusValue,
                success = statusValue?.lowercase() == "processed",
                assetId = data?.optStringOrNull("assetId"),
                networkId = data?.optStringOrNull("networkId"),
                amount = data?.optStringOrNull("amount"),
                rawData = data
            )
        }
    }
}

/**
 * Handler that converts raw data to typed events
 */
class AuthCallbackHandler(
    private val callbacks: AuthCallbacks
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

    override fun handleDeposit(data: JSONObject?) {
        val depositEvent = DepositEvent.fromJSON(data)
        callbacks.onDeposit(depositEvent)
    }
}
