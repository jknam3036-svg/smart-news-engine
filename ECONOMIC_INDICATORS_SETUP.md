# 경제지표 Firestore 컬렉션 생성 가이드

## 🔍 현재 상황

Firestore에 `economic_indicators` 컬렉션이 없는 이유:

1. ❌ GitHub Actions가 아직 실행되지 않음
2. ❌ `ECOS_API_KEY`가 GitHub Secrets에 설정되지 않음
3. ❌ `main.py`의 Phase 3가 실행되지 않음

---

## ✅ 해결 방법 (3가지 옵션)

### **옵션 1: GitHub Actions 수동 실행** (권장)

#### **Step 1: GitHub Secrets 확인**

```
Repository → Settings → Secrets and variables → Actions
→ ECOS_API_KEY 존재 확인
```

**없으면 추가**:

1. [한국은행 ECOS](https://ecos.bok.or.kr/) 접속
2. 회원가입 → 로그인
3. "인증키 신청/관리" → "인증키 신청"
4. 발급된 키 복사
5. GitHub Secrets에 추가:
   - Name: `ECOS_API_KEY`
   - Secret: `발급받은 키`

#### **Step 2: GitHub Actions 실행**

```
1. GitHub Repository → Actions 탭
2. "Intelligent News Crawler" 선택
3. "Run workflow" 클릭
4. 실행 완료 대기 (2-3분)
```

#### **Step 3: 로그 확인**

```
실행된 워크플로우 → "Run News & Calendar Crawler"

✅ 성공 시:
--- Phase 3: Economic Indicators Fetching ---
Fetching: 722Y001/0101000 (M)
  ✅ Value: 3.25, Change: +0.00
Fetching: 817Y002/010200001 (D)
  ✅ Value: 2.85, Change: +0.05
...
✅ Saved 7 indicators to Firestore
✅ Collected 7/7 economic indicators

❌ 실패 시:
ECOS_API_KEY not set
⚠️ No economic indicators collected
```

---

### **옵션 2: 로컬에서 직접 실행** (빠른 테스트)

#### **Step 1: ECOS API 키 설정**

`local.properties` 파일에 추가:

```properties
ECOS_API_KEY=your_ecos_api_key_here
```

또는 환경변수 설정:

```bash
# Windows PowerShell
$env:ECOS_API_KEY="your_ecos_api_key_here"

# Linux/Mac
export ECOS_API_KEY="your_ecos_api_key_here"
```

#### **Step 2: Python 실행**

```bash
cd news_crawler
python ecos_crawler.py
```

**예상 출력**:

```
🚀 Starting ECOS Economic Indicators Crawler...
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
✅ Successfully collected 7/7 indicators
Done.
```

#### **Step 3: Firestore 확인**

Firebase Console → Firestore Database → `economic_indicators` 컬렉션 확인

---

### **옵션 3: 수동으로 컬렉션 생성** (임시 방법)

Firebase Console에서 직접 생성:

#### **Step 1: Firestore 접속**

```
Firebase Console → Firestore Database
```

#### **Step 2: 컬렉션 추가**

```
1. "컬렉션 시작" 클릭
2. 컬렉션 ID: economic_indicators
3. 첫 번째 문서 추가:
   - 문서 ID: base_rate
   - 필드:
     * id (string): "base_rate"
     * name (string): "기준금리"
     * value (number): 3.25
     * change_rate (number): 0.0
     * unit (string): "%"
     * type (string): "interest_rate"
     * source (string): "한국은행"
     * stat_code (string): "722Y001"
     * item_code (string): "0101000"
     * updated_at (timestamp): 현재 시간
     * captured_at (string): "2026-01-03T08:40:00Z"
```

**나머지 6개 지표도 동일하게 추가**:

- `treasury_3y` (국고채 3년)
- `treasury_10y` (국고채 10년)
- `cd_91d` (CD 91일)
- `usd_krw` (원/달러)
- `jpy_krw` (원/엔(100))
- `eur_krw` (원/유로)

---

## 📊 Firestore 데이터 구조

### **Collection: economic_indicators**

#### **Document: base_rate**

```json
{
  "id": "base_rate",
  "name": "기준금리",
  "value": 3.25,
  "change_rate": 0.0,
  "unit": "%",
  "type": "interest_rate",
  "source": "한국은행",
  "stat_code": "722Y001",
  "item_code": "0101000",
  "updated_at": "2026-01-03T08:40:00Z",
  "captured_at": "2026-01-03T08:40:00Z"
}
```

#### **Document: usd_krw**

```json
{
  "id": "usd_krw",
  "name": "원/달러",
  "value": 1320.5,
  "change_rate": -5.2,
  "unit": "원",
  "type": "exchange_rate",
  "source": "한국은행",
  "stat_code": "731Y001",
  "item_code": "0000001",
  "updated_at": "2026-01-03T08:40:00Z",
  "captured_at": "2026-01-03T08:40:00Z"
}
```

**나머지 5개 문서도 동일한 구조**

---

## 🔄 Android 앱 확인

컬렉션 생성 후 Android 앱에서 확인:

### **Step 1: 앱 실행**

```
MarketScreen 진입
→ Refresh 버튼 클릭
```

### **Step 2: 로그 확인**

```
Logcat에서 "MarketScreen" 필터:

✅ 성공 시:
✅ Loaded 7 indicators from Firestore

❌ 실패 시:
⚠️ No indicators found in Firestore
```

### **Step 3: UI 확인**

```
"주요 지표" 섹션에 7개 카드 표시:
- 기준금리: 3.25%
- 국고채 3년: 2.85%
- 국고채 10년: 3.10%
- CD 91일: 3.42%
- 원/달러: 1,320.5원
- 원/엔(100): 945.3원
- 원/유로: 1,425.8원
```

---

## 🎯 권장 순서

1. **ECOS API 키 발급** (5분)
   - <https://ecos.bok.or.kr/>
   - 회원가입 → 인증키 신청

2. **GitHub Secrets 설정** (1분)
   - Repository → Settings → Secrets
   - `ECOS_API_KEY` 추가

3. **GitHub Actions 실행** (3분)
   - Actions → Run workflow
   - 로그 확인

4. **Firestore 확인** (1분)
   - Firebase Console
   - `economic_indicators` 컬렉션 확인

5. **Android 앱 테스트** (1분)
   - MarketScreen 진입
   - 데이터 표시 확인

**총 소요 시간: 약 10분**

---

## 💡 문제 해결

### **Q: ECOS API 키가 없어요**

```
1. https://ecos.bok.or.kr/ 접속
2. 회원가입 (무료)
3. 로그인 후 "인증키 신청/관리"
4. 용도 입력 (예: "개인 앱 개발")
5. 발급받은 키 복사
```

### **Q: GitHub Actions 로그에 "ECOS_API_KEY not set"**

```
1. Repository → Settings → Secrets
2. ECOS_API_KEY 존재 확인
3. 없으면 추가
4. Actions 재실행
```

### **Q: 로컬 실행 시 "No Firebase credentials found"**

```
1. serviceAccountKey.json 파일 확인
2. news_crawler/ 폴더에 복사
3. 또는 프로젝트 루트에 복사
4. 재실행
```

### **Q: Firestore에 데이터가 없어요**

```
1. GitHub Actions 로그 확인
   → "✅ Saved 7 indicators" 메시지 확인
2. Firebase Console → Firestore Database
   → economic_indicators 컬렉션 확인
3. 없으면 로컬에서 직접 실행
   → python ecos_crawler.py
```

---

## 🚀 최종 확인

모든 설정 완료 후:

- [x] ECOS API 키 발급
- [x] GitHub Secrets 설정
- [x] GitHub Actions 실행
- [x] Firestore `economic_indicators` 컬렉션 생성
- [x] 7개 문서 (지표) 저장
- [x] Android 앱에서 데이터 표시

**Last Updated**: 2026-01-03
