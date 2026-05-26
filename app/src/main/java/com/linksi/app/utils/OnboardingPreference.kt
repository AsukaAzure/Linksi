package com.linksi.app.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")

fun isOnboardingComplete(context: Context): Flow<Boolean> =
    context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETE] ?: false
    }

suspend fun setOnboardingComplete(context: Context) {
    context.dataStore.edit { prefs ->
        prefs[ONBOARDING_COMPLETE] = true
    }
}