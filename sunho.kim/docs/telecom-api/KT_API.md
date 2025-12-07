# KT 공시지원금 API 분석

## 개요

KT shop.kt.com에서 휴대폰 공시지원금 데이터를 가져오는 API 분석 결과입니다.

---

## API 호출 순서

### 1단계: 세션 획득

```bash
curl -c cookies.txt "https://shop.kt.com/smart/supportAmtList.do?channel=VS" \
  -H "User-Agent: Mozilla/5.0"
```

세션 쿠키가 필요합니다. 이 쿠키 없이는 API가 데이터를 반환하지 않습니다.

---

### 2단계: 요금제 목록 조회

```bash
curl -b cookies.txt "https://shop.kt.com/oneMinuteReform/supportAmtChoiceList.json" \
  -H "User-Agent: Mozilla/5.0" \
  -H "Referer: https://shop.kt.com/smart/supportAmtList.do?channel=VS" \
  -d "pageNo=1&pplType=5G&pplSelect=ALL&spnsMonsType=2&sortPpl=amtDesc&deviceType=HDP"
```

#### 요청 파라미터

| 파라미터 | 설명 | 값 |
|---------|------|-----|
| `pageNo` | 페이지 번호 | 1 |
| `pplType` | 요금제 유형 | `5G`, `LTE` |
| `pplSelect` | 요금제 그룹 | `ALL` (전체), 또는 그룹코드 |
| `spnsMonsType` | 약정 개월 | `2` (24개월) |
| `sortPpl` | 정렬 | `amtDesc` (높은가격순), `amtAsc` (낮은가격순) |
| `deviceType` | 기기 유형 | `HDP` (핸드폰), `PAD` (태블릿), `WATCH` (워치) |

#### 응답 필드 (punoPplList)

| 필드 | 설명 | 예시 |
|-----|------|------|
| `onfrmCd` | 요금제 코드 (prdcCd로 사용) | `PL244N945` |
| `pplNm` | 요금제명 | `티빙/지니/밀리 초이스 베이직` |
| `punoMonthUseChage` | 월 요금 | `90,000` |
| `punoMonthUseDcChage` | 선택약정 월 할인액 (요금의 25%) | `22500` |
| `pplGb` | 요금제 구분 | `5G`, `LTE` |
| `dataBasic` | 기본 데이터 | `완전무제한` |
| `tlkBasic` | 기본 통화 | `집/이동전화 무제한` |
| `charBasic` | 기본 문자 | `기본제공` |
| `prsnlShare` | 데이터 쉐어 | `100GB` |
| `rOAMING` | 로밍 | `3Mbps 무제한` |
| `pplGrpCd` | 요금제 그룹 코드 | `140` |
| `pplId` | 요금제 ID | `0942` |
| `chageNote` | 요금제 상세 페이지 경로 | `/content/openshop/price/5G/...` |

#### 요금제 수

| 유형 | 핸드폰(HDP) | 태블릿(PAD) | 워치(WATCH) |
|-----|------------|------------|-------------|
| 5G | 62개 | 3개 | 0개 |
| LTE | 31개 | 1개 | 1개 |

#### 요금제 예시 (5G 핸드폰)

| 요금제 코드 | 요금제명 | 월 요금 |
|------------|---------|--------|
| `PL244N943` | 티빙/지니/밀리 초이스 프리미엄 | 130,000원 |
| `PL244N944` | 티빙/지니/밀리 초이스 스페셜 | 110,000원 |
| `PL244N945` | 티빙/지니/밀리 초이스 베이직 | 90,000원 |
| `5GINTMPP8` | 5G Y틴 | 47,000원 |
| `PL21BT586` | 5G 주니어 슬림 | 28,000원 |

#### 요금제 예시 (LTE 핸드폰)

| 요금제 코드 | 요금제명 | 월 요금 |
|------------|---------|--------|
| `KDRTLTE90` | 데이터ON 프리미엄 | 89,000원 |
| `PL227R448` | 데이터ON 비디오 플러스 | 69,000원 |
| `PL19B5412` | 데이터 ON 나눔 | 49,000원 |
| `NBIGI0001` | Y틴 ON | 33,000원 |
| `KDRTLTE40` | LTE 베이직 | 33,000원 |

---

### 3단계: 공시지원금 리스트 조회

```bash
curl -b cookies.txt "https://shop.kt.com/mobile/retvSuFuList.json" \
  -H "User-Agent: Mozilla/5.0" \
  -H "Referer: https://shop.kt.com/smart/supportAmtList.do?channel=VS" \
  -d "prodNm=mobile&prdcCd=PL244N945&prodType=30&deviceType=HDP&makrCd=&sortProd=oBspnsrPunoDateDesc&spnsMonsType=2&dscnOptnCd=HT&sbscTypeCd=04&pageNo=1"
```

#### 요청 파라미터

| 파라미터 | 설명 | 값 |
|---------|------|-----|
| `prodNm` | 상품명 | `mobile` (휴대폰), `pad` (태블릿), `watch` (워치) |
| `prdcCd` | 요금제 코드 **(필수)** | 2단계에서 얻은 `onfrmCd` 값 |
| `prodType` | 상품 유형 | 아래 표 참조 |
| `deviceType` | 기기 유형 | `HDP` (폰), `PAD` (태블릿), `WATCH` (워치) |
| `makrCd` | 제조사 코드 | `` (전체), `13` (삼성), `15` (Apple), `99` (기타) |
| `sortProd` | 정렬 기준 | `oBspnsrPunoDateDesc` (최근공시순) - **유일하게 작동하는 옵션** |
| `spnsMonsType` | 약정 개월 | `2` (24개월) |
| `dscnOptnCd` | 할인 옵션 코드 | 아래 표 참조 |
| `sbscTypeCd` | 가입 유형 | `01` (신규), `02` (번호이동), `04` (기기변경) |
| `pageNo` | 페이지 번호 | 1, 2, 3... |

#### 할인 옵션 코드 (dscnOptnCd)

