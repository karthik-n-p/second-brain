package `in`.karthiknp.secondbrain.ui.today

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.karthiknp.secondbrain.data.model.HabitEntity
import `in`.karthiknp.secondbrain.data.model.MemoryEntity
import `in`.karthiknp.secondbrain.domain.model.TaskStatus
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val SoftPeach = Color(0xFFFFF0E6)
private val SoftMint = Color(0xFFE8F5EC)
private val SoftLavender = Color(0xFFEDE7F6)
private val SoftCream = Color(0xFFFFFDE7)
private val StreakFire = Color(0xFFFF7043)
private val SoftGreen = Color(0xFF66BB6A)
private val SoftBlue = Color(0xFF64B5F6)
private val CalendarActive = Color(0xFF81C784)

@Composable
fun TodayScreen(viewModel: TodayViewModel) {
    val journalText by viewModel.journalText.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val completions by viewModel.todayCompletions.collectAsState()
    val tasks by viewModel.todayTasks.collectAsState()
    val globalStreak by viewModel.globalStreak.collectAsState()
    val activeDays by viewModel.activeDays.collectAsState()
    val completedIds = completions.map { it.habitId }.toSet()
    val today = LocalDate.now()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { DateHeader(today = today, streak = globalStreak) }
        item { CalendarHeatmap(today = today, activeDays = activeDays) }
        item { JournalSection(text = journalText, onTextChange = { viewModel.updateJournalText(it) }, onSave = { viewModel.saveJournal() }) }
        item { HabitsSection(habits = habits, completedIds = completedIds, onToggle = { viewModel.toggleHabit(it) }, onAdd = { viewModel.addHabit(it) }, onDelete = { viewModel.deleteHabit(it) }) }
        item { TasksSection(tasks = tasks, onToggle = { viewModel.toggleTask(it) }, onAdd = { viewModel.addTask(it) }, onDelete = { viewModel.deleteTask(it) }) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun DateHeader(today: LocalDate, streak: Int) {
    Surface(shape = RoundedCornerShape(20.dp), color = SoftCream, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()), style = MaterialTheme.typography.labelLarge, color = Color(0xFF8D6E63))
                Text(today.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
            }
            if (streak > 0) {
                Surface(shape = RoundedCornerShape(12.dp), color = StreakFire.copy(alpha = 0.15f)) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("\uD83D\uDD25", fontSize = 18.sp)
                        Text("$streak", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StreakFire)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarHeatmap(today: LocalDate, activeDays: Set<String>) {
    val currentMonth = YearMonth.from(today)
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value

    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { day ->
                    Text(day, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), textAlign = TextAlign.Center)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            val totalCells = firstDayOfWeek - 1 + daysInMonth
            val rows = (totalCells + 6) / 7
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val dayNumber = row * 7 + col - (firstDayOfWeek - 1) + 1
                        if (dayNumber in 1..daysInMonth) {
                            val date = currentMonth.atDay(dayNumber)
                            val dateStr = date.toString()
                            val isActive = activeDays.contains(dateStr)
                            val isToday = date == today
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(when { isToday && isActive -> CalendarActive; isToday -> SoftBlue.copy(alpha = 0.3f); isActive -> CalendarActive.copy(alpha = 0.6f); else -> Color.Transparent }).then(if (isToday) Modifier.border(1.5.dp, SoftBlue, CircleShape) else Modifier), contentAlignment = Alignment.Center) {
                                    Text("$dayNumber", style = MaterialTheme.typography.labelSmall, color = when { isActive -> Color.White; isToday -> SoftBlue; else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) }, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalSection(text: String, onTextChange: (String) -> Unit, onSave: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = SoftPeach, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("\u270f\ufe0f", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Journal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF5D4037))
            }
            Spacer(modifier = Modifier.height(12.dp))
            BasicTextField(value = text, onValueChange = onTextChange, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).onFocusChanged { if (!it.isFocused) onSave() }, textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF3E2723)), cursorBrush = SolidColor(Color(0xFF8D6E63)), decorationBox = { innerTextField ->
                Box { if (text.isEmpty()) { Text("How was your day? Write something...", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF8D6E63).copy(alpha = 0.5f)) }; innerTextField() }
            })
        }
    }
}

