package xyz.connect.sdk.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

/** AUTH-4657. The behaviour is covered by Tests/JSTests. */
class CoinbaseScreensTest {

    @Test
    fun theRegistryIsPrependedToBothFlows() {
        val coinbaseKt = File("src/main/java/xyz/connect/sdk/automation/Coinbase.kt").readText()
        assertEquals(2, Regex("automation/coinbase-screens\\.js").findAll(coinbaseKt).count())
    }

    @Test
    fun receiveUnavailableIsNeverAutoRetried() {
        assertFalse(isRetryable("RECEIVE_UNAVAILABLE"))
    }
}
