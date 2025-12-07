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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SKT 공시지원금 조회 서비스
 *
 * SKT 공시지원금 페이지를 파싱하여 데이터 추출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SktSubsidyService {

    // 5G 프라임 89,000원
    private static final String DEFAULT_PLAN_CODE = "NA00007790";
    private static final String DEFAULT_JOIN_TYPE = "20"; // 번호이동

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // _this.products = parseObject([...]); 패턴 추출용
    private static final Pattern PARSE_OBJECT_PATTERN = Pattern.compile(
        "_this\\.products\\s*=\\s*parseObject\\(\\[([\\s\\S]+?)\\]\\);",
        Pattern.MULTILINE
    );

    /**
     * SKT 공시지원금 전체 목록 조회 (기본 파라미터)
     */
    public List<CarrierSubsidy> fetchAllSubsidies() {
        return fetchAllSubsidies(DEFAULT_PLAN_CODE, DEFAULT_JOIN_TYPE);
    }

    /**
     * SKT 공시지원금 전체 목록 조회 (파라미터 지정)
     * URL: /notice?modelNwType=5G&scrbTypCd={joinType}&prodId={planCode}&saleYn=Y
     *
     * @param planCode 요금제 코드 (예: NA00007790)
     * @param joinType 가입유형 (10: 신규가입, 20: 번호이동, 30: 기기변경)
     */
    public List<CarrierSubsidy> fetchAllSubsidies(String planCode, String joinType) {
        String effectivePlanCode = (planCode != null && !planCode.isEmpty()) ? planCode : DEFAULT_PLAN_CODE;
        String effectiveJoinType = (joinType != null && !joinType.isEmpty()) ? joinType : DEFAULT_JOIN_TYPE;

        log.info("📡 SKT 공시지원금 조회 중... (요금제: {}, 가입유형: {})", effectivePlanCode, effectiveJoinType);

        try {
            String html = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .scheme("https")
                    .host("shop.tworld.co.kr")
                    .path("/notice")
                    .queryParam("modelNwType", "5G")
                    .queryParam("scrbTypCd", effectiveJoinType)
                    .queryParam("prodId", effectivePlanCode)
                    .queryParam("saleYn", "Y")
                    .build())
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .header("Accept", "text/html")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (html == null || html.isEmpty()) {
                log.warn("⚠️ SKT 페이지 응답 없음");
                return new ArrayList<>();
            }

            List<CarrierSubsidy> subsidies = parseSubsidyData(html);
            log.info("✅ SKT 공시지원금 {}개 조회 완료", subsidies.size());
            return subsidies;

        } catch (Exception e) {
            log.error("❌ SKT 공시지원금 조회 오류: {}", e.getMessage());
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
     * HTML에서 parseObject 데이터 파싱
     */
    private List<CarrierSubsidy> parseSubsidyData(String html) {
        List<CarrierSubsidy> subsidies = new ArrayList<>();

        Matcher matcher = PARSE_OBJECT_PATTERN.matcher(html);
        while (matcher.find()) {
            String jsonArrayContent = matcher.group(1);
            try {
                // JSON 배열로 파싱
                String jsonArray = "[" + jsonArrayContent + "]";
                JsonNode arrayNode = objectMapper.readTree(jsonArray);

                for (JsonNode node : arrayNode) {
                    CarrierSubsidy subsidy = parseSubsidyNode(node);
                    if (subsidy != null) {
                        subsidies.add(subsidy);
                    }
                }
            } catch (Exception e) {
                log.debug("parseObject 파싱 실패: {}", e.getMessage());
            }
        }

        return subsidies;
    }

    /**
     * JSON 노드에서 CarrierSubsidy 변환
     * SKT API 필드명:
     * - productNm: 기기명
     * - productMem: 용량
     * - modelCd: 모델 코드
     * - companyNm: 제조사
     * - prodId: 요금제 ID
     * - prodNm: 요금제명
     * - factoryPrice: 출고가
     * - sumSaleAmt: 공시지원금
     * - dsnetSupmAmt: 추가지원금 (공시의 15%)
     * - scrbTypCd: 가입 유형
     */
    private CarrierSubsidy parseSubsidyNode(JsonNode node) {
        try {
            String productNm = getTextValue(node, "productNm");
            String modelCd = getTextValue(node, "modelCd");

            // 필수 필드 체크
            if (productNm == null || modelCd == null) {
                return null;
            }

            // 기기명 + 용량
            String productMem = getTextValue(node, "productMem");
            String fullDeviceName = productMem != null
                ? productNm + " " + productMem
                : productNm;

            Integer factoryPrice = getIntValue(node, "factoryPrice");
            Integer sumSaleAmt = getIntValue(node, "sumSaleAmt");
            Integer dsnetSupmAmt = getIntValue(node, "dsnetSupmAmt");

            // 총 할인 = 공시지원금 + 추가지원금
            int totalDiscount = (sumSaleAmt != null ? sumSaleAmt : 0)
                              + (dsnetSupmAmt != null ? dsnetSupmAmt : 0);

            // 실구매가 = 출고가 - 총할인
            Integer actualPrice = factoryPrice != null
                ? factoryPrice - totalDiscount
                : null;

            String joinTypeCode = getTextValue(node, "scrbTypCd");
            String joinTypeKorean = CarrierSubsidy.convertJoinTypeToKorean(joinTypeCode, "SKT");

            CarrierSubsidy subsidy = CarrierSubsidy.builder()
                .carrier("SKT")
                .deviceCode(modelCd)
                .deviceName(fullDeviceName)
                .manufacturer(getTextValue(node, "companyNm"))
                .storage(productMem)
                .planCode(getTextValue(node, "prodId"))
                .planName(getTextValue(node, "prodNm"))
                .planMonthlyFee(89000) // 5GX 프라임 고정
                .planMaintainMonth(6)
                .msrp(factoryPrice)
                .carrierSubsidy(sumSaleAmt)
                .additionalSubsidy(dsnetSupmAmt)
                .installmentPrice(actualPrice)
                .joinType(joinTypeKorean)
                .discountType("공시지원")
                .rawData(node.toString())
                .build();
            subsidy.setId(subsidy.generateId());
            return subsidy;
        } catch (Exception e) {
            log.debug("노드 파싱 실패: {}", e.getMessage());
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
}
