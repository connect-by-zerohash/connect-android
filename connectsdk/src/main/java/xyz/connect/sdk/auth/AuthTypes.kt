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
 * Deposit event with parsed fields.
 *
 * **Not terminal.** It also fires while account matching is verifying, and can
 * arrive more than once for the same deposit — read the outcome off [status] /
 * [success] rather than treating the call itself as completion.
 */
data class DepositEvent(
    val depositId: String?,
    val status: String?,
    val success: Boolean,
    val assetId: String?,
    val networkId: String?,
    val amount: String?,
    /** Account-matching validation status, e.g. `PENDING`, `VALID`, `INVALID`, `ERROR`. */
    val accountMatchingStatus: String?,
    /**
     * Why account matching failed. On a name mismatch this is the only explanation
     * available anywhere in the stack, so prefer it over reporting a bare id.
     */
    val accountMatchingReason: String?,
    val rawData: JSONObject?
) {
    companion object {
        private fun JSONObject.optStringOrNull(key: String): String? =
            if (has(key) && !isNull(key)) getString(key) else null

        /**
         * Status values the Auth flow treats as a successful deposit.
         *
         * The web SDK's `isSuccessfulDeposit` shows success at CONFIRMED — its
         * `DepositStatusValue.COMPLETED` is the string `'CONFIRMED'` — or at
         * PROCESSED when the platform runs zerohash with auto-convert. That
         * profile flag never reaches the bridge, so both are accepted here.
         * Comparing against "processed" alone reported false for every
         * successful deposit on the default path, the same bug [WithdrawalEvent]
         * already carries a fix for.
         */
        private val SUCCESS_STATUSES = setOf("confirmed", "processed")

        /**
         * Account-matching states the web SDK routes away from success before it
         * ever looks at the status: PENDING shows the verifying screen, INVALID
         * and ERROR show the failed screen. Absent, VALID, or any value we don't
         * know yet falls through to the status check, exactly as the web hook
         * does.
         */
        private val NON_SUCCESS_MATCHING_STATUSES = setOf("pending", "invalid", "error")

        /**
         * Parse deposit event from JSON data.
         *
         * `status` arrives as an object (`{ value, details, occurredAt }`) and
         * there is no flat `success` field, so both are derived from
         * `status.value` and the account-matching validation. Matches how
         * [WithdrawalEvent] and connect-ios parse the same shape.
         */
        fun fromJSON(data: JSONObject?): DepositEvent {
            val statusValue = data?.optJSONObject("status")?.optStringOrNull("value")
            val validation = data?.optJSONObject("accountMatchingValidation")
            val matchingStatus = validation?.optStringOrNull("status")
            return DepositEvent(
                depositId = data?.optStringOrNull("depositId"),
                status = statusValue,
                success = statusValue?.lowercase() in SUCCESS_STATUSES &&
                    matchingStatus?.lowercase() !in NON_SUCCESS_MATCHING_STATUSES,
                assetId = data?.optStringOrNull("assetId"),
                networkId = data?.optStringOrNull("networkId"),
                amount = data?.optStringOrNull("amount"),
                accountMatchingStatus = matchingStatus,
                accountMatchingReason = validation?.optStringOrNull("reason"),
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
