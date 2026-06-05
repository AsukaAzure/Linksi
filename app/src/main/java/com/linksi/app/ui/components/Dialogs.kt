package com.linksi.app.ui.components

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.room.util.TableInfo
import com.linksi.app.domain.model.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.linksi.app.ui.screens.TourStep
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

data class FolderIconOption(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

val FOLDER_ICONS = listOf(
    FolderIconOption("folder", Icons.Outlined.Folder),
    FolderIconOption("work", Icons.Outlined.Work),
    FolderIconOption("bookmark", Icons.Outlined.Bookmark),
    FolderIconOption("star", Icons.Outlined.Star),
    FolderIconOption("heart", Icons.Outlined.FavoriteBorder),
    FolderIconOption("code", Icons.Outlined.Code),
    FolderIconOption("terminal", Icons.Outlined.Terminal),
    FolderIconOption("database", Icons.Outlined.Storage),
    FolderIconOption("web", Icons.Outlined.Language),
    FolderIconOption("ai", Icons.Outlined.AutoAwesome),
    FolderIconOption("cloud", Icons.Outlined.Cloud),
    FolderIconOption("security", Icons.Outlined.Lock),
    FolderIconOption("bug", Icons.Outlined.BugReport),
    FolderIconOption("analytics", Icons.Outlined.Analytics),
    FolderIconOption("tech", Icons.Outlined.Laptop),
    FolderIconOption("mobile", Icons.Outlined.PhoneAndroid),
    FolderIconOption("school", Icons.Outlined.School),
    FolderIconOption("book", Icons.Outlined.AutoStories),
    FolderIconOption("writing", Icons.Outlined.EditNote),
    FolderIconOption("design", Icons.Outlined.Brush),
    FolderIconOption("photo", Icons.Outlined.Photo),
    FolderIconOption("camera", Icons.Outlined.PhotoCamera),
    FolderIconOption("video", Icons.Outlined.VideoCameraBack),
    FolderIconOption("movie", Icons.Outlined.Movie),
    FolderIconOption("music", Icons.Outlined.MusicNote),
    FolderIconOption("audio", Icons.Outlined.Mic),
    FolderIconOption("game", Icons.Outlined.SportsEsports),
    FolderIconOption("shopping", Icons.Outlined.ShoppingCart),
    FolderIconOption("finance", Icons.Outlined.AttachMoney),
    FolderIconOption("wallet", Icons.Outlined.AccountBalanceWallet),
    FolderIconOption("travel", Icons.Outlined.Flight),
    FolderIconOption("food", Icons.Outlined.Restaurant),
    FolderIconOption("health", Icons.Outlined.HealthAndSafety),
    FolderIconOption("fitness", Icons.Outlined.FitnessCenter),
    FolderIconOption("news", Icons.Outlined.Newspaper),
    FolderIconOption("social", Icons.Outlined.Group),
    FolderIconOption("events", Icons.Outlined.Event),
    FolderIconOption("idea", Icons.Outlined.Lightbulb),
    FolderIconOption("home", Icons.Outlined.Home),
    FolderIconOption("personal", Icons.Outlined.Person),
    FolderIconOption("map", Icons.Outlined.Map),
    FolderIconOption("messaging", Icons.Outlined.Chat),
    FolderIconOption("email", Icons.Outlined.Mail),
    FolderIconOption("productivity", Icons.Outlined.TaskAlt),
    FolderIconOption("rocket", Icons.Outlined.RocketLaunch),
    FolderIconOption("tool", Icons.Outlined.Build),
    FolderIconOption("history", Icons.Outlined.History),
    FolderIconOption("archive", Icons.Outlined.Archive),
    FolderIconOption("science", Icons.Outlined.Science),
    FolderIconOption("nature", Icons.Outlined.Terrain),
    FolderIconOption("pets", Icons.Outlined.Pets),
)

fun iconFromName(name: String) =
    FOLDER_ICONS.find { it.name == name }?.icon ?: Icons.Outlined.Folder

// ─────────────────────────────────────────────────────────────
// Add Link Dialog
// ─────────────────────────────────────────────────────────────
@Composable
fun AddLinkDialog(
    folders: List<Folder>,
    isFetchingMetadata: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (url: String, folderId: Long?, reminderAt: Long?) -> Unit,
    onCreateFolder: (name: String, icon: String, color: String) -> Unit = { _, _, _ -> },
    isInTour: Boolean = false,
    tourStep: TourStep = TourStep.DONE,
    onTourNext: () -> Unit = {}
) {
    var url by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var reminderAt by remember { mutableStateOf<Long?>(null) }
    val startingShape =
        RoundedCornerShape(topStart = 20.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    val middleShape =
        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    val endingShape =
        RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Link, null) },
        title = { Text("Save Link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // After URL field — tour step 2
                if (isInTour && tourStep == TourStep.ADD_LINK_DIALOG) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("\uD83D\uDC47", fontSize = 20.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Step 2 of 6", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Paste any URL here to save it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            TextButton(onClick = onTourNext) { Text("Next") }
                        }
                    }
                }

// After reminder picker — tour step 3
                if (isInTour && tourStep == TourStep.REMINDER_IN_DIALOG) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔔", fontSize = 20.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Step 3 of 6", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    "Set a reminder to read this link later",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            TextButton(onClick = onTourNext) { Text("Next") }
                        }
                    }
                }

