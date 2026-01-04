# 경제 지표 대시보드 상세설계서

**프로젝트**: Make - 경제 인사이트 앱  
**작성일**: 2025-12-31  
**버전**: 1.0

---

## 1. 개요

### 1.1 목적

한국은행 ECOS API의 **KeyStatisticList(핵심 100 지표)**를 활용하여:

- 실시간 **금리·환율·물가(CPI)** 추이를 시각화
- 지표 간 **상관관계 분석** (금리-환율, 환율-CPI 등)
- **추세 신호**(이동평균 교차, 변동성 돌파) 자동 생성 및 알림

### 1.2 범위

| 기능 | 설명 |
|------|------|
| 오늘의 경제 위젯 | 홈 화면에 핵심 3개 지표 스냅샷 표시 |
| 대시보드 화면 | 시계열 차트 + 상관관계 히트맵 + 신호 패널 |
| 알림 시스템 | 중요 신호 발생 시 Snackbar + 푸시 알림 |

---

## 2. 시스템 아키텍처

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │
│  │EconomicWidget│  │DashboardScreen│  │DetailDialog│  │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  │
└─────────┼────────────────┼────────────────┼─────────┘
          │                │                │
          ▼                ▼                ▼
┌─────────────────────────────────────────────────────┐
│              ViewModel Layer                         │
│         ┌───────────────────────────┐               │
│         │     MarketViewModel       │               │
│         │  - keyIndicators          │               │
│         │  - timeSeries             │               │
│         │  - correlationMatrix      │               │
│         │  - signals                │               │
│         └─────────────┬─────────────┘               │
└───────────────────────┼─────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────┐
│              Repository Layer                        │
│  ┌─────────────────┐  ┌─────────────────┐           │
│  │ EcosRepository  │  │TwelveDataRepository│         │
│  │ - getKeyStats() │  │ - getQuote()    │           │
│  │ - getTimeSeries()│ │ - getTimeSeries()│          │
│  └────────┬────────┘  └────────┬────────┘           │
└───────────┼────────────────────┼────────────────────┘
            │                    │
            ▼                    ▼
┌─────────────────────────────────────────────────────┐
│              Remote API Layer                        │
│  ┌─────────────────┐  ┌─────────────────┐           │
│  │   ECOS API      │  │  Twelve Data    │           │
│  │ (한국은행)       │  │  (글로벌 시장)   │           │
│  └─────────────────┘  └─────────────────┘           │
└─────────────────────────────────────────────────────┘
```

---

## 3. 데이터 모델

### 3.1 도메인 모델

```kotlin
// 핵심 지표 DTO
data class KeyStatisticDto(
    val code: String,          // 통계코드
    val name: String,          // 지표명
    val value: Double,         // 현재값
    val unit: String,          // 단위
    val date: String,          // 기준일
    val changeRate: Double     // 전일 대비 변동률 (%)
)

// 시계열 데이터
data class KeyTimeSeries(
    val interestRate: List<Double>,  // 금리 시계열
    val fxRate: List<Double>,        // 환율 시계열
    val cpi: List<Double>,           // CPI 시계열
    val dates: List<String>          // 날짜 라벨
)

// 상관관계 매트릭스
data class CorrelationMatrix(
    val interestFx: Double,     // 금리-환율 상관계수
    val interestCpi: Double,    // 금리-CPI 상관계수
    val fxCpi: Double           // 환율-CPI 상관계수
)

// 신호
data class Signal(
    val title: String,
    val type: SignalType,       // BUY, SELL, HOLD, WARNING
    val description: String,
    val timestamp: Long
)

enum class SignalType { BUY, SELL, HOLD, WARNING }
```

### 3.2 API 응답 모델 (ECOS)

```kotlin
@Serializable
data class KeyStatisticListResponse(
    @SerialName("StatisticSearch")
    val statisticSearch: StatisticSearchWrapper?
)

@Serializable
data class StatisticSearchWrapper(
    val row: List<KeyStatisticRow>?
)

