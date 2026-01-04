#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RSS Feed 유효성 검증 및 비교 분석 스크립트
- RSS vs 직접 스크래핑 비교
- 현재 설정된 RSS URL 유효성 테스트
"""
import feedparser
import requests
from datetime import datetime
import time

# 현재 프로젝트에서 사용 중인 RSS Feeds
CURRENT_RSS_FEEDS = {
    # main.py
    "Google_US_Economy": "https://news.google.com/rss/search?q=US+Economy+when:1d&hl=en-US&gl=US&ceid=US:en",
    "Google_Global_Markets": "https://news.google.com/rss/search?q=Global+Markets+when:1d&hl=en-US&gl=US&ceid=US:en",
    "Google_Korea_Economy": "https://news.google.com/rss/search?q=%EA%B2%BD%EC%A0%9C+when:1d&hl=ko&gl=KR&ceid=KR:ko",
    
    # news_engine.py
    "US_Economy_KR": "https://news.google.com/rss/search?q=%EB%AF%B8%EA%B5%AD%EA%B2%BD%EC%A0%9C+when:1d&hl=ko&gl=KR&ceid=KR:ko",
    "Global_Market_KR": "https://news.google.com/rss/search?q=%EC%84%B8%EA%B3%84%EC%A6%9D%EC%8B%9C+when:1d&hl=ko&gl=KR&ceid=KR:ko",
    "KR_Economy": "https://news.google.com/rss/search?q=%ED%95%9C%EA%B5%AD%EA%B2%BD%EC%A0%9C+when:1d&hl=ko&gl=KR&ceid=KR:ko"
}

# 프리미엄 RSS Feeds (NEWS_SOURCES.md 참조)
PREMIUM_RSS_FEEDS = {
    "WSJ_Markets": "https://feeds.a.dj.com/rss/RSSMarketsMain.xml",
    "CNBC_Economy": "https://www.cnbc.com/id/20910258/device/rss/rss.html",
    "CNBC_Tech": "https://www.cnbc.com/id/19854910/device/rss/rss.html",
    "MarketWatch": "https://www.marketwatch.com/rss/topstories",
    "Reuters_Business": "https://www.reutersagency.com/feed/?taxonomy=best-topics&post_type=best",
    "Investing_News": "https://www.investing.com/rss/news.rss",
    "CoinDesk": "https://www.coindesk.com/arc/outboundfeeds/rss/",
    "TechCrunch": "https://techcrunch.com/feed/"
}

def test_rss_feed(name, url, timeout=10):
    """RSS Feed 테스트"""
    try:
        print(f"\n[TEST] {name}")
        print(f"  URL: {url}")
        
        start_time = time.time()
        feed = feedparser.parse(url)
        elapsed = time.time() - start_time
        
        if feed.bozo:
            print(f"  [WARNING] Parsing issue: {feed.bozo_exception}")
        
        if not feed.entries:
            print(f"  [FAIL] No entries found")
            return False
        
        print(f"  [OK] Found {len(feed.entries)} articles")
        print(f"  [OK] Response time: {elapsed:.2f}s")
        
        # 첫 번째 기사 샘플
        if feed.entries:
            first = feed.entries[0]
            print(f"  Sample: {first.title[:60]}...")
            if hasattr(first, 'published'):
                print(f"  Published: {first.published}")
        
        return True
        
    except Exception as e:
        print(f"  [ERROR] {e}")
        return False

def compare_methods():
    """RSS vs 직접 스크래핑 비교"""
    print("=" * 80)
    print("RSS vs 직접 웹 스크래핑 비교 분석")
    print("=" * 80)
    
    comparison = """
    
