# NOFEE 데이터 저장소

## 📋 개요

노피(NOFEE) 서비스의 모든 데이터를 체계적으로 수집, 저장, 분석하는 중앙 데이터 저장소입니다.

**최종 업데이트**: 2025-11-19
**버전**: 3.0
**목적**: 데이터 기반 의사결정, 회사소개서 작성, 성과 분석

---

## 📁 폴더 구조

```
nofee-data/
├── README.md                           # 이 문서
├── NoFee-광고데이터-2022-2025.csv      # Instagram 광고 실적 데이터
│
├── data/                               # 📊 모든 원본 데이터
│   ├── database/                       # MySQL DB 데이터
│   │   ├── db_data_latest.json        # 최신 비즈니스 데이터
│   │   ├── db_schema_latest.json      # 최신 DB 스키마
│   │   └── user_demographics_latest.json  # 고객 인구통계 (성별 분포)
│   ├── analytics/                      # Google Analytics 4 데이터
│   │   └── ga4_data_latest.json       # 최신 웹 트래픽 데이터
│   ├── products/                       # 상품/가격표/매장 데이터
│   │   └── product_store_data_latest.json
│   ├── codebase/                       # 코드 커밋 내역
│   │   ├── nofee-front-commits.json
│   │   └── nofee-springboot-commits.json
│   ├── deployments/                    # 배포 내역
│   │   ├── nofee-front-deployments.json
│   │   └── nofee-springboot-deployments.json
│   └── financial/                      # 재무 데이터
│       ├── 손익계산서.csv
│       └── docs/
│           ├── 김선호_자본금_계산식.md
│           └── 김선호_개인거래내역.md
│
├── reports/                            # 📈 분석 보고서 (JSON)
│   ├── application_funnel_analysis.json      # 견적신청 퍼널 분석
│   ├── service_version_analysis.json         # 서비스 버전별 성과 분석
│   ├── retention_analysis_latest.json        # 리텐션 분석 (최신)
│   └── comprehensive_summary.json             # 종합 데이터 요약
│
├── docs/                               # 📚 상세 문서 (Markdown)
│   ├── DATABASE_SCHEMA.md             # DB 스키마 상세 설명
│   ├── GA4_METRICS.md                 # GA4 지표 설명
│   ├── DATA_DICTIONARY.md             # 데이터 사전 (코드, 용어)
│   ├── APPLICATION_FUNNEL_ANALYSIS.md # 견적신청 퍼널 분석 문서
│   └── SERVICE_VERSION_HISTORY.md     # 서비스 버전 히스토리
│
└── scripts/                            # 🔧 데이터 수집/분석 스크립트
    ├── collectors/                     # 데이터 수집 스크립트
    │   ├── collect_db_data.py         # DB 데이터 수집
    │   ├── collect_db_schema.py       # DB 스키마 수집
    │   ├── collect_ga4_data.py        # GA4 데이터 수집
    │   └── collect_product_store_data.py  # 상품 데이터 수집
    ├── analyzers/                      # 데이터 분석 스크립트
    │   ├── analyze_application_funnel.py  # 퍼널 분석
    │   ├── analyze_service_version.py     # 버전 분석
    │   ├── analyze_retention.py           # 리텐션 분석
    │   └── analyze_comprehensive.py       # 종합 분석
    └── config/                         # 설정 파일
        ├── .env.example               # 환경변수 템플릿
        └── requirements.txt           # Python 의존성
```

---

## 📊 핵심 지표 요약 (2025-11-19 기준)

### 비즈니스 지표
- 총 가입자: **5,429명** (CAC: 515원)
- 총 신청: **8,873건** (견적 4,101 + 캠페인 4,772, CPA: 315원)
- 매장 구매 확정: **3,206건**
- 개통 완료: **240건** (개통당 비용: 11,654원)
- 등록 매장: **56개** (활성 11개)
- 실제 리뷰: **16건**

### 고객 인구통계
- **평균 연령**: 33.1세 (중앙값 32세)
- **연령대 분포**:
  - 20대: 1,989명 (37.2%)
  - 30대: 2,037명 (38.1%) ⭐ 최대
  - 40대: 882명 (16.5%)
  - 50대 이상: 308명 (5.8%)
  - 10대: 130명 (2.4%)
- **성별 분포**: 남성 4,772명 (89.2%) | 여성 578명 (10.8%)
- **핵심 타겟**: **30대 남성** (1,799명, 33.6%)
- **데이터 완성도**: 생년월일 99.9% | 성별 100%

### 상품/매장
- 활성 상품 그룹: **75개**
- 실제 판매중인 상품: **26개 기종**
- 총 가격표 항목: **779개**
- 서비스 커버리지: **9개 지역**

### 웹 트래픽 (최근 12개월)
- 총 세션: **59,831회**
- 총 사용자: **35,100명**

