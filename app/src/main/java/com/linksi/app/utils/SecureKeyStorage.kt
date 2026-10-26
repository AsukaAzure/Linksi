package com.linksi.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.linksi.app.domain.model.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * AI provider API keys, held in EncryptedSharedPreferences with an in-memory
 * cache. EncryptedSharedPreferences construction is expensive, so [preload]
 * must run once (on IO) before [getKey] is called on the UI thread; after that
 * reads are pure memory lookups.
 */
object SecureKeyStorage {
    private const val TAG = "SecureKeyStorage"
    private const val PREF_FILE_NAME = "secure_ai_keys_prefs"

    private val cache = ConcurrentHashMap<AiProvider, String>()
    @Volatile
    private var loaded = false

    suspend fun preload(context: Context) = withContext(Dispatchers.IO) {
        if (loaded) return@withContext
        for (provider in AiProvider.entries) {
            cache[provider] = readKey(context, provider)
        }
        loaded = true
    }

    fun getKey(provider: AiProvider): String = cache[provider] ?: ""

    fun getAll(): Map<AiProvider, String> = AiProvider.entries.associateWith { getKey(it) }

    suspend fun saveKey(context: Context, provider: AiProvider, key: String) =
        withContext(Dispatchers.IO) {
            try {
                getEncryptedPrefs(context).edit().putString(provider.name, key).apply()
                cache[provider] = key
                Log.i(TAG, "Successfully saved key for provider: $provider")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving key for $provider", e)
            }
        }

    suspend fun clearKey(context: Context, provider: AiProvider) = withContext(Dispatchers.IO) {
        try {
            getEncryptedPrefs(context).edit().remove(provider.name).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing key for $provider", e)
        }
        cache.remove(provider)
    }

    private fun readKey(context: Context, provider: AiProvider): String = try {
        getEncryptedPrefs(context).getString(provider.name, "") ?: ""
    } catch (e: Exception) {
        Log.e(TAG, "Error reading key for $provider", e)
        ""
    }

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            createPrefs(context)
        } catch (e: Exception) {
            // The master key is device-bound: after a backup restore the old
            // ciphertext can't be decrypted. Wipe it and start fresh — saved
            // keys are lost, which is the only sane recovery.
            Log.e(TAG, "Failed to create EncryptedSharedPreferences, attempting reset", e)
            try {
                context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE).edit().clear().apply()
                createPrefs(context)
            } catch (e2: Exception) {
                Log.e(TAG, "Fatal error creating EncryptedSharedPreferences", e2)
                throw e2
            }
        }
    }

    private fun createPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREF_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