// After folder row — tour step 4
                if (isInTour && tourStep == TourStep.FOLDER_IN_DIALOG) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📁", fontSize = 20.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Step 4 of 6", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    "Organize links into folders",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            TextButton(onClick = onTourNext) { Text("Next") }
                        }
                    }
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    placeholder = { Text("https://example.com") },
                    leadingIcon = { Icon(Icons.Outlined.Link, null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Folder picker
                // AFTER
                Text("Folder", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    item {
                        FilterChip(
                            shape = startingShape,
                            selected = selectedFolderId == null,
                            onClick = { selectedFolderId = null },
                            label = { Text("📥 Inbox") }
                        )
                    }
                    items(folders) { folder ->
                        FilterChip(
                            shape = middleShape,
                            selected = selectedFolderId == folder.id,
                            onClick = { selectedFolderId = folder.id },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        iconFromName(folder.icon), null,
                                        Modifier.size(14.dp),
                                        tint = Color(android.graphics.Color.parseColor(folder.color))
                                    )
                                    Text(folder.name)
                                }
                            }
                        )
                    }

                    item {
                        FilterChip(
                            shape = endingShape,
                            selected = false,
                            onClick = { showCreateFolder = true },
                            label = { Text("New folder") },
                            leadingIcon = {
                                Icon(Icons.Filled.Add, null, Modifier.size(14.dp))
                            }
                        )
                    }
                }

                if (showCreateFolder) {
                    AddFolderDialog(
                        onDismiss = { showCreateFolder = false },
                        onConfirm = { name, icon, color ->
                            onCreateFolder(name, icon, color)
                            showCreateFolder = false
                        }
                    )
                }

                ReminderPicker(
                    reminderAt = reminderAt,
                    onReminderSet = { reminderAt = it }
                )

                if (isFetchingMetadata) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Fetching preview…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isNotBlank()) onConfirm(
                        url.trim(),
                        selectedFolderId,
                        reminderAt
                    )
                },
                enabled = url.isNotBlank() && !isFetchingMetadata
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

val FOLDER_COLORS = listOf(
    "#6366F1",
    "#8B5CF6",
    "#EC4899",
    "#EF4444",
    "#F59E0B",
    "#10B981",
    "#06B6D4",
    "#3B82F6",
    "#84CC16",
    "#F97316"
)

