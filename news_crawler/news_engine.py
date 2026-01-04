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
from dateutil import parser as date_parser

# --- Configuration ---
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# [COLLECTION NAME] Must match NewsRepository.kt
COLLECTION_NAME = "investment_insights"  # 앱에서 조회하는 실제 컬렉션

# 2. Pure Korean Search Queries
RSS_FEEDS = {
    "US_Economy": "https://news.google.com/rss/search?q=%EB%AF%B8%EA%B5%AD%EA%B2%BD%EC%A0%9C+when:1d&hl=ko&gl=KR&ceid=KR:ko", # 미국경제
    "Global_Market": "https://news.google.com/rss/search?q=%EC%84%B8%EA%B3%84%EC%A6%9D%EC%8B%9C+when:1d&hl=ko&gl=KR&ceid=KR:ko", # 세계증시
    "KR_Economy": "https://news.google.com/rss/search?q=%ED%95%9C%EA%B5%AD%EA%B2%BD%EC%A0%9C+when:1d&hl=ko&gl=KR&ceid=KR:ko"    # 한국경제
}

def load_local_properties():
    props = {}
    try:
        paths = ["local.properties", "../local.properties", "../../local.properties"]
        for p in paths:
            if os.path.exists(p):
                with open(p, 'r', encoding='utf-8') as f:
                    for line in f:
                        if "=" in line and not line.strip().startswith("#"):
                            key, val = line.strip().split("=", 1)
                            props[key.strip()] = val.strip()
                break
    except Exception: pass
    return props

def initialize_services():
    cred_json = os.environ.get('FIREBASE_CREDENTIALS')
    api_key = os.environ.get('GEMINI_API_KEY')
    
    if not api_key:
        props = load_local_properties()
        api_key = props.get('geminiKey') or props.get('GEMINI_API_KEY')

    if cred_json:
        cred = credentials.Certificate(json.loads(cred_json))
        try: firebase_admin.get_app()
        except ValueError: firebase_admin.initialize_app(cred)
    else:
        try:
            key_paths = ["serviceAccountKey.json", "news_crawler/serviceAccountKey.json"]
            found = next((p for p in key_paths if os.path.exists(p)), None)
            if found:
                cred = credentials.Certificate(found)
                try: firebase_admin.get_app()
                except ValueError: firebase_admin.initialize_app(cred)
            else:
                raise FileNotFoundError("No serviceAccountKey.json")
        except Exception as e:
            raise e

    if api_key:
        try:
            genai.configure(api_key=api_key)
            return firestore.client(), genai.GenerativeModel('gemini-1.5-flash-latest')
        except: return firestore.client(), None
    else:
        return firestore.client(), None

def fetch_feeds():
    articles = []
    for source, url in RSS_FEEDS.items():
        try:
            feed = feedparser.parse(url)
            for entry in feed.entries[:10]:
                if not hasattr(entry, 'title'): continue
                
                pub_str = getattr(entry, 'published', str(datetime.now()))
                articles.append({
                    "id": hashlib.md5(entry.link.encode()).hexdigest(),
                    "title": entry.title,
                    "link": entry.link,
                    "published": pub_str,
                    "source": source
                })
        except Exception: pass
    return articles

def analyze_batch(model, articles):
    if not articles: return []
    prompt = "Analyze (Korean Output): " + "\n".join([f"[{i}] {a['title']}" for i, a in enumerate(articles)])
    prompt += "\nFormat: JSON List [{item_index, korean_title, korean_body, impact_score(1-10), market_sentiment, actionable_insight, related_assets}]"
    try:
        res = model.generate_content(prompt)
        return json.loads(res.text.replace("```json","").replace("```","").strip())
    except: return []

