package com.nofee.api.test.carrierintegration.controller;

import com.nofee.api.test.carrierintegration.dto.*;
import com.nofee.api.test.carrierintegration.service.CarrierIntegrationService;
import com.nofee.api.test.carrierintegration.service.CarrierPlanFetchService;
import com.nofee.api.test.carrierintegration.service.CarrierPlanSyncService;
import com.nofee.api.test.carrierintegration.service.KtSubsidyService;
import com.nofee.api.test.carrierintegration.service.SktSubsidyService;
import com.nofee.api.test.carrierintegration.service.LguSubsidyService;
import com.nofee.api.test.carrierintegration.service.NofeeRatePlanService;
import com.nofee.api.test.carrierintegration.service.PlanSheetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 통합 통신사 API 컨트롤러
 *
 * SKT, KT, LGU+ 공시지원금 통합 조회 API
 * Lazy Cache 적용 - Google Sheets를 캐시로 활용 (24시간 TTL)
 */
@Tag(name = "Carrier Integration", description = "통합 통신사 공시지원금 API (테스트)")
@RestController
@RequestMapping("/api/test/carrier")
@RequiredArgsConstructor
public class CarrierIntegrationController {

    private final CarrierIntegrationService carrierIntegrationService;
    private final PlanSheetService planSheetService;
    private final CarrierPlanFetchService carrierPlanFetchService;
    private final CarrierPlanSyncService carrierPlanSyncService;
    private final NofeeRatePlanService nofeeRatePlanService;
    private final KtSubsidyService ktSubsidyService;
    private final SktSubsidyService sktSubsidyService;
    private final LguSubsidyService lguSubsidyService;