### 베타테스트 기간 (2025년 7-10월)
- 총 액티브 유저: **43,716명**
- 총 세션: **54,433회**
- 월평균 액티브 유저: **10,929명**
- 월평균 세션: **13,608회**
- 모바일 비율: **84.1%**

### 리텐션 (재방문율)
- **베타테스트 기간 리텐션율**: **24.17%**
- 총 재방문 유저: **10,566명**
- 월별 리텐션 향상:
  - 7월: 13.71%
  - 8월: 23.01%
  - 9월: 27.41%
  - 10월: 28.80%
  - 11월: 31.74%
- **4개월 만에 리텐션 2배 이상 향상** (13.71% → 28.80%)

### 마케팅 성과 (Instagram)
- 총 마케팅비: **2,797,014원** (6개 캠페인)
- 총 링크 클릭: **28,240회** (CPC: 99원)
- 총 도달: **166,721명** (CPM: 16,777원)
- **CAC (고객획득비용)**: **515원/명**
- **CPA (신청당 비용)**: **315원/건**
- **개통당 비용**: **11,654원/건**

### 전환율
- 신청→구매: **36.13%**
- 신청→개통: **2.70%**
- 구매→개통: **7.49%**

### 서비스 버전별 개통율
- **버전 1 (일반견적)**: 2.8% (2025-07 ~ 2025-08)
- **버전 2 (캠페인 견적 v1)**: 0.5% (2025-07 ~ 2025-08)
- **버전 3 (동네 견적 v2)**: **6.0%** (2025-09 ~ 현재)
  - 최고 기록: 15.3% (2025-10)
  - 버전 1/2 대비 개통율 **2배 이상 향상**

---

## 🔧 데이터 수집 방법

### 1. 환경 설정

```bash
# Python 의존성 설치
pip install -r scripts/config/requirements.txt

# 환경변수 설정
cp scripts/config/.env.example .env
# .env 파일 편집하여 DB 정보, GA4 정보 입력
```

### 2. 데이터 수집 실행

#### DB 데이터 수집
```bash
python3 scripts/collectors/collect_db_data.py
# → data/database/db_data_latest.json 생성
```

**수집 내용**:
- 핵심 비즈니스 지표 (가입자, 신청, 개통 등)
- 월별/일별 신청 추이
- 매장 퍼포먼스
- 실제 리뷰 데이터
- 전환율

#### GA4 데이터 수집
```bash
python3 scripts/collectors/collect_ga4_data.py
# → data/analytics/ga4_data_latest.json 생성
```

**수집 내용**:
- 전체 트래픽 지표 (최근 12개월)
- 디바이스 카테고리 분포
- 트래픽 채널 분석
- 월별 추이
- 상위 페이지
- 일평균/월평균 지표

#### 상품/매장 데이터 수집
```bash
python3 scripts/collectors/collect_product_store_data.py
# → data/products/product_store_data_latest.json 생성
```

**수집 내용**:
- 활성화된 상품 그룹
- 가격표 노출 매장
- 현재 판매중인 가격표
- 상품별/매장별/지역별 통계

### 3. 데이터 분석

#### 견적신청 퍼널 분석
```bash
python3 scripts/analyzers/analyze_application_funnel.py
# → reports/application_funnel_analysis.json 생성
```

**분석 내용**:
- 일반 견적 / 캠페인 견적 분석
- Step Code별 분포 (신청→견적→선택→예약→개통)
- 전체 퍼널 전환율
- 월별/일별 신청 추이
- 상품별 신청 통계

**상세 문서**: [docs/APPLICATION_FUNNEL_ANALYSIS.md](docs/APPLICATION_FUNNEL_ANALYSIS.md)

#### 서비스 버전별 개통율 분석
```bash
python3 scripts/analyzers/analyze_service_version.py
# → reports/service_version_analysis.json 생성
```

**분석 내용**:
- 버전 1 (일반견적): 2025-07 ~ 2025-08
- 버전 2 (캠페인 견적): 2025-07 ~ 2025-08
- 버전 3 (동네 견적): 2025-09 ~ 현재
- 버전별 개통율 비교 및 성과 분석

**상세 문서**: [docs/SERVICE_VERSION_HISTORY.md](docs/SERVICE_VERSION_HISTORY.md)

#### 종합 분석
```bash
python3 scripts/analyzers/analyze_comprehensive.py
# → reports/comprehensive_summary.json 생성
```

---

## 📚 상세 문서

모든 상세 문서는 [docs/](docs/) 폴더에 있습니다:

