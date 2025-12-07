# SKT 공시지원금 API 분석

## 개요

SKT shop.tworld.co.kr에서 휴대폰 공시지원금 데이터를 가져오는 API 분석 결과입니다.

---

## 🔑 핵심 요약

### 데이터 수집 전략

| 데이터 | 소스 | 방식 | 상태 |
|-------|------|------|------|
| **요금제 목록** | `/api/wireless/subscription/list` | REST JSON API | ✅ 1,231개 (740개 공시대상) |
| **공시지원금** | `/notice` 페이지 | SSR JSON 추출 | ✅ 97개 기기 |
| **상품 목록** | `/api/wireless/product/list/mobile-list` | REST JSON API (세션 필요) | ✅ 44개 기기 |
| 제조사 목록 | `/api/wireless/product/list/company` | REST JSON API | ✅ 3개 |
| 할인방식 목록 | `/api/wireless/subscriptionInfo` | REST JSON API | ✅ 4개 |
| 요금제 상세 | `/api/wireless/product/list/param-subscription` | REST JSON API | ✅ 세션 불필요 |
| 사은품 정보 | `/api/wireless/tgiftInfo` | REST JSON API | ✅ 세션 불필요 |

### 핵심 발견 🎯

1. **공시지원금 데이터는 `/notice` 페이지에 JSON으로 임베딩됨**
   - `parseObject([...])` 형태로 97개 기기의 전체 데이터 제공
   - 파라미터: `scrbTypCd` (개통유형), `dcMthdCd` (할인방식), `prodId` (요금제)

2. **요금제별로 다른 공시지원금**
   - 5GX 프라임 (NA00007790): 갤럭시Z플립7 580,000원
   - T플랜 맥스 (NA00006539): 갤럭시Z플립7 590,000원
   - 다이렉트 요금제: 0원 (공시지원금 미적용)

3. **mobile-list API는 세션 쿠키로 작동!** 🆕
   - 페이지 접속 후 쿠키 획득 → API 호출 시 44개 상품 반환
   - 출고가, 월 납부액, 색상, 용량 등 상품 정보 포함
   - 공시지원금 관련 필드는 별도 계산 필요

4. **요금제 1,231개 중 740개가 공시지원금 대상**
   - `subcategoryId='H'`인 요금제만 공시지원금 적용

---

## ⚠️ JSON API 제한사항

### 순수 JSON API로 제공되는 데이터 (세션 불필요)
| 데이터 | API | 상태 |
|-------|-----|------|
| 요금제 목록 (1,231개) | `/api/wireless/subscription/list` | ✅ 작동 |
| 제조사 목록 | `/api/wireless/product/list/company` | ✅ 작동 |
| 할인방식 목록 | `/api/wireless/subscriptionInfo` | ✅ 작동 |
| 카테고리 정보 | `/api/wireless/product/list/rel-category-item` | ✅ 작동 |
| 요금제 상세 | `/api/wireless/product/list/param-subscription` | ✅ 작동 |
| 서브카테고리 | `/api/wireless/product/list/subcategory` | ✅ 작동 |
| 할인방법 목록 | `/api/wireless/product/list/comm-dc-method-list` | ✅ 작동 |
| 사은품 정보 | `/api/wireless/tgiftInfo` | ✅ 작동 |
| 퀵배송 정보 | `/api/wireless/childProductList` | ✅ 작동 |
| 배송 체크 | `/api/wireless/directQuickCheck` | ✅ 작동 |

### 세션 쿠키 필요한 API 🆕
| 데이터 | API | 세션 획득 방법 |
|-------|-----|---------------|
| **상품 목록 (44개)** | `/api/wireless/product/list/mobile-list` | 페이지 접속 후 쿠키 사용 |

### JSON API로 제공되지 않는 데이터
| 데이터 | 대안 방식 | 비고 |
|-------|----------|------|
| **공시지원금/기기 목록** | `/notice` SSR JSON 추출 | `parseObject([...])` |

**결론**:
- 대부분의 메타데이터는 순수 REST JSON API로 조회 가능
- 상품 목록(`mobile-list`)은 세션 쿠키 필요 (페이지 접속 후 쿠키 획득)
- 공시지원금 데이터는 `/notice` 페이지에서 JSON 추출 필요

