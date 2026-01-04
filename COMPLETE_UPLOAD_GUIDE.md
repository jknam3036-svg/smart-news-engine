# GitHub 소스파일 전체 업로드 가이드

## 🎯 목표

모든 소스파일을 GitHub에 업로드하고 중복 파일 정리

---

## ✅ 방법 1: 자동화 스크립트 (권장)

### **실행 방법**

```
1. 파일 탐색기에서 프로젝트 루트 폴더 열기
   → c:\AndroidStudioProjects\Make

2. "upload_to_github.bat" 파일 더블클릭

3. 자동 실행:
   - 중복 파일 삭제
   - 모든 파일 추가
   - 커밋
   - Push

4. 완료 메시지 확인
```

---

## ✅ 방법 2: 수동 명령어

### **PowerShell 또는 CMD에서 실행**

```powershell
# 1. 프로젝트 루트로 이동
cd c:\AndroidStudioProjects\Make

# 2. 중복 워크플로우 삭제
git rm -r news_crawler/.github

# 3. 모든 파일 추가
git add .

# 4. 상태 확인
git status

# 5. 커밋
git commit -m "Complete update: Add ecos_crawler, fix Gemini model, cleanup duplicates"

# 6. Push
git push
```

---

## 📊 업로드될 파일 목록

### **새로 추가될 파일**

```
✅ .github/workflows/news_update.yml (워크플로우)
✅ news_crawler/ecos_crawler.py (경제지표 크롤러)
✅ news_crawler/test_gemini_api.py (API 테스트)
✅ GEMINI_MODEL_FIX.md (문서)
✅ MISSING_FILES_FIX.md (문서)
✅ GITHUB_CLEANUP_GUIDE.md (문서)
✅ upload_to_github.bat (스크립트)
```

### **수정될 파일**

```
✅ news_crawler/main.py (Gemini 모델 수정)
```

### **삭제될 파일**

```
❌ news_crawler/.github/workflows/news_update.yml (중복)
```

---

## 🔍 Push 후 확인

### **Step 1: GitHub Repository 확인**

```
1. GitHub Repository 새로고침
2. 파일 구조 확인:

c:\AndroidStudioProjects\Make\
├── .github/
│   └── workflows/
│       └── news_update.yml ✅
│
├── news_crawler/
│   ├── calendar_crawler.py ✅
│   ├── ecos_crawler.py ✅ (새로 추가)
│   ├── main.py ✅ (수정됨)
│   ├── news_engine.py ✅
│   ├── test_gemini_api.py ✅ (새로 추가)
│   └── requirements.txt ✅
│
└── 문서들 (*.md)
```

---

### **Step 2: GitHub Actions 실행**

```
1. GitHub Repository → Actions 탭
2. "Intelligent News Crawler" 선택
3. "Run workflow" 버튼 클릭
4. "Run workflow" 다시 클릭 (확인)
5. 실행 시작 확인
```

---

### **Step 3: 로그 확인 (2-3분 후)**

**✅ 성공 로그**:

```
Run News & Calendar & Economic Indicators Crawler

--- Phase 1: Real News Fetching ---
📰 Fetched 90 NEW articles to process.
🤖 Analyzing batch 1/18 (5 articles)...
✅ Batch 1 analyzed successfully
[💾 NEWS SAVE] 바이두 반도체 자회사 쿤룬신, AI 칩 붐 속 홍콩 상장 추진 (Impact: 7)
[💾 NEWS SAVE] 건축자재 유통업체 QXO, 비콘에 적대적 인수 제안 (Impact: 6)
...

--- Phase 2: Real Calendar Fetching ---
📅 Fetching Real Economic Calendar from ko.tradingeconomics.com...
✅ [CALENDAR] Parsed 395 events from ko.tradingeconomics.com

--- Phase 3: Economic Indicators Fetching ---
Fetching: 722Y001/0101000 (M)
  ✅ Value: 3.25, Change: +0.00
Fetching: 817Y002/010200001 (D)
  ✅ Value: 2.85, Change: +0.05
Fetching: 817Y002/010210000 (D)
  ✅ Value: 3.10, Change: +0.02
Fetching: 817Y002/010502000 (D)
  ✅ Value: 3.42, Change: -0.01
Fetching: 731Y001/0000001 (D)
  ✅ Value: 1320.5, Change: -5.2
Fetching: 731Y001/0000002 (D)
  ✅ Value: 945.3, Change: +2.1
Fetching: 731Y001/0000003 (D)
  ✅ Value: 1425.8, Change: -3.5
✅ Saved 7 indicators to Firestore
✅ Collected 7/7 economic indicators

Done.
```

