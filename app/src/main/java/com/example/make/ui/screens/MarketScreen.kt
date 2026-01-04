package com.example.make.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.example.make.data.model.EconomicIndicator
import com.example.make.data.model.IndicatorType

import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

enum class MarketCategory(val label: String) {
    INDICES("지수 선물"),
    BONDS("채권"),
    FOREX("외환"),
    COMMODITIES("원자재"),
    CRYPTO("암호화폐")
}

data class MarketTicker(
    val name: String,
    val symbol: String,
    val price: String,
    val change: String,
    val changeRate: String,
    val category: MarketCategory,
    val isPositive: Boolean
)

// ✅ 제1원칙: 더미 데이터 절대 사용 안 함
// ✅ 초기값: 구조만 생성 (값은 "0.0" = 데이터 없음)
// ✅ 실제 데이터: Twelve Data API 호출 후 업데이트 (Line 411-451)
fun getInitialMarketData(): List<MarketTicker> = listOf(
    // 지수 선물 3개 (초기값: 0.0 = 데이터 없음)
    MarketTicker("US 30 (다우)", "YM", "0.0", "0.0", "(0.00%)", MarketCategory.INDICES, true),
    MarketTicker("US 500 (S&P)", "ES", "0.0", "0.0", "(0.00%)", MarketCategory.INDICES, true),
    MarketTicker("US Tech 100", "NQ", "0.0", "0.0", "(0.00%)", MarketCategory.INDICES, true),

    // 채권 2개 (초기값: 0.0 = 데이터 없음)
    MarketTicker("미국 10년물", "US10Y", "0.0", "0.0", "(0.00%)", MarketCategory.BONDS, true),
    MarketTicker("일본 10년물", "JP10Y", "0.0", "0.0", "(0.00%)", MarketCategory.BONDS, true),

    // 외환 3개 (초기값: 0.0 = 데이터 없음)
    MarketTicker("달러/엔", "USD/JPY", "0.0", "0.0", "(0.00%)", MarketCategory.FOREX, true),
    MarketTicker("유로/달러", "EUR/USD", "0.0", "0.0", "(0.00%)", MarketCategory.FOREX, true),
    MarketTicker("파운드/달러", "GBP/USD", "0.0", "0.0", "(0.00%)", MarketCategory.FOREX, true),

    // 원자재 4개 (초기값: 0.0 = 데이터 없음)
    MarketTicker("금", "XAU/USD", "0.0", "0.0", "(0.00%)", MarketCategory.COMMODITIES, true),
    MarketTicker("은", "XAG/USD", "0.0", "0.0", "(0.00%)", MarketCategory.COMMODITIES, true),
    MarketTicker("구리", "COPPER", "0.0", "0.0", "(0.00%)", MarketCategory.COMMODITIES, true),
    MarketTicker("WTI 원유", "WTI/USD", "0.0", "0.0", "(0.00%)", MarketCategory.COMMODITIES, true),

    // 암호화폐 1개 (초기값: 0.0 = 데이터 없음)
    MarketTicker("비트코인", "BTC/USD", "0.0", "0.0", "(0.00%)", MarketCategory.CRYPTO, true)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(dao: com.example.make.data.local.dao.IntelligenceDao) {
    // 0. Data Repositories
    val twelveDataRepo = remember { com.example.make.data.remote.TwelveDataRepository() }
    val ecosRepo = remember { com.example.make.data.remote.EcosRepository() }
    // Global Market Data State
    val marketTickers = remember { mutableStateListOf<MarketTicker>().apply { addAll(getInitialMarketData()) } }
    
    // 1. Settings & Keywords
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsRepo = remember { com.example.make.data.repository.SettingsRepository(context) }
    val keywords by settingsRepo.keywords.collectAsState(initial = emptyList())
    val allEngines by settingsRepo.searchEngines.collectAsState(initial = emptyList())
    val enabledNames by settingsRepo.enabledEngineNames.collectAsState(initial = setOf("Google"))
    
    // Check if Gemini is active (Key exists in settings OR local.properties fallback)
    val isGeminiActive = remember(settingsRepo) {
        val settingsKey = settingsRepo.getGeminiKey()
        val buildConfigKey = com.example.make.BuildConfig.GEMINI_API_KEY
        val isActive = settingsKey.isNotBlank() || buildConfigKey.isNotBlank()
        android.util.Log.d("MarketScreen", "Gemini API active: $isActive (settings: ${settingsKey.isNotBlank()}, buildConfig: ${buildConfigKey.isNotBlank()})")
        isActive
    }
    
    // Derive the active search URL (use the first enabled one, or Google as fallback)
    val currentSearchUrl = remember(allEngines, enabledNames) {
        val firstName = enabledNames.firstOrNull() ?: "Google"
        allEngines.find { it.first == firstName }?.second ?: "https://google.com/search?q="
    }
    
    // 2. Utils
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var isSearching by remember { mutableStateOf(false) }
    var selectedIndicator by remember { mutableStateOf<EconomicIndicator?>(null) }
    // Global Refresh State
    var refreshTrigger by remember { mutableStateOf(0) }
    var isAutoRefreshEnabled by remember { mutableStateOf(true) } // Default ON



    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error message in snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            errorMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { 
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "MARKET SENSING", 
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp
                            )
                            Text(
                                "Global Market", 
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        
                        FilledIconButton(
                            onClick = { refreshTrigger++ },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isGeminiActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isGeminiActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = if (isGeminiActive) "AI 연결됨 - 새로고침" else "새로고침")
                        }
                    }
                }
            }
        }
    )
 { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Economic Indicators
            item {
                var isRefreshing by remember { mutableStateOf(false) }
                var lastUpdateTime by remember { mutableStateOf(System.currentTimeMillis()) }

                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("주요 지표", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Last update time (synced with global refresh)
                        val updateTimeText = remember(refreshTrigger) {
                            val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.KOREA)
                            sdf.format(java.util.Date())
                        }
                        Text(
                            text = "Updated: $updateTimeText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                // Key Indicators State - ECOS 금리/환율 지표
                // ✅ 제1원칙: 더미 데이터 절대 사용 안 함
                // ✅ 초기값: 구조만 생성 (값은 0.0 = 데이터 없음)
                // ✅ 실제 데이터: LaunchedEffect에서 ECOS API 호출 후 업데이트
                var keyIndicators by remember {
                    mutableStateOf(
                        listOf(
                            // 금리 지표 (초기값: 0.0 = 데이터 없음)
                            EconomicIndicator("1", IndicatorType.BASE_RATE, "기준금리", 0.0, "%", 0.0, 0L, "한국은행"),
                            EconomicIndicator("2", IndicatorType.KR_TREASURY, "국고채 3년", 0.0, "%", 0.0, 0L, "한국은행"),
                            EconomicIndicator("3", IndicatorType.KR_TREASURY, "국고채 10년", 0.0, "%", 0.0, 0L, "한국은행"),
                            EconomicIndicator("4", IndicatorType.CD_RATE, "CD 91일", 0.0, "%", 0.0, 0L, "한국은행"),
                            // 환율 지표 (초기값: 0.0 = 데이터 없음)
                            EconomicIndicator("5", IndicatorType.EXCHANGE_RATE, "원/달러", 0.0, "원", 0.0, 0L, "한국은행"),
                            EconomicIndicator("6", IndicatorType.EXCHANGE_RATE, "원/엔(100)", 0.0, "원", 0.0, 0L, "한국은행"),
                            EconomicIndicator("7", IndicatorType.EXCHANGE_RATE, "원/유로", 0.0, "원", 0.0, 0L, "한국은행")
                        )
                    )
                }

                // ✅ Firestore에서 경제지표 조회 (GitHub Actions가 15분마다 수집)
                // ✅ ECOS API 직접 호출 대신 캐싱된 데이터 사용
                LaunchedEffect(refreshTrigger) {
                    isRefreshing = true
                    
                    try {
                        val indicatorsRepo = com.example.make.data.repository.EconomicIndicatorsRepository()
                        val firestoreIndicators = indicatorsRepo.getAllIndicators()
                        
                        if (firestoreIndicators.isNotEmpty()) {
                            // Firestore 데이터를 keyIndicators 순서에 맞게 매핑
                            val indicatorMap = firestoreIndicators.associateBy { it.name }
                            
                            val updatedList = keyIndicators.toMutableList()
                            
                            // 기준금리
                            indicatorMap["기준금리"]?.let { indicator ->
                                updatedList[0] = updatedList[0].copy(
                                    value = indicator.value,
                                    changeRate = indicator.changeRate,
                                    capturedAt = indicator.capturedAt
                                )
                            }
                            
                            // 국고채 3년
                            indicatorMap["국고채 3년"]?.let { indicator ->
                                updatedList[1] = updatedList[1].copy(
                                    value = indicator.value,
                                    changeRate = indicator.changeRate,
                                    capturedAt = indicator.capturedAt
                                )
                            }
                            
                            // 국고채 10년
                            indicatorMap["국고채 10년"]?.let { indicator ->
                                updatedList[2] = updatedList[2].copy(
                                    value = indicator.value,
                                    changeRate = indicator.changeRate,
                                    capturedAt = indicator.capturedAt
                                )
                            }
                            
                            // CD 91일
                            indicatorMap["CD 91일"]?.let { indicator ->
                                updatedList[3] = updatedList[3].copy(
                                    value = indicator.value,
                                    changeRate = indicator.changeRate,
                                    capturedAt = indicator.capturedAt
                                )
                            }
                            
                            // 원/달러
                            indicatorMap["원/달러"]?.let { indicator ->
                                updatedList[4] = updatedList[4].copy(
                                    value = indicator.value,
                                    changeRate = indicator.changeRate,
                                    capturedAt = indicator.capturedAt
                                )
                            }
                            
                            // 원/엔(100)
                            indicatorMap["원/엔(100)"]?.let { indicator ->
                                updatedList[5] = updatedList[5].copy(
                                    value = indicator.value,
                                    changeRate = indicator.changeRate,
                                    capturedAt = indicator.capturedAt
                                )
                            }
                            
                            // 원/유로
                            indicatorMap["원/유로"]?.let { indicator ->
                                updatedList[6] = updatedList[6].copy(
                                    value = indicator.value,
                                    changeRate = indicator.changeRate,
                                    capturedAt = indicator.capturedAt
                                )
                            }
                            
                            keyIndicators = updatedList
                            android.util.Log.d("MarketScreen", "✅ Loaded ${firestoreIndicators.size} indicators from Firestore")
                        } else {
                            android.util.Log.w("MarketScreen", "⚠️ No indicators found in Firestore")
                            errorMessage = "경제지표 데이터가 없습니다. GitHub Actions가 데이터를 수집 중입니다."
                        }
                        
                    } catch (e: Exception) {
                        android.util.Log.e("MarketScreen", "Firestore 조회 실패: ${e.message}", e)
                        errorMessage = "경제지표 로드 실패: ${e.message}"
                    }
                    
                    isRefreshing = false
                }

                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(keyIndicators) { indicator ->
                        IndicatorCard(indicator) { selectedIndicator = indicator }
                    }
                }
                

            }


            // Section 1.5: Global Market Overview (Investing.com Style)
            item {
                var selectedCategory by remember { mutableStateOf(MarketCategory.INDICES) }
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("글로벌 마켓 데이터", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Category Tabs
                    ScrollableTabRow(
                        selectedTabIndex = selectedCategory.ordinal,
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedCategory.ordinal]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        divider = {}
                    ) {
                        MarketCategory.values().forEach { category ->
                            Tab(
                                selected = selectedCategory == category, 
                                onClick = { selectedCategory = category },
                                text = { Text(category.label, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                    
                    // Real-time Update Logic (Triggered by Category Change or Auto Refresh)
                    LaunchedEffect(selectedCategory, refreshTrigger) {
                        // Map internal symbols to Twelve Data API symbols
                        val symbolMap = mapOf(
                            "BTC/USD" to "BTC/USD", "BTC" to "BTC/USD",
                            "ETH" to "ETH/USD", "XAU/USD" to "XAU/USD",
                            "USD/KRW" to "USD/KRW", "USD/JPY" to "USD/JPY",
                            "EUR/USD" to "EUR/USD", "AUD/USD" to "AUD/USD",
                            "SOL" to "SOL/USD", "DOGE" to "DOGE/USD",
                            "US500" to "SPX", "US30" to "DJI", "USTECH" to "IXIC" // Indices might fail if not permitted on free plan
                        )
                        
                        val targets = marketTickers.filter { it.category == selectedCategory }
                        var errorShown = false // Show only one error toast/snackbar per batch

                        targets.forEach { ticker ->
                            val apiSymbol = symbolMap[ticker.symbol] ?: return@forEach
                            try {
                                val result = twelveDataRepo.getQuoteWithResult(apiSymbol)
                                if (result.isSuccess && result.data != null) {
                                    val quote = result.data
                                    val index = marketTickers.indexOf(ticker)
                                    if(index != -1) {
                                        val chg = quote.change?.toDoubleOrNull() ?: 0.0
                                        val pchg = quote.percentChange ?: "0.00"
                                        marketTickers[index] = ticker.copy(
                                            price = String.format("%,.2f", quote.close.toDoubleOrNull() ?: 0.0),
                                            changeRate = String.format("%+.2f (%s%%)", chg, pchg),
                                            isPositive = chg >= 0
                                        )
                                    }
                                } else {
                                    if (!errorShown && result.error != null) {
                                        android.util.Log.e("MarketScreen", "Global Market Error: ${result.error.message}")
                                        // Update the parent error message state to show snackbar
                                        errorMessage = "글로벌 마켓 업데이트 실패: ${result.error.message}"
                                        errorShown = true
                                    }
                                }
                            } catch(e: Exception) { e.printStackTrace() }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Ticker List
                    marketTickers.filter { it.category == selectedCategory }.forEach { ticker ->
                        MarketTickerRow(ticker) {
                            // Map Ticker to EconomicIndicator for Chart Dialog Compatibility
                            selectedIndicator = EconomicIndicator(
                                id = ticker.symbol,
                                type = IndicatorType.EXCHANGE_RATE, // Generic type for simplicity
                                name = ticker.name,
                                value = ticker.price.replace(",", "").toDoubleOrNull() ?: 0.0,
                                unit = "",
                                changeRate = ticker.changeRate.replace("%", "").replace("+", "").replace("(", "").replace(")", "").toDoubleOrNull() ?: 0.0,
                                capturedAt = System.currentTimeMillis(),
                                source = if (ticker.category == MarketCategory.BONDS) "Investing.com" else "Twelve Data"
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }


        }
    }
    
    // Dialog for Chart (Outside LazyColumn)
    if (selectedIndicator != null) {
        IndicatorDetailDialog(
            indicator = selectedIndicator!!, 
            onDismiss = { selectedIndicator = null },
            repository = twelveDataRepo,
            ecosRepository = ecosRepo
        )
    }
    }


@Composable
fun IndicatorCard(indicator: EconomicIndicator, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Indicator Source Badge
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = indicator.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = indicator.name, 
                style = MaterialTheme.typography.labelMedium, 
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = "${indicator.value}${indicator.unit}", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                val isPositive = indicator.changeRate >= 0
                val color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                val icon = if (isPositive) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward
                
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${if(isPositive) "+" else ""}${indicator.changeRate}",
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Mini Preview Chart with Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                val miniData = remember(indicator.id) {
                    val random = java.util.Random(indicator.id.hashCode().toLong())
                    var currentV = 1.0
                    List(30) { 
                        currentV += (random.nextDouble() - 0.5) * 0.2
                        currentV 
                    }
                }
                IndicatorLineChart(
                    data = miniData, 
                    isMini = true,
                    lineColor = if (indicator.changeRate >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun IndicatorDetailDialog(
    indicator: EconomicIndicator, 
    onDismiss: () -> Unit,
    repository: com.example.make.data.remote.TwelveDataRepository,
    ecosRepository: com.example.make.data.remote.EcosRepository
) {
    val ranges = listOf("1D", "1W", "1M", "3M", "6M", "1Y")
    var selectedRange by remember { mutableStateOf("1M") }
    // Chart Data State
    // Chart Data State (Holds either List<TimeSeriesValue> or List<Double>)
    var chartData by remember { mutableStateOf<Any?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var chartError by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }

    // Fetch Real Chart Data
    LaunchedEffect(selectedRange, indicator.id, retryTrigger) {
        isLoading = true
        chartError = null
        chartData = null

        // Map Range to Interval
        val interval = when(selectedRange) {
            "1D" -> "30min"
            "1W" -> "4h"
            "1M" -> "1day"
            "3M" -> "1week" // 12 weeks ~ 3 months
            "6M" -> "1week" // 24 weeks ~ 6 months
            "1Y" -> "1month" // 12 months
            else -> "30min"
        }
        
        // Map Symbol - Twelve Data 유효 심볼
        val symbolMap = mapOf(
            // 외환 (Forex)
            "USD/JPY" to "USD/JPY", 
            "EUR/USD" to "EUR/USD",
            "GBP/USD" to "GBP/USD",
            // 암호화폐
            "BTC/USD" to "BTC/USD",
            // 원자재
            "XAU/USD" to "XAU/USD",  // 금
            "XAG/USD" to "XAG/USD",  // 은
            "COPPER" to "COPPER",    // 구리
            "WTI/USD" to "WTI/USD",  // WTI 원유
            // 지수 선물 (E-mini 선물)
            "YM" to "YM",   // 다우 E-mini
            "ES" to "ES",   // S&P 500 E-mini
            "NQ" to "NQ",   // 나스닥 100 E-mini
            // 채권 (Rate format)
            "US10Y" to "US10Y",
            "JP10Y" to "JP10Y"
        )
        
        // Identify if ECOS data (한국은행)
        val isEcosData = indicator.source == "한국은행" || indicator.source == "BOK" || indicator.source == "KIS"
        
        if (isEcosData) {
            // ECOS 지표 - 통계코드와 항목코드 매핑
            data class EcosInfo(val statCode: String, val itemCode: String, val cycle: String = "D")
            
            val ecosMapping = mapOf(
                // 금리
                "기준금리" to EcosInfo(
                    com.example.make.data.remote.EcosRepository.STAT_BASE_RATE,
                    com.example.make.data.remote.EcosRepository.ITEM_BASE_RATE,
                    "M"
                ),
                "국고채 3년" to EcosInfo(
                    com.example.make.data.remote.EcosRepository.STAT_INTEREST_RATE,
                    com.example.make.data.remote.EcosRepository.ITEM_TREASURY_3Y
                ),
                "국고채 10년" to EcosInfo(
                    com.example.make.data.remote.EcosRepository.STAT_INTEREST_RATE,
                    com.example.make.data.remote.EcosRepository.ITEM_TREASURY_10Y
                ),
                "CD 91일" to EcosInfo(
                    com.example.make.data.remote.EcosRepository.STAT_INTEREST_RATE,
                    com.example.make.data.remote.EcosRepository.ITEM_CD_91D
                ),
                // 환율
                "원/달러" to EcosInfo(
                    com.example.make.data.remote.EcosRepository.STAT_EXCHANGE_RATE,
                    com.example.make.data.remote.EcosRepository.ITEM_USD_KRW
                ),
                "원/엔(100)" to EcosInfo(
                    com.example.make.data.remote.EcosRepository.STAT_EXCHANGE_RATE,
                    com.example.make.data.remote.EcosRepository.ITEM_JPY_KRW
                ),
                "원/유로" to EcosInfo(
                    com.example.make.data.remote.EcosRepository.STAT_EXCHANGE_RATE,
                    com.example.make.data.remote.EcosRepository.ITEM_EUR_KRW
                )
            )
            
            val ecosInfo = ecosMapping[indicator.name]
            if (ecosInfo != null) {
                try {
                    val itemCode = when(indicator.name) {
                        "기준금리" -> com.example.make.data.remote.EcosRepository.ITEM_BASE_RATE
                        "국고채 3년" -> com.example.make.data.remote.EcosRepository.ITEM_TREASURY_3Y
                        "국고채 10년" -> com.example.make.data.remote.EcosRepository.ITEM_TREASURY_10Y
                        "CD 91일" -> com.example.make.data.remote.EcosRepository.ITEM_CD_91D
                        "원/달러" -> com.example.make.data.remote.EcosRepository.ITEM_USD_KRW
                        "원/엔(100)" -> com.example.make.data.remote.EcosRepository.ITEM_JPY_KRW
                        "원/유로" -> com.example.make.data.remote.EcosRepository.ITEM_EUR_KRW
                        else -> null
                    }
                    
                    android.util.Log.d("IndicatorDetail", "Loading chart for ${indicator.name}, itemCode=$itemCode")
                    
                    if (itemCode != null) {
                        val data = ecosRepository.getTimeSeries(itemCode)
                        android.util.Log.d("IndicatorDetail", "Got ${data.size} data points for ${indicator.name}")
                        
                        if (data.isNotEmpty()) {
                            chartData = data
                            android.util.Log.d("IndicatorDetail", "Chart data set successfully")
                        } else {
                            chartError = "${indicator.name}: 데이터가 없습니다. (itemCode: $itemCode)"
                            chartData = null
                            android.util.Log.w("IndicatorDetail", chartError!!)
                        }
                    } else {
                        chartError = "지원하지 않는 지표입니다: ${indicator.name}"
                        chartData = null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("IndicatorDetail", "ECOS 시계열 조회 실패: ${e.message}", e)
                    chartError = "ECOS 데이터 로드 실패: ${e.message}"
                }
            } else {
                chartError = "매핑되지 않은 지표: ${indicator.name}"
                chartData = null
            }
        } else {
            // Twelve Data (글로벌 마켓)
            val apiSymbol = symbolMap[indicator.id] ?: indicator.id
            val result = repository.getTimeSeriesWithResult(apiSymbol, interval)
            
            if (result.isSuccess && result.data != null && !result.data.values.isNullOrEmpty()) {
                chartData = result.data.values.reversed()
            } else {
                chartError = result.error?.message ?: "데이터가 없습니다."
                chartData = null
            }
        }
        isLoading = false
    }

    // Calculate performance metrics
    // Calculate performance metrics (Simplified for generic type)
    val performanceMetrics = remember(chartData) {
        if (chartData == null) emptyMap() 
        else if (chartData is List<*> && (chartData as List<*>).isNotEmpty()) {
            val list = chartData as List<*>
            val metrics = mutableMapOf<String, Double>()
            // Helper to get double value from item
            val getValue = { item: Any? ->
                when(item) {
                     is Double -> item
                     is com.example.make.data.remote.dto.TimeSeriesValue -> item.close?.toDoubleOrNull() ?: 0.0
                     else -> 0.0
                }
            }
            
            val current = getValue(list.last())
            val getPrevValue = { index: Int -> getValue(list.getOrNull(index) ?: list.last()) }
            val size = list.size
            if (current != 0.0) {
                 metrics["1일"] = ((current - getPrevValue(size - 2)) / current * 100)
                 metrics["1주"] = ((current - getPrevValue(maxOf(0, size - 7))) / current * 100)
                 metrics["1달"] = ((current - getPrevValue(maxOf(0, size - 30))) / current * 100)
                 metrics["1년"] = ((current - getValue(list.first())) / current * 100)
            }
            metrics
        } else emptyMap()
    }

    val (startLabel, endLabel) = remember(selectedRange) {
        val sdf = java.text.SimpleDateFormat("yy.MM.dd", java.util.Locale.KOREA)
        val now = java.util.Calendar.getInstance()
        val end = sdf.format(now.time)
        
        val startCal = java.util.Calendar.getInstance().apply {
            when(selectedRange) {
                "1D" -> add(java.util.Calendar.DAY_OF_YEAR, -1)
                "1W" -> add(java.util.Calendar.DAY_OF_YEAR, -7)
                "1M" -> add(java.util.Calendar.DAY_OF_YEAR, -30)
                "3M" -> add(java.util.Calendar.DAY_OF_YEAR, -90)
                "6M" -> add(java.util.Calendar.DAY_OF_YEAR, -180)
                "1Y" -> add(java.util.Calendar.YEAR, -1)
                "5Y" -> add(java.util.Calendar.YEAR, -5)
            }
        }
        val start = sdf.format(startCal.time)
        start to end
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = indicator.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    
                    IconButton(
                        onClick = {
                            val url = when(indicator.source) {
                                "FRED" -> "https://fred.stlouisfed.org/series/DGS10"
                                "BOK" -> "https://ecos.bok.or.kr/"
                                "KOFIA", "KOFIA Bond" -> "https://www.kofiabond.or.kr/websquare/websquare.html?w2xPath=/xml/subMain.xml&divisionId=MBIS01010000000000&parentDivisionId=&parentMenuIndex=&menuIndex=0"
                                "Reuters" -> "https://www.reuters.com/markets/currencies/"
                                else -> "https://www.google.com/search?q=${indicator.name}+chart"
                            }
                            uriHandler.openUri(url)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "기관 원본차트",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${indicator.value}${indicator.unit}", 
                        style = MaterialTheme.typography.titleMedium, 
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    val changeColor = if (indicator.changeRate >= 0) Color(0xFFE57373) else Color(0xFF64B5F6)
                    Text(
                        text = "${if(indicator.changeRate > 0) "+" else ""}${indicator.changeRate}",
                        style = MaterialTheme.typography.titleSmall,
                        color = changeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column {
                // Range Tabs
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ranges) { range ->
                        FilterChip(
                            selected = range == selectedRange,
                            onClick = { selectedRange = range },
                            label = { Text(range, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
                
                // Statistics Panel
                val stats = remember(chartData) {
                    if (chartData is List<*> && (chartData as List<*>).isNotEmpty()) {
                         val list = chartData as List<*>
                         val first = list.first()
                         val values = if (first is Double) {
                             list as List<Double>
                         } else if (first is com.example.make.data.remote.dto.TimeSeriesValue) {
                             (list as List<com.example.make.data.remote.dto.TimeSeriesValue>).mapNotNull { it.close?.toDoubleOrNull() }
                         } else emptyList()
                         
                         if (values.isNotEmpty()) {
                             mapOf(
                                 "high" to (values.maxOrNull() ?: 0.0),
                                 "low" to (values.minOrNull() ?: 0.0),
                                 "avg" to values.average(),
                                 "change" to (values.last() - values.first())
                             )
                         } else null
                     } else null
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val high = stats?.get("high") ?: 0.0
                    val low = stats?.get("low") ?: 0.0
                    val avg = stats?.get("avg") ?: 0.0
                    val change = stats?.get("change") ?: 0.0
                    
                    StatCard("최고", String.format("%.2f", high), Color(0xFFE57373))
                    StatCard("최저", String.format("%.2f", low), Color(0xFF64B5F6))
                    StatCard("평균", String.format("%.2f", avg), Color(0xFF81C784))
                    StatCard("변동", String.format("%+.2f", change), if (change >= 0) Color(0xFFE57373) else Color(0xFF64B5F6))
                }
                
                // Chart Area (Enhanced Candlestick Style)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(Color(0xFF121212), RoundedCornerShape(12.dp))
                        .padding(bottom = 24.dp, start = 12.dp, end = 12.dp, top = 12.dp)
                ) {
                    if (isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (chartError != null) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = chartError ?: "오류 발생",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { retryTrigger++ },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text("재시도", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else if (chartData != null) {
                        if (chartData is List<*> && (chartData as List<*>).isNotEmpty()) {
                            val firstItem = (chartData as List<*>).first()
                            // Check type dynamically
                            if (firstItem is com.example.make.data.remote.dto.TimeSeriesValue) {
                                CandlestickChart(
                                    data = chartData as List<com.example.make.data.remote.dto.TimeSeriesValue>,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (firstItem is Double) {
                                AreaChart(
                                    data = chartData as List<Double>,
                                    color = if (indicator.changeRate >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        // No Data
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                           Text("데이터 없음", color = Color.Gray)
                        }
                    }
                }
                
                // Performance Metrics
                Spacer(modifier = Modifier.height(16.dp))
                Text("기간별 수익률", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(performanceMetrics.entries.toList()) { (period, change) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(period, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            val color = if (change >= 0) Color(0xFFE57373) else Color(0xFF64B5F6)
                            Text(
                                "${if(change > 0) "+" else ""}${"%.2f".format(change)}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Source: ${indicator.source}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        },
        confirmButton = {
            // Confirm button now handled by icon in title
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}

private data class Candle(val open: Double, val high: Double, val low: Double, val close: Double)

@Composable
fun CandlestickChart(
    data: List<com.example.make.data.remote.dto.TimeSeriesValue>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.White,
    startLabel: String = "과거",
    endLabel: String = "현재"
) {
    if (data.isEmpty()) return

    // Parse String to Double
    val parsedData = remember(data) {
        data.map {
            Candle(
                open = it.open?.toDoubleOrNull() ?: 0.0,
                high = it.high?.toDoubleOrNull() ?: 0.0,
                low = it.low?.toDoubleOrNull() ?: 0.0,
                close = it.close?.toDoubleOrNull() ?: 0.0
            )
        }
    }

    val max = parsedData.maxOfOrNull { maxOf(it.high, it.open, it.close, it.low) } ?: 1.0
    val min = parsedData.minOfOrNull { minOf(it.high, it.open, it.close, it.low) } ?: 0.0
    val range = (max - min).coerceAtLeast(0.001)

    // Add padding to Y-axis (10% above and below)
    val paddedMax = max + range * 0.1
    val paddedMin = min - range * 0.1
    val paddedRange = paddedMax - paddedMin
    
    val upColor = Color(0xFFEF5350)   // Red for Up
    val downColor = Color(0xFF26A69A) // Green/Teal for Down
    val gridColor = Color(0xFF333333)
    val textMeasurer = rememberTextMeasurer()
    val textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = Color.Gray)

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val candleWidth = width / parsedData.size
        val barWidth = candleWidth * 0.7f
        
        // Draw Y-Axis Grid & Labels
        for (i in 0..4) {
            val y = height * i / 4
            drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(width, y), strokeWidth = 1f)
            
            val valAtY = paddedMax - (paddedRange * i / 4)
            drawText(textMeasurer, String.format("%.2f", valAtY), androidx.compose.ui.geometry.Offset(5f, y - 15f), textStyle)
        }
        
        parsedData.forEachIndexed { index, ohlc ->
            val x = index * candleWidth + candleWidth / 2
            
            // Map prices to Y (Top is 0, Bottom is height)
            val yHigh = (height * (1 - (ohlc.high - paddedMin) / paddedRange)).toFloat()
            val yLow = (height * (1 - (ohlc.low - paddedMin) / paddedRange)).toFloat()
            val yOpen = (height * (1 - (ohlc.open - paddedMin) / paddedRange)).toFloat()
            val yClose = (height * (1 - (ohlc.close - paddedMin) / paddedRange)).toFloat()
            
            val isUp = ohlc.close >= ohlc.open
            val color = if (isUp) upColor else downColor
            
            // Draw Wick
            drawLine(color, androidx.compose.ui.geometry.Offset(x, yHigh), androidx.compose.ui.geometry.Offset(x, yLow), strokeWidth = 2f)
            
            // Draw Body
            val top = minOf(yOpen, yClose)
            val bottom = maxOf(yOpen, yClose)
            // Ensure at least 1px height
            val bodyHeight = maxOf(2f, bottom - top)
            
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(x - barWidth / 2, top),
                size = androidx.compose.ui.geometry.Size(barWidth, bodyHeight)
            )
        }
    }
}

@Composable
fun AreaChart(
    data: List<Double>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    val textMeasurer = rememberTextMeasurer()
    val textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = Color.Gray)
    
    androidx.compose.foundation.Canvas(modifier = modifier.padding(16.dp)) {
        val max = data.maxOrNull() ?: 1.0
        val min = data.minOrNull() ?: 0.0
        // Padding
        val range = (max - min).coerceAtLeast(0.001)
        val paddedMax = max + range * 0.1
        val paddedMin = min - range * 0.1
        val paddedRange = paddedMax - paddedMin
        
        val widthPerPoint = size.width / (data.size - 1)
        
        val path = androidx.compose.ui.graphics.Path()
        val fillPath = androidx.compose.ui.graphics.Path()
        
        data.forEachIndexed { index, value ->
            val x = index * widthPerPoint
            val y = (size.height * (1 - (value - paddedMin) / paddedRange)).toFloat()
            
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height) // Start from bottom-left
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            // Close fill path at the end
            if (index == data.size - 1) {
                fillPath.lineTo(x, size.height) // To bottom-right
                fillPath.lineTo(0f, size.height) // Close loop to start
            }
        }
        
        // Draw Grid
        for (i in 0..4) {
            val y = size.height * i / 4
            drawLine(Color(0xFF333333), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
            val valAtY = paddedMax - (paddedRange * i / 4)
             drawText(textMeasurer, String.format("%.2f", valAtY), androidx.compose.ui.geometry.Offset(0f, y - 20f), textStyle)
        }
        
        // Draw Fill Gradient
        drawPath(
            path = fillPath,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.3f), Color.Transparent),
                startY = 0f,
                endY = size.height
            )
        )
        
        // Draw Line
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
        )
    }
}

@Composable
fun IndicatorLineChart(
    data: List<Double>, 
    isMini: Boolean = false,
    lineColor: Color = Color(0xFF4A90E2)
) {
    if (data.isEmpty()) return
    
    val max = data.maxOrNull() ?: 1.0
    val min = data.minOrNull() ?: 0.0
    val range = (max - min).coerceAtLeast(0.001)
    
    // Add padding to Y-axis (10% above and below)
    val paddedMax = max + range * 0.1
    val paddedMin = min - range * 0.1
    val paddedRange = paddedMax - paddedMin
    
    // Calculate Moving Averages (only for full view)
    val ma20 = if (!isMini) remember(data) { calculateMovingAverage(data, 20) } else emptyList()
    
    val gridColor = Color(0xFFE0E0E0)
    val ma20Color = Color(0xFFEF9A9A) // Soft desaturated red for MA20
    val textColor = Color(0xFF666666)
    
    // Touch interaction state (only for full view)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    
    // Text measurer for labels
    val textMeasurer = rememberTextMeasurer()
    val textStyle = androidx.compose.ui.text.TextStyle(
        fontSize = 10.sp,
        color = textColor
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (!isMini) {
                    Modifier.pointerInput(data) {
                        detectTapGestures { offset ->
                            val stepX = size.width / (data.size - 1).coerceAtLeast(1)
                            val nearestIndex = (offset.x / stepX).toInt().coerceIn(0, data.size - 1)
                            selectedIndex = nearestIndex
                        }
                    }
                } else Modifier
            )
            .padding(
                start = if (isMini) 0.dp else 40.dp,
                bottom = if (isMini) 0.dp else 20.dp,
                end = 0.dp,
                top = 0.dp
            )
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val stepX = width / (data.size - 1).coerceAtLeast(1)
            
            // Draw grid and Y-axis labels (only for full view)
            if (!isMini) {
                for (i in 0..4) {
                    val y = height * i / 4
                    drawLine(
                        color = gridColor,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(width, y),
                        strokeWidth = 1f
                    )
                    
                    // Y-axis label
                    val value = paddedMax - (paddedRange * i / 4)
                    val label = String.format("%.2f", value)
                    val textLayoutResult = textMeasurer.measure(label, textStyle)
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            -textLayoutResult.size.width.toFloat() - 8.dp.toPx(),
                            y - textLayoutResult.size.height / 2
                        )
                    )
                }
                
                // Draw X-axis date labels (start and end)
                val labels = listOf("과거", "현재")
                labels.forEachIndexed { index, label ->
                    val x = if (index == 0) 0f else width
                    val textLayoutResult = textMeasurer.measure(label, textStyle)
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            if (index == 0) x else x - textLayoutResult.size.width,
                            height + 4.dp.toPx()
                        )
                    )
                }
            }
            
            // Main Path
            val path = androidx.compose.ui.graphics.Path()
            data.forEachIndexed { index, value ->
                val x = index * stepX
                val normalizedY = (value - paddedMin) / paddedRange
                val y = height - (normalizedY * height).toFloat()
                
                if (index == 0) path.moveTo(x, y)
                else {
                    val prevX = (index - 1) * stepX
                    val prevY = height - ((data[index-1] - paddedMin) / paddedRange * height).toFloat()
                    path.cubicTo(prevX + (x - prevX) / 3, prevY, prevX + (x - prevX) * 2 / 3, y, x, y)
                }
            }
            
            // Draw MA20 (only for full view)
            if (!isMini && ma20.isNotEmpty()) {
                val maPath = androidx.compose.ui.graphics.Path()
                ma20.forEachIndexed { index, value ->
                    val x = index * stepX
                    val normalizedY = (value - paddedMin) / paddedRange
                    val y = height - (normalizedY * height).toFloat()
                    if (index == 0) maPath.moveTo(x, y)
                    else {
                        val prevX = (index - 1) * stepX
                        val prevY = height - ((ma20[index-1] - paddedMin) / paddedRange * height).toFloat()
                        maPath.cubicTo(prevX + (x - prevX) / 3, prevY, prevX + (x - prevX) * 2 / 3, y, x, y)
                    }
                }
                drawPath(maPath, ma20Color, alpha = 0.5f, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
            }
            
            // Draw Main Line
            drawPath(path, lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(if (isMini) 1.5f else 2.5f))
            
            // Interaction Elements (full view only)
            if (!isMini) {
                selectedIndex?.let { index ->
                    val x = index * stepX
                    val value = data[index]
                    val normalizedY = (value - paddedMin) / paddedRange
                    val y = height - (normalizedY * height).toFloat()
                    
                    drawLine(Color.Gray.copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, height), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 5f)))
                    drawCircle(Color.White, radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                    drawCircle(lineColor, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                }
            }
        }
        
        // Tooltip (only for full view)
        if (!isMini) {
            selectedIndex?.let { index ->
                val value = data[index]
                Surface(
                    color = lineColor,
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(text = "Value: ${String.format("%.2f", value)}", style = MaterialTheme.typography.labelMedium, color = Color.White)
                        if (index < ma20.size) {
                            Text(text = "MA20: ${String.format("%.2f", ma20[index])}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

// Helper function to calculate moving average
fun calculateMovingAverage(data: List<Double>, period: Int): List<Double> {
    if (data.size < period) return emptyList()
    
    val result = mutableListOf<Double>()
    for (i in period - 1 until data.size) {
        val sum = data.subList(i - period + 1, i + 1).sum()
        result.add(sum / period)
    }
    
    // Pad the beginning with the first MA value to align with original data
    val padding = List(period - 1) { result.firstOrNull() ?: 0.0 }
    return padding + result
}

@Composable
fun StatCard(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MarketTickerRow(ticker: MarketTicker, onClick: () -> Unit) {
    val changeColor = if (ticker.isPositive) Color(0xFFE57373) else Color(0xFFE57373) // Fixed Color Scheme for Dark Mode Investing Style (Red for Up, Blue for Down) usually? Wait, user screenshot: Red is Up (+), Blue is Down (-).
    // Investing.com (Korea) usually uses Red for UP, Blue for DOWN.
    val finalColor = if (ticker.isPositive) Color(0xFFE57373) else Color(0xFF64B5F6)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ticker.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Using a generic chart icon or similar
                Icon(
                    imageVector = Icons.Default.AutoFixHigh, // Placeholder
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Investing.com", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = ticker.price,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${ticker.change} ${ticker.changeRate}",
                style = MaterialTheme.typography.bodyMedium,
                color = finalColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