### 존재하지 않는 API (404)
| 시도한 API | 결과 |
|-----------|------|
| `/api/notice/list` | 404 Not Found |
| `/api/notice/product/list` | 404 Not Found |
| `/api/wireless/notice/list` | 404 Not Found |
| `/api/wireless/subsidy/list` | 404 Not Found |
| `/api/wireless/telecomSale` | 404 Not Found |
| `/api/wireless/saleAmt` | 404 Not Found |

---

## 발견된 API 엔드포인트 전체 목록

### 순수 JSON API (세션 불필요) ✅

| API | 용도 | 방식 | 반환 데이터 |
|-----|------|------|-----------|
| `/api/wireless/subscription/list` | **전체 요금제 목록** | GET | 1,231개 (content 배열) |
| `/api/wireless/subscriptionInfo` | 할인방식 목록 | GET | subCommDcMethdList 배열 |
| `/api/wireless/product/list/company` | 제조사 목록 | GET | 삼성/Apple/기타 |
| `/api/wireless/product/list/param-subscription` | 요금제 상세 정보 | GET | 요금제 세부정보 |
| `/api/wireless/product/list/comm-dc-method-list` | 할인방법 목록 | GET | 4가지 할인 방법 |
| `/api/wireless/product/list/rel-category-item` | 카테고리 정보 | GET | 대표 요금제 |
| `/api/wireless/product/list/subcategory` | 서브카테고리 조회 | GET | subcategoryId |
| `/api/wireless/childProductList` | 하위 상품 목록 | GET | 퀵배송 정보 |
| `/api/wireless/directQuickCheck` | 퀵배송 가능 체크 | GET | 지역별 배송 정보 |
| `/api/wireless/tgiftInfo` | **사은품 정보** | GET | 사은품 목록 |
| `/api/wireless/clubInfo` | 클럽 정보 | GET | T클럽 정보 |
| `/api/wireless/checkpoints` | 유의사항 | GET | 체크포인트 목록 |
| `/api/wireless/common/code/list/{lcd}` | 공통 코드 | GET | 코드 목록 |

### 세션 필요 API (페이지 접속 후 쿠키 필요) 🆕

| API | 용도 | 방식 | 상태 |
|-----|------|------|------|
| `/api/wireless/product/list/mobile-list` | **상품 목록** | GET | ✅ 44개 상품 (쿠키 필요) |
| `/api/wireless/product/list/category-info` | 카테고리 상세 | GET | ✅ (쿠키 필요) |
| `/api/wireless/subscription/category` | 요금제 카테고리 | GET | ✅ (쿠키 필요) |
| `/api/wireless/mdlList` | 모델 목록 (중고폰) | GET | 세션 필요 |

### HTML 파싱 방식 (공시지원금 데이터) 📄

| 페이지 | 용도 | 추출 방식 |
|--------|------|----------|
| `/notice` | **공시지원금 데이터** | `parseObject([...])` |

---

## 🆕 mobile-list API 상세 (세션 필요)

### 세션 획득 방법

```bash
# 1. 페이지 접속하여 쿠키 획득
curl -s -c cookies.txt "https://shop.tworld.co.kr/wireless/product/list?categoryId=20010014" \
  -H "User-Agent: Mozilla/5.0"

# 2. 쿠키를 사용하여 API 호출
curl -s -b cookies.txt "https://shop.tworld.co.kr/api/wireless/product/list/mobile-list?categoryId=20010014&subcommType=10&subcommTerm=24&sortType=N&subscriptionId=NA00007790&companyCodes=&entryCd=31" \
  -H "User-Agent: Mozilla/5.0" \
  -H "Accept: application/json" \
  -H "Referer: https://shop.tworld.co.kr/wireless/product/list?categoryId=20010014"
```

### 요청 파라미터

| 파라미터 | 설명 | 필수 | 예시 |
|---------|------|------|------|
| `categoryId` | 카테고리 ID | ✅ | `20010014` (5G 휴대폰) |
| `subcommType` | 할인방식 | ✅ | `10`=공시, `20`=선약, `90`=더유리 |
| `subcommTerm` | 약정기간 | ✅ | `24` |
| `sortType` | 정렬 | | `N`=최신순, `O`=주문순 |
| `subscriptionId` | 요금제 ID | ✅ | `NA00007790` |
| `companyCodes` | 제조사 필터 | | 빈값=전체 |
| `entryCd` | 가입유형 | ✅ | `11`=신규, `20`=번이, `31`=기변 |

### 응답 필드 (상품당)

