package com.example.make.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.make.data.news.CalendarEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(dao: com.example.make.data.local.dao.IntelligenceDao) {
    val repository = remember { com.example.make.data.news.NewsRepository() }
    val scope = rememberCoroutineScope()

    var events by remember { mutableStateOf(emptyList<CalendarEvent>()) }
    var displayedEvents by remember { mutableStateOf(emptyList<CalendarEvent>()) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Filters: Default ALL on
    var filterImportance by remember { mutableStateOf(setOf(1, 2, 3)) }

    // Initial Load
    LaunchedEffect(Unit) {
        isRefreshing = true
        events = repository.getEconomicEvents()
        isRefreshing = false
    }

    // Filter Logic
    LaunchedEffect(events, filterImportance) {
        displayedEvents = events.filter { 
            filterImportance.contains(it.importance)
        }.sortedByDescending { it.date }
    }

    // Group by Date
    val groupedEvents = remember(displayedEvents) {
        displayedEvents.groupBy { it.date }.toSortedMap()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    // 1. Title Row
                    Text(
                        "ECONOMIC CALENDAR",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "Global Economic Events",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // 2. Filter & Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Filters
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = filterImportance.contains(3),
                                onClick = { 
                                    filterImportance = if (filterImportance.contains(3)) filterImportance - 3 else filterImportance + 3
                                },
                                label = { Text("High") },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp))
                                            .background(getImportanceColor(3))
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            FilterChip(
                                selected = filterImportance.contains(2),
                                onClick = { 
                                    filterImportance = if (filterImportance.contains(2)) filterImportance - 2 else filterImportance + 2
                                },
                                label = { Text("Med") },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp))
                                            .background(getImportanceColor(2))
                                    )
                                }
                            )
                            FilterChip(
                                selected = filterImportance.contains(1),
                                onClick = { 
                                    filterImportance = if (filterImportance.contains(1)) filterImportance - 1 else filterImportance + 1
                                },
                                label = { Text("Low") },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp))
                                            .background(getImportanceColor(1))
                                    )
                                }
                            )
                        }

                        // Refresh Button
                        IconButton(
                            onClick = {
                                scope.launch {
                                    isRefreshing = true
                                    events = repository.getEconomicEvents()
                                    isRefreshing = false
                                }
                            }
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            groupedEvents.forEach { (date, dayEvents) ->
                item(key = "header_$date") {
                    DateHeader(date)
                }
                items(dayEvents, key = { it.id }) { event ->
                    EventCard(event)
                }
            }
        }
    }
}

@Composable
fun DateHeader(date: String) {
    val displayDate = try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        // 한국어 포맷: 1월 2일 (금)
        val formatter = SimpleDateFormat("M월 d일 (E)", Locale.KOREA)
        parser.parse(date)?.let { formatter.format(it) } ?: date
    } catch (e: Exception) { date }

    Text(
        text = displayDate,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun EventCard(event: CalendarEvent) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time & Country
            Column(
                modifier = Modifier.width(60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = event.time,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (event.country.isNotEmpty()) {
                    Text(
                        text = event.country,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Event Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(getImportanceColor(event.importance))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Figures
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ConfigItem("Act", event.actual, true)
                    ConfigItem("Fcst", event.forecast)
                    ConfigItem("Prev", event.previous)
                }
            }
        }
    }
}

@Composable
fun ConfigItem(label: String, value: String?, isHighlight: Boolean = false) {
    if (value != null) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(
                value, 
                style = MaterialTheme.typography.bodySmall, 
                fontWeight = if(isHighlight) FontWeight.Bold else FontWeight.Normal,
                color = if(isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun getImportanceColor(importance: Int): Color {
    return when (importance) {
        3 -> Color(0xFFD32F2F) // High (Red)
        2 -> Color(0xFFF57C00) // Medium (Orange)
        else -> Color(0xFF388E3C) // Low (Green)
    }
}
