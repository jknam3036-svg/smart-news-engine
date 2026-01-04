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
# Random module removed to prevent ANY fake data generation

# --- Configuration ---
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# [REAL DATA ONLY] Premium RSS Feeds (NEWS_SOURCES.md 기준)
RSS_FEEDS = {
    # Tier-1 Global Financial Media
    "WSJ_Markets": "https://feeds.a.dj.com/rss/RSSMarketsMain.xml",
    "CNBC_Economy": "https://www.cnbc.com/id/20910258/device/rss/rss.html",
    "CNBC_Tech": "https://www.cnbc.com/id/19854910/device/rss/rss.html",
    "MarketWatch": "https://www.marketwatch.com/rss/topstories",
    "Reuters_Business": "https://www.reutersagency.com/feed/?taxonomy=best-topics&post_type=best",
    "Investing_News": "https://www.investing.com/rss/news.rss",
    "CoinDesk": "https://www.coindesk.com/arc/outboundfeeds/rss/",
    "TechCrunch": "https://techcrunch.com/feed/",
    
    # Google News (Korean Market Coverage)
    "Google_Korea_Economy": "https://news.google.com/rss/search?q=%EA%B2%BD%EC%A0%9C+when:1d&hl=ko&gl=KR&ceid=KR:ko",
    "Google_US_Economy": "https://news.google.com/rss/search?q=US+Economy+when:1d&hl=en-US&gl=US&ceid=US:en",
    "Google_Global_Markets": "https://news.google.com/rss/search?q=Global+Markets+when:1d&hl=en-US&gl=US&ceid=US:en"
}

def load_local_properties():
    """Helper to read local.properties from Android project root"""
    props = {}
    try:
        # Assuming script is run from project root or news_crawler dir
        paths = ["local.properties", "../local.properties", "../../local.properties"]
        for p in paths:
            if os.path.exists(p):
                with open(p, 'r', encoding='utf-8') as f:
                    for line in f:
                        if "=" in line and not line.strip().startswith("#"):
                            key, val = line.strip().split("=", 1)
                            props[key.strip()] = val.strip()
                break
    except Exception:
        pass
    return props

def initialize_services():
    # 1. Try Environment Variable (GitHub Actions)
    cred_json = os.environ.get('FIREBASE_CREDENTIALS')
    api_key = os.environ.get('GEMINI_API_KEY')
    
    logger.info(f"🔍 Checking GEMINI_API_KEY from environment: {'Found' if api_key else 'Not found'}")
    if api_key:
        logger.info(f"   API Key (first 10 chars): {api_key[:10]}...")
    
    # 2. If missing, Try local.properties (Local Run)
    if not api_key:
        logger.info("🔍 Trying local.properties...")
        props = load_local_properties()
        # Check common key names in local.properties
        api_key = props.get('geminiKey') or props.get('GEMINI_API_KEY')
        if api_key:
            logger.info(f"   ✅ Found in local.properties (first 10 chars): {api_key[:10]}...")
        else:
            logger.warning("   ❌ Not found in local.properties")

    # Firebase Setup
    if cred_json:
        # GitHub Actions context
        cred = credentials.Certificate(json.loads(cred_json))
        try:
            firebase_admin.get_app()
        except ValueError:
            firebase_admin.initialize_app(cred)
    else:
        # Local context - Look for file
        try:
            # Check common locations
            key_paths = ["serviceAccountKey.json", "news_crawler/serviceAccountKey.json"]
            found_path = None
            for p in key_paths:
                if os.path.exists(p):
                    found_path = p
                    break
            
            if found_path:
                cred = credentials.Certificate(found_path)
                try:
                    firebase_admin.get_app()
                except ValueError:
                    firebase_admin.initialize_app(cred)
            else:
                logger.warning("⚠️ serviceAccountKey.json not found in root or news_crawler/")
                # Raise error to trigger Mock DB in main()
                raise FileNotFoundError("Firebase Credential File Missing")
        except Exception as e:
            raise e # Propagate to main() for Mock DB fallback

    # Gemini Setup
    model = None
    if api_key:
        try:
            genai.configure(api_key=api_key)
            # v1beta에서 모델을 찾지 못하는 문제 해결을 위해 models/ 접두사 사용 시도
            try:
                model = genai.GenerativeModel('gemini-1.5-flash')
                logger.info("✅ Gemini API configured successfully (gemini-1.5-flash)")
            except Exception:
                model = genai.GenerativeModel('models/gemini-1.5-flash')
                logger.info("✅ Gemini API configured successfully (models/gemini-1.5-flash)")
        except Exception as e:
            logger.error(f"Gemini configuration failed: {e}")
            model = None
    else:
        logger.warning("⚠️ GEMINI_API_KEY not found. AI analysis will be skipped.")
    
    return firestore.client(), model