@Serializable
data class KeyStatisticRow(
    @SerialName("STAT_CODE") val statCode: String,
    @SerialName("STAT_NAME") val statName: String,
    @SerialName("DATA_VALUE") val dataValue: String?,
    @SerialName("UNIT_NAME") val unitName: String?,
    @SerialName("TIME") val time: String?
)
```

---

## 4. API 설계

### 4.1 ECOS API 엔드포인트

| 서비스 | URL | 용도 |
|--------|-----|------|
| KeyStatisticList | `/KeyStatisticList/{KEY}/json/kr/1/100` | 핵심 100 지표 조회 |
| StatisticSearch | `/StatisticSearch/{KEY}/json/kr/1/30/{CODE}/D/{START}/{END}/{ITEM}` | 시계열 조회 |

### 4.2 주요 통계코드

| 지표 | 통계코드 | 항목코드 |
|------|----------|----------|
| 기준금리 | 722Y001 | 0101000 |
| 국고채 10년 | 817Y002 | 010210000 |
| CD 91일 | 817Y002 | 010502000 |
| 원/달러 환율 | 731Y001 | 0000001 |
| 소비자물가지수(CPI) | 901Y009 | 0 |

---

## 5. Repository 설계

### 5.1 EcosRepository 확장

```kotlin
class EcosRepository {
    // 기존 메서드 유지
    
    // 신규: 핵심 지표 조회
    suspend fun getKeyStatistics(): ApiResult<List<KeyStatisticDto>>
    
    // 신규: 시계열 조회 (캐시 적용)
    suspend fun getTimeSeriesCached(
        statCode: String,
        itemCode: String,
        days: Int = 30
    ): ApiResult<List<Double>>
    
    // 캐시 관리
    private val cache: MutableMap<String, CachedData> = mutableMapOf()
    private val cacheTtlMs = 5 * 60 * 1000L  // 5분
}