| 코스 | 신규(01) | 번호이동(02) | 기기변경(04) |
|------|---------|-------------|-------------|
| 심플 | `NT` | `MT` | `HT` |
| 심플2 | `NS` | `MS` | `HS` |
| 베이직 | `NK` | `MK` | `HK` |

#### 제조사 코드 (makrCd)

| 코드 | 제조사 | 기기 수 (5G 폰 기준) |
|-----|-------|---------------------|
| `` (빈값) | 전체 | 112개 |
| `13` | 삼성 | 44개 |
| `15` | Apple | 56개 |
| `02` | 샤오미 | 12개 |
| `19` | 모토로라 | 12개 |
| `99` | 기타 | 12개 |

**참고**: makrCd=02, 19, 99 모두 같은 12개 반환 (기타 제조사 그룹으로 묶여있음)

#### 응답 필드 (LIST_DATA)

| 필드 | 설명 | 예시 |
|-----|------|------|
| `prodNo` | 상품 번호 | `WL00076372` |
| `petNm` | 기기명 | `갤럭시 S25+ 512GB` |
| `hndsetModelNm` | 모델 코드 | `SM-S936NK512` |
| `hndsetModelId` | 모델 ID | `K7039551` |
| `ofwAmt` | 출고가 | `1496000` |
| **`ktSuprtAmt`** | **KT 공시지원금 (핵심!)** | `500000` |
| `realAmt` | 실결제가 | `996000` |
| `monthUseChageDcAmt` | 요금할인(24개월 합계) | `540000` |
| `spnsrPunoDate` | 공시일자 | `20251121000000` |
| `pplId` | 요금제 ID | `0942` |
| `pplNm` | 요금제명 | `티빙/지니/밀리 초이스 베이직` |
| `hndSetImgNm` | 이미지 경로 | `/upload/public_notice/...` |
| `makrCd` | 제조사 코드 | `13` |
| `saleSttusCd` | 판매 상태 코드 | `03` |

#### 페이지네이션 (pageInfoBean)

| 필드 | 설명 | 예시 |
|-----|------|------|
| `pageNo` | 현재 페이지 | `1` |
| `recordCount` | 페이지당 레코드 수 | `12` |
| `totalCount` | 전체 레코드 수 | `112` |
| `totalPageCount` | 전체 페이지 수 | `10` |
| `firstPageNo` | 첫 페이지 | `1` |
| `lastPageNo` | 마지막 페이지 | `10` |

---

## 상품 유형 코드 매핑

| 카테고리 | prodNm | prodType | deviceType | pplType |
|---------|--------|----------|------------|---------|
| 5G 핸드폰 | mobile | 30 | HDP | 5G |
| LTE 핸드폰 | mobile | 15 | HDP | LTE |
| 5G 태블릿 | pad | 34 | PAD | 5G |
| LTE 태블릿 | pad | 18 | PAD | LTE |
| 워치 | watch | 16 | WATCH | LTE |
| 키즈워치 | - | 15 | KIDSWATCH | - |
| 5G 에그 | - | - | EGG | 5G |

---

## 요금제 그룹 목록

```bash
curl -b cookies.txt "https://shop.kt.com/oneMinuteReform/supportAmtChoiceGroupList.json" \
  -H "User-Agent: Mozilla/5.0" \
  -H "Referer: https://shop.kt.com/smart/supportAmtList.do?channel=VS" \
  -d "pageNo=1&pplType=5G&spnsMonsType=2&deviceType=HDP"
```

#### 5G 요금제 그룹 (핸드폰)

| 그룹 코드 | 그룹명 |
|----------|--------|
| `140` | 5G 초이스 |
| `93` | 5G 일반 |
| `96` | 5G 청소년 |
| `145` | 5G 주니어 |
| `147` | 5G 시니어 |
| `146` | 5G 군인 |
| `99` | 5G 복지 |
| `120` | 5G 외국인 |

#### LTE 요금제 그룹 (핸드폰)

| 그룹 코드 | 그룹명 |
|----------|--------|
| `90` | 데이터ON |
| `91` | Y데이터ON |
| `114` | LTE 일반 |
| `240` | LTE Y틴(청소년) |
| `242` | LTE Y 주니어 |
| `110` | LTE 시니어 |
| `117` | Y 군인 |
| `31` | LTE 복지 |
| `247` | 순 선택형(LTE) |
| `248` | 순 망내무한 선택형(LTE) |

#### 태블릿/워치 요금제 그룹

| deviceType | pplType | 그룹 코드 | 그룹명 |
|------------|---------|----------|--------|
| PAD | 5G | `97` | 스마트기기 요금제(5G) |
| WATCH | LTE | `300` | 스마트기기 요금제(LTE) |

#### pplSelect 파라미터 사용법

```bash
# 전체 요금제 조회
pplSelect=ALL

# 5G 초이스 그룹만 조회
pplSelect=140

# 5G 청소년 그룹만 조회
pplSelect=96
```

---

## 추천 요금제 조회

```bash
curl -b cookies.txt "https://shop.kt.com/oneMinuteReform/defaultRecommPplList.json" \
  -H "User-Agent: Mozilla/5.0" \
  -H "Referer: https://shop.kt.com/smart/supportAmtList.do?channel=VS" \
  -d "pageNo=1&pplType=5G&spnsMonsType=2&deviceType=HDP"
```

기본 추천 요금제: `티빙/지니/밀리 초이스 베이직` (PL244N945, 월 90,000원)

---

## 태블릿/워치 공시지원금 조회

### 5G 태블릿 조회 예시

```bash
curl -b cookies.txt "https://shop.kt.com/mobile/retvSuFuList.json" \
  -H "User-Agent: Mozilla/5.0" \
  -H "Referer: https://shop.kt.com/smart/supportAmtList.do?channel=VS" \
  -d "prodNm=pad&prdcCd=PL2097135&prodType=34&deviceType=PAD&spnsMonsType=2&dscnOptnCd=HT&sbscTypeCd=04&pageNo=1"
```

