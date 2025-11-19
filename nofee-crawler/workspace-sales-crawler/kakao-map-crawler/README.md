# 카카오맵 휴대폰 매장 크롤러

카카오맵에서 휴대폰 매장 정보를 수집하여 CSV 파일로 저장하는 웹 크롤러입니다.

## 📋 기능

### 크롤링 기능
- 카카오맵 검색 결과에서 매장 정보 수집
- 매장명, 주소, 전화번호, 카테고리, 카카오맵 링크 등 수집
- 자동 스크롤로 모든 검색 결과 로드
- 상세 페이지 방문하여 추가 정보 수집 (옵션)
- CSV 파일로 결과 저장 (UTF-8 with BOM, Excel 호환)

### 데이터 병합 기능 (merge_csv.py)
- 여러 크롤링 결과 파일을 하나로 자동 병합
- 전화번호 기준 중복 제거 (동일 전화번호는 하나만 유지)
- 주소에서 시/도, 군/구 정보 자동 파싱
- 표준 형식으로 자동 변환 (`지역명_매장명,매장명,지역명,시,군구,전화번호,주소,링크`)
- 상세한 병합 통계 및 로그 출력

## 🛠️ 설치

### 1. Python 패키지 설치

```bash
cd nofee_planning/workspace/workspace-sales-crawler/kakao-map-crawler
pip3 install -r requirements.txt
```

### 2. Chrome 브라우저 설치

- Chrome 브라우저가 설치되어 있어야 합니다
- Selenium이 자동으로 ChromeDriver를 관리합니다

## 🚀 운영 명령어

아래 명령어들은 모두 프로젝트 루트(`/Users/jacob/Desktop/workspace/nofee/nofee_planning/workspace/workspace-sales-crawler/kakao-map-crawler`)에서 실행합니다.

### 1. 기본 다중 키워드 크롤링
- `main()`에 정의된 기본 키워드(휴대폰 성지/판매점/백화점/대리점/할인점/매장)를 순차로 돌리고, 010 번호만 CSV로 저장합니다.
```bash
cd /Users/jacob/Desktop/workspace/nofee/nofee_planning/workspace/workspace-sales-crawler/kakao-map-crawler && python3 kakao_map_crawler_v2.py
```

### 2. 특정 키워드만 실행 (3글자 `휴대폰` 등)
```bash
cd /Users/jacob/Desktop/workspace/nofee/nofee_planning/workspace/workspace-sales-crawler/kakao-map-crawler && python3 - <<'PY'
from kakao_map_crawler_v2 import KakaoMapCrawlerV2
crawler = KakaoMapCrawlerV2(headless=False)  # 서버/자동화 시 headless=True 권장
crawler.crawl_keywords(["휴대폰"], get_details=True)
PY
```
- 여러 키워드를 넣고 싶으면 `["휴대폰", "휴대폰 성지", ...]` 처럼 배열을 확장합니다.
- 완전히 커스텀 URL을 쓰고 싶다면 `crawler.crawl_from_url(url, get_details=False)`를 호출하면 됩니다.

### 3. 출력 CSV 통합 & 중복 제거 (권장)
- `output/` 안의 모든 `kakao_phone_stores_*_multi.csv`를 병합하여 `kakao_phone_stores_merged_타임스탬프.csv`로 저장합니다.
- **전화번호 기준 중복 제거**: 같은 전화번호는 하나만 유지합니다.
- **표준 형식 변환**: 주소에서 시/도, 군/구 정보를 자동으로 파싱하여 표준 형식으로 변환합니다.

```bash
cd /Users/jacob/Desktop/dev/nofee/nofee_planning/workspace/workspace-sales-crawler/kakao-map-crawler && python3 merge_csv.py
```

#### 병합 스크립트 주요 기능
- ✅ 자동 파일 검색: `kakao_phone_stores_*_multi.csv` 패턴의 모든 파일 검색
- ✅ 2단계 중복 제거:
  - 1단계: 모든 컬럼이 동일한 완전 중복 제거
  - 2단계: 전화번호 기준 중복 제거 (첫 번째 행만 유지)
- ✅ 표준 형식 변환: `지역명_매장명,매장명,지역명,시,군구,전화번호,주소,링크` 형식으로 변환
- ✅ 주소 파싱: 주소에서 시/도, 군/구 정보 자동 추출
- ✅ 상세 로그: 각 단계별 처리 결과 및 통계 출력

#### 출력 예시
```
총 101개의 CSV 파일을 발견했습니다.
병합 전 총 행 수: 3754
1단계 - 완전 중복 제거 후 행 수: 3619
2단계 - 전화번호 중복 제거 후 행 수: 520
최종 행 수: 520

=== 병합 결과 요약 ===
총 매장 수: 520
지역별 매장 수:
  - 서울 구로구: 40개
  - 경기 부천시: 38개
  ...
```

### 4. Python 코드에서 직접 제어 (스크립트 내 사용)

```python
from kakao_map_crawler_v2 import KakaoMapCrawlerV2

# URL 직접 지정
url = "https://map.kakao.com/?q=강남구+휴대폰매장&tab=place"

crawler = KakaoMapCrawlerV2(headless=True)  # headless=False면 브라우저가 보임
results = crawler.crawl_from_url(url, get_details=False)
print(f"010 번호 있는 매장: {len(results)}개")
```

## 📊 출력 파일

### CSV 파일 구조

#### 1. 크롤링 결과 파일 (`kakao_phone_stores_YYYYMMDD_HHMMSS_multi.csv`)
크롤러가 생성하는 개별 결과 파일입니다.

**컬럼 구조:**
```csv
매장명,주소,전화번호,카테고리,카카오맵링크
휴대폰성지,서울 중구 장충단로 247,010-8800-8118,휴대폰판매,https://map.kakao.com/...
```

