
import os
import logging
import requests
import datetime
import firebase_admin
from firebase_admin import credentials, firestore
from bs4 import BeautifulSoup
import time

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Firebase Initialization
if not firebase_admin._apps:
    cred_path = "serviceAccountKey.json"
    if os.path.exists(cred_path):
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred)
    else:
        logger.warning("Warning: serviceAccountKey.json not found.")

db = firestore.client()

def fetch_economic_calendar():
    """
    Try to fetch from Investing.com. If blocked/failed, use fallback generator
    to ensure the App always has data to display.
    """
    logger.info("📅 Starting Economic Calendar Crawl...")
    
    events = []
    try:
        # 1. Attempt Crawling
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
            
            current_date_str = datetime.datetime.now().strftime("%Y-%m-%d")

            for row in rows:
                try:
                    time_elm = row.select_one('.time')
                    event_time = time_elm.get_text(strip=True) if time_elm else "All Day"
                    
                    flag_elm = row.select_one('.flag')
                    country = "Global"
                    if flag_elm:
                        classes = flag_elm.get('class', [])
                        for c in classes:
                            if c != 'flag':
                                country = c.upper()
                                break
                    
                    importance = 1
                    if row.select('.high'): importance = 3
                    elif row.select('.medium'): importance = 2

                    title_elm = row.select_one('.event')
                    title = title_elm.get_text(strip=True) if title_elm else "Economic Event"
                    
                    actual_elm = row.select_one('.actual')
                    forecast_elm = row.select_one('.fore')
                    prev_elm = row.select_one('.prev')
                    
                    actual = actual_elm.get_text(strip=True) if actual_elm else None
                    forecast = forecast_elm.get_text(strip=True) if forecast_elm else None
                    previous = prev_elm.get_text(strip=True) if prev_elm else None

                    event_id = f"{current_date_str}-{event_time}-{country}-{title}"
                    event_id = "".join([c for c in event_id if c.isalnum() or c in "-:"])
                    
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
                except Exception:
                    continue
            
            logger.info(f"✅ Crawled {len(events)} events from Web.")
        else:
            logger.warning(f"⚠️ Web Request failed with status: {response.status_code}")

    except Exception as e:
        logger.error(f"❌ Crawling Error: {e}")

    # 2. If crawling failed, log error and return empty (NO DUMMY DATA)
    if not events:
        logger.error("❌ No events gathered from web crawling. NO FALLBACK DATA - App will show empty until next successful crawl.")
    
    # 3. Save to Firestore (only if real data exists)
    if events:
        batch = db.batch()
        count = 0
        for event in events:
            doc_ref = db.collection('economic_calendar').document(event['id'])
            batch.set(doc_ref, event, merge=True)
            count += 1
            if count >= 400: break # Batch limit
        batch.commit()
        logger.info(f"🔥 Successfully saved {len(events)} REAL events to Firestore (Collection: economic_calendar).")
    else:
        logger.error("❌ No real data to save. Skipping Firestore update.")

if __name__ == "__main__":
    fetch_economic_calendar()
