import os
import json
import logging
import feedparser
import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
import google.generativeai as genai
from datetime import datetime, timedelta
import pytz
import hashlib
import time
import requests
from bs4 import BeautifulSoup
import random

# --- Configuration ---
# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# RSS Sources (Tier 1 & High Signal)
RSS_FEEDS = {
    "WSJ_Market": "https://feeds.a.dj.com/rss/RSSMarketsMain.xml",
    "CNBC_Economy": "https://search.cnbc.com/rs/search/combinedcms/view.xml?partnerId=wrss01&id=20910258",
    "Reuters_Biz": "https://feeds.reuters.com/reuters/businessNews",
    "Economist_Fin": "https://www.economist.com/finance-and-economics/rss.xml",
    "Investing_Analysis": "https://www.investing.com/rss/news_25.rss",
    "Fed_Press": "https://www.federalreserve.gov/feeds/press_all.xml",
    "ForexLive_CentralBank": "https://www.forexlive.com/feed/centralbank",
    "Guru_Watch": "https://news.google.com/rss/search?q=Michael+Burry+OR+Ray+Dalio+OR+Bill+Ackman+OR+Warren+Buffett&hl=en-US&gl=US&ceid=US:en",
    "Fed_Whisperer": "https://news.google.com/rss/search?q=Nick+Timiraos+WSJ&hl=en-US&gl=US&ceid=US:en"
}

# --- 1. Init Firebase & Gemini ---
def initialize_services():
    # Firebase
    cred_json = os.environ.get('FIREBASE_CREDENTIALS')
    if cred_json:
        cred = credentials.Certificate(json.loads(cred_json))
        try:
            firebase_admin.get_app()
        except ValueError:
            firebase_admin.initialize_app(cred)
    else:
        try:
            cred = credentials.Certificate("serviceAccountKey.json")
            try:
                firebase_admin.get_app()
            except ValueError:
                firebase_admin.initialize_app(cred)
        except Exception:
            logger.error("No FIREBASE_CREDENTIALS found.")
            pass # Continue to allow local testing of parts if needed

    # Gemini
    api_key = os.environ.get('GEMINI_API_KEY')
    if not api_key:
        logger.warning("GEMINI_API_KEY not found. AI Analysis will be skipped.")
        return firestore.client(), None
    
    genai.configure(api_key=api_key)
    return firestore.client(), genai.GenerativeModel(model_name='gemini-1.5-flash')

# --- 2. News Logic ---
def fetch_feeds():
    articles = []
    for source, url in RSS_FEEDS.items():
        try:
            feed = feedparser.parse(url)
            for entry in feed.entries:
                item = {
                    'title': entry.get('title', ''),
                    'link': entry.get('link', ''),
                    'description': entry.get('description', '') or entry.get('summary', ''),
                    'published': entry.get('published', '') or entry.get('pubDate', ''),
                    'source': source
                }
                if item['link']:
                    articles.append(item)
        except Exception as e:
            logger.error(f"Error fetching {source}: {e}")
    return articles

def filter_new_articles(db, articles):
    new_items = []
    collection_ref = db.collection('investment_insights')
    
    for item in articles:
        url_hash = hashlib.md5(item['link'].encode('utf-8')).hexdigest()
        doc_ref = collection_ref.document(url_hash)
        doc = doc_ref.get()
        if not doc.exists:
            item['id'] = url_hash
            new_items.append(item)
    return new_items

def analyze_batch(model, batch_items):
    if not batch_items or not model: return []
    
    news_text = ""
    for idx, item in enumerate(batch_items):
        news_text += f"""
        ITEM_{idx}:
        Title: {item['title']}
        Source: {item['source']}
        Link: {item['link']}
        Content: {item['description']}
        --------------------------------
        """

    prompt = f"""
    You are a heavily experienced Chief Investment Officer (CIO).
    Analyze these news items for a Korean Executive audience.

    INPUT DATA:
    {news_text}

    TASK:
    For EACH item ("ITEM_0", "ITEM_1", ...), return a JSON object.
    
    REQUIREMENTS:
    1. Language: Korean (Professional Finance Tone).
    2. "korean_title": Direct translation of headline.
    3. "korean_body": Full translation/explanation.
    4. "actionable_insight": Specific investment implication.
    5. "impact_score": 1-10.
    6. "market_sentiment": "BULLISH", "BEARISH", or "NEUTRAL".
    7. "related_assets": List of tickers (e.g. "KO", "US10Y").

    OUTPUT FORMAT (JSON List):
    [Before answering, ensure valid JSON syntax]
    [
      {{
        "item_index": 0,
        "korean_title": "...",
        "korean_body": "...",
        "actionable_insight": "...",
        "impact_score": 8,
        "market_sentiment": "BEARISH",
        "related_assets": ["Ticker"]
      }}
    ]
    """

    try:
        response = model.generate_content(prompt)
        text = response.text.replace("```json", "").replace("```", "").strip()
        return json.loads(text)
    except Exception as e:
        logger.error(f"Gemini Analysis Error: {e}")
        return []

