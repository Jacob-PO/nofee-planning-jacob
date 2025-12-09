package com.nofee.api.test.carrierintegration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nofee.api.test.carrierintegration.dto.CarrierSubsidy;
import com.nofee.api.test.carrierintegration.dto.SktRatePlan;
import com.nofee.api.test.carrierintegration.util.JsonNodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    // SKT 요금제 목록 API URL
    private static final String SKT_PLAN_LIST_URL = "https://shop.tworld.co.kr/api/wireless/subscription/list";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // 요금제 캐시 (5분 TTL)
    private final Map<String, List<SktRatePlan>> planCache = new ConcurrentHashMap<>();
    private volatile long planCacheTime = 0;
    private static final long PLAN_CACHE_TTL = 5 * 60 * 1000; // 5분

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
        return fetchAllSubsidies(planCode, joinType, "5G");
    }

    /**
     * SKT 공시지원금 전체 목록 조회 (네트워크 유형 지정)
     *
     * @param planCode 요금제 코드 (예: NA00007790)
     * @param joinType 가입유형 (10: 신규가입, 20: 번호이동, 30: 기기변경)
     * @param networkType 네트워크 유형 (5G 또는 LTE)
     */
    public List<CarrierSubsidy> fetchAllSubsidies(String planCode, String joinType, String networkType) {
        String effectivePlanCode = (planCode != null && !planCode.isEmpty()) ? planCode : DEFAULT_PLAN_CODE;
        String effectiveJoinType = (joinType != null && !joinType.isEmpty()) ? joinType : DEFAULT_JOIN_TYPE;
        String effectiveNetworkType = (networkType != null && !networkType.isEmpty()) ? networkType : "5G";

        log.info("📡 SKT 공시지원금 조회 중... (요금제: {}, 가입유형: {}, 네트워크: {})", effectivePlanCode, effectiveJoinType, effectiveNetworkType);

        try {
            String html = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .scheme("https")
                    .host("shop.tworld.co.kr")
                    .path("/notice")
                    .queryParam("modelNwType", effectiveNetworkType)
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
     * - effStaDt: 공시일 (YYYYMMDD 형식)
     */
    private CarrierSubsidy parseSubsidyNode(JsonNode node) {
        try {
            String productNm = JsonNodeUtils.getTextValue(node, "productNm");
            String modelCd = JsonNodeUtils.getTextValue(node, "modelCd");

            // 필수 필드 체크
            if (productNm == null || modelCd == null) {
                return null;
            }

            // 기기명 + 용량
            String productMem = JsonNodeUtils.getTextValue(node, "productMem");
            String fullDeviceName = productMem != null
                ? productNm + " " + productMem
                : productNm;

            Integer factoryPrice = JsonNodeUtils.getIntValue(node, "factoryPrice");
            Integer sumSaleAmt = JsonNodeUtils.getIntValue(node, "sumSaleAmt");
            Integer dsnetSupmAmt = JsonNodeUtils.getIntValue(node, "dsnetSupmAmt");

            // 총 할인 = 공시지원금 + 추가지원금
            int totalDiscount = (sumSaleAmt != null ? sumSaleAmt : 0)
                              + (dsnetSupmAmt != null ? dsnetSupmAmt : 0);

            // 실구매가 = 출고가 - 총할인
            Integer actualPrice = factoryPrice != null
                ? factoryPrice - totalDiscount
                : null;

            String joinTypeCode = JsonNodeUtils.getTextValue(node, "scrbTypCd");
            String joinTypeKorean = CarrierSubsidy.convertJoinTypeToKorean(joinTypeCode, "SKT");

            // 공시일 파싱 (YYYYMMDD -> YYYY-MM-DD)
            String effStaDt = JsonNodeUtils.getTextValue(node, "effStaDt");
            String announceDate = formatSktDate(effStaDt);

            // API 응답에서 요금제 월정액 추출 (basicFee 필드)
            Integer basicFee = JsonNodeUtils.getIntValue(node, "basicFee");

            CarrierSubsidy subsidy = CarrierSubsidy.builder()
                .carrier("SKT")
                .deviceCode(modelCd)
                .deviceName(fullDeviceName)
                .manufacturer(JsonNodeUtils.getTextValue(node, "companyNm"))
                .storage(productMem)
                .planCode(JsonNodeUtils.getTextValue(node, "prodId"))
                .planName(JsonNodeUtils.getTextValue(node, "prodNm"))
                .planMonthlyFee(basicFee) // API 응답에서 가져옴 (없으면 null)
                .planMaintainMonth(6)
                .msrp(factoryPrice)
                .carrierSubsidy(sumSaleAmt)
                .additionalSubsidy(dsnetSupmAmt)
                .installmentPrice(actualPrice)
                .joinType(joinTypeKorean)
                .discountType("공시지원")
                .supportType("공시지원금")
                .announceDate(announceDate)
                .rawData(node.toString())
                .build();
            subsidy.setId(subsidy.generateId());
            return subsidy;
        } catch (Exception e) {
            log.debug("노드 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * SKT 날짜 형식 변환 (YYYYMMDD -> YYYY-MM-DD)
     */
    private String formatSktDate(String dateStr) {
        if (dateStr == null || dateStr.length() < 8) return null;
        try {
            return dateStr.substring(0, 4) + "-" + dateStr.substring(4, 6) + "-" + dateStr.substring(6, 8);
        } catch (Exception e) {
            return null;
        }
    }


    // ==================== 요금제 목록 API ====================

    /**
     * SKT 요금제 목록 조회
     *
     * API: /api/wireless/subscription/list
     * - 전체 1,231개 요금제 중 subcategoryId='H'인 740개가 공시지원금 대상
     *
     * @param networkType 네트워크 유형 (5G 또는 LTE)
     * @return 요금제 목록
     */
    public List<SktRatePlan> fetchRatePlans(String networkType) {
        String cacheKey = networkType != null ? networkType : "ALL";

        // 캐시 체크
        if (System.currentTimeMillis() - planCacheTime < PLAN_CACHE_TTL && planCache.containsKey(cacheKey)) {
            log.debug("📦 SKT 요금제 캐시 히트: {}", cacheKey);
            return planCache.get(cacheKey);
        }

        log.info("📡 SKT 요금제 목록 조회 중... (networkType: {})", networkType);

        try {
            String response = webClient.get()
                .uri(SKT_PLAN_LIST_URL)
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (response == null || response.isEmpty()) {
                log.warn("⚠️ SKT 요금제 목록 응답 없음");
                return new ArrayList<>();
            }

            List<SktRatePlan> plans = parseRatePlanResponse(response, networkType);

            // 캐시 저장
            planCache.put(cacheKey, plans);
            planCacheTime = System.currentTimeMillis();

            log.info("✅ SKT 요금제 {}개 조회 완료 (공시지원금 대상만)", plans.size());
            return plans;

        } catch (Exception e) {
            log.error("❌ SKT 요금제 목록 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * SKT 요금제 응답 파싱
     */
    private List<SktRatePlan> parseRatePlanResponse(String jsonResponse, String networkType) {
        List<SktRatePlan> plans = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            // SKT 응답 구조: { content: [...] } 또는 배열
            JsonNode contentNode = root.path("content");
            if (contentNode.isMissingNode() && root.isArray()) {
                contentNode = root;
            }

            if (contentNode.isArray()) {
                for (JsonNode node : contentNode) {
                    // subcategoryId='H'인 요금제만 공시지원금 대상
                    String subcategoryId = JsonNodeUtils.getTextValue(node, "subcategoryId");
                    if (!"H".equals(subcategoryId)) {
                        continue;
                    }

                    String subscriptionId = JsonNodeUtils.getTextValue(node, "subscriptionId");
                    String subscriptionNm = JsonNodeUtils.getTextValue(node, "subscriptionNm");
                    Integer basicCharge = JsonNodeUtils.getIntValue(node, "basicCharge");

                    if (subscriptionId == null || basicCharge == null) {
                        continue;
                    }

                    // 네트워크 타입 필터링 (5G: 40,000원 이상, LTE: 40,000원 미만)
                    if (networkType != null && !networkType.isEmpty()) {
                        boolean is5G = basicCharge >= 40000;
                        if ("5G".equalsIgnoreCase(networkType) && !is5G) {
                            continue;
                        }
                        if ("LTE".equalsIgnoreCase(networkType) && is5G) {
                            continue;
                        }
                    }

                    SktRatePlan plan = SktRatePlan.builder()
                        .subscriptionId(subscriptionId)
                        .subscriptionNm(subscriptionNm)
                        .basicCharge(basicCharge)
                        .dataOffer(JsonNodeUtils.getTextValue(node, "dataOffer"))
                        .callOffer(JsonNodeUtils.getTextValue(node, "callOffer"))
                        .smsOffer(JsonNodeUtils.getTextValue(node, "smsOffer"))
                        .subcategoryId(subcategoryId)
                        .categoryId(JsonNodeUtils.getTextValue(node, "categoryId"))
                        .displayYn(JsonNodeUtils.getTextValue(node, "displayYn"))
                        .build();

                    plans.add(plan);
                }
            }
        } catch (Exception e) {
            log.error("SKT 요금제 응답 파싱 실패: {}", e.getMessage());
        }

        return plans;
    }
}