def fetch_feeds():
    """Fetch REAL RSS feeds."""
    articles = []
    for source, url in RSS_FEEDS.items():
        try:
            feed = feedparser.parse(url)
            if not feed.entries:
                logger.warning(f"No entries found for {source}")
                continue
                
            for entry in feed.entries[:10]: # Process top 10 real items
                # Validate essential fields
                if not hasattr(entry, 'link') or not hasattr(entry, 'title'):
                    continue
                    
                articles.append({
                    "id": hashlib.md5(entry.link.encode()).hexdigest(),
                    "title": entry.title,
                    "link": entry.link,
                    "published": getattr(entry, 'published', str(datetime.now())),
                    "source": source,
                    "full_content": getattr(entry, 'description', '')
                })
        except Exception as e:
            logger.error(f"Feed error {source}: {e}")
    return articles

def filter_new_articles(db, articles):
    if not articles: return []
    new_items = []
    for art in articles:
        # Check if ID exists in Firestore
        doc_ref = db.collection('investment_insights').document(art['id'])
        if not doc_ref.get().exists:
            new_items.append(art)
    return new_items

def analyze_batch(model, articles):
    results = []
    if not articles: return []
    
    prompt = """
    Analyze these REAL economic news articles.
    Return a LIST of JSON objects.
    Structure:
    {
      "item_index": <int>,
      "korean_title": "<Translated Title>",
      "korean_body": "<Translated Summary>",
      "impact_score": <1-10>,
      "market_sentiment": "<POSITIVE|NEGATIVE|NEUTRAL>",
      "actionable_insight": "<One sentence advice>",
      "related_assets": ["Asset1", "Asset2"]
    }
    Articles:
    """
    for idx, art in enumerate(articles):
        prompt += f"\n[{idx}] Title: {art['title']}\n"

    try:
        response = model.generate_content(prompt)
        cleaned = response.text.replace("```json", "").replace("```", "").strip()
        data = json.loads(cleaned)
        if isinstance(data, list):
            return data
        else:
            return []
    except Exception as e:
        logger.error(f"Analysis Failed: {e}")
        return []

