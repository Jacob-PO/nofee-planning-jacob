# Google Phone Store Crawler (당근마켓 휴대폰 매장 크롤러)

Google 검색을 통해 당근마켓의 휴대폰 매장 정보를 수집하는 Selenium 기반 크롤러입니다.

## 📁 파일 구조

```
google-phone-store-crawler/
├── crawler.py          # 메인 크롤러 스크립트
├── requirements.txt    # Python 패키지 의존성
├── README.md          # 설명서
├── output/            # 크롤링 결과 CSV 파일 저장
└── venv/              # Python 가상환경
```

## 🎯 기능

- ✅ Google 검색으로 당근마켓 local-profile 페이지 검색
- ✅ 개별 매장 페이지에서 매장명, 전화번호, 지역 정보 추출
- ✅ CSV 파일로 저장
- ✅ Google Sheets 자동 업로드
- ✅ 검색 결과 페이지 필터링 (개별 매장 페이지만 처리)

## 🚀 설치

```bash
# 가상환경 활성화
source ../venv/bin/activate

# 필요한 패키지 설치
pip install selenium pandas gspread google-auth
```

## 📖 사용법

```bash
# 크롤러 실행
python crawler.py
```

## ⚙️ 설정

`crawler.py` 파일의 `main()` 함수에서 다음 설정을 변경할 수 있습니다:

```python
# Google Sheets URL
SPREADSHEET_URL = "https://docs.google.com/spreadsheets/d/..."

# 검색 수 설정 (기본값: 200 - 서울/수도권 집중)
# - 테스트: max_searches=10
# - 소규모: max_searches=50 (서울 일부 구 + 주요 키워드)
# - 중규모: max_searches=200 (서울 전체 + 경기 일부) ⭐ 기본값
# - 대규모: max_searches=500 (서울 + 수도권 전체)
# - 전체: max_searches=1000 이상 (모든 지역 × 모든 키워드)
results = crawler.crawl(max_searches=200)

# 브라우저 표시 여부 (headless=True면 숨김)
crawler = DaangnStoreCrawlerSelenium(headless=False)
```

### 추천 크롤링 전략

#### 1단계: 테스트 (10개 검색)
```python
results = crawler.crawl(max_searches=10)
```
- 소요시간: 약 5-10분
- 예상 수집: 5-20개 매장

#### 2단계: 서울 집중 (200개 검색)
```python
results = crawler.crawl(max_searches=200)
```
- 소요시간: 약 1-2시간
- 예상 수집: 100-300개 매장
- 서울 25개 구 대부분 커버

#### 3단계: 수도권 전체 (500개 검색)
```python
results = crawler.crawl(max_searches=500)
```
- 소요시간: 약 3-5시간
- 예상 수집: 300-600개 매장
- 서울 + 경기 + 인천 전체 커버

#### 4단계: 전국 크롤링 (1000+ 검색)
```python
results = crawler.crawl(max_searches=1500)
```
- 소요시간: 약 6-10시간
- 예상 수집: 500-1000개 매장
- 전국 모든 지역 커버

## 📊 출력 형식

CSV 및 Google Sheets에 다음 형식으로 저장됩니다:

| 지역명 | 매장명 | 지역명_매장명 | 전화번호 | 링크 |
|--------|--------|---------------|----------|------|
| 서울 종로구 | 휴대폰 성지 | 서울 종로구_휴대폰 성지 | 010-2131-0374 | https://www.daangn.com/kr/local-profile/... |
| 부산 부산진구 | 휴대폰성지 | 부산 부산진구_휴대폰성지 | 010-6676-8832 | https://www.daangn.com/kr/local-profile/... |

## 🔍 검색 전략 (서울/수도권 집중)

### 검색 지역 (총 67개 지역)

#### 서울 25개 구 (세분화)
- 강남구, 강동구, 강북구, 강서구, 관악구, 광진구, 구로구, 금천구
- 노원구, 도봉구, 동대문구, 동작구, 마포구, 서대문구, 서초구, 성동구
- 성북구, 송파구, 양천구, 영등포구, 용산구, 은평구, 종로구, 중구, 중랑구

