package com.linksi.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.linksi.app.ui.screens.HomeScreen
import com.linksi.app.ui.screens.LockScreen
import com.linksi.app.ui.screens.OnboardingScreen
import com.linksi.app.ui.theme.LinksTheme
import com.linksi.app.utils.*
import dagger.hilt.android.AndroidEntryPoint
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var isLocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            // State for language and onboarding
            val appLanguage by remember {
                context.dataStore.data.map { it[APP_LANGUAGE] ?: "" }
            }.collectAsState(initial = null)

            val onboardingComplete by remember {
                isOnboardingComplete(context)
            }.collectAsState(initial = null)

            val securityPrefs by remember {
                context.dataStore.data.map { prefs ->
                    Triple(
                        prefs[SECURITY_LOCK_ENABLED] ?: false,
                        prefs[SECURITY_BIOMETRIC_ENABLED] ?: false,
                        prefs[SECURITY_PIN] ?: ""
                    )
                }
            }.collectAsState(initial = null)

            // Keep splash screen visible until we have all values
            splashScreen.setKeepOnScreenCondition {
                appLanguage == null || onboardingComplete == null || securityPrefs == null
            }

            // Check if we should lock on startup
            LaunchedEffect(securityPrefs) {
                if (securityPrefs != null && !isLocked) {
                    if (SecurityManager.shouldLockApp(context)) {
                        isLocked = true
                    }
                }
            }

            // Apply language if it differs from current system/app setting
            LaunchedEffect(appLanguage) {
                appLanguage?.let { lang ->
                    val appLocale: LocaleListCompat = if (lang.isEmpty()) {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        LocaleListCompat.forLanguageTags(lang)
                    }
                    if (AppCompatDelegate.getApplicationLocales() != appLocale) {
                        AppCompatDelegate.setApplicationLocales(appLocale)
                    }
                }
            }

            LinksTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isLocked && securityPrefs != null) {
                        LockScreen(
                            savedPin = securityPrefs!!.third,
                            isBiometricEnabled = securityPrefs!!.second,
                            onUnlock = { isLocked = false }
                        )
                    } else {
                        when (onboardingComplete) {
                            null -> {
                                // Empty box while splash screen is still covering
                                Box(Modifier.fillMaxSize())
                            }
                            false -> {
                                OnboardingScreen(
                                    onFinish = {
                                        scope.launch {
                                            setOnboardingComplete(context)
                                        }
                                    }
                                )
                            }
                            true -> {
                                HomeScreen()
                            }
                        }
                    }

                    UpdateCheckDialog()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            dataStore.edit { it[LAST_APP_PAUSE_TIME] = System.currentTimeMillis() }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            if (SecurityManager.shouldLockApp(this@MainActivity)) {
                isLocked = true
            }
        }
    }

    @Composable
    private fun UpdateCheckDialog() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var showUpdateDialog by remember { mutableStateOf(false) }
        var latestVersion by remember { mutableStateOf("") }
        var currentVersion by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            try {
                // Check if user skipped updates recently
                val skipUntil = context.dataStore.data.map { it[SKIP_UPDATE_UNTIL] ?: 0L }.first()
                if (System.currentTimeMillis() < skipUntil) return@LaunchedEffect

                val version = context.packageManager
                    .getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
                currentVersion = version

                val response = withContext(Dispatchers.IO) {
                    java.net.URL("https://api.github.com/repos/AsukaAzure/Linksi/releases/latest")
                        .openConnection()
                        .apply {
                            connectTimeout = 6000
                            readTimeout = 6000
                        }
                        .getInputStream()
                        .bufferedReader()
                        .readText()
                }

                val tagName = Regex(""""tag_name"\s*:\s*"([^"]+)"""")
                    .find(response)?.groupValues?.get(1) ?: ""

                latestVersion = tagName.removePrefix("v")

                if (isNewerVersion(latestVersion, currentVersion)) {
                    showUpdateDialog = true
                }
            } catch (e: Exception) {
                // Silently fail update check
            }
        }

        if (showUpdateDialog) {
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                icon = {
                    Icon(
                        Icons.Outlined.SystemUpdate, null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text(stringResource(id = R.string.update_available_dialog_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(id = R.string.update_available_dialog_text))
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                Text(
                                    stringResource(id = R.string.current),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text("v$currentVersion", style = MaterialTheme.typography.bodyMedium)
                            }
                            Column {
                                Text(
                                    stringResource(id = R.string.latest),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "v$latestVersion",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/AsukaAzure/Linksi/releases/latest")
                        )
                        context.startActivity(intent)
                        showUpdateDialog = false
                    }) {
                        Icon(Icons.Outlined.Download, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(id = R.string.update_now))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        scope.launch {
                            context.dataStore.edit {
                                it[SKIP_UPDATE_UNTIL] = System.currentTimeMillis() + 24 * 60 * 60 * 1000
                            }
                        }
                        showUpdateDialog = false
                    }) {
                        Text(stringResource(id = R.string.skip))
                    }
                }
            )
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        return try {
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}
