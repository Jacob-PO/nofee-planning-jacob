package com.nofee.api.test.trustscore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 신뢰 점수 히스토리 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrustScoreHistoryResponse {

    /**
     * 매장 ID
     */
    private String storeId;

    /**
     * 매장명
     */
    private String storeName;

    /**
     * 현재 신뢰 점수
     */
    private double currentTrustScore;

    /**
     * 현재 신뢰 높이 (층)
     */
    private int currentTrustFloor;

    /**
     * 히스토리 목록
     */
    private List<TrustScoreHistoryItem> history;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrustScoreHistoryItem {
        /**
         * 거래 ID
         */
        private String transactionId;

        /**
         * 이전 점수
         */
        private double previousScore;

        /**
         * 변경 후 점수
         */
        private double newScore;

        /**
         * 점수 변동 (delta)
         */
        private double scoreDelta;

        /**
         * 가격 일치 여부
         */
        private boolean priceMatched;

        /**
         * 조건 일치 여부
         */
        private boolean conditionMatched;

        /**
         * 리뷰 평점 (있는 경우)
         */
        private Integer reviewRating;

        /**
         * 기록 시간
         */
        private LocalDateTime recordedAt;
    }
}