| 필드 | 설명 | 예시 |
|-----|------|------|
| `modelName` | 기기명 | `iPhone 17` |
| `productGrpId` | 상품 그룹 ID | `000006958` |
| `productPrice` | 출고가 | `1287000` |
| `monthlyProductSum` | 월 총 납부액 | `124598` |
| `monthlyProductCharge` | 월 단말 할부금 | `35598` |
| `monthlyCommCharge` | 월 요금제 | `89000` |
| `image1` | 이미지 경로 | `/A6/A6SQ/default/A6SQ_001_1.png` |
| `productColors` | 색상 배열 | `[{colorSeq, colorHex, colorName}]` |
| `productCapacity` | 용량 배열 | `[{phoneCapacity: "256G"}]` |
| `companyCode` | 제조사 코드 | `100CG` |
| `qckDlvPsblYn` | 퀵배송 가능 | `Y/N` |
| `reservationYn` | 예약 상품 여부 | `Y/N` |

### 응답 예시

```json
{
  "content": [
    {
      "modelName": "iPhone 17",
      "productGrpId": "000006958",
      "productPrice": 1287000,
      "monthlyProductSum": "124598",
      "monthlyProductCharge": "35598",
      "monthlyCommCharge": "89000",
      "image1": "/A6/A6SQ/default/A6SQ_001_1.png",
      "productColors": [
        {"colorSeq": "1", "colorHex": "C7D1AC", "colorName": "세이지"}
      ],
      "productCapacity": [
        {"phoneCapacity": "512G"},
        {"phoneCapacity": "256G"}
      ],
      "companyCode": "100CG"
    }
  ],
  "error": {"code": "00", "message": ""}
}
```

### 가입유형별 상품 수

| 가입유형 | 코드 | 상품 수 |
|---------|------|--------|
| 신규가입 | `11` | 44개 |
| 번호이동 | `20` | 43개 |
| 기기변경 | `31` | 44개 |

---

## API 호출 순서

### 1단계: 요금제 카테고리 목록 조회

```bash
curl -s "https://shop.tworld.co.kr/api/wireless/subscription/category?categoryId=20010014" \
  -H "User-Agent: Mozilla/5.0"
```

#### 응답 필드

| 필드 | 설명 | 예시 |
|-----|------|------|
| `categoryId` | 카테고리 ID | `20010031` |
| `categoryNm` | 카테고리명 | `5G 다이렉트` |
| `categoryType` | 카테고리 유형 | `SMALL` |
| `depth` | 깊이 | `3` |

#### 전체 카테고리 목록 (14개)

| 카테고리 ID | 카테고리명 | 요금제 수 |
|------------|-----------|----------|
| `20010030` | 5G 만34세이하 | 11개 |
| `20010031` | 5G 다이렉트 | 19개 |
| `20010032` | 5G 5GX플랜 | 21개 |
| `20010033` | 5G 0청년 | 22개 |
| `20010034` | 5G 청소년/어린이 | 3개 |
| `20010035` | 5G 시니어 | 3개 |
| `20010036` | 5G 베이직 | 6개 |
| `20010037` | LTE 다이렉트 | 1개 |
| `20010038` | LTE T플랜 | 4개 |
| `20010039` | LTE YOUNG | 3개 |
| `20010040` | LTE 청소년 | 3개 |
| `20010041` | LTE 어린이 | 2개 |
| `20010042` | LTE 어르신 | 3개 |
| `20010043` | LTE 기타 | 3개 |

---

### 2단계: 카테고리별 요금제 목록 조회

```bash
curl -s "https://shop.tworld.co.kr/api/wireless/subscription/list?type=1&noticeYn=Y&categoryId=20010031" \
  -H "User-Agent: Mozilla/5.0"
```

#### 요청 파라미터

| 파라미터 | 설명 | 값 |
|---------|------|-----|
| `type` | 조회 유형 | `1` |
| `noticeYn` | 공시 여부 | `Y` |
| `categoryId` | 카테고리 ID | 1단계에서 얻은 값 |

#### 응답 필드

| 필드 | 설명 | 예시 |
|-----|------|------|
| `subscriptionId` | 요금제 ID (prodId로 사용) | `NA00007790` |
| `subscriptionNm` | 요금제명 | `5GX 프라임` |
| `basicCharge` | 월 기본료 | `89000` |
| `dataOffer` | 데이터 제공량 | `완전무제한` |
| `callOffer` | 통화 제공량 | `무제한` |
| `smsOffer` | 문자 제공량 | `무제한` |

