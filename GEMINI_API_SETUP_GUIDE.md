# Gemini API 키 GitHub Secret 설정 완벽 가이드

## ✅ API 키 확인 완료

스크린샷 확인 결과:

- ✅ API 키 존재: `...VFrc`
- ✅ 생성일: 2026.1.2
- ✅ 상태: 활성화 (결제 설정 - 무료 등급)
- ✅ 프로젝트: Default Gemini Project

**이 API 키는 정상 작동합니다!**

---

## 🚨 문제: GitHub Secret 설정 오류

API 키는 정상이지만, GitHub Actions에서 인식하지 못하는 이유:

### **가능한 원인**

1. ❌ Secret 이름 오타
2. ❌ Secret 값 복사 시 공백/따옴표 포함
3. ❌ 잘못된 Secret 위치 (Organization vs Repository)

---

## ✅ 정확한 설정 방법

### **Step 1: Google AI Studio에서 API 키 복사**

1. **Google AI Studio** 접속
   - <https://aistudio.google.com/app/apikey>

2. **API 키 복사**

   ```
   1. "...VFrc" 키 옆 "복사" 아이콘 클릭
   2. 클립보드에 전체 키 복사됨
   3. 메모장에 붙여넣기 (확인용)
   ```

3. **복사된 키 확인**

   ```
   ✅ 올바른 형식:
   AIzaSyC... (약 39자)
   
   ❌ 잘못된 형식:
   "AIzaSyC..." (따옴표 포함)
   AIzaSyC...  (끝에 공백)
   ```

---

### **Step 2: GitHub Repository Secrets 설정**

#### **2-1. Secrets 페이지 접속**

```
1. GitHub Repository 접속
2. "Settings" 탭 클릭
3. 왼쪽 메뉴에서 "Secrets and variables" 클릭
4. "Actions" 클릭
```

#### **2-2. 기존 Secret 확인 및 삭제**

```
Repository secrets 목록에서:

1. "GEMINI_API_KEY" 찾기
2. 있으면 오른쪽 "Remove" 클릭
3. 확인 팝업에서 "Yes, remove this secret" 클릭
```

#### **2-3. 새 Secret 추가**

```
1. "New repository secret" 버튼 클릭

2. Name 입력:
   GEMINI_API_KEY
   (정확히 대문자, 언더스코어)

3. Secret 입력:
   - Google AI Studio에서 복사한 키 붙여넣기
   - 따옴표 없이!
   - 공백 없이!
   - 순수 키 값만!

4. "Add secret" 버튼 클릭
```

---

### **Step 3: Secret 이름 정확성 체크**

#### **정확한 이름** (대소문자 구분)

```
✅ GEMINI_API_KEY
```

#### **흔한 실수들**

```
❌ gemini_api_key (소문자)
❌ Gemini_Api_Key (카멜케이스)
❌ GEMINI_KEY (API 빠짐)
❌ GEMINI-API-KEY (하이픈)
❌ GEMINI_API_KEY_ (끝에 언더스코어)
❌ _GEMINI_API_KEY (앞에 언더스코어)
❌ GEMINI API KEY (공백)
```

---

### **Step 4: Secret 값 정확성 체크**

#### **올바른 형식**

```
✅ AIzaSyC... (순수 키 값)
   - 길이: 약 39자
   - 시작: AIza
   - 끝: ...VFrc (스크린샷 확인)
```

#### **잘못된 형식**

```
❌ "AIzaSyC..." (따옴표 포함)
❌ 'AIzaSyC...' (작은따옴표 포함)
❌ AIzaSyC...  (끝에 공백)
❌  AIzaSyC... (앞에 공백)
❌ AIzaSyC...\n (줄바꿈 포함)
```

---

### **Step 5: Organization vs Repository Secrets**

#### **확인 사항**

```
Settings → Secrets and variables → Actions

상단 탭 확인:
✅ "Repository secrets" 선택 (개인 프로젝트)
❌ "Organization secrets" (조직 프로젝트)

GEMINI_API_KEY가 "Repository secrets"에 있어야 함!
```

---

### **Step 6: GitHub Actions 재실행**

#### **6-1. 기존 실행 삭제** (선택)

```
1. Actions 탭
2. 최신 실행 클릭
3. 오른쪽 상단 "..." 메뉴
4. "Delete workflow run" 클릭
```

#### **6-2. 새로 실행**

```
1. Actions 탭
2. "Intelligent News Crawler" 선택
3. "Run workflow" 버튼 클릭
4. "Run workflow" 다시 클릭 (확인)
```

#### **6-3. 로그 확인**

```
실행 중인 워크플로우 클릭
→ "Run News & Calendar & Economic Indicators Crawler" 단계 클릭

✅ 성공 로그:
🔍 Checking GEMINI_API_KEY from environment: Found
   API Key (first 10 chars): AIzaSyC...
✅ Gemini API configured successfully
🤖 Analyzing batch 1/2 (5 articles)...
✅ Batch 1 analyzed successfully

❌ 실패 로그:
🔍 Checking GEMINI_API_KEY from environment: Not found
⚠️ GEMINI_API_KEY not found. AI analysis will be skipped.
```

