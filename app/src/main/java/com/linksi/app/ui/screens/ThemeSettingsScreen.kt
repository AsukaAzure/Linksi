package com.linksi.app.ui.screens

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linksi.app.R
import com.linksi.app.ui.components.ExpressiveSettingsCard
import com.linksi.app.ui.components.IconContainer

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
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, stringResource(id = R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Expressive Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Palette,
                            null,
                            modifier = Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        stringResource(id = R.string.theme),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        stringResource(id = R.string.theme_mode_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }

            // Theme Mode Section
            item {
                Text(
                    text = stringResource(id = R.string.theme_mode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )
                ExpressiveSettingsCard {
                    ThemeOption(
                        title = stringResource(id = R.string.system),
                        selected = currentThemeMode == "system",
                        icon = Icons.Outlined.SettingsBrightness,
                        onClick = { onThemeSelected("system") }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    ThemeOption(
                        title = stringResource(id = R.string.light),
                        selected = currentThemeMode == "light",
                        icon = Icons.Outlined.LightMode,
                        onClick = { onThemeSelected("light") }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    ThemeOption(
                        title = stringResource(id = R.string.dark),
                        selected = currentThemeMode == "dark",
                        icon = Icons.Outlined.DarkMode,
                        onClick = { onThemeSelected("dark") }
                    )
                }
            }

            // Customization Section
            item {
                Text(
                    text = stringResource(id = R.string.general),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                )
                ExpressiveSettingsCard {
                    // Dynamic Color (Android 12+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ListItem(
                            headlineContent = { 
                                Text(
                                    stringResource(id = R.string.dynamic_color),
                                    fontWeight = FontWeight.SemiBold
                                ) 
                            },
                            supportingContent = { 
                                Text(
                                    stringResource(id = R.string.dynamic_color_subtitle),
                                    style = MaterialTheme.typography.labelMedium
                                ) 
                            },
                            leadingContent = { IconContainer(Icons.Outlined.AutoAwesome) },
                            trailingContent = {
                                Switch(checked = useDynamicColor, onCheckedChange = onDynamicColorToggled)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }

                    // AMOLED Mode
                    ListItem(
                        headlineContent = { 
                            Text(
                                stringResource(id = R.string.amoled_mode),
                                fontWeight = FontWeight.SemiBold
                            ) 
                        },
                        supportingContent = { 
                            Text(
                                stringResource(id = R.string.amoled_mode_subtitle),
                                style = MaterialTheme.typography.labelMedium
                            ) 
                        },
                        leadingContent = { IconContainer(Icons.Outlined.Contrast) },
                        trailingContent = {
                            Switch(checked = useAmoled, onCheckedChange = onAmoledToggled)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
        headlineContent = { 
            Text(
                title,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            ) 
        },
        leadingContent = {
            IconContainer(
                icon = icon,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        trailingContent = {
            RadioButton(selected = selected, onClick = onClick)
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
