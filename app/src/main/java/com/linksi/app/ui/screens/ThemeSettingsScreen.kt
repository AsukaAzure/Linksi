package com.linksi.app.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.linksi.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    currentThemeMode: String,
    useAmoled: Boolean,
    useDynamicColor: Boolean,
    onThemeSelected: (String) -> Unit,
    onAmoledToggled: (Boolean) -> Unit,
    onDynamicColorToggled: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.theme)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, stringResource(id = R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Theme Mode Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(id = R.string.theme_mode),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                SettingsCard {
                    ThemeOption(
                        title = stringResource(id = R.string.system),
                        selected = currentThemeMode == "system",
                        icon = Icons.Outlined.SettingsBrightness,
                        onClick = { onThemeSelected("system") }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ThemeOption(
                        title = stringResource(id = R.string.light),
                        selected = currentThemeMode == "light",
                        icon = Icons.Outlined.LightMode,
                        onClick = { onThemeSelected("light") }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ThemeOption(
                        title = stringResource(id = R.string.dark),
                        selected = currentThemeMode == "dark",
                        icon = Icons.Outlined.DarkMode,
                        onClick = { onThemeSelected("dark") }
                    )
                }
            }

            // Customization Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(id = R.string.general),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                SettingsCard {
                    // Dynamic Color (Android 12+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ListItem(
                            headlineContent = { Text(stringResource(id = R.string.dynamic_color)) },
                            supportingContent = { Text(stringResource(id = R.string.dynamic_color_subtitle)) },
                            leadingContent = { Icon(Icons.Outlined.Palette, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = {
                                Switch(checked = useDynamicColor, onCheckedChange = onDynamicColorToggled)
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }

                    // AMOLED Mode (Only visible if dark mode could be active)
                    ListItem(
                        headlineContent = { Text(stringResource(id = R.string.amoled_mode)) },
                        supportingContent = { Text(stringResource(id = R.string.amoled_mode_subtitle)) },
                        leadingContent = { Icon(Icons.Outlined.Contrast, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = {
                            Switch(checked = useAmoled, onCheckedChange = onAmoledToggled)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = {
            Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = {
            RadioButton(selected = selected, onClick = onClick)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
