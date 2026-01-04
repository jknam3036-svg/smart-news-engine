# Make: 지능형 비즈니스 오토메이션 플랫폼 상세 개발 설계서

## 1. 개요 (Overview)

본 프로젝트 **'Make'**는 단순한 메일/뉴스 클라이언트가 아닌, **다차원 정보 수집 및 지능형 분석 플랫폼**입니다.
사용자의 비즈니스 데이터를 수집(Mail, SMS, News, Market Data), 분석(Gemini AI), 행동(Calendar, Notification)으로 연결하는 **'결합형 인텔리전스 오토메이션 허브'**입니다.  
유명 자동화 툴인 'Make(구 Integromat)'처럼, 다양한 데이터 소스(Input)가 AI 엔진(Process)을 거쳐 구체적인 업무 행동(Output)으로 자동 연결되는 구조를 지향합니다.

---

## 3. Data Collection Strategy: The 10-Second Strategy

**Core Philosophy**: Reliability and Freshness > Quantity.

- **Cross-Check**: AI validates data by cross-referencing Global sources (Bloomberg, Reuters) with Local official sources (BOK, FSC).
- **Speed**: Direct API/RSS feeds to query 15-30 mins faster than secondary media.

### 3.1 Global & Macro Data (Trust Tier 1)

| Category | Primary Source | Integration Method | Data Points |
| :--- | :--- | :--- | :--- |
| **Global Indicators** | **Bloomberg / Reuters** | Web Scraping / API | Real-time market indices, Breaking News |
| **Market Data** | **Investing.com / CNBC** | HTML Parsing | Futures, Commodities, Crypto, Economic Calendar |
| **Macro Economy** | **FRED (St. Louis Fed)** | **Official API** | Interest Rates, CPI, Dollar Index (Time-series) |
| **Trends** | **The Economist / WSJ** | RSS / Search | In-depth analysis, Industry trends |

### 3.2 Korea Financial & Public Data (Trust Tier 1)

| Category | Primary Source | Integration Method | Data Points |
| :--- | :--- | :--- | :--- |
| **Central Bank** | **Bank of Korea (BOK)** | **RSS / API** | Base Rate, Economic Outlook, BSI |
| **Regulations** | **FSC / FSS** | RSS | Policy changes, DART (Corporate Filings) |
| **Statistics** | **KOSTAT (Statistics Korea)** | API | CPI, Employment trends |
| **Insight** | **KDI / SERI** | Report Parsing | Mid-to-long term policy suggestions |

### 3.3 Integration Logic

1. **API Ingestion**:
    - **FRED**: Real-time dashboard for Interest Rates & USD Index.
    - **DART**: Immediate push notification via Gemini summary when watched companies file disclosures.
2. **Web/RSS**:
    - **BOK**: 'Policy Brief' generation from press releases.
    - **Investing.com**: Calendar integration for upcoming major events (FOMC, CPI release).
3. **Search & Curation**:
    - **Engine**: Google Search API or Perplexity for "Latest News".
    - **Gemini Role**: Read raw data/news and generate **"Strategy Implications" (전략적 시사점)**.

---

## 4. Advanced News Intelligence Pipeline (Detailed Design)

시스템의 핵심인 뉴스 수집 및 정리 로직은 **'지능형 쿼리 확장'**과 **'전략적 시사점 도출'**에 집중합니다.

### 4.1. Process Logic (Sequence)

1. **Step 1: 지능형 쿼리 생성 (Query Expansion)**
    - **Concept**: 단순 키워드 검색의 한계를 극복하기 위해 Gemini가 관련 비즈니스 문맥을 파악하여 쿼리를 확장.
    - **Logic**: 사용자 키워드("AI") $\xrightarrow{Gemini}$ ["AI Global Market", "AI Investment Trends", "AI Regulation 2025"].
2. **Step 2: 뉴스 수집 및 필터링 (Data Acquisition)**
    - **Engine**: **Perplexity API** (검색+1차요약) 또는 **Google Custom Search API**.
    - **Filter**: 중복 URL 제거, 신뢰도가 낮은 커뮤니티성 소스 배제.