### 5G 태블릿 요금제

| 요금제 코드 | 요금제명 | 월 요금 |
|------------|---------|--------|
| `PL2097135` | 5G 스마트기기 28GB | 30,000원 |
| `PL2097136` | 5G 스마트기기 14GB | 19,800원 |
| `PL19C4537` | 5G 데이터투게더 | 19,800원 |

### 5G 태블릿 기기 예시 (49개)

| 기기 | 출고가 | 지원금 |
|------|-------|-------|
| iPad Air 13 (M3) 128GB | 1,496,000원 | 100,000원 |
| 갤럭시 탭 S10 FE Plus 5G | 1,028,500원 | 200,000원 |
| 갤럭시 탭 S10 FE 5G | 858,000원 | 200,000원 |

### LTE 워치 조회 예시

```bash
curl -b cookies.txt "https://shop.kt.com/mobile/retvSuFuList.json" \
  -H "User-Agent: Mozilla/5.0" \
  -H "Referer: https://shop.kt.com/smart/supportAmtList.do?channel=VS" \
  -d "prodNm=watch&prdcCd=DATATGWRA&prodType=16&deviceType=WATCH&spnsMonsType=2&dscnOptnCd=HT&sbscTypeCd=04&pageNo=1"
```

### LTE 워치 요금제

| 요금제 코드 | 요금제명 | 월 요금 |
|------------|---------|--------|
| `DATATGWRA` | 데이터투게더 Watch | 11,000원 |

### LTE 워치 기기 예시 (26개)

| 기기 | 출고가 | 지원금 |
|------|-------|-------|
| 갤럭시 워치6 40mm | 359,700원 | 180,000원 |
| 갤럭시 워치8 44mm | 489,500원 | 150,000원 |
| 갤럭시 워치8 클래식 46mm | 599,500원 | 150,000원 |

---

## 테스트 결과 요약

### 기기 수량 (요금제별)

| 기기 유형 | 요금제 예시 | 기기 수 |
|----------|------------|--------|
| 5G 핸드폰 | 티빙/지니/밀리 초이스 베이직 (90,000원) | 112개 |
| 5G 태블릿 | 5G 스마트기기 28GB (30,000원) | 49개 |
| LTE 태블릿 | 데이터투게더Large (11,000원) | 49개 |
| LTE 워치 | 데이터투게더 Watch (11,000원) | 26개 |

### 가입유형별 지원금 (동일 요금제 기준)

**130,000원 요금제(5G 초이스 프리미엄) 기준:**

| 기기 | 신규가입(01) | 번호이동(02) | 기기변경(04) |
|------|------------|------------|------------|
| 갤럭시 A17 128GB | **300,000원** | 250,000원 | 250,000원 |
| 갤럭시 S25+ 512GB | 500,000원 | 500,000원 | 500,000원 |
| 갤럭시 Z Fold6 1TB | 700,000원 | 700,000원 | 700,000원 |
| iPhone 17 512GB | 450,000원 | 450,000원 | 450,000원 |

**중요 발견**: 일부 기기(갤럭시 A17 등)는 신규가입 시 지원금이 더 높음!

### 요금제별 지원금 차이 예시

동일 기기(갤럭시 A17 128GB)의 요금제별 지원금:

| 요금제 | 월 요금 | 공시지원금 |
|-------|--------|----------|
| 티빙/지니/밀리 초이스 프리미엄 | 130,000원 | 250,000원 |
| 티빙/지니/밀리 초이스 베이직 | 90,000원 | 150,000원 |
| 5G Y틴 | 47,000원 | 86,000원 |
| 5G 주니어 슬림 | 28,000원 | 50,000원 |

**핵심 인사이트**: 월 요금이 높은 요금제일수록 공시지원금이 높음

### LTE 요금제 지원금 비교

동일 기기의 5G vs LTE 요금제 지원금:

| 기기 | LTE 90 (89,000원) | LTE 40 (33,000원) | 비율 |
|------|-----------------|-----------------|------|
| 갤럭시 A17 128GB | 150,000원 | 60,000원 | 2.5배 |
| 갤럭시 S25+ 512GB | 500,000원 | 183,000원 | 2.7배 |
| iPhone 17 512GB | 450,000원 | 141,000원 | 3.2배 |

**핵심 인사이트**: 월 요금이 약 2.7배 차이나면 공시지원금은 약 2.5~3배 차이남

### 아이폰 지원금 특이사항

5G 슈퍼 프리미엄 (130,000원) 요금제 기준 아이폰 지원금:

| 기기 | 출고가 | 공시지원금 |
|------|-------|----------|
| iPhone 17 256GB | 1,287,000원 | 450,000원 |
| iPhone 17 512GB | 1,584,000원 | 450,000원 |
| iPhone 17 Pro 256GB | 1,782,000원 | 450,000원 |
| iPhone 17 Pro Max 256GB | 1,980,000원 | **250,000원** |
| iPhone 17 Pro Max 2TB | 3,190,000원 | **250,000원** |

**발견**: 최고가 기기인 iPhone 17 Pro Max가 오히려 지원금이 가장 낮음 (250,000원)

### 전체 기기 통계 (5G 130,000원 요금제 기준)

| 항목 | 값 |
|-----|-----|
| 총 기기 수 | 112개 |
| 삼성 | 44개 |
| 애플 | 56개 |
| 기타 | 12개 |
| 출고가 범위 | 199,100원 ~ 3,190,000원 |
| 지원금 범위 | 184,000원 ~ 700,000원 |
| 판매중 | 57개 |
| 단종 등 | 55개 |

### 지원금 TOP 5 / BOTTOM 5

**지원금 TOP 5:**

| 기기 | 공시지원금 |
|------|----------|
| 갤럭시 Z Fold6 1TB | 700,000원 |
| 갤럭시 S24 Ultra 512GB | 700,000원 |
| 갤럭시 S24+ 512GB | 700,000원 |
| 갤럭시 S24 512GB | 700,000원 |
| 갤럭시 S24 Ultra | 700,000원 |