def fetch_and_save_calendar(db):
    try:
        logger.info("📅 Fetching Calendar (ko.tradingeconomics.com)...")
        headers = {'User-Agent': 'Mozilla/5.0', 'Accept-Language': 'ko-KR'}
        items = []
        try:
            resp = requests.get("https://ko.tradingeconomics.com/calendar", headers=headers, timeout=20)
            soup = BeautifulSoup(resp.content, 'html.parser')
            table = soup.select_one('#calendar')
            if table:
                items = table.find_all('tr', attrs={'data-id': True}) or [r for r in table.find_all('tr') if len(r.find_all('td')) > 3]
        except: pass

        if not items: return

        batch = db.batch()
        today_str = datetime.now().strftime("%Y-%m-%d")
        
        for row in items:
            try:
                cols = row.find_all('td')
                if not cols: continue
                
                time_val = cols[0].get_text(strip=True)[:5]
                country = row.get('data-country', '').title() or (cols[1].get_text(strip=True).title() if len(cols)>1 else 'Global')
                title = row.select_one('a').get_text(strip=True) if row.select_one('a') else (cols[2].get_text(strip=True) if len(cols)>2 else '')
                if not title: continue
                
                eid = hashlib.md5(f"{today_str}-{title}-{time_val}".encode()).hexdigest()
                batch.set(db.collection('economic_calendar').document(eid), {
                    "id": eid, "date": today_str, "time": time_val, "country": country, "title": title,
                    "importance": int(row.get('data-importance', '1') or 1),
                    "actual": row.get('data-actual', ''), "forecast": row.get('data-forecast', ''),
                    "previous": row.get('data-previous', ''), "updated_at": firestore.SERVER_TIMESTAMP
                }, merge=True)
            except: continue
        
        if "ConsoleOutputDB" not in str(type(db)): batch.commit()
    except: pass

def main():
    print("🚀 [BUILD VERSION 2026-01-02-FINAL: FORCE KOREAN + NEW COLLECTION]")
    
    db = None
    model = None
    try: db, model = initialize_services()
    except:
        print("⚠️ No Credentials -> Dry Run Mode")
        class ConsoleOutputDB:
            def batch(self): return self
            def collection(self, _): return self
            def document(self, _): return self
            def set(self, ref, data, merge=False): print(f"[DRY RUN] {data.get('content',{}).get('korean_title')}")
            def commit(self): pass
            def get(self): 
                class MockDoc:
                    exists = False
                return MockDoc()
        db = ConsoleOutputDB()

    # 1. Fetch
    news = fetch_feeds()
    print(f"📰 Fetched {len(news)} articles.")

    # 2. Analyze
    ai_results = {}
    if model and news:
        try:
            # Batch first 10 for speed
            batch = news[:10]
            for r in analyze_batch(model, batch):
                if 'item_index' in r: ai_results[r['item_index']] = r
        except Exception as e: print(f"AI Error: {e}")

    # 3. Save to V2 Collection
    for i, art in enumerate(news):
        try:
            pub_dt = date_parser.parse(art['published'])
        except: pub_dt = datetime.now(pytz.utc)
        
        res = ai_results.get(i)
        
        # FINAL FALLBACK STRINGS
        k_title = res.get('korean_title', art['title']) if res else art['title']
        k_body = res.get('korean_body', "요약 정보 없음") if res else "AI 분석 대기 중 (원문 확인 요망)"
        
        doc_data = {
            "id": art['id'],
            "meta_data": { 
                "source_name": art['source'], 
                "published_at": pub_dt, 
                "analyzed_at": datetime.now(pytz.utc) 
            },
            "content": { 
                "original_title": art['title'], 
                "korean_title": k_title, 
                "korean_body": k_body 
            },
            "intelligence": { 
                "impact_score": res.get('impact_score', 5) if res else 5,
                "market_sentiment": res.get('market_sentiment', 'NEUTRAL') if res else 'NEUTRAL',
                "actionable_insight": res.get('actionable_insight', '-') if res else '-',
                "related_assets": res.get('related_assets', []) if res else []
            }
        }
        
        if "ConsoleOutputDB" in str(type(db)):
            print(f"[SAVE V2] {k_title}")
        else:
            db.collection(COLLECTION_NAME).document(art['id']).set(doc_data)

    # 4. Calendar
    fetch_and_save_calendar(db)
    print("✅ Done.")

if __name__ == "__main__":
    main()
