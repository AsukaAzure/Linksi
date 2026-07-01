package com.linksi.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import com.linksi.app.R
import com.linksi.app.domain.model.*
import com.linksi.app.ui.components.iconFromName
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiOrganizerScreen(
    onBack: () -> Unit,
    viewModel: AiOrganizerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onScreenOpened()
    }

    AnimatedContent(
        targetState = state.step,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
            } else {
                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
            }
        },
        label = "ai_organizer_step_transition"
    ) { step ->
        when (step) {
            AiOrganizerStep.IDLE -> AiOrganizerIdle(
                state = state,
                onBack = onBack,
                onStartOrganize = viewModel::startOrganize,
                onRevert = viewModel::revertLastSession,
                onSetBatchSize = viewModel::setBatchSize,
                onTestModel = viewModel::testModel
            )

            AiOrganizerStep.SELECT_SCOPE -> AiScopeSelector(
                selectedScope = state.selectedScope,
                onScopeSelect = viewModel::setScope,
                onConfirm = viewModel::generatePlan,
                onCancel = viewModel::cancelOrganize
            )

            AiOrganizerStep.GENERATING -> AiGeneratingScreen(onCancel = viewModel::cancelOrganize)
            AiOrganizerStep.PREVIEW -> state.plan?.let { plan ->
                AiPreviewScreen(
                    plan = plan,
                    existingFolders = state.folders,
                    onApply = viewModel::applyPlan,
                    onCancel = viewModel::cancelOrganize
                )
            }

            AiOrganizerStep.APPLYING -> AiApplyingScreen(
                progress = state.applyProgress,
                total = state.applyTotal
            )

            AiOrganizerStep.DONE -> AiDoneScreen(
                onBack = onBack,
                onRevert = viewModel::revertLastSession,
                onOrganizeAgain = viewModel::resetToIdle
            )

            AiOrganizerStep.ERROR -> AiErrorScreen(
                message = state.errorMessage ?: "Unknown error",
                onRetry = viewModel::generatePlan,
                onCancel = viewModel::cancelOrganize
            )
        }
    }
}