**지원금 BOTTOM 5:**

| 기기 | 공시지원금 |
|------|----------|
| moto g34 5G 128GB | 184,000원 |
| 샤오미 레드미 14C | 190,000원 |
| 클래식 폴더 64GB | 205,000원 |
| 시나모롤 키즈폰 | 239,000원 |
| 샤오미 레드미노트 12 | 249,000원 |

---

## 선택약정(요금할인) 계산

### 계산 공식

```
선택약정 할인 = 월 요금 × 25% × 24개월
```

### 요금제별 선택약정 예시

| 월 요금 | 월 할인액 (25%) | 24개월 총 할인 |
|--------|---------------|---------------|
| 130,000원 | 32,500원 | 780,000원 |
| 90,000원 | 22,500원 | 540,000원 |
| 47,000원 | 11,750원 | 282,000원 |
| 28,000원 | 7,000원 | 168,000원 |

### 관련 응답 필드

| 필드 | 설명 | 예시 (90,000원 요금제) |
|-----|------|---------------------|
| `monthUseChageDcAmt` | 선택약정 24개월 총 할인액 | `540000` |
| `punoMonthUseDcChage` | 월 할인액 (요금의 25%) | `22500` |

### 공시지원금 vs 선택약정 비교

갤럭시 S25+ 512GB (출고가 1,496,000원) 기준:

| 할인 방식 | 90,000원 요금제 |
|----------|---------------|
| 공시지원금 | 500,000원 |
| 선택약정(24개월) | 540,000원 |
| **차이** | **선택약정이 40,000원 더 유리**

---

## 예시 응답 데이터

```json
{
  "LIST_DATA": [
    {
      "prodNo": "WL00076372",
      "petNm": "갤럭시 S25+ 512GB",
      "hndsetModelNm": "SM-S936NK512",
      "hndsetModelId": "K7038219",
      "ofwAmt": 1496000,
      "ktSuprtAmt": 500000,
      "realAmt": 996000,
      "monthUseChageDcAmt": 540000,
      "spnsrPunoDate": "20251121000000",
      "pplId": "0942",
      "pplNm": "티빙/지니/밀리 초이스 베이직",
      "makrCd": "13",
      "hndSetImgNm": "/upload/public_notice/7293/1738575712545.png"
    }
  ],
  "pageInfoBean": {
    "pageNo": 1,
    "totalCount": 112,
    "recordCount": 12,
    "totalPageCount": 10
  }
}
```

---

## 전체 크롤링 플로우

```python
import requests

# 1. 세션 생성
session = requests.Session()
session.headers.update({'User-Agent': 'Mozilla/5.0'})

# 2. 세션 쿠키 획득
session.get('https://shop.kt.com/smart/supportAmtList.do?channel=VS')

# 3. 요금제 목록 조회
plans_response = session.post(
    'https://shop.kt.com/oneMinuteReform/supportAmtChoiceList.json',
    data={
        'pageNo': 1,
        'pplType': '5G',
        'pplSelect': 'ALL',
        'spnsMonsType': '2',
        'sortPpl': 'amtDesc',
        'deviceType': 'HDP'
    },
    headers={'Referer': 'https://shop.kt.com/smart/supportAmtList.do?channel=VS'}
)
plans = plans_response.json().get('punoPplList', [])

# 4. 각 요금제별 지원금 조회
for plan in plans:
    plan_code = plan.get('onfrmCd')
    plan_name = plan.get('pplNm')
    monthly_fee = plan.get('punoMonthUseChage')

    # 5. 모든 페이지 조회
    page = 1
    while True:
        support_response = session.post(
            'https://shop.kt.com/mobile/retvSuFuList.json',
            data={
                'prodNm': 'mobile',
                'prdcCd': plan_code,
                'prodType': '30',
                'deviceType': 'HDP',
                'makrCd': '',  # 전체 제조사
                'sortProd': 'oBspnsrPunoDateDesc',
                'spnsMonsType': '2',
                'dscnOptnCd': 'HT',  # 기기변경-심플
                'sbscTypeCd': '04',  # 기기변경
                'pageNo': page
            },
            headers={'Referer': 'https://shop.kt.com/smart/supportAmtList.do?channel=VS'}
        )

        data = support_response.json()
        devices = data.get('LIST_DATA', [])
        page_info = data.get('pageInfoBean', {})

        for device in devices:
            print(f"요금제: {plan_name} (월 {monthly_fee}원)")
            print(f"  기기: {device.get('petNm')}")
            print(f"  모델: {device.get('hndsetModelNm')}")
            print(f"  출고가: {device.get('ofwAmt'):,}원")
            print(f"  지원금: {device.get('ktSuprtAmt'):,}원")
            print(f"  실결제가: {device.get('realAmt'):,}원")
            print(f"  공시일: {device.get('spnsrPunoDate')}")

        # 다음 페이지 확인
        if page >= page_info.get('totalPageCount', 1):
            break
        page += 1
```

---

## Edge Case 테스트 결과

| 테스트 케이스 | 결과 |
|-------------|------|
| prdcCd 없이 호출 | 0개 반환 (필수 파라미터) |
| 잘못된 prdcCd (예: `INVALID`) | 0개 반환 |
| searchNm 파라미터 | 필터링 안됨 (무시됨) |
| hndsetModelId 파라미터 | 필터링 안됨 (무시됨) |
| sortProd=oOfwAmtDesc (출고가 높은순) | 작동 안함 (무시됨) |
| sortProd=oOfwAmtAsc (출고가 낮은순) | 작동 안함 (무시됨) |
| sortProd=oKtSuprtAmtDesc (지원금순) | 작동 안함 (무시됨) |
| sortProd=oRealAmtDesc (실판가순) | 작동 안함 (무시됨) |
| dscnOptnCd=HS (기변-심플2) | 0개 반환 (해당 요금제에서 미지원) |
| strRow/toRow 직접 지정 | 무시됨 (페이지당 12개 고정) |

