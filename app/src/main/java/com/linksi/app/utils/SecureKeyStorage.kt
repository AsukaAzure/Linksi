package com.linksi.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.linksi.app.domain.model.AiProvider

object SecureKeyStorage {
    private const val TAG = "SecureKeyStorage"
    private const val PREF_FILE_NAME = "secure_ai_keys_prefs"

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREF_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences, attempting reset", e)
            try {
                context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE).edit().clear().apply()
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    PREF_FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e2: Exception) {
                Log.e(TAG, "Fatal error creating EncryptedSharedPreferences", e2)
                throw e2
            }
        }
    }

    fun getKey(context: Context, provider: AiProvider): String {
        return try {
            val prefs = getEncryptedPrefs(context)
            prefs.getString(provider.name, "") ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error reading key for $provider", e)
            ""
        }
    }

    fun saveKey(context: Context, provider: AiProvider, key: String) {
        try {
            val prefs = getEncryptedPrefs(context)
            prefs.edit().putString(provider.name, key).apply()
            Log.i(TAG, "Successfully saved key for provider: $provider")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving key for $provider", e)
        }
    }

    fun clearKey(context: Context, provider: AiProvider) {
        try {
            val prefs = getEncryptedPrefs(context)
            prefs.edit().remove(provider.name).apply()
            Log.i(TAG, "Successfully cleared key for provider: $provider")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing key for $provider", e)
        }
    }
}