+---------------------+----------------------+----------------------+
|      항목           |    RSS 수집          |   직접 스크래핑       |
+---------------------+----------------------+----------------------+
| 안정성              | ***** (매우 높음)    | **    (낮음)         |
|                     | 공식 API, 변경 적음  | HTML 구조 변경 시    |
|                     |                      | 즉시 작동 중단       |
+---------------------+----------------------+----------------------+
| 속도                | ***** (매우 빠름)    | ***   (보통)         |
|                     | XML 파싱만 필요      | 전체 HTML 다운로드   |
|                     | 평균 0.5~2초         | 평균 3~10초          |
+---------------------+----------------------+----------------------+
| 차단 위험           | ***** (거의 없음)    | *     (매우 높음)    |
|                     | 공식 제공 서비스     | 403/429 에러 빈번    |
|                     |                      | IP 차단 가능         |
+---------------------+----------------------+----------------------+
| 법적 문제           | ***** (안전)         | **    (위험)         |
|                     | 공식 배포 채널       | robots.txt 위반 가능 |
|                     | 저작권 명시          | 법적 분쟁 소지       |
+---------------------+----------------------+----------------------+
| 데이터 품질         | ****  (높음)         | ***** (매우 높음)    |
|                     | 요약본 제공          | 전체 본문 수집 가능  |
|                     | 메타데이터 풍부      |                      |
+---------------------+----------------------+----------------------+
| 유지보수            | ***** (매우 쉬움)    | *     (매우 어려움)  |
|                     | 코드 변경 거의 불필요| 지속적 모니터링 필요 |
|                     |                      | 사이트별 파서 필요   |
+---------------------+----------------------+----------------------+
| 확장성              | ***** (매우 높음)    | **    (낮음)         |
|                     | 수백 개 소스 동시    | 사이트마다 별도 구현 |
|                     | 처리 가능            |                      |
+---------------------+----------------------+----------------------+
| 서버 부하           | ***** (매우 낮음)    | **    (높음)         |
|                     | 경량 XML             | 전체 페이지 로드     |
|                     | 캐싱 가능            |                      |
+---------------------+----------------------+----------------------+

[결론]
[OK] RSS 수집이 압도적으로 유리
   - 안정성, 속도, 법적 안전성, 유지보수 모든 면에서 우수
   - 유일한 단점: 전체 본문 미제공 (하지만 링크로 접근 가능)

[WARNING] 직접 스크래핑은 다음 경우에만 고려:
   - RSS를 제공하지 않는 사이트
   - 특정 데이터 필드가 RSS에 없는 경우
   - 실시간성이 극도로 중요한 경우 (RSS는 보통 5~15분 지연)
    """
    print(comparison)

def main():
    print("=" * 80)
    print("RSS Feed 유효성 검증 시작")
    print("=" * 80)
    
    # 1. 비교 분석 출력
    compare_methods()
    
    # 2. 현재 설정 테스트
    print("\n" + "=" * 80)
    print("현재 프로젝트 RSS Feeds 테스트")
    print("=" * 80)
    
    current_results = {}
    for name, url in CURRENT_RSS_FEEDS.items():
        result = test_rss_feed(name, url)
        current_results[name] = result
        time.sleep(0.5)  # Rate limiting
    
    # 3. 프리미엄 RSS 테스트
    print("\n" + "=" * 80)
    print("프리미엄 RSS Feeds 테스트 (NEWS_SOURCES.md)")
    print("=" * 80)
    
    premium_results = {}
    for name, url in PREMIUM_RSS_FEEDS.items():
        result = test_rss_feed(name, url)
        premium_results[name] = result
        time.sleep(0.5)
    
    # 4. 결과 요약
    print("\n" + "=" * 80)
    print("테스트 결과 요약")
    print("=" * 80)
    
    print("\n[현재 프로젝트 RSS]")
    success = sum(1 for v in current_results.values() if v)
    total = len(current_results)
    print(f"  성공: {success}/{total} ({success/total*100:.1f}%)")
    for name, result in current_results.items():
        status = "[OK]" if result else "[FAIL]"
        print(f"  {status} {name}")
    
    print("\n[프리미엄 RSS]")
    success = sum(1 for v in premium_results.values() if v)
    total = len(premium_results)
    print(f"  성공: {success}/{total} ({success/total*100:.1f}%)")
    for name, result in premium_results.items():
        status = "[OK]" if result else "[FAIL]"
        print(f"  {status} {name}")
    
    # 5. 권장 사항
    print("\n" + "=" * 80)
    print("권장 사항")
    print("=" * 80)
    
    recommendations = """
    
[OK] 현재 Google News RSS 사용은 올바른 선택입니다:
   - 안정적이고 빠른 응답
   - 다양한 소스 통합 (WSJ, Reuters 등 자동 포함)
   - 한국어/영어 모두 지원
   - 무료, 무제한

[IMPROVE] 개선 제안:
   1. Google News RSS (현재) + 프리미엄 RSS (WSJ, CNBC 등) 병행
      -> 더 풍부한 데이터 소스
   
   2. RSS에서 링크만 가져오고, 중요 기사는 본문 스크래핑
      -> RSS의 안정성 + 전체 본문 확보
   
   3. 경제 캘린더는 스크래핑 유지 (RSS 미제공)
      -> 현재 방식 유지

[WARNING] 주의: 직접 스크래핑으로 전환하지 마세요
   - 유지보수 비용 10배 증가
   - 차단 위험으로 서비스 중단 가능성
    """
    print(recommendations)

if __name__ == "__main__":
    main()