**결론**:
- 기기 검색/필터링은 클라이언트 사이드에서 처리해야 함
- 정렬 옵션은 `oBspnsrPunoDateDesc` (최근공시순)만 작동함
- 일부 요금제에서는 심플2 옵션이 없을 수 있음 (0개 반환)

---

## 이미지 URL 구조

```
기기 이미지 경로: /upload/public_notice/{폴더ID}/{타임스탬프}.png

전체 URL 예시:
- PC: https://shop.kt.com/upload/public_notice/7293/1738575712545.png
- Mobile: https://shop.kt.com/upload/public_notice/7293/1738575718141.png
```

**주의**: 일부 이미지 URL은 404를 반환할 수 있음 (이미지 삭제/변경 시)

---

## 주의사항

1. **세션 필수**: 쿠키 없이 API 호출 시 데이터가 반환되지 않음
2. **prdcCd 필수**: 요금제 코드 없이 지원금 조회 불가 (0개 반환)
3. **페이징**: 한 페이지에 12개씩 반환, `pageNo`로 페이지네이션
4. **요금제별 지원금 상이**: 동일 기기라도 요금제에 따라 지원금이 다름
5. **Referer 필수**: Referer 헤더 없이 호출 시 일부 API가 동작하지 않을 수 있음
6. **HTML 인코딩**: 일부 응답값에 HTML 엔티티 인코딩 포함 (예: `&#x2F;` → `/`)
7. **기기 검색 미지원**: searchNm, hndsetModelId 등의 파라미터로 기기 필터링 불가
8. **제조사 필터링**: makrCd로만 기기 필터링 가능 (13=삼성, 15=Apple, 99=기타)
9. **정렬 옵션 제한**: `oBspnsrPunoDateDesc` (최근공시순)만 작동, 다른 정렬은 무시됨
10. **지원금 역전 현상**: 최고가 기기가 반드시 최고 지원금을 받지 않음 (예: iPhone 17 Pro Max)
11. **가입유형별 차이**: 일부 기기는 신규가입 시 지원금이 더 높음 (갤럭시 A17: 신규 300,000원 vs 번이/기변 250,000원)

---

## 관련 API 엔드포인트

| API | 용도 |
|-----|------|
| `/smart/supportAmtList.do?channel=VS` | 세션 획득용 메인 페이지 |
| `/oneMinuteReform/supportAmtChoiceList.json` | 요금제 목록 조회 |
| `/oneMinuteReform/supportAmtChoiceGroupList.json` | 요금제 그룹 목록 조회 |
| `/oneMinuteReform/defaultRecommPplList.json` | 추천 요금제 조회 |
| `/mobile/retvSuFuList.json` | 공시지원금 리스트 조회 |

---

## 테스트 날짜

2025-12-06

---

## LG U+ API와의 비교 분석

### API 구조 비교

| 항목 | KT | LG U+ |
|------|-----|-------|
| 인증 | 세션 쿠키 필수 | 인증 불필요 |
| 페이지 사이즈 | **12개 고정** (변경 불가) | 1~1000개 자유롭게 설정 |
| 검색 기능 | **미지원** | `shwd` 파라미터로 키워드 검색 가능 |
| 정렬 옵션 | 최근공시순만 작동 | 4가지 정렬 옵션 모두 작동 |
| 필터 API | 없음 | `/filter-list` 전용 API 있음 |
| 기기 상세 API | 없음 | 색상/스펙/추천요금제 API 있음 |
| 6개월 약정 | 지원 (spnsMonsType=1) | 지원 |
| 제조사 필터 | 지원 (makrCd) | 지원 (dvicManfCds) |

### 주요 차이점 요약

1. **KT는 세션 필수** - LG U+와 달리 쿠키 없이는 0개 반환
2. **KT는 페이지네이션 고정** - 12개씩만 가져올 수 있어 전체 데이터 수집 시 여러 번 호출 필요
3. **KT는 검색/정렬 제한적** - 클라이언트 사이드 처리 필요
4. **KT는 기기 상세 API 없음** - 공시지원금 목록만 제공

---

## 전체 응답 필드 (33개)

### LIST_DATA 기기 필드 상세

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| `prodNo` | string | 상품 번호 | `WL00076372` |
| `petNm` | string | 기기명 (한글) | `갤럭시 A17 128GB` |
| `hndsetModelId` | string | KT 내부 모델 ID | `K7039551` |
| `hndsetModelNm` | string | 제조사 모델 코드 | `SM-A175NK` |
| `makrCd` | string | 제조사 코드 | `13` (삼성), `15` (Apple) |
| `ofwAmt` | number | 출고가 | `319000` |
| `ktSuprtAmt` | number | **KT 공시지원금** | `150000` |
| `realAmt` | number | 실결제가 (출고가-지원금) | `169000` |
| `monthUseChageDcAmt` | number | 선택약정 24개월 총 할인액 | `540000` |
| `spnsrPunoDate` | string | 공시일자 (YYYYMMDDHHMMSS) | `20251201000000` |
| `pplId` | string | 요금제 ID | `0942` |
| `pplNm` | string | 요금제명 | `티빙/지니/밀리 초이스 베이직` |
| `pplGroupDivCd` | string | 요금제 그룹 코드 | `140` |
| `hndSetImgNm` | string | PC용 이미지 경로 | `/upload/public_notice/...` |
| `mshopHndSetImgNm` | string | 모바일용 이미지 경로 | `/upload/public_notice/...` |
| `saleSttusCd` | string | 판매 상태 코드 | `01` (판매중), `03` (단종) |
| `dispYn` | string | 표시 여부 | `Y` / `N` |
| `showOdrg` | number | 정렬 순서 | `2156` |

### 미사용/예약 필드 (현재 값 없음)

