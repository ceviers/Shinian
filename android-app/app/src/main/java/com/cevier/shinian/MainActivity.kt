package com.cevier.shinian

import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Bundle
import android.os.CancellationSignal
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cevier.shinian.data.EncryptedNoteStore
import com.cevier.shinian.data.AccessManager
import com.cevier.shinian.ui.ShinianApp
import com.cevier.shinian.ui.theme.ShinianTheme

class MainActivity : ComponentActivity() {
    private val store by lazy { EncryptedNoteStore(applicationContext) }
    private val accessManager by lazy { AccessManager(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updateRecentPreviewProtection(accessManager.hideRecentPreview)
        setContent {
            ShinianTheme {
                ShinianApp(
                    store = store,
                    requestBiometric = ::requestBiometric,
                    onHideRecentPreviewChanged = ::updateRecentPreviewProtection,
                    onExit = { finish() },
                )
            }
        }
    }

    private fun updateRecentPreviewProtection(hidden: Boolean) {
        setRecentsScreenshotEnabled(!hidden)
    }

    private fun requestBiometric(onSuccess: () -> Unit, onUnavailable: () -> Unit) {
        val manager = getSystemService(BiometricManager::class.java)
        val available = manager?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

        if (!available) {
            onUnavailable()
            return
        }

        var resolved = false
        fun useRecovery() {
            if (!resolved) {
                resolved = true
                onUnavailable()
            }
        }

        val prompt = BiometricPrompt.Builder(this)
            .setTitle("解锁拾念")
            .setSubtitle("验证身份以打开本地加密记录")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButton("使用恢复口令", mainExecutor) { _, _ -> useRecovery() }
            .build()

        prompt.authenticate(
            CancellationSignal(),
            mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    if (!resolved) {
                        resolved = true
                        onSuccess()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    when (errorCode) {
                        BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT,
                        BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE,
                        BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS,
                        -> useRecovery()

                        BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED,
                        BiometricPrompt.BIOMETRIC_ERROR_CANCELED,
                        -> Unit

                        else -> Toast.makeText(this@MainActivity, errString ?: "验证失败", Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }
}
