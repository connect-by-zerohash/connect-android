package xyz.connect.sdk

import xyz.connect.sdk.auth.AuthCallbacks
import xyz.connect.sdk.auth.ConnectAuthSession
import xyz.connect.sdk.recovery.ConnectRecoverySession
import xyz.connect.sdk.recovery.RecoveryCallbacks
import xyz.connect.sdk.withdrawal.ConnectWithdrawalSession
import xyz.connect.sdk.withdrawal.WithdrawalCallbacks



/**
 * ConnectSDK - Main entry point for Connect SDK
 *
 * Provides static factory methods to create authenticated sessions
 * with the Connect platform.
 */
object ConnectSDK {

    /**
     * Configure and create an authentication session
     *
     * @param jwt JWT token for authentication
     * @param environment Environment to connect to (default: PRODUCTION)
     * @param theme UI theme (default: SYSTEM)
     * @param callbacks Callbacks for session events
     * @return ConnectAuthSession instance ready to be presented
     *
     * Example usage:
     * ```
     * val session = ConnectSDK.configureAuth(
     *     jwt = "your-jwt-token",
     *     environment = Environment.PRODUCTION,
     *     theme = Theme.SYSTEM,
     *     callbacks = object : AuthCallbacks {
     *         override fun onClose() { /* handle close */ }
     *         override fun onError(error: ConnectError) { /* handle error */ }
     *         override fun onEvent(event: GenericEvent) { /* handle event */ }
     *         override fun onDeposit(event: DepositEvent) { /* handle deposit */ }
     *     }
     * )
     * session.present(activity)
     * ```
     */
    fun configureAuth(
        jwt: String,
        environment: Environment = Environment.PRODUCTION,
        theme: Theme = Theme.SYSTEM,
        allowList: ConnectAllowList = ConnectAllowList.DEFAULT,
        callbacks: AuthCallbacks
    ): ConnectAuthSession {
        return ConnectAuthSession(
            jwt = jwt,
            environment = environment,
            theme = theme,
            allowList = allowList,
            callbacks = callbacks
        )
    }

    /**
     * Configure and create a recovery session
     *
     * @param jwt JWT token for authentication
     * @param environment Environment to connect to (default: PRODUCTION)
     * @param theme UI theme (default: SYSTEM)
     * @param callbacks Callbacks for session events
     * @return ConnectRecoverySession instance ready to be presented
     *
     * Example usage:
     * ```
     * val session = ConnectSDK.configureRecovery(
     *     jwt = "your-jwt-token",
     *     callbacks = object : RecoveryCallbacks {
     *         override fun onClose() { /* handle close */ }
     *         override fun onError(error: ConnectError) { /* handle error */ }
     *         override fun onEvent(event: GenericEvent) { /* handle event */ }
     *         override fun onWithdrawal(event: WithdrawalEvent) { /* handle withdrawal */ }
     *     }
     * )
     * session.present(activity)
     * ```
     */
    fun configureRecovery(
        jwt: String,
        environment: Environment = Environment.PRODUCTION,
        theme: Theme = Theme.SYSTEM,
        allowList: ConnectAllowList = ConnectAllowList.DEFAULT,
        callbacks: RecoveryCallbacks
    ): ConnectRecoverySession {
        return ConnectRecoverySession(
            jwt = jwt,
            environment = environment,
            theme = theme,
            allowList = allowList,
            callbacks = callbacks
        )
    }

    /**
     * Configure and create a withdrawal session
     *
     * @param jwt JWT token for authentication
     * @param environment Environment to connect to (default: PRODUCTION)
     * @param theme UI theme (default: SYSTEM)
     * @param callbacks Callbacks for session events
     * @return ConnectWithdrawalSession instance ready to be presented
     *
     * Example usage:
     * ```
     * val session = ConnectSDK.configureWithdrawal(
     *     jwt = "your-jwt-token",
     *     callbacks = object : WithdrawalCallbacks {
     *         override fun onClose() { /* handle close */ }
     *         override fun onError(error: ConnectError) { /* handle error */ }
     *         override fun onEvent(event: GenericEvent) { /* handle event */ }
     *         override fun onWithdrawal(event: WithdrawalEvent) { /* handle withdrawal */ }
     *     }
     * )
     * session.present(activity)
     * ```
     */
    fun configureWithdrawal(
        jwt: String,
        environment: Environment = Environment.PRODUCTION,
        theme: Theme = Theme.SYSTEM,
        allowList: ConnectAllowList = ConnectAllowList.DEFAULT,
        callbacks: WithdrawalCallbacks
    ): ConnectWithdrawalSession {
        return ConnectWithdrawalSession(
            jwt = jwt,
            environment = environment,
            theme = theme,
            allowList = allowList,
            callbacks = callbacks
        )
    }
}
