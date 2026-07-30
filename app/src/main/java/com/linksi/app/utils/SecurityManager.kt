package com.linksi.app.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.first

object SecurityManager {

    fun canUseBiometric(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    suspend fun shouldLockApp(context: Context): Boolean {
        val prefs = context.dataStore.data.first()
        val isEnabled = prefs[SECURITY_LOCK_ENABLED] ?: false
        if (!isEnabled) return false

        val pin = prefs[SECURITY_PIN] ?: ""
        if (pin.isEmpty()) return false // Don't lock if PIN is not set

        val lastPauseTime = prefs[LAST_APP_PAUSE_TIME] ?: 0L
        if (lastPauseTime == 0L) return true // First launch or cleared

        val lockDelay = prefs[SECURITY_LOCK_DELAY] ?: 0L
        val currentTime = System.currentTimeMillis()

        return (currentTime - lastPauseTime) >= lockDelay
    }
}