---

### **Step 4: Firestore 확인**

```
1. Firebase Console 접속
   → https://console.firebase.google.com/

2. Firestore Database 클릭

3. 컬렉션 확인:
   ✅ investment_insights (90개 한국어 뉴스)
   ✅ economic_calendar (395개 이벤트)
   ✅ economic_indicators (7개 지표) ← 새로 생성!
```

**economic_indicators 예시**:

```json
{
  "base_rate": {
    "id": "base_rate",
    "name": "기준금리",
    "value": 3.25,
    "change_rate": 0.0,
    "unit": "%",
    "type": "interest_rate",
    "source": "한국은행"
  },
  "usd_krw": {
    "id": "usd_krw",
    "name": "원/달러",
    "value": 1320.5,
    "change_rate": -5.2,
    "unit": "원",
    "type": "exchange_rate",
    "source": "한국은행"
  }
  // ... 나머지 5개 지표
}
```

---

### **Step 5: Android 앱 확인**

```
1. Android Studio에서 앱 실행

2. MarketScreen 진입
   → "주요 지표" 섹션 확인
   → 7개 카드에 실제 값 표시 확인
   → 0.0이 아닌 값 (예: 3.25%, 1,320.5원)

3. NewsScreen 진입
   → 한국어 제목 및 본문 확인
   → AI 인사이트 확인

4. CalendarScreen 진입
   → 경제 캘린더 이벤트 확인
```

---

## 📋 최종 체크리스트

### **Git 작업**

- [ ] `upload_to_github.bat` 실행 또는 수동 명령어 실행
- [ ] Push 완료 확인

### **GitHub 확인**

- [ ] Repository에서 파일 확인
  - [ ] `.github/workflows/news_update.yml` 존재
  - [ ] `news_crawler/ecos_crawler.py` 존재
  - [ ] `news_crawler/.github/` 삭제됨
- [ ] Actions 탭에서 워크플로우 표시 확인

### **GitHub Actions**

- [ ] "Run workflow" 실행
- [ ] 로그에서 Phase 1, 2, 3 모두 성공 확인
- [ ] 에러 없음 확인

### **Firestore**

- [ ] `investment_insights`: 한국어 뉴스 확인
- [ ] `economic_calendar`: 경제 캘린더 확인
- [ ] `economic_indicators`: 7개 지표 확인

### **Android 앱**

- [ ] MarketScreen: 경제지표 표시 확인
- [ ] NewsScreen: 한국어 뉴스 표시 확인
- [ ] CalendarScreen: 경제 캘린더 표시 확인

---

## 🎯 예상 결과

**모든 작업 완료 후**:

1. ✅ GitHub에 모든 소스파일 업로드
2. ✅ 중복 파일 정리 완료
3. ✅ GitHub Actions 15분마다 자동 실행
4. ✅ Firestore 3개 컬렉션 모두 업데이트
5. ✅ Android 앱에서 실시간 데이터 표시

**15분마다 자동으로**:

- 📰 90개 뉴스 기사 한국어 번역 및 AI 분석
- 📅 395개 경제 캘린더 이벤트 수집
- 📊 7개 경제지표 업데이트

---

## 🚀 시작하기

### **지금 바로 실행**

```
1. 파일 탐색기 열기
2. c:\AndroidStudioProjects\Make 폴더 이동
3. "upload_to_github.bat" 더블클릭
4. 완료 메시지 확인
5. GitHub Actions 실행
```

**소요 시간: 약 5분**

**Last Updated**: 2026-01-03
