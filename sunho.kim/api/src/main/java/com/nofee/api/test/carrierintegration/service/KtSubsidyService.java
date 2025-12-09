package com.nofee.api.test.carrierintegration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nofee.api.test.carrierintegration.dto.CarrierSubsidy;
import com.nofee.api.test.carrierintegration.dto.KtRatePlan;
import com.nofee.api.test.carrierintegration.util.JsonNodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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
    private static final String KT_PLAN_LIST_URL = KT_BASE_URL + "/oneMinuteReform/supportAmtChoiceList.json";
    private static final String KT_SESSION_URL = KT_BASE_URL + "/smart/supportAmtList.do?channel=VS";

    // 요금제 캐시 (5분간 유지)
    private final Map<String, List<KtRatePlan>> planCache = new ConcurrentHashMap<>();
    private volatile long planCacheTime = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5분

    // 티빙/지니/밀리 초이스 베이직 90,000원
    private static final String DEFAULT_PLAN_CODE = "PL244N945";
    private static final String DEFAULT_JOIN_TYPE = "04"; // 기기변경
    private static final String DEFAULT_DISCOUNT_OPTION = "HT"; // 기변-심플

    // 저장용량 추출용 정규식 (성능 최적화: 컴파일 1회)
    private static final Pattern STORAGE_PATTERN = Pattern.compile("(\\d+)\\s*(GB|TB)", Pattern.CASE_INSENSITIVE);

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
        return fetchAllSubsidies(planCode, joinType, "5G");
    }

    /**
     * KT 공시지원금 전체 목록 조회 (네트워크 유형 지정)
     *
     * @param planCode 요금제 코드 (예: PL244N945)
     * @param joinType 가입유형 (01: 신규가입, 02: 번호이동, 04: 기기변경)
     * @param networkType 네트워크 유형 (5G 또는 LTE)
     */
    public List<CarrierSubsidy> fetchAllSubsidies(String planCode, String joinType, String networkType) {
        String effectivePlanCode = (planCode != null && !planCode.isEmpty()) ? planCode : DEFAULT_PLAN_CODE;
        String effectiveJoinType = (joinType != null && !joinType.isEmpty()) ? joinType : DEFAULT_JOIN_TYPE;
        String effectiveNetworkType = (networkType != null && !networkType.isEmpty()) ? networkType : "5G";
        // KT prodType: 30=5G, 20=LTE
        String prodType = "LTE".equalsIgnoreCase(effectiveNetworkType) ? "20" : "30";

        log.info("📡 KT 공시지원금 조회 중... (요금제: {}, 가입유형: {}, 네트워크: {} -> prodType: {})",
            effectivePlanCode, effectiveJoinType, effectiveNetworkType, prodType);

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
                PageResult pageResult = fetchPageWithInfo(sessionCookie, page, effectivePlanCode, effectiveJoinType, prodType);
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
     * KT 요금제 목록 조회 (캐시 적용)
     *
     * API: /oneMinuteReform/supportAmtChoiceList.json
     * - 5G: 62개, LTE: 31개 요금제
     *
     * @param networkType 네트워크 유형 (5G 또는 LTE)
     * @return 요금제 목록
     */
    public List<KtRatePlan> fetchRatePlans(String networkType) {
        String effectiveNetworkType = (networkType != null && !networkType.isEmpty()) ? networkType : "5G";
        String cacheKey = effectiveNetworkType;

        // 캐시 확인
        if (System.currentTimeMillis() - planCacheTime < CACHE_TTL_MS && planCache.containsKey(cacheKey)) {
            log.debug("📦 KT 요금제 캐시 사용: {}", cacheKey);
            return planCache.get(cacheKey);
        }

        log.info("📡 KT 요금제 목록 조회 중... (네트워크: {})", effectiveNetworkType);

        try {
            String sessionCookie = getSessionCookie();
            if (sessionCookie == null) {
                log.warn("⚠️ KT 세션 쿠키 획득 실패");
                return new ArrayList<>();
            }

            String response = webClient.post()
                .uri(KT_PLAN_LIST_URL)
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .header("Cookie", sessionCookie)
                .header("Referer", KT_SESSION_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("pageNo", "1")
                    .with("pplType", effectiveNetworkType)
                    .with("pplSelect", "ALL")
                    .with("spnsMonsType", "2")
                    .with("sortPpl", "amtDesc")
                    .with("deviceType", "HDP"))
                .retrieve()
                .bodyToMono(String.class)
                .block();

            List<KtRatePlan> plans = parseRatePlanResponse(response);

            // 캐시 저장
            planCache.put(cacheKey, plans);
            planCacheTime = System.currentTimeMillis();

            log.info("✅ KT 요금제 {}개 조회 완료 ({})", plans.size(), effectiveNetworkType);
            return plans;

        } catch (Exception e) {
            log.error("❌ KT 요금제 목록 조회 오류: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 요금제 응답 파싱
     */
    private List<KtRatePlan> parseRatePlanResponse(String jsonResponse) {
        List<KtRatePlan> plans = new ArrayList<>();
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            return plans;
        }

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode planList = root.path("punoPplList");

            if (planList.isArray()) {
                for (JsonNode node : planList) {
                    KtRatePlan plan = KtRatePlan.builder()
                        .onfrmCd(JsonNodeUtils.getTextValue(node, "onfrmCd"))
                        .pplNm(JsonNodeUtils.getTextValue(node, "pplNm"))
                        .punoMonthUseChage(parseMonthlyFee(JsonNodeUtils.getTextValue(node, "punoMonthUseChage")))
                        .punoMonthUseDcChage(JsonNodeUtils.getIntValue(node, "punoMonthUseDcChage"))
                        .pplGb(JsonNodeUtils.getTextValue(node, "pplGb"))
                        .pplGrpCd(JsonNodeUtils.getTextValue(node, "pplGrpCd"))
                        .pplId(JsonNodeUtils.getTextValue(node, "pplId"))
                        .dataBasic(JsonNodeUtils.getTextValue(node, "dataBasic"))
                        .tlkBasic(JsonNodeUtils.getTextValue(node, "tlkBasic"))
                        .charBasic(JsonNodeUtils.getTextValue(node, "charBasic"))
                        .build();
                    plans.add(plan);
                }
            }
        } catch (Exception e) {
            log.error("KT 요금제 응답 파싱 실패: {}", e.getMessage());
        }

        return plans;
    }

    /**
     * 월정액 문자열 파싱 (예: "90,000" -> 90000)
     */
    private Integer parseMonthlyFee(String feeStr) {
        if (feeStr == null || feeStr.isEmpty()) return null;
        try {
            return Integer.parseInt(feeStr.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 모든 요금제로 공시지원금 조회 (한번에 전체 데이터)
     *
     * 요금제 목록 API로 요금제 조회 후, 각 요금제별로 공시지원금 조회
     * - 결과를 Map으로 반환: key=요금제코드, value=공시지원금 목록
     *
     * @param joinType 가입유형 (01: 신규가입, 02: 번호이동, 04: 기기변경)
     * @param networkType 네트워크 유형 (5G 또는 LTE)
     * @return Map<요금제코드, 공시지원금 목록>
     */
    public Map<String, List<CarrierSubsidy>> fetchAllSubsidiesByAllPlans(String joinType, String networkType) {
        String effectiveJoinType = (joinType != null && !joinType.isEmpty()) ? joinType : DEFAULT_JOIN_TYPE;
        String effectiveNetworkType = (networkType != null && !networkType.isEmpty()) ? networkType : "5G";

        log.info("📡 KT 전체 요금제별 공시지원금 조회 시작 (가입유형: {}, 네트워크: {})",
            effectiveJoinType, effectiveNetworkType);

        Map<String, List<CarrierSubsidy>> result = new ConcurrentHashMap<>();

        // 1. 요금제 목록 조회
        List<KtRatePlan> plans = fetchRatePlans(effectiveNetworkType);
        if (plans.isEmpty()) {
            log.warn("⚠️ KT 요금제 목록이 비어있습니다.");
            return result;
        }

        log.info("📊 KT {}개 요금제에 대해 공시지원금 조회 시작", plans.size());

        // 2. 각 요금제별로 공시지원금 조회
        for (KtRatePlan plan : plans) {
            if (plan.getOnfrmCd() == null || plan.getOnfrmCd().isEmpty()) {
                continue;
            }

            try {
                List<CarrierSubsidy> subsidies = fetchAllSubsidies(plan.getOnfrmCd(), effectiveJoinType, effectiveNetworkType);

                // 요금제 정보 보강
                for (CarrierSubsidy subsidy : subsidies) {
                    if (subsidy.getPlanMonthlyFee() == null || subsidy.getPlanMonthlyFee() == 0) {
                        subsidy.setPlanMonthlyFee(plan.getPunoMonthUseChage());
                    }
                    if (subsidy.getPlanName() == null || subsidy.getPlanName().isEmpty()) {
                        subsidy.setPlanName(plan.getPplNm());
                    }
                    if (subsidy.getPlanCode() == null || subsidy.getPlanCode().isEmpty()) {
                        subsidy.setPlanCode(plan.getOnfrmCd());
                    }
                }

                result.put(plan.getOnfrmCd(), subsidies);
                log.debug("  {} ({}원): {}건", plan.getPplNm(), plan.getPunoMonthUseChage(), subsidies.size());

            } catch (Exception e) {
                log.warn("⚠️ 요금제 {} 조회 실패: {}", plan.getOnfrmCd(), e.getMessage());
            }
        }

        int totalCount = result.values().stream().mapToInt(List::size).sum();
        log.info("✅ KT 전체 공시지원금 조회 완료: {}개 요금제, 총 {}건", result.size(), totalCount);

        return result;
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
     *
     * @param sessionCookie 세션 쿠키
     * @param page 페이지 번호
     * @param planCode 요금제 코드
     * @param joinType 가입유형
     * @param prodType 상품유형 (30=5G, 20=LTE)
     */
    private PageResult fetchPageWithInfo(String sessionCookie, int page, String planCode, String joinType, String prodType) {
        try {
            String response = webClient.post()
                .uri(KT_SUBSIDY_URL)
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .header("Cookie", sessionCookie)
                .header("Referer", KT_SESSION_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("prodNm", "mobile")
                    .with("prdcCd", planCode)
                    .with("prodType", prodType)  // 30=5G, 20=LTE
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
     * - spnsrPunoDate: 공시일 (YYYYMMDDHHMISS 형식)
     */
    private CarrierSubsidy parseKtNode(JsonNode node) {
        try {
            String petNm = JsonNodeUtils.getTextValue(node, "petNm");
            String hndsetModelNm = JsonNodeUtils.getTextValue(node, "hndsetModelNm");

            if (petNm == null || hndsetModelNm == null) {
                return null;
            }

            Integer ofwAmt = JsonNodeUtils.getIntValue(node, "ofwAmt");
            Integer ktSuprtAmt = JsonNodeUtils.getIntValue(node, "ktSuprtAmt");
            Integer realAmt = JsonNodeUtils.getIntValue(node, "realAmt");

            // 저장용량 추출 (기기명에서)
            String storage = extractStorage(petNm);

            String joinTypeKorean = CarrierSubsidy.convertJoinTypeToKorean(DEFAULT_JOIN_TYPE, "KT");

            // 공시일 파싱 (YYYYMMDDHHMISS -> YYYY-MM-DD)
            String spnsrPunoDate = JsonNodeUtils.getTextValue(node, "spnsrPunoDate");
            String announceDate = formatKtDate(spnsrPunoDate);

            // API 응답에서 요금제 월정액 추출 (pplBasicAmt 또는 basicAmt 필드)
            Integer basicAmt = JsonNodeUtils.getIntValue(node, "pplBasicAmt");
            if (basicAmt == null) {
                basicAmt = JsonNodeUtils.getIntValue(node, "basicAmt");
            }

            CarrierSubsidy subsidy = CarrierSubsidy.builder()
                .carrier("KT")
                .deviceCode(hndsetModelNm)
                .deviceName(petNm)
                .manufacturer(getMakrName(JsonNodeUtils.getTextValue(node, "makrCd")))
                .storage(storage)
                .planCode(JsonNodeUtils.getTextValue(node, "pplId"))
                .planName(JsonNodeUtils.getTextValue(node, "pplNm"))
                .planMonthlyFee(basicAmt) // API 응답에서 가져옴 (없으면 null)
                .planMaintainMonth(6)
                .msrp(ofwAmt)
                .carrierSubsidy(ktSuprtAmt)
                .additionalSubsidy(0) // KT는 추가지원금 없음
                .installmentPrice(realAmt)
                .joinType(joinTypeKorean)
                .discountType("공시지원")
                .supportType("공시지원금")
                .announceDate(announceDate)
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
     * KT 날짜 형식 변환 (YYYYMMDDHHMISS -> YYYY-MM-DD)
     */
    private String formatKtDate(String dateStr) {
        if (dateStr == null || dateStr.length() < 8) return null;
        try {
            return dateStr.substring(0, 4) + "-" + dateStr.substring(4, 6) + "-" + dateStr.substring(6, 8);
        } catch (Exception e) {
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
        var matcher = STORAGE_PATTERN.matcher(deviceName);
        return matcher.find() ? matcher.group(1) + matcher.group(2).toUpperCase() : null;
    }

}
