# 🗺️ 복수 플랫폼 휴대폰 매장 수집 시스템

## 개요

당근마켓, 네이버 플레이스, 카카오맵, 구글 마이비즈니스에서 **010 전화번호**를 가진 휴대폰 매장 정보를 자동으로 수집하는 시스템입니다.

---

## 📂 프로젝트 구조

```
workspace-sales-crawler/
├── google-phone-store-crawler/      # 당근마켓 크롤러 (기존)
│   ├── crawler.py
│   └── output/
├── naver-place-crawler/             # 네이버 플레이스 크롤러 (신규)
│   ├── naver_crawler.py
│   └── output/
├── kakao-map-crawler/               # 카카오맵 크롤러 (신규)
│   ├── kakao_crawler.py
│   └── output/
├── google-mybusiness-crawler/       # 구글 마이비즈니스 크롤러 (신규)
│   ├── google_mybusiness_crawler.py
│   └── output/
├── merge_all_platforms.py           # 데이터 통합 스크립트
└── merged_output/                   # 통합 결과 저장
```

---

## 🚀 사용 방법

### 1단계: 각 플랫폼별 크롤링 실행

#### 1-1. 당근마켓 (이미 완료 - 11개 수집)
```bash
cd google-phone-store-crawler
python3 crawler.py
```

#### 1-2. 네이버 플레이스
```bash
cd naver-place-crawler
python3 naver_crawler.py
```

#### 1-3. 카카오맵
```bash
cd kakao-map-crawler
python3 kakao_crawler.py
```

#### 1-4. 구글 마이비즈니스
```bash
cd google-mybusiness-crawler
python3 google_mybusiness_crawler.py
```

---

### 2단계: 데이터 통합 및 중복 제거

모든 플랫폼 크롤링이 완료되면:

```bash
cd workspace-sales-crawler
python3 merge_all_platforms.py
```

**작동 과정**:
1. 각 플랫폼의 `output/` 폴더에서 최신 CSV 파일 자동 로드
2. 매장명 + 전화번호 기준으로 중복 제거
3. 통합 CSV 파일 생성 (`merged_output/`)
4. 구글 시트 `merged` 워크시트에 자동 업로드

---

## 📊 수집되는 데이터

| 컬럼명 | 설명 |
|--------|------|
| 지역명 | 서울 강남구, 경기 성남 등 |
| 매장명 | 휴대폰 매장 이름 |
| 전화번호 | **010으로 시작하는 번호만** |
| 링크 | 원본 플랫폼 링크 |
| 플랫폼 | 당근마켓, 네이버플레이스, 카카오맵, 구글마이비즈니스 |

---

## ⚙️ 주요 기능

### ✅ 010 전화번호 필터링
- 02, 031, 1588 등 지역번호/대표번호는 **제외**
- 개인 판매점만 정확히 수집

### ✅ CAPTCHA 회피
- 검색 간격 랜덤화 (3~15초)
- User-Agent 랜덤화
- WebDriver 탐지 방지

### ✅ 자동 재시작
- 브라우저 크래시 시 자동 복구
- 최대 3회 재시도

### ✅ 중간 저장
- 50개 검색마다 자동 저장
- 데이터 손실 방지

### ✅ 중복 제거
- 매장명 + 전화번호 조합으로 중복 제거
- 플랫폼 간 중복 매장 자동 통합

---

## 🎯 검색 대상

### 지역 (79개)
- 서울 25개 구 전체
- 경기 29개 시/군
- 인천 11개 구/군

### 키워드 (26개)
```
핵심: 휴대폰매장, 휴대폰성지, 스마트폰매장, 폰매장
판매: 휴대폰판매, 휴대폰대리점, 핸드폰매장
기능: 휴대폰개통, 기기변경, 번호이동
기기: 아이폰, 갤럭시, 아이폰매장, 갤럭시매장
신뢰: 휴대폰매장추천, 믿을만한휴대폰매장, 휴대폰성지후기
```

---

## 📈 예상 수집량

