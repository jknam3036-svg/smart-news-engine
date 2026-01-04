# API 진단 보고서 - 마켓 데이터 실시간 업데이트 문제

## 📋 문제 요약

**증상**: 마켓 > 경제지표 지수, 차트 실시간 업데이트가 작동하지 않음

## 🔍 원인 분석

### 1. Twelve Data API 제한사항

- **무료 플랜 한도**: 하루 800 요청
- **현재 구현**: 카테고리 변경 및 새로고침 시 모든 심볼에 대해 API 호출
- **문제**: 빠른 API 한도 소진으로 인한 요청 실패

### 2. API 키 상태

✅ **Twelve Data API Key**: 설정됨 (`3f2e4547f483450787e281ce1752fcdc`)
✅ **ECOS API Key**: 설정됨 (`255NKPDQPN0Y4OYFQUQK`)

### 3. 코드 레벨 문제점

#### A. 과도한 API 호출

**위치**: `MarketScreen.kt` 라인 354-385

```kotlin
LaunchedEffect(selectedCategory, refreshTrigger) {
    // 카테고리 변경 시마다 모든 티커에 대해 API 호출
    targets.forEach { ticker ->
        val quote = twelveDataRepo.getQuote(apiSymbol)
        // ...
    }
}
```

**문제**:

- 5개 카테고리 × 평균 7개 티커 = 35회 호출/전환
- 새로고침 버튼 클릭 시 추가 호출

#### B. 에러 피드백 부재

**위치**: `TwelveDataRepository.kt` 라인 36-40

```kotlin
if (body.contains("\"code\":")) {
    android.util.Log.e(TAG, "API Error: $body")
    return@use null
}
```

**문제**:

- 에러 발생 시 로그만 출력하고 UI에 표시하지 않음
- 사용자는 데이터가 업데이트되지 않는 이유를 알 수 없음

#### C. 차트 데이터 로딩 실패 처리 미흡

**위치**: `MarketScreen.kt` 라인 597-603

```kotlin
if (response != null && !response.values.isNullOrEmpty()) {
    chartData = response.values.reversed()
} else {
    chartData = null // No Data
}
```

**문제**:

- `chartData = null`일 때 "데이터 없음" 메시지만 표시
- 실패 원인(API 한도, 네트워크 오류 등)을 구분하지 않음

## 🛠️ 해결 방안

### 1. API 호출 최적화

- **배치 처리**: 동시 호출 수 제한 (최대 3-5개)
- **캐싱**: 최근 조회 데이터를 로컬에 캐시 (5분 TTL)
- **우선순위**: 현재 보이는 카테고리만 업데이트

### 2. 에러 상태 관리

```kotlin
sealed class ApiState<out T> {
    object Idle : ApiState<Nothing>()
    object Loading : ApiState<Nothing>()
    data class Success<T>(val data: T) : ApiState<T>()
    data class Error(val message: String, val code: ErrorCode) : ApiState<Nothing>()
}

enum class ErrorCode {
    RATE_LIMIT_EXCEEDED,
    NETWORK_ERROR,
    INVALID_SYMBOL,
    API_KEY_INVALID
}
```

### 3. 사용자 피드백 개선

- API 한도 초과 시: "일일 API 한도에 도달했습니다. 내일 다시 시도해주세요."
- 네트워크 오류 시: "네트워크 연결을 확인해주세요."
- 로딩 중: 스켈레톤 UI 또는 프로그레스 인디케이터

### 4. 대체 데이터 소스

- **ECOS (한국은행)**: 국내 금리, 환율 데이터
- **Yahoo Finance (비공식 API)**: 글로벌 지수, 환율
- **로컬 캐시**: 최근 24시간 데이터 보존

## 📊 API 사용량 추정

### 현재 구조

| 액션 | 호출 수 | 비고 |
|------|---------|------|
| 앱 시작 | 3 | 주요 지표 (USD/KRW, 국고채, CD금리) |
| 카테고리 전환 (5개) | 35 | 7개 티커 × 5 카테고리 |
| 새로고침 | 38 | 주요 지표 3 + 현재 카테고리 7 |
| 차트 조회 | 1 | 티커 클릭 시 |
| **일일 총합 (예상)** | **~200-400** | 사용 패턴에 따라 변동 |

### 최적화 후

| 액션 | 호출 수 | 개선 사항 |
|------|---------|-----------|
| 앱 시작 | 3 | 동일 |
| 카테고리 전환 | 7 | 현재 카테고리만 업데이트 |
| 새로고침 | 10 | 캐시 활용 (5분 이내 재사용) |
| 차트 조회 | 1 | 동일 |
| **일일 총합 (예상)** | **~50-100** | **75% 감소** |

## 🔧 즉시 적용 가능한 임시 조치

### 1. 수동 새로고침 전용 모드

```kotlin
// 자동 새로고침 비활성화
var isAutoRefreshEnabled by remember { mutableStateOf(false) }

// 새로고침 버튼에만 의존
FilledIconButton(onClick = { 
    if (!isRefreshing) {
        refreshTrigger++ 
    }
})
```

### 2. API 호출 디바운싱

```kotlin
LaunchedEffect(selectedCategory) {
    delay(500) // 500ms 대기
    // API 호출
}
```

### 3. 에러 토스트 메시지

```kotlin
if (quote == null) {
    Toast.makeText(context, "데이터 로드 실패: API 한도 초과 가능", Toast.LENGTH_SHORT).show()
}
```

## 📝 권장 사항

### 단기 (1-2일)

1. ✅ API 호출 로깅 강화 (성공/실패 카운트)
2. ✅ 에러 메시지 UI 추가
3. ✅ 캐시 메커니즘 구현 (SharedPreferences + 타임스탬프)

### 중기 (1주)

1. ⚠️ 대체 API 통합 (Yahoo Finance 또는 Alpha Vantage)
2. ⚠️ 로컬 데이터베이스에 히스토리 저장
3. ⚠️ 배치 처리 및 우선순위 큐 구현

### 장기 (1개월)

1. 🔄 백엔드 프록시 서버 구축 (API 키 보호 + 캐싱)
2. 🔄 WebSocket 기반 실시간 업데이트 (지원 시)
3. 🔄 프리미엄 API 플랜 고려 (Twelve Data Pro: $79/월)

## 🧪 테스트 체크리스트

- [ ] API 키 유효성 확인
- [ ] 네트워크 연결 상태 확인
- [ ] Logcat에서 API 응답 확인 (`TwelveDataRepo` 태그)
- [ ] API 한도 초과 시나리오 테스트
- [ ] 오프라인 모드 동작 확인
- [ ] 차트 데이터 로딩 실패 시 UI 확인

## 📞 다음 단계

1. **로그 수집**: 앱 실행 후 Logcat에서 `TwelveDataRepo`, `EcosRepository` 태그 확인
2. **API 상태 확인**: <https://twelvedata.com/account/api-usage> 에서 사용량 확인
3. **코드 수정**: 위 해결 방안 중 우선순위 높은 항목부터 적용
4. **테스트**: 실제 디바이스에서 동작 확인

---

**작성일**: 2025-12-31
**작성자**: Antigravity AI Assistant
