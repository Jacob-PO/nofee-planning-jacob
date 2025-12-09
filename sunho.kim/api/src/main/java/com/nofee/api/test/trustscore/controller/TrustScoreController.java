package com.nofee.api.test.trustscore.controller;

import com.nofee.api.test.trustscore.dto.TrustScoreHistoryResponse;
import com.nofee.api.test.trustscore.dto.TrustScoreResponse;
import com.nofee.api.test.trustscore.dto.TrustScoreUpdateRequest;
import com.nofee.api.test.trustscore.service.TrustScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 신뢰 높이(Trust Score) API 컨트롤러
 *
 * EXECUTION_GUIDE.md API 스케치:
 * - POST /trust:update     { txId } → { trustScore }
 * - GET  /stores/{storeId}/trust → { score, history }
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/trust")
@RequiredArgsConstructor
@Tag(name = "Trust Score", description = "신뢰 높이(Trust Score) API - 매장 신뢰도 조회 및 업데이트")
public class TrustScoreController {

    private final TrustScoreService trustScoreService;

    /**
     * 특정 매장의 신뢰 점수 조회
     */
    @GetMapping("/stores/{storeId}")
    @Operation(
            summary = "매장 신뢰 점수 조회",
            description = "특정 매장의 신뢰 높이(Trust Score)를 조회합니다. " +
                    "내부 점수(0~1)와 UI용 층(1~100층) 모두 반환합니다."
    )
    public ResponseEntity<TrustScoreResponse> getTrustScore(
            @Parameter(description = "매장 ID", example = "store-001")
            @PathVariable String storeId
    ) {
        log.info("GET /api/v1/trust/stores/{}", storeId);
        TrustScoreResponse response = trustScoreService.getTrustScore(storeId);
        return ResponseEntity.ok(response);
    }

    /**
     * 여러 매장의 신뢰 점수 일괄 조회
     */
    @GetMapping("/stores")
    @Operation(
            summary = "여러 매장 신뢰 점수 일괄 조회",
            description = "여러 매장의 신뢰 높이를 한 번에 조회합니다."
    )
    public ResponseEntity<List<TrustScoreResponse>> getTrustScores(
            @Parameter(description = "매장 ID 목록 (쉼표 구분)", example = "store-001,store-002,store-003")
            @RequestParam List<String> storeIds
    ) {
        log.info("GET /api/v1/trust/stores?storeIds={}", storeIds);
        List<TrustScoreResponse> responses = trustScoreService.getTrustScores(storeIds);
        return ResponseEntity.ok(responses);
    }

    /**
     * 신뢰 점수 업데이트 (거래 완료 후)
     */
    @PostMapping("/update")
    @Operation(
            summary = "신뢰 점수 업데이트",
            description = "거래 완료 후 신뢰 점수를 업데이트합니다. " +
                    "EMA 알고리즘을 사용하여 점진적으로 업데이트됩니다. " +
                    "Δ = 0.5×priceMatch + 0.3×conditionMatch + 0.2×reviewScore - 0.5"
    )
    public ResponseEntity<TrustScoreResponse> updateTrustScore(
            @Valid @RequestBody TrustScoreUpdateRequest request
    ) {
        log.info("POST /api/v1/trust/update - storeId: {}, txId: {}",
                request.getStoreId(), request.getTransactionId());
        TrustScoreResponse response = trustScoreService.updateTrustScore(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 매장 신뢰 점수 히스토리 조회
     */
    @GetMapping("/stores/{storeId}/history")
    @Operation(
            summary = "매장 신뢰 점수 히스토리 조회",
            description = "매장의 신뢰 점수 변동 히스토리를 조회합니다."
    )
    public ResponseEntity<TrustScoreHistoryResponse> getTrustScoreHistory(
            @Parameter(description = "매장 ID", example = "store-001")
            @PathVariable String storeId
    ) {
        log.info("GET /api/v1/trust/stores/{}/history", storeId);
        TrustScoreHistoryResponse response = trustScoreService.getTrustScoreHistory(storeId);
        return ResponseEntity.ok(response);
    }
}
