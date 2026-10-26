package com.linksi.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "linksi_settings")
val FOLDER_VIEW_MODE = stringPreferencesKey("folder_view_mode")
val FOLDER_SORT_OPTION = stringPreferencesKey("folder_sort_option")
val FOLDER_LINKS_VIEW_MODE = stringPreferencesKey("folder_links_view_mode")
val HOME_VIEW_MODE = stringPreferencesKey("home_view_mode")
val HOME_SORT_OPTION = stringPreferencesKey("home_sort_option")
val AI_ENABLED = booleanPreferencesKey("ai_enabled")
val AI_SELECTED_MODEL = stringPreferencesKey("ai_selected_model")
// API keys live in SecureKeyStorage (EncryptedSharedPreferences), not here.
// Store API URLs per provider
val AI_URL_OPENAI = stringPreferencesKey("ai_url_openai")
val AI_URL_ANTHROPIC = stringPreferencesKey("ai_url_anthropic")
val AI_URL_GEMINI = stringPreferencesKey("ai_url_gemini")
val AI_URL_DEEPSEEK = stringPreferencesKey("ai_url_deepseek")
val AI_URL_GROK = stringPreferencesKey("ai_url_grok")
val AI_URL_OPENROUTER = stringPreferencesKey("ai_url_openrouter")
val APP_LANGUAGE = stringPreferencesKey("app_language")
val APP_THEME = stringPreferencesKey("app_theme") // Values: "system", "light", "dark"
val APP_AMOLED = booleanPreferencesKey("app_amoled")
val APP_DYNAMIC_COLOR = booleanPreferencesKey("app_dynamic_color")
val SHOW_QUICK_FILTERS = booleanPreferencesKey("show_quick_filters")

// Security Settings
val SECURITY_LOCK_ENABLED = booleanPreferencesKey("security_lock_enabled")
val SECURITY_BIOMETRIC_ENABLED = booleanPreferencesKey("security_biometric_enabled")
val SECURITY_PIN = stringPreferencesKey("security_pin")
val SECURITY_LOCK_DELAY = longPreferencesKey("security_lock_delay")
val SECURITY_UNIVERSAL_LOCK = booleanPreferencesKey("security_universal_lock")
val SECURITY_FOLDER_LOCK_ENABLED = booleanPreferencesKey("security_folder_lock_enabled")
val LAST_APP_PAUSE_TIME = longPreferencesKey("last_app_pause_time")
val SECURITY_FAILED_ATTEMPTS = intPreferencesKey("security_failed_attempts")
val SECURITY_LOCKOUT_UNTIL = longPreferencesKey("security_lockout_until")
val SKIP_UPDATE_UNTIL = longPreferencesKey("skip_update_until")
val TRASH_BIN_ENABLED = booleanPreferencesKey("trash_bin_enabled")
val GLOBAL_PREVENT_SCREENSHOT = booleanPreferencesKey("global_prevent_screenshot")
val EXPORT_INCLUDE_LOCKED = booleanPreferencesKey("export_include_locked")
