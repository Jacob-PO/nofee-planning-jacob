# LG U+ 공시지원금 API 문서

> 분석일: 2025-12-06
> 소스: https://www.lguplus.com/mobile/financing-model

## 개요

LG U+ 공식 웹사이트에서 사용하는 내부 API를 분석하여 정리한 문서입니다.
휴대폰 공시지원금, 요금제, 기기 정보를 조회할 수 있습니다.

---

## API 엔드포인트 요약

### 핵심 API
| 엔드포인트 | 용도 | 필수 파라미터 |
|-----------|------|--------------|
| `/uhdc/fo/prdv/mdlbsufu/v1/filter-list` | 필터 옵션 목록 | 없음 |
| `/uhdc/fo/prdv/mdlbsufu/v1/mdlb-pp-list` | 요금제 목록 + **선택약정 할인** | `hphnPpGrpKwrdCd` |
| `/uhdc/fo/prdv/mdlbsufu/v2/mdlb-sufu-list` | **공시지원금 목록** | `urcMblPpCd` |

### 기기/요금제 상세 API
| 엔드포인트 | 용도 | 필수 파라미터 |
|-----------|------|--------------|
| `/uhdc/fo/prdv/dvic/v1/{모델코드}/colrs` | 기기 색상 정보 | 모델코드 (path) |
| `/uhdc/fo/prdv/dvic/v1/{모델코드}/prod-info` | 기기 스펙 정보 | 모델코드 (path) |
| `/uhdc/fo/prdv/dvic/v1/{모델코드}/rcmd-pp` | 추천 요금제 목록 | 모델코드 (path) |
| `/uhdc/fo/prdv/dvic/v1/manf-dvic-list` | 제조사별 기기 목록 | 없음 |
| `/uhdc/fo/prdv/mblpp/entz/v1/mblppdtl` | 요금제 상세 정보 | `urcMblPpCd` |

### 부가 API
| 엔드포인트 | 용도 | 필수 파라미터 |
|-----------|------|--------------|
| `/uhdc/fo/prdv/hmcnvgpr/v1/cnvg-list` | 결합상품 목록 | 없음 |
| `/uhdc/fo/prdv/mblspps/v1/catg-list` | 부가서비스 카테고리 | 없음 |

### Base URL
```
https://www.lguplus.com
```

---

## 1. 필터 목록 조회 API

```
GET /uhdc/fo/prdv/mdlbsufu/v1/filter-list
```

**헤더:** 필수 헤더 없음 (User-Agent 권장)

**응답:**
```json
{
  "manfClss": [
    {"id": "02", "name": "삼성"},
    {"id": "03", "name": "애플"},
    {"id": "99", "name": "기타"}
  ],
  "urcHphnEntrPsblKdCd": [
    {"id": "1", "name": "기기변경"},
    {"id": "2", "name": "번호이동"},
    {"id": "3", "name": "신규가입"}
  ],
  "hphnPpKwrdClss": [
    {"id": "00", "name": "5G폰"},
    {"id": "01", "name": "LTE폰"},
    {"id": "03", "name": "태블릿/워치/노트북"}
  ],
  "sortOrd": [
    {"id": "00", "name": "지원금 총액 높은 순"},
    {"id": "01", "name": "최신 공시일자 순"},
    {"id": "02", "name": "판매가 높은 상품 순"},
    {"id": "03", "name": "판매가 낮은 상품 순"}
  ]
}
```

---

## 2. 요금제 목록 조회 API

```
GET /uhdc/fo/prdv/mdlbsufu/v1/mdlb-pp-list?hphnPpGrpKwrdCd={타입}
```

**파라미터:**
| 파라미터 | 필수 | 설명 | 값 |
|---------|:----:|------|-----|
| `hphnPpGrpKwrdCd` | ✅ | 기기 종류 | `00`=5G폰, `01`=LTE폰, `03`=태블릿/워치 |

### 기기 종류별 요금제 현황 (2025-12-06 기준)

| 기기 종류 | 코드 | 요금제 그룹 수 | 총 요금제 수 | 월요금 범위 |
|----------|------|--------------|-------------|------------|
| 5G폰 | 00 | 3개 | 74개 | 29,000원 ~ 130,000원 |
| LTE폰 | 01 | 9개 | 22개 | 13,200원 ~ 69,000원 |
| 태블릿/워치 | 03 | 4개 | 10개 | 8,800원 ~ 65,892원 |

### 5G 요금제 그룹

| 그룹명 | 그룹번호 | 요금제 수 | 월요금 범위 |
|--------|---------|----------|------------|
| 5G 요금제 | 1 | 36개 | 29,000원 ~ 95,000원 |
| 5G 프리미엄 팩 | 35 | 36개 | 105,000원 ~ 130,000원 |
| 5G 복지 요금제 | 36 | 2개 | 55,000원 ~ 75,000원 |

### LTE 요금제 그룹

| 그룹명 | 그룹번호 | 요금제 수 |
|--------|---------|----------|
| 속도 용량 걱정 없는 데이터 요금제 | 2 | 1개 |
| 추가 요금 걱정 없는 데이터 청소년 요금제 | 4 | 3개 |
| 현역병사 데이터 요금제 | 5 | 2개 |
| LTE 선택형 요금제 | 29 | 8개 |
| LTE 데이터 33 요금제 | 8 | 1개 |
| LTE 키즈/청소년 요금제 | 10 | 1개 |
| LTE 시니어 요금제 | 11 | 2개 |
| LTE 복지 요금제 | 31 | 2개 |
| 표준 요금제 | 19 | 2개 |

### 태블릿/워치 요금제 그룹

| 그룹명 | 그룹번호 | 요금제 수 |
|--------|---------|----------|
| 5G 태블릿 요금제 | 34 | 2개 |
| 태블릿/스마트기기 요금제 (나눠쓰기) | 26 | 2개 |
| 태블릿/스마트기기 요금제 | 27 | 3개 |
| 스마트워치 요금제 | 28 | 3개 |

### 응답 구조

```json
{
  "dvicMdlbSufuPpList": [
    {
      "urcTrmPpGrpKwrdCd": "00",
      "urcTrmPpGrpNo": "1",
      "trmPpGrpNm": "5G 요금제",
      "dvicMdlbSufuPpDetlList": [
        {
          "urcMblPpCd": "LPZ0000409",
          "urcMblPpNm": "5G 프리미어 에센셜",
          "urcPpBasfAmt": "85000",
          "lastBasfAmt": "63750",
          "mm24ChocAgmtDcntAmt": "21250",
          "mm24ChocAgmtDcntTamt": "510000",
          "mblMcnPpDataScrnEposDscr": "무제한",
          "nagmPpYn": false,
          "ppDirtDcntAplyPsblYn": false
        }
      ]
    }
  ],
  "repPp": {
    "urcMblPpCd": "LPZ0000409",
    "urcMblPpNm": "5G 프리미어 에센셜"
  }
}
```