    /**
     * 전체 통신사 공시지원금 통합 조회 (캐시 우선)
     */
    @Operation(summary = "전체 공시지원금 조회", description = "SKT, KT, LGU+ 공시지원금을 조회합니다. 캐시가 있으면 캐시에서, 없으면 API 호출 후 캐시 저장.")
    @GetMapping("/subsidies")
    public ResponseEntity<UnifiedSubsidyResponse> getAllSubsidies(
            @RequestParam(defaultValue = "false") boolean refresh) {
        UnifiedSubsidyResponse response = refresh
            ? carrierIntegrationService.refreshAllCache()
            : carrierIntegrationService.fetchAllSubsidies();
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 통신사 공시지원금 조회 (캐시 우선)
     *
     * joinType 코드 가이드:
     * - SKT: 10(신규가입), 20(번호이동), 30(기기변경)
     * - KT: 01(신규가입), 02(번호이동), 04(기기변경)
     * - LGU: 1(신규가입), 2(번호이동), 3(기기변경)
     *
     * planMonthlyFee: 요금제 월 금액 (예: 85000, 89000, 90000)
     */
    @Operation(summary = "통신사별 공시지원금 조회",
        description = "특정 통신사(SKT, KT, LGU)의 공시지원금을 조회합니다. " +
            "planCode, joinType, planMonthlyFee 지정 가능. " +
            "joinType: SKT(10/20/30), KT(01/02/04), LGU(1/2/3)")
    @GetMapping("/subsidies/{carrier}")
    public ResponseEntity<Map<String, Object>> getSubsidiesByCarrier(
            @PathVariable String carrier,
            @RequestParam(required = false) String planCode,
            @RequestParam(required = false) String joinType,
            @RequestParam(required = false) Integer planMonthlyFee,
            @RequestParam(defaultValue = "false") boolean refresh) {
        List<CarrierSubsidy> subsidies = carrierIntegrationService.fetchSubsidiesByCarrierWithParams(
            carrier, planCode, joinType, planMonthlyFee, refresh);

        boolean fromCache = !refresh && (planCode == null || planCode.isEmpty())
                         && (joinType == null || joinType.isEmpty())
                         && planMonthlyFee == null;

        return ResponseEntity.ok(Map.of(
            "success", true,
            "carrier", carrier.toUpperCase(),
            "planCode", planCode != null ? planCode : "default",
            "joinType", joinType != null ? joinType : "default",
            "planMonthlyFee", planMonthlyFee != null ? planMonthlyFee : "default",
            "count", subsidies.size(),
            "fromCache", fromCache,
            "data", subsidies
        ));
    }

    /**
     * 기기별 통합 공시지원금 조회
     */
    @Operation(summary = "기기별 공시지원금 조회", description = "각 통신사 기기 코드로 통합 공시지원금을 조회합니다.")
    @PostMapping("/subsidies/device")
    public ResponseEntity<UnifiedSubsidyResponse> getSubsidiesByDevice(@RequestBody SubsidyRequest request) {
        UnifiedSubsidyResponse response = carrierIntegrationService.fetchSubsidiesByDevice(
            request.getDeviceCode(),
            request.getDeviceCode(),
            request.getDeviceCode()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 통신사별 기기 코드로 통합 조회
     *
     * joinType: 가입유형 (한글: 신규, 기기변경, 번호이동)
     * planMonthlyFee: 요금제 월정액 (예: 37000, 85000)
     */
    @Operation(summary = "통신사별 기기 코드로 조회",
        description = "SKT, KT, LGU+ 각각의 기기 코드로 통합 조회합니다. " +
            "joinType(신규/기기변경/번호이동)과 planMonthlyFee(월정액)로 필터링 가능.")
    @GetMapping("/subsidies/device")
    public ResponseEntity<UnifiedSubsidyResponse> getSubsidiesByDeviceCodes(
            @RequestParam(required = false) String sktCode,
            @RequestParam(required = false) String ktCode,
            @RequestParam(required = false) String lguCode,
            @RequestParam(required = false) String joinType,
            @RequestParam(required = false) Integer planMonthlyFee) {

        // joinType과 planMonthlyFee가 있으면 조건부 조회
        if (joinType != null && !joinType.isEmpty() && planMonthlyFee != null) {
            // planMonthlyFee를 planCode 대신 사용 (캐시키에 월정액 사용)
            String planCode = String.valueOf(planMonthlyFee);
            UnifiedSubsidyResponse response = carrierIntegrationService.fetchSubsidiesByDeviceWithCondition(
                sktCode, ktCode, lguCode, joinType, planCode
            );
            return ResponseEntity.ok(response);
        }

        // 조건 없으면 전체 조회 (기존 로직)
        UnifiedSubsidyResponse response = carrierIntegrationService.fetchSubsidiesByDevice(
            sktCode, ktCode, lguCode
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 노피 상품 코드로 통합 조회
     */
    @Operation(summary = "노피 상품별 공시지원금 조회", description = "노피 상품 코드로 매핑된 통신사 기기의 공시지원금을 조회합니다.")
    @GetMapping("/subsidies/nofee/{nofeeProductCode}")
    public ResponseEntity<UnifiedSubsidyResponse> getSubsidiesByNofeeProduct(
            @PathVariable String nofeeProductCode) {
        UnifiedSubsidyResponse response = carrierIntegrationService.fetchSubsidiesByNofeeProduct(nofeeProductCode);
        return ResponseEntity.ok(response);
    }

    // ==================== 캐시 관리 API ====================

    /**
     * 캐시 상태 조회
     */
    @Operation(summary = "캐시 상태 조회", description = "각 통신사별 캐시 상태를 확인합니다.")
    @GetMapping("/cache/status")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        return ResponseEntity.ok(carrierIntegrationService.getCacheStatus());
    }

    /**
     * 캐시 강제 갱신 (전체 데이터)
     */
    @Operation(summary = "캐시 강제 갱신", description = "모든 통신사 데이터를 API에서 새로 조회하여 캐시를 갱신합니다. 전체 덮어쓰기.")
    @PostMapping("/cache/refresh")
    public ResponseEntity<UnifiedSubsidyResponse> refreshCache() {
        return ResponseEntity.ok(carrierIntegrationService.refreshAllCache());
    }

    /**
     * 증분 업데이트 (공시일 최근 N일)
     *
     * 기존 캐시가 있으면 최근 공시일 데이터만 조회해서 업데이트
     * 기존 캐시가 없으면 전체 조회 실행
     */
    @Operation(summary = "증분 업데이트", description = "공시일이 최근 N일인 데이터만 조회해서 기존 캐시와 병합합니다. " +
        "처음 실행 시 전체 조회, 이후에는 증분 업데이트만 수행.")
    @PostMapping("/cache/incremental")
    public ResponseEntity<Map<String, Object>> incrementalUpdate(
            @RequestParam(defaultValue = "7") int days) {
        UnifiedSubsidyResponse response = carrierIntegrationService.incrementalUpdate(days);
        return ResponseEntity.ok(Map.of(
            "success", response.isSuccess(),
            "message", response.isSuccess()
                ? String.format("증분 업데이트 완료 (최근 %d일 공시일 기준)", days)
                : "증분 업데이트 실패: " + response.getErrorMessage(),
            "elapsedMs", response.getElapsedMs(),
            "counts", Map.of(
                "skt", response.getSktSubsidies() != null ? response.getSktSubsidies().size() : 0,
                "kt", response.getKtSubsidies() != null ? response.getKtSubsidies().size() : 0,
                "lgu", response.getLguSubsidies() != null ? response.getLguSubsidies().size() : 0
            )
        ));
    }

    /**
     * 캐시 초기화 (삭제)
     */
    @Operation(summary = "캐시 초기화", description = "모든 캐시 데이터를 삭제합니다.")
    @DeleteMapping("/cache")
    public ResponseEntity<Map<String, Object>> clearCache() {
        carrierIntegrationService.clearCache();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "캐시가 초기화되었습니다."
        ));
    }

    /**
     * API 상태 확인
     */
    @Operation(summary = "API 상태 확인", description = "통합 API 상태를 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "carrier-integration",
            "cache", "Google Sheets (24h TTL)",
            "endpoints", Map.of(
                "getAllSubsidies", "GET /api/test/carrier/subsidies?refresh=false",
                "getByCarrier", "GET /api/test/carrier/subsidies/{carrier}?refresh=false",
                "getByDevice", "GET /api/test/carrier/subsidies/device?sktCode=&ktCode=&lguCode=",
                "getByNofeeProduct", "GET /api/test/carrier/subsidies/nofee/{code}",
                "cacheStatus", "GET /api/test/carrier/cache/status",
                "refreshCache", "POST /api/test/carrier/cache/refresh (전체 갱신)",
                "incrementalUpdate", "POST /api/test/carrier/cache/incremental?days=7 (증분 업데이트)",
                "clearCache", "DELETE /api/test/carrier/cache",
                "getPlans", "GET /api/test/carrier/plans",
                "syncPlans", "POST /api/test/carrier/plans/sync"
            )
        ));
    }

