package com.linksi.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "linksi_settings")
val FOLDER_VIEW_MODE = stringPreferencesKey("folder_view_mode")
val FOLDER_SORT_OPTION = stringPreferencesKey("folder_sort_option")
val HOME_VIEW_MODE = stringPreferencesKey("home_view_mode")
val HOME_SORT_OPTION = stringPreferencesKey("home_sort_option")
val AI_ENABLED = booleanPreferencesKey("ai_enabled")
val AI_SELECTED_MODEL = stringPreferencesKey("ai_selected_model")
// Store API keys per provider
val AI_KEY_OPENAI = stringPreferencesKey("ai_key_openai")
val AI_KEY_ANTHROPIC = stringPreferencesKey("ai_key_anthropic")
val AI_KEY_GEMINI = stringPreferencesKey("ai_key_gemini")
val AI_KEY_DEEPSEEK = stringPreferencesKey("ai_key_deepseek")
val AI_KEY_GROK = stringPreferencesKey("ai_key_grok")
val APP_LANGUAGE = stringPreferencesKey("app_language")
