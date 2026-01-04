# Backend-Driven News Intelligence Architecture Proposal

(Using High-Frequency Free Tier & Serverless Strategy)

## 1. Problem Diagnosis

Currently, the `Make` app performs **Client-Side Processing**:

- **Mechanism**: The user's phone fetches RSS -> Sends to Gemini individually -> Parses response.
- **Critical Issues**:
  1. **Rate Limit Bottlesnecks**: 20 users launching the app simultaneously = 20x API load (Token/RPM limits hit instantly).
  2. **Slow UX**: Users must wait ~90 seconds for analysis to complete.
  3. **Data Redundancy**: If 100 users read the same "Fed Rate Hike" news, the AI analyzes it 100 times needlessly.

## 2. Proposed Solution: Serverless Aggregation Architecture

**Core Concept**: "Analyze Once, Distribute to Many."
Instead of the app doing the work, a free cloud function runs periodically (e.g., every 30 mins), analyzes news **ONCE**, saves the result to a database (Firebase/Google Sheets), and the app simply **READS** the finished data.

### Architecture Diagram

```mermaid
graph TD
    A[Cloud Scheduler\n(Every 30 min)] -->|Trigger| B[Cloud Function\n(Python/Node.js)]
    B -->|1. Fetch| C[Global RSS Feeds\n(WSJ, Reuters, etc)]
    B -->|2. Analyze| D[Gemini 1.5 Flash API]
    D -->|3. JSON Result| B
    B -->|4. Save| E[Firebase Firestore / Realtime DB]
    F[App User 1] -->|Read| E
    G[App User 2] -->|Read| E
    H[App User 3] -->|Read| E
```

## 3. Technology Stack (Free Tier Optimized)

| Component | Technology | Free Tier Limits | Why? |
|-----------|------------|------------------|------|
| **Compute** | **Google Cloud Functions (2nd Gen)** or **Github Actions** | 2M invokes/month (Free) | Perfect for running a script every 30 mins. |
| **Database** | **Firebase Firestore** | 50k reads, 20k writes/day | More than enough for storing ~100 daily news items. Faster than Sheets. |
| **AI API** | **Gemini 1.5 Flash (via Server)** | 60 RPM (Pay-as-you-go) | Server-side keys are easier to manage and secure. |
| **App** | **Android (Compose)** | N/A | Just needs to read from Firestore (Lightweight). |

## 4. Workflows

### A. The "Crawler" (Server-Side)

1. **Trigger**: Runs every 15 or 30 minutes via Cloud Scheduler.
2. **Fetch**: Pulls top 50 articles from the defined RSS list.
3. **Filter**: Checks DB to see if `link` already exists (Deduplication).
4. **Analyze**: Sends *only new* articles to Gemini.
   - Batching is easier here (can send larger contexts).
5. **Write**: Saves `SmartNewsItem` JSON objects to Firestore `news` collection.
6. **Cleanup**: Deletes articles older than 48 hours to save DB space.

### B. The "Reader" (Android App)

1. **Launch**: App opens.
2. **Sync**: Subscribes to Firestore `news` collection (Real-time updates).
3. **Display**: Shows pre-analyzed, high-quality news **INSTANTLY** (0 wait time).

## 5. Detailed Implementation Design

### 5.1 Firestore Data Model

**Collection: `news_articles`**
Document ID: Auto-generated or Hash of URL

```json
{
  "id": "hash_12345",
  "title": "Fed Hikes Rates by 0.25%",
  "source": "WSJ",
  "sourceType": "MEDIA",
  "url": "https://wsj.com/...",
  "publishedAt": "2024-05-20T10:00:00Z",
  "analysis": {
    "summary": "Fed raised rates...",
    "koreanTranslation": "연준이 금리를 0.25% 인상했습니다...",
    "insight": "Short-term market volatility expected...",
    "sentiment": "NEGATIVE",
    "impactScore": 8,
    "tags": ["Fed", "Interest Rates", "USD"]
  },
  "createdAt": "2024-05-20T10:05:00Z" // Server timestamp
}
```

### 5.2 Python Crawler Script (Draft Logic)

```python
import firebase_admin
from firebase_admin import firestore
import feedparser
import google.generativeai as genai

# 1. Setup
db = firestore.client()
model = genai.GenerativeModel('gemini-1.5-flash-latest')

def job():
    # 2. Fetch RSS
    feeds = ["url1", "url2"]
    all_articles = []
    for url in feeds:
        feed = feedparser.parse(url)
        all_articles.extend(feed.entries)

    # 3. Deduplicate
    new_articles = []
    for article in all_articles:
        if not db.collection('news_articles').where('url', '==', article.link).get():
            new_articles.append(article)

    # 4. Batch Analyze (Gemini)
    if new_articles:
        prompt = create_prompt(new_articles)
        response = model.generate_content(prompt)
        analyzed_data = parse_json(response.text)

        # 5. Save to Firestore
        for item in analyzed_data:
            db.collection('news_articles').add(item)
            
    # 6. Cleanup Old Data
    # (Delete items where createdAt < now - 48h)
```

### 5.3 Android Client Update Plan

1. **Add Dependencies**:

    ```kotlin
    implementation("com.google.firebase:firebase-firestore-ktx")
    ```

2. **Repository Layer**:
    Create `NewsRepository` that returns `Flow<List<SmartNewsItem>>`.

    ```kotlin
    fun getNewsStream(): Flow<List<SmartNewsItem>> = callbackFlow {
        val listener = firestore.collection("news_articles")
            .orderBy("publishedAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, _ ->
                val items = snapshot?.toObjects(SmartNewsItem::class.java)
                trySend(items)
            }
        awaitClose { listener.remove() }
    }
    ```

3. **UI Layer**:
    Collect the flow in `NewsViewModel` and update `NewsScreen`.

## 6. Recommendations

**Transitioning to this architecture effectively solves the "Usage Limit" and "Loading Time" issues permanently.** It transforms the app from a "Tool" (that works on demand) to a "Platform" (that serves curated data).

---
**Next Steps:**

1. **Approval**: Confirm if this detailed design meets your expectations.
2. **Implementation**: I can generate the actual Python script for the Cloud Function and the Kotlin code for the Android app.
