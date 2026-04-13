package `in`.karthiknp.secondbrain.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.karthiknp.secondbrain.data.model.MemoryEntity
import `in`.karthiknp.secondbrain.domain.model.MemoryType
import `in`.karthiknp.secondbrain.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dashboard screen — displays all stored memories with filtering, search,
 * summary cards, and quick actions (complete task, delete).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val filteredMemories by viewModel.filteredMemories.collectAsState()
    val ideas by viewModel.ideas.collectAsState()
    val tasks by viewModel.pendingTasks.collectAsState()
    val events by viewModel.upcomingEvents.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ─── Summary Cards ───────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        emoji = "💡",
                        label = "Ideas",
                        count = ideas.size,
                        color = IdeaColor,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.setFilter(
                                if (activeFilter == MemoryType.IDEA) null else MemoryType.IDEA
                            )
                        }
                    )
                    SummaryCard(
                        emoji = "✅",
                        label = "Tasks",
                        count = tasks.size,
                        color = TaskColor,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.setFilter(
                                if (activeFilter == MemoryType.TASK) null else MemoryType.TASK
                            )
                        }
                    )
                    SummaryCard(
                        emoji = "📅",
                        label = "Events",
                        count = events.size,
                        color = EventColor,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.setFilter(
                                if (activeFilter == MemoryType.EVENT) null else MemoryType.EVENT
                            )
                        }
                    )
                }
            }

            // ─── Search Bar ──────────────────────────────────────
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            "Search memories...",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )
            }

            // ─── Filter Chips ────────────────────────────────────
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = activeFilter == null,
                            onClick = { viewModel.setFilter(null) },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentBlue.copy(alpha = 0.15f),
                                selectedLabelColor = AccentBlue
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = activeFilter == MemoryType.IDEA,
                            onClick = { viewModel.setFilter(MemoryType.IDEA) },
                            label = { Text("💡 Ideas") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IdeaColor.copy(alpha = 0.15f),
                                selectedLabelColor = IdeaColor
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = activeFilter == MemoryType.TASK,
                            onClick = { viewModel.setFilter(MemoryType.TASK) },
                            label = { Text("✅ Tasks") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TaskColor.copy(alpha = 0.15f),
                                selectedLabelColor = TaskColor
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = activeFilter == MemoryType.EVENT,
                            onClick = { viewModel.setFilter(MemoryType.EVENT) },
                            label = { Text("📅 Events") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EventColor.copy(alpha = 0.15f),
                                selectedLabelColor = EventColor
                            )
                        )
                    }
                }
            }

            // ─── Memory List ─────────────────────────────────────
            if (filteredMemories.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "📭",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No memories yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            Text(
                                "Start capturing thoughts in the Chat tab!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            } else {
                items(
                    items = filteredMemories,
                    key = { it.id }
                ) { memory ->
                    MemoryCard(
                        memory = memory,
                        onToggleComplete = { viewModel.toggleTaskCompletion(memory) },
                        onDelete = { viewModel.deleteMemory(memory) }
                    )
                }
            }

            // Bottom spacer for nav bar
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

// ─── Sub-Composables ─────────────────────────────────────────────────

@Composable
private fun SummaryCard(
    emoji: String,
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f),
        tonalElevation = 0.dp,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Text(
                "$count",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun MemoryCard(
    memory: MemoryEntity,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val (emoji, typeColor) = when (memory.type) {
        MemoryType.IDEA -> "💡" to IdeaColor
        MemoryType.TASK -> (if (memory.status == `in`.karthiknp.secondbrain.domain.model.TaskStatus.COMPLETED) "☑️" else "⬜") to TaskColor
        MemoryType.EVENT -> "📅" to EventColor
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Type indicator + checkbox for tasks
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(typeColor.copy(alpha = 0.12f))
                    .then(
                        if (memory.type == MemoryType.TASK)
                            Modifier.clickable { onToggleComplete() }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, style = MaterialTheme.typography.bodyLarge)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = memory.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (memory.status == `in`.karthiknp.secondbrain.domain.model.TaskStatus.COMPLETED) TextDecoration.LineThrough else null
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Type badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = typeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = memory.type.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Date
                    memory.datetime?.let {
                        Text(
                            text = dateFormat.format(Date(it)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // Delete button
            IconButton(
                onClick = { showDeleteConfirm = !showDeleteConfirm },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (showDeleteConfirm) Icons.Default.DeleteForever else Icons.Default.MoreVert,
                    contentDescription = "Delete",
                    tint = if (showDeleteConfirm) AccentRed
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }

            if (showDeleteConfirm) {
                IconButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Confirm delete",
                        tint = AccentRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
