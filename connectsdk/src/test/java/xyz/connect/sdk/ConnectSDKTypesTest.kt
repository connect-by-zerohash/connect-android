package xyz.connect.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ConnectSDKTypesTest {

    // MARK: - ConnectApp

    @Test
    fun `connectApp has AUTH variant`() {
        assertEquals(ConnectApp.AUTH, ConnectApp.AUTH)
    }

    @Test
    fun `connectApp has RECOVERY variant`() {
        assertEquals(ConnectApp.RECOVERY, ConnectApp.RECOVERY)
    }

    @Test
    fun `connectApp has WITHDRAWAL variant`() {
        assertEquals(ConnectApp.WITHDRAWAL, ConnectApp.WITHDRAWAL)
    }

    @Test
    fun `connectApp variants are distinct`() {
        assertNotEquals(ConnectApp.AUTH, ConnectApp.RECOVERY)
        assertNotEquals(ConnectApp.AUTH, ConnectApp.WITHDRAWAL)
        assertNotEquals(ConnectApp.RECOVERY, ConnectApp.WITHDRAWAL)
    }

    // MARK: - Environment

    @Test
    fun `environment sandbox toWebValue returns sandbox`() {
        assertEquals("sandbox", Environment.SANDBOX.toWebValue())
    }

    @Test
    fun `environment production toWebValue returns production`() {
        assertEquals("production", Environment.PRODUCTION.toWebValue())
    }

    // MARK: - Theme

    @Test
    fun `theme light toWebValue returns light`() {
        assertEquals("light", Theme.LIGHT.toWebValue())
    }

    @Test
    fun `theme dark toWebValue returns dark`() {
        assertEquals("dark", Theme.DARK.toWebValue())
    }

    @Test
    fun `theme system toWebValue returns system`() {
        assertEquals("system", Theme.SYSTEM.toWebValue())
    }

    // MARK: - ConnectError.fromWebError

    @Test
    fun `connectError fromWebError NETWORK_ERROR maps to NetworkError`() {
        val error = ConnectError.fromWebError("NETWORK_ERROR", "No connection")
        assert(error is ConnectError.NetworkError)
        assertEquals("No connection", (error as ConnectError.NetworkError).message)
    }

    @Test
    fun `connectError fromWebError AUTH_ERROR maps to AuthenticationError`() {
        val error = ConnectError.fromWebError("AUTH_ERROR", "Unauthorized")
        assert(error is ConnectError.AuthenticationError)
    }

    @Test
    fun `connectError fromWebError INVALID_TOKEN maps to AuthenticationError`() {
        val error = ConnectError.fromWebError("INVALID_TOKEN", "Token expired")
        assert(error is ConnectError.AuthenticationError)
    }

    @Test
    fun `connectError fromWebError CONFIG_ERROR maps to ConfigurationError`() {
        val error = ConnectError.fromWebError("CONFIG_ERROR", "Bad config")
        assert(error is ConnectError.ConfigurationError)
    }

    @Test
    fun `connectError fromWebError OAUTH_ERROR maps to OAuthError`() {
        val error = ConnectError.fromWebError("OAUTH_ERROR", "OAuth failed")
        assert(error is ConnectError.OAuthError)
    }

    @Test
    fun `connectError fromWebError unknown code maps to UnknownError`() {
        val error = ConnectError.fromWebError("SOME_UNKNOWN", "Mystery")
        assert(error is ConnectError.UnknownError)
    }

    @Test
    fun `connectError fromWebError null code maps to UnknownError`() {
        val error = ConnectError.fromWebError(null, "No code")
        assert(error is ConnectError.UnknownError)
    }

    // MARK: - ConnectSession

    @Test
    fun `connectSession isActive true after creation`() {
        val session = ConnectSession(app = ConnectApp.AUTH)
        assert(session.isActive())
    }

    @Test
    fun `connectSession isActive false after close`() {
        val session = ConnectSession(app = ConnectApp.AUTH)
        session.close()
        assertNotEquals(true, session.isActive())
    }

    @Test
    fun `connectSession close fires callback`() {
        var called = false
        val session = ConnectSession(app = ConnectApp.AUTH)
        session.setOnCloseCallback { called = true }
        session.close()
        assert(called)
    }

    @Test
    fun `connectSession close is idempotent`() {
        var callCount = 0
        val session = ConnectSession(app = ConnectApp.AUTH)
        session.setOnCloseCallback { callCount++ }
        session.close()
        session.close()
        assertEquals(1, callCount)
    }

    @Test
    fun `connectSession recovery app is stored`() {
        val session = ConnectSession(app = ConnectApp.RECOVERY)
        assertEquals(ConnectApp.RECOVERY, session.app)
    }

    @Test
    fun `connectSession withdrawal app is stored`() {
        val session = ConnectSession(app = ConnectApp.WITHDRAWAL)
        assertEquals(ConnectApp.WITHDRAWAL, session.app)
    }

    // MARK: - F-012: Thread-safety of ConnectSession.close()

    @Test
    fun `connectSession close is safe when called concurrently from multiple threads`() {
        val callCount = AtomicInteger(0)
        val session = ConnectSession(app = ConnectApp.AUTH)
        session.setOnCloseCallback { callCount.incrementAndGet() }

        val threadCount = 20
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        repeat(threadCount) {
            executor.submit {
                try {
                    session.close()
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue("All threads should complete within 5s", latch.await(5, TimeUnit.SECONDS))
        executor.shutdown()

        assertEquals(
            "Callback must be invoked exactly once regardless of concurrent close calls",
            1,
            callCount.get()
        )
        assertNotEquals(true, session.isActive())
    }

    @Test
    fun `connectSession isActive is false immediately after concurrent close`() {
        val session = ConnectSession(app = ConnectApp.AUTH)
        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(10)

        repeat(10) {
            executor.submit {
                session.close()
                latch.countDown()
            }
        }

        latch.await(5, TimeUnit.SECONDS)
        executor.shutdown()

        assertNotEquals(true, session.isActive())
    }
}