#### 경기 남부 (15개 시)
- 성남, 수원, 안양, 안산, 용인, 광명, 과천, 의왕, 군포, 시흥, 부천, 광주, 하남, 화성, 오산

#### 경기 북부 (10개 시)
- 고양, 파주, 의정부, 양주, 동두천, 남양주, 구리, 포천, 연천, 가평

#### 경기 동부 (3개 시)
- 이천, 여주, 양평

#### 인천 (11개 구/군)
- 중구, 동구, 남구, 연수구, 남동구, 부평구, 계양구, 서구, 강화군, 옹진군

#### 기타 지역
- 부산, 대구, 광주, 대전, 울산, 세종, 강원, 충북, 충남, 전북, 전남, 경북, 경남, 제주

### 검색 키워드 (총 25개) - 노피 "동네성지" 컨셉 기반

#### 핵심: 동네 판매점 키워드 (7개)
- 휴대폰매장, 휴대폰성지, 스마트폰매장, 폰매장
- 휴대폰가게, 핸드폰가게, 동네휴대폰매장

#### 판매/개통 키워드 (8개) - O2O 신뢰 기반
- 휴대폰판매, 휴대폰대리점, 핸드폰매장, 핸드폰판매
- 스마트폰판매, 휴대폰개통, 기기변경, 번호이동

#### 기기별 키워드 (6개) - 주요 검색 수요
- 아이폰, 갤럭시, 아이폰매장, 갤럭시매장
- 아이폰판매, 갤럭시판매

#### 신뢰/후기 키워드 (5개) - 동네성지 핵심
- 휴대폰매장추천, 믿을만한휴대폰매장, 안전한개통
- 휴대폰성지후기, 휴대폰매장후기

### 검색 조합
- 최대 검색 조합: **67개 지역 × 25개 키워드 = 1,675개 조합**
- 서울만: **25개 구 × 25개 키워드 = 625개 조합**
- 수도권(서울+경기+인천): **약 1,200개 조합**

### 검색 쿼리 예시
- "서울 강남구 휴대폰매장 site:daangn.com"
- "경기 성남 휴대폰성지 site:daangn.com"
- "인천 부평구 아이폰매장 site:daangn.com"
- "서울 송파구 믿을만한휴대폰매장 site:daangn.com"

### 🎯 키워드 전략 배경 (노피 비즈니스 모델)
노피는 **"내 동네 판매점 후기 기반 신뢰 플랫폼"**입니다.
- ✅ **O2O 하이브리드**: 온라인 견적 → 오프라인 개통
- ✅ **지역 기반 신뢰**: 당근마켓처럼 "내 동네" 강조
- ✅ **판매점 후기**: 야놀자처럼 업소 리뷰 시스템
- ❌ **제외 키워드**: 알뜰폰, AS, 액정수리, 폰테크 (노피와 무관)

## ⚠️ 주의사항

- Chrome 브라우저가 설치되어 있어야 합니다
- Google Sheets 업로드를 위해서는 `../../config/google_api_key.json` 파일이 필요합니다
- 검색 결과 페이지(`?in=...&search=...`)는 자동으로 건너뜁니다
- 개별 매장 페이지만 처리합니다
- 크롤링 속도 제한을 위해 요청 간 2-3초 대기 시간이 설정되어 있습니다

## 📁 결과 파일

수집된 데이터는 `output/` 폴더에 저장됩니다:

```
output/
└── daangn_stores_selenium_20251016_170940.csv
```

## 🔧 구조

```python
class DaangnStoreCrawlerSelenium:
    - init_driver()                    # Chrome 드라이버 초기화
    - search_daangn_stores()           # Google에서 당근마켓 링크 검색
    - extract_store_info_from_page()   # 매장 정보 추출
    - crawl()                          # 메인 크롤링 실행
    - save_to_csv()                    # CSV 저장
    - upload_to_sheets()               # Google Sheets 업로드
```
