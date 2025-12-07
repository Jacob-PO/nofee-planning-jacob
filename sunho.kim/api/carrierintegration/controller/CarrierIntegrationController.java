package com.nofee.api.test.carrierintegration.controller;

import com.nofee.api.test.carrierintegration.dto.CarrierSubsidy;
import com.nofee.api.test.carrierintegration.dto.SubsidyRequest;
import com.nofee.api.test.carrierintegration.dto.UnifiedSubsidyResponse;
import com.nofee.api.test.carrierintegration.service.CarrierIntegrationService;
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
     */
    @Operation(summary = "통신사별 기기 코드로 조회", description = "SKT, KT, LGU+ 각각의 기기 코드로 통합 조회합니다.")
    @GetMapping("/subsidies/device")
    public ResponseEntity<UnifiedSubsidyResponse> getSubsidiesByDeviceCodes(
            @RequestParam(required = false) String sktCode,
            @RequestParam(required = false) String ktCode,
            @RequestParam(required = false) String lguCode) {
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
     * 캐시 강제 갱신
     */
    @Operation(summary = "캐시 강제 갱신", description = "모든 통신사 데이터를 API에서 새로 조회하여 캐시를 갱신합니다.")
    @PostMapping("/cache/refresh")
    public ResponseEntity<UnifiedSubsidyResponse> refreshCache() {
        return ResponseEntity.ok(carrierIntegrationService.refreshAllCache());
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
                "refreshCache", "POST /api/test/carrier/cache/refresh",
                "clearCache", "DELETE /api/test/carrier/cache"
            )
        ));
    }
}
