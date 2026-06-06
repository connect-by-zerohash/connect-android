package xyz.connect.sdk

/**
 * Allow-list of hosts the SDK is permitted to navigate to or load resources from.
 *
 * Integrators can supply their own list (e.g. fetched over the air) instead of
 * using the SDK default, by passing a [ConnectAllowList] to
 * [ConnectSDK.configureAuth], [ConnectSDK.configureRecovery], or
 * [ConnectSDK.configureWithdrawal].
 *
 * A host matches an entry if it is **exactly equal** to the entry or if it ends
 * with `"." + entry`.  This means `"connect.xyz"` covers `"sdk.connect.xyz"` and
 * `"sdk.sandbox.connect.xyz"` but NOT `"evilconnect.xyz"` or
 * `"connect.xyz.attacker.com"`.
 *
 */
class ConnectAllowList(val hosts: List<String>) {

    companion object {
        /**
         * Default allow-list shipped with the SDK.
         * Covers all Connect web-app and zerohash domains.
         */
        @JvmField
        val DEFAULT = ConnectAllowList(listOf("connect.xyz", "zerohash.com"))
    }

    /**
     * Returns `true` if [host] is permitted under this allow-list.
     */
    fun contains(host: String): Boolean {
        val lowered = host.lowercase()
        return hosts.any { entry ->
            val target = entry.lowercase()
            lowered == target || lowered.endsWith(".$target")
        }
    }
}