| 필드 | 추정 용도 | 현재 상태 |
|------|----------|---------|
| `tdKtSuprtAmt` | 단말기 지원금? | None |
| `tcKtSuprtAmt` | 단말기 전환지원금? | None |
| `kdKtSuprtAmt` | KT 단말 지원금? | None |
| `kcKtSuprtAmt` | KT 전환 지원금? | None |
| `storSuprtAmt` | 대리점 지원금 | None |
| `convSupotProdYn` | 전환지원 대상 여부 | None |
| `deviceType` | 기기 유형 (요청값 그대로) | None |
| `dscnOptnCd` | 할인 옵션 (요청값 그대로) | None |
| `dispCtgCd` | 전시 카테고리 | None |
| `prdcCd` | 요금제 코드 (요청값) | None |
| `spnsGrpCd` | 지원 그룹 코드 | None |
| `spnsMonsType` | 약정 개월 (요청값) | None |
| `sortProd` | 정렬 (요청값) | None |
| `strRow` | 시작 행 | None |
| `toRow` | 종료 행 | None |

### supportFundReqBean (요청 파라미터 반영)

API 응답에 포함되는 요청 파라미터 에코:

```json
{
  "sortProd": "oBspnsrPunoDateDesc",
  "dscnOptnCd": "HT",
  "dispCtgCd": "mobile",
  "prdcCd": "PL244N945",
  "spnsMonsType": "2",
  "strRow": 0,
  "toRow": 12,
  "deviceType": "HDP"
}
```

---

## 요금제별 지원금 차이 상세

### 핵심 발견: 월 요금이 높을수록 지원금 증가

동일 기기의 요금제별 공시지원금 비교:

#### 갤럭시 A17 128GB

| 요금제 코드 | 요금제명 | 월 요금 | 공시지원금 |
|------------|---------|--------|----------|
| `PL244N943` | 티빙/지니/밀리 초이스 스페셜 | 100,000원 | **250,000원** |
| `PL244N945` | 티빙/지니/밀리 초이스 베이직 | 90,000원 | 150,000원 |
| `5GINTMPP8` | 5G 심플 베이직 | 49,000원 | 86,000원 |
| `PL21BT586` | 5G 심플 에센셜 | 54,000원 | 50,000원 |

#### 갤럭시 S25+ 512GB

| 요금제 코드 | 요금제명 | 월 요금 | 공시지원금 |
|------------|---------|--------|----------|
| `PL244N945` | 티빙/지니/밀리 초이스 베이직 | 90,000원 | **500,000원** |
| `PL244N943` | 티빙/지니/밀리 초이스 스페셜 | 100,000원 | **500,000원** |
| `5GINTMPP8` | 5G 심플 베이직 | 49,000원 | 260,000원 |
| `PL21BT586` | 5G 심플 에센셜 | 54,000원 | 156,000원 |

#### iPhone 17 512GB

| 요금제 코드 | 월 요금 | 공시지원금 |
|------------|--------|----------|
| `PL244N945` | 90,000원 | **450,000원** |
| `PL244N943` | 100,000원 | **450,000원** |
| `5GINTMPP8` | 49,000원 | 220,000원 |
| `PL21BT586` | 54,000원 | 113,000원 |

### 인사이트

- 월 요금 90,000원 이상 요금제: 지원금 최대치
- 월 요금 49,000원: 지원금 약 50% 수준
- 월 요금 28,000원: 지원금 약 30% 수준

---

## 제조사 코드 (makrCd) 업데이트

| 코드 | 제조사 | 기기 수 (5G 폰 기준) |
|-----|-------|---------------------|
| `` (빈값) | 전체 | 112개 |
| `13` | 삼성 | 44개 |
| `15` | Apple | 56개 |
| `02` | 샤오미 | 8개 |
| `19` | 모토로라 | 2개 |
| `22` | 기타 (폴더폰) | 1개 |

**새 발견**: 제조사 코드 `22` - 클래식 폴더 64GB

---

## API 제한 사항 정리

### 페이지네이션

| 항목 | 값 | 비고 |
|-----|-----|------|
| 페이지당 결과 수 | **12개 고정** | toRow/strRow 파라미터 무시됨 |
| 최대 조회 방법 | 페이지 반복 | 전체 데이터는 여러 번 호출 필요 |

### 검색/필터링

| 기능 | 상태 |
|------|------|
| 키워드 검색 | ❌ 미지원 (searchWord, shwd 등 무시) |
| 제조사 필터 | ✅ 지원 (makrCd) |
| 요금제 필터 | ✅ 지원 (prdcCd 필수) |
| 기기유형 필터 | ✅ 지원 (deviceType) |

### 정렬

| 파라미터 | 상태 |
|---------|------|
| `oBspnsrPunoDateDesc` (최근공시순) | ✅ 작동 |
| `oOfwAmtDesc` (출고가 높은순) | ❌ 무시 |
| `oOfwAmtAsc` (출고가 낮은순) | ❌ 무시 |
| `oKtSuprtAmtDesc` (지원금순) | ❌ 무시 |

### pplType 파라미터

| 테스트 | 결과 |
|-------|------|
| `pplType=5G` | 112개 반환 |
| `pplType=LTE` | 112개 반환 |
| 미지정 | 112개 반환 |

**결론**: pplType 파라미터는 retvSuFuList.json에서 무시됨

---

## defaultRecommPplList 응답 상세

```json
{
  "recommPplBean": {
    "rcmdPplId": "0942",
    "rcmdPplGroupDivCd": "140",
    "rcmdPplNm": "티빙/지니/밀리 초이스 베이직",
    "onfrmCd": "PL244N945",
    "rcmdPplCharge": 90000,
    "monthUseChageDcAmt": 22500
  }
}
```

| 필드 | 설명 |
|------|------|
| `rcmdPplId` | 추천 요금제 ID |
| `rcmdPplGroupDivCd` | 추천 요금제 그룹 코드 |
| `onfrmCd` | 요금제 코드 (prdcCd로 사용) |
| `rcmdPplCharge` | 월 요금 |
| `monthUseChageDcAmt` | 선택약정 월 할인액 (25%)