### 요금제 필드 설명

| 필드 | 설명 |
|------|------|
| `urcMblPpCd` | 요금제 코드 (지원금 API에서 사용) |
| `urcMblPpNm` | 요금제명 |
| `urcPpBasfAmt` | 기본 월 요금 (원) |
| `lastBasfAmt` | 25% 선택약정 할인 후 요금 |
| `mm24ChocAgmtDcntAmt` | 24개월 선택약정 월 할인액 |
| `mm24ChocAgmtDcntTamt` | 24개월 선택약정 총 할인액 |
| `mblMcnPpDataScrnEposDscr` | 데이터 소진 시 안내 |
| `nagmPpYn` | 무약정 요금제 여부 |
| `ppDirtDcntAplyPsblYn` | 공시지원금 적용 가능 여부 |

### 주요 요금제 코드

#### 5G 요금제
| 요금제 코드 | 요금제명 | 월 요금 | 선택약정 할인 |
|------------|---------|--------|-------------|
| LPZ0000433 | 5G 프리미어 레귤러 | 95,000원 | 23,750원/월 |
| LPZ0000409 | 5G 프리미어 에센셜 | 85,000원 | 21,250원/월 |
| LPZ0000415 | 5G 스탠다드 | 75,000원 | 18,750원/월 |
| LPZ0000784 | 5G 스탠다드 에센셜 | 70,000원 | 17,500원/월 |
| LPZ0002860 | 5G 심플+ | 61,000원 | 15,250원/월 |
| LPZ0000437 | 5G 라이트+ | 55,000원 | 13,750원/월 |
| LPZ0000487 | 5G 슬림+ | 47,000원 | 11,750원/월 |
| LPZ1000325 | 5G 미니 | 37,000원 | 9,250원/월 |

#### LTE 요금제
| 요금제 코드 | 요금제명 | 월 요금 |
|------------|---------|--------|
| LPZ0000464 | 추가 요금 걱정 없는 데이터 69 | 69,000원 |
| LPZ0000471 | 현역병사 데이터 55 | 55,000원 |
| LPZ0000472 | LTE 데이터 33 | 33,000원 |

#### 태블릿/워치 요금제
| 요금제 코드 | 요금제명 | 월 요금 |
|------------|---------|--------|
| LPZ0000189 | 태블릿/스마트기기 데이터 걱정없는 25GB | 65,892원 |
| LPZ0001090 | 5G 태블릿 6GB+데이터 나눠쓰기 | 33,000원 |
| LPZ0000187 | 태블릿/스마트기기 데이터 10GB | 16,500원 |
| LPZ0001870 | LTE Wearable | 11,000원 |

---

## 3. 지원금 목록 조회 API (핵심)

```
GET /uhdc/fo/prdv/mdlbsufu/v2/mdlb-sufu-list
```

### 파라미터

| 파라미터 | 필수 | 설명 | 값 | 기본값 |
|---------|:----:|------|-----|--------|
| `urcMblPpCd` | ✅ | 요금제 코드 | 예: `LPZ0000409` | - |
| `urcHphnEntrPsblKdCd` | | 개통유형 | `1`=기기변경, `2`=번호이동, `3`=신규가입 | 자동 |
| `sortOrd` | | 정렬 기준 | `00`=지원금높은순, `01`=최신순, `02`=가격높은순, `03`=가격낮은순 | `01` |
| `pageNo` | | 페이지 번호 | 1, 2, 3... | `1` |
| `rowSize` | | 결과 수 | 1~1000+ | `10` |
| `dvicManfCds` | | 제조사 필터 | `02`=삼성, `03`=애플, `99`=기타 | - |
| `shwd` | | 검색어 | 예: "iPhone", "갤럭시", "S25" | - |
| `onlnOrdrPsblEposDivsCd` | | 온라인 주문 필터 | `Y`=온라인주문가능만 | - |

### 중요 발견사항

1. **필수 파라미터**: `urcMblPpCd`만 필수 (누락 시 500 에러)
2. **헤더**: 모든 헤더 선택사항 (User-Agent 없이도 동작)
3. **페이지네이션**: `rowSize=1000`도 동작하여 전체 데이터 한 번에 조회 가능
4. **가입유형**: `urcHphnEntrPsblKdCd` 누락 또는 잘못된 값이어도 기본값 적용됨

### 요청 예시

```bash
# 기본 조회 (모든 기기 한 번에)
curl -s 'https://www.lguplus.com/uhdc/fo/prdv/mdlbsufu/v2/mdlb-sufu-list?urcMblPpCd=LPZ0000409&rowSize=1000'

# 삼성 기기만 조회
curl -s 'https://www.lguplus.com/uhdc/fo/prdv/mdlbsufu/v2/mdlb-sufu-list?urcHphnEntrPsblKdCd=2&urcMblPpCd=LPZ0000409&dvicManfCds=02'

# 검색어로 조회
curl -s 'https://www.lguplus.com/uhdc/fo/prdv/mdlbsufu/v2/mdlb-sufu-list?urcHphnEntrPsblKdCd=2&urcMblPpCd=LPZ0000409&shwd=iPhone%2016'

# 지원금 높은 순 정렬
curl -s 'https://www.lguplus.com/uhdc/fo/prdv/mdlbsufu/v2/mdlb-sufu-list?urcHphnEntrPsblKdCd=2&urcMblPpCd=LPZ0000409&sortOrd=00'
```

### 응답 구조

```json
{
  "dvicMdlbSufuDtoList": [
    {
      "urcTrmMdlCd": "SM-S938N256",
      "urcTrmMdlNm": "갤럭시 S25 Ultra 256GB",
      "dlvrPrc": 1698400,
      "basicPlanPuanSuptAmt": 500000,
      "basicPlanAddSuptAmt": 0,
      "basicPlanCvrtSuptAmt": 0,
      "basicPlanAddCvrtSuptAmt": 0,
      "basicPlanSuptTamt": 575000,
      "basicPlanBuyPrc": 1123400,
      "sixPlanPuanSuptAmt": 500000,
      "sixPlanSuptTamt": 575000,
      "sixpPanBuyPrc": 1123400,
      "dsnwSupportAmt": 75000,
      "chagDsnwSupportAmt": 75000,
      "chagTotSupportAmt": 1623400,
      "rlCoutDttm": "2025-01-16",
      "dvicManfEngNm": "samsung",
      "urcTrmKndEngNm": "5g-phone",
      "onlnOrdrPsblYn": "Y",
      "pcUsgListImgeUrlAddr": "/pc-contents/images/prdv/...",
      "trmMdlEposEngNm": "galaxy-s25-ultra-256gb"
    }
  ],
  "totalCnt": 740,
  "ppLinkUrl": "/plan/mplan"
}
```

---

