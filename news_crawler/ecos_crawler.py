"""
ECOS 경제지표 크롤러
한국은행 경제통계시스템(ECOS) API를 사용하여 경제지표 수집
Firestore에 저장하여 Android 앱에서 빠르게 조회 가능
"""

import os
import logging
import requests
import firebase_admin
from firebase_admin import credentials, firestore
from datetime import datetime
import pytz

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# ECOS API 설정
ECOS_API_KEY = os.environ.get('ECOS_API_KEY', '')
BASE_URL = "https://ecos.bok.or.kr/api/StatisticSearch"

# 통계코드 및 항목코드 정의
INDICATORS = [
    # 금리 지표
    {
        "id": "base_rate",
        "name": "기준금리",
        "stat_code": "722Y001",
        "item_code": "0101000",
        "cycle": "M",  # 월간
        "unit": "%",
        "type": "interest_rate"
    },
    {
        "id": "treasury_3y",
        "name": "국고채 3년",
        "stat_code": "817Y002",
        "item_code": "010200001",
        "cycle": "D",  # 일간
        "unit": "%",
        "type": "interest_rate"
    },
    {
        "id": "treasury_10y",
        "name": "국고채 10년",
        "stat_code": "817Y002",
        "item_code": "010210000",
        "cycle": "D",
        "unit": "%",
        "type": "interest_rate"
    },
    {
        "id": "cd_91d",
        "name": "CD 91일",
        "stat_code": "817Y002",
        "item_code": "010502000",
        "cycle": "D",
        "unit": "%",
        "type": "interest_rate"
    },
    # 환율 지표
    {
        "id": "usd_krw",
        "name": "원/달러",
        "stat_code": "731Y001",
        "item_code": "0000001",
        "cycle": "D",
        "unit": "원",
        "type": "exchange_rate"
    },
    {
        "id": "jpy_krw",
        "name": "원/엔(100)",
        "stat_code": "731Y001",
        "item_code": "0000002",
        "cycle": "D",
        "unit": "원",
        "type": "exchange_rate"
    },
    {
        "id": "eur_krw",
        "name": "원/유로",
        "stat_code": "731Y001",
        "item_code": "0000003",
        "cycle": "D",
        "unit": "원",
        "type": "exchange_rate"
    }
]

def fetch_ecos_data(stat_code, item_code, cycle="D", days=10):
    """
    ECOS API에서 데이터 조회
    
    Args:
        stat_code: 통계코드 (예: 722Y001)
        item_code: 항목코드 (예: 0101000)
        cycle: 주기 (D=일간, M=월간, Q=분기)
        days: 조회 기간 (일수)
    
    Returns:
        (latest_value, change_rate) 또는 None
    """
    if not ECOS_API_KEY:
        logger.error("ECOS_API_KEY not set")
        return None
    
    # 날짜 계산
    from datetime import timedelta
    end_date = datetime.now()
    
    if cycle == "M":
        # 월간 데이터: 1년치
        start_date = end_date - timedelta(days=365)
    elif cycle == "Q":
        # 분기 데이터: 2년치
        start_date = end_date - timedelta(days=730)
    else:
        # 일간 데이터
        start_date = end_date - timedelta(days=days)
    
    start_str = start_date.strftime("%Y%m%d")
    end_str = end_date.strftime("%Y%m%d")
    
    # API URL 구성
    url = f"{BASE_URL}/{ECOS_API_KEY}/json/kr/1/50/{stat_code}/{cycle}/{start_str}/{end_str}/{item_code}"
    
    logger.info(f"Fetching: {stat_code}/{item_code} ({cycle})")
    
    try:
        response = requests.get(url, timeout=10)
        
        if response.status_code != 200:
            logger.error(f"HTTP Error: {response.status_code}")
            return None
        
        data = response.json()
        
        # 에러 체크
        if "RESULT" in data:
            result = data["RESULT"]
            if result.get("CODE") != "INFO-000":
                logger.warning(f"ECOS Error: {result.get('MESSAGE')}")
                return None
        
        # 데이터 추출
        if "StatisticSearch" not in data or "row" not in data["StatisticSearch"]:
            logger.warning("No data found")
            return None
        
        rows = data["StatisticSearch"]["row"]
        if not rows:
            return None
        
        # 최신 데이터 정렬
        sorted_rows = sorted(rows, key=lambda x: x["TIME"], reverse=True)
        
        latest_value = float(sorted_rows[0]["DATA_VALUE"])
        
        # 변동폭 계산
        change_rate = 0.0
        if len(sorted_rows) >= 2:
            prev_value = float(sorted_rows[1]["DATA_VALUE"])
            change_rate = latest_value - prev_value
        
        logger.info(f"  ✅ Value: {latest_value}, Change: {change_rate:+.2f}")
        
        return (latest_value, change_rate)
        
    except Exception as e:
        logger.error(f"Exception: {e}")
        return None

def save_to_firestore(db, indicators_data):
    """
    Firestore에 경제지표 저장
    Collection: economic_indicators
    """
    if not indicators_data:
        logger.warning("No data to save")
        return
    
    try:
        batch = db.batch()
        
        for indicator in indicators_data:
            doc_ref = db.collection('economic_indicators').document(indicator['id'])
            batch.set(doc_ref, indicator, merge=True)
        
        batch.commit()
        logger.info(f"✅ Saved {len(indicators_data)} indicators to Firestore")
        
    except Exception as e:
        logger.error(f"Firestore save error: {e}")

def main():
    logger.info("🚀 Starting ECOS Economic Indicators Crawler...")
    
    # Firebase 초기화
    try:
        cred_json = os.environ.get('FIREBASE_CREDENTIALS')
        if cred_json:
            import json
            cred = credentials.Certificate(json.loads(cred_json))
            try:
                firebase_admin.get_app()
            except ValueError:
                firebase_admin.initialize_app(cred)
        else:
            # Local testing
            if os.path.exists("serviceAccountKey.json"):
                cred = credentials.Certificate("serviceAccountKey.json")
                try:
                    firebase_admin.get_app()
                except ValueError:
                    firebase_admin.initialize_app(cred)
            else:
                logger.error("No Firebase credentials found")
                return
        
        db = firestore.client()
        
    except Exception as e:
        logger.error(f"Firebase init error: {e}")
        return
    
    # 경제지표 수집
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
        else:
            logger.warning(f"Failed to fetch: {indicator_config['name']}")
    
    # Firestore 저장
    if indicators_data:
        save_to_firestore(db, indicators_data)
        logger.info(f"✅ Successfully collected {len(indicators_data)}/{len(INDICATORS)} indicators")
    else:
        logger.error("❌ No indicators collected")
    
    logger.info("Done.")

if __name__ == "__main__":
    main()
