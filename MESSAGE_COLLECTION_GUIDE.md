# Make 앱 - 메시지 수집 가이드

## 📱 수집되는 메시지 유형

### 1. 메신저 앱 (Messenger)

#### ✅ 카카오톡 (KakaoTalk)

- **패키지명**: `com.kakao.talk`
- **수집 방식**: 알림(Notification) 감지
- **수집 내용**:
  - 발신자 이름 (EXTRA_TITLE)
  - 메시지 내용 (EXTRA_TEXT)
  - 채팅방 이름 (EXTRA_SUB_TEXT, 그룹채팅인 경우)
- **제한사항**:
  - 알림에 표시되는 미리보기 텍스트만 수집 가능
  - 카카오톡 설정에서 "알림 미리보기"가 꺼져있으면 내용 수집 불가
  - 이미지, 파일, 이모티콘은 수집 불가

#### ✅ 라인 (LINE)

- **패키지명**: `jp.naver.line.android`
- **수집 방식**: 알림 감지
- **수집 내용**: 카카오톡과 동일

#### ✅ 슬랙 (Slack)

- **패키지명**: `com.slack`
- **수집 방식**: 알림 감지
- **수집 내용**: 카카오톡과 동일

#### ✅ 텔레그램 (Telegram)

- **패키지명**: `org.telegram.messenger`
- **수집 방식**: 알림 감지
- **수집 내용**: 카카오톡과 동일

---

### 2. 이메일 앱 (Email)

#### ✅ Gmail

- **패키지명**: `com.google.android.gm`
- **수집 방식**: 알림 감지
- **수집 내용**:
  - 발신자 이메일 주소
  - 이메일 제목
  - 본문 미리보기 (첫 몇 줄)
- **제한사항**:
  - 전체 본문은 Gmail API 연동 필요
  - 첨부파일 정보는 수집 불가

#### ✅ 삼성 이메일

- **패키지명**: `com.samsung.android.email.provider`
- **수집 방식**: 알림 감지
- **수집 내용**: Gmail과 동일

---

### 3. SMS/문자 메시지

#### ✅ 기본 SMS 앱

- **수집 방식**: SMS ContentProvider 직접 읽기
- **필요 권한**: `READ_SMS`
- **수집 내용**:
  - 발신자 전화번호
  - 메시지 전체 내용
  - 수신 시간
- **제한사항**:
  - Android 4.4 이상에서만 정상 작동
  - 일부 제조사 커스텀 ROM에서 제한 가능

---

## 🔐 필요한 권한

### 1. 알림 접근 권한 (Notification Listener)

```
설정 → 알림 → 알림 접근 → Make 앱 활성화
```

**필수 대상**:

- 카카오톡
- 라인
- 슬랙
- 텔레그램
- Gmail
- 삼성 이메일

### 2. SMS 읽기 권한

```xml
<uses-permission android:name="android.permission.READ_SMS" />
```

**필수 대상**:

- SMS/문자 메시지

---

## 📊 수집 데이터 구조

### NotificationListenerService가 수집하는 정보

```kotlin
// 카카오톡 알림 예시
{
  packageName: "com.kakao.talk",
  extras: {
    EXTRA_TITLE: "홍길동",           // 발신자
    EXTRA_TEXT: "안녕하세요!",       // 메시지 내용
    EXTRA_SUB_TEXT: "프로젝트 팀"   // 채팅방 이름 (그룹채팅)
  },
  postTime: 1735560000000,          // 수신 시간
  key: "0|com.kakao.talk|123|null|10456" // 고유 키
}
```

### 데이터베이스 저장 형식

```kotlin
IntelligenceEntity(
  id = "noti_0|com.kakao.talk|123|null|10456",
  sourceType = SourceType.MESSENGER,
  sourceId = "0|com.kakao.talk|123|null|10456",
  rawContent = "안녕하세요!",
  summary = "안녕하세요!",
  priority = ItemPriority.NORMAL,
  capturedAt = 1735560000000,
  senderOrSource = "프로젝트 팀 (홍길동)",
  type = "KAKAOTALK"
)
```

---

## ⚠️ 제한사항 및 주의사항

### 1. 알림 기반 수집의 한계

#### 수집 가능한 것 ✅

- 알림에 표시되는 텍스트
- 발신자 이름/번호
- 수신 시간
- 앱 종류

#### 수집 불가능한 것 ❌

- 이미지, 동영상, 파일
- 이모티콘, 스티커
- 전체 대화 내역
- 읽음/안읽음 상태
- 메시지 삭제 여부

### 2. 카카오톡 특수 케이스

#### 알림 미리보기 설정

```
카카오톡 → 설정 → 알림 → 메시지 알림 → 알림 미리보기
```