3. **Step 3: 본문 추출 (Content Extraction)**
    - **Tool**: *Jina Reader API* 또는 *Newspaper3k* (Python Lib).
    - **Goal**: 광고, 메뉴 등을 제거한 순수 기사 본문(Clean Text) 확보.
4. **Step 4: 전략 분석 (Curation Logic)**
    - **Core**: Gemini 1.5 Pro가 추출된 본문을 분석하여 단순 요약이 아닌 **"Strategy Implications" (전략적 시사점)** 도출.

### 4.2. Data Structure (JSON)

AI 분석 결과는 아래와 같은 표준화된 JSON 포맷으로 저장 및 활용됩니다.

```json
{
  "search_metadata": {
    "keyword": "AI",
    "timestamp": "2025-12-30T19:46:50Z"
  },
  "news_items": [
    {
      "NewsTitle": "글로벌 AI 반도체 시장 점유율 변화 분석",
      "Source": "Bloomberg",
      "PublishedDate": "2025-12-30",
      "Link": "https://www.bloomberg.com/news/...",
      "AI_Insight": {
        "Summary": "엔비디아가 차세대 칩셋을 통해 시장 지배력을 강화하고 있으며, 경쟁사와의 격차가 벌어지고 있음.",
        "Strategy_Implications": "국내 팹리스 기업들은 범용 AI 칩셋보다는 특정 산업(On-device AI) 특화 칩셋으로 니치마켓을 공략해야 함."
      }
    }
  ]
}
```

### 4.3. Curation Prompt (Gemini Instructions)

AI에게 부여되는 페르소나와 작업 지시문입니다.

- **Role**: 경영기획전략 전문가이자 기술 분석가.
- **Input**: 뉴스 기사 전문(Clean Text) + 사용자 관심 키워드.
- **Requirement**:
  1. **Implications**: 컨설턴트 시각에서 **비즈니스 기회(Opportunity), 위기(Threat), 6개월 내 예측(Prediction)** 포함.
  2. **Tone**: 전문 용어는 유지하되, 명확하고 간결한 비즈니스 리포트 톤(Korean).

### 4.4. UX Strategy (Native App)

- **Insight-First View**: 뉴스 리스트에서 제목보다 **'전략적 시사점'**을 최상단에 강조하여 노출 (3초 내 파악).
- **Search Engine**: 지능형 쿼리 변환을 통해 사용자가 '로봇'만 입력해도 '로봇 산업 규제' 등을 함께 검색.
- **Interactive Filter**: 키워드별 탭(Tab) 구성을 통해 관심 분야 독립 관리.

### 2.1. 데이터 흐름도

```mermaid
graph LR
    subgraph Sources
    A1[Email]
    A2[News/RSS]
    A3[SMS/MMS]
    A4[Economic Indicators]
    A5[KakaoTalk/IM (Noti)]
    end
    
    A1 & A2 & A3 & A4 & A5 -->|Sync| B(Ingestion Engine)
    B -->|Raw Text/Data| C{Gemini AI Brain}
    C -->|Analysis & Insight| D[Local Intelligence DB]
    D -->|Context Check| E(Action Orchestrator)
    E -->|Write| F[Calendar / Notion]
    E -->|Notify| G[Smart Notification]
    E -->|Display| H[Dashboard UI]
```

---

## 3. 주요 기능 및 상세 작동 원리 (Detailed Functional Logic)

### 3.1. 스마트 인박스 (Intelligent Mail Pipeline)

단순 수신이 아닌 **'분류 및 처리 자동화'**에 초점을 맞춥니다.

- **작동 원리**:
    1. **Trigger**: 새로운 메일 수신 (백그라운드 WorkManager 15분 주기).
    2. **Process (AI)**: Gemini에게 `Sender`, `Subject`, `Body` 전송.
        - *Prompt 전략*: "이 메일이 [긴급/프로젝트/뉴스레터] 중 어디에 속하는지, 그리고 3줄 요약과 추천 행동(캘린더 등록 등)을 JSON으로 반환하라."
    3. **Action**:
        - **Red Flag (긴급)**: 중요도가 `CRITICAL`이면 즉시 푸시 알림 발생. (일반 메일은 무음 처리)
        - **Auto-Archiving**: `SPAM`이나 단순 `PROMOTION`은 사용자에게 알리지 않고 `Archived` DB로 직행.
        - **Task Extraction**: "다음 주 월요일 2시 미팅" 문구 감지 시 `Draft Event` 객체 생성.

