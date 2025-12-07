package com.nofee.api.test.carrierintegration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nofee.api.test.carrierintegration.dto.CarrierSubsidy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.ArrayList;
import java.util.List;

/**
 * KT 공시지원금 조회 서비스
 *
 * KT API: /mobile/retvSuFuList.json
 * 세션 쿠키 필수! /smart/supportAmtList.do?channel=VS 접속 후 쿠키 획득
 * POST 요청, 12 items/page 고정
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KtSubsidyService {

    private static final String KT_BASE_URL = "https://shop.kt.com";
    private static final String KT_SUBSIDY_URL = KT_BASE_URL + "/mobile/retvSuFuList.json";
    private static final String KT_SESSION_URL = KT_BASE_URL + "/smart/supportAmtList.do?channel=VS";

    // 티빙/지니/밀리 초이스 베이직 90,000원
    private static final String DEFAULT_PLAN_CODE = "PL244N945";
    private static final String DEFAULT_JOIN_TYPE = "04"; // 기기변경
    private static final String DEFAULT_DISCOUNT_OPTION = "HT"; // 기변-심플

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // 페이지 결과 (데이터 + 페이지네이션 정보)
    private record PageResult(List<CarrierSubsidy> data, int totalPages, int totalCount) {}

    /**
     * KT 공시지원금 전체 목록 조회 (기본 파라미터)
     */
    public List<CarrierSubsidy> fetchAllSubsidies() {
        return fetchAllSubsidies(DEFAULT_PLAN_CODE, DEFAULT_JOIN_TYPE);
    }

    /**
     * KT 공시지원금 전체 목록 조회 (파라미터 지정)
     *
     * @param planCode 요금제 코드 (예: PL244N945)
     * @param joinType 가입유형 (01: 신규가입, 02: 번호이동, 04: 기기변경)
     */
    public List<CarrierSubsidy> fetchAllSubsidies(String planCode, String joinType) {
        String effectivePlanCode = (planCode != null && !planCode.isEmpty()) ? planCode : DEFAULT_PLAN_CODE;
        String effectiveJoinType = (joinType != null && !joinType.isEmpty()) ? joinType : DEFAULT_JOIN_TYPE;

        log.info("📡 KT 공시지원금 조회 중... (요금제: {}, 가입유형: {})", effectivePlanCode, effectiveJoinType);

        try {
            // 세션 쿠키 획득
            String sessionCookie = getSessionCookie();
            if (sessionCookie == null) {
                log.warn("⚠️ KT 세션 쿠키 획득 실패");
                return new ArrayList<>();
            }

            List<CarrierSubsidy> allSubsidies = new ArrayList<>();
            int page = 1;
            int totalPages = 1;

            do {
                PageResult pageResult = fetchPageWithInfo(sessionCookie, page, effectivePlanCode, effectiveJoinType);
                if (pageResult.data.isEmpty()) {
                    break;
                }
                allSubsidies.addAll(pageResult.data);

                // 첫 페이지에서 총 페이지 수 계산
                if (page == 1) {
                    totalPages = pageResult.totalPages;
                    log.info("📄 KT 총 {}페이지 조회 예정 (총 {}건)", totalPages, pageResult.totalCount);
                }

                page++;
            } while (page <= totalPages && page <= 100);

            log.info("✅ KT 공시지원금 {}개 조회 완료", allSubsidies.size());
            return allSubsidies;

        } catch (Exception e) {
            log.error("❌ KT 공시지원금 조회 오류: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 특정 기기의 공시지원금 조회
     */
    public List<CarrierSubsidy> fetchSubsidiesByDevice(String deviceCode) {
        List<CarrierSubsidy> all = fetchAllSubsidies();
        return all.stream()
            .filter(s -> deviceCode.equals(s.getDeviceCode()))
            .toList();
    }

    /**
     * 세션 쿠키 획득
     * /smart/supportAmtList.do?channel=VS 접속해서 쿠키 획득
     */
    private String getSessionCookie() {
        try {
            return webClient.get()
                .uri(KT_SESSION_URL)
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .exchangeToMono(response -> {
                    List<String> cookies = response.headers().header("Set-Cookie");
                    StringBuilder cookieString = new StringBuilder();
                    for (String cookie : cookies) {
                        if (cookieString.length() > 0) {
                            cookieString.append("; ");
                        }
                        // 쿠키 값만 추출 (Path, Domain 등 제외)
                        cookieString.append(cookie.split(";")[0]);
                    }
                    log.debug("KT 세션 쿠키 획득: {}", cookieString);
                    return response.bodyToMono(String.class)
                        .thenReturn(cookieString.toString());
                })
                .block();
        } catch (Exception e) {
            log.error("세션 쿠키 획득 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 특정 페이지 데이터 조회 (페이지네이션 정보 포함)
     * POST /mobile/retvSuFuList.json
     * pageInfoBean 구조: { totalCount, pageNo, pageSize, totalPages, ... }
     */
    private PageResult fetchPageWithInfo(String sessionCookie, int page, String planCode, String joinType) {
        try {
            String response = webClient.post()
                .uri(KT_SUBSIDY_URL)
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .header("Cookie", sessionCookie)
                .header("Referer", KT_SESSION_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("prodNm", "mobile")
                    .with("prdcCd", planCode)
                    .with("prodType", "30")  // 5G
                    .with("deviceType", "HDP")
                    .with("makrCd", "")  // 전체 제조사
                    .with("sortProd", "oBspnsrPunoDateDesc")
                    .with("spnsMonsType", "2")  // 24개월
                    .with("dscnOptnCd", DEFAULT_DISCOUNT_OPTION)
                    .with("sbscTypeCd", joinType)
                    .with("pageNo", String.valueOf(page)))
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (response == null || response.isEmpty()) {
                return new PageResult(new ArrayList<>(), 0, 0);
            }

            return parseKtResponseWithPageInfo(response);

        } catch (Exception e) {
            log.error("KT 페이지 {} 조회 실패: {}", page, e.getMessage());
            return new PageResult(new ArrayList<>(), 0, 0);
        }
    }

    /**
     * KT JSON 응답 파싱 (페이지네이션 정보 포함)
     */
    private PageResult parseKtResponseWithPageInfo(String jsonResponse) {
        List<CarrierSubsidy> subsidies = new ArrayList<>();
        int totalPages = 0;
        int totalCount = 0;

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            // 페이지네이션 정보 추출
            JsonNode pageInfo = root.path("pageInfoBean");
            if (!pageInfo.isMissingNode()) {
                totalCount = pageInfo.path("totalCount").asInt(0);
                int pageSize = pageInfo.path("pageSize").asInt(12);
                totalPages = (totalCount + pageSize - 1) / pageSize; // 올림 계산
            }

            // 데이터 추출
            JsonNode dataList = root.path("LIST_DATA");
            if (dataList.isArray()) {
                for (JsonNode node : dataList) {
                    CarrierSubsidy subsidy = parseKtNode(node);
                    if (subsidy != null) {
                        subsidies.add(subsidy);
                    }
                }
            }
        } catch (Exception e) {
            log.error("KT 응답 파싱 실패: {}", e.getMessage());
        }

        return new PageResult(subsidies, totalPages, totalCount);
    }

    /**
     * KT JSON 노드에서 CarrierSubsidy 변환
     * KT API 필드명:
     * - petNm: 기기명
     * - hndsetModelNm: 모델 코드
     * - ofwAmt: 출고가
     * - ktSuprtAmt: KT 공시지원금
     * - realAmt: 실결제가
     * - monthUseChageDcAmt: 선택약정 24개월 총 할인액
     * - pplId: 요금제 ID
     * - pplNm: 요금제명
     * - makrCd: 제조사 코드
     */
    private CarrierSubsidy parseKtNode(JsonNode node) {
        try {
            String petNm = getTextValue(node, "petNm");
            String hndsetModelNm = getTextValue(node, "hndsetModelNm");

            if (petNm == null || hndsetModelNm == null) {
                return null;
            }

            Integer ofwAmt = getIntValue(node, "ofwAmt");
            Integer ktSuprtAmt = getIntValue(node, "ktSuprtAmt");
            Integer realAmt = getIntValue(node, "realAmt");

            // 저장용량 추출 (기기명에서)
            String storage = extractStorage(petNm);

            String joinTypeKorean = CarrierSubsidy.convertJoinTypeToKorean(DEFAULT_JOIN_TYPE, "KT");

            CarrierSubsidy subsidy = CarrierSubsidy.builder()
                .carrier("KT")
                .deviceCode(hndsetModelNm)
                .deviceName(petNm)
                .manufacturer(getMakrName(getTextValue(node, "makrCd")))
                .storage(storage)
                .planCode(getTextValue(node, "pplId"))
                .planName(getTextValue(node, "pplNm"))
                .planMonthlyFee(90000) // 티빙/지니/밀리 초이스 베이직 고정
                .planMaintainMonth(6)
                .msrp(ofwAmt)
                .carrierSubsidy(ktSuprtAmt)
                .additionalSubsidy(0) // KT는 추가지원금 없음
                .installmentPrice(realAmt)
                .joinType(joinTypeKorean)
                .discountType("공시지원")
                .rawData(node.toString())
                .build();
            subsidy.setId(subsidy.generateId());
            return subsidy;
        } catch (Exception e) {
            log.debug("KT 노드 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 제조사 코드 -> 제조사명 변환
     */
    private String getMakrName(String makrCd) {
        if (makrCd == null) return null;
        return switch (makrCd) {
            case "13" -> "삼성";
            case "15" -> "Apple";
            case "02" -> "샤오미";
            case "19" -> "모토로라";
            default -> "기타";
        };
    }

    /**
     * 기기명에서 저장용량 추출
     */
    private String extractStorage(String deviceName) {
        if (deviceName == null) return null;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("(\\d+)\\s*(GB|TB)", java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(deviceName);
        return matcher.find() ? matcher.group(1) + matcher.group(2).toUpperCase() : null;
    }

    private String getTextValue(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asText() : null;
    }

    private Integer getIntValue(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode != null && !fieldNode.isNull()) {
            try {
                String value = fieldNode.asText().replaceAll("[^0-9]", "");
                return value.isEmpty() ? null : Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
