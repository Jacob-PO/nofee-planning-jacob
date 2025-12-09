package com.nofee.api.test.trustscore.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 신뢰 점수 업데이트 요청 DTO
 *
 * EXECUTION_GUIDE.md EMA 업데이트 공식:
 * Δ = 0.5×priceMatch + 0.3×conditionMatch + 0.2×reviewScore - 0.5
 * T_next = α×T + (1-α)×Δ   // α ≈ 0.95
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrustScoreUpdateRequest {

    /**
     * 거래 ID (Transaction ID)
     */
    @NotBlank(message = "거래 ID는 필수입니다")
    private String transactionId;

    /**
     * 매장 ID
     */
    @NotBlank(message = "매장 ID는 필수입니다")
    private String storeId;

    /**
     * 스냅샷 ID (σ)
     */
    @NotBlank(message = "스냅샷 ID는 필수입니다")
    private String snapshotId;

    /**
     * 실제 할부원금 (원)
     */
    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    private int actualInstallmentPrice;

    /**
     * 스냅샷 할부원금 (원)
     */
    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    private int snapshotInstallmentPrice;

    /**
     * 조건 일치 여부
     */
    private boolean conditionMatched;

    /**
     * 리뷰 평점 (1~5, 없으면 null)
     */
    @Min(value = 1, message = "평점은 1 이상이어야 합니다")
    @Max(value = 5, message = "평점은 5 이하여야 합니다")
    private Integer reviewRating;
}
