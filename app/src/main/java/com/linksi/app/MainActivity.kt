package com.linksi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import com.linksi.app.ui.screens.HomeScreen
import com.linksi.app.ui.screens.OnboardingScreen
import com.linksi.app.ui.theme.LinksTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.linksi.app.utils.isOnboardingComplete
import com.linksi.app.utils.setOnboardingComplete
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LinksTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                // Check if onboarding is complete
                val onboardingComplete by isOnboardingComplete(context)
                    .collectAsState(initial = null)

                var showUpdateDialog by remember { mutableStateOf(false) }
                var latestVersion by remember { mutableStateOf("") }
                var currentVersion by remember { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    try {
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
                        // Silently fail — don't interrupt user if check fails
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (onboardingComplete) {
                        null -> {
                            // Loading — show nothing or splash
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
                    if (showUpdateDialog) {
                        AlertDialog(
                            onDismissRequest = { showUpdateDialog = false },
                            icon = {
                                Icon(
                                    Icons.Outlined.SystemUpdate, null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            title = { Text("Update Available") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("A new version of Linksi is available.")
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Column {
                                            Text(
                                                "Current",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "v$currentVersion",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Column {
                                            Text(
                                                "Latest",
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
                                    Text("Update Now")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showUpdateDialog = false }) {
                                    Text("Later")
                                }
                            }
                        )
                    }
                }
            }
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