### 3.2. 마켓 센싱 & 리서치 (Context-Aware Market Sensing)

단순 뉴스 키워드 검색이 아닌, **'내 업무와의 연관성'**을 분석합니다.

- **작동 원리**:
    1. **Context Injection**: 사용자의 현재 진행 프로젝트 키워드(예: "AI 헬스케어", "금융 규제")를 시스템 프롬프트(System Prompt)에 상시 주입.
    2. **Filter & Insight**: RSS/News API로 수집된 수백 개의 기사 중, 위 컨텍스트와 일치하는 상위 5%만 선별.
    3. **Value-Add**: Gemini가 "이 뉴스가 현재 진행 중인 A 프로젝트의 경쟁사 동향과 관련이 있습니다"라는 **'인사이트 코멘트'**를 생성하여 뉴스 하단에 부착.

### 3.3. 모바일 커뮤니케이션 & 시장 지표 (SMS & Market Data)

**확장된 수집 범위를 통해 개인화된 비즈니스 맥락을 완성합니다.**

- **SMS/MMS 인텔리전스 (Native Sync)**:
  - **Direct Read**: 안드로이드 `Telephony` Provider를 통해 기기에 저장된 문자 내역을 직접 동기화.
  - **금융 결제 문자**: 카드 결제 문자를 인식하여 지출 내역 자동 정리.
  - **인증/알림 문자**: 택배, 예약 확정 문자를 인식하여 캘린더 일정으로 자동 변환.
- **메신저 & 이메일 (Notification Sync)**:
  - **기술적 접근**: Google/Samsung 정책상 3rd Party 앱의 이메일/카톡 DB 직접 접근은 불가능합니다. 따라서 `NotificationListenerService`를 활용하여 **실시간 수신된 알림 메시지**를 캡처하여 DB에 저장합니다.
  - **지원 앱**: KakaoTalk, Slack, Telegram, Line, **Gmail**, Samsung Email.
  - **작동 방식**: 앱이 실행 중이지 않아도 알림이 오면 자동으로 내부 DB(`intelligence_items`)에 저장되고 AI 분석 대기 상태가 됩니다.
- **글로벌 경제 지표 (Economic Indicators)**:
  - **Key Metrics**: CD금리, 미국 10년물 국채, 한국 국고채 10년물, 환율 등 핵심 지표 실시간 추적.
  - **Trend Alert**: 단순 수치 나열이 아닌, "전일 대비 0.5% 급등하여 대출 이자 부담 증가 예상" 식의 **인사이트 코멘트** 생성.

### 3.4. 업무 오케스트레이션 (Task Orchestration)

모든 정보(메일, 뉴스, 문자, 경제지표)가 하나의 **'인사이트 카드'**로 통합됩니다.

- **작동 원리**:
  - **Unified Data Model**: 내부 DB에서 메일(`Email`)과 뉴스(`Insight`), 일정(`Event`)은 모두 `Item`이라는 상위 개념으로 연결됩니다.
  - **Smart Link**: 메일 본문에서 발견된 일정은 클릭 한 번으로 네이티브 캘린더에 등록되며, 원본 메일 링크가 캘린더 메모에 자동 첨부됩니다.
  - **Morning Briefing**: 매일 아침 8시, 밤새 수신된 '중요 메일'과 '핵심 뉴스', '오늘의 일정'을 통합 브리핑 카드로 생성하여 최상단에 노출.

---

## 4. 기술 스택 및 데이터베이스 설계 (Technical Specs)

### 4.1. Tech Stack