#### 주요 요금제 예시 (5G)

| 요금제 ID | 요금제명 | 월 요금 |
|----------|---------|--------|
| `NA00007790` | 5GX 프라임 | 89,000원 |
| `NA00007791` | 5GX 플래티넘 | 109,000원 |
| `NA00008553` | T 다이렉트 5G 69 | 69,000원 |
| `NA00008554` | T 다이렉트 5G 59 | 59,000원 |
| `NA00008555` | T 다이렉트 5G 49 | 49,000원 |
| `NA00007841` | 5GX 스탠다드 | 69,000원 |

---

### 2-1단계: 요금제 상세 정보 조회 (추가 발견)

```bash
curl -s "https://shop.tworld.co.kr/api/wireless/product/list/param-subscription?subscriptionId=NA00007790" \
  -H "User-Agent: Mozilla/5.0"
```

#### 응답 필드

| 필드 | 설명 | 예시 |
|-----|------|------|
| `subscriptionId` | 요금제 ID | `NA00007790` |
| `subscriptionNm` | 요금제명 | `5GX 프라임` |
| `basicCharge` | 월 기본료 | `89000` |
| `subcategoryId` | 서브카테고리 ID | `H` |
| `displayYn` | 표시 여부 | `Y` |

---

### 2-2단계: 제조사 목록 조회 (추가 발견)

```bash
curl -s "https://shop.tworld.co.kr/api/wireless/product/list/company?categoryId=20010001" \
  -H "User-Agent: Mozilla/5.0"
```

#### 응답

```json
{
  "content": [
    {"companyCd": "100SS", "companyNm": "삼성전자"},
    {"companyCd": "100CG", "companyNm": "Apple"},
    {"companyCd": "ETC", "companyNm": "기타"}
  ],
  "error": {"code": "00", "message": ""}
}
```

---

### 2-3단계: 할인방법 목록 조회 (추가 발견)

```bash
curl -s "https://shop.tworld.co.kr/api/wireless/product/list/comm-dc-method-list?subcategoryId=H" \
  -H "User-Agent: Mozilla/5.0"
```

#### 할인방법 코드 (subcommDcMthd)

| 코드 | 할인방법 | 약정기간 | 설명 |
|-----|---------|---------|------|
| `10` | 공통지원금A | 24개월 | 공시지원금 |
| `20` | 선택약정 | 12개월 | 선택약정 12개월 |
| `20` | 선택약정 | 24개월 | 선택약정 24개월 |
| `90` | 더 좋은 방법 | 24개월 | 요금할인/단말할인 중 유리한 것 |

---

### 3단계: 공시지원금 데이터 조회 (HTML 파싱)

```bash
curl -s "https://shop.tworld.co.kr/notice?modelNwType=5G&scrbTypCd=20&prodId=NA00007790&saleYn=Y" \
  -H "User-Agent: Mozilla/5.0"
```

#### 요청 파라미터

| 파라미터 | 설명 | 값 |
|---------|------|-----|
| `modelNwType` | 네트워크 유형 | `5G`, `LTE` |
| `scrbTypCd` | 가입 유형 코드 **(필수)** | 아래 표 참조 |
| `prodId` | 요금제 ID **(필수)** | 2단계에서 얻은 `subscriptionId` |
| `saleYn` | 판매중 여부 | `Y` (판매중), `N` (단종포함) |
| `dcMthdCd` | 할인방법 코드 | `10` (공시), `20` (선택약정) |

#### 가입 유형 코드 (scrbTypCd)

| 코드 | 가입 유형 | 비고 |
|-----|---------|------|
| `11` | 신규가입 | |
| `20` | 번호이동 | |
| `31` | 기기변경 | |

#### JSON 추출 방법

HTML 응답에서 `_this.products = parseObject([...]);` 패턴을 찾아 JSON 추출:

```bash
curl -s "https://shop.tworld.co.kr/notice?modelNwType=5G&scrbTypCd=20&prodId=NA00007790&saleYn=Y" | \
  sed -n 's/.*_this.products = parseObject(\[\(.*\)\]);.*/[\1]/p'
```

---

## 응답 데이터 구조 (전체 필드)

### 기본 정보