## 응답 필드 상세 설명

### 기기 정보
| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| `urcTrmMdlCd` | string | 모델 코드 | SM-S938N256 |
| `urcTrmMdlNm` | string | 모델명 (한글) | 갤럭시 S25 Ultra 256GB |
| `dvicManfEngNm` | string | 제조사 | samsung, apple, xiaomi, huawei, etc |
| `urcTrmKndEngNm` | string | 기기 종류 | 5g-phone, 4g-phone, tablet, watch, notebook |
| `trmMdlEposEngNm` | string | URL용 슬러그 | galaxy-s25-ultra-256gb |
| `pcUsgListImgeUrlAddr` | string | 제품 이미지 경로 | /pc-contents/images/prdv/... |
| `rlCoutDttm` | string | 공시일자 | 2025-01-16 |
| `onlnOrdrPsblYn` | string | 온라인 주문 가능 | Y/N |

### 가격 정보 (24개월 약정 - basicPlan)
| 필드 | 타입 | 설명 |
|------|------|------|
| `dlvrPrc` | number | 출고가 (원) |
| `basicPlanPuanSuptAmt` | number | **공시지원금** |
| `basicPlanAddSuptAmt` | number | 추가 공시지원금 |
| `basicPlanCvrtSuptAmt` | number | 전환지원금 |
| `basicPlanAddCvrtSuptAmt` | number | 추가 전환지원금 |
| `basicPlanSuptTamt` | number | **총 지원금** (공시+추가+대리점) |
| `basicPlanBuyPrc` | number | **실구매가** (출고가 - 총지원금) |

### 가격 정보 (6개월 약정 - sixPlan)
| 필드 | 타입 | 설명 |
|------|------|------|
| `sixPlanPuanSuptAmt` | number | 공시지원금 |
| `sixPlanAddSuptAmt` | number | 추가 공시지원금 |
| `sixPlanCvrtSuptAmt` | number | 전환지원금 |
| `sixPlanAddCvrtSuptAmt` | number | 추가 전환지원금 |
| `sixPlanSuptTamt` | number | 총 지원금 |
| `sixpPanBuyPrc` | number | 실구매가 |

### 대리점 지원금
| 필드 | 타입 | 설명 |
|------|------|------|
| `dsnwSupportAmt` | number | 대리점 추가지원금 |
| `chagDsnwSupportAmt` | number | 변경 대리점지원금 |
| `chagTotSupportAmt` | number | 변경 총 지원금 |

### 지원금 계산식

```
총 지원금 = 공시지원금 + 추가공시지원금 + 전환지원금 + 추가전환지원금 + 대리점지원금
실구매가 = 출고가 - 총 지원금
```

**예시 (갤럭시 S25 Ultra 256GB, 5G 프리미어 에센셜):**
- 출고가: 1,698,400원
- 공시지원금: 500,000원
- 대리점지원금: 75,000원
- 총 지원금: 575,000원 (= 500,000 + 75,000)
- 실구매가: 1,123,400원 (= 1,698,400 - 575,000)

---

## 테스트 결과

### 데이터 현황 (2025-12-06 기준, 5G 프리미어 에센셜 요금제)

**기기 총 수:** 740개

**제조사별:**
| 제조사 | 기기 수 |
|--------|--------|
| Apple | 288개 |
| Samsung | 278개 |
| 기타 (etc) | 103개 |
| Xiaomi | 15개 |
| Huawei | 2개 |
| Sharp | 1개 |
| 미지정 | 53개 |

**기기 종류별:**
| 종류 | 기기 수 |
|------|--------|
| 5G폰 (5g-phone) | 221개 |
| LTE폰 (4g-phone) | 203개 |
| 태블릿 (tablet) | 140개 |
| 2G/3G폰 (2g3g-phone) | 62개 |
| 워치 (watch) | 46개 |
| 노트북 (notebook) | 15개 |
| 미지정 | 53개 |

**온라인 주문:**
| 구분 | 기기 수 |
|------|--------|
| 온라인 주문 가능 | 132개 |
| 온라인 주문 불가 | 608개 |

**지원금 통계:**
- 최소: 10,000원
- 최대: 800,000원
- 평균: 310,520원

**출고가 통계:**
- 최소: 1,000원
- 최대: 3,872,000원
- 평균: 957,274원

### 요금제별 지원금 차이

동일 기기 (갤럭시 S25 Ultra 256GB) 기준:

| 요금제 | 월 요금 | 공시지원금 | 총 지원금 | 실구매가 |
|--------|--------|----------|----------|---------|
| 5G 프리미어 레귤러 | 95,000원 | 500,000원 | 575,000원 | 1,123,400원 |
| 5G 프리미어 에센셜 | 85,000원 | 500,000원 | 575,000원 | 1,123,400원 |
| 5G 스탠다드 | 75,000원 | 442,000원 | 508,300원 | 1,190,100원 |
| 5G 스탠다드 에센셜 | 70,000원 | 414,000원 | 476,100원 | 1,222,300원 |
| 5G 라이트+ | 55,000원 | 326,000원 | 374,900원 | 1,323,500원 |
| 5G 슬림+ | 47,000원 | 280,000원 | 322,000원 | 1,376,400원 |
| 5G 미니 | 37,000원 | 222,000원 | 255,300원 | 1,443,100원 |

> **결론:** 월 요금이 높은 요금제일수록 공시지원금이 높음

### 6개월 vs 24개월 약정

LG U+는 현재 6개월 약정과 24개월 약정의 공시지원금이 **동일**함

### 가입유형별 차이

기기변경, 번호이동, 신규가입 모두 **동일한 공시지원금** 적용

### 선택약정 할인

모든 요금제에서 **25% 고정 할인율** 적용
- 예: 85,000원 요금제 → 월 21,250원 할인 (63,750원 납부)
- 24개월 총 할인액: 510,000원

---

## 에러 처리

| 상황 | HTTP 상태 | 응답 |
|------|----------|------|
| 정상 조회 | 200 | `{"dvicMdlbSufuDtoList": [...], "totalCnt": N}` |
| 필수 파라미터 누락 (urcMblPpCd) | 200 | `{"httpStatus": "CONFLICT", "message": "시스템 이용에 불편을 드려서 죄송합니다."}` |
| 잘못된 요금제 코드 | 200 | `{"dvicMdlbSufuDtoList": [], "totalCnt": 0}` |
| 검색 결과 없음 | 200 | `{"dvicMdlbSufuDtoList": [], "totalCnt": 0}` |
| 마지막 페이지 초과 | 200 | `{"dvicMdlbSufuDtoList": [], "totalCnt": 740}` |

---

## 사용 예시