- **Mobile Framework**: Android (Kotlin, Jetpack Compose) - *향후 iOS 확장 고려한 Clean Architecture 적용*
- **AI Engine**: Gemini 1.5 Pro (via Google Generative AI SDK)
- **Local DB**: Room Database (SQLite) - *오프라인 우선(Offline-First) 설계*
- **Background Task**: WorkManager (주기적 동기화 및 무거운 AI 처리 작업 수행)
- **Connectivity**: Retrofit + OkHttp (Gmail API, News API)

### 4.2. Database Schema (Room)

#### Table: `intelligence_items` (통합 관리 테이블)

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | UUID | 고유 식별자 |
| `source_type` | ENUM | EMAIL, NEWS, SMS, IM, ECONOMY |
| `raw_content` | TEXT | 원본 데이터 (메일 본문, 문자 내용, 지표 수치 등) |
| `summary` | TEXT | AI가 생성한 인사이트/요약 |
| `captured_at` | DATETIME | 데이터 수신/발생 시각 |
| `priority` | INT | 1(Low) ~ 5(Critical) |
| `tags` | LIST | #Finance, #ProjectA, #Alert 등 |
| `linked_action`| JSON | { type: "CALENDAR", status: "PENDING" } |

---

## 5. UI/UX 전략 (Interactive Design)

### 5.1. 네비게이션 구조 (Tab Architecture)

앱은 5개의 핵심 탭으로 구성되어 정보의 유형별 접근성을 보장합니다.

1. **Market (뉴스/경제지표)**:
    - 사용자 설정 키워드 기반 뉴스 피드.
    - 주요 경제 지표(금리, 환율) 대시보드.
2. **Email (지능형 메일함)**:
    - 통합 인박스 (여러 계정 합산).
    - AI 중요도 태그(Critical, Project 등) 필터 뷰.
3. **SMS (문자/결제)**:
    - 금융/결제 내역 리포트 뷰.
    - 택배/인증 문자 모아보기.
4. **Messenger (카카오톡/IM)**:
    - 업무 키워드로 필터링된 알림 메시지 아카이브.
5. **Settings (설정 및 제어)**:
    - 계정 연결, 필터링 룰, 알림 제어.

### 5.2. 설정 및 필터링 시스템 (Granular Control)

사용자가 AI의 판단 기준을 직접 미세 조정할 수 있는 강력한 설정창을 제공합니다.

- **계정 관리**: 복수의 이메일 계정 (Gmail, Outlook) 등록 및 관리.
- **키워드 모니터링**:
  - "관심 키워드 등록" (예: `반도체`, `A프로젝트`). -> 뉴스/경제 탭에 반영.
- **필터링 및 분류 규칙 (Rule-based Engine)**:
  - **Text Matching**: "특정 단어(`광고`, `이벤트`)가 포함되면 -> `Spam`으로 분류".
  - **Priority Setting**: "특정 발신자(`Boss`) or 단어(`긴급`) -> `Critical`로 격상".
- **알림(Notification) 제어**:
  - 중요도별 진동/소리 설정 (Critical만 소리, 나머지는 무음 등).

### 5.3. 보안 및 키 관리 (Security & Key Management)

- **API Key 보안 계층**:
    1. **빌드 타임**: `local.properties`에 정의된 `GEMINI_API_KEY`를 `BuildConfig`를 통해 주입하여 기본값으로 사용 (소스 코드 미포함).
    2. **런타임**: 사용자가 앱 설정에서 직접 입력한 키는 `EncryptedSharedPreferences` (AES-256 GCM)를 통해 기기 내 암호화 영역에 안전하게 저장.
- **권한 보안**:
  - **Notification Listener**: 메시지 캡처를 위해 시스템 권한을 사용하며, 금융 데이터 및 인증 번호는 AI 분석 전 로컬 필터링을 통해 즉시 마스킹 처리.

---

## 6. 개발 로드맵 (Development Phases)

1. **Phase 2 (Mail Engine)**: Gmail API 연동, 메일 수신 및 AI 분류/요약 구현
2. **Phase 3 (Dashboard)**: 우선순위 기반 카드 UI 및 메인 대시보드 개발
3. **Phase 4 (Expansion)**: 뉴스/RSS 연동 및 캘린더/Notion 내보내기 기능 구현