---

## JavaScript 번들 분석으로 발견된 전체 API 목록

### 분석된 JavaScript 파일

| 파일 | 크기 | 경로 |
|------|------|------|
| shopLayer.js | 47KB | `/common/pc/js/shopLayer.js` |
| kt_common.js | 25KB | `/common/pc/js/kt_common.js` |
| ollehShopCommon.js | 61KB | `/js/common/ollehShopCommon.js` |
| supportAmt.list.js | 53KB | `/js/pc/smart/supports/supportAmt.list.js` |
| mobile_common.js | 20KB | `/common/mobile/js/mobile_common.js` |
| mobile_shopLayer.js | 65KB | `/common/mobile/js/shopLayer.js` |
| mobile_supportAmt.list.js | 57KB | `/js/mobile/smart/supports/supportAmt.list.js` |

**총 분석된 JS 크기**: 약 328KB

---

## 발견된 전체 JSON API 엔드포인트

### 정상 작동 API (9개)

| API | 용도 | 방식 | 세션 | 상태 |
|-----|------|------|------|------|
| `/mobile/retvSuFuList.json` | **공시지원금 목록** | POST | 필요 | ✅ 정상 |
| `/oneMinuteReform/supportAmtChoiceList.json` | 요금제 목록 (98개) | POST | 필요 | ✅ 정상 |
| `/oneMinuteReform/supportAmtChoiceGroupList.json` | 요금제 그룹 목록 (20개) | POST | 필요 | ✅ 정상 |
| `/oneMinuteReform/defaultRecommPplList.json` | 추천 요금제 | POST | 필요 | ✅ 정상 |
| `/wire/getPageRturnType.json` | 유선 상품 정보 | GET | 불필요 | ✅ 정상 |
| `/smart/punoChngNotiCmplt.json` | 요금제 변경 알림 완료 | POST | 필요 | ✅ 정상 |
| `/smart/athnSend.json` | SMS 인증 전송 | POST | 필요 | ✅ 정상 |
| `/smart/athnSucc.json` | 인증 성공 확인 | POST | 필요 | ✅ 정상 |
| `/common/sndlamplog.json` | 로그 전송 | POST | 필요 | ✅ 정상 |

### 모바일 전용 API (5개)

| API | 용도 | 방식 |
|-----|------|------|
| `/m/oneMinuteReform/supportAmtChoiceList.json` | 모바일 요금제 목록 | POST |
| `/m/oneMinuteReform/supportAmtChoiceGroupList.json` | 모바일 그룹 목록 | POST |
| `/m/oneMinuteReform/defaultRecommPplList.json` | 모바일 추천 요금제 | POST |
| `/m/smart/athnSend.json` | 모바일 인증 전송 | POST |
| `/m/smart/athnSucc.json` | 모바일 인증 성공 | POST |

### 오류 반환 API (4개)

| API | 용도 | 오류 |
|-----|------|------|
| `/common/getOnmasProdInfo.json` | 온마스 상품 정보 | X0001 프로그램 오류 |
| `/common/mainPop.json` | 메인 팝업 | X0001 프로그램 오류 |
| `/common/menuCodePop.json` | 메뉴코드 팝업 | X0001 프로그램 오류 |
| `/smart/getSntyNo.json` | 시리얼 번호 조회 | X0001 프로그램 오류 |

---

## 새로 발견된 API 상세

### /wire/getPageRturnType.json

**유선 상품 주문 정보 조회** (세션 불필요!)

```bash
curl -s "https://shop.kt.com/wire/getPageRturnType.json" \
  -H "User-Agent: Mozilla/5.0"
```

**응답:**
```json
{
  "wireProductOrderBean": {
    "wireProdItgBean": null,
    "mainProdAdtnSvcCd": "",
    "subProd": "",
    "inetProdNo": "",
    "phnProdNo": "",
    "prodNo": null,
    "sntyNo": null,
    "prodCtgCd": null,
    "homeHub": false
  }
}
```

### /smart/athnSend.json

**SMS 인증 전송 API**

```bash
curl -s -b cookies.txt "https://shop.kt.com/smart/athnSend.json" \
  -X POST \
  -H "User-Agent: Mozilla/5.0" \
  -H "Referer: https://shop.kt.com/smart/supportAmtList.do"
```

**응답:**
```json
{
  "athnSmsDspBean": null,
  "encKey": "",
  "athnKeySeq": "4732108",
  "dspSucesYn": "N"
}
```

| 필드 | 설명 |
|------|------|
| `athnKeySeq` | 인증 키 시퀀스 |
| `dspSucesYn` | 전송 성공 여부 (Y/N) |
| `encKey` | 암호화 키 |

### /smart/athnSucc.json

**인증 성공 확인 API**

```bash
curl -s -b cookies.txt "https://shop.kt.com/smart/athnSucc.json" \
  -X POST \
  -H "User-Agent: Mozilla/5.0" \
  -H "Referer: https://shop.kt.com/smart/supportAmtList.do"
```

**응답:**
```json
{
  "isAthnSucc": "FAIL"
}
```

### /smart/punoChngNotiCmplt.json

**요금제 변경 알림 완료 API**

```bash
curl -s -b cookies.txt "https://shop.kt.com/smart/punoChngNotiCmplt.json" \
  -X POST \
  -H "User-Agent: Mozilla/5.0" \
  -H "Referer: https://shop.kt.com/smart/supportAmtList.do"
```

**응답:**
```json
{
  "notiPageDiv": null,
  "custSessionFlag": false
}
```

### /common/sndlamplog.json

**로그 전송 API** (세션 필요)

```bash
curl -s -b cookies.txt "https://shop.kt.com/common/sndlamplog.json" \
  -X POST \
  -H "User-Agent: Mozilla/5.0" \
  -H "Referer: https://shop.kt.com/"
```

**응답:** `{}`

---

## 발견된 페이지 엔드포인트 (.do)