---

## 🧪 로컬 테스트 (선택)

GitHub Secret 설정 전에 로컬에서 API 키 테스트:

### **테스트 스크립트 실행**

```powershell
# 1. 환경변수 설정 (Google AI Studio에서 복사한 키)
$env:GEMINI_API_KEY="your_actual_api_key_here"

# 2. 테스트 실행
cd news_crawler
python test_gemini_api.py
```

### **예상 출력**

```
✅ GEMINI_API_KEY 환경변수 확인됨
   길이: 39 문자
   첫 10자: AIzaSyC...
   마지막 5자: ...VFrc
✅ google-generativeai 패키지 import 성공
✅ Gemini API 키 설정 성공

📋 사용 가능한 모델 테스트:
  ✅ gemini-1.5-flash-latest: 생성 성공
  ✅ gemini-1.5-flash: 생성 성공
  ✅ gemini-1.5-pro-latest: 생성 성공
  ✅ gemini-1.5-pro: 생성 성공
  ✅ gemini-2.0-flash-exp: 생성 성공

🧪 실제 API 호출 테스트 (모델: gemini-1.5-flash-latest):
✅ API 호출 성공!
   응답: API test successful...

==================================================
✅ Gemini API 키 테스트 완료!
==================================================
사용 가능한 모델: gemini-1.5-flash-latest, gemini-1.5-flash, ...

이 API 키는 GitHub Actions에서 정상 작동합니다.
```

---

## 📋 최종 체크리스트

### **Google AI Studio**

- [x] API 키 존재 확인 (`...VFrc`)
- [x] 활성화 상태 확인 (결제 설정)
- [ ] API 키 전체 복사 (클립보드)

### **GitHub Secrets**

- [ ] Settings → Secrets and variables → Actions 접속
- [ ] 기존 `GEMINI_API_KEY` 삭제 (있으면)
- [ ] 새 Secret 추가
  - [ ] Name: `GEMINI_API_KEY` (정확히)
  - [ ] Secret: 복사한 키 (따옴표/공백 없이)
- [ ] "Repository secrets"에 추가 확인

### **GitHub Actions**

- [ ] Actions 탭 접속
- [ ] "Run workflow" 실행
- [ ] 로그에서 "Found" 메시지 확인
- [ ] "✅ Gemini API configured successfully" 확인

### **Firestore**

- [ ] Firebase Console 접속
- [ ] `investment_insights` 컬렉션 확인
- [ ] 최신 문서의 `korean_body` 확인
- [ ] 한국어 번역 데이터 확인

---

## 🎯 예상 결과

### **Before** (현재)

```json
{
  "korean_body": "AI 키가 설정되지 않아 원문 제목만 표시합니다.",
  "korean_title": "Baidu's semiconductor unit Kunlunxin files...",
  "actionable_insight": "AI 분석 대기 중"
}
```

### **After** (Secret 재설정 후)

```json
{
  "korean_body": "중국 검색엔진 대기업 바이두의 반도체 자회사 쿤룬신이 AI 칩 시장 성장에 힘입어 홍콩 증시 상장을 추진하고 있습니다. 이번 상장은 중국 AI 반도체 산업의 급성장을 반영하며...",
  "korean_title": "바이두 반도체 자회사 쿤룬신, AI 칩 붐 속 홍콩 상장 추진",
  "intelligence": {
    "impact_score": 7,
    "market_sentiment": "BULLISH",
    "actionable_insight": "중국 AI 칩 시장 성장에 주목. 바이두(BIDU) 주식 매수 고려. 엔비디아(NVDA), AMD 등 글로벌 AI 칩 기업도 수혜 예상.",
    "related_assets": ["BIDU", "NVDA", "AMD", "TSM"]
  }
}
```

---

## 💡 추가 팁

### **Tip 1: API 키 복사 시 주의사항**

```
1. Google AI Studio에서 "복사" 아이콘 클릭
2. 메모장에 붙여넣기 (확인)
3. 앞뒤 공백 제거 확인
4. 따옴표 없음 확인
5. GitHub Secrets에 붙여넣기
```

### **Tip 2: Secret 이름 대소문자 확인**

```
워크플로우 파일:
env:
  GEMINI_API_KEY: ${{ secrets.GEMINI_API_KEY }}
                              ^^^^^^^^^^^^^^^^
                              정확히 일치해야 함!
```

### **Tip 3: 여러 개의 API 키**

```
여러 프로젝트에서 사용하는 경우:
- 각 프로젝트마다 별도 API 키 발급 권장
- 또는 하나의 키를 여러 프로젝트에서 공유 가능
```

---

## 🚀 최종 확인

모든 단계 완료 후:

1. ✅ Google AI Studio에서 API 키 복사
2. ✅ GitHub Secrets에 정확히 설정
3. ✅ GitHub Actions 재실행
4. ✅ 로그에서 "Found" 확인
5. ✅ Firestore에서 한국어 데이터 확인
6. ✅ Android 앱에서 뉴스 표시 확인

**이제 15분마다 자동으로 한국어 뉴스가 업데이트됩니다!** 🎉

**Last Updated**: 2026-01-03
