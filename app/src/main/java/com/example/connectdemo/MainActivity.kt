package com.example.connectdemo

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.connectdemo.databinding.ActivityMainBinding
import xyz.connect.sdk.ConnectError
import xyz.connect.sdk.ConnectSDK
import xyz.connect.sdk.Environment
import xyz.connect.sdk.GenericEvent
import xyz.connect.sdk.Theme
import xyz.connect.sdk.auth.AuthCallbacks
import xyz.connect.sdk.auth.ConnectAuthSession
import xyz.connect.sdk.auth.DepositEvent
import xyz.connect.sdk.recovery.ConnectRecoverySession
import xyz.connect.sdk.recovery.RecoveryCallbacks
import xyz.connect.sdk.withdrawal.ConnectWithdrawalSession
import xyz.connect.sdk.withdrawal.WithdrawalCallbacks
import xyz.connect.sdk.withdrawal.WithdrawalEvent

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var authSession: ConnectAuthSession? = null
    private var recoverySession: ConnectRecoverySession? = null
    private var withdrawalSession: ConnectWithdrawalSession? = null

    companion object {
        private const val TAG = "ConnectDemo"
        private const val DEMO_JWT = "your-jwt-token-here"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.etJwt.setText(DEMO_JWT)

        binding.rgSdk.setOnCheckedChangeListener { _, checkedId ->
            binding.btnConnect.text = when (checkedId) {
                R.id.rbRecovery -> "Open Recovery"
                R.id.rbWithdrawal -> "Open Withdrawal"
                else -> "Connect Account"
            }
        }

        binding.btnConnect.setOnClickListener {
            startSelectedSdk()
        }

        binding.btnClearLog.setOnClickListener {
            binding.tvLog.text = ""
        }
    }

    private fun startSelectedSdk() {
        val jwt = resolveJwt()
        val environment = selectedEnvironment()
        val theme = selectedTheme()

        addLog("Environment: ${environment.toWebValue()}")
        addLog("Theme: ${theme.toWebValue()}")

        try {
            when (binding.rgSdk.checkedRadioButtonId) {
                R.id.rbRecovery -> startRecovery(jwt, environment, theme)
                R.id.rbWithdrawal -> startWithdrawal(jwt, environment, theme)
                else -> startAuth(jwt, environment, theme)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SDK", e)
            addLog("Exception: ${e.message}")
            showToast("Failed to start: ${e.message}")
        }
    }

    private fun resolveJwt(): String {
        val jwt = binding.etJwt.text.toString().trim()
        return if (jwt.isBlank() || jwt == DEMO_JWT) {
            addLog("Using dummy JWT for testing (will fail authentication)")
            "test-jwt-token-for-ui-testing"
        } else {
            jwt
        }
    }

    private fun selectedEnvironment(): Environment = when (binding.rgEnvironment.checkedRadioButtonId) {
        R.id.rbSandbox -> Environment.SANDBOX
        else -> Environment.PRODUCTION
    }

    private fun selectedTheme(): Theme = when (binding.rgTheme.checkedRadioButtonId) {
        R.id.rbLight -> Theme.LIGHT
        R.id.rbDark -> Theme.DARK
        else -> Theme.SYSTEM
    }

    private fun startAuth(jwt: String, environment: Environment, theme: Theme) {
        addLog("Starting Auth session...")
        authSession = ConnectSDK.configureAuth(
            jwt = jwt,
            environment = environment,
            theme = theme,
            callbacks = object : AuthCallbacks {
                override fun onClose() {
                    addLog("Session closed")
                    showToast("Session closed")
                    authSession = null
                }

                override fun onError(error: ConnectError) {
                    Log.e(TAG, "Auth error: ${error.message}")
                    addLog("Error: ${error.message}")
                    showToast("Error: ${error.message}")
                }

                override fun onEvent(event: GenericEvent) {
                    addLog("Event: ${event.type}")
                }

                override fun onDeposit(event: DepositEvent) {
                    addLog("Deposit: ${event.depositId} (${event.status})")
                    showToast(if (event.success) "Deposit successful" else "Deposit failed")
                }
            }
        )
        authSession?.present(this)
        addLog("Auth session presented")
    }

    private fun startRecovery(jwt: String, environment: Environment, theme: Theme) {
        addLog("Starting Recovery session...")
        recoverySession = ConnectSDK.configureRecovery(
            jwt = jwt,
            environment = environment,
            theme = theme,
            callbacks = object : RecoveryCallbacks {
                override fun onClose() {
                    addLog("Session closed")
                    showToast("Session closed")
                    recoverySession = null
                }

                override fun onError(error: ConnectError) {
                    Log.e(TAG, "Recovery error: ${error.message}")
                    addLog("Error: ${error.message}")
                    showToast("Error: ${error.message}")
                }

                override fun onEvent(event: GenericEvent) {
                    addLog("Event: ${event.type}")
                }

                override fun onWithdrawal(event: WithdrawalEvent) {
                    addLog("Withdrawal: ${event.withdrawalId} (${event.status})")
                    showToast(if (event.success) "Withdrawal successful" else "Withdrawal failed")
                }
            }
        )
        recoverySession?.present(this)
        addLog("Recovery session presented")
    }

    private fun startWithdrawal(jwt: String, environment: Environment, theme: Theme) {
        addLog("Starting Withdrawal session...")
        withdrawalSession = ConnectSDK.configureWithdrawal(
            jwt = jwt,
            environment = environment,
            theme = theme,
            callbacks = object : WithdrawalCallbacks {
                override fun onClose() {
                    addLog("Session closed")
                    showToast("Session closed")
                    withdrawalSession = null
                }

                override fun onError(error: ConnectError) {
                    Log.e(TAG, "Withdrawal error: ${error.message}")
                    addLog("Error: ${error.message}")
                    showToast("Error: ${error.message}")
                }

                override fun onEvent(event: GenericEvent) {
                    addLog("Event: ${event.type}")
                }

                override fun onWithdrawal(event: WithdrawalEvent) {
                    addLog("Withdrawal: ${event.withdrawalId} (${event.status})")
                    showToast(if (event.success) "Withdrawal successful" else "Withdrawal failed")
                }
            }
        )
        withdrawalSession?.present(this)
        addLog("Withdrawal session presented")
    }

    private fun addLog(message: String) {
        runOnUiThread {
            val currentLog = binding.tvLog.text.toString()
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())
            val newLog = "[$timestamp] $message\n$currentLog"
            binding.tvLog.text = newLog
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        authSession?.cancel()
        recoverySession?.cancel()
        withdrawalSession?.cancel()
    }
}
