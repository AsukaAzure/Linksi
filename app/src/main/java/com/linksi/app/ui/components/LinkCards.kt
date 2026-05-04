package com.linksi.app.ui.components

import android.R
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import coil.compose.AsyncImage
import com.linksi.app.domain.model.Folder
import com.linksi.app.domain.model.Link
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
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val cardColor = when {
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        !link.isRead -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    val density = LocalDensity.current
    // AFTER
    var swipeConfirmed by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.6f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    if (swipeConfirmed) {
                        swipeConfirmed = false
                        onDelete()
                        true
                    } else {
                        false
                    }
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (swipeConfirmed) {
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

// Only confirm when progress crosses 0.6f threshold
    LaunchedEffect(dismissState.progress) {
        if (dismissState.progress >= 0.6f) {
            swipeConfirmed = true
        } else {
            swipeConfirmed = false
        }
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
                            // Show lock icon until threshold reached, then delete icon
                            if (swipeConfirmed) Icons.Outlined.Delete else Icons.Outlined.Lock,
                            null,
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
                            null,
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
//                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                      onClick = onClick,
                        onLongClick = onLongPress
                    ),
                colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (!link.isRead) 2.dp else 0.dp)
            ) {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    Row {
                        AnimatedVisibility(visible = isSelectionMode) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onLongPress() },
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .align(Alignment.CenterVertically)
                            )
                        }

                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .fillMaxHeight()
                            .background(domainColor(link.domain))
                    )

                    Column(modifier = Modifier.padding(12.dp)) {
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
                            }

                            // Menu button
                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.MoreVert, "More", Modifier.size(18.dp))
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                                        onClick = { showMenu = false; onEdit() }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Move to folder") },
                                        leadingIcon = { Icon(Icons.Outlined.FolderOpen, null) },
                                        onClick = { showMenu = false; showFolderPicker = true }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Delete",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Outlined.Delete,
                                                null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = { showMenu = false; onDelete() }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Share") },
                                        leadingIcon = { Icon(Icons.Outlined.Share, null) },
                                        onClick = {
                                            showMenu = false
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "${link.title}\n ${link.url}"
                                                )
                                                type = "text/plain"
                                            }
                                            context.startActivity(
                                                Intent.createChooser(
                                                    sendIntent,
                                                    "Share Link via"
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                            }
                        }

                        // Bottom row: date + tags + favorite
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Folder badge
                            val folder = folders.find { it.id == link.folderId }
                            if (folder != null) {
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        // AFTER
                                        Row(verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(iconFromName(folder.icon), null, Modifier.size(14.dp),
                                                tint = Color(android.graphics.Color.parseColor(folder.color)))
                                            Text(
                                                "${folder.name}",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
//                                        Text(
//                                            "${folder.emoji} ${folder.name}",
//                                            style = MaterialTheme.typography.labelSmall
//                                        )
                                    },
                                    modifier = Modifier.height(24.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            }

                            Spacer(Modifier.weight(1f))

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
                                    "Favorite",
                                    Modifier.size(18.dp),
                                    tint = if (link.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
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



@Composable
fun LinkGridCard(
    link: Link,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Preview image if available
            if (link.previewImageUrl.isNotBlank()) {
                AsyncImage(
                    model = link.previewImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Gradient placeholder with favicon
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
                        modifier = Modifier.size(32.dp).clip(CircleShape)
                    )
                }
            }

            Column(Modifier.padding(10.dp)) {
                Text(
                    text = link.domain.ifBlank { "link" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
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
                    IconButton(onClick = onFavoriteToggle, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (link.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            null,
                            Modifier.size(16.dp),
                            tint = if (link.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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

fun domainColor(domain: String): Color {
    return when {
        // Social
        domain.contains("twitter") || domain.contains("x.com") -> Color(0xFF1DA1F2)
        domain.contains("instagram")                            -> Color(0xFFE1306C)
        domain.contains("facebook")                            -> Color(0xFF1877F2)
        domain.contains("reddit")                              -> Color(0xFFFF4500)
        domain.contains("linkedin")                            -> Color(0xFF0A66C2)
        domain.contains("tiktok")                              -> Color(0xFF010101)

        // Video
        domain.contains("youtube")                             -> Color(0xFFFF0000)
        domain.contains("twitch")                              -> Color(0xFF9146FF)
        domain.contains("vimeo")                               -> Color(0xFF1AB7EA)

        // Dev
        domain.contains("github")                              -> Color(0xFF6E40C9)
        domain.contains("stackoverflow")                       -> Color(0xFFF48024)
        domain.contains("medium")                              -> Color(0xFF00AB6C)
        domain.contains("dev.to")                              -> Color(0xFF0A0A0A)
        domain.contains("producthunt")                         -> Color(0xFFDA552F)

        // News
        domain.contains("bbc")                                 -> Color(0xFFBB1919)
        domain.contains("cnn")                                 -> Color(0xFFCC0000)
        domain.contains("nytimes")                             -> Color(0xFF121212)
        domain.contains("theverge")                            -> Color(0xFFE21218)

        // Shopping
        domain.contains("amazon")                              -> Color(0xFFFF9900)
        domain.contains("ebay")                                -> Color(0xFF0064D2)

        // Music
        domain.contains("spotify")                             -> Color(0xFF1DB954)
        domain.contains("soundcloud")                          -> Color(0xFFFF5500)

        // Productivity
        domain.contains("notion")                              -> Color(0xFF000000)
        domain.contains("figma")                               -> Color(0xFFF24E1E)
        domain.contains("linear")                              -> Color(0xFF5E6AD2)

        // Fallback — hash the domain name for a consistent unique color
        else -> {
            val hue = (domain.hashCode().and(0xFFFFFF).toFloat() / 0xFFFFFF) * 360f
            Color.hsv(hue, 0.6f, 0.8f)
        }
    }
}