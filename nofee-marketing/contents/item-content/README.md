# Item Content Generator

**노피 상품별 마케팅 콘텐츠 자동 생성 도구**

DB에서 주요 상품 데이터를 가져와 상품별 마케팅 이미지를 자동으로 생성합니다.

---

## 주요 기능

- 📊 DB에서 실시간 상품 데이터 수집 (출고가, 최저가, 노피지원금)
- 🎨 상품별 개별 HTML 생성
- 📸 Puppeteer를 사용한 자동 스크린샷 생성
- 📱 두 가지 비율 지원 (3x4, 1x1)
- 📁 날짜별 자동 폴더 구조 생성
- 🔄 6개 주요 상품 일괄 처리

---

## 파일 구조

```
item-content/
├── fetch_product_data.py        # 메인 Python 스크립트
├── screenshot.js                # Puppeteer 스크린샷 스크립트
├── assets/                      # 상품 이미지 폴더
│   ├── 아이폰17프로.png
│   ├── 아이폰17.png
│   ├── 아이폰 17 프로 맥스.png
│   ├── 갤럭시 Z 폴드 7.png
│   ├── 갤럭시 Z 플립 7.png
│   └── 갤럭시 S25 울트라.png
├── output/                      # 생성된 결과 파일
│   └── YYYYMMDD/                # 날짜별 폴더
│       ├── all_products_3x4.html
│       ├── all_products_1x1.html
│       ├── 3x4/                 # 3:4 비율 이미지
│       │   ├── 아이폰_17_프로_3x4.png
│       │   ├── 아이폰_17_3x4.png
│       │   └── ...
│       └── 1x1/                 # 1:1 비율 이미지
│           ├── 아이폰_17_프로_1x1.png
│           ├── 아이폰_17_1x1.png
│           └── ...
└── README.md                    # 이 문서
```

---

## 사용 방법

### 1. 의존성 설치

#### Python 패키지
```bash
pip install pymysql
```

#### Node.js 패키지
```bash
cd /Users/jacob/Desktop/dev/nofee/nofee_planning/nofee-marketing/contents/item-content
npm install puppeteer
```

### 2. 스크립트 실행

```bash
cd /Users/jacob/Desktop/dev/nofee/nofee_planning/nofee-marketing/contents/item-content
python3 fetch_product_data.py
```

### 3. 결과 확인

생성된 파일은 `output/YYYYMMDD/` 폴더에 저장됩니다.

```bash
# HTML 파일 열기
open output/20251122/all_products_3x4.html

# 생성된 이미지 확인
open output/20251122/3x4/
open output/20251122/1x1/
```

---

## 처리 상품 목록

| 순서 | 상품명 | 상품코드 | 이미지 파일 |
|------|--------|----------|-------------|
| 1 | 아이폰 17 프로 | AP-P-17 | 아이폰17프로.png |
| 2 | 아이폰 17 | AP-B-17 | 아이폰17.png |
| 3 | 아이폰 17 프로 맥스 | AP-PM-17 | 아이폰 17 프로 맥스.png |
| 4 | 갤럭시 Z 폴드 7 | SM-ZF-7 | 갤럭시 Z 폴드 7.png |
| 5 | 갤럭시 Z 플립 7 | SM-ZP-7 | 갤럭시 Z 플립 7.png |
| 6 | 갤럭시 S25 울트라 | SM-SU-25 | 갤럭시 S25 울트라.png |

---

## 데이터 소스

### 사용 테이블

1. **tb_product_group_phone**: 상품 기본 정보
2. **tb_pricetable_phone**: 출고가 정보
3. **tb_pricetable_store_phone_col**: 시세표 (최저가 계산)

### 가격 계산 로직

- **출고가**: `tb_pricetable_phone.retail_price`에서 조회
- **최저가**: `tb_pricetable_store_phone_col`의 모든 통신사/요금제/가입유형 조합에서 최저값
  - SKT: common/select × 신규/번호이동/기기변경
  - KT: common/select × 신규/번호이동/기기변경
  - LG: common/select × 신규/번호이동/기기변경
  - 총 18개 조합 중 최저가 선택
- **노피지원금**: 출고가 - 최저가

---

## 출력 형식

### HTML 레이아웃

- **헤더**: "100% 할부원금만 받아요 / 집 근처에서 성지 가격으로"
- **콘텐츠**:
  - 상품명 (대형 폰트)
  - 상품 정보 (결합없음 ㅣ 추가금없음 ㅣ 즉시개통)
  - 상품 이미지
  - 가격 비교 (출고가 vs 최저가)