### Python 예시
```python
import requests

BASE_URL = "https://www.lguplus.com"
HEADERS = {"User-Agent": "Mozilla/5.0"}

def get_filter_list():
    """필터 옵션 조회"""
    url = f"{BASE_URL}/uhdc/fo/prdv/mdlbsufu/v1/filter-list"
    return requests.get(url, headers=HEADERS).json()

def get_plan_list(device_type="00"):
    """요금제 목록 조회

    Args:
        device_type: 00=5G폰, 01=LTE폰, 03=태블릿/워치
    """
    url = f"{BASE_URL}/uhdc/fo/prdv/mdlbsufu/v1/mdlb-pp-list"
    params = {"hphnPpGrpKwrdCd": device_type}
    return requests.get(url, params=params, headers=HEADERS).json()

def get_subsidy_list(
    plan_code="LPZ0000409",
    subscription_type=2,
    manufacturer=None,
    search=None,
    sort="01",
    page=1,
    size=100
):
    """
    LG U+ 공시지원금 목록 조회

    Args:
        plan_code: 요금제 코드 (필수)
        subscription_type: 1=기기변경, 2=번호이동, 3=신규가입
        manufacturer: 02=삼성, 03=애플, 99=기타
        search: 검색어
        sort: 00=지원금높은순, 01=최신순, 02=가격높은순, 03=가격낮은순
        page: 페이지 번호
        size: 결과 수 (최대 1000+)
    """
    url = f"{BASE_URL}/uhdc/fo/prdv/mdlbsufu/v2/mdlb-sufu-list"
    params = {
        "urcMblPpCd": plan_code,
        "urcHphnEntrPsblKdCd": subscription_type,
        "sortOrd": sort,
        "pageNo": page,
        "rowSize": size
    }

    if manufacturer:
        params["dvicManfCds"] = manufacturer
    if search:
        params["shwd"] = search

    return requests.get(url, params=params, headers=HEADERS).json()

def get_all_devices(plan_code="LPZ0000409"):
    """모든 기기 조회 (한 번에)"""
    data = get_subsidy_list(plan_code=plan_code, size=1000)
    return data.get("dvicMdlbSufuDtoList", [])

def get_all_plans():
    """모든 요금제 코드 추출"""
    all_plans = []

    for device_type in ["00", "01", "03"]:
        data = get_plan_list(device_type)
        for grp in data.get("dvicMdlbSufuPpList", []):
            for pp in grp.get("dvicMdlbSufuPpDetlList", []):
                all_plans.append({
                    "code": pp["urcMblPpCd"],
                    "name": pp["urcMblPpNm"],
                    "monthly": int(pp["urcPpBasfAmt"]),
                    "device_type": device_type
                })

    return all_plans

# 사용 예시
if __name__ == "__main__":
    # 모든 기기 조회
    devices = get_all_devices()
    print(f"총 {len(devices)}개 기기")

    # 삼성 기기만 조회
    samsung = get_subsidy_list(manufacturer="02")
    print(f"삼성 기기: {samsung.get('totalCnt', 0)}개")

    # iPhone 검색
    iphone = get_subsidy_list(search="iPhone 16")
    for device in iphone.get("dvicMdlbSufuDtoList", [])[:5]:
        print(f"  {device['urcTrmMdlNm']}: {device['basicPlanPuanSuptAmt']:,}원")

    # 모든 요금제 코드 조회
    plans = get_all_plans()
    print(f"총 {len(plans)}개 요금제")
```

### TypeScript 예시
```typescript
interface LGUDevice {
  urcTrmMdlCd: string;
  urcTrmMdlNm: string | null;
  dlvrPrc: number;
  basicPlanPuanSuptAmt: number;
  basicPlanAddSuptAmt: number;
  basicPlanCvrtSuptAmt: number;
  basicPlanAddCvrtSuptAmt: number;
  basicPlanSuptTamt: number;
  basicPlanBuyPrc: number;
  sixPlanPuanSuptAmt: number;
  sixPlanSuptTamt: number;
  sixpPanBuyPrc: number;
  dsnwSupportAmt: number;
  chagDsnwSupportAmt: number;
  chagTotSupportAmt: number;
  rlCoutDttm: string;
  dvicManfEngNm: string | null;
  urcTrmKndEngNm: string | null;
  onlnOrdrPsblYn: string;
  pcUsgListImgeUrlAddr: string;
  trmMdlEposEngNm: string | null;
  mdlbSufuGuidCntn: string | null;
}

interface LGUSubsidyResponse {
  dvicMdlbSufuDtoList: LGUDevice[];
  totalCnt: number;
  ppLinkUrl: string | null;
}

interface LGUPlan {
  urcMblPpCd: string;
  urcMblPpNm: string;
  urcPpBasfAmt: string;
  lastBasfAmt: string;
  mm24ChocAgmtDcntAmt: string;
  mm24ChocAgmtDcntTamt: string;
}

interface LGUPlanGroup {
  urcTrmPpGrpKwrdCd: string;
  urcTrmPpGrpNo: string;
  trmPpGrpNm: string;
  dvicMdlbSufuPpDetlList: LGUPlan[];
}

interface LGUPlanResponse {
  dvicMdlbSufuPpList: LGUPlanGroup[];
  repPp: LGUPlan;
}

const BASE_URL = 'https://www.lguplus.com';

async function getSubsidyList(params: {
  planCode: string;
  subscriptionType?: 1 | 2 | 3;
  manufacturer?: '02' | '03' | '99';
  search?: string;
  sort?: '00' | '01' | '02' | '03';
  size?: number;
}): Promise<LGUSubsidyResponse> {
  const queryParams = new URLSearchParams({
    urcMblPpCd: params.planCode,
    urcHphnEntrPsblKdCd: String(params.subscriptionType || 2),
    sortOrd: params.sort || '01',
    rowSize: String(params.size || 100)
  });

  if (params.manufacturer) {
    queryParams.set('dvicManfCds', params.manufacturer);
  }
  if (params.search) {
    queryParams.set('shwd', params.search);
  }

  const response = await fetch(
    `${BASE_URL}/uhdc/fo/prdv/mdlbsufu/v2/mdlb-sufu-list?${queryParams}`
  );

  return response.json();
}

async function getAllDevices(planCode = 'LPZ0000409'): Promise<LGUDevice[]> {
  const data = await getSubsidyList({ planCode, size: 1000 });
  return data.dvicMdlbSufuDtoList;
}

async function getPlanList(deviceType: '00' | '01' | '03'): Promise<LGUPlanResponse> {
  const response = await fetch(
    `${BASE_URL}/uhdc/fo/prdv/mdlbsufu/v1/mdlb-pp-list?hphnPpGrpKwrdCd=${deviceType}`
  );
  return response.json();
}
```

---

## 참고 사항