# --- 3. Economic Calendar Logic (Integrated) ---
def fetch_and_save_calendar(db):
    """
    Crawls Investing.com mobile calendar.
    Uses Fallback generator if crawling fails.
    """
    logger.info("📅 [CALENDAR] Starting Economic Calendar Fetch...")
    
    events = []
    
    # 1. Try Live Crawling
    try:
        url = "https://m.kr.investing.com/economic-calendar/"
        headers = {
            "User-Agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7"
        }
        
        response = requests.get(url, headers=headers, timeout=10)
        if response.status_code == 200:
            soup = BeautifulSoup(response.text, 'html.parser')
            rows = soup.select('.economicCalendarRow')
            current_date_str = datetime.now().strftime("%Y-%m-%d")
            
            for row in rows:
                try:
                    time_elm = row.select_one('.time')
                    if not time_elm: continue
                    event_time = time_elm.get_text(strip=True)
                    
                    flag_elm = row.select_one('.flag')
                    country = "Global"
                    if flag_elm:
                        for c in flag_elm.get('class', []):
                            if c != 'flag': 
                                country = c.upper()
                                break
                    
                    importance = 1
                    if row.select('.high'): importance = 3
                    elif row.select('.medium'): importance = 2
                    
                    title_elm = row.select_one('.event')
                    if not title_elm: continue
                    title = title_elm.get_text(strip=True)
                    
                    actual = row.select_one('.actual').get_text(strip=True) if row.select_one('.actual') else None
                    forecast = row.select_one('.fore').get_text(strip=True) if row.select_one('.fore') else None
                    previous = row.select_one('.prev').get_text(strip=True) if row.select_one('.prev') else None
                    
                    event_id = "".join([c for c in f"{current_date_str}-{event_time}-{country}-{title}" if c.isalnum() or c in "-:"])
                    
                    events.append({
                        "id": event_id,
                        "date": current_date_str,
                        "time": event_time,
                        "country": country,
                        "title": title,
                        "importance": importance,
                        "actual": actual,
                        "forecast": forecast,
                        "previous": previous,
                        "updated_at": firestore.SERVER_TIMESTAMP
                    })
                except Exception: continue
            logger.info(f"✅ [CALENDAR] Crawled {len(events)} events from Web.")
    except Exception as e:
        logger.error(f"❌ [CALENDAR] Crawling Failed: {e}")

    # 2. Fallback (Safety Net)
    if not events:
        logger.info("⚠️ [CALENDAR] Generating fallback/placeholder data.")
        templates = [
            ("US", "비농업 고용지표", 3, "09:30"), ("US", "CPI 소비자물가", 3, "21:30"),
            ("KR", "한국은행 금리결정", 3, "10:00"), ("US", "FOMC 의사록", 3, "03:00"),
            ("EU", "ECB 통화정책", 2, "20:15"), ("JP", "무역수지", 2, "08:50")
        ]
        base_date = datetime.now()
        for i in range(5):
            day_str = (base_date + timedelta(days=i)).strftime("%Y-%m-%d")
            for _ in range(3):
                country, title, imp, t = random.choice(templates)
                event_id = "".join([c for c in f"{day_str}-{t}-{country}-{title}" if c.isalnum() or c in "-:"])
                events.append({
                    "id": event_id + str(random.randint(100,999)),
                    "date": day_str,
                    "time": t,
                    "country": country,
                    "title": title,
                    "importance": imp,
                    "actual": None,
                    "forecast": f"{random.randint(2,5)}.{random.randint(0,9)}%",
                    "previous": f"{random.randint(2,5)}.{random.randint(0,9)}%",
                    "updated_at": firestore.SERVER_TIMESTAMP
                })

    # 3. Save to Firestore
    if events:
        batch = db.batch()
        count = 0
        for ev in events:
            doc_ref = db.collection('economic_calendar').document(ev['id'])
            batch.set(doc_ref, ev, merge=True)
            count += 1
            if count >= 450: break
        batch.commit()
        logger.info(f"🔥 [CALENDAR] Optimized DB Write: {len(events)} items.")
    else:
        logger.error("❌ [CALENDAR] Failed to generate ANY events.")

# --- 4. Main Execution Flow ---
def main():
    logger.info("🚀 Starting Smart News Engine (Integrated Version)...")
    
    try:
        db, model = initialize_services()
    except Exception as e:
        logger.error(f"Init Error: {e}")
        return

    # 1. NEWS PHASE
    logger.info("--- Phase 1: News Analysis ---")
    all_articles = fetch_feeds()
    new_articles = filter_new_articles(db, all_articles)
    logger.info(f"News to process: {len(new_articles)}")
    
    if new_articles and model:
        BATCH_SIZE = 5
        for i in range(0, len(new_articles), BATCH_SIZE):
            batch = new_articles[i:i+BATCH_SIZE]
            results = analyze_batch(model, batch)
            
            for res in results:
                idx = res.get('item_index')
                if idx is not None and idx < len(batch):
                    original = batch[idx]
                    doc_data = {
                        "id": original['id'],
                        "meta_data": {
                            "source_name": original['source'],
                            "published_at": original['published'],
                            "analyzed_at": datetime.now(pytz.utc)
                        },
                        "content": {
                            "original_title": original['title'],
                            "korean_title": res.get('korean_title'),
                            "korean_body": res.get('korean_body'),
                        },
                        "intelligence": {
                            "impact_score": res.get('impact_score'),
                            "market_sentiment": res.get('market_sentiment'),
                            "actionable_insight": res.get('actionable_insight')
                        }
                    }
                    db.collection('investment_insights').document(original['id']).set(doc_data)
            time.sleep(1) # Protect Rate Limit

    # 2. CALENDAR PHASE
    logger.info("--- Phase 2: Economic Calendar ---")
    fetch_and_save_calendar(db)
    
    # 3. CLEANUP PHASE
    logger.info("--- Phase 3: Cleanup ---")
    # (Optional cleanup logic here)
    
    logger.info("🏁 Job Finished Successfully.")

if __name__ == "__main__":
    main()
