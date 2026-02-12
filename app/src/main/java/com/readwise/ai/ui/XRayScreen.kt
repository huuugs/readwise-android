package com.readwise.ai.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.readwise.ai.model.XRayEntityType
import com.readwise.ai.model.XRayEntity
import com.readwise.ai.viewmodel.XRayViewModel

/**
 * X-ray Analysis Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XRayScreen(
    viewModel: XRayViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val entities by viewModel.entities.collectAsState()
    val selectedEntity by viewModel.selectedEntity.collectAsState()
    val entityCounts by viewModel.getEntityCountByType().collectAsState(emptyMap())

    var showFilterMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("X-ray") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.hasGenerated) {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                LoadingView(modifier = Modifier.padding(padding))
            }

            !uiState.hasGenerated -> {
                EmptyView(
                    onGenerate = { /* TODO: trigger generation */ },
                    modifier = Modifier.padding(padding)
                )
            }

            uiState.isGenerating -> {
                GeneratingView(
                    progress = uiState.progress,
                    total = uiState.progressTotal,
                    modifier = Modifier.padding(padding)
                )
            }

            else -> {
                MainContentView(
                    entities = entities,
                    entityCounts = entityCounts,
                    selectedFilter = uiState.selectedFilter,
                    searchQuery = uiState.searchQuery,
                    onFilterClick = { showFilterMenu = true },
                    onSearch = { viewModel.searchEntities(it) },
                    onEntityClick = { viewModel.selectEntity(it) },
                    modifier = Modifier.padding(padding)
                )
            }
        }

        // Filter Menu
        if (showFilterMenu) {
            FilterMenu(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = {
                    viewModel.filterByType(it)
                    showFilterMenu = false
                },
                onDismiss = { showFilterMenu = false }
            )
        }

        // Entity Detail Sheet
        if (selectedEntity != null) {
            EntityDetailBottomSheet(
                entity = selectedEntity!!,
                relatedEntities = viewModel.getRelatedEntities(selectedEntity!!),
                chapters = viewModel.getEntityChapters(selectedEntity!!),
                onDismiss = { viewModel.closeEntityDetail() },
                onRelatedClick = { viewModel.selectEntity(it) }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            DeleteConfirmDialog(
                onConfirm = {
                    viewModel.deleteXRayData()
                    showDeleteDialog = false
                },
                onDismiss = { showDeleteDialog = false }
            )
        }

        // Error Snackbar
        if (uiState.error != null) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.retryGeneration() }) {
                        Text("Retry")
                    }
                }
            ) {
                Text(uiState.error ?: "Unknown error")
            }
        }
    }
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyView(
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PeopleOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Explore Characters & Locations",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Generate X-ray analysis to discover characters, locations, and important terms throughout the book",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        FilledTonalButton(onClick = onGenerate) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate X-ray")
        }
    }
}

@Composable
fun GeneratingView(
    progress: Int?,
    total: Int?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Analyzing content...",
            style = MaterialTheme.typography.titleMedium
        )
        if (progress != null && total != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Chapter $progress of $total",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MainContentView(
    entities: List<XRayEntity>,
    entityCounts: Map<XRayEntityType, Int>,
    selectedFilter: XRayEntityType?,
    searchQuery: String,
    onFilterClick: () -> Unit,
    onSearch: (String) -> Unit,
    onEntityClick: (XRayEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Type Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // All filter
            item {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { onFilterClick() },
                    label = { Text("All (${entities.size})") },
                    leadingIcon = if (selectedFilter == null) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }

            // Type filters
            XRayEntityType.values().forEach { type ->
                val count = entityCounts[type] ?: 0
                item {
                    FilterChip(
                        selected = selectedFilter == type,
                        onClick = { onFilterClick() },
                        label = { Text("${type.displayName} ($count)") }
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearch,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search entities...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            singleLine = true
        )

        // Entity List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entities) { entity ->
                EntityListItem(
                    entity = entity,
                    onClick = { onEntityClick(entity) }
                )
            }
        }
    }
}

@Composable
fun EntityListItem(
    entity: XRayEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Entity icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(entity.type.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = entity.type.icon,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Entity info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entity.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entity.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${entity.mentions.size} mentions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (entity.relatedEntities.isNotEmpty()) {
                        Text(
                            text = "${entity.relatedEntities.size} related",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FilterMenu(
    selectedFilter: XRayEntityType?,
    onFilterSelected: (XRayEntityType?) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("All Types") },
            onClick = { onFilterSelected(null) },
            leadingIcon = if (selectedFilter == null) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else null
        )

        Divider()

        XRayEntityType.values().forEach { type ->
            DropdownMenuItem(
                text = { Text(type.displayName) },
                onClick = { onFilterSelected(type) },
                leadingIcon = if (selectedFilter == type) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}

@Composable
fun EntityDetailBottomSheet(
    entity: XRayEntity,
    relatedEntities: List<XRayEntity>,
    chapters: List<Int>,
    onDismiss: () -> Unit,
    onRelatedClick: (XRayEntity) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(entity.type.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = entity.type.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = entity.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = entity.description,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mentions
            Text(
                text = "Appears in ${chapters.size} chapters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chapters) { chapter ->
                    SuggestionChip(
                        onClick = { },
                        label = { Text("Ch. ${chapter + 1}") }
                    )
                }
            }

            if (relatedEntities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                // Related Entities
                Text(
                    text = "Related",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(relatedEntities) { related ->
                        SuggestionChip(
                            onClick = { onRelatedClick(related) },
                            label = { Text(related.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete X-ray Data") },
        text = { Text("Are you sure you want to delete all X-ray analysis data? This cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Extension properties

private val XRayEntityType.displayName: String
    get() = when (this) {
        XRayEntityType.CHARACTER -> "Characters"
        XRayEntityType.LOCATION -> "Locations"
        XRayEntityType.TERM -> "Terms"
        XRayEntityType.ORGANIZATION -> "Organizations"
        XRayEntityType.EVENT -> "Events"
        XRayEntityType.CONCEPT -> "Concepts"
        XRayEntityType.OTHER -> "Other"
    }

private val XRayEntityType.color: Color
    get() = when (this) {
        XRayEntityType.CHARACTER -> Color(0xFF6200EA)
        XRayEntityType.LOCATION -> Color(0xFF00695C)
        XRayEntityType.TERM -> Color(0xFF617005)
        XRayEntityType.ORGANIZATION -> Color(0xFF7F2207)
        XRayEntityType.EVENT -> Color(0xFF950527)
        XRayEntityType.CONCEPT -> Color(0xFF006C4C)
        XRayEntityType.OTHER -> Color(0xFF555555)
    }

private val XRayEntityType.icon
    @Composable get() = when (this) {
        XRayEntityType.CHARACTER -> Icons.Default.Person
        XRayEntityType.LOCATION -> Icons.Default.Place
        XRayEntityType.TERM -> Icons.Default.MenuBook
        XRayEntityType.ORGANIZATION -> Icons.Default.Business
        XRayEntityType.EVENT -> Icons.Default.Event
        XRayEntityType.CONCEPT -> Icons.Default.Lightbulb
        XRayEntityType.OTHER -> Icons.Default.Category
    }
