package com.nofee.api.test.devicemapping.controller;

import com.nofee.api.test.carrierintegration.dto.UnifiedSubsidyResponse;
import com.nofee.api.test.devicemapping.dto.DeviceMapping;
import com.nofee.api.test.devicemapping.service.DeviceMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 기기 매핑 API 컨트롤러
 *
 * 노피 상품 ↔ 통신사 기기 매핑 관리
 */
@Tag(name = "Device Mapping", description = "기기 매핑 API (테스트)")
@RestController
@RequestMapping("/api/test/device-mapping")
@RequiredArgsConstructor
public class DeviceMappingController {

    private final DeviceMappingService deviceMappingService;

    /**
     * 모든 매핑 조회
     */
    @Operation(summary = "전체 매핑 조회", description = "Google Sheets에 저장된 모든 매핑 데이터를 조회합니다.")
    @GetMapping("/mappings")
    public ResponseEntity<List<DeviceMapping>> getMappings() {
        List<DeviceMapping> mappings = deviceMappingService.getMappings();
        return ResponseEntity.ok(mappings);
    }

    /**
     * 특정 상품 코드로 매핑 조회
     * 프론트엔드 호환 형식: { success: true, data: DeviceMapping }
     */
    @Operation(summary = "단일 매핑 조회", description = "노피 상품 코드로 특정 매핑을 조회합니다.")
    @GetMapping("/mapping/{nofeeCode}")
    public ResponseEntity<Map<String, Object>> getMappingByCode(@PathVariable String nofeeCode) {
        DeviceMapping mapping = deviceMappingService.getMappingByCode(nofeeCode);
        if (mapping == null) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "매핑을 찾을 수 없습니다: " + nofeeCode
            ));
        }
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", mapping
        ));
    }

    /**
     * 동기화 수행 (수동 트리거)
     */
    @Operation(summary = "매핑 동기화", description = "노피 DB와 통신사 API를 조회하여 매핑을 수행하고 Google Sheets에 저장합니다.")
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncMappings() {
        DeviceMappingService.SyncResult result = deviceMappingService.syncMappings();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "count", result.count(),
            "elapsed", result.elapsed(),
            "message", String.format("%d개 매핑 완료 (%.1f초)", result.count(), result.elapsed())
        ));
    }

    /**
     * 노피 상품 목록 조회 (디버그용)
     */
    @Operation(summary = "노피 상품 조회", description = "DB에서 활성 상태의 노피 상품 목록을 조회합니다.")
    @GetMapping("/nofee-products")
    public ResponseEntity<?> getNofeeProducts() {
        return ResponseEntity.ok(deviceMappingService.fetchNofeeProducts());
    }

    // ==================== 공시지원금 조회 API (CarrierIntegration 연동) ====================

    /**
     * 노피 상품 코드로 공시지원금 조회
     */
    @Operation(summary = "노피 상품별 공시지원금 조회", description = "노피 상품 코드로 매핑된 통신사 기기의 공시지원금을 조회합니다.")
    @GetMapping("/subsidies/{nofeeCode}")
    public ResponseEntity<UnifiedSubsidyResponse> getSubsidiesByNofeeCode(@PathVariable String nofeeCode) {
        UnifiedSubsidyResponse response = deviceMappingService.getSubsidiesByNofeeProductCode(nofeeCode);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 통신사 공시지원금 조회 (캐시 우선)
     */
    @Operation(summary = "전체 공시지원금 조회", description = "SKT, KT, LGU+ 전체 공시지원금을 조회합니다. (캐시 우선)")
    @GetMapping("/subsidies")
    public ResponseEntity<UnifiedSubsidyResponse> getAllSubsidies(
            @RequestParam(defaultValue = "false") boolean refresh) {
        UnifiedSubsidyResponse response = refresh
            ? deviceMappingService.refreshAllSubsidies()
            : deviceMappingService.getAllSubsidies();
        return ResponseEntity.ok(response);
    }

    /**
     * 매핑 직접 추가 (테스트용)
     */
    @Operation(summary = "매핑 추가", description = "테스트용 매핑 데이터를 직접 추가합니다.")
    @PostMapping("/mappings")
    public ResponseEntity<Map<String, Object>> addMapping(@RequestBody DeviceMapping mapping) {
        deviceMappingService.addMapping(mapping);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "매핑 추가 완료: " + mapping.getNofeeProductCode()
        ));
    }
}
