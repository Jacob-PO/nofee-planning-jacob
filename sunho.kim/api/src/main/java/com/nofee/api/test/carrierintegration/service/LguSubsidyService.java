package com.nofee.api.test.carrierintegration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nofee.api.test.carrierintegration.dto.CarrierSubsidy;
import com.nofee.api.test.carrierintegration.dto.LguRatePlan;
import com.nofee.api.test.carrierintegration.util.JsonNodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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
    private static final String LGU_PLAN_LIST_URL = LGU_BASE_URL + "/uhdc/fo/prdv/mdlbsufu/v1/mdlb-pp-list";
    private static final int ROW_SIZE = 1000;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // 5G 프리미어 에센셜 85,000원
    private static final String DEFAULT_PLAN_CODE = "LPZ0000409";
    private static final String DEFAULT_JOIN_TYPE = "2"; // 번호이동

    // 저장용량 추출용 정규식 (성능 최적화: 컴파일 1회)
    private static final Pattern STORAGE_PATTERN = Pattern.compile("(\\d+)\\s*(GB|TB)", Pattern.CASE_INSENSITIVE);

    // 요금제 캐시 (5분 TTL)
    private final Map<String, List<LguRatePlan>> planCache = new ConcurrentHashMap<>();
    private volatile long planCacheTime = 0;
    private static final long PLAN_CACHE_TTL = 5 * 60 * 1000; // 5분

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
        return fetchAllSubsidies(planCode, joinType, "5G");
    }

    /**
     * LGU+ 공시지원금 전체 목록 조회 (네트워크 유형 지정)
     *
     * @param planCode 요금제 코드 (예: LPZ0000409)
     * @param joinType 가입유형 (1: 신규가입, 2: 번호이동, 3: 기기변경)
     * @param networkType 네트워크 유형 (5G 또는 LTE)
     */
    public List<CarrierSubsidy> fetchAllSubsidies(String planCode, String joinType, String networkType) {
        String effectivePlanCode = (planCode != null && !planCode.isEmpty()) ? planCode : DEFAULT_PLAN_CODE;
        String effectiveJoinType = (joinType != null && !joinType.isEmpty()) ? joinType : DEFAULT_JOIN_TYPE;
        String effectiveNetworkType = (networkType != null && !networkType.isEmpty()) ? networkType : "5G";

        log.info("📡 LGU+ 공시지원금 조회 중... (요금제: {}, 가입유형: {}, 네트워크: {})",
            effectivePlanCode, effectiveJoinType, effectiveNetworkType);

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
     * 특정 기기의 공시지원금 조회 (기본 파라미터)
     */
    public List<CarrierSubsidy> fetchSubsidiesByDevice(String deviceCode) {
        return fetchSubsidiesByDevice(deviceCode, null, null);
    }

    /**
     * 특정 기기의 공시지원금 조회 (파라미터 지정)
     *
     * @param deviceCode 기기 코드
     * @param joinType 가입유형 (한글: 신규, 기기변경, 번호이동)
     * @param planMonthlyFee 요금제 월정액 (원 단위)
     */
    public List<CarrierSubsidy> fetchSubsidiesByDevice(String deviceCode, String joinType, Integer planMonthlyFee) {
        log.info("📡 LGU+ 기기별 조회: deviceCode={}, joinType={}, planMonthlyFee={}",
            deviceCode, joinType, planMonthlyFee);

        // joinType 한글 → LGU 코드 변환
        String lguJoinTypeCode = convertKoreanToLguJoinType(joinType);

        // 요금제 코드 결정 (월정액으로는 직접 매핑 불가, 기본값 사용)
        String planCode = DEFAULT_PLAN_CODE;

        // API 호출
        List<CarrierSubsidy> allSubsidies = fetchAllSubsidiesWithJoinType(planCode, lguJoinTypeCode, joinType);

        // 기기 코드로 필터링
        List<CarrierSubsidy> filtered = allSubsidies.stream()
            .filter(s -> deviceCode.equals(s.getDeviceCode()))
            .toList();

        log.info("📊 LGU+ 기기 필터링 결과: {}건 (전체 {}건)", filtered.size(), allSubsidies.size());
        return filtered;
    }

    /**
     * 한글 가입유형 → LGU API 코드 변환
     * LGU+ joinType: 1=기기변경, 2=번호이동, 3=신규
     */
    private String convertKoreanToLguJoinType(String korean) {
        if (korean == null) return DEFAULT_JOIN_TYPE;
        return switch (korean) {
            case "신규" -> "3";
            case "기기변경" -> "1";
            case "번호이동" -> "2";
            default -> DEFAULT_JOIN_TYPE;
        };
    }

    /**
     * joinType을 응답에 반영하는 전체 조회
     */
    private List<CarrierSubsidy> fetchAllSubsidiesWithJoinType(String planCode, String joinTypeCode, String joinTypeKorean) {
        String effectivePlanCode = (planCode != null && !planCode.isEmpty()) ? planCode : DEFAULT_PLAN_CODE;
        String effectiveJoinType = (joinTypeCode != null && !joinTypeCode.isEmpty()) ? joinTypeCode : DEFAULT_JOIN_TYPE;
        String effectiveJoinTypeKorean = (joinTypeKorean != null && !joinTypeKorean.isEmpty())
            ? joinTypeKorean
            : CarrierSubsidy.convertJoinTypeToKorean(effectiveJoinType, "LGU");

        log.info("📡 LGU+ 공시지원금 조회 중... (요금제: {}, 가입유형: {} -> {})",
            effectivePlanCode, effectiveJoinType, effectiveJoinTypeKorean);

        try {
            // WebClient 대신 RestTemplate 스타일로 직접 HTTP 호출
            String url = String.format(
                "https://www.lguplus.com/uhdc/fo/prdv/mdlbsufu/v2/mdlb-sufu-list?urcMblPpCd=%s&urcHphnEntrPsblKdCd=%s&rowSize=%d&sortOrd=00",
                effectivePlanCode, effectiveJoinType, ROW_SIZE
            );

            log.debug("📡 LGU+ API 호출 URL: {}", url);

            String response = webClient.get()
                .uri(url)
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Referer", "https://www.lguplus.com/mobile/phone")
                .exchangeToMono(clientResponse -> {
                    log.debug("📡 LGU+ 응답 상태: {}", clientResponse.statusCode());
                    return clientResponse.bodyToMono(String.class);
                })
                .block();

            if (response == null || response.isEmpty()) {
                log.warn("⚠️ LGU+ 응답 없음");
                return new ArrayList<>();
            }

            log.debug("📡 LGU+ 응답 길이: {} bytes", response.length());

            List<CarrierSubsidy> subsidies = parseLguResponseWithJoinType(response, effectiveJoinTypeKorean);
            log.info("✅ LGU+ 공시지원금 {}개 조회 완료 (joinType={})", subsidies.size(), effectiveJoinTypeKorean);
            return subsidies;

        } catch (Exception e) {
            log.error("❌ LGU+ 공시지원금 조회 오류: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("상세 스택트레이스:", e);
            }
            return new ArrayList<>();
        }
    }

    /**
     * joinType을 동적으로 설정하는 파싱
     */
    private List<CarrierSubsidy> parseLguResponseWithJoinType(String jsonResponse, String joinTypeKorean) {
        List<CarrierSubsidy> subsidies = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            JsonNode dataList = root.path("dvicMdlbSufuDtoList");
            if (dataList.isMissingNode()) {
                dataList = root.path("data").path("dvicMdlbSufuDtoList");
            }
            if (dataList.isMissingNode()) {
                dataList = root.path("list");
            }

            if (dataList.isArray()) {
                for (JsonNode node : dataList) {
                    CarrierSubsidy subsidy = parseLguNodeWithJoinType(node, joinTypeKorean);
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
     * joinType을 동적으로 설정하는 노드 파싱
     * LGU+ API 필드명:
     * - rlCoutDttm: 공시일 (YYYY-MM-DD 형식)
     */
    private CarrierSubsidy parseLguNodeWithJoinType(JsonNode node, String joinTypeKorean) {
        try {
            String mdlCd = JsonNodeUtils.getTextValue(node, "urcTrmMdlCd");
            String mdlNm = JsonNodeUtils.getTextValue(node, "urcTrmMdlNm");

            if (mdlCd == null || mdlNm == null) {
                return null;
            }

            // 공시일 파싱 (YYYY-MM-DD 형식 그대로)
            String announceDate = JsonNodeUtils.getTextValue(node, "rlCoutDttm");

            // API 응답에서 요금제 정보 추출
            String planCode = JsonNodeUtils.getTextValue(node, "urcMblPpCd");
            String planName = JsonNodeUtils.getTextValue(node, "mblPpNm");
            Integer monthlyFee = JsonNodeUtils.getIntValue(node, "mblPpFee");
            if (monthlyFee == null) {
                monthlyFee = JsonNodeUtils.getIntValue(node, "basicPlanMthlyFee");
            }

            CarrierSubsidy subsidy = CarrierSubsidy.builder()
                .carrier("LGU")
                .deviceCode(mdlCd)
                .deviceName(mdlNm)
                .manufacturer(JsonNodeUtils.getTextValue(node, "dvicManfEngNm"))
                .storage(extractStorage(mdlNm))
                .planCode(planCode != null ? planCode : DEFAULT_PLAN_CODE)
                .planName(planName != null ? planName : "5G 프리미어 에센셜")
                .planMonthlyFee(monthlyFee) // API 응답에서 가져옴 (없으면 null)
                .planMaintainMonth(6)
                .msrp(JsonNodeUtils.getIntValue(node, "dlvrPrc"))
                .carrierSubsidy(JsonNodeUtils.getIntValue(node, "basicPlanPuanSuptAmt"))
                .additionalSubsidy(JsonNodeUtils.getIntValue(node, "basicPlanAddSuptAmt"))
                .installmentPrice(JsonNodeUtils.getIntValue(node, "basicPlanBuyPrc"))
                .joinType(joinTypeKorean)  // 동적으로 설정
                .discountType("공시지원")
                .announceDate(announceDate)
                .rawData(node.toString())
                .build();
            subsidy.setId(subsidy.generateId());
            return subsidy;
        } catch (Exception e) {
            log.debug("LGU+ 노드 파싱 실패: {}", e.getMessage());
            return null;
        }
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
     * - rlCoutDttm: 공시일 (YYYY-MM-DD 형식)
     */
    private CarrierSubsidy parseLguNode(JsonNode node) {
        try {
            String mdlCd = JsonNodeUtils.getTextValue(node, "urcTrmMdlCd");
            String mdlNm = JsonNodeUtils.getTextValue(node, "urcTrmMdlNm");

            if (mdlCd == null || mdlNm == null) {
                return null;
            }

            String joinTypeKorean = CarrierSubsidy.convertJoinTypeToKorean(DEFAULT_JOIN_TYPE, "LGU");

            // 공시일 파싱 (YYYY-MM-DD 형식 그대로)
            String announceDate = JsonNodeUtils.getTextValue(node, "rlCoutDttm");

            // API 응답에서 요금제 정보 추출
            String planCode = JsonNodeUtils.getTextValue(node, "urcMblPpCd");
            String planName = JsonNodeUtils.getTextValue(node, "mblPpNm");
            Integer monthlyFee = JsonNodeUtils.getIntValue(node, "mblPpFee");
            if (monthlyFee == null) {
                monthlyFee = JsonNodeUtils.getIntValue(node, "basicPlanMthlyFee");
            }

            CarrierSubsidy subsidy = CarrierSubsidy.builder()
                .carrier("LGU")
                .deviceCode(mdlCd)
                .deviceName(mdlNm)
                .manufacturer(JsonNodeUtils.getTextValue(node, "dvicManfEngNm"))
                .storage(extractStorage(mdlNm)) // 기기명에서 추출
                .planCode(planCode != null ? planCode : DEFAULT_PLAN_CODE)
                .planName(planName != null ? planName : "5G 프리미어 에센셜")
                .planMonthlyFee(monthlyFee) // API 응답에서 가져옴 (없으면 null)
                .planMaintainMonth(6)
                .msrp(JsonNodeUtils.getIntValue(node, "dlvrPrc"))
                .carrierSubsidy(JsonNodeUtils.getIntValue(node, "basicPlanPuanSuptAmt"))
                .additionalSubsidy(JsonNodeUtils.getIntValue(node, "basicPlanAddSuptAmt"))
                .installmentPrice(JsonNodeUtils.getIntValue(node, "basicPlanBuyPrc"))
                .joinType(joinTypeKorean)
                .discountType("공시지원")
                .supportType("공시지원금")
                .announceDate(announceDate)
                .rawData(node.toString())
                .build();
            subsidy.setId(subsidy.generateId());
            return subsidy;
        } catch (Exception e) {
            log.debug("LGU+ 노드 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 기기명에서 저장용량 추출
     */
    private String extractStorage(String deviceName) {
        if (deviceName == null) return null;
        var matcher = STORAGE_PATTERN.matcher(deviceName);
        return matcher.find() ? matcher.group(1) + matcher.group(2).toUpperCase() : null;
    }

    // ==================== 요금제 목록 API ====================

    /**
     * LGU+ 요금제 목록 조회
     *
     * API: /uhdc/fo/prdv/mdlbsufu/v1/mdlb-pp-list
     * - 5G: 74개, LTE: 22개 요금제
     *
     * @param networkType 네트워크 유형 (5G 또는 LTE)
     * @return 요금제 목록
     */
    public List<LguRatePlan> fetchRatePlans(String networkType) {
        // 네트워크 타입 → API 파라미터 변환 (00=5G, 01=LTE, 03=태블릿/워치)
        String hphnPpGrpKwrdCd = "5G".equalsIgnoreCase(networkType) ? "00" :
                                 "LTE".equalsIgnoreCase(networkType) ? "01" : "00";
        String cacheKey = networkType != null ? networkType : "5G";

        // 캐시 체크
        if (System.currentTimeMillis() - planCacheTime < PLAN_CACHE_TTL && planCache.containsKey(cacheKey)) {
            log.debug("📦 LGU 요금제 캐시 히트: {}", cacheKey);
            return planCache.get(cacheKey);
        }

        log.info("📡 LGU+ 요금제 목록 조회 중... (networkType: {})", networkType);

        try {
            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .scheme("https")
                    .host("www.lguplus.com")
                    .path("/uhdc/fo/prdv/mdlbsufu/v1/mdlb-pp-list")
                    .queryParam("hphnPpGrpKwrdCd", hphnPpGrpKwrdCd)
                    .build())
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (response == null || response.isEmpty()) {
                log.warn("⚠️ LGU+ 요금제 목록 응답 없음");
                return new ArrayList<>();
            }

            List<LguRatePlan> plans = parseRatePlanResponse(response, networkType);

            // 캐시 저장
            planCache.put(cacheKey, plans);
            planCacheTime = System.currentTimeMillis();

            log.info("✅ LGU+ 요금제 {}개 조회 완료", plans.size());
            return plans;

        } catch (Exception e) {
            log.error("❌ LGU+ 요금제 목록 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * LGU+ 요금제 응답 파싱
     */
    private List<LguRatePlan> parseRatePlanResponse(String jsonResponse, String networkType) {
        List<LguRatePlan> plans = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            // LGU+ 응답 구조: { dvicMdlbSufuPpList: [ { dvicMdlbSufuPpDetlList: [...] } ] }
            JsonNode ppListNode = root.path("dvicMdlbSufuPpList");

            if (ppListNode.isArray()) {
                for (JsonNode groupNode : ppListNode) {
                    String trmPpGrpNm = JsonNodeUtils.getTextValue(groupNode, "trmPpGrpNm");
                    String urcTrmPpGrpKwrdCd = JsonNodeUtils.getTextValue(groupNode, "urcTrmPpGrpKwrdCd");

                    JsonNode detailListNode = groupNode.path("dvicMdlbSufuPpDetlList");
                    if (detailListNode.isArray()) {
                        for (JsonNode node : detailListNode) {
                            String urcMblPpCd = JsonNodeUtils.getTextValue(node, "urcMblPpCd");
                            String urcMblPpNm = JsonNodeUtils.getTextValue(node, "urcMblPpNm");
                            Integer urcPpBasfAmt = parseMonthlyFee(JsonNodeUtils.getTextValue(node, "urcPpBasfAmt"));

                            if (urcMblPpCd == null || urcPpBasfAmt == null) {
                                continue;
                            }

                            LguRatePlan plan = LguRatePlan.builder()
                                .urcMblPpCd(urcMblPpCd)
                                .urcMblPpNm(urcMblPpNm)
                                .urcPpBasfAmt(urcPpBasfAmt)
                                .lastBasfAmt(parseMonthlyFee(JsonNodeUtils.getTextValue(node, "lastBasfAmt")))
                                .mm24ChocAgmtDcntAmt(parseMonthlyFee(JsonNodeUtils.getTextValue(node, "mm24ChocAgmtDcntAmt")))
                                .mm24ChocAgmtDcntTamt(parseMonthlyFee(JsonNodeUtils.getTextValue(node, "mm24ChocAgmtDcntTamt")))
                                .mblMcnPpDataScrnEposDscr(JsonNodeUtils.getTextValue(node, "mblMcnPpDataScrnEposDscr"))
                                .nagmPpYn(node.path("nagmPpYn").asBoolean(false))
                                .ppDirtDcntAplyPsblYn(node.path("ppDirtDcntAplyPsblYn").asBoolean(false))
                                .urcTrmPpGrpKwrdCd(urcTrmPpGrpKwrdCd)
                                .trmPpGrpNm(trmPpGrpNm)
                                .build();

                            plans.add(plan);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("LGU+ 요금제 응답 파싱 실패: {}", e.getMessage());
        }

        return plans;
    }

    /**
     * 월정액 문자열 파싱 (숫자만 추출)
     */
    private Integer parseMonthlyFee(String feeStr) {
        if (feeStr == null || feeStr.isEmpty()) return null;
        try {
            String numericStr = feeStr.replaceAll("[^0-9]", "");
            return numericStr.isEmpty() ? null : Integer.parseInt(numericStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