| 필드 | 설명 | 예시 |
|-----|------|------|
| `num` | 순번 | `1` |
| `productNm` | 기기명 | `갤럭시 Z 플립7` |
| `productMem` | 용량 | `256G` |
| `modelCd` | 모델 코드 | `A6N7` |
| `companyNm` | 제조사 | `삼성전자(주)` |
| `productGrpId` | 상품 그룹 ID | `000006917` |
| `phoneImg` | 이미지 경로 | `/A6/A6N7/default/A6N7_001_13.png` |
| `categoryId` | 카테고리 ID | `20010014` |
| `saleYn` | 판매 여부 | `Y` |

### 요금제 정보

| 필드 | 설명 | 예시 |
|-----|------|------|
| `prodId` | 요금제 ID | `NA00007790` |
| `prodNm` | 요금제명 | `5GX 프라임` |
| `scrbTypCd` | 가입 유형 | `20` |
| `dcMthdCd` | 할인 방법 | `10` |

### 가격 정보

| 필드 | 설명 | 예시 |
|-----|------|------|
| `factoryPrice` | 출고가 | `1485000` |
| `factorySaleAmt` | 출고가 할인 | `0` |
| `price` | 일반 구매가 | `905000` |
| `twdPrice` | T다이렉트 구매가 | `818000` |

### 공시지원금 관련 (핵심!)

| 필드 | 설명 | 예시 |
|-----|------|------|
| **`sumSaleAmt`** | **공시지원금 합계** | `580000` |
| `telecomSaleAmt` | 통신사 지원금 | `580000` |
| `twdSaleAmt` | T다이렉트 지원금 | `0` |
| `twdSumSaleAmt` | T다이렉트 지원금 합계 | `667000` |
| `saleAmtGrpId` | 지원금 그룹 ID | `PR70000143` |

### 추가지원금 관련

| 필드 | 설명 | 예시 |
|-----|------|------|
| **`dsnetSupmAmt`** | **추가지원금 (공시의 15%)** | `87000` |
| `selDsnetSupmAmt` | 선택약정 추가지원금 | `87000` |
| `nagrmtDsnetSupmAmt` | 무약정 추가지원금 | `0` |
| `sprateSupmAmt` | 분리지원금 | `0` |

### 선택약정 관련

| 필드 | 설명 | 예시 |
|-----|------|------|
| **`feeSaleAmt`** | **선택약정 요금할인 (24개월 합계)** | `534000` |
| `selSubcommSumSaleAmt` | 공시+추가 합계 | `621000` |
| `diffDiscount` | 차액할인 | `133000` |

### 번통혜택 관련 (btr = Better)

| 필드 | 설명 | 예시 |
|-----|------|------|
| `btrMSaleAmt` | 번통 M 할인 | `0` |
| `btrTwdSaleAmt` | 번통 T다이렉트 할인 | `0` |
| `btrSprateSupmAmt` | 번통 분리지원금 | `0` |
| `btrSumSaleAmt` | 번통 합계 | `0` |
| `btrTwdSumSaleAmt` | 번통 T다이렉트 합계 | `0` |
| `btrPrice` | 번통 가격 | `1485000` |
| `btrTwdPrice` | 번통 T다이렉트 가격 | `1485000` |
| `btrDsnetSupmAmt` | 번통 추가지원금 | `0` |

### 날짜 정보

| 필드 | 설명 | 예시 |
|-----|------|------|
| `effStaDt` | 효력 시작일 | `20250905` |
| `factoryDt` | 출고일 | `20250722` |

### 기타

| 필드 | 설명 | 예시 |
|-----|------|------|
| `productRentAmt` | 렌탈 금액 | `0` |
| `gbn` | 구분 | `1` |
| `rowspan` | 행 병합 | `2` |

---

## 할인 방식별 필드

| 할인 방식 | 사용 필드 | 설명 |
|----------|----------|------|
| 공시지원금 | `sumSaleAmt` | 공시지원금 |
| 공시지원금 | `dsnetSupmAmt` | 추가지원금 (공시의 15%) |
| 선택약정 | `feeSaleAmt` | 24개월 요금할인 합계 |

**참고**: 선택약정의 경우 공시지원금은 0원이며, `feeSaleAmt`만 적용됩니다.

---

## 데이터 수량

### saleYn별 기기 수

| saleYn | 설명 | 기기 수 |
|--------|------|--------|
| `Y` | 현재 판매중 | 약 97개 |
| `N` | 단종 포함 전체 | 약 425개 |