1. **인증 불필요**: API 키나 인증 토큰 없이 호출 가능
2. **헤더 선택사항**: User-Agent 없이도 동작하나, 권장
3. **요청 제한**: 과도한 요청 시 차단 가능 (1초 이상 딜레이 권장)
4. **데이터 갱신**: 공시지원금은 수시로 변경 (대부분 2025-07 공시)
5. **페이지네이션**: rowSize=1000으로 전체 데이터 한 번에 조회 가능
6. **이미지 URL**: `pcUsgListImgeUrlAddr` 앞에 `https://www.lguplus.com` 추가

---

## 기기 상세 페이지

기기 상세 정보는 별도의 API가 아닌 웹 페이지에서 확인:

```
https://www.lguplus.com/mobile/financing-model/financing-model-detail/{기기종류}/{슬러그}
```

예시:
- 5G폰: `/mobile/financing-model/financing-model-detail/5g-phone/galaxy-s25-ultra-256gb`
- 태블릿: `/mobile/financing-model/financing-model-detail/tablet/ipad-pro-13-m4-256gb`

슬러그는 `trmMdlEposEngNm` 필드 값 사용

---

## 4. 기기 상세 API

### 4.1 기기 색상 조회

```
GET /uhdc/fo/prdv/dvic/v1/{urcTrmMdlCd}/colrs
```

**응답 예시:**
```json
{
  "chocColrIdx": 0,
  "chocColrXstnYn": false,
  "colrList": [
    {
      "trmColrCd": "AN",
      "trmEposColrNm": "미스틱 브론즈",
      "trmRgbColrCd": "#A87D75",
      "isNotSoldOutYn": false,
      "dvicImgeResDtoList": [
        {
          "urcTrmImgeAllUrl": "/common/images/hphn/product/SM-F916N/imge_cut/ushop_SM-F916N_AN_A.jpg",
          "imgeAltrTxtCntn": "1번 이미지"
        }
      ],
      "urcTrmSaleStusCd": "2",
      "nuseIvntYn": false
    }
  ]
}
```

| 필드 | 설명 |
|------|------|
| `trmColrCd` | 색상 코드 (예: AN, AK) |
| `trmEposColrNm` | 색상명 (예: 미스틱 브론즈) |
| `trmRgbColrCd` | RGB 헥스 코드 |
| `isNotSoldOutYn` | 재고 여부 |
| `dvicImgeResDtoList` | 이미지 URL 목록 |
| `urcTrmSaleStusCd` | 판매 상태 |

### 4.2 기기 상품 정보 조회

```
GET /uhdc/fo/prdv/dvic/v1/{urcTrmMdlCd}/prod-info
```

**응답 예시:**
```json
{
  "urcTrmMdlCd": "SM-F916N",
  "mmryCapaCntn": "RAM 12GB(LPDDR5), ROM 256GB",
  "trmScrnSpecCntn": "메인 디스플레이 : 192.7 mm(7.6\") Dynamic AMOLED 2X",
  "btryCapaCntn": "4,500 mAh",
  "cpuSpecCntn": "Snapdragon 865+",
  "trmWgSpecNm": "282g",
  "trmSizSpecNm": "접었을 때 159.2 x 68.0 x 16.8mm",
  "trmWtprSpecNm": null,
  "camSpecCntn": "전면 10MP, 후면 12MP x 3 (초광각/광각/망원)",
  "colrList": "미스틱 브론즈, 미스틱 블랙",
  "manfPrifCntn": "<img alt=\"...\" src=\"/common/images/hphn/...\">..."
}
```

### 4.3 추천 요금제 조회

```
GET /uhdc/fo/prdv/dvic/v1/{urcTrmMdlCd}/rcmd-pp
```

**응답 예시:**
```json
{
  "rcmdPpResDtoList": [
    {
      "urcMblPpCd": "LPZ0002536",
      "urcMblMcnPpNm": "(넷플릭스) 5G 프리미어 플러스",
      "saleYn": true,
      "rcmdPpFlg": {"flgCd": "2", "flgNm": "추천"},
      "urcPpBasfAmt": 105000,
      "mblMcnPpPmtnAmt": 99750,
      "mblMcnPpDataScrnEposNm": "무제한",
      "mblPpVcePhclScrnEposNm": "집/이동전화 무제한",
      "dtshScrnEposNm": "100GB",
      "ppBnftDtoList": [...],
      "premiumBenefitList": [...],
      "mediaBenefitList": [...],
      "giftUseInfos": [
        {"giftOfferKindCd": "01", "giftOfferKindNm": "기본일반형", "useYn": "Y"},
        {"giftOfferKindCd": "03", "giftOfferKindNm": "선택일반형", "useYn": "Y"}
      ]
    }
  ]
}
```

---

## 5. 요금제 상세 API

### 5.1 요금제 상세 정보

```
GET /uhdc/fo/prdv/mblpp/entz/v1/mblppdtl?urcMblPpCd={요금제코드}
```

**파라미터:**
| 파라미터 | 필수 | 설명 |
|---------|:----:|------|
| `urcMblPpCd` | ✅ | 요금제 코드 (예: LPZ0000409) |

**응답 주요 필드:**
```json
{
  "urcMblMcnPpNm": "5G 프리미어 에센셜",
  "urcPpBasfAmt": 85000,
  "mblMcnPpDataScrnEposNm": "무제한",
  "mblPpVcePhclScrnEposNm": "집/이동전화 무제한",
  "dtshScrnEposNm": "70GB",
  "ppBnftDtoList": [...]
}
```

### 5.2 요금제 기본 정보

```
GET /uhdc/fo/prdv/mblpp/entz/v1/mblppinfo?urcMblPpCd={요금제코드}
```

---

## 6. 제조사별 기기 목록

```
GET /uhdc/fo/prdv/dvic/v1/manf-dvic-list
```

**응답:**
```json
[
  {
    "manfNm": "삼성",
    "urcTrmManfCd": "02",
    "dvicList": [
      {"urcTrmMdlCd": "SM-S938N256", "urcTrmMdlNm": "갤럭시 S25 Ultra 256GB"},
      {"urcTrmMdlCd": "SM-S938N512", "urcTrmMdlNm": "갤럭시 S25 Ultra 512GB"}
    ]
  },
  {
    "manfNm": "애플",
    "urcTrmManfCd": "03",
    "dvicList": [...]
  },
  {
    "manfNm": "기타",
    "urcTrmManfCd": "99",
    "dvicList": [...]
  }
]
```

**제조사별 기기 현황 (2025-12-06 기준):**
| 제조사 | 코드 | 기기 수 |
|--------|------|--------|
| 삼성 | 02 | 51개 |
| 애플 | 03 | 79개 |
| 기타 | 99 | 11개 |

---

## 7. 결합상품/부가서비스 API

### 7.1 홈 결합상품 목록

```
GET /uhdc/fo/prdv/hmcnvgpr/v1/cnvg-list
```

인터넷+IPTV 결합상품 정보 조회

### 7.2 부가서비스 카테고리

```
GET /uhdc/fo/prdv/mblspps/v1/catg-list
```

