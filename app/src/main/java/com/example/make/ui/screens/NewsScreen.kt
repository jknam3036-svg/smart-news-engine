package com.example.make.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.make.data.model.InvestmentInsight
import com.example.make.data.news.NewsRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewsScreen() {
    val repository = remember { NewsRepository() }
    val scope = rememberCoroutineScope()
    
    // Data States
    var allNewsItems by remember { mutableStateOf(emptyList<InvestmentInsight>()) }
    var displayedItems by remember { mutableStateOf(emptyList<InvestmentInsight>()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // UI States
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSourceStats by remember { mutableStateOf(false) }
    
    // Multi-Filter State Object
    var filterPeriod by remember { mutableStateOf(FilterPeriod.ALL) }
    var filterHighImpact by remember { mutableStateOf(false) }
    var filterSourceTypes by remember { 
        mutableStateOf(setOf(SourceCategory.GLOBAL_POLICY, SourceCategory.GURUS, SourceCategory.KOREA, SourceCategory.MARKET, SourceCategory.DEEP)) 
    }

    // Initial Load
    LaunchedEffect(Unit) {
        isLoading = true
        val items = repository.getLatestInsights()
        allNewsItems = items
        displayedItems = items
        isLoading = false
    }

    // Advanced Filtering Logic
    fun applyFilters() {
        val now = java.util.Date()
        
        displayedItems = allNewsItems.filter { item ->
            
            // 1. Period Filter
            val periodMatch = if (filterPeriod == FilterPeriod.ALL) true else {
                val itemDate = item.meta_data.analyzed_at?.toDate()
                if (itemDate != null) {
                    val diffInMillis = now.time - itemDate.time
                    val diffInDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffInMillis)
                    diffInDays <= filterPeriod.days
                } else false
            }
            
            // 2. Impact Filter
            val impactMatch = if (filterHighImpact) item.intelligence.impact_score >= 8 else true
            
            // 3. Source Category Filter
            val sourceMatch = filterSourceTypes.any { category ->
                category.keywords.any { keyword ->
                    item.meta_data.source_name.contains(keyword, ignoreCase = true)
                }
            }

            // Combine All (AND Logic)
            periodMatch && impactMatch && sourceMatch
        }.sortedByDescending { 
            // Sort by Analyzed Date (Timestamp) which is reliable and comparable
             it.meta_data.analyzed_at 
        }
    }

    // Effect to apply filters whenever any filter state changes
    LaunchedEffect(filterPeriod, filterHighImpact, filterSourceTypes, allNewsItems) {
        applyFilters()
    }

    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Smart Filters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                
                // 1. Period
                Text("Time Period", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FilterPeriod.values().toList()) { period ->
                        FilterChip(
                            selected = filterPeriod == period,
                            onClick = { filterPeriod = period },
                            label = { Text(period.label) }
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // 2. Impact
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Analysis", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    FilterChip(
                        selected = filterHighImpact,
                        onClick = { filterHighImpact = !filterHighImpact },
                        label = { Text("🔥 High Impact") },
                        leadingIcon = { if(filterHighImpact) Icon(Icons.Default.Check, null) }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                // 3. Source Type (Replaces Sentiment)
                Text("Source Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                
                // Using FlowRow for safer wrapping if many categories
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp), 
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SourceCategory.values().forEach { category ->
                        FilterChip(
                            selected = filterSourceTypes.contains(category),
                            onClick = { 
                                filterSourceTypes = if (filterSourceTypes.contains(category)) {
                                    if (filterSourceTypes.size > 1) filterSourceTypes - category else filterSourceTypes // Prevent empty
                                } else {
                                    filterSourceTypes + category
                                }
                            },
                            label = { Text(category.label) },
                            leadingIcon = { if(filterSourceTypes.contains(category)) Icon(Icons.Default.Check, null) }
                        )
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Filters")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = { 
                        Column {
                            Text(
                                "EXECUTIVE INTELLIGENCE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Global Market Watch",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray
                            )
                        }
                    },
                    actions = {
                        // Filter Button
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, "Filter")
                        }
                        // Refresh Button
                        IconButton(onClick = { 
                            scope.launch { 
                                isLoading = true
                                val items = repository.getLatestInsights()
                                allNewsItems = items
                                isLoading = false
                            }
                        }) {
                            Icon(Icons.Default.Refresh, "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                
                // Dashboard Stats
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Line 1: Counts
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Total Reports: ${allNewsItems.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "|",
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Showing: ${displayedItems.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))

                    // Line 2: Updates (Renamed to Latest Report)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Latest Report: ${if(allNewsItems.isNotEmpty()) formatDate(allNewsItems[0].meta_data.analyzed_at) else "Syncing..."}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Source Stats",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { showSourceStats = true }
                        )
                    }
                }
                // Removed horizontal scroll chips...
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            // Source Statistics Dialog
            if (showSourceStats) {
                AlertDialog(
                    onDismissRequest = { showSourceStats = false },
                    title = { Text("Source Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                    text = {
                        val stats = allNewsItems.groupingBy { it.meta_data.source_name }
                            .eachCount()
                            .toList()
                            .sortedByDescending { it.second }
                        
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text("Statistics of ${allNewsItems.size} loaded reports:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(Modifier.height(8.dp))
                            stats.forEach { (source, count) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(source, style = MaterialTheme.typography.bodyMedium)
                                    Text("$count", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSourceStats = false }) {
                            Text("Close")
                        }
                    }
                )
            }
            
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(displayedItems) { item ->
                    InsightCard(item)
                }
                
                if (displayedItems.isEmpty() && !isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No news matches your filters.",
                                    color = Color.Gray
                                )
                                Button(onClick = {
                                    // Reset Fitlers
                                    filterPeriod = FilterPeriod.ALL
                                    filterHighImpact = false
                                    filterSourceTypes = SourceCategory.values().toSet()
                                }, modifier = Modifier.padding(top = 16.dp)) {
                                    Text("Reset Filters")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ... InsightCard ...

enum class FilterPeriod(val label: String, val days: Long) {
    ONE_DAY("1 Day", 1),
    THREE_DAYS("3 Days", 3),
    TEN_DAYS("10 Days", 10),
    THIRTY_DAYS("30 Days", 30),
    ALL("All Time", 3650)
}

enum class SourceCategory(val label: String, val keywords: List<String>) {
    GLOBAL_POLICY("🏛️ Global Policy", listOf("Fed", "SEC", "Treasury", "Nick")),
    GURUS("🧠 Guru Watch", listOf("Burry", "Dalio", "Ackman", "Buffett")),
    KOREA("🇰🇷 Korea Market", listOf("MK", "Hankyung", "Yonhap", "Korea", "BOK")),
    MARKET("📈 Gloabl Markets", listOf("WSJ", "Reuters", "CNBC", "Bloomberg", "Forex")),
    DEEP("📖 Deep Dive", listOf("Economist", "Investing", "Insight"))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightCard(item: InvestmentInsight) {
    val sentimentColor = when(item.intelligence.market_sentiment) {
        "BULLISH" -> Color(0xFF4CAF50) // Green
        "BEARISH" -> Color(0xFFE57373) // Red
        else -> Color(0xFFFFB74D) // Orange
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Source & Impact Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.meta_data.source_name.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                // Impact Score Badge
                Surface(
                    color = Color(0xFF424242), // Dark Gray
                    shape = RoundedCornerShape(50),
                    border = if(item.intelligence.impact_score >= 8) BorderStroke(1.dp, Color(0xFFEF5350)) else null // Highlight border for high impact
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if(item.intelligence.market_sentiment == "BULLISH") Icons.AutoMirrored.Filled.TrendingUp 
                                          else if(item.intelligence.market_sentiment == "BEARISH") Icons.AutoMirrored.Filled.TrendingDown 
                                          else Icons.AutoMirrored.Filled.TrendingFlat,
                            contentDescription = null,
                            tint = Color.White, // Icon White
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "IMPACT ${item.intelligence.impact_score}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White // Text White
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // 2. Title & Date (Verification)
            Text(
                item.content.korean_title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp
            )
            
            // Published Date Row (Crucial for Verification)
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Published Time",
                    tint = Color.Gray,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Published: ${formatDate(item.meta_data.published_at, isRelative = false)} (Verified)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            // 3. Body
            Text(
                item.content.korean_body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 20.sp
            )
            
            Spacer(Modifier.height(16.dp))
            
            // 4. Actionable Insight (CIO Box)
            Surface(
                color = sentimentColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = "Insight",
                        tint = sentimentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "CIO's Actionable Insight",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = sentimentColor
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            item.intelligence.actionable_insight,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // 5. Related Tags (FlowRow)
            if (item.intelligence.related_assets.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(
                     horizontalArrangement = Arrangement.spacedBy(8.dp),
                     verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.intelligence.related_assets.forEach { ticker ->
                        SuggestionChip(
                            onClick = { /* Check Quote */ },
                            label = { 
                                Text(
                                    "#$ticker",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                ) 
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f),
                                labelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = null,
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }
        }
    }
}

// Format Date Helper
// Format Date Helper
fun formatDate(input: Any?, isRelative: Boolean = true): String {
    if (input == null) return ""
    
    try {
        val date: java.util.Date = when (input) {
            is com.google.firebase.Timestamp -> input.toDate()
            is String -> {
                // Try to handle ISO string or return as is
                 if (input.contains("Timestamp")) return "Just now"
                 // If it's a simple string date like "Tue, 02 Jan...", we might just return it truncated
                 // But for consistency let's try to pass it simply
                 return input.replace("T", " ").take(16)
            }
            else -> return ""
        }

        if (!isRelative) {
             val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
             return sdf.format(date)
        }

        val now = java.util.Date().time
        val time = date.time
        val diff = now - time
        
        val minute = 60 * 1000L
        val hour = 60 * minute
        val day = 24 * hour

        return when {
            diff < minute -> "Just now"
            diff < 60 * minute -> "${diff / minute}m ago"
            diff < 24 * hour -> "${diff / hour}h ago"
            else -> {
                val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                sdf.format(date)
            }
        }
    } catch (e: Exception) {
         return input?.toString()?.take(16) ?: ""
    }
}

enum class NewsCategory(val label: String, val keywords: List<String>) {
    ALL("All View", emptyList()),
    MEDIA("Tier-1 Media", listOf("WSJ", "Reuters", "CNBC", "Bloomberg", "FT")),
    MAGAZINE("In-Depth", listOf("Economist", "Fortune", "Forbes", "Barron", "HBR")),
    POLICY("Gov/Policy", listOf("Fed", "Treasury", "SEC", "BOK", "WhiteHouse")),
    IB("Global IB", listOf("Goldman", "JPM", "Morgan", "BlackRock", "Citi")),
    SNS("SNS/Guru", listOf("Walter", "Dalio", "Ackman", "Burry", "Reddit"))
}