- **켜짐**: 메시지 내용 수집 가능 ✅
- **꺼짐**: "새 메시지가 도착했습니다" 만 수집 ❌

#### 단체 채팅방

```
수집 형식: "채팅방이름 (발신자)"
예시: "프로젝트 팀 (홍길동)"
```

#### 1:1 채팅

```
수집 형식: "발신자"
예시: "홍길동"
```

### 3. 중복 방지 메커니즘

```kotlin
// 알림 고유 키로 중복 체크
val uniqueId = "noti_${sbn.key}"
val existing = dao.getItemById(uniqueId)
if (existing != null) {
    return // 이미 수집됨, 스킵
}
```

**중복이 발생할 수 있는 경우**:

- 같은 메시지가 여러 번 알림될 때
- 알림 업데이트 (예: 메시지 개수 증가)
- 앱 재시작 시 기존 알림 재처리

**해결 방법**:

- 알림 고유 키(`sbn.key`) 사용
- DB에 저장 전 중복 체크

---

## 🔄 실시간 수집 프로세스

```
1. 카카오톡에서 메시지 수신
   ↓
2. Android 시스템이 알림 생성
   ↓
3. MakNotificationService.onNotificationPosted() 호출
   ↓
4. 패키지명 확인 (com.kakao.talk)
   ↓
5. 알림 데이터 추출
   - EXTRA_TITLE: 발신자
   - EXTRA_TEXT: 메시지
   - EXTRA_SUB_TEXT: 채팅방
   ↓
6. 중복 체크 (sbn.key)
   ↓
7. Room DB에 저장
   ↓
8. MessengerScreen에 실시간 표시
```

---

## 📱 사용자 설정 가이드

### 1단계: 알림 접근 권한 허용

```
1. Make 앱 실행
2. 메신저 탭 또는 이메일 탭 선택
3. "알림 접근 권한 필요" 배너 표시
4. "권한 허용하기" 버튼 클릭
5. 설정 화면에서 "Make" 찾기
6. 토글 ON
```

### 2단계: 카카오톡 알림 설정 확인

```
1. 카카오톡 앱 열기
2. 설정 → 알림
3. "메시지 알림" ON 확인
4. "알림 미리보기" ON 확인 (중요!)
```

### 3단계: SMS 권한 허용 (선택)

```
1. Make 앱 실행
2. 문자 탭 선택
3. "SMS 읽기 권한 필요" 배너 표시
4. "권한 허용하기" 버튼 클릭
5. "허용" 선택
```

---

## 🛠️ 개발자 정보

### 지원 추가 방법

새로운 메신저 앱을 추가하려면:

```kotlin
// MakNotificationService.kt
val sourcePackage = when (packageName) {
    "com.kakao.talk" -> "KAKAOTALK"
    "jp.naver.line.android" -> "LINE"
    "com.slack" -> "SLACK"
    "org.telegram.messenger" -> "TELEGRAM"
    "새로운.앱.패키지명" -> "새로운앱이름"  // ← 여기에 추가
    else -> return
}
```

### 패키지명 확인 방법

```bash
# ADB로 설치된 앱 패키지명 확인
adb shell pm list packages | grep 앱이름

# 예시: 카카오톡
adb shell pm list packages | grep kakao
# 결과: package:com.kakao.talk
```

---

## 📊 현재 지원 현황

| 앱 종류 | 앱 이름 | 패키지명 | 수집 방식 | 상태 |
|---------|---------|----------|-----------|------|
| 메신저 | 카카오톡 | com.kakao.talk | 알림 | ✅ |
| 메신저 | 라인 | jp.naver.line.android | 알림 | ✅ |
| 메신저 | 슬랙 | com.slack | 알림 | ✅ |
| 메신저 | 텔레그램 | org.telegram.messenger | 알림 | ✅ |
| 이메일 | Gmail | com.google.android.gm | 알림 | ✅ |
| 이메일 | 삼성 이메일 | com.samsung.android.email.provider | 알림 | ✅ |
| 문자 | SMS | - | ContentProvider | ✅ |

---

## 🔮 향후 개선 계획

### 1. API 기반 전체 내용 수집

- Gmail API: 전체 이메일 본문
- Slack API: 전체 대화 내역
- Telegram API: Bot을 통한 메시지 수집

### 2. 추가 메신저 지원

- WhatsApp
- Facebook Messenger
- Discord
- Microsoft Teams

### 3. 고급 필터링

- 특정 발신자만 수집
- 키워드 기반 필터링
- 중요도 자동 분류

### 4. AI 분석

- 메시지 요약
- 감정 분석
- 자동 답장 제안
- 우선순위 자동 설정
