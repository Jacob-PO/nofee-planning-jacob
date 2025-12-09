# NOFEE Planning

노피(NOFEE) 서비스의 **전략 기획, 데이터 분석, 마케팅 기획**을 위한 통합 저장소입니다.

**최종 업데이트**: 2025-12-09
**버전**: 3.1
**관리자**: 김선호 (CEO)

---

## 📋 목차

1. [프로젝트 개요](#-프로젝트-개요)
2. [폴더 구조](#-폴더-구조)
3. [주요 프로젝트](#-주요-프로젝트)
4. [핵심 데이터 요약](#-핵심-데이터-요약)
5. [사용 방법](#-사용-방법)
6. [기술 스택](#-기술-스택)

---

## 🎯 프로젝트 개요

### nofee_planning이란?

노피 서비스의 **비즈니스 성장**을 위한 모든 기획, 분석, 데이터 작업을 관리하는 중앙 저장소입니다.

### 주요 목적

1. **데이터 기반 의사결정**: 실시간 비즈니스 데이터 수집 및 분석
2. **회사소개서/투자자료**: 투자유치 및 파트너십을 위한 자료 작성
3. **마케팅 기획**: 마케팅 전략 수립 및 성과 분석
4. **경쟁 분석**: 경쟁사 및 시장 동향 파악

---

## 📁 폴더 구조

```
nofee_planning/
├── README.md                          # 본 문서
├── index.html                         # 프로젝트 대시보드 (선택사항)
│
├── nofee-data/                        # 📊 데이터 저장소 (핵심!)
│   ├── data/                          # 모든 원본 데이터
│   │   ├── database/                  # MySQL DB 데이터
│   │   ├── analytics/                 # Google Analytics 4
│   │   ├── products/                  # 상품/매장 데이터
│   │   ├── codebase/                  # Git 커밋 내역
│   │   ├── deployments/               # 배포 내역
│   │   └── financial/                 # 재무 데이터
│   ├── reports/                       # 분석 보고서
│   ├── docs/                          # 상세 문서
│   │   ├── DATABASE_SCHEMA.md        # DB 스키마
│   │   ├── GA4_METRICS.md            # GA4 지표
│   │   ├── DATA_DICTIONARY.md        # 데이터 사전
│   │   ├── APPLICATION_FUNNEL_ANALYSIS.md
│   │   └── SERVICE_VERSION_HISTORY.md
│   ├── scripts/                       # 수집/분석 스크립트
│   │   ├── collectors/               # 데이터 수집
│   │   └── analyzers/                # 데이터 분석
│   ├── NoFee-광고데이터-2022-2025.csv # Instagram 광고 실적
│   └── README.md                      # nofee-data 상세 문서
│
├── nofee-marketing/                   # 📣 마케팅 자료
│   ├── contents/                      # 콘텐츠 자료
│   │   └── company-introduction/     # 회사소개서 데이터
│   │       └── README.md             # 회사소개서 완전판
│   └── strategy/                      # 마케팅 전략
│
├── nofee-crawler/                     # 🕷️ 크롤러
│   ├── workspace-fee-crawler/        # 요금제 크롤러
│   └── workspace-sales-crawler/      # 판매점 크롤러
│
└── docs/                              # 📚 기획 문서
    ├── 서비스_개선_제안.md
    ├── 경쟁사_분석.md
    └── 기타 전략 문서들...
```

---

## 🚀 주요 프로젝트

### 1. nofee-data (데이터 중앙 저장소) ⭐

**위치**: [`nofee-data/`](nofee-data/)

**설명**: 노피 서비스의 **모든 실제 데이터**를 체계적으로 수집, 저장, 분석하는 중앙 저장소

**주요 기능**:
- MySQL DB 데이터 수집 (비즈니스 지표)
- Google Analytics 4 데이터 수집 (웹 트래픽)
- 상품/매장/가격표 데이터 수집
- Instagram 광고 성과 분석
- 견적신청 퍼널 분석
- 서비스 버전별 개통율 분석

**핵심 데이터**:
- 총 가입자: **5,429명** (CAC: 515원)
- 총 신청: **8,873건** (CPA: 315원)
- 개통 완료: **240건** (개통당 비용: 11,654원)
- 웹 트래픽: **월평균 11,531세션** (2025년 7-11월)

**문서**: [nofee-data/README.md](nofee-data/README.md)

---

### 2. nofee-marketing (마케팅 자료)

**위치**: [`nofee-marketing/`](nofee-marketing/)

**설명**: 마케팅 콘텐츠 및 전략 자료

#### 2-1. company-introduction (회사소개서)

**위치**: [`nofee-marketing/contents/company-introduction/`](nofee-marketing/contents/company-introduction/)

**설명**: 투자유치 및 파트너십용 **회사소개서 완전판**

**주요 내용**:
- 📊 상세 비즈니스 지표 (실제 데이터)
- 🌐 웹 트래픽 및 마케팅 성과
- 💪 경쟁 우위 분석
- 📈 성장 지표 및 추이
- 🎯 투자 포인트
- 🚀 향후 성장 전략
- 🏆 30초/1분/5분 피치

**핵심 메시지**:
> "노피는 MZ세대를 위한 **No-Fee 휴대폰 개통 플랫폼**입니다.
> **5,429명의 실제 고객**과 **56개 판매점**이 증명하는 검증된 비즈니스 모델입니다."

**문서**: [company-introduction/README.md](nofee-marketing/contents/company-introduction/README.md)

---

### 3. nofee-crawler (데이터 크롤러)

**위치**: [`nofee-crawler/`](nofee-crawler/)

**설명**: 경쟁사 및 시장 데이터 수집

#### 3-1. workspace-fee-crawler
- 통신사 요금제 정보 크롤링
- 가격 비교 데이터 수집

#### 3-2. workspace-sales-crawler
- 판매점 정보 크롤링
- 경쟁사 가격 정보 수집

---

### 4. docs (기획 문서)

**위치**: [`docs/`](docs/)

**설명**: 서비스 기획, 전략, 분석 문서

**주요 문서**:
- 서비스 개선 제안
- 경쟁사 분석
- 시장 조사
- 기능 기획서
- 프로젝트 계획

---

## 📊 핵심 데이터 요약 (2025-11-19 기준)

### 비즈니스 지표
| 지표 | 수치 | 비고 |
|------|------|------|
| 총 가입자 | **5,429명** | CAC: 515원 |
| 총 견적 신청 | **8,873건** | CPA: 315원 |
| 매장 구매 확정 | **3,206건** | 전환율 36.13% |
| 개통 완료 | **240건** | 개통당 비용: 11,654원 |
| 파트너 매장 | **56개** | 활성 11개 |
| 실제 리뷰 | **16건** | 가상 리뷰 제외 |

### 웹 트래픽 (최근 12개월)
| 지표 | 수치 | 비고 |
|------|------|------|
| 총 세션 | **59,831회** | - |
| 총 사용자 | **35,100명** | - |
| **월평균 세션** | **11,531회** | 2025년 7-11월 기준 |
| **월평균 사용자** | **7,607명** | 2025년 7-11월 기준 |
| 모바일 비율 | **84.1%** | MZ세대 타겟팅 성공 |
| 이탈률 | **2.87%** | 매우 낮음 ⭐ |
| 참여율 | **97%+** | 매우 높음 ⭐ |

### 마케팅 성과 (Instagram)
| 지표 | 수치 | 업계 비교 |
|------|------|----------|
| 총 마케팅비 | **2,797,014원** | - |
| 총 링크 클릭 | **28,240회** | - |
| CPC (클릭당 비용) | **99원** | 업계 평균 대비 80% 저렴 ⭐ |
| **CAC** (고객획득비용) | **515원** | 업계 평균 대비 50-80% 저렴 ⭐ |
| **CPA** (신청당 비용) | **315원** | 매우 효율적 ⭐ |
| 개통당 비용 | **11,654원** | 효율적 |

### 서비스 버전별 개통율 (핵심 차별점!)
| 버전 | 기간 | 개통율 | 특징 |
|------|------|--------|------|
| 버전 1: 일반견적 | 2025-07 ~ 08 | 2.8% | 전국 평균가, 노피 중개 |
| 버전 2: 캠페인 v1 | 2025-07 ~ 08 | 0.5% | 검색 없음 (실패) |
| **버전 3: 동네성지 v2** | 2025-09 ~ 현재 | **6.0%** | 검색 기능, VOC 반영 ⭐ |

**버전 3 최고 기록**: **15.3%** (2025년 10월)
**개선율**: 버전 2 대비 **12배**, 버전 1 대비 **2.1배** 향상

---

## 🛠️ 사용 방법

### 1. 데이터 수집 (nofee-data)

```bash
cd nofee-data

# 환경 설정
pip install -r scripts/config/requirements.txt
cp scripts/config/.env.example .env
# .env 파일 편집 (DB 정보, GA4 정보 입력)

# 데이터 수집
python3 scripts/collectors/collect_db_data.py         # DB 데이터
python3 scripts/collectors/collect_ga4_data.py        # GA4 데이터
python3 scripts/collectors/collect_product_store_data.py  # 상품 데이터

# 데이터 분석
python3 scripts/analyzers/analyze_application_funnel.py   # 퍼널 분석
python3 scripts/analyzers/analyze_service_version.py      # 버전 분석
python3 scripts/analyzers/analyze_comprehensive.py        # 종합 분석
```

**결과 파일**:
- `data/database/db_data_latest.json`
- `data/analytics/ga4_data_latest.json`
- `data/products/product_store_data_latest.json`
- `reports/application_funnel_analysis.json`
- `reports/service_version_analysis.json`
- `reports/comprehensive_summary.json`

### 2. 회사소개서 작성 (nofee-marketing)

```bash
cd nofee-marketing/contents/company-introduction
```

**README.md** 파일에 모든 데이터가 정리되어 있습니다:
- 📊 상세 비즈니스 지표
- 🌐 웹 트래픽 및 마케팅 성과
- 💪 경쟁 우위
- 📈 성장 지표
- 🎯 투자 포인트
- 🚀 향후 성장 전략
- 🏆 30초/1분/5분 피치

**활용**:
- 투자유치 자료
- 파트너십 제안서
- 비즈니스 미팅 자료
- IR 피칭 자료

### 3. 크롤링 (nofee-crawler)

```bash
cd nofee-crawler/workspace-fee-crawler
# 요금제 크롤링

cd nofee-crawler/workspace-sales-crawler
# 판매점 크롤링
```

---

## 💻 기술 스택

### 데이터 수집 및 분석
- **언어**: Python 3.9+
- **데이터베이스**: MySQL 8.0
- **분석 도구**: Google Analytics Data API (GA4)
- **주요 라이브러리**:
  - `pymysql` - MySQL 연결
  - `google-analytics-data` - GA4 API
  - `google-auth` - Google 인증
  - `python-dotenv` - 환경변수 관리

### 크롤링
- Python 3.9+
- BeautifulSoup4
- Selenium
- Requests

### 문서 작성
- Markdown
- HTML/CSS (대시보드)

---

## 📈 주요 인사이트

### 1. 검증된 비즈니스 모델
- ✅ 5,429명의 실제 고객 확보
- ✅ 56개 판매점 네트워크 구축
- ✅ 240건의 실제 개통 완료
- ✅ 양면 시장 성공 (고객 + 판매점)

### 2. 업계 최고 수준의 마케팅 효율
- ✅ **CAC 515원**: 업계 평균(1,000-3,000원) 대비 50-80% 저렴
- ✅ **CPC 99원**: 업계 평균(300-500원) 대비 80% 저렴
- ✅ **CPA 315원**: 견적 신청까지 효율적 유도

### 3. 지속적인 개선 역량
- ✅ **개통율 12배 향상**: 0.5% → 6.0%
- ✅ **10월 최고 15.3%**: 지속 가능한 성장 입증
- ✅ **VOC 기반 개선**: 고객/판매점 피드백 100% 반영

### 4. 높은 고객 참여도
- ✅ **이탈률 2.87%**: 업계 평균(40-60%) 대비 압도적으로 낮음
- ✅ **참여율 97%+**: 거의 모든 방문자가 적극 참여
- ✅ **평균 체류 3.1분**: 충분한 정보 전달

### 5. 모바일 퍼스트 전략
- ✅ **모바일 84.1%**: MZ세대 타겟팅 성공
- ✅ **월 11,531세션**: 안정적 트래픽
- ✅ **세션당 5.5페이지**: 적극적 서비스 탐색

---

## 🎯 활용 시나리오

### 시나리오 1: 투자 미팅 준비
1. [`nofee-marketing/contents/company-introduction/README.md`](nofee-marketing/contents/company-introduction/README.md) 확인
2. 30초/1분/5분 피치 선택
3. 핵심 지표 암기 (CAC, CPA, 개통율 등)
4. 서비스 버전 히스토리 스토리텔링 준비

### 시나리오 2: 월간 성과 보고
1. `nofee-data/scripts/collectors/` 스크립트 실행
2. 최신 데이터 수집
3. `nofee-data/scripts/analyzers/` 스크립트 실행
4. 분석 보고서 생성
5. 전월 대비 성과 분석

### 시나리오 3: 경쟁사 분석
1. `nofee-crawler` 실행
2. 경쟁사 데이터 수집
3. `docs/경쟁사_분석.md` 업데이트
4. 전략 수립

### 시나리오 4: 신규 기능 기획
1. `nofee-data/docs/SERVICE_VERSION_HISTORY.md` 확인
2. 기존 버전별 성과 분석
3. VOC 데이터 검토
4. `docs/`에 기획서 작성

---

## 📚 주요 문서 바로가기

### 데이터 및 분석
- [nofee-data 메인](nofee-data/README.md) - 데이터 저장소 전체 가이드
- [DB 스키마](nofee-data/docs/DATABASE_SCHEMA.md) - 62개 테이블 상세 설명
- [GA4 지표](nofee-data/docs/GA4_METRICS.md) - GA4 지표 및 마케팅 ROI
- [데이터 사전](nofee-data/docs/DATA_DICTIONARY.md) - 코드, 용어 정의
- [퍼널 분석](nofee-data/docs/APPLICATION_FUNNEL_ANALYSIS.md) - 견적신청 퍼널
- [서비스 버전 히스토리](nofee-data/docs/SERVICE_VERSION_HISTORY.md) - 버전별 개통율

### 마케팅 및 비즈니스
- [회사소개서 완전판](nofee-marketing/contents/company-introduction/README.md) - 투자/파트너십용

---

## 🔒 보안 및 권한

### 환경변수 설정
모든 민감정보는 프로젝트 루트의 `.env` 파일에서 관리됩니다.

```bash
# .env 파일 위치
/Users/jacob/Desktop/workspace/nofee/.env

# 설정 항목
- DB_HOST, DB_PORT, DB_USER, DB_PASSWORD, DB_NAME  # 개발 DB
- DB_HOST_PROD, DB_PASSWORD_PROD 등               # 운영 DB
- AES_SECRET_KEY                                   # 암호화 키
- OPENAI_API_KEY                                   # AI Agent용
- GOOGLE_SHEETS_*                                  # Google Sheets API
- NEXT_PUBLIC_KAKAO_*                              # Kakao API
- NEXT_PUBLIC_GTM_ID                               # Google Tag Manager
```

### DB 접근
- **개발 DB**: 환경변수 `DB_HOST` 참조
- **운영 DB**: 환경변수 `DB_HOST_PROD` 참조
- **접근 권한**: CEO, CTO 만

### GA4 접근
- **Service Account**: nofee-price-bot@nofee-price.iam.gserviceaccount.com
- **권한 파일**: `google_api_key.json` (프로젝트 루트)
- **Property ID**: 474694872

### Git 관리
- **저장소**: Private Repository
- **접근 권한**: 팀원만
- **.gitignore**: `.env` 파일은 Git에서 제외됨

---

## ✅ 변경 이력

### 2025-12-09 (v3.1) - 보안 강화
- 모든 민감정보를 환경변수(.env)로 분리
- 하드코딩된 DB 접속정보, API 키 제거
- Python 스크립트에서 `python-dotenv` 사용하도록 변경
- .gitignore에 .env 추가
- 개발/운영 DB 환경 분리

### 2025-11-19 (v3.0) - 전면 개편
- nofee-data 폴더 구조 전면 재구성
- 중복 파일 제거 (30개 이상)
- 실제 데이터 추가 (CAC, CPA, 개통당 비용)
- 월평균 데이터로 전환
- company-introduction README 완전 재작성
- nofee_planning README 완전 재작성

### 2025-11-19 (v2.0)
- 상품/매장 데이터 수집 기능 추가
- 서비스 버전별 개통율 분석 추가
- Instagram 광고 실제 데이터 추가

### 2025-11-04 (v1.1)
- GA4 데이터 수집 기능 추가
- DB 데이터 수집 스크립트 개선

### 2025-10-23 (v1.0)
- 초기 프로젝트 구조 생성
- DB 데이터 수집 스크립트 작성

---

## 🚀 Next Steps

### 단기 (1개월)
1. ✅ 데이터 저장소 정리 완료
2. ✅ 회사소개서 데이터 완성
3. ⏭️ HTML/PDF 회사소개서 디자인
4. ⏭️ 투자유치 IR 덱 제작

### 중기 (3개월)
1. 대시보드 자동화 (index.html 개선)
2. 주간 리포트 자동 생성
3. 경쟁사 분석 정기화
4. 크롤러 데이터 통합

### 장기 (6개월)
1. 실시간 대시보드 구축
2. BI 도구 도입 (Tableau, Looker 등)
3. 예측 모델 개발 (개통율 예측)
4. A/B 테스트 자동화

---

## 📞 문의

**관리자**: 김선호 (CEO)
**최종 업데이트**: 2025-12-09
**문서 버전**: 3.1
**다음 업데이트 예정**: 2026-01-09 (월 1회 정기 업데이트)

---

**© 2025 NOFEE. All rights reserved.**
