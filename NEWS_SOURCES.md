# 스마트 뉴스피드 수집 원천 및 분석 대상 (Smart News Feed Sources)

Make 앱의 인공지능(AI) 엔진은 전 세계 주요 경제 미디어와 데이터를 실시간으로 수집하고 분석하여 사용자에게 제공합니다. 현재 연결된 데이터 파이프라인과 감지 대상은 다음과 같습니다.

## 1. 실시간 뉴스 데이터 수집 채널 (Real-time Data Pipelines)

현재 `RssFetcher` 엔진이 8개의 글로벌 핵심 채널을 병렬로 모니터링하고 있습니다.

| 기관명 (Agency) | 카테고리 | 주요 다루는 내용 (Coverage) | 수집 방식 |
| :--- | :--- | :--- | :--- |
| **Wall Street Journal (WSJ)** | 🏛️ 금융/시장 | 미국 증시 핵심, 기관 투자자 동향, M&A | RSS (Markets) |
| **CNBC Economy** | 🌏 거시경제 | 연준(Fed) 금리 정책, CPI/PPI 지표, 글로벌 경제 이슈 | RSS (Economy) |
| **CNBC Tech** | 🤖 기술/성장주 | AI, 반도체, 빅테크 기업 실적 및 신제품 발표 | RSS (Tech) |
| **MarketWatch** | 📈 실시간 시황 | 장중 특이 종목, 급등락 원인 분석, 프리마켓 동향 | RSS (Top Stories) |
| **Reuters Business** | 🌐 국제 비즈니스 | 유럽/아시아 시장, 원유/에너지, 글로벌 공급망 이슈 | RSS (Business) |
| **Investing.com** | 💱 원자재/환율 | 금, 달러 인덱스, 국채 금리 등 매크로 지표 | RSS (News) |
| **CoinDesk** | 🪙 암호화폐 | 비트코인, 이더리움 시세, 블록체인 규제 및 기술 | RSS (Crypto) |
| **TechCrunch** | 🚀 스타트업 | 벤처 캐피탈(VC) 투자, 유니콘 기업, 신기술 트렌드 | RSS (Feed) |

---

## 2. AI 분석 인플루언서 및 키맨 (Influencers & Key Men)

AI(Gemini 1.5 Flash)가 수집된 뉴스 텍스트를 정밀 분석하여, 아래 주요 인물들의 발언이 포함된 경우 별도의 **'Influencer'** 태그나 **'Author Handle'**로 식별합니다.

| 구분 | 주요 감지 인물 (예시) | 분석 포인트 |
| :--- | :--- | :--- |
| **Policy Makers** | **Jerome Powell** (Fed 의장)<br>**Janet Yellen** (재무장관) | 금리 결정 힌트, 통화 정책 방향성 |
| **Big Tech CEOs** | **Jensen Huang** (NVIDIA)<br>**Elon Musk** (Tesla/SpaceX)<br>**Tim Cook** (Apple) | 미래 기술 전망, 기업 가이던스, 신제품 힌트 |
| **Investment Gurus** | **Warren Buffett** (Berkshire)<br>**Michael Burry** (Scion)<br>**Ray Dalio** (Bridgewater) | 시장 버블 경고, 가치 투자 철학, 포트폴리오 변화 |

---

## 3. 데이터 처리 및 보안 (Processing & Security)

1. **수집 (Ingestion)**: 8개 채널 동시 연결 (Parallel Fetching)로 지연 시간 최소화.
2. **정제 (Cleaning)**: 중복 기사 제거 및 광고 필터링.
3. **AI 검증 (Verification)**:
    * **Fact-Check**: `sourceUrl` 원본 대조.
    * **Sentiment**: 긍정/부정/중립 감성 분석.
    * **Impact Score**: 시장에 미칠 영향도(1~10) 산출.
4. **저장 (Persistence)**: 사용자 기기 내부(Room DB)에 암호화되어 저장 (외부 유출 없음).

> *이 문서는 시스템 업데이트에 따라 수집 대상이 변경될 수 있습니다. (Last Updated: 2026-01-02)*
