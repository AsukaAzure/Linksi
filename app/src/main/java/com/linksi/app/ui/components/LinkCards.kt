package com.linksi.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.linksi.app.domain.model.Folder
import com.linksi.app.domain.model.Link
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LinkCard(
    link: Link,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongPress: () -> Unit = {},
    folders: List<Folder>,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit,
    onMoveToFolder: (Long?) -> Unit,
    onEdit: () -> Unit,
    onMarkRead: () -> Unit = {},
    onFolderClick: (Folder) -> Unit = {},
    onPin: () -> Unit = {},
    onSetNote: (String) -> Unit = {},
    onSetReminder: (Long?) -> Unit = {},
    onSetExpiry: (Long?) -> Unit = {},
    onSetTags: (List<String>) -> Unit = {},
    allTags: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val cardColor = when {
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        !link.isRead -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    var swipeConfirmed by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.6f },
        confirmValueChange = { value ->
            // FIX #3: read progress inline so the check is always current
            val confirmed = swipeConfirmed
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    if (confirmed) {
                        swipeConfirmed = false
                        onDelete()
                        true
                    } else {
                        false
                    }
                }

                SwipeToDismissBoxValue.StartToEnd -> {
                    if (confirmed) {
                        swipeConfirmed = false
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "${link.title}\n${link.url}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share link via"))
                    }
                    false
                }

                else -> false
            }
        }
    )

    LaunchedEffect(dismissState.progress) {
        swipeConfirmed = dismissState.progress >= 0.5f
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isDelete = direction == SwipeToDismissBoxValue.EndToStart
            val isShare = direction == SwipeToDismissBoxValue.StartToEnd

            val bgColor by animateColorAsState(
                targetValue = when {
                    isDelete -> MaterialTheme.colorScheme.errorContainer
                    isShare -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                },
                label = "swipe_bg"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CardDefaults.elevatedShape)
                    .background(bgColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (isDelete) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                if (isDelete) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (swipeConfirmed) Icons.Outlined.Delete else Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            if (swipeConfirmed) "Release to delete" else "Keep swiping…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                } else if (isShare) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (swipeConfirmed) Icons.Outlined.Share else Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            if (swipeConfirmed) "Release to share" else "Keep swiping…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardDefaults.elevatedShape) // VERY important
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true),
                    onClick = onClick,
                    onLongClick = onLongPress
                ),
            colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = if (!link.isRead) 2.dp else 0.dp
            )
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                // Selection checkbox
                AnimatedVisibility(visible = isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onLongPress() },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .align(Alignment.CenterVertically)
                    )
                }

                // Domain color accent bar
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(domainColor(link.domain))
                )

                Column(modifier = Modifier.padding(12.dp)) {
                    // Top row: favicon + title + menu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Favicon
                        AsyncImage(
                            model = link.faviconUrl,
                            contentDescription = "Favicon",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            // Domain + unread indicator
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Pin indicator
                                if (link.isPinned) {
                                    Icon(
                                        Icons.Filled.PushPin,
                                        null,
                                        Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                if (!link.isRead) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(
                                    text = link.domain.ifBlank { "link" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }

                            // Title
                            Text(
                                text = link.title.ifBlank { link.url },
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Description
                            if (link.description.isNotBlank()) {
                                Text(
                                    text = link.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (link.tags.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    link.tags.forEach { tag ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF22C55E).copy(alpha = 0.12f),
                                            border = BorderStroke(
                                                0.5.dp,
                                                Color(0xFF22C55E).copy(alpha = 0.4f)
                                            )
                                        ) {
                                            Text(
                                                "#$tag",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = Color(0xFF22C55E),
                                                modifier = Modifier.padding(
                                                    horizontal = 6.dp,
                                                    vertical = 2.dp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Menu button
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.MoreVert, "More", Modifier.size(18.dp))
                            }

                            if (showMenu) {
                                LinkOptionsSheet(
                                    link = link,
                                    folder = folders.find { it.id == link.folderId },
                                    onDismiss = { showMenu = false },
                                    onEdit = { showMenu = false; onEdit() },
                                    onDelete = { showMenu = false; onDelete() },
                                    onMoveToFolder = { showMenu = false; showFolderPicker = true },
                                    onShare = {
                                        showMenu = false
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "${link.title}\n${link.url}"
                                            )
                                            type = "text/plain"
                                        }
                                        context.startActivity(
                                            Intent.createChooser(
                                                sendIntent,
                                                "Share link via"
                                            )
                                        )
                                    },
                                    onToggleFavorite = { showMenu = false; onFavoriteToggle() },
                                    onMarkRead = { },
                                    onRefreshMetadata = { },
                                    onPin = { onPin() },
                                    onSetNote = { note -> onSetNote(note) },
                                    onSetReminder = { time -> onSetReminder(time) },
                                    onSetExpiry = { time -> onSetExpiry(time) },
                                    onSetTags = { tags -> onSetTags(tags) },
                                    allTags = allTags
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Folder badge
                        val folder = folders.find { it.id == link.folderId }
                        if (folder != null) {
                            AssistChip(
                                onClick = { onFolderClick(folder) },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            iconFromName(folder.icon),
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = Color(
                                                android.graphics.Color.parseColor(folder.color)
                                            )
                                        )
                                        Text(
                                            folder.name,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                },
                                modifier = Modifier.height(24.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Spacer(Modifier.weight(1f))

                        if (link.reminderAt != null && link.reminderAt > System.currentTimeMillis()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.6f
                                        )
                                    )
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Notifications, null,
                                    Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    formatTimeLeft(link.reminderAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                        }

                        // ── Expiry indicator ──────────────────────────────
                        if (link.expiresAt != null && link.expiresAt > System.currentTimeMillis()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer.copy(
                                            alpha = 0.6f
                                        )
                                    )
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Timer, null,
                                    Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    formatTimeLeft(link.expiresAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                        }

                        // Date
                        Text(
                            text = formatDate(link.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.width(4.dp))

                        // Favorite
                        IconButton(
                            onClick = onFavoriteToggle,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (link.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                modifier = Modifier.size(18.dp),
                                tint = if (link.isFavorite)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (showFolderPicker) {
            FolderPickerDialog(
                folders = folders,
                currentFolderId = link.folderId,
                onSelect = { folderId -> onMoveToFolder(folderId); showFolderPicker = false },
                onDismiss = { showFolderPicker = false }
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LinkGridCard(
    link: Link,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongPress: () -> Unit = {},
    folders: List<Folder>,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit,
    onMoveToFolder: (Long?) -> Unit,
    onEdit: () -> Unit,
    onMarkRead: () -> Unit = {},
    onFolderClick: (Folder) -> Unit = {},
    onPin: () -> Unit = {},
    onSetNote: (String) -> Unit = {},
    onSetReminder: (Long?) -> Unit = {},
    onSetExpiry: (Long?) -> Unit = {},
    onSetTags: (List<String>) -> Unit = {},
    allTags: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardDefaults.elevatedShape) // VERY important
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Box {
            Column {
                if (link.previewImageUrl.isNotBlank()) {
                    var imageLoadFailed by remember { mutableStateOf(false) }
                    if (!imageLoadFailed) {
                        AsyncImage(
                            model = link.previewImageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentScale = ContentScale.Crop,
                            onError = { imageLoadFailed = true }
                        )
                    } else {
                        // Fallback to favicon gradient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = link.faviconUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = link.faviconUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                Column(Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = link.domain.ifBlank { "link" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )

                        // Menu button in Grid
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.MoreVert, "More", Modifier.size(16.dp))
                            }

                            if (showMenu) {
                                LinkOptionsSheet(
                                    link = link,
                                    folder = folders.find { it.id == link.folderId },
                                    onDismiss = { showMenu = false },
                                    onEdit = { showMenu = false; onEdit() },
                                    onDelete = { showMenu = false; onDelete() },
                                    onMoveToFolder = { showMenu = false; showFolderPicker = true },
                                    onShare = {
                                        showMenu = false
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "${link.title}\n${link.url}"
                                            )
                                            type = "text/plain"
                                        }
                                        context.startActivity(
                                            Intent.createChooser(
                                                sendIntent,
                                                "Share link via"
                                            )
                                        )
                                    },
                                    onToggleFavorite = { showMenu = false; onFavoriteToggle() },
                                    onMarkRead = { },
                                    onRefreshMetadata = { },
                                    onPin = { onPin() },
                                    onSetNote = { note -> onSetNote(note) },
                                    onSetReminder = { time -> onSetReminder(time) },
                                    onSetExpiry = { time -> onSetExpiry(time) },
                                    onSetTags = { tags -> onSetTags(tags) },
                                    allTags = allTags
                                )
                            }
                        }
                    }
                    Text(
                        text = link.title.ifBlank { link.url },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDate(link.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = onFavoriteToggle,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                if (link.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (link.isFavorite)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onLongPress() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                )
            }
        }
    }

    if (showFolderPicker) {
        FolderPickerDialog(
            folders = folders,
            currentFolderId = link.folderId,
            onSelect = { folderId -> onMoveToFolder(folderId); showFolderPicker = false },
            onDismiss = { showFolderPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkOptionsSheet(
    link: Link,
    folder: Folder?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveToFolder: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMarkRead: () -> Unit,
    onRefreshMetadata: () -> Unit,
    onPin: () -> Unit,
    onSetNote: (String) -> Unit,
    onSetReminder: (Long?) -> Unit,
    onSetExpiry: (Long?) -> Unit,
    onSetTags: (List<String>) -> Unit = {},
    allTags: List<String> = emptyList()
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showReminderSheet by remember { mutableStateOf(false) }
    var showExpirySheet by remember { mutableStateOf(false) }
    var showNoteSheet by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }

    fun dismiss() {
        scope.launch { sheetState.hide(); onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    if (link.previewImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = link.previewImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            domainColor(link.domain),
                                            domainColor(link.domain).copy(alpha = 0.4f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = link.faviconUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }

                    // Gradient scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.3f), // Top shadow for handle
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.65f) // Bottom shadow for text
                                    )
                                )
                            )
                    )

                    // Drag handle
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                            .width(40.dp)
                            .height(4.dp),
                        shape = RoundedCornerShape(2.dp),
                        color = Color.White.copy(alpha = 0.5f)
                    ) {}

                    // Link name + domain at bottom
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            link.title.ifBlank { link.url },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AsyncImage(
                                model = link.faviconUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                            )
                            Text(
                                link.domain.ifBlank { "link" },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ── Row 1: Copy link (wide) + Share + Delete ──────
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Copy link — wide
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val clipboard = context.getSystemService(
                                        android.content.ClipboardManager::class.java
                                    )
                                    clipboard?.setPrimaryClip(
                                        android.content.ClipData.newPlainText("url", link.url)
                                    )
                                    dismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.ContentCopy, null,
                                    Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Copy link",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        link.url,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Share square
                        OptionsSquareButton(
                            icon = Icons.Outlined.Share,
                            onClick = { onShare(); dismiss() }
                        )

                        // Delete square
                        OptionsSquareButton(
                            icon = Icons.Outlined.Delete,
                            iconTint = MaterialTheme.colorScheme.error,
                            onClick = { onDelete(); dismiss() }
                        )
                    }

                    // ── Edit bookmark ─────────────────────────────────
                    OptionsFullRow(
                        icon = Icons.Outlined.Edit,
                        title = "Edit bookmark",
                        onClick = { onEdit(); dismiss() }
                    )

                    // ── Move to folder ────────────────────────────────
                    OptionsFullRow(
                        icon = Icons.Outlined.Folder,
                        title = "Move to folder",
                        onClick = { onMoveToFolder(); dismiss() }
                    )

                    // ── Set note ──────────────────────────────────────
                    OptionsFullRow(
                        icon = Icons.Outlined.Notes,
                        title = "Set note",
                        subtitle = link.note.ifBlank { null },
                        onClick = { showNoteSheet = true }
                    )

                    // ── Set reminder ──────────────────────────────────
                    OptionsFullRow(
                        icon = Icons.Outlined.Notifications,
                        title = "Set reminder",
                        subtitle = link.reminderAt?.let {
                            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(it))
                        },
                        iconTint = if (link.reminderAt != null) MaterialTheme.colorScheme.primary else null,
                        onClick = { showReminderSheet = true }
                    )

                    // ── Set expiration ────────────────────────────────
                    OptionsFullRow(
                        icon = Icons.Outlined.Timer,
                        title = "Set expiration",
                        subtitle = link.expiresAt?.let {
                            "Expires ${
                                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(
                                    Date(
                                        it
                                    )
                                )
                            }"
                        } ?: "Auto-delete after selected time",
                        onClick = { showExpirySheet = true }
                    )

                    // ── Manage Tags ───────────────────────────────────
                    OptionsFullRow(
                        icon = Icons.Outlined.Tag,
                        title = "Manage Tags",
                        subtitle = if (link.tags.isNotEmpty())
                            link.tags.joinToString(" · ") { "#$it" }
                        else
                            "Add tags for easy search",
                        onClick = { showTagSheet = true }
                    )

                    // ── Pin to Top ────────────────────────────────────
                    OptionsFullRow(
                        icon = if (link.isPinned) Icons.Outlined.PushPin else Icons.Outlined.PushPin,
                        title = if (link.isPinned) "Unpin link" else "Pin to top",
                        subtitle = if (!link.isPinned) "Keep this link at the top (max 3)" else null,
                        iconTint = if (link.isPinned) MaterialTheme.colorScheme.primary else null,
                        onClick = { onPin(); dismiss() }
                    )

                    Spacer(Modifier.height(4.dp))
                }
            }

            FloatingActionButton(
                onClick = {
                    val browserIntent = Intent(Intent.ACTION_VIEW, link.url.toUri())
                    context.startActivity(browserIntent)
                    dismiss()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Outlined.OpenInNew, contentDescription = "Open Link")
            }
        }
    }

    if (showNoteSheet) {
        NoteBottomSheet(
            currentNote = link.note,
            onSave = { note -> onSetNote(note); showNoteSheet = false; dismiss() },
            onDismiss = { showNoteSheet = false }
        )
    }

    if (showReminderSheet) {
        ReminderBottomSheet(
            currentReminder = link.reminderAt,
            onSet = { time -> onSetReminder(time); showReminderSheet = false; dismiss() },
            onDismiss = { showReminderSheet = false }
        )
    }

    if (showExpirySheet) {
        ExpiryBottomSheet(
            currentExpiry = link.expiresAt,
            onSet = { time -> onSetExpiry(time); showExpirySheet = false; dismiss() },
            onDismiss = { showExpirySheet = false }
        )
    }

    if (showTagSheet) {
        TagManagerSheet(
            currentTags = link.tags,
            allExistingTags = allTags,
            onSave = { tags -> onSetTags(tags); showTagSheet = false; dismiss() },
            onDismiss = { showTagSheet = false }
        )
    }

//    if (showFolderPicker) {
//        FolderPickerDialog(
//            folders = folders,
//            currentFolderId = link.folderId,
//            onSelect = { folderId ->
//                onMoveToFolder(folderId); showFolderPicker = false; dismiss()
//            },
//            onDismiss = { showFolderPicker = false }
//        )
//    }
}

// ── Square icon button ────────────────────────────────────────
@Composable
fun OptionsSquareButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .size(56.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon, null,
                Modifier.size(22.dp),
                tint = iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Full width row button ─────────────────────────────────────
@Composable
fun OptionsFullRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = if (subtitle != null) 14.dp else 16.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                icon, null,
                Modifier.size(22.dp),
                tint = iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Icon only square button ───────────────────────────────────
@Composable
fun SheetIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String? = null,
    iconTint: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .size(if (label != null) 64.dp else 52.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon, null,
                Modifier.size(20.dp),
                tint = iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (label != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Text + icon wide button ───────────────────────────────────
@Composable
fun SheetTextButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    iconTint: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .height(52.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon, null,
                Modifier.size(16.dp),
                tint = iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── Reminder bottom sheet ─────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderBottomSheet(
    currentReminder: Long?,
    onSet: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance()

    // Tonight 9:30 PM
    val tonightTime = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 21)
        set(Calendar.MINUTE, 30)
        set(Calendar.SECOND, 0)
        if (timeInMillis < now) add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis

    // Tomorrow 8:30 PM
    val tomorrowTime = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 20)
        set(Calendar.MINUTE, 30)
        set(Calendar.SECOND, 0)
    }.timeInMillis

    // Next week same time
    val nextWeekTime = Calendar.getInstance().apply {
        add(Calendar.WEEK_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 20)
        set(Calendar.MINUTE, 30)
        set(Calendar.SECOND, 0)
    }.timeInMillis

    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("EEE h:mm a", Locale.getDefault()) }
    val fullFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    fun dismiss() {
        scope.launch { sheetState.hide(); onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                ) {}
            }

            Text(
                "Set reminder",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // 2x2 grid of options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Later today
                ReminderOptionCard(
                    icon = Icons.Outlined.WbTwilight,
                    title = "Later today",
                    subtitle = timeFormatter.format(Date(tonightTime)),
                    modifier = Modifier.weight(1f),
                    onClick = { onSet(tonightTime); dismiss() }
                )
                // Tomorrow
                ReminderOptionCard(
                    icon = Icons.Outlined.LightMode,
                    title = "Tomorrow",
                    subtitle = timeFormatter.format(Date(tomorrowTime)),
                    modifier = Modifier.weight(1f),
                    onClick = { onSet(tomorrowTime); dismiss() }
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Next week
                ReminderOptionCard(
                    icon = Icons.Outlined.CalendarMonth,
                    title = "Next week",
                    subtitle = dateFormatter.format(Date(nextWeekTime)),
                    modifier = Modifier.weight(1f),
                    onClick = { onSet(nextWeekTime); dismiss() }
                )
                // Pick custom
                ReminderOptionCard(
                    icon = Icons.Outlined.EditCalendar,
                    title = "Pick date & time",
                    subtitle = fullFormatter.format(Date(now)),
                    modifier = Modifier.weight(1f),
                    onClick = { showDatePicker = true }
                )
            }

            Spacer(Modifier.height(12.dp))

            if (currentReminder != null) {
                OutlinedButton(
                    onClick = { onSet(null); dismiss() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.NotificationsOff, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Remove reminder")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Save / clear button
            Button(
                onClick = { dismiss() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Cancel", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    // Date picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = now
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

    // Time picker
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = 20, initialMinute = 30)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pick a time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val date = selectedDate ?: now
                    val finalTime = Calendar.getInstance().apply {
                        timeInMillis = date
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                    }.timeInMillis
                    onSet(finalTime)
                    showTimePicker = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ReminderOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .height(90.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                icon, null,
                Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteBottomSheet(
    currentNote: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf(currentNote) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun dismiss() {
        scope.launch { sheetState.hide(); onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    Modifier
                        .width(40.dp)
                        .height(4.dp), CircleShape,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                ) {}
            }

            Text(
                "Set note", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Write a note about this link…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(16.dp),
                maxLines = 6
            )

            Button(
                onClick = { onSave(note) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Save note", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryBottomSheet(
    currentExpiry: Long?,
    onSet: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val now = System.currentTimeMillis()

    fun dismiss() {
        scope.launch { sheetState.hide(); onDismiss() }
    }

    val laterToday = Calendar.getInstance().apply {
        add(Calendar.HOUR_OF_DAY, 1)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.timeInMillis

    val tomorrow = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.timeInMillis

    val nextWeek = Calendar.getInstance().apply {
        add(Calendar.WEEK_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.timeInMillis

    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("EEE h:mm a", Locale.getDefault()) }
    val fullFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        scrimColor = Color.Black.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp), //horizontal 16
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    Modifier
                        .width(40.dp)
                        .height(4.dp), CircleShape,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                ) {}
            }

            Text(
                "Set expiration",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 2x2 grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReminderOptionCard(
                    icon = Icons.Outlined.WbTwilight,
                    title = "Later today",
                    subtitle = timeFormatter.format(Date(laterToday)),
                    modifier = Modifier.weight(1f),
                    onClick = { onSet(laterToday); dismiss() }
                )
                ReminderOptionCard(
                    icon = Icons.Outlined.LightMode,
                    title = "Tomorrow",
                    subtitle = timeFormatter.format(Date(tomorrow)),
                    modifier = Modifier.weight(1f),
                    onClick = { onSet(tomorrow); dismiss() }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReminderOptionCard(
                    icon = Icons.Outlined.CalendarMonth,
                    title = "Next week",
                    subtitle = dateFormatter.format(Date(nextWeek)),
                    modifier = Modifier.weight(1f),
                    onClick = { onSet(nextWeek); dismiss() }
                )
                ReminderOptionCard(
                    icon = Icons.Outlined.EditCalendar,
                    title = "Pick date & time",
                    subtitle = fullFormatter.format(Date(now)),
                    modifier = Modifier.weight(1f),
                    onClick = { showDatePicker = true }
                )
            }

            // Remove expiry if set
            if (currentExpiry != null) {
                OutlinedButton(
                    onClick = { onSet(null); dismiss() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.Clear, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Remove expiration")
                }
            }

            // Save button (dismiss without changing)
            Button(
                onClick = { dismiss() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) { Text("Cancel", style = MaterialTheme.typography.titleMedium) }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = now)
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
        val timePickerState = rememberTimePickerState(initialHour = 8, initialMinute = 0)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pick expiry time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val final = Calendar.getInstance().apply {
                        timeInMillis = selectedDate ?: now
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                    }.timeInMillis
                    onSet(final)
                    showTimePicker = false
                    dismiss()
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SheetOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconTint: Color? = null,
    labelColor: Color? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                label,
                color = labelColor ?: MaterialTheme.colorScheme.onSurface
            )
        },
        leadingContent = {
            Icon(
                icon, null,
                tint = iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagManagerSheet(
    currentTags: List<String>,
    allExistingTags: List<String>,  // all tags from all links
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTags by remember { mutableStateOf(currentTags.toSet()) }
    var input by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Filter existing tags by search input
    val filteredExisting = remember(input, allExistingTags, selectedTags) {
        allExistingTags
            .filter { tag ->
                input.isBlank() || tag.contains(input.trim(), ignoreCase = true)
            }
            .sortedBy { it }
    }

    // Can add new tag if input is not blank and not already in existing
    val canAddNew = input.trim().isNotBlank() &&
            !allExistingTags.contains(input.trim().lowercase().replace(" ", "-")) &&
            !selectedTags.contains(input.trim().lowercase().replace(" ", "-"))

    fun addTag(tag: String) {
        val clean = tag.trim().lowercase().replace(" ", "-")
        if (clean.isNotBlank()) {
            selectedTags = selectedTags + clean
            input = ""
        }
    }

    fun toggleTag(tag: String) {
        selectedTags = if (selectedTags.contains(tag)) {
            selectedTags - tag
        } else {
            selectedTags + tag
        }
    }

    fun dismiss() {
        scope.launch { sheetState.hide(); onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Handle
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    Modifier
                        .width(40.dp)
                        .height(4.dp),
                    CircleShape,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                ) {}
            }

            Text(
                "Manage Tags",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // Search / add input
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.lowercase().replace(" ", "-") },
                placeholder = { Text("Search or add tag…") },
                leadingIcon = {
                    Icon(
                        if (input.isBlank()) Icons.Outlined.Tag else Icons.Outlined.Search,
                        null
                    )
                },
                trailingIcon = {
                    if (canAddNew) {
                        IconButton(onClick = { addTag(input) }) {
                            Icon(
                                Icons.Filled.Add, null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (input.isNotBlank()) {
                        IconButton(onClick = { input = "" }) {
                            Icon(Icons.Filled.Clear, null)
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (canAddNew) addTag(input)
                    else if (filteredExisting.size == 1) toggleTag(filteredExisting.first())
                }),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Tags section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // Show "currently selected" section at top if any selected
                if (selectedTags.isNotEmpty()) {
                    Text(
                        "Selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedTags.sorted().forEach { tag ->
                            // GREEN selected chip
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF22C55E).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFF22C55E)),
                                modifier = Modifier.clickable { toggleTag(tag) }
                            ) {
                                Text(
                                    "#$tag",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF22C55E),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider()
                }

                // All / filtered existing tags
                val unselectedFiltered = filteredExisting.filter { !selectedTags.contains(it) }

                if (unselectedFiltered.isNotEmpty() || canAddNew) {
                    Text(
                        if (input.isBlank()) "All tags" else "Results",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "Add new" chip at top of results if input is new
                    if (canAddNew) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable { addTag(input) }
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 10.dp, vertical = 6.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Add, null,
                                    Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Create \"#$input\"",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Unselected existing tags — grey, tap to select
                    unselectedFiltered.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { toggleTag(tag) }
                        ) {
                            Text(
                                "#$tag",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    horizontal = 10.dp, vertical = 6.dp
                                )
                            )
                        }
                    }

                    if (unselectedFiltered.isEmpty() && !canAddNew && input.isNotBlank()) {
                        Text(
                            "No tags found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (allExistingTags.isEmpty() && input.isBlank()) {
                        Text(
                            "No tags yet — type above to create one",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f, fill = false))

            Button(
                onClick = { onSave(selectedTags.toList()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Save",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatTimeLeft(timestamp: Long): String {
    val diff = timestamp - System.currentTimeMillis()
    return when {
        diff < 0 -> "expired"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        diff < 604_800_000 -> "${diff / 86_400_000}d"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

fun domainColor(domain: String): Color {
    return when {
        // Social
        domain.contains("twitter") || domain.contains("x.com") -> Color(0xFF1DA1F2)
        domain.contains("instagram") -> Color(0xFFE1306C)
        domain.contains("facebook") -> Color(0xFF1877F2)
        domain.contains("reddit") -> Color(0xFFFF4500)
        domain.contains("linkedin") -> Color(0xFF0A66C2)
        domain.contains("tiktok") -> Color(0xFF010101)

        // Video
        domain.contains("youtube") -> Color(0xFFFF0000)
        domain.contains("twitch") -> Color(0xFF9146FF)
        domain.contains("vimeo") -> Color(0xFF1AB7EA)

        // Dev
        domain.contains("github") -> Color(0xFF6E40C9)
        domain.contains("stackoverflow") -> Color(0xFFF48024)
        domain.contains("medium") -> Color(0xFF00AB6C)
        domain.contains("dev.to") -> Color(0xFF0A0A0A)
        domain.contains("producthunt") -> Color(0xFFDA552F)

        // News
        domain.contains("bbc") -> Color(0xFFBB1919)
        domain.contains("cnn") -> Color(0xFFCC0000)
        domain.contains("nytimes") -> Color(0xFF121212)
        domain.contains("theverge") -> Color(0xFFE21218)

        // Shopping
        domain.contains("amazon") -> Color(0xFFFF9900)
        domain.contains("ebay") -> Color(0xFF0064D2)

        // Music
        domain.contains("spotify") -> Color(0xFF1DB954)
        domain.contains("soundcloud") -> Color(0xFFFF5500)

        // Productivity
        domain.contains("notion") -> Color(0xFF000000)
        domain.contains("figma") -> Color(0xFFF24E1E)
        domain.contains("linear") -> Color(0xFF5E6AD2)

        // Fallback — hash domain for consistent unique color
        else -> {
            val hue = (domain.hashCode().and(0xFFFFFF).toFloat() / 0xFFFFFF) * 360f
            Color.hsv(hue, 0.6f, 0.8f)
        }
    }
}