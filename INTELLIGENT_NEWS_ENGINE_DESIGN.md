# Intelligent Global Economic News Engine (Version 2.0)

**"The CFO's Morning Briefing"**

## 1. Executive Summary

This project aims to build an **Enterprise-Grade AI News Engine** that autonomously monitors global markets, filters noise, and delivers high-value, actionable investment intelligence to Korean executives(CFOs/CEOs).
Moving beyond simple translation, the system utilizes **Gemini 1.5 Flash** as a virtual Chief Investment Officer to analyze correlations, risks, and market impacts in real-time.

---

## 2. System Architecture (Serverless & Autonomous)

| Component | Technology | Role | Status |
| :--- | :--- | :--- | :--- |
| **Collector (Brain)** | **GitHub Actions (Python)** | Runs every 30 mins. Fetches raw data from premium sources. | 🔄 Intelligent |
| **Analyst (AI)** | **Gemini 1.5 Flash** | Reading comprehension, investment analysis, insight extraction. | 🧠 Cognitive |
| **Storage (Memory)** | **Firebase Firestore** | NoSQL Database. Stores processed insights & history. | 💾 Persistent |
| **Distribution (UI)** | **Android (Jetpack Compose)** | Real-time dashboard. "Zero-loading" experience. | 📱 Interactive |

---

## 3. Core Intelligence Modules (The "Brain")

### Module A: The Gatekeeper (Smart Filtering)

The system will not just fetch everything. It applies strict filters to screen out noise.

- **Source Authority Tiering**:
  - **Tier 1 (Critical)**: WSJ, Bloomberg, Reuters, Financial Times.
  - **Tier 2 (Official)**: Fed Statements, BOK, Bureau of Labor Statistics.
  - **Tier 3 (Signals)**: Recognized Guru Blogs (Dalio), High-Signal Newsletters.
- **Deduplication Logic**: Uses fuzzy matching (Title Similarity > 85%) to group similar articles into one cluster, analyzing them together for a holistic view.

### Module B: The Virtual CIO (AI Prompt Strategy)

The AI is instructed not to "summarize" but to **"Analyze for Decision Making."**
**Prompt Persona**: "You are a veteran Chief Investment Officer on Wall Street."
**Output Requirements**:

1. **Korean Executive Summary**: 3 bullet points, professional tone.
2. **Market Impact Analysis**:
    - **Asset Impact**: specific predictions (e.g., "Yields ↑ -> Tech Stocks ↓").
    - **Risk Assessment**: Probability of downside.
3. **Sentiment Score (0-100)**: Quantitative fear/greed metric.
4. **Keywords**: Ticker symbols (AAPL, TSLA), Commodities (Gold, Oil).

---

## 4. Data Schema Design (Firestore)

**Collection: `investment_insights`**

```json
{
  "id": "unique_hash",
  "cluster_id": "if_merged",
  "meta_data": {
    "source_name": "Wall Street Journal",
    "source_type": "TIER_1_MEDIA",
    "original_url": "https://...",
    "published_at": "timestamp"
  },
  "content": {
    "original_title": "Fed Signals Pause",
    "korean_title": "연준, 금리 인상 중단 시사... 시장 안도 랠리 예상",
    "korean_body": "Full professional translation...",
    "key_takeaways": ["Point 1", "Point 2", "Point 3"]
  },
  "intelligence": {
    "impact_score": 8.5,  // 1-10 (High Impact)
    "market_sentiment": "BULLISH",
    "related_assets": ["$SPX", "$TLT", "Gold"],
    "actionable_insight": "Bond yields expected to drop. Good entry point for long-duration treasury bonds."
  },
  "status": "PUBLISHED"
}
```

---

## 5. Implementation Roadmap

### Phase 1: The Engine Room (Backend Setup) ✅ **NEXT STEP**

- [ ] **Firebase Setup**: Create Project `Make-Intelligence`, Setup Firestore & Authentication.
- [ ] **GitHub Repo**: Create private repository for the crawler.
- [ ] **Python Skeleton**: Setup `main.py` with `feedparser` and `firebase-admin`.

### Phase 2: The Intelligence Core (AI Logic)

- [ ] **Prompt Engineering**: Refine the "CIO Persona" prompt for Gemini 1.5 Flash.
- [ ] **Batch Processor**: Implement logic to send 5-10 articles per request to maximize context window usage.
- [ ] **Automation**: Configure `.github/workflows/daily_briefing.yml` (CRON schedule: `*/30 * * * *`).

### Phase 3: The Dashboard (App Integration)

- [ ] **SDK Integration**: Add Firebase SDK to Android project.
- [ ] **Repository Swap**: Replace `RssFetcher` with `NewsRepository` (Firestore).
- [ ] **UI Update**: Create a premium "Feed" layout optimized for quick scanning.

### Phase 4: Advanced Features (CEO Level)

- [ ] **Push Notifications**: Trigger FCM push when `impact_score > 9.0` (Critical Events).
- [ ] **Search & Archive**: Enable searching historical insights.

---

## 6. Development Discipline

- **Code Quality**: All Python code will be modular and type-hinted.
- **Security**: API Keys managed via GitHub Secrets. No hardcoding.
- **Reliability**: Error logging to Firestore/Discord (for developer alerts).

**Status**: Ready for Phase 1.