### 가입유형별 지원금 (동일 기기 기준)

**갤럭시 Z 플립7 256G + 5GX 프라임 기준:**

| 가입 유형 | 공시지원금 |
|----------|----------|
| 신규가입 (11) | 580,000원 |
| 번호이동 (20) | 580,000원 |
| 기기변경 (31) | 580,000원 |

※ 현재 동일 요금제에서는 가입유형별 지원금 차이가 없음

---

## 예시 응답 데이터 (전체 필드)

```json
{
  "num": 1,
  "phoneImg": "/A6/A6N7/default/A6N7_001_13.png",
  "modelCd": "A6N7",
  "companyNm": "삼성전자(주)",
  "productNm": "갤럭시 Z 플립7",
  "productGrpId": "000006917",
  "productMem": "256G",
  "productRentAmt": 0,
  "prodId": "NA00007790",
  "prodNm": "5GX 프라임",
  "categoryId": "20010014",
  "factoryPrice": 1485000,
  "factorySaleAmt": 0,
  "telecomSaleAmt": 580000,
  "twdSaleAmt": 0,
  "sumSaleAmt": 580000,
  "twdSumSaleAmt": 667000,
  "price": 905000,
  "twdPrice": 818000,
  "saleAmtGrpId": "PR70000143",
  "saleYn": "Y",
  "effStaDt": "20250905",
  "factoryDt": "20250722",
  "feeSaleAmt": 534000,
  "diffDiscount": 133000,
  "sprateSupmAmt": 0,
  "scrbTypCd": "20",
  "dcMthdCd": "10",
  "gbn": 1,
  "rowspan": 2,
  "btrMSaleAmt": 0,
  "btrTwdSaleAmt": 0,
  "btrSprateSupmAmt": 0,
  "btrSumSaleAmt": 0,
  "btrTwdSumSaleAmt": 0,
  "btrPrice": 1485000,
  "btrTwdPrice": 1485000,
  "dsnetSupmAmt": 87000,
  "btrDsnetSupmAmt": 0,
  "selDsnetSupmAmt": 87000,
  "nagrmtDsnetSupmAmt": 0,
  "selSubcommSumSaleAmt": 621000
}
```

---

## 전체 크롤링 플로우

```python
import requests
import re
import json

BASE_URL = "https://shop.tworld.co.kr"

# 1. 세션 생성
session = requests.Session()
session.headers.update({'User-Agent': 'Mozilla/5.0'})

# 2. 카테고리 목록 조회 (JSON API)
categories_response = session.get(
    f"{BASE_URL}/api/wireless/subscription/category",
    params={'categoryId': '20010014'}
)
categories = categories_response.json().get('content', [])

# 3. 각 카테고리별 요금제 목록 조회 (JSON API)
all_plans = []
for category in categories:
    category_id = category.get('categoryId')
    plans_response = session.get(
        f"{BASE_URL}/api/wireless/subscription/list",
        params={
            'type': '1',
            'noticeYn': 'Y',
            'categoryId': category_id
        }
    )
    plans = plans_response.json()
    if isinstance(plans, list):
        all_plans.extend(plans)

print(f"총 요금제 수: {len(all_plans)}개")

# 4. 각 요금제별 공시지원금 조회 (HTML 파싱)
for plan in all_plans:
    prod_id = plan.get('subscriptionId')
    prod_nm = plan.get('subscriptionNm')
    basic_charge = plan.get('basicCharge', 0)

    # 5G/LTE 판별
    model_nw_type = '5G' if basic_charge >= 40000 else 'LTE'

    # 가입유형별 조회 (번호이동 기준)
    notice_response = session.get(
        f"{BASE_URL}/notice",
        params={
            'modelNwType': model_nw_type,
            'scrbTypCd': '20',  # 번호이동
            'prodId': prod_id,
            'saleYn': 'Y'
        }
    )

    # HTML에서 JSON 추출
    html = notice_response.text
    match = re.search(r'_this\.products = parseObject\(\[(.*?)\]\);', html, re.DOTALL)

    if match:
        json_str = '[' + match.group(1) + ']'
        products = json.loads(json_str)

        for product in products:
            print(f"요금제: {prod_nm} (월 {basic_charge:,}원)")
            print(f"  기기: {product.get('productNm')} {product.get('productMem')}")
            print(f"  출고가: {product.get('factoryPrice'):,}원")
            print(f"  공시지원금: {product.get('sumSaleAmt'):,}원")
            print(f"  추가지원금: {product.get('dsnetSupmAmt'):,}원")
            print(f"  선택약정: {product.get('feeSaleAmt'):,}원")
            print(f"  공시일: {product.get('effStaDt')}")
            print()
```