**응답:**
```json
{
  "sppsCatgDtoList": [
    {"mblUrcMenuId": "M10052", "mblUrcMenuNm": "데이터", "mblUrcMenuEngNm": "addon-data"},
    {"mblUrcMenuId": "M10054", "mblUrcMenuNm": "통화/문자메시지", "mblUrcMenuEngNm": "addon-call-msg"},
    {"mblUrcMenuId": "M21343", "mblUrcMenuNm": "휴대폰케어", "mblUrcMenuEngNm": "addon-phonecare"},
    {"mblUrcMenuId": "M21345", "mblUrcMenuNm": "디지털콘텐츠", "mblUrcMenuEngNm": "addon-digitalcontent"},
    {"mblUrcMenuId": "M21387", "mblUrcMenuNm": "통화연결음/벨소리", "mblUrcMenuEngNm": "addon-ringtones-callertunes"},
    {"mblUrcMenuId": "M21389", "mblUrcMenuNm": "가족보호/안심", "mblUrcMenuEngNm": "addon-familysafety-info"},
    {"mblUrcMenuId": "M21396", "mblUrcMenuNm": "PASS/정보", "mblUrcMenuEngNm": "addon-pass"},
    {"mblUrcMenuId": "M21398", "mblUrcMenuNm": "카테고리팩 혜택", "mblUrcMenuEngNm": "addon-standard-benefits"}
  ]
}
```

---

## 8. 선택약정 할인 상세

### 할인율
모든 요금제에서 **고정 25% 할인율** 적용

### 계산식
```
월 할인액 = 월정액 × 25%
24개월 총 할인액 = 월 할인액 × 24
선택약정 적용 후 월정액 = 월정액 × 75%
```

### 요금제별 선택약정 할인 (예시)

| 요금제 | 월정액 | 월 할인액 | 24개월 총 할인 | 적용 후 월정액 |
|--------|--------|----------|--------------|--------------|
| 5G 프리미어 플러스 | 105,000원 | 26,250원 | 630,000원 | 78,750원 |
| 5G 프리미어 에센셜 | 85,000원 | 21,250원 | 510,000원 | 63,750원 |
| 5G 스탠다드 | 75,000원 | 18,750원 | 450,000원 | 56,250원 |
| 5G 라이트+ | 55,000원 | 13,750원 | 330,000원 | 41,250원 |
| 5G 슬림+ | 47,000원 | 11,750원 | 282,000원 | 35,250원 |
| 5G 미니 | 37,000원 | 9,250원 | 222,000원 | 27,750원 |

### API에서 선택약정 정보 조회

`mdlb-pp-list` API 응답에서 선택약정 정보 확인:
```json
{
  "urcMblPpCd": "LPZ0000409",
  "urcMblPpNm": "5G 프리미어 에센셜",
  "urcPpBasfAmt": "85000",
  "lastBasfAmt": "63750",
  "mm24ChocAgmtDcntAmt": "21250",
  "mm24ChocAgmtDcntTamt": "510000"
}
```

| 필드 | 설명 |
|------|------|
| `urcPpBasfAmt` | 기본 월정액 |
| `lastBasfAmt` | 선택약정 적용 후 월정액 |
| `mm24ChocAgmtDcntAmt` | 24개월 선택약정 월 할인액 |
| `mm24ChocAgmtDcntTamt` | 24개월 선택약정 총 할인액 |

---

## 9. 공시지원금 vs 선택약정 비교

### 할인 방식 비교

| 구분 | 공시지원금 | 선택약정 |
|------|----------|---------|
| 적용 대상 | 기기 가격 | 월 요금 |
| 할인 시점 | 구매 시 즉시 할인 | 매월 할인 |
| 할인 금액 | 기기/요금제별 상이 | 월정액의 25% 고정 |
| API 필드 | `basicPlanPuanSuptAmt` | `mm24ChocAgmtDcntAmt` |

### 동일 기기 예시 (갤럭시 S25 Ultra 256GB, 5G 프리미어 에센셜)

**공시지원금 선택 시:**
- 기기 가격 할인: 575,000원 (공시 500,000 + 추가 75,000)
- 실구매가: 1,123,400원
- 월 요금: 85,000원

**선택약정 선택 시:**
- 기기 가격: 1,698,400원 (할인 없음)
- 월 요금: 63,750원 (21,250원 할인)
- 24개월 총 할인: 510,000원

---

## 10. 전체 API 엔드포인트 목록 (483개 발견)

JavaScript 번들(3.8MB) 분석으로 총 483개의 API 엔드포인트 발견

### 카테고리별 API 수
| 카테고리 | API 수 | 설명 |
|----------|--------|------|
| entp | 89개 | 기업/입점 관련 |
| pogg | 81개 | 포인트/적립 |
| mbrm | 71개 | 멤버십 |
| **prdv** | 55개 | **상품/기기/요금제** |
| acce | 42개 | 액세서리 |
| cusp | 41개 | 고객서비스 |
| mbec | 38개 | 모바일 이커머스 |
| myin | 31개 | 마이페이지 |
| shec | 14개 | 배송 |
| hiec | 13개 | 홈 인터넷 |
| 기타 | 8개 | fcmm, sycm, evet, apcm |

### 10.1 상품(prdv) API - 정상 동작

| 엔드포인트 | 설명 | 파라미터 |
|------------|------|----------|
| `/uhdc/fo/prdv/mdlbsufu/v1/filter-list` | 필터 옵션 목록 | 없음 |
| `/uhdc/fo/prdv/mdlbsufu/v1/mdlb-pp-list` | 요금제 목록 + 선택약정 | `hphnPpGrpKwrdCd` |
| `/uhdc/fo/prdv/mdlbsufu/v2/mdlb-sufu-list` | **공시지원금 목록** | `urcMblPpCd` |
| `/uhdc/fo/prdv/dvic/v1/{모델}/colrs` | 기기 색상 정보 | 모델코드 |
| `/uhdc/fo/prdv/dvic/v1/{모델}/prod-info` | 기기 스펙 정보 | 모델코드 |
| `/uhdc/fo/prdv/dvic/v1/{모델}/rcmd-pp` | 추천 요금제 | 모델코드 |
| `/uhdc/fo/prdv/dvic/v1/manf-dvic-list` | 제조사별 기기 목록 | 없음 |
| `/uhdc/fo/prdv/mblpp/entz/v1/mblppdtl` | 요금제 상세 | `urcMblPpCd` |
| `/uhdc/fo/prdv/mblpp/entz/v1/mblppinfo` | 요금제 기본 정보 | `urcMblPpCd` |
| `/uhdc/fo/prdv/hmcnvgpr/v1/cnvg-list` | 결합상품 목록 | 없음 |
| `/uhdc/fo/prdv/hmcnvgpr/v1/cnvg-dcnt-amt` | **결합 할인 금액** | 없음 |
| `/uhdc/fo/prdv/hmprppmg/v1/hm-prod-list` | 홈상품(TV) 목록 | 없음 |
| `/uhdc/fo/prdv/mblspps/v1/catg-list` | 부가서비스 카테고리 | 없음 |
| `/uhdc/fo/prdv/mblspps/v1/spps-exhi-fo-list` | 부가서비스 전시 목록 | 없음 |
| `/uhdc/fo/prdv/mblspps/v1/spps-info` | 부가서비스 상세 | `urcAdvpCd` |
| `/uhdc/fo/prdv/dvicexhi/v1/dif-chnl` | 다른 채널 배너 | 없음 |
| `/uhdc/fo/prdv/mblpp/entz/v1/entzpp-popup` | 요금제 팝업 | 없음 |