    // ==================== 요금제 관리 API ====================

    /**
     * 전체 요금제 목록 조회
     */
    @Operation(summary = "전체 요금제 조회", description = "summary-plan 시트에 저장된 모든 요금제를 조회합니다.")
    @GetMapping("/plans")
    public ResponseEntity<Map<String, Object>> getAllPlans() {
        List<CarrierPlan> plans = planSheetService.getAllPlans();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "count", plans.size(),
            "data", plans,
            "cacheStatus", planSheetService.getCacheStatus()
        ));
    }

    /**
     * 통신사별 요금제 목록 조회
     */
    @Operation(summary = "통신사별 요금제 조회", description = "특정 통신사(SKT, KT, LGU)의 활성화된 요금제를 조회합니다.")
    @GetMapping("/plans/{carrier}")
    public ResponseEntity<Map<String, Object>> getPlansByCarrier(@PathVariable String carrier) {
        List<CarrierPlan> plans = planSheetService.getActivePlansByCarrier(carrier);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "carrier", carrier.toUpperCase(),
            "count", plans.size(),
            "data", plans
        ));
    }

    /**
     * 월정액으로 요금제 코드 조회
     */
    @Operation(summary = "월정액으로 요금제 코드 조회", description = "통신사와 월정액으로 요금제 코드를 조회합니다.")
    @GetMapping("/plans/{carrier}/code")
    public ResponseEntity<Map<String, Object>> getPlanCodeByMonthlyFee(
            @PathVariable String carrier,
            @RequestParam Integer monthlyFee) {
        String planCode = planSheetService.getPlanCodeByMonthlyFee(carrier, monthlyFee);
        CarrierPlan plan = planCode != null ? planSheetService.getPlan(carrier, planCode) : null;

        return ResponseEntity.ok(Map.of(
            "success", planCode != null,
            "carrier", carrier.toUpperCase(),
            "monthlyFee", monthlyFee,
            "planCode", planCode != null ? planCode : "",
            "plan", plan != null ? plan : Map.of()
        ));
    }

    /**
     * 요금제 정보 동기화 (기본 요금제 데이터 -> summary-plan 시트)
     */
    @Operation(summary = "요금제 동기화", description = "기본 요금제 데이터를 summary-plan 시트에 저장합니다.")
    @PostMapping("/plans/sync")
    public ResponseEntity<Map<String, Object>> syncPlans() {
        List<CarrierPlan> plans = carrierPlanFetchService.fetchAllPlans();
        planSheetService.savePlans(plans);

        Map<String, Long> carrierCounts = plans.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                CarrierPlan::getCarrier,
                java.util.stream.Collectors.counting()
            ));

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "요금제 동기화 완료",
            "totalCount", plans.size(),
            "byCarrier", carrierCounts
        ));
    }

    /**
     * 시트에서 요금제 다시 로드
     */
    @Operation(summary = "요금제 시트 리로드", description = "summary-plan 시트에서 요금제를 다시 로드합니다.")
    @PostMapping("/plans/reload")
    public ResponseEntity<Map<String, Object>> reloadPlans() {
        planSheetService.loadPlansFromSheet();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "요금제 시트 리로드 완료",
            "cacheStatus", planSheetService.getCacheStatus()
        ));
    }

    /**
     * 요금제 캐시 상태 조회
     */
    @Operation(summary = "요금제 캐시 상태", description = "요금제 캐시 상태를 조회합니다.")
    @GetMapping("/plans/status")
    public ResponseEntity<Map<String, Object>> getPlanCacheStatus() {
        return ResponseEntity.ok(planSheetService.getCacheStatus());
    }

    // ==================== 노피 DB 요금제 API ====================

    /**
     * 노피 DB 요금제 목록 조회
     */
    @Operation(summary = "노피 DB 요금제 조회", description = "노피 DB의 tb_rate_plan_phone 테이블에서 요금제를 조회합니다.")
    @GetMapping("/nofee/plans")
    public ResponseEntity<Map<String, Object>> getNofeeRatePlans(
            @RequestParam(required = false) String carrierCode) {
        List<NofeeRatePlan> plans = (carrierCode != null && !carrierCode.isEmpty())
            ? nofeeRatePlanService.getRatePlansByCarrier(carrierCode)
            : nofeeRatePlanService.getAllRatePlans();

        return ResponseEntity.ok(Map.of(
            "success", true,
            "source", "노피 DB (tb_rate_plan_phone)",
            "carrierCode", carrierCode != null ? carrierCode : "ALL",
            "count", plans.size(),
            "data", plans
        ));
    }

    /**
     * 노피 DB 요금제 금액 목록 (중복 제거)
     */
    @Operation(summary = "요금제 금액 목록", description = "노피 DB에서 통신사별 요금제 금액 목록을 조회합니다 (중복 제거).")
    @GetMapping("/nofee/plans/fees")
    public ResponseEntity<Map<String, Object>> getNofeeMonthlyFees() {
        List<NofeeRatePlanSummary> fees = nofeeRatePlanService.getDistinctMonthlyFees();

        Map<String, List<Integer>> feesByCarrier = fees.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                NofeeRatePlanSummary::getCarrierCode,
                java.util.stream.Collectors.mapping(
                    NofeeRatePlanSummary::getMonthFee,
                    java.util.stream.Collectors.toList()
                )
            ));

        return ResponseEntity.ok(Map.of(
            "success", true,
            "source", "노피 DB",
            "totalCount", fees.size(),
            "byCarrier", feesByCarrier
        ));
    }

    /**
     * 통신사 기본 정책 조회
     */
    @Operation(summary = "통신사 기본 정책", description = "노피 DB의 tb_carrier_plan_phone에서 통신사 기본 정책을 조회합니다.")
    @GetMapping("/nofee/policy")
    public ResponseEntity<Map<String, Object>> getCarrierPlanPolicy() {
        List<CarrierPlanPolicy> policies = nofeeRatePlanService.getCarrierPlanPolicy();

        return ResponseEntity.ok(Map.of(
            "success", true,
            "source", "노피 DB (tb_carrier_plan_phone)",
            "count", policies.size(),
            "data", policies
        ));
    }

    /**
     * 요금제 매핑 정보 조회 (노피 코드 -> 통신사 API 코드)
     */
    @Operation(summary = "요금제 매핑 정보", description = "노피 DB 요금제와 통신사 API 요금제 코드 매핑 정보를 조회합니다.")
    @GetMapping("/nofee/plans/mapping")
    public ResponseEntity<Map<String, Object>> getPlanMappingInfo() {
        return ResponseEntity.ok(nofeeRatePlanService.getPlanMappingInfo());
    }

    /**
     * 노피 DB 요금제 -> summary-plan 시트 동기화
     *
     * 노피 DB의 요금제 정보를 기반으로 통신사 API 요금제 코드를 매핑하여
     * summary-plan 시트를 업데이트합니다.
     */
    @Operation(summary = "노피 DB -> summary-plan 동기화",
        description = "노피 DB의 요금제 정보를 기반으로 통신사 API 요금제 코드를 매핑하여 summary-plan 시트를 업데이트합니다.")
    @PostMapping("/nofee/plans/sync")
    public ResponseEntity<Map<String, Object>> syncFromNofeeDb() {
        Map<String, Object> result = nofeeRatePlanService.syncPlanSheetFromNofeeDb();
        return ResponseEntity.ok(result);
    }

    // ==================== KT 요금제 API (통신사 직접 조회) ====================

    /**
     * KT 요금제 목록 조회 (통신사 API 직접 호출)
     *
     * API: /oneMinuteReform/supportAmtChoiceList.json
     * - 5G: 62개, LTE: 31개 요금제 반환
     * - 실제 KT API 요금제 코드(onfrmCd) 포함
     */
    @Operation(summary = "KT 요금제 목록 조회",
        description = "KT API에서 직접 요금제 목록을 조회합니다. 5G: 62개, LTE: 31개 요금제.")
    @GetMapping("/kt/plans")
    public ResponseEntity<Map<String, Object>> getKtRatePlans(
            @RequestParam(defaultValue = "5G") String networkType) {
        List<KtRatePlan> plans = ktSubsidyService.fetchRatePlans(networkType);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "source", "KT API (/oneMinuteReform/supportAmtChoiceList.json)",
            "networkType", networkType,
            "count", plans.size(),
            "data", plans
        ));
    }

    /**
     * KT 전체 요금제별 공시지원금 조회 (한번에 모든 값)
     *
     * 모든 요금제를 조회한 후 각 요금제별로 공시지원금을 가져옴
     * - 결과: Map<요금제코드, 공시지원금 목록>
     */
    @Operation(summary = "KT 전체 요금제별 공시지원금 조회",
        description = "KT의 모든 요금제에 대해 공시지원금을 한번에 조회합니다. " +
            "joinType: 01(신규), 02(번호이동), 04(기기변경)")
    @GetMapping("/kt/subsidies/all")
    public ResponseEntity<Map<String, Object>> getKtAllSubsidiesByAllPlans(
            @RequestParam(defaultValue = "04") String joinType,
            @RequestParam(defaultValue = "5G") String networkType) {

        long startTime = System.currentTimeMillis();
        Map<String, List<CarrierSubsidy>> subsidiesByPlan = ktSubsidyService.fetchAllSubsidiesByAllPlans(joinType, networkType);
        long elapsed = System.currentTimeMillis() - startTime;

        int totalSubsidies = subsidiesByPlan.values().stream().mapToInt(List::size).sum();

        // 요금제별 요약 정보
        Map<String, Integer> planSummary = new java.util.LinkedHashMap<>();
        subsidiesByPlan.forEach((planCode, subsidies) -> {
            planSummary.put(planCode, subsidies.size());
        });

        return ResponseEntity.ok(Map.of(
            "success", true,
            "source", "KT API (전체 요금제)",
            "joinType", joinType,
            "networkType", networkType,
            "planCount", subsidiesByPlan.size(),
            "totalSubsidyCount", totalSubsidies,
            "elapsedMs", elapsed,
            "planSummary", planSummary,
            "data", subsidiesByPlan
        ));
    }

    // ==================== 통신사 API 요금제 Sync API ====================

    /**
     * 전체 통신사 요금제를 통신사 API에서 직접 조회하여 summary-plan 시트에 동기화
     *
     * - KT: /oneMinuteReform/supportAmtChoiceList.json (5G 62개, LTE 31개)
     * - SKT: /api/wireless/subscription/list (공시지원금 대상 740개)
     * - LGU: /uhdc/fo/prdv/mdlbsufu/v1/mdlb-pp-list (5G 74개, LTE 22개)
     */
    @Operation(summary = "통신사 API 요금제 전체 동기화",
        description = "KT, SKT, LGU+ 각 통신사 공식 API에서 요금제 목록을 조회하여 summary-plan 시트에 동기화합니다.")
    @PostMapping("/plans/sync/carrier-api")
    public ResponseEntity<Map<String, Object>> syncFromCarrierApi() {
        Map<String, Object> result = carrierPlanSyncService.syncAllCarrierPlans();
        return ResponseEntity.ok(result);
    }

    /**
     * KT 요금제만 통신사 API에서 동기화
     */
    @Operation(summary = "KT 요금제 동기화",
        description = "KT 공식 API에서 요금제 목록을 조회하여 summary-plan 시트에 동기화합니다.")
    @PostMapping("/plans/sync/kt")
    public ResponseEntity<Map<String, Object>> syncKtPlansFromApi() {
        Map<String, Object> result = carrierPlanSyncService.syncKtPlans();
        return ResponseEntity.ok(result);
    }

    // ==================== SKT 요금제 API (통신사 직접 조회) ====================

    /**
     * SKT 요금제 목록 조회 (통신사 API 직접 호출)
     *
     * API: /api/wireless/subscription/list
     * - 공시지원금 대상: subcategoryId='H'인 요금제 (약 740개)
     */
    @Operation(summary = "SKT 요금제 목록 조회",
        description = "SKT API에서 직접 요금제 목록을 조회합니다. 공시지원금 대상(subcategoryId='H')만 반환.")
    @GetMapping("/skt/plans")
    public ResponseEntity<Map<String, Object>> getSktRatePlans(
            @RequestParam(defaultValue = "5G") String networkType) {
        List<SktRatePlan> plans = sktSubsidyService.fetchRatePlans(networkType);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "source", "SKT API (/api/wireless/subscription/list)",
            "networkType", networkType,
            "count", plans.size(),
            "data", plans
        ));
    }

    // ==================== LGU 요금제 API (통신사 직접 조회) ====================

    /**
     * LGU+ 요금제 목록 조회 (통신사 API 직접 호출)
     *
     * API: /uhdc/fo/prdv/mdlbsufu/v1/mdlb-pp-list
     * - 5G: 74개, LTE: 22개 요금제
     */
    @Operation(summary = "LGU+ 요금제 목록 조회",
        description = "LGU+ API에서 직접 요금제 목록을 조회합니다. 5G: 74개, LTE: 22개.")
    @GetMapping("/lgu/plans")
    public ResponseEntity<Map<String, Object>> getLguRatePlans(
            @RequestParam(defaultValue = "5G") String networkType) {
        List<LguRatePlan> plans = lguSubsidyService.fetchRatePlans(networkType);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "source", "LGU+ API (/uhdc/fo/prdv/mdlbsufu/v1/mdlb-pp-list)",
            "networkType", networkType,
            "count", plans.size(),
            "data", plans
        ));
    }
}
