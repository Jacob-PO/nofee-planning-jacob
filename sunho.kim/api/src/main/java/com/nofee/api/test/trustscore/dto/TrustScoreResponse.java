package com.nofee.api.test.trustscore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 신뢰 높이(Trust Score) 응답 DTO
 *
 * EXECUTION_GUIDE.md 기준:
 * - T = 0.6 × (σ 일치 신호) + 0.4 × (후기/클레임)
 * - 내부 0~1 → UI 1~100층 맵핑
 * - 랭킹 가중치: 최소 0.35 고정 (가격만으로 상단 불가)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrustScoreResponse {

    /**
     * 매장 ID
     */
    private String storeId;

    /**
     * 매장명
     */
    private String storeName;

    /**
     * 신뢰 높이 (층 단위: 1~100층)
     * 내부 trustScore(0~1)를 UI용 층 단위로 변환
     */
    private int trustFloor;

    /**
     * 내부 신뢰 점수 (0.0 ~ 1.0)
     */
    private double trustScore;

    /**
     * 가격 일치율 (0.0 ~ 1.0)
     * σ(스냅샷) 대비 실제 가격 일치 비율
     */
    private double priceMatchRate;

    /**
     * 조건 일치율 (0.0 ~ 1.0)
     * σ(스냅샷) 대비 실제 조건 일치 비율
     */
    private double conditionMatchRate;

    /**
     * 평균 평점 (1.0 ~ 5.0)
     */
    private double avgRating;

    /**
     * 리뷰 수
     */
    private int reviewCount;

    /**
     * 클레임 수
     */
    private int claimCount;

    /**
     * 방문자 수
     */
    private int visitCount;

    /**
     * 마지막 업데이트 시간
     */
    private LocalDateTime lastUpdatedAt;

    /**
     * 신뢰 등급 (EXCELLENT, GOOD, NORMAL, WARNING, POOR)
     */
    private TrustGrade grade;

    /**
     * 신뢰 등급 enum
     *
     * EXECUTION_GUIDE.md 제재 정책 기준:
     * - T >= 0.7: EXCELLENT
     * - T >= 0.5: GOOD
     * - T >= 0.3: NORMAL
     * - T >= 0.2: WARNING (노출 -50%)
     * - T < 0.2: POOR (하단 고정)
     */
    public enum TrustGrade {
        EXCELLENT("우수", "신뢰도가 매우 높은 매장입니다"),
        GOOD("양호", "신뢰도가 높은 매장입니다"),
        NORMAL("보통", "일반적인 수준의 매장입니다"),
        WARNING("주의", "주의가 필요한 매장입니다"),
        POOR("경고", "신뢰도가 낮은 매장입니다");

        private final String displayName;
        private final String description;

        TrustGrade(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }
}