def fetch_and_save_calendar(db):
    """
    Crawls Trading Economics (Korean) for Real Economic Calendar.
    Target: https://ko.tradingeconomics.com/calendar
    This site is generally less aggressive with 403 blocks than Investing.com.
    """
    logger.info("📅 Fetching Real Economic Calendar from ko.tradingeconomics.com...")
    
    url = "https://ko.tradingeconomics.com/calendar"
    
    # Cookie is often needed for TradingEconomics to show data properly
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
        'Referer': 'https://ko.tradingeconomics.com/',
        'Accept-Language': 'ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7'
    }
    
    try:
        # Use session to handle cookies automatically
        session = requests.Session()
        resp = session.get(url, headers=headers, timeout=20)
        
        if resp.status_code != 200:
            logger.error(f"HTTP Error: {resp.status_code}")
            return

        soup = BeautifulSoup(resp.content, 'html.parser')
        
        # TradingEconomics Calendar Table
        table = soup.select_one('#calendar')
        if not table:
            logger.warning("⚠️ Table #calendar not found. Site structure might be different.")
            # Debug: Check title
            logger.info(f"Page Title: {soup.title.string if soup.title else 'No Title'}")
            return
            
        # Rows usually have data-id
        items = table.find_all('tr', attrs={'data-id': True})
        
        # If specific selector fails, try generic fallback
        if not items:
            logger.warning("⚠️ No tr[data-id] found. Trying all rows with valid content.")
            all_rows = table.find_all('tr')
            items = [r for r in all_rows if len(r.find_all('td')) > 3]

        batch = db.batch()
        count = 0
        today_str = datetime.now().strftime("%Y-%m-%d")

        for row in items:
            try:
                # 1. Time
                # TE structure: Date header row -> Event rows
                # Event row: <td class="day"...> or just <td>Time</td>
                
                cols = row.find_all('td')
                if not cols: continue

                # Extract ID if available
                te_id = row.get('data-id', '')

                # Time is usually in the first column
                time_text = cols[0].get_text(strip=True)
                # If time_text looks like date (e.g. "2024..."), skip or handle
                if "202" in time_text and "월" in time_text: continue 
                
                time_val = time_text[:5] # "10:30"
                
                # 2. Country (Col 1 or 2 usually)
                country_val = "Global"
                country_raw = row.get('data-country', '')
                if country_raw:
                    country_val = country_raw.title() # south korea -> South Korea
                else:
                    # Try finding in columns
                    if len(cols) > 1:
                        country_val = cols[1].get_text(strip=True).title()
                
                # 3. Title (Event Name) 
                title_val = ""
                event_a = row.select_one('a')
                if event_a:
                    title_val = event_a.get_text(strip=True)
                else:
                    if len(cols) > 2:
                        title_val = cols[2].get_text(strip=True)

                # 4. Importance (Stars)
                imp = 1
                try:
                    imp = int(row.get('data-importance', '1'))
                except:
                    pass
                
                # 5. Data (Actual, Previous, Consensus)
                # Indices vary, but typically:
                # Actual: data-actual attribute or specific col class
                act = row.get('data-actual', '')
                prev = row.get('data-previous', '')
                fore = row.get('data-forecast', '')

                # Fallback to column text if attributes empty
                # Usually Cols: Time, Country, Event, Actual, Previous, Consensus, Forecast
                if not act and len(cols) >= 4:
                     # This is risky without strict indices, but let's trust attributes first
                     pass

                if not title_val: continue

                # Generate ID
                event_id = hashlib.md5(f"{today_str}-{title_val}-{time_val}".encode()).hexdigest()

                doc_ref = db.collection('economic_calendar').document(event_id)
                data = {
                    "id": event_id,
                    "date": today_str,
                    "time": time_val,
                    "country": country_val,
                    "title": title_val,
                    "importance": imp,
                    "actual": str(act),
                    "forecast": str(fore),
                    "previous": str(prev),
                    "updated_at": firestore.SERVER_TIMESTAMP
                }
                
                if "MockDB" in str(type(db)):
                    print(f"[📅 CALENDAR] {time_val} [{country_val}] {title_val} (Imp: {imp}) Act:{act}")
                else:
                    batch.set(doc_ref, data, merge=True)
                
                count += 1

            except Exception:
                continue

        if count > 0:
            if "MockDB" not in str(type(db)):
                batch.commit()
            logger.info(f"✅ [CALENDAR] Parsed {count} events from ko.tradingeconomics.com")
        else:
            logger.warning(f"⚠️ Table found but parsed 0 events. Table HTML snippet: {str(table)[:500]}")

    except Exception as e:
        logger.error(f"Calendar Crawler Failed: {e}")

