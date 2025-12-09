package com.nofee.api.test.carrierintegration.controller;

import com.nofee.api.test.carrierintegration.dto.UnifiedSubsidyResponse;
import com.nofee.api.test.carrierintegration.service.DirectSubsidyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 직접 조회 API 컨트롤러 (캐시 없이 실시간 통신사 API 호출)
 *
 * 사용법:
 * 1. 노피 상품코드로 조회: GET /api/test/carrier/direct/nofee/{code}?joinType=&planMonthlyFee=
 * 2. 통신사별 기기코드로 조회: GET /api/test/carrier/direct/device?sktCode=&ktCode=&lguCode=&joinType=&planMonthlyFee=
 */
@Tag(name = "Direct Subsidy", description = "직접 공시지원금 조회 API (실시간, 캐시 없음)")
@RestController
@RequestMapping("/api/test/carrier/direct")
@RequiredArgsConstructor
public class DirectSubsidyController {

    private final DirectSubsidyService directSubsidyService;

    /**
     * 노피 상품코드 + 통신사로 공시지원금 직접 조회 (단일 통신사)
     *
     * @param nofeeProductCode 노피 상품코드 (예: "SM-ZP-7")
     * @param carrier 통신사 (SKT, KT, LGU 또는 노피코드 0301001001, 0301001002, 0301001003)
     * @param joinType 가입유형 (0301007001=신규, 0301007002=기기변경, 0301007003=번호이동)
     * @param planMonthlyFee 요금제 월정액 (예: 89000)
     */
    @Operation(summary = "노피 상품코드 + 통신사로 직접 조회 (권장)",
        description = "노피 상품코드와 통신사를 지정하여 해당 통신사만 조회합니다. " +
            "summary-mapping에서 기기코드를, summary-plan에서 요금제코드를 자동으로 찾습니다.")
    @GetMapping("/subsidy")
    public ResponseEntity<UnifiedSubsidyResponse> getByNofeeProductAndCarrier(
            @RequestParam String nofeeProductCode,
            @RequestParam String carrier,
            @RequestParam String joinType,
            @RequestParam Integer planMonthlyFee) {

        UnifiedSubsidyResponse response = directSubsidyService.fetchByNofeeProductAndCarrier(
            nofeeProductCode, carrier, joinType, planMonthlyFee);
        return ResponseEntity.ok(response);
    }

    /**
     * 노피 상품코드로 공시지원금 직접 조회 (전체 통신사)
     *
     * @param nofeeProductCode 노피 상품코드 (예: "AP-E-16")
     * @param joinType 가입유형 (0301007001=신규, 0301007002=기기변경, 0301007003=번호이동)
     * @param planMonthlyFee 요금제 월정액 (예: 85000)
     */
    @Operation(summary = "노피 상품코드로 직접 조회 (전체 통신사)",
        description = "노피 상품코드로 SKT, KT, LGU+ 전체를 조회합니다.")
    @GetMapping("/nofee/{nofeeProductCode}")
    public ResponseEntity<UnifiedSubsidyResponse> getByNofeeProduct(
            @PathVariable String nofeeProductCode,
            @RequestParam String joinType,
            @RequestParam Integer planMonthlyFee) {

        UnifiedSubsidyResponse response = directSubsidyService.fetchByNofeeProduct(
            nofeeProductCode, joinType, planMonthlyFee);
        return ResponseEntity.ok(response);
    }

    /**
     * 상품명 + 통신사로 공시지원금 직접 조회
     *
     * @param productGroupNm 상품명 (예: "갤럭시 S24 울트라")
     * @param carrier 통신사 노피코드 (0301001001=SKT, 0301001002=KT, 0301001003=LGU)
     * @param joinType 가입유형 노피코드 (0301007001=신규, 0301007002=기기변경, 0301007003=번호이동)
     * @param planMonthlyFee 요금제 월정액 (예: 69000)
     * @param networkType 네트워크 유형 (5G 또는 LTE)
     * @param supportType 지원유형 (공시지원금, 선택약정) - 선택약정 시 공시지원금/추가지원금 0원
     */
    @Operation(summary = "상품명으로 공시지원금 조회",
        description = "상품명과 통신사 코드로 공시지원금을 조회합니다. summary-mapping에서 상품명으로 기기코드를 찾아 해당 통신사 API를 호출합니다.")
    @GetMapping("/device")
    public ResponseEntity<UnifiedSubsidyResponse> getByProductGroupNm(
            @RequestParam String productGroupNm,
            @RequestParam String carrier,
            @RequestParam String joinType,
            @RequestParam Integer planMonthlyFee,
            @RequestParam String networkType,
            @RequestParam String supportType) {

        UnifiedSubsidyResponse response = directSubsidyService.fetchByProductGroupNm(
            productGroupNm, carrier, joinType, planMonthlyFee, networkType, supportType);
        return ResponseEntity.ok(response);
    }

    /**
     * API 상태 확인
     */
    @Operation(summary = "API 상태 확인", description = "직접 조회 API 상태를 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "direct-subsidy",
            "description", "캐시 없이 실시간 통신사 API 직접 호출",
            "endpoints", Map.of(
                "subsidy (권장)", "GET /api/test/carrier/direct/subsidy?nofeeProductCode=SM-ZP-7&carrier=SKT&joinType=번호이동&planMonthlyFee=89000",
                "byNofeeProduct", "GET /api/test/carrier/direct/nofee/{code}?joinType=&planMonthlyFee=",
                "byDeviceCodes", "GET /api/test/carrier/direct/device?sktCode=&ktCode=&lguCode=&joinType=&planMonthlyFee="
            ),
            "carrierCodes", Map.of(
                "SKT", "0301001001",
                "KT", "0301001002",
                "LGU", "0301001003"
            ),
            "joinTypeCodes", Map.of(
                "신규", "0301007001",
                "기기변경", "0301007002",
                "번호이동", "0301007003"
            ),
            "planMonthlyFeeExamples", "37000, 55000, 69000, 85000, 89000, 90000"
        ));
    }
}