@Composable
fun AddFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, color: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("folder") }
    var selectedColor by remember { mutableStateOf(FOLDER_COLORS[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.CreateNewFolder, null) },
        title = { Text("New Folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Icon", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FOLDER_ICONS) { option ->
                        Surface(
                            shape = CircleShape,
                            color = if (selectedIcon == option.name)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { selectedIcon = option.name }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    option.icon, null,
                                    Modifier.size(20.dp),
                                    tint = if (selectedIcon == option.name)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text("Color", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FOLDER_COLORS) { color ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .then(
                                    if (color == selectedColor)
                                        Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape
                                        )
                                    else Modifier
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(
                        name.trim(),
                        selectedIcon,
                        selectedColor
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─────────────────────────────────────────────────────────────
// Folder Picker Dialog
// ─────────────────────────────────────────────────────────────
@Composable
fun FolderPickerDialog(
    folders: List<Folder>,
    currentFolderId: Long?,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val scope = rememberCoroutineScope()

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

    BackHandler { dismiss() }

    Dialog(
        onDismissRequest = { dismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,  // handled by BackHandler
            dismissOnClickOutside = false  // handled manually below
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
                                Icon(Icons.Outlined.ArrowBack, "Close")
                            }
                            Text(
                                "Move to folder",
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
                            FolderPickerItem(
                                icon = {
                                    Icon(
                                        Icons.Outlined.FolderOff, null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                name = "No folder",
                                isSelected = currentFolderId == null,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = { select(null) }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 4.dp
                                )
                            )

                            folders.forEach { folder ->
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
                                                    tint = Color(
                                                        android.graphics.Color.parseColor(folder.color)
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    name = folder.name,
                                    isSelected = folder.id == currentFolderId,
                                    color = Color(
                                        android.graphics.Color.parseColor(folder.color)
                                    ),
                                    onClick = { select(folder.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderPickerItem(
    icon: @Composable () -> Unit,
    name: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else
            Color.Transparent,
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

            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )

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

// ─────────────────────────────────────────────────────────────
// Sort Bottom Sheet
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSort: SortOption,
    onSortSelect: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 32.dp)) {
            Text(
                "Sort by",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            SortOption.values().forEach { option ->
                ListItem(
                    headlineContent = { Text(option.label()) },
                    leadingContent = { Icon(option.icon(), null) },
                    trailingContent = {
                        if (option == currentSort) Icon(
                            Icons.Filled.Check,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable { onSortSelect(option) }
                )
            }
        }
    }
}

private fun SortOption.label() = when (this) {
    SortOption.DATE_NEWEST -> "Newest first"
    SortOption.DATE_OLDEST -> "Oldest first"
    SortOption.TITLE_AZ -> "Title A → Z"
    SortOption.TITLE_ZA -> "Title Z → A"
    SortOption.DOMAIN -> "By domain"
}

private fun SortOption.icon() = when (this) {
    SortOption.DATE_NEWEST, SortOption.DATE_OLDEST -> Icons.Outlined.Schedule
    SortOption.TITLE_AZ, SortOption.TITLE_ZA -> Icons.Outlined.SortByAlpha
    SortOption.DOMAIN -> Icons.Outlined.Language
}

@Composable
fun EditLinkDialog(
    link: Link,
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onConfirm: (Link) -> Unit
) {
    var url by remember { mutableStateOf(link.url) }
    var title by remember { mutableStateOf(link.title) }
    var description by remember { mutableStateOf(link.description) }
    var reminderAt by remember { mutableStateOf(link.reminderAt) }
    var selectedFolderId by remember {
        mutableStateOf(link.folderId)
    }
    val startingShape =
        RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 4.dp,
            bottomStart = 20.dp,
            bottomEnd = 4.dp
        )
    val middleShape =
        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    val endingShape = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 20.dp,
        bottomStart = 4.dp,
        bottomEnd = 20.dp
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Edit, null) },
        title = { Text("Edit Link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    label = { Text("URL") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (folders.isNotEmpty()) {
                    Text("Folder", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        item {
                            FilterChip(
                                shape = startingShape,
                                selected = selectedFolderId == null,
                                onClick = { selectedFolderId = null },
                                label = { Text("\uD83D\uDCE5 Inbox") })
                        }
                        itemsIndexed(folders) { index, folder ->
                            val currentShape = if (index == folders.lastIndex){
                                endingShape
                            } else {
                                middleShape
                            }
                            FilterChip(
                                shape = currentShape,
                                selected = selectedFolderId == folder.id,
                                onClick = { selectedFolderId = folder.id },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            iconFromName(folder.icon),
                                            null,
                                            Modifier.size(14.dp),
                                            tint = Color(
                                                android.graphics.Color.parseColor(
                                                    folder.color
                                                )
                                            )
                                        )
                                        Text(folder.name)
                                    }
                                })
                        }
                    }
                }
                ReminderPicker(reminderAt = reminderAt, onReminderSet = { reminderAt = it })
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    link.copy(
                        url = url.trim(),
                        title = title.trim(),
                        description = description.trim(),
                        folderId = selectedFolderId,
                        reminderAt = reminderAt
                    )
                )
            }, enabled = url.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderPicker(
    reminderAt: Long?,
    onReminderSet: (Long?) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    val dateFormatter =
        remember { SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->

    }

    fun setReminderWithPermissionCheck(time: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        onReminderSet(time)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Outlined.Notifications, null,
            Modifier.size(18.dp),
            tint = if (reminderAt != null) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (reminderAt != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        dateFormatter.format(Date(reminderAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onReminderSet(null) },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close, null, Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                val startingShape =
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 4.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 4.dp
                    )
                val middleShape =
                    RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 4.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp
                    )
                val endingShape =
                    RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 20.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 20.dp
                    )
                val suggestions = listOf(
                    "1h" to (System.currentTimeMillis() + 3_600_000L),
                    "Tonight" to todayAt(21, 0),
                    "Tomorrow" to tomorrowAt(9, 0),
                    "Weekend" to nextWeekendAt(9, 0),
                )
                items(suggestions) { (label, time) ->
                    SuggestionChip(
                        shape = if (label == "1h") startingShape else middleShape,
                        onClick = { setReminderWithPermissionCheck(time) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
                item {
                    SuggestionChip(
                        shape = endingShape,
                        onClick = { showDatePicker = true },
                        label = { Text("Custom", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = 9, initialMinute = 0)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pick a time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val date = selectedDate ?: System.currentTimeMillis()
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = date
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                    }
                    setReminderWithPermissionCheck(cal.timeInMillis)
                    showTimePicker = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

private fun todayAt(hour: Int, minute: Int): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        if (timeInMillis < System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
}

private fun tomorrowAt(hour: Int, minute: Int): Long {
    return Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }.timeInMillis
}

private fun nextWeekendAt(hour: Int, minute: Int): Long {
    return Calendar.getInstance().apply {
        while (get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }.timeInMillis
}

@Composable
fun EditFolderDialog(
    folder: Folder,
    onDismiss: () -> Unit,
    onConfirm: (name: String, icon: String, color: String) -> Unit
) {
    var name by remember { mutableStateOf(folder.name) }
    var selectedIcon by remember { mutableStateOf(folder.icon) }
    var selectedColor by remember { mutableStateOf(folder.color) }
    var iconSearch by remember { mutableStateOf("") }

    val filteredIcons = remember(iconSearch) {
        if (iconSearch.isBlank()) FOLDER_ICONS
        else FOLDER_ICONS.filter { it.name.contains(iconSearch, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Edit, null) },
        title = { Text("Edit Folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Icon", style = MaterialTheme.typography.labelMedium)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.height(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredIcons) { option ->
                        Surface(
                            shape = CircleShape,
                            color = if (selectedIcon == option.name)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .clickable { selectedIcon = option.name }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    option.icon, null,
                                    Modifier.size(20.dp),
                                    tint = if (selectedIcon == option.name)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text("Color", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FOLDER_COLORS) { color ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .then(
                                    if (color == selectedColor)
                                        Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape
                                        )
                                    else Modifier
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name.trim(), selectedIcon, selectedColor)
                },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