- **스타일**:
  - 배경: 흰색
  - 헤더: 네이비 블루 (#131FA0)
  - 폰트: SUIT Variable

### 이미지 크기

- **3:4 비율**: 1080 × 1440px (인스타그램/세로 포맷)
- **1:1 비율**: 1080 × 1080px (인스타그램/정사각형)

---

## 폴더 구조 설명

### assets/
- 상품 이미지 원본 파일 보관
- HTML 생성 시 상대 경로로 참조 (`../../assets/`)

### output/YYYYMMDD/
- 날짜별로 자동 생성
- HTML 파일 및 이미지 모두 포함
- 각 실행마다 새로운 폴더 생성

### output/YYYYMMDD/3x4/
- 3:4 비율 이미지 (1080x1440)
- 파일명 형식: `{상품명}_3x4.png`
- 인스타그램 세로형, 스토리 등에 활용

### output/YYYYMMDD/1x1/
- 1:1 비율 이미지 (1080x1080)
- 파일명 형식: `{상품명}_1x1.png`
- 인스타그램 피드, 카카오톡 등에 활용

---

## 스크립트 상세

### fetch_product_data.py

**주요 함수:**

- `get_product_data(product_name, product_code)`: DB에서 상품 정보 조회
  - 3개 테이블 JOIN으로 출고가, 최저가 계산
  - 18개 조합(통신사 × 요금제 × 가입유형) 중 최저가 선택
- `format_price(price)`: 가격을 만원 단위로 변환
  - 마이너스 가격은 0으로 처리 (`max(0, formatted)`)
- `generate_product_html(product, ratio)`: 단일 상품 HTML 생성
  - 상품명 길이에 따라 폰트 크기 자동 조정
  - 가격이 큰 경우 compact 클래스 적용
- `generate_multi_product_html(products, output_filename, ratio)`: 전체 HTML 생성
  - 비율별 캔버스 크기 설정 (3x4: 1080×1440, 1x1: 1080×1080)
  - SUIT Variable 폰트 적용
  - 네이비 블루 헤더 (#131FA0)
- `create_output_directories()`: 날짜별 폴더 구조 생성
  - YYYYMMDD 형식으로 자동 폴더 생성
- `generate_screenshots(html_file, output_dir, ratio)`: Node.js 스크린샷 스크립트 호출
  - subprocess를 통한 Node.js 실행

**실행 흐름:**

1. 날짜별 output 폴더 생성
2. 6개 상품 데이터 DB 조회
3. 3x4 비율 HTML 생성
4. 1x1 비율 HTML 생성
5. 3x4 이미지 스크린샷 생성
6. 1x1 이미지 스크린샷 생성

### screenshot.js

**주요 기능:**

- Puppeteer를 사용하여 HTML 로드
- 각 `.canvas` 요소를 개별 스크린샷
- 상품명 기반 파일명 자동 생성
- Clip 기반 정확한 영역 캡처
- 정확한 이미지 크기 보장 (1080×1440 / 1080×1080)

**기술 구현:**

- 뷰포트 크기: 2000×3000px (충분한 렌더링 공간 확보)
- `deviceScaleFactor: 1` (정확한 픽셀 크기)
- `boundingBox()`로 요소 위치 파악
- `clip` 옵션으로 정확한 영역 추출

**사용법:**

```bash
node screenshot.js <HTML파일> <출력폴더> <비율(3x4|1x1)>
```

---

## 커스터마이징

### 상품 목록 변경

`fetch_product_data.py`의 `PRODUCT_LIST` 수정:

```python
PRODUCT_LIST = [
    {'name': '상품명', 'code': '상품코드', 'image': '이미지파일명.png'},
    # ...
]
```

### 디자인 수정

`generate_multi_product_html()` 함수 내 `<style>` 태그 수정:
- 색상 변경
- 폰트 크기 조정
- 레이아웃 변경

### 이미지 크기 조정

`screenshot.js` 또는 `generate_multi_product_html()`에서:

```python
canvas_width = 1080
canvas_height = 1440  # 원하는 높이로 변경
```

---

## 문제 해결

### DB 연결 실패

```python
DB_CONFIG = {
    'host': '43.203.125.223',
    'port': 3306,
    'user': 'nofee',
    'password': 'HBDyNLZBXZ41TkeZ',
    'database': 'db_nofee',
    'charset': 'utf8mb4'
}
```

DB 연결 정보를 확인하세요.

### Node.js 미설치

```bash
# macOS (Homebrew)
brew install node

# 설치 확인
node --version
npm --version
```

### Puppeteer 설치 오류

```bash
# Puppeteer 재설치
npm install puppeteer --save

# 또는 전역 설치
npm install -g puppeteer
```

### 이미지 경로 오류

- `assets/` 폴더에 모든 상품 이미지가 있는지 확인
- 파일명이 `PRODUCT_LIST`의 `image` 값과 일치하는지 확인

### 스크린샷 실패

- Chrome/Chromium이 설치되어 있는지 확인
- Puppeteer가 headless 모드에서 실행 가능한지 확인
- 권한 문제: `chmod +x screenshot.js`

### 이미지 크기가 잘못됨

- `sips -g pixelWidth -g pixelHeight <이미지파일>` 명령어로 실제 크기 확인
- 1080x1440 또는 1080x1080이 아닌 경우:
  - screenshot.js의 `dimensions` 설정 확인
  - 뷰포트 크기가 충분한지 확인 (최소 2000×3000 권장)
  - `deviceScaleFactor: 1` 설정 확인

### 레이아웃이 깨짐

- HTML에서 `.canvas` 요소가 정확한 크기로 설정되어 있는지 확인
- CSS flex 레이아웃이 제대로 적용되었는지 확인
- 브라우저에서 HTML 파일을 열어 직접 확인

---

## 업데이트 내역

- **2025-11-22**:
  - output 폴더 날짜별 자동 생성 기능 추가
  - Puppeteer 스크린샷 자동화 추가
  - Clip 기반 정확한 영역 캡처 구현 (1080×1440, 1080×1080 보장)
  - 뷰포트 최적화 (2000×3000, deviceScaleFactor: 1)
  - 레이아웃 크기 최적화 (3x4 비율 콘텐츠 잘림 방지)
  - 마이너스 가격 처리 로직 추가 (`max(0, formatted)`)
  - assets 폴더로 이미지 정리
  - 3x4, 1x1 비율 이미지 자동 생성
  - README 문서 작성 및 업데이트

- **2025-11-21**:
  - 초기 버전 생성
  - 6개 상품 HTML 생성 기능

---

## 관련 링크

- [NOFEE 웹사이트](https://nofee.team)
- [Puppeteer 문서](https://pptr.dev/)
- [SUIT 폰트](https://sunn.us/suit/)

---

## 라이센스

Copyright © 2025 NOFEE. All rights reserved.
