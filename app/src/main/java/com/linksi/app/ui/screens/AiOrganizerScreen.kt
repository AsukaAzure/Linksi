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
import androidx.compose.foundation.text.selection.SelectionContainer
import com.linksi.app.R
import com.linksi.app.domain.model.*
import com.linksi.app.ui.components.ExpressiveSettingsCard
import com.linksi.app.ui.components.IconContainer
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
    val selectedModel = state.availableModels.find { it.id == state.selectedModelId }
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
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
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
                        AiHeroArt(Modifier.size(64.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        stringResource(R.string.ai_link_organizer),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        stringResource(R.string.ai_hero_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }

            // Current model info
            item {
                ExpressiveSettingsCard {
                    ListItem(
                        headlineContent = { 
                            Text(
                                stringResource(R.string.model),
                                fontWeight = FontWeight.SemiBold
                            ) 
                        },
                        supportingContent = {
                            Text(
                                selectedModel?.name ?: stringResource(R.string.none_selected),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingContent = {
                            IconContainer(Icons.Outlined.SmartToy)
                        },
                        trailingContent = {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
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
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconContainer(Icons.Outlined.Layers)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.links_per_batch),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Text(
                                    stringResource(R.string.batch_size_desc),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        val batchOptions = listOf(4, 8, 16, 32)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            batchOptions.forEachIndexed { index, size ->
                                val isSelected = state.batchSize == size
                                
                                // Animate shapes for "Expressive" feel
                                val cornerPercent by animateIntAsState(
                                    targetValue = if (isSelected) 50 else 25,
                                    animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
                                    label = "corner_anim"
                                )

                                val shape = remember(index, isSelected, cornerPercent) {
                                    val topStart = if (index == 0 || isSelected) cornerPercent else 15
                                    val bottomStart = if (index == 0 || isSelected) cornerPercent else 15
                                    val topEnd = if (index == batchOptions.size - 1 || isSelected) cornerPercent else 15
                                    val bottomEnd = if (index == batchOptions.size - 1 || isSelected) cornerPercent else 15
                                    
                                    RoundedCornerShape(
                                        topStartPercent = topStart,
                                        bottomStartPercent = bottomStart,
                                        topEndPercent = topEnd,
                                        bottomEndPercent = bottomEnd
                                    )
                                }

                                Surface(
                                    onClick = { onSetBatchSize(size) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = shape,
                                    color = if (isSelected) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    contentColor = if (isSelected) 
                                        MaterialTheme.colorScheme.onPrimary 
                                    else 
                                        MaterialTheme.colorScheme.onSurface
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = size.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Last session info
            if (state.hasRevertableSession) {
                state.lastSession?.let { session ->
                    item {
                        ExpressiveSettingsCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ListItem(
                                headlineContent = { 
                                    Text(
                                        stringResource(R.string.last_organized),
                                        fontWeight = FontWeight.SemiBold
                                    ) 
                                },
                                supportingContent = {
                                    Text(
                                        stringResource(
                                            R.string.last_organized_stats,
                                            session.movedLinks.size,
                                            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                                .format(Date(session.timestamp))
                                        ),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                leadingContent = {
                                    IconContainer(Icons.Outlined.History, color = MaterialTheme.colorScheme.secondary)
                                },
                                trailingContent = {
                                    TextButton(onClick = onRevert) {
                                        Text(stringResource(R.string.revert))
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Organize button
            item {
                Button(
                    onClick = onStartOrganize,
                    enabled = isReady,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.organize_with_ai),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!isReady) {
                item {
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
        icon = { 
            IconContainer(Icons.Outlined.FilterList, color = MaterialTheme.colorScheme.primary)
        },
        title = { 
            Text(
                stringResource(R.string.what_to_organize),
                fontWeight = FontWeight.Bold
            ) 
        },
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
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.generate_plan))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        },
        shape = RoundedCornerShape(28.dp)
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
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(16.dp)
                ) else Modifier
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconContainer(
                icon = icon,
                enabled = isSelected,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
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
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ── Generating ────────────────────────────────────────────────
@Composable
fun AiGeneratingScreen(onCancel: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(40.dp))
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
                AiHeroArt(Modifier.size(80.dp))
            }

            Text(
                stringResource(R.string.ai_analyzing),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
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
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                title = { Text(stringResource(R.string.review_ai_plan), fontWeight = FontWeight.Bold) },
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
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryChip(
                            label = "${plan.linkPlans.size} " + stringResource(R.string.total_links).lowercase(),
                            icon = Icons.Outlined.Link,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryChip(
                            label = "${grouped.size} " + stringResource(R.string.folders_title).lowercase(),
                            icon = Icons.Outlined.Folder,
                            modifier = Modifier.weight(1f)
                        )
                        if (plan.newFolders.isNotEmpty()) {
                            SummaryChip(
                                label = "${plan.newFolders.size} " + stringResource(R.string.new_folder).lowercase(),
                                icon = Icons.Outlined.CreateNewFolder,
                                highlight = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text(stringResource(R.string.cancel)) }

                        Button(
                            onClick = onApply,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Outlined.Check, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.apply_plan), fontWeight = FontWeight.Bold)
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
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconContainer(Icons.Outlined.CreateNewFolder)
                            Column {
                                Text(
                                    stringResource(R.string.new_folders_created),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
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

            item { Spacer(Modifier.height(80.dp)) }
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
    val folderColor = try { Color(android.graphics.Color.parseColor(color)) } catch(e:Exception) { MaterialTheme.colorScheme.primary }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        // Folder header
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconContainer(
                    icon = iconFromName(icon),
                    color = folderColor
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        folderName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (isNew) {
                        Text(
                            stringResource(R.string.new_folder),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ) {
                    Text(
                        "${linkPlans.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess
                    else Icons.Outlined.ExpandMore,
                    null,
                    Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Links in this folder
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                linkPlans.forEach { lp ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Link, null,
                                Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    lp.link.title.ifBlank { lp.link.url },
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (lp.currentFolderName != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            lp.currentFolderName,
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
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
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
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (highlight)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon, null, Modifier.size(18.dp),
                tint = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { if (total > 0) progress.toFloat() / total else 0f },
                    modifier = Modifier.size(120.dp),
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
                Text(
                    "${if(total > 0) (progress.toFloat()/total * 100).toInt() else 0}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Text(
                stringResource(R.string.organizing),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                stringResource(R.string.organizing_progress, progress, total),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
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
            .size(120.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(60.dp)) {
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
                    width = 8.dp.toPx(),
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            AnimatedCheckmark()
            Text(
                stringResource(R.string.links_organized),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.links_organized_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.done), fontWeight = FontWeight.Bold)
            }
            
            OutlinedButton(
                onClick = onRevert,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.Undo, null, Modifier.size(20.dp))
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
    models: List<AiModel>,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedProvider by remember {
        mutableStateOf(
            models.find { it.id == currentModelId }?.provider ?: AiProvider.ANTHROPIC
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
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
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
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

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
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(providers) { provider ->
                    FilterChip(
                        selected = selectedProvider == provider,
                        onClick = { selectedProvider = provider },
                        label = { Text(providerNames[provider] ?: provider.name) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Models for selected provider
            val providerModels = models.filter { it.provider == selectedProvider }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.models),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                AnimatedContent(
                    targetState = selectedProvider,
                    transitionSpec = {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    },
                    label = "provider_models"
                ) { provider ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        models.filter { it.provider == provider }.forEach { model ->
                            val isSelected = model.id == currentModelId
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onModelSelected(model.id)
                                        dismissWithAnimation()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    IconContainer(
                                        icon = Icons.Outlined.SmartToy,
                                        enabled = isSelected
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            model.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected)
                                                FontWeight.Bold
                                            else
                                                FontWeight.Medium,
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
                                            Modifier.size(24.dp),
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
            .size(120.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(60.dp)) {
            val width = size.width
            val height = size.height
            val strokeWidth = 8.dp.toPx()

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
                title = { },
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
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                AnimatedCross()
                Text(
                    stringResource(R.string.something_went_wrong),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SelectionContainer {
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(20.dp),
                            textAlign = TextAlign.Start
                        )
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Outlined.Refresh, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.check_for_updates), fontWeight = FontWeight.Bold)
                }
                
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
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