// ── Idle / Home ───────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiOrganizerIdle(
    state: AiOrganizerUiState,
    onBack: () -> Unit,
    onStartOrganize: () -> Unit,
    onRevert: () -> Unit,
    onSetBatchSize: (Int) -> Unit,
    onTestModel: () -> Unit
) {
    val selectedModel = AI_MODELS.find { it.id == state.selectedModelId }
    val apiKey = state.apiKeys[selectedModel?.provider] ?: ""
    val isReady = apiKey.isNotBlank()

    val modelStatus = state.modelStatus
    val isTestingModel = state.isTestingModel

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show snackbar when test fails
    LaunchedEffect(state.testErrorMessage) {
        state.testErrorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_organizer)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, stringResource(R.string.back))
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero section
            AiHeroArt(Modifier.size(100.dp))

            Text(
                stringResource(R.string.ai_link_organizer),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                stringResource(R.string.ai_hero_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Current model info
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.SmartToy, null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.model),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            selectedModel?.name ?: stringResource(R.string.none_selected),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            !isReady -> MaterialTheme.colorScheme.errorContainer
                            modelStatus == SettingsViewModel.ModelStatus.ACTIVE -> Color(0xFF22C55E).copy(alpha = 0.2f)
                            modelStatus == SettingsViewModel.ModelStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        modifier = Modifier.clickable {
                            if (isReady && !isTestingModel) onTestModel()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isTestingModel) {
                                CircularProgressIndicator(
                                    Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(stringResource(R.string.testing),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            } else {
                                when {
                                    !isReady -> Text(stringResource(R.string.no_api_key),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer)
                                    modelStatus == SettingsViewModel.ModelStatus.ACTIVE -> {
                                        Icon(Icons.Outlined.CheckCircle, null,
                                            Modifier.size(12.dp),
                                            tint = Color(0xFF22C55E))
                                        Text(stringResource(R.string.active),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF22C55E))
                                    }
                                    modelStatus == SettingsViewModel.ModelStatus.ERROR -> {
                                        Icon(Icons.Outlined.Error, null,
                                            Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.error)
                                        Text(stringResource(R.string.failed),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error)
                                    }
                                    else -> Text(stringResource(R.string.check),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Layers, null,
                            tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.links_per_batch),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium)
                            Text(
                                stringResource(R.string.batch_size_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(4, 8, 16, 32).forEach { size ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (state.batchSize == size)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSetBatchSize(size) }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        "$size",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.batchSize == size)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Last session info
            if (state.hasRevertableSession) {
                state.lastSession?.let { session ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Outlined.History, null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.last_organized),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    stringResource(
                                        R.string.last_organized_stats,
                                        session.movedLinks.size,
                                        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                            .format(Date(session.timestamp))
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            TextButton(onClick = onRevert) {
                                Text(stringResource(R.string.revert))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Organize button
            Button(
                onClick = onStartOrganize,
                enabled = isReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.organize_with_ai),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (!isReady) {
                Text(
                    stringResource(R.string.ai_settings_hint),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Scope Selector ────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScopeSelector(
    selectedScope: String,
    onScopeSelect: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Outlined.FilterList, null) },
        title = { Text(stringResource(R.string.what_to_organize)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ScopeOption(
                    title = stringResource(R.string.unorganized_links_only),
                    description = stringResource(R.string.unorganized_links_desc),
                    icon = Icons.Outlined.FolderOff,
                    isSelected = selectedScope == OrganizeScope.UNORGANIZED,
                    onClick = { onScopeSelect(OrganizeScope.UNORGANIZED) }
                )
                ScopeOption(
                    title = stringResource(R.string.all_links_option),
                    description = stringResource(R.string.all_links_desc),
                    icon = Icons.Outlined.SelectAll,
                    isSelected = selectedScope == OrganizeScope.ALL,
                    onClick = { onScopeSelect(OrganizeScope.ALL) }
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.generate_plan))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun ScopeOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon, null,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Outlined.CheckCircle, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Generating ────────────────────────────────────────────────
@Composable
fun AiGeneratingScreen(onCancel: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            AiHeroArt(Modifier.size(100.dp))

            Text(
                stringResource(R.string.ai_analyzing),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                stringResource(R.string.ai_analyzing_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPreviewScreen(
    plan: OrganizePlan,
    existingFolders: List<Folder>,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    // Group by target folder
    val grouped = plan.linkPlans.groupBy { it.targetFolderName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.review_ai_plan)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.cancel))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SummaryChip(
                            label = "${plan.linkPlans.size} " + stringResource(R.string.total_links).lowercase(),
                            icon = Icons.Outlined.Link
                        )
                        SummaryChip(
                            label = "${grouped.size} " + stringResource(R.string.folders_title).lowercase(),
                            icon = Icons.Outlined.Folder
                        )
                        if (plan.newFolders.isNotEmpty()) {
                            SummaryChip(
                                label = "${plan.newFolders.size} " + stringResource(R.string.new_folder).lowercase(),
                                icon = Icons.Outlined.CreateNewFolder,
                                highlight = true
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.cancel)) }

                        Button(
                            onClick = onApply,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Check, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.apply_plan))
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // New folders notice
            if (plan.newFolders.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.CreateNewFolder, null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Column {
                                Text(
                                    stringResource(R.string.new_folders_created),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    plan.newFolders.joinToString(" · ") { it.name },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Grouped by folder
            grouped.forEach { (folderName, linkPlans) ->
                val isNew = linkPlans.first().isNewFolder
                val (icon, color) = if (isNew) {
                    val newFolder = plan.newFolders.find { it.name == folderName }
                    (newFolder?.icon ?: "folder") to (newFolder?.color ?: "#6366F1")
                } else {
                    val folder = existingFolders.find { it.name == folderName }
                    (folder?.icon ?: "folder") to (folder?.color ?: "#6750A4")
                }

                item {
                    FolderPreviewGroup(
                        folderName = folderName,
                        icon = icon,
                        color = color,
                        isNew = isNew,
                        linkPlans = linkPlans
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
fun FolderPreviewGroup(
    folderName: String,
    icon: String,
    color: String,
    isNew: Boolean,
    linkPlans: List<LinkOrganizePlan>
) {
    var expanded by remember { mutableStateOf(true) }

    Column {
        // Folder header
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(android.graphics.Color.parseColor(color)).copy(alpha = 0.15f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            iconFromName(icon), null,
                            tint = Color(android.graphics.Color.parseColor(color)),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    folderName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (isNew) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            stringResource(R.string.all).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    "${linkPlans.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess
                    else Icons.Outlined.ExpandMore,
                    null,
                    Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Links in this folder
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                linkPlans.forEach { lp ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Link, null,
                                Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    lp.link.title.ifBlank { lp.link.url },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (lp.currentFolderName != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            stringResource(R.string.from_folder, lp.currentFolderName),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            Icons.Outlined.ArrowForward, null,
                                            Modifier.size(10.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            folderName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    highlight: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (highlight)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon, null, Modifier.size(14.dp),
                tint = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Applying ──────────────────────────────────────────────────
@Composable
fun AiApplyingScreen(progress: Int, total: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                progress = { if (total > 0) progress.toFloat() / total else 0f },
                modifier = Modifier.size(80.dp),
                strokeWidth = 6.dp
            )
            Text(
                stringResource(R.string.organizing),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.organizing_progress, progress, total),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Done ──────────────────────────────────────────────────────
@Composable
fun AnimatedCheckmark(modifier: Modifier = Modifier) {
    val checkProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        checkProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val containerColor = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = modifier
            .size(100.dp)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(48.dp)) {
            val width = size.width
            val height = size.height

            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(width * 0.2f, height * 0.5f)
                lineTo(width * 0.45f, height * 0.75f)
                lineTo(width * 0.8f, height * 0.3f)
            }

            val pathMeasure = androidx.compose.ui.graphics.PathMeasure()
            pathMeasure.setPath(path, false)
            val segmentPath = androidx.compose.ui.graphics.Path()
            pathMeasure.getSegment(0f, checkProgress.value * pathMeasure.length, segmentPath)

            drawPath(
                path = segmentPath,
                color = primaryColor,
                style = Stroke(
                    width = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
fun AiDoneScreen(onBack: () -> Unit, onRevert: () -> Unit, onOrganizeAgain: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            AnimatedCheckmark()
            Text(
                stringResource(R.string.links_organized),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.links_organized_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRevert,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Undo, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.undo_organization))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerSheet(
    currentModelId: String,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedProvider by remember {
        mutableStateOf(
            AI_MODELS.find { it.id == currentModelId }?.provider ?: AiProvider.ANTHROPIC
        )
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()

    fun dismissWithAnimation() {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Title
            Text(
                stringResource(R.string.select_ai_model),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            HorizontalDivider()

            // Provider tabs
            val providers = AiProvider.values().toList()
            val providerNames = mapOf(
                AiProvider.OPENAI    to "OpenAI",
                AiProvider.ANTHROPIC to "Anthropic",
                AiProvider.GEMINI    to "Gemini",
                AiProvider.DEEPSEEK  to "DeepSeek",
                AiProvider.GROK      to "Grok"
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(providers) { provider ->
                    FilterChip(
                        selected = selectedProvider == provider,
                        onClick = { selectedProvider = provider },
                        label = { Text(providerNames[provider] ?: provider.name) }
                    )
                }
            }

            HorizontalDivider()

            // Models for selected provider
            val providerModels = AI_MODELS.filter { it.provider == selectedProvider }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    stringResource(R.string.models),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                AnimatedContent(
                    targetState = selectedProvider,
                    transitionSpec = {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    },
                    label = "provider_models"
                ) { provider ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AI_MODELS.filter { it.provider == provider }.forEach { model ->
                            val isSelected = model.id == currentModelId
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onModelSelected(model.id)
                                        dismissWithAnimation()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 14.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            model.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected)
                                                FontWeight.SemiBold
                                            else
                                                FontWeight.Normal,
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            model.modelId,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Outlined.CheckCircle, null,
                                            Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedCross(modifier: Modifier = Modifier) {
    val crossProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        crossProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    val errorColor = MaterialTheme.colorScheme.error
    val containerColor = MaterialTheme.colorScheme.errorContainer

    Box(
        modifier = modifier
            .size(100.dp)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(48.dp)) {
            val width = size.width
            val height = size.height
            val strokeWidth = 6.dp.toPx()

            // Line 1: \
            val path1 = androidx.compose.ui.graphics.Path().apply {
                moveTo(width * 0.3f, height * 0.3f)
                lineTo(width * 0.7f, height * 0.7f)
            }

            // Line 2: /
            val path2 = androidx.compose.ui.graphics.Path().apply {
                moveTo(width * 0.7f, height * 0.3f)
                lineTo(width * 0.3f, height * 0.7f)
            }

            val pathMeasure = androidx.compose.ui.graphics.PathMeasure()

            // Draw first line
            pathMeasure.setPath(path1, false)
            val segmentPath1 = androidx.compose.ui.graphics.Path()
            val progress1 = (crossProgress.value * 2f).coerceAtMost(1f)
            pathMeasure.getSegment(0f, progress1 * pathMeasure.length, segmentPath1)
            drawPath(
                path = segmentPath1,
                color = errorColor,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )

            // Draw second line
            if (crossProgress.value > 0.5f) {
                pathMeasure.setPath(path2, false)
                val segmentPath2 = androidx.compose.ui.graphics.Path()
                val progress2 = (crossProgress.value - 0.5f) * 2f
                pathMeasure.getSegment(0f, progress2 * pathMeasure.length, segmentPath2)
                drawPath(
                    path = segmentPath2,
                    color = errorColor,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}

// ── Error ─────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.something_went_wrong)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                AnimatedCross()
                Text(
                    stringResource(R.string.something_went_wrong),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.check_for_updates)) // Using an existing retry-like string
                }
                
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.go_back))
                }
            }
        }
    }
}

@Composable
fun AiHeroArt(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_art")
    val duration = 1350

    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    // Patterns based on slot index (0 to 8) derived from the SVG's keyframes
    val patterns = listOf(
        listOf(1, 1, 1, 0, 0, 0, 0, 0, 0), // d=0: f111000000
        listOf(0, 1, 0, 1, 0, 0, 0, 0, 0), // d=1: f010100000
        listOf(0, 0, 1, 0, 1, 0, 0, 0, 0), // d=2: f001010000
        listOf(0, 0, 0, 1, 0, 1, 0, 0, 0), // d=3: f000101000
        listOf(0, 0, 0, 1, 1, 0, 1, 0, 0), // d=4: f000110100
        listOf(0, 0, 0, 0, 1, 1, 0, 1, 0), // d=5: f000011010
        listOf(0, 0, 0, 0, 0, 1, 1, 1, 0)  // d=6: f000001110
    )

    val onColor = MaterialTheme.colorScheme.primary
    val offColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        val dotRadius = size.width / 42f * 2f
        val step = size.width / 42f * 6f
        val startOffset = size.width / 42f * 3f

        val currentSlot = (time * 9).toInt().coerceIn(0, 8)

        for (row in 0 until 7) {
            for (col in 0 until 7) {
                // Manhattan distance from center (3,3)
                val d = abs(row - 3) + abs(col - 3)
                val isOn = if (d < patterns.size) patterns[d][currentSlot] == 1 else false

                drawCircle(
                    color = if (isOn) onColor else offColor,
                    radius = dotRadius,
                    center = Offset(startOffset + col * step, startOffset + row * step)
                )
            }
        }
    }
}
