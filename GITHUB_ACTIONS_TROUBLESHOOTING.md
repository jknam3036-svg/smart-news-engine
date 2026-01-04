# GitHub Actions 및 Gemini API 키 확인 가이드

## 🔍 현재 상황

Firestore에 저장된 뉴스 데이터:

```json
{
  "korean_body": "AI 키가 설정되지 않아 원문 제목만 표시합니다.",
  "korean_title": "Baidu's semiconductor unit Kunlunxin files for Hong Kong listing...",
  "actionable_insight": "AI 분석 대기 중"
}
```

**문제**: Gemini API가 작동하지 않아 AI 번역 및 분석이 실행되지 않음

---

## ✅ 해결 방법

### **Step 1: GitHub Secrets 확인**

1. GitHub Repository 접속
2. **Settings** → **Secrets and variables** → **Actions**
3. 다음 Secret이 존재하는지 확인:
   - `GEMINI_API_KEY` ✅
   - `FIREBASE_CREDENTIALS` ✅
   - `TWELVE_DATA_API_KEY` (선택)
   - `ECOS_API_KEY` ✅

**GEMINI_API_KEY 확인**:

- Secret 이름이 정확히 `GEMINI_API_KEY`인지 확인
- 값이 `AIza...`로 시작하는지 확인
- 만료되지 않았는지 확인

---

### **Step 2: GitHub Actions 수동 실행**

1. GitHub Repository → **Actions** 탭
2. 왼쪽에서 **"Intelligent News Crawler"** 선택
3. 오른쪽 상단 **"Run workflow"** 클릭
4. **"Run workflow"** 버튼 다시 클릭 (확인)
5. 실행 완료 대기 (약 2-3분)

---

### **Step 3: 실행 로그 확인**

GitHub Actions 실행 후:

1. 실행된 워크플로우 클릭
2. **"Run News & Calendar Crawler"** 단계 확인
3. 로그에서 다음 메시지 확인:

**✅ 정상 작동 시**:

```
--- Phase 1: Real News Fetching ---
📰 Fetched 10 NEW articles to process.
🤖 Analyzing batch 1/2 (5 articles)...
✅ Batch 1 analyzed successfully
[💾 NEWS SAVE] 바이두 반도체 자회사 쿤룬신, AI 칩 붐 속 홍콩 상장 추진 (Impact: 7)
```

**❌ API 키 문제 시**:

```
⚠️ Skipping AI Analysis (No API Key). Using metadata only.
[💾 NEWS SAVE] Baidu's semiconductor unit Kunlunxin files... (Impact: 5)
```

---

### **Step 4: Gemini API 키 재발급 (필요 시)**

API 키가 작동하지 않는 경우:

1. [Google AI Studio](https://aistudio.google.com/app/apikey) 접속
2. 기존 키 삭제 (선택)
3. **"Create API Key"** 클릭
4. 새 키 복사
5. GitHub Secrets 업데이트:
   - `GEMINI_API_KEY` 편집
   - 새 키 붙여넣기
   - **"Update secret"** 클릭

---

### **Step 5: Firestore 데이터 확인**

GitHub Actions 실행 후 Firestore 확인:

1. [Firebase Console](https://console.firebase.google.com/) 접속
2. 프로젝트 선택
3. **Firestore Database** 클릭
4. `investment_insights` 컬렉션 열기
5. 최신 문서 확인

**✅ 정상 데이터 예시**:

```json
{
  "content": {
    "korean_title": "바이두 반도체 자회사 쿤룬신, AI 칩 붐 속 홍콩 상장 추진",
    "korean_body": "중국 검색엔진 대기업 바이두의 반도체 자회사 쿤룬신이...",
    "original_title": "Baidu's semiconductor unit Kunlunxin files..."
  },
  "intelligence": {
    "impact_score": 7,
    "market_sentiment": "BULLISH",
    "actionable_insight": "중국 AI 칩 시장 성장에 주목. 바이두 주식 매수 고려...",
    "related_assets": ["BIDU", "NVDA", "AMD"]
  }
}
```

---

## 🔄 Fallback 로직 (API 키 없을 때)

현재 코드 (`main.py` Line 391-414):

```python
else:
    # FALLBACK - Use RSS description/summary as body content
    korean_title = art['title']  # Original English title
    
    # Use RSS description if available
    rss_description = art.get('full_content', '').strip()
    if rss_description and len(rss_description) > 20:
        korean_body = f"{rss_description}\n\n[AI 번역 대기 중 - 원문 기사입니다. 자세한 내용은 원문 링크를 확인하세요.]"
    else:
        korean_body = f"[AI 분석 대기 중]\n\n이 기사는 {art['source']} 소스에서 수집되었습니다.\n제목: {art['title']}\n\n자세한 내용은 원문 링크를 확인하세요."
    
    impact = 5
    sentiment = 'NEUTRAL'
    insight = "AI 분석 대기 중 - Gemini API 키를 설정하면 한국어 번역 및 투자 인사이트를 제공합니다."
```

**개선 사항**:

- ✅ RSS `full_content` 필드 활용
- ✅ 실제 기사 요약 제공
- ✅ 사용자에게 유용한 정보 제공

---

## 🎯 체크리스트

### **GitHub Secrets**

- [ ] `GEMINI_API_KEY` 존재 확인
- [ ] API 키 형식 확인 (`AIza...`)
- [ ] API 키 만료 여부 확인

### **GitHub Actions**

- [ ] 최신 코드 Push 확인
- [ ] 워크플로우 수동 실행
- [ ] 실행 로그 확인
- [ ] 에러 메시지 확인

### **Firestore**

- [ ] `investment_insights` 컬렉션 확인
- [ ] 최신 문서 데이터 확인
- [ ] `korean_body` 내용 확인
- [ ] `actionable_insight` 내용 확인

### **테스트**

- [ ] Android 앱에서 뉴스 화면 열기
- [ ] 한국어 제목 표시 확인
- [ ] AI 인사이트 표시 확인

---

## 💡 문제 해결 팁

### **Q: GitHub Actions가 실행되지 않아요**

```
1. .github/workflows/news_update.yml 파일 위치 확인
   → news_crawler/.github/workflows/ (X)
   → .github/workflows/ (O)

2. 파일 이름 확인
   → news_update.yml (정확히)

3. YAML 문법 오류 확인
   → GitHub Actions 탭에서 에러 메시지 확인
```

### **Q: API 키를 설정했는데도 작동하지 않아요**

```
1. Secret 이름 확인
   → GEMINI_API_KEY (대문자, 언더스코어)

2. API 키 형식 확인
   → AIzaSyC... (정확한 키)

3. API 키 활성화 확인
   → Google AI Studio에서 키 상태 확인

4. 워크플로우 재실행
   → Actions → Run workflow
```

### **Q: 로그에 "No API Key" 메시지가 나와요**

```
1. GitHub Secrets 확인
   → Settings → Secrets and variables → Actions

2. GEMINI_API_KEY 존재 확인
   → 없으면 추가

3. 워크플로우 파일 확인
   → env: GEMINI_API_KEY: ${{ secrets.GEMINI_API_KEY }}

4. 재실행
```

---

## 🚀 최종 확인

모든 설정이 완료되면:

1. ✅ GitHub Actions 자동 실행 (15분마다)
2. ✅ Gemini AI 번역 및 분석
3. ✅ Firestore에 한국어 데이터 저장
4. ✅ Android 앱에서 한국어 뉴스 표시

**Last Updated**: 2026-01-03
