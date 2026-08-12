package com.linksi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linksi.app.R
import com.linksi.app.domain.model.Link
import com.linksi.app.ui.components.LinkCard
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashBinScreen(
    onBack: () -> Unit,
    viewModel: TrashBinViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEmptyConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trash_bin)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (state.links.isNotEmpty()) {
                        IconButton(onClick = { showEmptyConfirm = true }) {
                            Icon(Icons.Outlined.DeleteSweep, stringResource(R.string.empty_bin))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Trash Bin Toggle and Info
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.trash_bin),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                stringResource(R.string.bin_auto_delete_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.isEnabled,
                            onCheckedChange = viewModel::setEnabled
                        )
                    }
                }
            }

            if (state.links.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "🗑️", fontSize = 64.sp)
                        Text(
                            text = stringResource(R.string.no_links_in_bin),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(state.links, key = { it.id }) { link ->
                        TrashLinkCard(
                            link = link,
                            onRestore = { viewModel.restoreLink(link) },
                            onDelete = { viewModel.permanentlyDeleteLink(link) }
                        )
                    }
                }
            }
        }
    }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { Text(stringResource(R.string.empty_bin)) },
            text = { Text(stringResource(R.string.empty_bin_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.emptyBin()
                        showEmptyConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun TrashLinkCard(
    link: Link,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Favicon or Domain icon placeholder
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = link.domain.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = link.title.ifBlank { link.url },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = link.domain,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onRestore) {
                    Icon(Icons.Outlined.Restore, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.restore))
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Outlined.DeleteForever, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }
}