### 10.2 액세서리(acce) API - 정상 동작

| 엔드포인트 | 설명 | 파라미터 |
|------------|------|----------|
| `/uhdc/fo/acce/exhi/v1/manf-list` | 액세서리 제조사 목록 | 없음 |
| `/uhdc/fo/acce/exhi/v1/hphn-prod-grp-list` | **휴대폰 상품 그룹** | 없음 |
| `/uhdc/fo/acce/exhi/v1/prc-list` | 가격대 목록 | 없음 |
| `/uhdc/fo/acce/exhi/v1/sort-ord-list` | 정렬 옵션 목록 | 없음 |

### 10.3 고객서비스(cusp) API - 정상 동작

| 엔드포인트 | 설명 | 파라미터 |
|------------|------|----------|
| `/uhdc/fo/cusp/onqa/v1/faq-topten` | FAQ Top 10 | 없음 |
| `/uhdc/fo/cusp/cucm/v1/code-list` | 공통코드 목록 | 없음 |
| `/uhdc/fo/cusp/sfdg/v1/service-area-dong` | 서비스 지역 동 목록 | `sidoCd`, `siguCd` |

### 10.4 인증 필요 API (401)

| 엔드포인트 | 설명 |
|------------|------|
| `/uhdc/fo/prdv/mblpp/entz/v1/entr-netflix` | 넷플릭스 요금제 |
| `/uhdc/fo/prdv/mblpp/entz/v1/entr-disney` | 디즈니+ 요금제 |
| `/uhdc/fo/prdv/mblpp/entz/v1/entr-tving` | 티빙 요금제 |
| `/uhdc/fo/prdv/mblpp/entz/v1/entzpp-media-pack` | 미디어팩 |
| `/uhdc/fo/myin/*` | 마이페이지 관련 전체 |

### 10.5 세션/파라미터 필요 API (500)

| 엔드포인트 | 설명 |
|------------|------|
| `/uhdc/fo/prdv/dvic/v1/{모델}/detl` | 기기 상세 (POST) |
| `/uhdc/fo/prdv/dvic/v1/{모델}/gift` | 사은품 정보 |
| `/uhdc/fo/prdv/dvic/v1/{모델}/istt-term` | 할부 조건 |
| `/uhdc/fo/prdv/dvic/v1/{모델}/add-dcnt` | 추가 할인 |
| `/uhdc/fo/prdv/calcamt/v1/dcnt-kind-all:get` | 할인 종류 |
| `/uhdc/fo/prdv/dvic/v1/popu` | 인기 기기 |
| `/uhdc/fo/prdv/entrtrm/v1/trm-list-bnft` | 단말 혜택 |

### 10.6 새로 발견된 유용한 API

#### 휴대폰 상품 그룹 목록
```
GET /uhdc/fo/acce/exhi/v1/hphn-prod-grp-list
```
**응답:** iPhone17, 갤럭시 S25 Ultra, 갤럭시 Z Fold7 등 최신 기기 그룹 목록

#### 결합 할인 금액
```
GET /uhdc/fo/prdv/hmcnvgpr/v1/cnvg-dcnt-amt
```
**응답:**
```json
{
  "phoneDiscountAmount": 0,
  "familyDiscountAmount": 5500
}
```

#### 홈상품(TV) 목록
```
GET /uhdc/fo/prdv/hmprppmg/v1/hm-prod-list
```
**응답:** U+tv 요금제 목록 (실속형, 기본형 등)

#### 부가서비스 상세
```
GET /uhdc/fo/prdv/mblspps/v1/spps-info?urcAdvpCd={서비스코드}
```
**예시:** `urcAdvpCd=Z202301109` (육아 지원 데이터)

---

## 11. 추가 발견된 API (2차 분석)

### 11.1 포그(pogg) - 구독 서비스 API

#### 배너 목록
```
GET /uhdc/fo/pogg/main/v1/banner-list
```
**응답:** 구독 서비스 배너 목록 (프로모션 정보 포함)
```json
{
  "rspnCd": "biz.intf.100",
  "rspnMsg": "정상적으로 조회되었습니다.",
  "bnnrBasList": [
    {
      "bnnrCd": "P01",
      "bnnrLst": [
        {
          "bnnrNo": "396",
          "bnnrTit": "구글 AI 프로 출시 이벤트",
          "pcImgUrl": "/pc-contents/images/pogg/...",
          "pcBnnrLinkUrl": "/pogg/event/구글-ai-프로-출시-기념-이벤트"
        }
      ]
    }
  ]
}
```

#### 카테고리 목록
```
GET /uhdc/fo/pogg/main/v1/category-list
```
**응답:** 구독 서비스 카테고리 (OTT/뮤직, AI, 도서/아티클, 자기개발 등)
```json
{
  "categoryList": [
    {"catgId": "1016", "catgNm": "OTT/뮤직"},
    {"catgId": "1060", "catgNm": "AI"},
    {"catgId": "1022", "catgNm": "도서/아티클"},
    {"catgId": "1032", "catgNm": "자기개발"}
  ]
}
```

#### 전시 상품 목록
```
GET /uhdc/fo/pogg/main/v1/display-prod-list
```
**응답:** 66개 구독 상품 (라이너, 캔바, 넷플릭스 등)
```json
{
  "pageData": {"totalCnt": 66},
  "sppsFoDtoList": [
    {
      "dispProdNo": "1809",
      "mainDispProdNm": "라이너Pro + 추가AI(택1)",
      "normPrcInfo": 19800,
      "dispDcntPrcInfo": 9900,
      "logoImgUrl": "/pc-contents/images/pogg/..."
    }
  ]
}
```

#### 프로모션 상품 목록
```
GET /uhdc/fo/pogg/main/v1/pmtn-prod-list
```
**응답:** 할인 프로모션 중인 구독 상품

#### 메인 전시 목록
```
GET /uhdc/fo/pogg/main/v1/main-disp-list
```
**응답:** AI + AI 결합상품 등 메인 페이지 전시 상품