data class CachedData(
    val data: Any,
    val timestamp: Long
)
```

---

## 6. ViewModel 설계

### 6.1 MarketViewModel

```kotlin
@HiltViewModel
class MarketViewModel @Inject constructor(
    private val ecosRepo: EcosRepository,
    private val twelveDataRepo: TwelveDataRepository
) : ViewModel() {

    // State
    private val _keyIndicators = MutableStateFlow<List<KeyStatisticDto>>(emptyList())
    val keyIndicators: StateFlow<List<KeyStatisticDto>> = _keyIndicators.asStateFlow()

    private val _timeSeries = MutableStateFlow<KeyTimeSeries?>(null)
    val timeSeries: StateFlow<KeyTimeSeries?> = _timeSeries.asStateFlow()

    private val _correlation = MutableStateFlow<CorrelationMatrix?>(null)
    val correlation: StateFlow<CorrelationMatrix?> = _correlation.asStateFlow()

    private val _signals = MutableStateFlow<List<Signal>>(emptyList())
    val signals: StateFlow<List<Signal>> = _signals.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Actions
    fun refreshAll() { ... }
    fun refreshKeyIndicators() { ... }
    fun fetchTimeSeries() { ... }
    fun computeCorrelation() { ... }
    fun generateSignals() { ... }
}
```

---

## 7. UI 설계

### 7.1 EconomicWidget (위젯)

```
┌────────────────────────────────────────┐
│  오늘의 경제              🔄 Refresh   │
├────────────────────────────────────────┤
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│ │ 📈 금리   │ │ 💱 환율   │ │ 📊 CPI   │ │
│ │  3.50%   │ │ 1,450.2  │ │  102.5   │ │
│ │  ▲0.25   │ │  ▼12.5   │ │  ▲0.3    │ │
│ └──────────┘ └──────────┘ └──────────┘ │
└────────────────────────────────────────┘
```

### 7.2 DashboardScreen (대시보드)

```
┌────────────────────────────────────────┐
│  핵심 100 지표 대시보드    2025-12-31  │
├────────────────────────────────────────┤
│  📈 시계열 차트 (금리/환율/CPI)         │
│  ┌──────────────────────────────────┐  │
│  │     ~~~  Line Chart  ~~~         │  │
│  │   (3개 라인: 금리, 환율, CPI)      │  │
│  └──────────────────────────────────┘  │
├────────────────────────────────────────┤
│  🔥 상관관계 히트맵                     │
│  ┌──────────────────────────────────┐  │
│  │  금리  환율  CPI                  │  │
│  │  ■■■   ■■□   ■□□   금리           │  │
│  │  ■■□   ■■■   ■■□   환율           │  │
│  │  ■□□   ■■□   ■■■   CPI            │  │
│  └──────────────────────────────────┘  │
├────────────────────────────────────────┤
│  🚨 신호 패널                          │
│  ┌──────────────────────────────────┐  │
│  │ ⬆️ 금리 상승 교차 (20MA > 50MA)   │  │
│  │ ⚠️ 환율 변동성 확대 (ATR 2σ)      │  │
│  │ ➡️ CPI 안정 (7일 평균 이하)        │  │
│  └──────────────────────────────────┘  │
└────────────────────────────────────────┘
```

---

## 8. 신호 생성 알고리즘

### 8.1 이동평균 교차 (MA Cross)

```kotlin
fun detectMaCross(series: List<Double>): Signal? {
    val ma20 = series.takeLast(20).average()
    val ma50 = series.takeLast(50).average()
    val prevMa20 = series.dropLast(1).takeLast(20).average()
    val prevMa50 = series.dropLast(1).takeLast(50).average()
    
    return when {
        ma20 > ma50 && prevMa20 <= prevMa50 -> 
            Signal("상승 교차", SignalType.BUY, "20MA가 50MA 상향 돌파")
        ma20 < ma50 && prevMa20 >= prevMa50 -> 
            Signal("하락 교차", SignalType.SELL, "20MA가 50MA 하향 돌파")
        else -> null
    }
}
```

### 8.2 변동성 돌파 (Volatility Breakout)

```kotlin
fun detectVolatilityBreakout(series: List<Double>): Signal? {
    val current = series.last()
    val atr = calculateAtr(series, 14)
    val threshold = series.dropLast(1).last() + (2 * atr)
    
    return if (current > threshold) {
        Signal("변동성 돌파", SignalType.WARNING, "ATR 2배 초과 상승")
    } else null
}
```

### 8.3 피어슨 상관계수

```kotlin
fun pearsonCorrelation(x: List<Double>, y: List<Double>): Double {
    val n = minOf(x.size, y.size)
    val meanX = x.take(n).average()
    val meanY = y.take(n).average()
    
    var numerator = 0.0
    var denomX = 0.0
    var denomY = 0.0
    
    for (i in 0 until n) {
        val dx = x[i] - meanX
        val dy = y[i] - meanY
        numerator += dx * dy
        denomX += dx * dx
        denomY += dy * dy
    }
    
    return numerator / sqrt(denomX * denomY)
}
```

---

## 9. 에러 처리

| 에러 코드 | 원인 | 처리 방법 |
|-----------|------|-----------|
| INFO-100 | API 키 무효 | 설정 화면으로 이동 안내 |
| INFO-200 | 데이터 없음 | "데이터가 없습니다" 표시 |
| ERROR-602 | 과도한 호출 | 30초 대기 후 재시도 |
| NETWORK_ERROR | 네트워크 오류 | "인터넷 연결 확인" 메시지 |

---

## 10. 파일 구조

```
app/src/main/java/com/example/make/
├── data/
│   ├── model/
│   │   ├── KeyStatisticDto.kt
│   │   ├── KeyTimeSeries.kt
│   │   ├── CorrelationMatrix.kt
│   │   └── Signal.kt
│   └── remote/
│       └── EcosRepository.kt (확장)
├── ui/
│   ├── components/
│   │   ├── EconomicWidget.kt
│   │   ├── LineChartView.kt
│   │   ├── CorrelationHeatMap.kt
│   │   └── SignalPanel.kt
│   ├── screens/
│   │   └── DashboardScreen.kt
│   └── viewmodel/
│       └── MarketViewModel.kt (확장)
└── util/
    └── StatisticsUtils.kt (MA, ATR, Pearson)
```

---

## 11. 구현 일정

| 단계 | 작업 | 예상 시간 |
|------|------|-----------|
| 1 | 데이터 모델 정의 | 1시간 |
| 2 | EcosRepository 확장 (캐시 포함) | 2시간 |
| 3 | MarketViewModel 구현 | 2시간 |
| 4 | EconomicWidget UI | 1시간 |
| 5 | DashboardScreen UI | 3시간 |
| 6 | 신호 알고리즘 구현 | 2시간 |
| 7 | 테스트 및 디버깅 | 2시간 |
| **합계** | | **13시간** |

---

## 12. 참고 자료

- [ECOS API 공식 문서](https://ecos.bok.or.kr/api/)
- [한국은행 통계 코드 목록](https://ecos.bok.or.kr/)
- [MPAndroidChart 라이브러리](https://github.com/PhilJay/MPAndroidChart)