def main():
    logger.info("🚀 Starting PURE REAL DATA Engine (Local Test Mode)...")
    
    # --- CREDENTIAL CHECK ---
    db = None
    model = None
    try:
        db, model = initialize_services()
    except Exception:
        logger.warning("⚠️ Firebase/Gemini Auth Failed. Running in READ-ONLY mode (Mock DB).")
        # Pseudo-DB for testing
        class MockDB:
            def batch(self): return self
            def collection(self, _): return self
            def document(self, _): return self
            def set(self, ref, data, merge=False): 
                print(f"\n[💾 MOCK SAVE] {data.get('title', 'Unknown')} ({data.get('date', '')} {data.get('time', '')})")
            def commit(self): pass
            def get(self): 
                class MockDoc:
                    exists = False
                return MockDoc()
        db = MockDB()

    # 1. News Phase
    logger.info("--- Phase 1: Real News Fetching ---")
    all_articles = fetch_feeds()
    
    # Filter out already existing articles (if DB is real)
    new_articles = filter_new_articles(db, all_articles)
    logger.info(f"📰 Fetched {len(new_articles)} NEW articles to process.")

    BATCH_SIZE = 5
    for i in range(0, len(new_articles), BATCH_SIZE):
        batch_arts = new_articles[i:i+BATCH_SIZE]
        
        # Try AI Analysis if model exists
        ai_results = {} # Map ID -> Result
        if model:
            try:
                results_list = analyze_batch(model, batch_arts)
                for res in results_list:
                    idx = res.get('item_index')
                    if idx is not None and idx < len(batch_arts):
                        art_id = batch_arts[idx]['id']
                        ai_results[art_id] = res
            except Exception as e:
                logger.error(f"Batch Analysis Error: {e}")
        else:
             logger.warning("⚠️ Skipping AI Analysis (No API Key). Using metadata only.")

        # Save EACH article to DB (AI or Fallback)
        for art in batch_arts:
            art_id = art['id']
            ai_data = ai_results.get(art_id)
            
            # Prepare Data
            if ai_data:
                korean_title = ai_data.get('korean_title', art['title'])
                korean_body = ai_data.get('korean_body', '번역 불가')
                impact = ai_data.get('impact_score', 5)
                sentiment = ai_data.get('market_sentiment', 'NEUTRAL')
                insight = ai_data.get('actionable_insight', '정보 없음')
                assets = ai_data.get('related_assets', [])
            else:
                # FALLBACK - Use RSS description/summary as body content
                # Even without AI, provide actual article content to users
                korean_title = art['title']  # Original English title
                
                # Use RSS description if available, otherwise provide helpful message
                rss_description = art.get('full_content', '').strip()
                if rss_description and len(rss_description) > 20:
                    korean_body = f"{rss_description}\n\n[AI 번역 대기 중 - 원문 기사입니다. 자세한 내용은 원문 링크를 확인하세요.]"
                else:
                    korean_body = f"[AI 분석 대기 중]\n\n이 기사는 {art['source']} 소스에서 수집되었습니다.\n제목: {art['title']}\n\n자세한 내용은 원문 링크를 확인하세요."
                
                impact = 5
                sentiment = 'NEUTRAL'
                insight = "AI 분석 대기 중 - Gemini API 키를 설정하면 한국어 번역 및 투자 인사이트를 제공합니다."
                assets = []

            doc_data = {
                "id": art_id,
                "meta_data": {
                    "source_name": art['source'],
                    "published_at": datetime.now(pytz.utc), # Force Freshness
                    "analyzed_at": datetime.now(pytz.utc)
                },
                "content": {
                    "original_title": art['title'],
                    "korean_title": korean_title,
                    "korean_body": korean_body,
                },
                "intelligence": {
                    "impact_score": impact,
                    "market_sentiment": sentiment,
                    "actionable_insight": insight,
                    "related_assets": assets
                }
            }
            
            # Save
            if "MockDB" in str(type(db)):
                print(f"[💾 NEWS SAVE] {korean_title} (Impact: {impact})")
            else:
                db.collection('investment_insights').document(art_id).set(doc_data)
        
        time.sleep(1)

    # 2. Calendar
    logger.info("--- Phase 2: Real Calendar Fetching ---")
    fetch_and_save_calendar(db)
    
    # 3. Economic Indicators (ECOS)
    logger.info("--- Phase 3: Economic Indicators Fetching ---")
    try:
        # Import ecos_crawler module
        import sys
        import os
        sys.path.insert(0, os.path.dirname(__file__))
        from ecos_crawler import fetch_ecos_data, INDICATORS, save_to_firestore
        
        indicators_data = []
        for indicator_config in INDICATORS:
            result = fetch_ecos_data(
                stat_code=indicator_config["stat_code"],
                item_code=indicator_config["item_code"],
                cycle=indicator_config["cycle"]
            )
            
            if result:
                value, change = result
                indicator_data = {
                    "id": indicator_config["id"],
                    "name": indicator_config["name"],
                    "value": value,
                    "change_rate": change,
                    "unit": indicator_config["unit"],
                    "type": indicator_config["type"],
                    "source": "한국은행",
                    "stat_code": indicator_config["stat_code"],
                    "item_code": indicator_config["item_code"],
                    "updated_at": firestore.SERVER_TIMESTAMP,
                    "captured_at": datetime.now(pytz.utc).isoformat()
                }
                indicators_data.append(indicator_data)
        
        if indicators_data:
            save_to_firestore(db, indicators_data)
            logger.info(f"✅ Collected {len(indicators_data)}/{len(INDICATORS)} economic indicators")
        else:
            logger.warning("⚠️ No economic indicators collected")
            
    except Exception as e:
        logger.error(f"Economic indicators collection failed: {e}")
    
    logger.info("Done.")

if __name__ == "__main__":
    main()