#### 2. 병합 결과 파일 (`kakao_phone_stores_merged_YYYYMMDD_HHMMSS.csv`)
`merge_csv.py`가 생성하는 통합 및 표준화된 결과 파일입니다.

**컬럼 구조:**
```csv
지역명_매장명,매장명,지역명,시,군구,전화번호,주소,링크
서울 금천구_옆커폰 시흥점,옆커폰 시흥점,서울 금천구,서울,금천구,010-5609-1128,서울 금천구 금하로 619 1층,https://map.kakao.com/...
경기 부천시_꿈뱅이네 휴대폰,꿈뱅이네 휴대폰,경기 부천시,경기,부천시,010-2468-8843,경기 부천시 ...,https://map.kakao.com/...
```

**변환 내용:**
- `지역명_매장명`: 지역명과 매장명을 결합한 고유 키
- `지역명`: 시 + 군구 조합 (예: "서울 금천구", "경기 부천시")
- `시`: 시/도 정보 (예: "서울", "경기", "강원특별자치도")
- `군구`: 시/군/구 정보 (예: "금천구", "부천시", "강릉시")
- `링크`: 카카오맵링크를 링크로 컬럼명 변경

### 저장 위치

- 경로: `nofee_planning/workspace/workspace-sales-crawler/kakao-map-crawler/output/`
- 크롤링 결과: `kakao_phone_stores_YYYYMMDD_HHMMSS_multi.csv`
- 병합 결과: `kakao_phone_stores_merged_YYYYMMDD_HHMMSS.csv`
- 예시:
  - `kakao_phone_stores_20251115_171757_multi.csv` (크롤링 결과)
  - `kakao_phone_stores_merged_20251115_180056.csv` (병합 결과)

## 🔧 커스터마이징 포인트

- `main()` 내부 `keywords` 리스트로 기본 검색어를 수정할 수 있습니다.
- `KakaoMapCrawlerV2(headless=True/False)`로 브라우저 표시 여부를 제어합니다.
- `scroll_page()` 내부 `max_scrolls`, `time.sleep()` 구간을 조정하면 더 빠르거나 느리게 동작합니다.
- `crawl_from_url(..., get_details=True)`를 활성화하면 카카오 상세 페이지까지 방문하도록 확장할 수 있습니다.

## 🧩 추천 키워드 & 스케줄링 팁

- 추가로 자주 쓰는 키워드 예시:
  - `["휴대폰", "휴대폰 성지", "휴대폰 판매점", "휴대폰 백화점", "휴대폰 대리점", "휴대폰 할인점", "휴대폰 매장", "휴대폰 최저가", "휴대폰 도매"]`
  - `crawler.crawl_keywords(키워드_리스트, get_details=False)` 형태로 재사용하면 됩니다.
- Mac/Linux에서 매일 새벽 3시에 headless 모드로 자동 수집하고 싶다면 크론탭에 아래처럼 등록할 수 있습니다:
  ```bash
  crontab -e
  # 매일 03:00 실행 예시
  0 3 * * * cd /Users/jacob/Desktop/workspace/nofee/nofee_planning/workspace/workspace-sales-crawler/kakao-map-crawler && /usr/bin/python3 kakao_map_crawler_v2.py >> ~/kakao_crawler.log 2>&1
  ```
  - 로그 파일(`~/kakao_crawler.log`)을 지정해두면 에러를 확인하기 쉽습니다.
  - 서버 환경에서는 `headless=True`로 두고, 필요 시 `Xvfb` 같은 가상 디스플레이를 사용하세요.

## ⚠️ 주의사항

1. **과도한 요청 금지**: 카카오맵 서버에 부담을 주지 않도록 적절한 대기 시간을 설정하세요
2. **로봇 방지**: 너무 빠른 속도로 크롤링하면 차단될 수 있습니다
3. **저작권**: 수집한 데이터는 개인적/연구 용도로만 사용하세요
4. **이용약관**: 카카오맵 이용약관을 준수하세요

## 🐛 문제 해결

### Chrome 드라이버 오류

```
❌ Chrome 드라이버 초기화 실패
```

**해결 방법:**
- Chrome 브라우저가 설치되어 있는지 확인
- Chrome 버전과 ChromeDriver 버전이 호환되는지 확인
- `pip3 install --upgrade selenium` 실행

### 검색 결과를 찾을 수 없음

```
❌ 검색 결과를 찾을 수 없습니다
```

**해결 방법:**
- URL이 올바른지 확인
- 카카오맵 페이지 구조가 변경되었을 수 있음
- `headless=False`로 설정하여 브라우저를 직접 확인

### 전화번호가 수집되지 않음

- `get_details=True` 옵션을 사용하여 상세 페이지도 방문하도록 설정
- 일부 매장은 전화번호를 공개하지 않을 수 있음

## 📝 버전 히스토리

### v2.1 (2025-11-15)
- **CSV 병합 스크립트 추가** (`merge_csv.py`)
  - 여러 크롤링 결과 파일 자동 병합
  - 전화번호 기준 중복 제거 기능
  - 주소 파싱을 통한 지역 정보 자동 추출
  - 표준 형식으로 자동 변환
  - 상세한 병합 통계 출력
- README 문서 개선 및 병합 기능 가이드 추가

### v2.0 (2024-11-12)
- 특정 URL 지원 추가
- 자동 스크롤 기능 추가
- CSV 출력 구조 개선
- 상세 정보 수집 옵션 추가

### v1.0 (이전 버전)
- 기본 크롤링 기능
- 다중 지역/키워드 검색

## 📞 문의

문제가 발생하거나 개선 사항이 있으면 이슈를 등록해주세요.
