# 노피 Google Analytics 4 (GA4) 메트릭 문서

**작성일**: 2025-11-19
**Property ID**: 474694872
**목적**: 노피 웹사이트 트래픽 및 사용자 행동 분석

---

## 📋 목차

1. [개요](#개요)
2. [핵심 메트릭](#핵심-메트릭)
3. [차원 (Dimensions)](#차원-dimensions)
4. [이벤트 추적](#이벤트-추적)
5. [데이터 수집 방법](#데이터-수집-방법)

---

## 개요

노피는 Google Analytics 4 (GA4)를 사용하여 웹사이트 트래픽 및 사용자 행동을 추적합니다.

### Property 정보
- **Property ID**: 474694872
- **Data Stream**: nofee.team (웹)
- **수집 기간**: 2020-01-01 ~ 현재
- **시간대**: Asia/Seoul (KST)

### 핵심 성과 (최근 12개월, 2025-11-19 기준)
- **총 세션**: 59,831회
- **총 사용자**: 35,100명
- **페이지뷰**: 330,916회
- **월평균 세션**: 11,531회 (서비스 본격화 기준: 2025년 7-11월)
- **월평균 사용자**: 7,607명 (서비스 본격화 기준: 2025년 7-11월)
- **월평균 페이지뷰**: 66,255회 (서비스 본격화 기준: 2025년 7-11월)

---

## 핵심 메트릭

### 1. 사용자 메트릭

#### `totalUsers` (총 사용자 수)
- **설명**: 웹사이트를 방문한 고유 사용자 수
- **측정 방법**: Google Analytics 클라이언트 ID 기반
- **최근 12개월**: 35,100명
- **월평균**: 7,607명 (2025년 7-11월 기준)

#### `newUsers` (신규 사용자 수)
- **설명**: 처음 방문한 사용자 수
- **특징**: GA4 쿠키 기반 식별

#### `activeUsers` (활성 사용자 수)
- **설명**: 10초 이상 참여한 사용자
- **GA4 특징**: UA의 "사용자"와 유사하지만 더 엄격한 기준

### 2. 세션 메트릭

#### `sessions` (세션 수)
- **설명**: 웹사이트 방문 세션 수
- **최근 12개월**: 59,831회
- **월평균**: 11,531회 (2025년 7-11월 기준)
- **세션 정의**: 30분 비활동 후 새 세션

#### `averageSessionDuration` (평균 세션 시간)
- **설명**: 세션당 평균 체류 시간
- **최근 12개월**: 185초 (약 3.1분)
- **의미**: 높을수록 콘텐츠 참여도 높음

#### `sessionsPerUser` (사용자당 세션 수)
- **설명**: 사용자 1인당 평균 세션 수
- **계산**: sessions / totalUsers
- **최근 12개월**: 1.7회

### 3. 페이지뷰 메트릭

#### `screenPageViews` (페이지뷰)
- **설명**: 총 페이지 조회 수
- **최근 12개월**: 330,916회
- **월평균**: 66,255회 (2025년 7-11월 기준)

#### `screenPageViewsPerSession` (세션당 페이지뷰)
- **설명**: 세션 1회당 평균 페이지뷰
- **계산**: screenPageViews / sessions
- **최근 12개월**: 5.5페이지
- **의미**: 높을수록 사이트 탐색 활발

### 4. 참여도 메트릭

#### `engagementRate` (참여율)
- **설명**: 참여 세션 비율
- **정의**: 10초 이상 OR 전환 이벤트 OR 2개 이상 페이지뷰
- **최근 12개월**: 97%+
- **의미**: 매우 높은 참여도

#### `bounceRate` (이탈률)
- **설명**: 10초 미만 + 전환 없음 + 1페이지만 본 비율
- **GA4 특징**: UA와 계산 방식 다름 (더 엄격)
- **최근 12개월**: 2.87%
- **의미**: 매우 낮은 이탈률 (양질의 트래픽)

#### `userEngagementDuration` (사용자 참여 시간)
- **설명**: 실제로 페이지에 머문 시간 (포커스 상태)
- **특징**: 백그라운드 탭 제외

---

## 차원 (Dimensions)

### 1. 디바이스 (`deviceCategory`)

노피 사용자의 디바이스 분포:

| 디바이스 | 세션 비율 | 특징 |
|---------|----------|------|
| **mobile** | **84.1%** | 모바일 최적화 필수 |
| desktop | 14.0% | - |
| tablet | 1.9% | - |

**인사이트**: 모바일 중심 MZ세대 타겟팅 성공

### 2. 트래픽 소스 (`sessionDefaultChannelGroup`)

| 채널 | 설명 | 특징 |
|------|------|------|
| Paid Social | 유료 소셜 미디어 광고 | Facebook, Instagram 등 |
| Direct | 직접 유입 | 북마크, 직접 URL 입력 |
| Organic Search | 자연 검색 | Google, Naver 등 |
| Referral | 외부 링크 | 다른 웹사이트에서 유입 |
| Organic Social | 자연 소셜 | 무료 소셜 미디어 |

#### 주요 채널 성과 (최근 12개월)

**Paid Social** (주력 채널):
- 세션: 24,523회 (전체의 41.0%)
- 사용자: 17,263명
- 참여율: 99.78%
- **Instagram 마케팅비**: 2,797,014원 (누적)
- **CPS (세션당 비용)**: 114원
- **CPU (사용자당 비용)**: 162원

### 3. 시간 차원

#### `date` (날짜)
- **형식**: YYYYMMDD
- **용도**: 일별 추이 분석

#### `yearMonth` (년월)
- **형식**: YYYYMM
- **용도**: 월별 추이 분석

#### `dayOfWeek` (요일)
- **값**: 0 (일요일) ~ 6 (토요일)
- **용도**: 요일별 패턴 분석

### 4. 페이지 차원

#### `pagePath` (페이지 경로)
- **설명**: 방문한 페이지 URL
- **예시**: `/`, `/campaign`, `/store`

#### `pageTitle` (페이지 제목)
- **설명**: 페이지 타이틀
- **용도**: 페이지 성과 분석

---

## 이벤트 추적

GA4는 이벤트 기반 측정 모델 사용:

### 자동 수집 이벤트

#### `page_view`
- **설명**: 페이지 조회
- **파라미터**: page_location, page_title

#### `session_start`
- **설명**: 새 세션 시작
- **용도**: 세션 수 계산

#### `user_engagement`
- **설명**: 사용자 참여 (포커스 상태)
- **용도**: 참여 시간 측정

### 커스텀 이벤트

노피 서비스 특화 이벤트 (추정):

#### `estimate_submit` (견적 신청)
- **설명**: 고객이 견적 신청 완료
- **파라미터**:
  - product_group: 상품 그룹
  - carrier: 통신사
  - join_type: 가입 유형

#### `store_select` (판매점 선택)
- **설명**: 동네성지에서 판매점 선택
- **파라미터**:
  - store_no: 판매점 번호
  - region: 지역

#### `price_compare` (가격 비교)
- **설명**: 가격 비교 기능 사용
- **파라미터**:
  - product_count: 비교 상품 수

---

## 데이터 수집 방법

### Google Analytics Data API 사용

#### 인증
```python
from google.analytics.data_v1beta import BetaAnalyticsDataClient
from google.oauth2 import service_account

credentials = service_account.Credentials.from_service_account_file(
    'google_api_key.json'
)

client = BetaAnalyticsDataClient(credentials=credentials)
```

#### Service Account
- **계정**: `nofee-price-bot@nofee-price.iam.gserviceaccount.com`
- **키 파일**: `google_api_key.json` (루트 폴더)
- **권한**: 뷰어 (Viewer)

### 데이터 수집 스크립트

```bash
python3 3-scripts/collectors/collect_ga4_data_latest.py
```

### API Request 예시

```python
from google.analytics.data_v1beta.types import (
    RunReportRequest,
    DateRange,
    Metric,
    Dimension,
)

request = RunReportRequest(
    property=f"properties/474694872",
    date_ranges=[DateRange(start_date="2024-11-19", end_date="2025-11-19")],
    metrics=[
        Metric(name="sessions"),
        Metric(name="totalUsers"),
        Metric(name="screenPageViews"),
    ],
    dimensions=[
        Dimension(name="date"),
    ],
)

response = client.run_report(request)
```

---

## 주요 보고서

### 1. 전체 트래픽 보고서 (Overall Metrics)

**기간**: 최근 12개월 (2024-11-19 ~ 2025-11-19)

| 메트릭 | 전체 기간 | 월평균 (2025년 7-11월) |
|--------|----------|----------------------|
| 총 세션 | 59,831회 | 11,531회 |
| 총 사용자 | 35,100명 | 7,607명 |
| 페이지뷰 | 330,916회 | 66,255회 |
| 평균 세션 시간 | 185초 (3.1분) | - |
| 이탈률 | 2.87% | - |
| 참여율 | 97%+ | - |

**주목**: 서비스 본격화 시점(2025년 7-11월) 월평균 데이터 기준

### 2. 디바이스별 보고서

| 디바이스 | 세션 | 사용자 | 세션 비율 |
|---------|------|--------|----------|
| mobile | 50,098회 | 30,633명 | 84.1% |
| desktop | 8,332회 | 3,870명 | 14.0% |
| tablet | 1,158회 | 800명 | 1.9% |

### 3. 월별 추이 보고서

```python
# 수집 스크립트 실행 시 자동 생성
# 파일: 1-raw-data/analytics/ga4_data_YYYYMMDD_HHMMSS.json
```

---

## 분석 인사이트

### 강점
1. **높은 참여율 (97%+)**: 양질의 트래픽
2. **낮은 이탈률 (2.87%)**: 콘텐츠 품질 우수
3. **높은 페이지뷰/세션 (5.5)**: 적극적인 탐색
4. **모바일 최적화 성공 (84%)**: MZ세대 타겟팅 성공
5. **효율적인 Instagram 마케팅**: CPS 114원, CPU 162원 (매우 낮은 수준)

### 개선 포인트
1. **세션당 평균 시간 (3.1분)**: 콘텐츠 확장 필요
2. **사용자당 세션 (1.7)**: 재방문 유도 강화
3. **전환 추적**: 신청 → 개통 퍼널 GA4 연동

### 마케팅 성과

#### Instagram 마케팅 ROI (실제 데이터)
- **총 마케팅비**: 2,797,014원 (누적)
- **총 캠페인 수**: 6개
- **총 링크 클릭**: 28,240회
- **총 도달**: 166,721명
- **CPC (클릭당 비용)**: 99원
- **CPM (1,000명 도달 비용)**: 16,777원

#### 비즈니스 지표
- **총 가입자**: 5,429명 (DB 기준)
- **CAC (고객획득비용)**: 515원/명
- **총 견적 신청**: 8,873건
- **CPA (신청당 비용)**: 315원/건
- **총 개통 완료**: 240건
- **개통당 비용**: 11,654원/건

**평가**:
- CPC 99원은 업계 평균(300-500원) 대비 **80% 저렴**
- CAC 515원은 MZ세대 타겟 서비스 평균(1,000-3,000원) 대비 **매우 우수**
- CPA 315원으로 **견적 신청까지 효율적으로 유도**

---

## 데이터 활용

### 마케팅 최적화
- 모바일 광고 우선 집행
- Paid Social 채널 강화
- 이탈률 낮은 페이지 분석

### 서비스 개선
- 모바일 UX 지속 개선
- 페이지뷰 높은 콘텐츠 확대
- 참여도 낮은 페이지 리뉴얼

---

## 참고 자료

- [Google Analytics Data API 문서](https://developers.google.com/analytics/devguides/reporting/data/v1)
- [GA4 이벤트 참조](https://support.google.com/analytics/answer/9322688)
- [nofee-data 수집 스크립트](../3-scripts/collectors/collect_ga4_data_latest.py)

---

**작성자**: Data Team
**최종 업데이트**: 2025-11-19
**버전**: 2.0