#### 추천 키워드
```
GET /uhdc/fo/pogg/srch/v1/srch-rcmd-kwrd-list
```
**응답:** 인기 검색어 (AI, 라이너, 캔바, OTT, 유튜브, 넷플릭스, 디즈니, 티빙)

#### 상품 검색
```
GET /uhdc/fo/pogg/srch/v1/srch-rslt-list?kwrd={검색어}
```
**응답:** 키워드 기반 구독 상품 검색 결과

---

### 11.2 SOHO(소호) 인터넷 API

#### SOHO 인터넷 요금제
```
GET /uhdc/fo/shec/sohopp/v1/soho-insvcbassnglpp-list
```
**응답:** 소호(소규모 사업장) 인터넷 요금 정보
```json
{
  "urcSohoPpNo": 1000003022,
  "ppNm": "(소호_WiFi기본)기가슬림안심_500M",
  "yy3AgmtChrg": 33000,
  "yy2AgmtChrg": 41580,
  "yy1AgmtChrg": 49500,
  "nagmChrg": 56100,
  "urcSnglProdPpNm": "인터넷 500M"
}
```

| 필드 | 설명 |
|------|------|
| `ppNm` | 요금제명 |
| `yy3AgmtChrg` | 3년 약정 월 요금 |
| `yy2AgmtChrg` | 2년 약정 월 요금 |
| `yy1AgmtChrg` | 1년 약정 월 요금 |
| `nagmChrg` | 무약정 월 요금 |

---

### 11.3 홈 융합상품 API

#### 융합상품 목록
```
GET /uhdc/fo/prdv/hmcnvgpr/v1/cnvg-list
```
**응답:** 인터넷+IPTV 결합상품 상세 정보
```json
{
  "hmCnvgProdList": [
    {
      "urcHmProdcKdCd": "INTV",
      "urcHmProdcCntsNm": "smart_disney",
      "urcHmProdcCntsDscr": "가장 많은 고객이 선택!",
      "itntUrcHmProdNo": 990001770,
      "iptvUrcHmProdNo": 990001735,
      "urcHmProdPpAgmtKndCd": "3",
      "chrgIdcBaseGuidCntn": "3년 약정 기준, 부가세 포함 금액",
      "urcHmProdcGiftCntn": "혜택 53만원..."
    }
  ]
}
```

---

### 11.4 액세서리 메뉴 API

#### 전체 메뉴 목록
```
GET /uhdc/fo/acce/exhi/v1/all-menu-list
```
**응답:** 액세서리 카테고리 메뉴 구조
```json
{
  "fomeList": [
    {"urcMenuId": "ACC001", "urcMenuNm": "액세서리", "depth": 3},
    {"urcMenuId": "ACC730", "urcMenuNm": "액세서리 홈", "depth": 4},
    {"urcMenuId": "M21338", "urcMenuNm": "디지털/가전 특별관", "depth": 4}
  ]
}
```

---

### 11.5 부가서비스 카테고리 API

```
GET /uhdc/fo/prdv/mblspps/v1/catg-list
```
**응답:** 모바일 부가서비스 카테고리 전체 목록
```json
{
  "sppsCatgDtoList": [
    {"mblUrcMenuId": "M10052", "mblUrcMenuNm": "데이터", "mblUrcMenuEngNm": "addon-data"},
    {"mblUrcMenuId": "M10054", "mblUrcMenuNm": "통화/문자메시지", "mblUrcMenuEngNm": "addon-call-msg"},
    {"mblUrcMenuId": "M21343", "mblUrcMenuNm": "휴대폰케어", "mblUrcMenuEngNm": "addon-phonecare"},
    {"mblUrcMenuId": "M21345", "mblUrcMenuNm": "디지털콘텐츠", "mblUrcMenuEngNm": "addon-digitalcontent"},
    {"mblUrcMenuId": "M21387", "mblUrcMenuNm": "통화연결음/벨소리", "mblUrcMenuEngNm": "addon-ringtones-callertunes"},
    {"mblUrcMenuId": "M21389", "mblUrcMenuNm": "가족보호/안심", "mblUrcMenuEngNm": "addon-familysafety-info"},
    {"mblUrcMenuId": "M21396", "mblUrcMenuNm": "PASS/정보", "mblUrcMenuEngNm": "addon-pass"},
    {"mblUrcMenuId": "M21398", "mblUrcMenuNm": "카테고리팩 혜택", "mblUrcMenuEngNm": "addon-standard-benefits"}
  ]
}
```

---

### 11.6 기업(entp) API

#### 기업 검색 키워드
```
GET /uhdc/fo/entp/shwd/v1/fo-entp-shwd-list
```
**응답:** 기업 서비스 추천 검색어
```json
[
  {"urcEntpShwdNm": "오피스넷", "entpShwdPcUrl": "/search/result?searchWord=오피스넷&category=entp"},
  {"urcEntpShwdNm": "U⁺커넥트", "entpShwdPcUrl": "/search/result?searchWord=커넥트&category=entp"},
  {"urcEntpShwdNm": "U⁺모바일인터넷", "entpShwdPcUrl": "/search/result?searchWord=모바일인터넷&category=entp"},
  {"urcEntpShwdNm": "AICC 클라우드", "entpShwdPcUrl": "/search/result?searchWord=AICC&category=entp"}
]
```

---

### 11.7 서버 시간 API

```
GET /uhdc/fo/mbrm/mbrcm/v1/time
```
**응답:** 서버 현재 시간과 인증 토큰
```json
{
  "timestamp": "20251206124130509",
  "authentications": "UKLjazsgQZc/WVUdpFkrn/55Eul806bNK6dx9CHtseg="
}
```

---

## 12. API 카테고리별 상세 분석

### 정상 동작 API 요약 (총 35개+)

| 카테고리 | API 수 | 주요 용도 |
|----------|--------|----------|
| prdv (상품) | 17개 | 공시지원금, 요금제, 기기 정보 |
| pogg (구독) | 8개 | 유독(UDOK) 구독 서비스 |
| acce (액세서리) | 5개 | 액세서리 상품, 메뉴 |
| cusp (고객) | 3개 | FAQ, 공통코드, 지역 |
| shec (SOHO) | 1개 | 소호 인터넷 요금 |
| mbrm (멤버십) | 1개 | 서버 시간 |
| entp (기업) | 1개 | 기업 검색어 |

### 인증 필요 API (entp/mbrm/myin 카테고리 대부분)

- 기업 상품 목록, 회원 정보, 마이페이지 기능
- HTTP 401 또는 세션 에러 반환

---

## 관련 페이지

- 공시지원금 조회: https://www.lguplus.com/mobile/financing-model
- 5G 요금제 안내: https://www.lguplus.com/plan/5g
- LTE 요금제 안내: https://www.lguplus.com/plan/lte
- 유독(구독) 서비스: https://www.lguplus.com/pogg
- 소호 인터넷: https://www.lguplus.com/home/soho
