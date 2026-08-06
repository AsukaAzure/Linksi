package com.linksi.app.ui.components

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import com.linksi.app.R
import com.linksi.app.domain.model.Folder
import com.linksi.app.domain.model.SortOption
import com.linksi.app.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map

data class FolderIconOption(
    val name: String,
    val icon: ImageVector
)

val folderIcons = listOf(
    FolderIconOption("folder", Icons.Outlined.Folder),
    FolderIconOption("work", Icons.Outlined.WorkOutline),
    FolderIconOption("home", Icons.Outlined.Home),
    FolderIconOption("star", Icons.Outlined.StarOutline),
    FolderIconOption("favorite", Icons.Outlined.FavoriteBorder),
    FolderIconOption("bookmark", Icons.Outlined.BookmarkBorder),
    FolderIconOption("book", Icons.Outlined.Book),
    FolderIconOption("movie", Icons.Outlined.Movie),
    FolderIconOption("music", Icons.Outlined.MusicNote),
    FolderIconOption("game", Icons.Outlined.Gamepad),
    FolderIconOption("code", Icons.Outlined.Code),
    FolderIconOption("school", Icons.Outlined.School),
    FolderIconOption("travel", Icons.Outlined.Flight),
    FolderIconOption("food", Icons.Outlined.Restaurant),
    FolderIconOption("health", Icons.Outlined.HealthAndSafety),
    FolderIconOption("finance", Icons.Outlined.Payments),
    FolderIconOption("shopping", Icons.Outlined.ShoppingCart),
    FolderIconOption("camera", Icons.Outlined.PhotoCamera),
    FolderIconOption("lightbulb", Icons.Outlined.Lightbulb),
    FolderIconOption("palette", Icons.Outlined.Palette),
    FolderIconOption("pets", Icons.Outlined.Pets),
    FolderIconOption("fitness", Icons.Outlined.FitnessCenter)
)

val folderColors = listOf(
    "#6750A4", // Deep Purple (Default)
    "#F44336", // Red
    "#E91E63", // Pink
    "#9C27B0", // Purple
    "#3F51B5", // Indigo
    "#2196F3", // Blue
    "#03A9F4", // Light Blue
    "#00BCD4", // Cyan
    "#009688", // Teal
    "#4CAF50", // Green
    "#8BC34A", // Light Green
    "#CDDC39", // Lime
    "#FFEB3B", // Yellow
    "#FFC107", // Amber
    "#FF9800", // Orange
    "#FF5722", // Deep Orange
    "#795548", // Brown
    "#607D8B", // Blue Grey
    "#000000"  // Black
)

@Composable
fun iconFromName(name: String): ImageVector {
    return folderIcons.find { it.name == name }?.icon ?: Icons.Outlined.Folder
}