| 플랫폼 | 예상 수집량 | 특징 |
|--------|-------------|------|
| 당근마켓 | 11개 (완료) | 010 공개율 낮음 |
| 네이버 플레이스 | 500~1000개 | ⭐⭐⭐⭐⭐ 가장 많음 |
| 카카오맵 | 300~600개 | ⭐⭐⭐⭐ 네이버 미등록 매장 |
| 구글 | 200~400개 | ⭐⭐⭐ 독립 매장 |
| **통합 (중복 제거 후)** | **800~1500개** | 🎯 최종 목표 |

---

## ⏱️ 예상 소요 시간

| 작업 | 소요 시간 |
|------|-----------|
| 당근마켓 (완료) | 2.5시간 |
| 네이버 플레이스 | 6~8시간 |
| 카카오맵 | 6~8시간 |
| 구글 마이비즈니스 | 8~10시간 |
| 데이터 통합 | 1분 |
| **총 소요 시간** | **약 22~28시간** |

💡 **팁**: 각 크롤러를 **병렬로 실행**하면 시간 단축 가능!

---

## 🔧 병렬 실행 방법

4개 터미널을 열어서 동시에 실행:

```bash
# 터미널 1
cd naver-place-crawler && python3 naver_crawler.py

# 터미널 2
cd kakao-map-crawler && python3 kakao_crawler.py

# 터미널 3
cd google-mybusiness-crawler && python3 google_mybusiness_crawler.py

# 터미널 4 (모니터링)
watch -n 60 'ls -lh */output/*.csv | tail -20'
```

모두 완료되면:
```bash
python3 merge_all_platforms.py
```

---

## 📤 구글 시트 업로드

통합 스크립트는 자동으로 다음 시트에 업로드합니다:

**시트 URL**: https://docs.google.com/spreadsheets/d/1_kRQWg7yvwGP8uXGkrXLzL82bNLLVN4S-VYkvI-cJMw/

**워크시트**: `merged` (자동 생성)

---

## 🔍 중복 제거 로직

```python
# 중복 제거 기준
unique_key = 매장명 + "_" + 전화번호

# 예시
"강남 키움텔레콤_010-6543-2084"  # 고유 키 생성
→ 같은 키가 여러 플랫폼에 있으면 첫 번째만 유지
```

---

## 📊 플랫폼별 통계 예시

```
📈 플랫폼별 통계:
   네이버플레이스: 750개
   카카오맵: 423개
   구글마이비즈니스: 287개
   당근마켓: 11개

🔍 중복 제거:
   제거 전: 1471개
   제거 후: 1024개
   제거됨: 447개 중복

🎉 통합 완료! 최종 1024개 매장 수집
```

---

## ⚠️ 주의사항

1. **Google API Key 필요**: `/Users/jacob/Desktop/dev/config/google_api_key.json`
2. **Chrome 브라우저 필요**: Selenium 사용
3. **안정적인 인터넷 연결**: 장시간 크롤링
4. **봇 탐지 주의**: 너무 빠르게 실행 시 차단 가능

---

## 🐛 문제 해결

### Chrome 드라이버 오류
```bash
# 최신 Chrome 버전 확인 후 chromedriver 설치
brew install chromedriver
```

### CAPTCHA 차단
- 검색 간격이 자동으로 조정됨
- headless 모드로 실행 중
- 문제 지속 시 `wait_time` 증가

### 메모리 부족
- 중간 저장 간격 줄이기 (`save_interval=25`)
- 한 번에 하나씩 실행

---

## 📝 로그 확인

각 크롤러는 실시간으로 진행 상황을 출력합니다:

```
[234/2000] 🔍 서울 강남구 휴대폰매장
    📌 39개 검색 결과 발견
    💾 저장: 강남 키움텔레콤 (010-6543-2084)
    ⏳ 12.3초 대기 중...
```

---

## 🎉 완료 후

최종 통합 파일 위치:
```
workspace-sales-crawler/merged_output/
└── merged_all_platforms_20251104_180000.csv
```

구글 시트에서 확인:
- 워크시트: `merged`
- 자동 업로드 완료!

---

## 💡 향후 개선 아이디어

- [ ] API 기반 수집 (더 빠른 속도)
- [ ] 매장 정보 추가 (주소, 영업시간, 평점)
- [ ] 실시간 모니터링 대시보드
- [ ] 자동 스케줄링 (cron)
- [ ] 데이터 품질 검증

---

## 📞 문의

문제 발생 시 이슈 등록 또는 담당자에게 연락하세요!