- **[DATABASE_SCHEMA.md](docs/DATABASE_SCHEMA.md)** - DB 스키마 상세 정보 (62개 테이블)
- **[GA4_METRICS.md](docs/GA4_METRICS.md)** - GA4 지표 설명 및 마케팅 ROI
- **[DATA_DICTIONARY.md](docs/DATA_DICTIONARY.md)** - 데이터 사전 (코드, 용어, 계산식)
- **[APPLICATION_FUNNEL_ANALYSIS.md](docs/APPLICATION_FUNNEL_ANALYSIS.md)** - 견적신청 퍼널 분석
- **[SERVICE_VERSION_HISTORY.md](docs/SERVICE_VERSION_HISTORY.md)** - 서비스 버전 히스토리

---

## 🗃️ 데이터 버전 관리

### 파일 네이밍 규칙

최신 데이터는 항상 `_latest` suffix를 사용합니다:
- `db_data_latest.json` - 최신 DB 데이터
- `ga4_data_latest.json` - 최신 GA4 데이터
- `product_store_data_latest.json` - 최신 상품 데이터

### 데이터 업데이트

```bash
# 모든 데이터를 한 번에 수집
python3 scripts/collectors/collect_db_data.py
python3 scripts/collectors/collect_ga4_data.py
python3 scripts/collectors/collect_product_store_data.py

# 모든 분석을 한 번에 실행
python3 scripts/analyzers/analyze_application_funnel.py
python3 scripts/analyzers/analyze_service_version.py
python3 scripts/analyzers/analyze_comprehensive.py
```

---

## 🔐 보안 및 접근 권한

### DB 접근
- Host: `43.203.125.223`
- Port: `3306`
- Database: `db_nofee`
- 접근 권한: CEO, CTO만 접근 가능
- 암호화: AES 암호화 (user_nm, tel_no 등)

### GA4 접근
- Service Account: `nofee-price-bot@nofee-price.iam.gserviceaccount.com`
- 권한 파일: `google_api_key.json` (루트 폴더)
- Property ID: 474694872

---

## 📈 주요 인사이트

### 마케팅 효율
- **CAC 515원**: MZ세대 타겟 서비스 업계 평균(1,000-3,000원) 대비 **매우 우수**
- **CPC 99원**: 업계 평균(300-500원) 대비 **80% 저렴**
- **CPA 315원**: 견적 신청까지 효율적 유도

### 서비스 성장
- **버전 3 개통율 6.0%**: 버전 1/2 대비 **2배 이상 향상**
- **10월 최고 기록 15.3%**: 지속 가능한 성장 가능성 입증
- **월평균 11,531세션**: 서비스 본격화 후 안정적 트래픽 확보

### 사용자 행동
- **모바일 84.1%**: MZ세대 타겟팅 성공
- **참여율 97%+**: 양질의 트래픽
- **이탈률 2.87%**: 콘텐츠 품질 우수

### 타겟 고객 특성
- **핵심 타겟층**: 30대 남성 (1,799명, 전체의 33.6%)
- **20-30대 집중**: 전체의 75.3% (4,026명)가 2030세대
- **평균 연령 33.1세**: MZ세대 중심의 디지털 네이티브 타겟
- **남성 중심**: 모든 연령대에서 남성 비율 85-90% 유지
- **여성 시장 기회**: 현재 10.8% → 마케팅 강화로 2배 성장 가능

---

## ✅ 변경 이력

### 2025-11-19 (v3.2) - 고객 인구통계 데이터 완전 분석
- **생년월일 복호화 성공**: HEX 중첩 AES 암호화 방식 확인
- **연령대 분석 완료**: 평균 33.1세, 30대 38.1% (최다), 20대 37.2%
- **핵심 타겟 확정**: 30대 남성 (1,799명, 전체의 33.6%)
- **20-30대 집중**: 전체의 75.3%가 2030세대
- **연령×성별 교차분석**: 모든 연령대에서 남성 85-90% 유지
- **데이터 완성도**: 생년월일 99.9% (5,346/5,350명)

### 2025-11-19 (v3.0) - 폴더 구조 전면 개편
- 폴더 구조 단순화 (1-raw-data → data, 5-docs → docs 등)
- 중복 파일 제거, 최신 데이터만 유지
- `_latest` suffix로 최신 파일 명확화
- Instagram 광고 실제 데이터 추가
- CAC, CPA, 개통당 비용 지표 추가
- 월평균 데이터로 전환 (일평균 → 월평균)

### 2025-11-19 (v2.0)
- 상품/가격표/매장 데이터 수집 기능 추가
- 통합 분석 스크립트 추가
- 서비스 버전별 개통율 분석 추가

### 2025-11-04 (v1.1)
- GA4 데이터 수집 기능 추가
- DB 데이터 수집 스크립트 개선

### 2025-10-23 (v1.0)
- 초기 nofee-data 폴더 구조 생성
- DB 데이터 수집 스크립트 작성

---

**관리자**: 김선호 (CEO)
**최종 업데이트**: 2025-11-19
**데이터 정책**: 월 1회 정기 업데이트, 주요 이벤트 시 수시 업데이트