@Composable
fun AddFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
    initialName: String = "",
    initialIcon: String = "folder",
    initialColor: String = "#6750A4",
    title: String = stringResource(R.string.new_folder)
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedIcon by remember { mutableStateOf(initialIcon) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.folder_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (name.isNotBlank()) onConfirm(name, selectedIcon, selectedColor)
                    })
                )

                Text(
                    stringResource(R.string.icon),
                    style = MaterialTheme.typography.titleSmall
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(folderIcons) { iconOption ->
                        val isSelected = selectedIcon == iconOption.name
                        Surface(
                            onClick = { selectedIcon = iconOption.name },
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    iconOption.icon,
                                    null,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text(
                    stringResource(R.string.color),
                    style = MaterialTheme.typography.titleSmall
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(folderColors) { colorHex ->
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        val isSelected = selectedColor == colorHex
                        Surface(
                            onClick = { selectedColor = colorHex },
                            shape = CircleShape,
                            color = color,
                            modifier = Modifier.size(40.dp),
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                            else null
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (colorHex == "#FFFFFF" || colorHex == "#FFEB3B") Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, selectedIcon, selectedColor) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun EditFolderDialog(
    folder: Folder,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    AddFolderDialog(
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        initialName = folder.name,
        initialIcon = folder.icon,
        initialColor = folder.color,
        title = stringResource(R.string.edit_folder)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSort: SortOption,
    onSortSelect: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Text(
                stringResource(R.string.sort_by),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            HorizontalDivider()

            val options = listOf(
                SortOption.DATE_NEWEST to (stringResource(R.string.newest_first) to Icons.Outlined.Schedule),
                SortOption.DATE_OLDEST to (stringResource(R.string.oldest_first) to Icons.Outlined.Schedule),
                SortOption.TITLE_AZ to (stringResource(R.string.title_az) to Icons.Outlined.SortByAlpha),
                SortOption.TITLE_ZA to (stringResource(R.string.title_za) to Icons.Outlined.SortByAlpha),
                SortOption.DOMAIN to (stringResource(R.string.by_domain) to Icons.Outlined.Language)
            )

            options.forEach { (option, pair) ->
                val (label, icon) = pair
                ListItem(
                    headlineContent = { Text(label) },
                    leadingContent = { Icon(icon, null) },
                    trailingContent = {
                        if (option == currentSort) {
                            Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier.clickable {
                        onSortSelect(option)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
fun FolderPickerDialog(
    folders: List<Folder>,
    currentFolderId: Long?,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit,
    onCreateFolder: ((String, String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val securityPrefs by remember {
        context.dataStore.data.map { prefs ->
            Triple(
                prefs[SECURITY_PIN] ?: "",
                prefs[SECURITY_BIOMETRIC_ENABLED] ?: false,
                prefs[SECURITY_FOLDER_LOCK_ENABLED] ?: false
            )
        }
    }.collectAsState(initial = Triple("", false, false))

    var visible by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var folderToUnlock by remember { mutableStateOf<Folder?>(null) }
    var showPinVerify by remember { mutableStateOf(false) }

    // Trigger enter animation on first frame
    LaunchedEffect(Unit) { visible = true }

    // When visible becomes false, wait for animation then execute pending action
    LaunchedEffect(visible) {
        if (!visible) {
            delay(280)  // match exit animation duration
            pendingAction?.invoke()
        }
    }

    fun dismiss() {
        pendingAction = { onDismiss() }
        visible = false
    }

    fun select(folderId: Long?) {
        pendingAction = { onSelect(folderId) }
        visible = false
    }

    fun handleFolderClick(folder: Folder) {
        val (savedPin, biometricEnabled, folderLockEnabled) = securityPrefs
        if (folderLockEnabled && folder.isLocked && savedPin.isNotEmpty()) {
            folderToUnlock = folder
            if (biometricEnabled && SecurityManager.canUseBiometric(context)) {
                SecurityManager.showBiometricPrompt(
                    activity = context as FragmentActivity,
                    title = context.getString(R.string.unlock_folder),
                    subtitle = folder.name,
                    onSuccess = { select(folder.id) },
                    onError = { showPinVerify = true }
                )
            } else {
                showPinVerify = true
            }
        } else {
            if (folder.id == currentFolderId) {
                select(null)
            } else {
                select(folder.id)
            }
        }
    }

    BackHandler { dismiss() }

    Dialog(
        onDismissRequest = { dismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Scrim
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(280))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { dismiss() }
                )
            }

            // Left drawer
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(300)),
                exit = slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(280)),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .fillMaxHeight(0.65f),
                    shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { dismiss() }) {
                                Icon(Icons.Outlined.ArrowBack, stringResource(R.string.close))
                            }
                            Text(
                                stringResource(R.string.move_to_folder),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        HorizontalDivider()

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp)
                        ) {
                            var showCreateFolder by remember { mutableStateOf(false) }

                            FolderPickerItem(
                                icon = {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Outlined.CreateNewFolder, null,
                                                Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                },
                                name = stringResource(R.string.new_folder),
                                isSelected = false,
                                color = MaterialTheme.colorScheme.primary,
                                onClick = { showCreateFolder = true }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )

                            // Existing folders
                            folders.forEach { folder ->
                                val isCurrentFolder = folder.id == currentFolderId
                                FolderPickerItem(
                                    icon = {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(
                                                android.graphics.Color.parseColor(folder.color)
                                            ).copy(alpha = 0.15f),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    iconFromName(folder.icon), null,
                                                    Modifier.size(18.dp),
                                                    tint = Color(android.graphics.Color.parseColor(folder.color))
                                                )
                                            }
                                        }
                                    },
                                    name = folder.name,
                                    isSelected = isCurrentFolder,
                                    isLocked = folder.isLocked,
                                    color = Color(android.graphics.Color.parseColor(folder.color)),
                                    subtitle = if (isCurrentFolder) stringResource(R.string.tap_to_remove) else null,
                                    onClick = { handleFolderClick(folder) }
                                )
                            }

                            // Create folder dialog
                            if (showCreateFolder) {
                                AddFolderDialog(
                                    onDismiss = { showCreateFolder = false },
                                    onConfirm = { name, icon, color ->
                                        onCreateFolder?.invoke(name, icon, color)
                                        showCreateFolder = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPinVerify && folderToUnlock != null) {
        PinVerifyDialog(
            savedPin = securityPrefs.first,
            onSuccess = {
                showPinVerify = false
                select(folderToUnlock?.id)
            },
            onDismiss = {
                showPinVerify = false
                folderToUnlock = null
            }
        )
    }
}

@Composable
fun PinVerifyDialog(
    savedPin: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(enteredPin) {
        if (enteredPin.length == 4) {
            if (enteredPin == savedPin) {
                onSuccess()
            } else {
                error = true
                delay(500)
                enteredPin = ""
                error = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.enter_pin)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    repeat(4) { index ->
                        val isFilled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (error) MaterialTheme.colorScheme.error
                                    else if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                val numpad = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "backspace")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    numpad.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { item ->
                                when (item) {
                                    "backspace" -> {
                                        IconButton(
                                            onClick = { if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.Outlined.Backspace, null)
                                        }
                                    }
                                    "" -> {
                                        Spacer(modifier = Modifier.size(48.dp))
                                    }
                                    else -> {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .clickable { if (enteredPin.length < 4) enteredPin += item }
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = item,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun FolderPickerItem(
    icon: @Composable () -> Unit,
    name: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    subtitle: String? = null,
    isLocked: Boolean = false
) {
    Surface(
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon()
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isLocked) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Outlined.Lock,
                            null,
                            Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Filled.Check, null,
                    Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