@Composable
private fun HabitsSection(habits: List<HabitEntity>, completedIds: Set<Long>, onToggle: (HabitEntity) -> Unit, onAdd: (String) -> Unit, onDelete: (HabitEntity) -> Unit) {
    var showAddField by remember { mutableStateOf(false) }
    var newHabitName by remember { mutableStateOf("") }
    Surface(shape = RoundedCornerShape(20.dp), color = SoftMint, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("\uD83D\uDD25", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Habits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32), modifier = Modifier.weight(1f))
                IconButton(onClick = { showAddField = !showAddField }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add habit", tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                }
            }
            if (habits.isEmpty() && !showAddField) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("No habits yet. Tap + to add one!", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2E7D32).copy(alpha = 0.5f))
            }
            habits.forEach { habit ->
                val isCompleted = completedIds.contains(habit.id)
                HabitRow(habit = habit, isCompleted = isCompleted, onToggle = { onToggle(habit) })
            }
            AnimatedVisibility(visible = showAddField) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newHabitName, onValueChange = { newHabitName = it }, placeholder = { Text("e.g. Read 30 min", fontSize = 14.sp) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), unfocusedBorderColor = Color(0xFF2E7D32).copy(alpha = 0.3f), focusedContainerColor = Color.White.copy(alpha = 0.5f), unfocusedContainerColor = Color.White.copy(alpha = 0.3f)))
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(onClick = { onAdd(newHabitName); newHabitName = ""; showAddField = false }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF2E7D32)), modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitRow(habit: HabitEntity, isCompleted: Boolean, onToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = if (isCompleted) 0.6f else 0.3f)).clickable { onToggle() }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(if (isCompleted) SoftGreen else Color.White.copy(alpha = 0.8f)).border(1.5.dp, if (isCompleted) SoftGreen else Color(0xFF2E7D32).copy(alpha = 0.3f), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
            if (isCompleted) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(habit.name, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF1B5E20), modifier = Modifier.weight(1f), textDecoration = if (isCompleted) TextDecoration.LineThrough else null)
        if (habit.streakCount > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("\uD83D\uDD25", fontSize = 14.sp)
                Text("${habit.streakCount}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = StreakFire)
            }
        }
    }
}

@Composable
private fun TasksSection(tasks: List<MemoryEntity>, onToggle: (MemoryEntity) -> Unit, onAdd: (String) -> Unit, onDelete: (MemoryEntity) -> Unit) {
    var showAddField by remember { mutableStateOf(false) }
    var newTaskName by remember { mutableStateOf("") }
    Surface(shape = RoundedCornerShape(20.dp), color = SoftLavender, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("\u2705", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tasks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF4527A0), modifier = Modifier.weight(1f))
                IconButton(onClick = { showAddField = !showAddField }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add task", tint = Color(0xFF4527A0), modifier = Modifier.size(20.dp))
                }
            }
            if (tasks.isEmpty() && !showAddField) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("No tasks for today. Tap + to add one!", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4527A0).copy(alpha = 0.5f))
            }
            tasks.forEach { task ->
                TaskRow(task = task, onToggle = { onToggle(task) })
            }
            AnimatedVisibility(visible = showAddField) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newTaskName, onValueChange = { newTaskName = it }, placeholder = { Text("e.g. Submit PR", fontSize = 14.sp) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF4527A0), unfocusedBorderColor = Color(0xFF4527A0).copy(alpha = 0.3f), focusedContainerColor = Color.White.copy(alpha = 0.5f), unfocusedContainerColor = Color.White.copy(alpha = 0.3f)))
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(onClick = { onAdd(newTaskName); newTaskName = ""; showAddField = false }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF4527A0)), modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: MemoryEntity, onToggle: () -> Unit) {
    val isCompleted = task.status == TaskStatus.COMPLETED
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = if (isCompleted) 0.6f else 0.3f)).clickable { onToggle() }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(if (isCompleted) SoftBlue else Color.White.copy(alpha = 0.8f)).border(1.5.dp, if (isCompleted) SoftBlue else Color(0xFF4527A0).copy(alpha = 0.3f), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
            if (isCompleted) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(task.content, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF311B92), modifier = Modifier.weight(1f), textDecoration = if (isCompleted) TextDecoration.LineThrough else null, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
