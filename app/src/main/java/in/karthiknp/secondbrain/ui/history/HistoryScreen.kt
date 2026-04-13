package `in`.karthiknp.secondbrain.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.karthiknp.secondbrain.data.model.JournalEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val TimelineColor = Color(0xFF81C784)
private val SoftPeach = Color(0xFFFFF0E6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val entries by viewModel.journalEntries.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83D\uDCDD", style = MaterialTheme.typography.displayLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No entries yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    Text("Start writing in the Today tab!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(entries, key = { it.id }) { entry -> TimelineEntry(entry = entry) }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun TimelineEntry(entry: JournalEntry) {
    val date = runCatching { LocalDate.parse(entry.dateString) }.getOrNull()
    val dayName = date?.dayOfWeek?.getDisplayName(TextStyle.SHORT, Locale.getDefault()) ?: ""
    val formattedDate = date?.format(DateTimeFormatter.ofPattern("MMM d")) ?: entry.dateString
    val isToday = date == LocalDate.now()

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (isToday) TimelineColor else TimelineColor.copy(alpha = 0.4f)))
            Box(modifier = Modifier.width(2.dp).height(60.dp).background(TimelineColor.copy(alpha = 0.2f)))
        }
        Surface(shape = RoundedCornerShape(16.dp), color = SoftPeach.copy(alpha = 0.7f), modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("$dayName, $formattedDate", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Color(0xFF5D4037))
                    if (isToday) {
                        Surface(shape = RoundedCornerShape(4.dp), color = TimelineColor.copy(alpha = 0.2f)) {
                            Text("Today", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                if (entry.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(entry.content, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF3E2723), maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
