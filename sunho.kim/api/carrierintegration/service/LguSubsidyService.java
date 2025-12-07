package com.nofee.api.test.carrierintegration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nofee.api.test.carrierintegration.dto.CarrierSubsidy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

/**
 * LGU+ 공시지원금 조회 서비스
 *
 * LGU+ API: /uhdc/fo/prdv/mdlbsufu/v2/mdlb-sufu-list
 * 인증 불필요, rowSize 최대 1000
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LguSubsidyService {

    private static final String LGU_BASE_URL = "https://www.lguplus.com";
    private static final String LGU_SUBSIDY_URL = LGU_BASE_URL + "/uhdc/fo/prdv/mdlbsufu/v2/mdlb-sufu-list";
    private static final int ROW_SIZE = 1000;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // 5G 프리미어 에센셜 85,000원
    private static final String DEFAULT_PLAN_CODE = "LPZ0000409";
    private static final String DEFAULT_JOIN_TYPE = "2"; // 번호이동

    /**
     * LGU+ 공시지원금 전체 목록 조회 (기본 파라미터)
     */
    public List<CarrierSubsidy> fetchAllSubsidies() {
        return fetchAllSubsidies(DEFAULT_PLAN_CODE, DEFAULT_JOIN_TYPE);
    }

    /**
     * LGU+ 공시지원금 전체 목록 조회 (파라미터 지정)
     *
     * @param planCode 요금제 코드 (예: LPZ0000409)
     * @param joinType 가입유형 (1: 신규가입, 2: 번호이동, 3: 기기변경)
     */
    public List<CarrierSubsidy> fetchAllSubsidies(String planCode, String joinType) {
        String effectivePlanCode = (planCode != null && !planCode.isEmpty()) ? planCode : DEFAULT_PLAN_CODE;
        String effectiveJoinType = (joinType != null && !joinType.isEmpty()) ? joinType : DEFAULT_JOIN_TYPE;

        log.info("📡 LGU+ 공시지원금 조회 중... (요금제: {}, 가입유형: {})", effectivePlanCode, effectiveJoinType);

        try {
            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .scheme("https")
                    .host("www.lguplus.com")
                    .path("/uhdc/fo/prdv/mdlbsufu/v2/mdlb-sufu-list")
                    .queryParam("urcMblPpCd", effectivePlanCode)
                    .queryParam("urcHphnEntrPsblKdCd", effectiveJoinType)
                    .queryParam("rowSize", String.valueOf(ROW_SIZE))
                    .queryParam("sortOrd", "00")
                    .build())
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (response == null || response.isEmpty()) {
                log.warn("⚠️ LGU+ 응답 없음");
                return new ArrayList<>();
            }

            List<CarrierSubsidy> subsidies = parseLguResponse(response);
            log.info("✅ LGU+ 공시지원금 {}개 조회 완료", subsidies.size());
            return subsidies;

        } catch (Exception e) {
            log.error("❌ LGU+ 공시지원금 조회 오류: {}", e.getMessage());
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
     * LGU+ JSON 응답 파싱
     */
    private List<CarrierSubsidy> parseLguResponse(String jsonResponse) {
        List<CarrierSubsidy> subsidies = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            // LGU+ 응답 구조: { dvicMdlbSufuDtoList: [...] }
            JsonNode dataList = root.path("dvicMdlbSufuDtoList");
            if (dataList.isMissingNode()) {
                dataList = root.path("data").path("dvicMdlbSufuDtoList");
            }
            if (dataList.isMissingNode()) {
                dataList = root.path("list");
            }

            if (dataList.isArray()) {
                for (JsonNode node : dataList) {
                    CarrierSubsidy subsidy = parseLguNode(node);
                    if (subsidy != null) {
                        subsidies.add(subsidy);
                    }
                }
            }
        } catch (Exception e) {
            log.error("LGU+ 응답 파싱 실패: {}", e.getMessage());
        }

        return subsidies;
    }

    /**
     * LGU+ JSON 노드에서 CarrierSubsidy 변환
     * LGU+ API 필드명:
     * - urcTrmMdlCd: 기기코드
     * - urcTrmMdlNm: 기기명
     * - dvicManfEngNm: 제조사 (영문)
     * - dlvrPrc: 출고가
     * - basicPlanPuanSuptAmt: 공시지원금
     * - basicPlanAddSuptAmt: 추가지원금
     * - basicPlanSuptTamt: 총지원금
     * - basicPlanBuyPrc: 실구매가
     */
    private CarrierSubsidy parseLguNode(JsonNode node) {
        try {
            String mdlCd = getTextValue(node, "urcTrmMdlCd");
            String mdlNm = getTextValue(node, "urcTrmMdlNm");

            if (mdlCd == null || mdlNm == null) {
                return null;
            }

            String joinTypeKorean = CarrierSubsidy.convertJoinTypeToKorean(DEFAULT_JOIN_TYPE, "LGU");

            CarrierSubsidy subsidy = CarrierSubsidy.builder()
                .carrier("LGU")
                .deviceCode(mdlCd)
                .deviceName(mdlNm)
                .manufacturer(getTextValue(node, "dvicManfEngNm"))
                .storage(extractStorage(mdlNm)) // 기기명에서 추출
                .planCode(DEFAULT_PLAN_CODE)
                .planName("5G 프리미어 에센셜")
                .planMonthlyFee(85000)
                .planMaintainMonth(6)
                .msrp(getIntValue(node, "dlvrPrc"))
                .carrierSubsidy(getIntValue(node, "basicPlanPuanSuptAmt"))
                .additionalSubsidy(getIntValue(node, "basicPlanAddSuptAmt"))
                .installmentPrice(getIntValue(node, "basicPlanBuyPrc"))
                .joinType(joinTypeKorean)
                .discountType("공시지원")
                .rawData(node.toString())
                .build();
            subsidy.setId(subsidy.generateId());
            return subsidy;
        } catch (Exception e) {
            log.debug("LGU+ 노드 파싱 실패: {}", e.getMessage());
            return null;
        }
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
}