### 공시지원금 관련

| 엔드포인트 | 용도 |
|------------|------|
| `/smart/supportAmtList.do` | 공시지원금 메인 (PC) |
| `/m/smart/supportAmtList.do` | 공시지원금 메인 (모바일) |
| `/oneMinuteReform/supportAmtList.do` | 원미닛 공시지원금 |
| `/m/oneMinuteReform/supportAmtList.do` | 원미닛 공시지원금 (모바일) |
| `/smart/supportAmtNotiPopup.do` | 공시지원금 알림 팝업 |
| `/smart/supportAmtNotiComplete.do` | 공시지원금 알림 완료 |

### 상품 관련

| 엔드포인트 | 용도 |
|------------|------|
| `/smart/productView.do` | 상품 상세 (PC) |
| `/m/smart/productView.do` | 상품 상세 (모바일) |
| `/smart/productTabletView.do` | 태블릿 상세 |
| `/smart/productUsimView.do` | 유심 상세 |
| `/smart/productHybridView.do` | 하이브리드 상세 |
| `/smart/productWibroView.do` | 와이브로 상세 |
| `/mobile/view.do` | 모바일 상품 보기 |

### 주문 관련

| 엔드포인트 | 용도 |
|------------|------|
| `/smart/wirelessOrderForm.do` | 무선 주문 폼 |
| `/m/smart/wirelessOrderForm.do` | 무선 주문 폼 (모바일) |
| `/m/smart/wirelessOrderForm_new.do` | 무선 주문 폼 (신규) |
| `/uniteOrder/orderCartView.do` | 장바구니 |
| `/m/uniteOrder/orderCartView.do` | 장바구니 (모바일) |
| `/uniteOrder/uniteOrderForm.do` | 통합 주문 폼 |
| `/mobile/orderView.do` | 모바일 주문 보기 |

### 액세서리 관련

| 엔드포인트 | 용도 |
|------------|------|
| `/accessory/accsProductView.do` | 액세서리 상세 |
| `/accessory/accsAuth.do` | 액세서리 인증 |
| `/accessory/accsOrderProductsList.do` | 액세서리 주문 목록 |
| `/accessory/accsOrderProductResultView.do` | 액세서리 주문 결과 |
| `/m/accessory/accsAuth.do` | 액세서리 인증 (모바일) |
| `/m/accessory/accsOrderProductsList.do` | 액세서리 주문 목록 (모바일) |
| `/m/accessory/accsOrderProductResultView.do` | 액세서리 주문 결과 (모바일) |

### 유선 관련

| 엔드포인트 | 용도 |
|------------|------|
| `/wire/wireProductView.do` | 유선 상품 상세 |
| `/internet/internetView.do` | 인터넷 보기 |
| `/tv/tvView.do` | TV 보기 |
| `/homePhone/homePhoneView.do` | 집전화 보기 |
| `/interPhone/interPhoneView.do` | 인터폰 보기 |

### 인증/로그인 관련

| 엔드포인트 | 용도 |
|------------|------|
| `/login/loginLayerPopView.do` | 로그인 레이어 팝업 |
| `/m/login/loginLayerPopView.do` | 로그인 레이어 팝업 (모바일) |
| `/wireless/userCert.do` | 무선 사용자 인증 |
| `/wamui/AthWebPopup.do` | 인증 웹 팝업 |
| `/wamui/ComSSOLogout.do` | SSO 로그아웃 |

### 기타

| 엔드포인트 | 용도 |
|------------|------|
| `/common/mainPopView.do` | 메인 팝업 보기 |
| `/m/common/mainPopView.do` | 메인 팝업 보기 (모바일) |
| `/common/menuCodePopView.do` | 메뉴코드 팝업 보기 |
| `/m/common/menuCodePopView.do` | 메뉴코드 팝업 보기 (모바일) |
| `/common/zipCodePopView.do` | 우편번호 팝업 |
| `/m/common/zipCodeView.do` | 우편번호 (모바일) |
| `/common/ulrRedirect.do` | URL 리다이렉트 |
| `/display/olhsPlan.do` | 올레샵 요금제 |
| `/deal/pick.do` | 딜 선택 |
| `/deal/collaboDeal5g.do` | 콜라보 딜 5G |
| `/deal/foodDeal5g.do` | 푸드 딜 5G |
| `/m/deal/pick.do` | 딜 선택 (모바일) |
| `/m/deal/collaboDeal5g.do` | 콜라보 딜 5G (모바일) |
| `/m/deal/foodDeal5g.do` | 푸드 딜 5G (모바일) |
| `/support/counselReceivePopView.do` | 상담 접수 팝업 |
| `/smart/usedPhoneOrderCont.do` | 중고폰 주문 |
| `/iot/gigaIotHomeCamView.do` | 기가 IoT 홈캠 |
| `/m/commonErrorView.do` | 공통 에러 (모바일) |

---

## KT API vs LGU+ API 비교 요약

| 항목 | KT | LGU+ |
|------|-----|------|
| **총 JSON API** | 14개 | 35개+ |
| **세션 요구** | 대부분 필요 | 대부분 불필요 |
| **페이지 사이즈** | 12개 고정 | 1~1000개 자유 |
| **검색 기능** | 미지원 | 키워드 검색 지원 |
| **정렬 옵션** | 1개만 작동 | 4개 모두 작동 |
| **기기 상세 API** | 없음 | 색상/스펙/추천요금제 API 있음 |
| **필터 API** | 없음 | `/filter-list` 전용 API |
| **구독 서비스 API** | 없음 | 유독(UDOK) 66개 상품 API |
| **결합 할인 API** | 없음 | `/cnvg-dcnt-amt` API |

**결론**: KT는 LGU+에 비해 JSON API가 제한적이며, 대부분 세션 쿠키가 필요합니다. 공시지원금 데이터 수집에 필요한 핵심 API는 모두 발견되었습니다.

---

## 테스트 날짜

2025-12-06 (2차 심층 분석)