---

## 이미지 URL 구조

```
이미지 기본 경로: https://cdnw.shop.tworld.co.kr/pimg/product

전체 URL 예시:
https://cdnw.shop.tworld.co.kr/pimg/product/A6/A6N7/default/A6N7_001_13.png

구조: /pimg/product/{시리즈}/{모델코드}/default/{모델코드}_{순번}_{사이즈}.png
```

---

## 성능 정보

| 항목 | 값 |
|-----|-----|
| HTML 페이지 크기 | 약 218KB |
| 추출된 JSON 크기 | 약 85KB |
| 요청 시간 | 약 300-400ms |
| 요청 횟수 | 요금제 수 × 가입유형 수 |

---

## 주의사항

1. **SSR 방식**: 순수 JSON API가 아닌 HTML에서 JSON 추출 필요
2. **prodId 필수**: 요금제 ID 없이 공시지원금 조회 불가
3. **scrbTypCd 필수**: 가입유형 코드 없이 조회 불가
4. **saleYn 권장**: `Y`로 설정해야 현재 판매중인 기기만 조회
5. **가입유형별 동일**: 현재 동일 요금제에서는 가입유형별 지원금 차이 없음
6. **JSON 파싱 주의**: `parseObject()` 함수로 감싸진 형태로 제공됨
7. **세션 API**: `/api/wireless/product/list/mobile-list` 등은 세션 쿠키 필요

---

## 🆕 tgiftInfo API (사은품 정보)

### 요청

```bash
curl -s "https://shop.tworld.co.kr/api/wireless/tgiftInfo?productGrpId=000006917" \
  -H "User-Agent: Mozilla/5.0" -H "Accept: application/json"
```

### 응답 예시

```json
{
  "error": {"code": "00", "message": ""},
  "count": "4",
  "lists": [
    {
      "giftGrpId": "GG0001369",
      "giftGrpNm": "[다이소] 3만원 이용권",
      "defaultGiftId": "000009496",
      "rank": "1",
      "listCnt": "4",
      "giftOptions": [
        {
          "giftId": "000009496",
          "giftNm": "[다이소] 3만원 이용권",
          "optValue": "공통",
          "image1": "/pimg/gift/000009496/000009496_Thumb1.png",
          "giftGb": "5",
          "giftRealYn": "N"
        }
      ]
    }
  ]
}
```

---

## 🆕 subscriptionInfo API (할인방식)

### 요청

```bash
curl -s "https://shop.tworld.co.kr/api/wireless/subscriptionInfo" \
  -H "User-Agent: Mozilla/5.0" -H "Accept: application/json"
```

### 응답

```json
{
  "error": {"code": "00", "message": ""},
  "subscriptionList": [],
  "subCommDcMethdList": [
    {"subcommDcMthd": "10", "subcommDcNm": "공통지원금A", "selSubcommTerm": null},
    {"subcommDcMthd": "20", "subcommDcNm": "선택약정", "selSubcommTerm": "12"},
    {"subcommDcMthd": "20", "subcommDcNm": "선택약정", "selSubcommTerm": "24"},
    {"subcommDcMthd": "00", "subcommDcNm": "무약정 플랜", "selSubcommTerm": null}
  ]
}
```

---

## 🆕 comm-dc-method-list API (할인방법 목록)

### 요청

```bash
curl -s "https://shop.tworld.co.kr/api/wireless/product/list/comm-dc-method-list?subcategoryId=H" \
  -H "User-Agent: Mozilla/5.0"
```

### 응답 요약

| 코드 | 할인방법 | 약정기간 | 설명 |
|-----|---------|---------|------|
| `90` | 더 좋은 방법 | 24개월 | 요금할인/단말할인 중 유리한 것 |
| `20` | 선택약정 | 12개월 | 선택약정 12개월 |
| `20` | 선택약정 | 24개월 | 선택약정 24개월 |
| `10` | 공통지원금 | 24개월 | 공시지원금 |

---

## 테스트 날짜

2025-12-06 (업데이트: mobile-list API 발견)
