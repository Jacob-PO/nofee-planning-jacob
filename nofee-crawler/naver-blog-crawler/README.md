# 네이버 블로그 크롤러

네이버 검색을 통해 휴대폰 업체 블로그를 찾고, 블로그 포스팅에서 전화번호를 추출하는 크롤러입니다.

## 주요 기능

- 🔍 네이버 검색을 통한 블로그 검색
- 📝 블로그 포스팅에서 전화번호 자동 추출
- 💾 CSV 파일로 결과 저장
- 🔄 중복 제거 (같은 전화번호와 링크 조합)

## 설치 방법

### 1. Python 패키지 설치

```bash
cd naver-blog-crawler
pip install -r requirements.txt
```

### 2. ChromeDriver 설치

Selenium을 사용하기 위해 ChromeDriver가 필요합니다.

**macOS (Homebrew):**
```bash
brew install chromedriver
```

**또는 수동 설치:**
1. [ChromeDriver 다운로드](https://chromedriver.chromium.org/downloads)
2. Chrome 버전에 맞는 드라이버 다운로드
3. PATH에 추가하거나 프로젝트 폴더에 배치

## 사용 방법

### 기본 실행

```bash
python naver_blog_crawler.py
```

기본적으로 "안양 휴대폰성지 010" 검색어로 크롤링을 시작합니다.

### 검색어 변경

`naver_blog_crawler.py` 파일의 `main()` 함수에서 검색어를 변경할 수 있습니다:

```python
# 검색어 설정
query = "안양 휴대폰성지 010"  # 원하는 검색어로 변경
```

### 크롤링 옵션 조정

```python
crawler.crawl(
    query=query,
    max_pages=5,   # 최대 검색 페이지 수
    max_posts=50   # 최대 크롤링할 포스트 수
)
```

## 출력 형식

### CSV 파일 구조

생성된 CSV 파일은 다음 형식입니다:

| phone_number | blog_url |
|--------------|----------|
| 010-1234-5678 | https://blog.naver.com/... |
| 010-9876-5432 | https://blog.naver.com/... |

### 파일 저장 위치

- **출력 폴더**: `output/`
- **파일명 형식**: `naver_blog_phones_YYYYMMDD_HHMMSS.csv`
- **로그 파일**: `logs/crawler.log`

## 전화번호 추출 패턴

다음 형식의 전화번호를 자동으로 인식합니다:

- `010-1234-5678` (하이픈)
- `010.1234.5678` (점)
- `010 1234 5678` (공백)
- `01012345678` (연속)
- `010 - 1234 - 5678` (공백 포함 하이픈)

모든 전화번호는 `010-XXXX-XXXX` 형식으로 정규화되어 저장됩니다.

## 주의사항

⚠️ **합법적 사용만 허용**
- 네이버 서비스 이용약관 준수
- 과도한 요청으로 서버에 부하를 주지 않도록 요청 간격 설정됨
- 수집된 데이터는 개인정보보호법을 준수하여 사용

⚠️ **크롤링 속도**
- 각 포스트 크롤링 간 1초 대기 시간 설정
- 전체 크롤링 시간은 포스트 수에 비례

⚠️ **ChromeDriver 버전**
- Chrome 브라우저 버전과 ChromeDriver 버전이 일치해야 함
- 버전 불일치 시 오류 발생

## 문제 해결

### ChromeDriver 오류

```
selenium.common.exceptions.WebDriverException: Message: 'chromedriver' executable needs to be in PATH
```

**해결 방법:**
1. ChromeDriver 설치 확인: `which chromedriver`
2. PATH에 추가 또는 프로젝트 폴더에 배치

### 블로그 탭을 찾을 수 없음

네이버 검색 결과 페이지 구조가 변경되었을 수 있습니다. 
`search_naver_blog()` 함수의 XPath 선택자를 업데이트하세요.

### 전화번호가 추출되지 않음

1. 블로그 포스트의 본문 영역 선택자 확인
2. 전화번호 패턴이 `extract_phone_numbers()` 함수에 포함되어 있는지 확인
3. 로그 파일(`logs/crawler.log`)에서 상세 오류 확인

## 파일 구조

```
naver-blog-crawler/
├── naver_blog_crawler.py    # 메인 크롤러
├── requirements.txt          # Python 패키지 목록
├── README.md                 # 사용 설명서
├── output/                   # 출력 CSV 파일
│   └── naver_blog_phones_*.csv
└── logs/                     # 로그 파일
    └── crawler.log
```

## 커스터마이징

### 헤드리스 모드

브라우저를 표시하지 않고 실행하려면:

```python
crawler = NaverBlogCrawler(headless=True)
```

### 검색어 목록 자동 처리

여러 검색어를 자동으로 처리하려면:

```python
queries = [
    "안양 휴대폰성지 010",
    "수원 휴대폰성지 010",
    "부산 휴대폰성지 010"
]

for query in queries:
    crawler.crawl(query=query, max_pages=3, max_posts=30)
    crawler.save_to_csv(f"result_{query.replace(' ', '_')}.csv")
    crawler.results = []  # 결과 초기화
```

## 라이선스

개인 및 상업적 용도 사용 가능 (합법적 범위 내)

## 버전

- v1.0.0 - 초기 릴리즈 (2025-12-01)



