# Gemini API 키 환경변수 전달 문제 해결

## 🔍 현재 상황

### **증상**

- ✅ GitHub Actions 실행됨 (Firestore 타임스탬프: 2026-01-04 10:45)
- ❌ Gemini API 키 인식 안 됨
- ❌ "AI 키가 설정되지 않아..." 메시지

### **진단**

GitHub Actions는 정상 실행 중이지만, **환경변수 `GEMINI_API_KEY`가 Python 코드로 전달되지 않음**

---

## 🚨 가능한 원인 (우선순위순)

### **1. GitHub Secret 이름 오타** (가장 흔함)

#### **확인 방법**

```
GitHub Repository → Settings → Secrets and variables → Actions
→ Repository secrets 목록 확인
```

#### **정확한 이름**

```
✅ GEMINI_API_KEY (정확히 대문자, 언더스코어)

❌ 흔한 실수들:
- gemini_api_key (소문자)
- GEMINI_KEY (KEY 빠짐)
- GEMINI-API-KEY (하이픈)
- GEMINI_API_KEY_ (끝에 언더스코어)
- _GEMINI_API_KEY (앞에 언더스코어)
```

---

### **2. GitHub Secret 값에 공백/따옴표 포함**

#### **잘못된 예**

```
❌ "AIzaSyC..." (따옴표 포함)
❌ AIzaSyC...  (끝에 공백)
❌  AIzaSyC... (앞에 공백)
```

#### **올바른 예**

```
✅ AIzaSyC... (따옴표 없음, 공백 없음)
```

#### **해결 방법**

1. GitHub Secrets에서 `GEMINI_API_KEY` 삭제
2. 새로 추가 시 값 복사 후 **공백 제거 확인**
3. 따옴표 없이 순수 키 값만 입력

---

### **3. API 키 만료 또는 비활성화**

#### **확인 방법**

1. [Google AI Studio](https://aistudio.google.com/app/apikey) 접속
2. 기존 API 키 상태 확인
3. 필요 시 새 키 발급

#### **테스트 방법**

```bash
# PowerShell에서 테스트
$env:GEMINI_API_KEY="your_api_key_here"
cd news_crawler
python -c "import os; print('Key:', os.environ.get('GEMINI_API_KEY', 'NOT FOUND')[:20] + '...')"
```

---

### **4. GitHub Actions 캐시 문제**

#### **해결 방법**

```
1. GitHub Repository → Actions
2. 최신 실행 워크플로우 클릭
3. 오른쪽 상단 "..." 메뉴
4. "Delete workflow run" 클릭
5. 새로 "Run workflow" 실행
```

---

## ✅ 종합 해결 방안

### **Step 1: GitHub Secret 완전 재설정**

#### **1-1. 기존 Secret 삭제**

```
Settings → Secrets and variables → Actions
→ GEMINI_API_KEY 옆 "Remove" 클릭
→ 확인
```

#### **1-2. API 키 재발급** (권장)

```
1. https://aistudio.google.com/app/apikey 접속
2. 기존 키 삭제 (선택)
3. "Create API Key" 클릭
4. 새 키 복사 (공백 없이!)
```

#### **1-3. 새 Secret 추가**

```
Settings → Secrets and variables → Actions
→ "New repository secret" 클릭

Name: GEMINI_API_KEY
Secret: [복사한 키를 붙여넣기]
       (따옴표 없음, 공백 없음, 순수 키 값만)

→ "Add secret" 클릭
```

---

### **Step 2: 다른 환경변수도 확인**

#### **필수 Secrets (4개)**

```
1. GEMINI_API_KEY
   - 형식: AIzaSyC...
   - 길이: 약 39자

2. FIREBASE_CREDENTIALS
   - 형식: {"type":"service_account",...}
   - JSON 전체 (따옴표 없이)

3. ECOS_API_KEY
   - 형식: 한국은행 발급 키
   - 길이: 약 40자

4. TWELVE_DATA_API_KEY
   - 형식: Twelve Data 발급 키
   - 길이: 약 32자
```

---

### **Step 3: 워크플로우 파일 환경변수 확인**

현재 `.github/workflows/news_update.yml`:

```yaml
env:
  GEMINI_API_KEY: ${{ secrets.GEMINI_API_KEY }}
  FIREBASE_CREDENTIALS: ${{ secrets.FIREBASE_CREDENTIALS }}
  TWELVE_DATA_API_KEY: ${{ secrets.TWELVE_DATA_API_KEY }}
  ECOS_API_KEY: ${{ secrets.ECOS_API_KEY }}
```

**확인 사항**:

- ✅ `secrets.GEMINI_API_KEY` (정확히 대문자)
- ✅ 중괄호 2개 `{{ }}`
- ✅ 공백 없음

---

### **Step 4: Python 코드 디버깅 강화**

`main.py`에 이미 추가된 로그:

```python
logger.info(f"🔍 Checking GEMINI_API_KEY from environment: {'Found' if api_key else 'Not found'}")
if api_key:
    logger.info(f"   API Key (first 10 chars): {api_key[:10]}...")
```

**GitHub Actions 로그에서 확인**:

```
✅ 성공 시:
🔍 Checking GEMINI_API_KEY from environment: Found
   API Key (first 10 chars): AIzaSyC...

❌ 실패 시:
🔍 Checking GEMINI_API_KEY from environment: Not found
⚠️ GEMINI_API_KEY not found. AI analysis will be skipped.
```

---

### **Step 5: 로컬 테스트로 API 키 검증**

#### **5-1. 환경변수 설정**

```powershell
# Windows PowerShell
$env:GEMINI_API_KEY="your_actual_api_key_here"
$env:FIREBASE_CREDENTIALS=Get-Content serviceAccountKey.json -Raw
```

#### **5-2. Python 실행**

```bash
cd news_crawler
python main.py
```

#### **5-3. 로그 확인**

```
✅ 성공 시:
🔍 Checking GEMINI_API_KEY from environment: Found
   API Key (first 10 chars): AIzaSyC...
✅ Gemini API configured successfully
🤖 Analyzing batch 1/2 (5 articles)...
✅ Batch 1 analyzed successfully

❌ 실패 시:
🔍 Checking GEMINI_API_KEY from environment: Not found
⚠️ GEMINI_API_KEY not found. AI analysis will be skipped.
```

---

## 🎯 체크리스트

### **GitHub Secrets**

- [ ] `GEMINI_API_KEY` 이름 정확히 확인 (대문자, 언더스코어)
- [ ] 값에 따옴표 없음 확인
- [ ] 값에 공백 없음 확인
- [ ] API 키 형식 확인 (AIzaSyC...)
- [ ] API 키 길이 확인 (약 39자)

### **API 키 유효성**

- [ ] Google AI Studio에서 키 상태 확인
- [ ] 필요 시 새 키 발급
- [ ] 로컬 테스트로 키 검증

### **워크플로우**

- [ ] `.github/workflows/news_update.yml` 위치 확인
- [ ] `env:` 섹션에 `GEMINI_API_KEY` 포함 확인
- [ ] `${{ secrets.GEMINI_API_KEY }}` 문법 확인

### **GitHub Actions**

- [ ] 기존 실행 삭제
- [ ] 새로 "Run workflow" 실행
- [ ] 로그에서 "Found" 메시지 확인

---

## 💡 디버깅 팁

### **Tip 1: Secret 값 확인 (간접적)**

GitHub Secrets는 보안상 값을 직접 볼 수 없지만, 길이는 확인 가능:

```yaml
# 임시 디버깅 단계 추가 (워크플로우 파일)
- name: Debug Environment Variables
  run: |
    echo "GEMINI_API_KEY length: ${#GEMINI_API_KEY}"
    echo "First 10 chars: ${GEMINI_API_KEY:0:10}"
```

**주의**: 디버깅 후 이 단계는 삭제해야 함!

---

### **Tip 2: 다른 Secret으로 테스트**

`FIREBASE_CREDENTIALS`는 작동하는지 확인:

```yaml
- name: Test Firebase Credentials
  run: |
    if [ -z "$FIREBASE_CREDENTIALS" ]; then
      echo "FIREBASE_CREDENTIALS is empty!"
    else
      echo "FIREBASE_CREDENTIALS is set (length: ${#FIREBASE_CREDENTIALS})"
    fi
```

---

### **Tip 3: API 키 직접 테스트**

로컬에서 Python으로 직접 테스트:

```python
import os
import google.generativeai as genai

api_key = "your_api_key_here"  # 직접 입력
genai.configure(api_key=api_key)
model = genai.GenerativeModel('gemini-1.5-flash-latest')

response = model.generate_content("Hello, test!")
print(response.text)
```

**성공 시**: 응답 텍스트 출력
**실패 시**: API 키 문제 확인

---

## 🚀 최종 해결 순서

1. **GitHub Secret 완전 재설정** (5분)
   - 기존 `GEMINI_API_KEY` 삭제
   - Google AI Studio에서 새 키 발급
   - 새 Secret 추가 (공백/따옴표 없이)

2. **GitHub Actions 재실행** (3분)
   - 기존 실행 삭제
   - "Run workflow" 클릭
   - 로그 확인

3. **로그에서 "Found" 확인** (1분)

   ```
   🔍 Checking GEMINI_API_KEY from environment: Found
      API Key (first 10 chars): AIzaSyC...
   ```

4. **Firestore 데이터 확인** (1분)

   ```json
   {
     "korean_title": "바이두 반도체 자회사 쿤룬신...",
     "korean_body": "중국 검색엔진 대기업 바이두의...",
     "intelligence": {
       "impact_score": 7,
       "actionable_insight": "중국 AI 칩 시장..."
     }
   }
   ```

**총 소요 시간: 약 10분**

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
  "korean_body": "중국 검색엔진 대기업 바이두의 반도체 자회사 쿤룬신이 AI 칩 시장 성장에 힘입어 홍콩 증시 상장을 추진하고 있습니다...",
  "korean_title": "바이두 반도체 자회사 쿤룬신, AI 칩 붐 속 홍콩 상장 추진",
  "intelligence": {
    "impact_score": 7,
    "market_sentiment": "BULLISH",
    "actionable_insight": "중국 AI 칩 시장 성장에 주목. 바이두(BIDU) 주식 매수 고려...",
    "related_assets": ["BIDU", "NVDA", "AMD"]
  }
}
```

**Last Updated**: 2026-01-03
