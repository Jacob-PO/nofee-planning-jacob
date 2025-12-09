package com.nofee.api.test.trustscore.service;

import com.nofee.api.test.trustscore.dto.TrustScoreHistoryResponse;
import com.nofee.api.test.trustscore.dto.TrustScoreResponse;
import com.nofee.api.test.trustscore.dto.TrustScoreUpdateRequest;
import com.nofee.api.test.trustscore.mapper.TrustScoreMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 신뢰 높이(Trust Score) 서비스
 *
 * EXECUTION_GUIDE.md 기준:
 * - T = 0.6 × (σ 일치 신호) + 0.4 × (후기/클레임)
 * - EMA 업데이트: Δ = 0.5×priceMatch + 0.3×conditionMatch + 0.2×reviewScore - 0.5
 * - T_next = α×T + (1-α)×Δ   // α ≈ 0.95
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrustScoreService {

    private final TrustScoreMapper trustScoreMapper;

    /**
     * EMA 평활 계수 (alpha)
     */
    private static final double ALPHA = 0.95;

    /**
     * 가격 일치 허용 오차 (원)
     */
    private static final int PRICE_EPSILON = 1000;

    /**
     * 매장별 신뢰 점수 히스토리 (메모리 캐시)
     */
    private final Map<String, List<TrustScoreHistoryResponse.TrustScoreHistoryItem>> historyMap = new ConcurrentHashMap<>();

    /**
     * 특정 매장의 신뢰 점수 조회 (실제 DB 데이터 사용)
     */
    public TrustScoreResponse getTrustScore(String storeId) {
        try {
            Integer storeNo = Integer.parseInt(storeId);
            Map<String, Object> storeInfo = trustScoreMapper.selectStoreInfo(storeNo);

            if (storeInfo == null || storeInfo.isEmpty()) {
                log.warn("Store not found in DB: {}", storeId);
                return createNotFoundResponse(storeId);
            }

            // 리뷰 통계 조회
            Map<String, Object> reviewStats = trustScoreMapper.selectReviewStats(storeNo);

            return buildTrustScoreResponse(storeId, storeInfo, reviewStats);
        } catch (NumberFormatException e) {
            log.error("Invalid storeId format: {}", storeId);
            return createNotFoundResponse(storeId);
        } catch (Exception e) {
            log.error("Error fetching trust score for store {}: {}", storeId, e.getMessage());
            return createNotFoundResponse(storeId);
        }
    }

    /**
     * 여러 매장의 신뢰 점수 일괄 조회
     */
    public List<TrustScoreResponse> getTrustScores(List<String> storeIds) {
        List<TrustScoreResponse> results = new ArrayList<>();

        try {
            List<Integer> storeNos = storeIds.stream()
                    .map(Integer::parseInt)
                    .toList();

            List<Map<String, Object>> storeInfoList = trustScoreMapper.selectStoreInfoList(storeNos);

            for (Map<String, Object> storeInfo : storeInfoList) {
                Integer storeNo = ((Number) storeInfo.get("storeNo")).intValue();
                String storeId = String.valueOf(storeNo);
                Map<String, Object> reviewStats = trustScoreMapper.selectReviewStats(storeNo);
                results.add(buildTrustScoreResponse(storeId, storeInfo, reviewStats));
            }

            // DB에 없는 매장 처리
            for (String storeId : storeIds) {
                boolean found = results.stream()
                        .anyMatch(r -> r.getStoreId().equals(storeId));
                if (!found) {
                    results.add(createNotFoundResponse(storeId));
                }
            }
        } catch (Exception e) {
            log.error("Error fetching trust scores: {}", e.getMessage());
            // 에러 발생 시 개별 조회로 폴백
            return storeIds.stream()
                    .map(this::getTrustScore)
                    .toList();
        }

        return results;
    }

    /**
     * DB 데이터를 기반으로 TrustScoreResponse 빌드
     *
     * 신뢰 점수 계산 공식:
     * T = 0.6 × (리뷰 기반 점수) + 0.4 × (클레임 패널티 반영)
     *
     * 리뷰 기반 점수 = (avgRating / 5.0) × min(1.0, reviewCount / 10)
     * 클레임 패널티 = max(0, 1.0 - claimCount × 0.1)
     */
    private TrustScoreResponse buildTrustScoreResponse(String storeId, Map<String, Object> storeInfo, Map<String, Object> reviewStats) {
        String storeName = (String) storeInfo.get("storeName");
        int reviewCount = getIntValue(storeInfo, "reviewCount");
        double avgRating = getDoubleValue(storeInfo, "avgRating");
        int viewCount = getIntValue(storeInfo, "viewCount");
        int favoriteCount = getIntValue(storeInfo, "favoriteCount");
        int complaintCount = getIntValue(storeInfo, "complaintCount");

        // 리뷰 통계에서 추가 정보 가져오기
        int totalReviews = reviewStats != null ? getIntValue(reviewStats, "totalReviews") : reviewCount;
        double calculatedAvgRating = reviewStats != null ? getDoubleValue(reviewStats, "avgRating") : avgRating;
        int positiveReviews = reviewStats != null ? getIntValue(reviewStats, "positiveReviews") : 0;
        int negativeReviews = reviewStats != null ? getIntValue(reviewStats, "negativeReviews") : 0;

        // 최종 avgRating (리뷰 통계가 있으면 그걸 사용)
        if (calculatedAvgRating > 0) {
            avgRating = calculatedAvgRating;
        }

        // 신뢰 점수 계산
        double trustScore = calculateTrustScore(avgRating, totalReviews, complaintCount, viewCount, favoriteCount, positiveReviews, negativeReviews);

        // 가격/조건 일치율은 실제 거래 데이터가 쌓이면 계산 (현재는 기본값)
        double priceMatchRate = 0.85; // 기본값 (추후 거래 데이터로 계산)
        double conditionMatchRate = 0.90; // 기본값

        return TrustScoreResponse.builder()
                .storeId(storeId)
                .storeName(storeName != null ? storeName : "매장 " + storeId)
                .trustScore(trustScore)
                .trustFloor(scoreToFloor(trustScore))
                .priceMatchRate(priceMatchRate)
                .conditionMatchRate(conditionMatchRate)
                .avgRating(avgRating)
                .reviewCount(totalReviews)
                .claimCount(complaintCount)
                .visitCount(viewCount)
                .lastUpdatedAt(LocalDateTime.now())
                .grade(calculateGrade(trustScore))
                .build();
    }

    /**
     * 실제 데이터 기반 신뢰 점수 계산
     *
     * T = 0.6 × (리뷰 품질 점수) + 0.4 × (신뢰 요소)
     *
     * 리뷰 품질 점수:
     * - 평균 평점 비중 (avgRating / 5.0) × 0.7
     * - 리뷰 수 보너스 min(1.0, reviewCount / 20) × 0.3
     *
     * 신뢰 요소:
     * - 클레임 패널티: max(0, 1.0 - complaintCount × 0.15)
     * - 긍정 리뷰 비율 보너스
     */
    private double calculateTrustScore(double avgRating, int reviewCount, int complaintCount,
                                       int viewCount, int favoriteCount, int positiveReviews, int negativeReviews) {
        // 리뷰가 없는 경우 기본 점수
        if (reviewCount == 0 && avgRating == 0) {
            return 0.5; // 중립 점수
        }

        // 평균 평점 점수 (0~1)
        double ratingScore = Math.min(avgRating / 5.0, 1.0);

        // 리뷰 수 보너스 (리뷰가 많을수록 신뢰도 상승, 최대 20개까지)
        double reviewCountBonus = Math.min(1.0, reviewCount / 20.0);

        // 리뷰 품질 점수 = 평점 70% + 리뷰 수 30%
        double reviewQualityScore = ratingScore * 0.7 + reviewCountBonus * 0.3;

        // 클레임 패널티 (클레임 1건당 15% 감점)
        double claimPenalty = Math.max(0, 1.0 - complaintCount * 0.15);

        // 긍정 리뷰 비율 보너스
        double positiveRatio = 1.0;
        if (positiveReviews + negativeReviews > 0) {
            positiveRatio = (double) positiveReviews / (positiveReviews + negativeReviews);
        }

        // 즐겨찾기 보너스 (최대 10%)
        double favoriteBonus = Math.min(0.1, favoriteCount * 0.01);

        // 신뢰 요소 점수
        double trustFactor = claimPenalty * 0.6 + positiveRatio * 0.3 + favoriteBonus + 0.1;
        trustFactor = Math.min(1.0, trustFactor);

        // 최종 신뢰 점수: 리뷰 품질 60% + 신뢰 요소 40%
        double trustScore = 0.6 * reviewQualityScore + 0.4 * trustFactor;

        // 0~1 범위 제한 및 소수점 2자리 반올림
        trustScore = Math.max(0.0, Math.min(1.0, trustScore));
        return Math.round(trustScore * 100) / 100.0;
    }

    /**
     * 거래 후 신뢰 점수 업데이트 (EMA 방식)
     */
    public TrustScoreResponse updateTrustScore(TrustScoreUpdateRequest request) {
        String storeId = request.getStoreId();
        TrustScoreResponse currentScore = getTrustScore(storeId);
        double previousScore = currentScore.getTrustScore();

        // 가격 일치 판정
        boolean priceMatched = Math.abs(request.getActualInstallmentPrice() - request.getSnapshotInstallmentPrice()) < PRICE_EPSILON;
        double priceMatchValue = priceMatched ? 1.0 : 0.0;

        // 조건 일치
        double conditionMatchValue = request.isConditionMatched() ? 1.0 : 0.0;

        // 리뷰 점수
        double reviewScore = request.getReviewRating() != null
                ? request.getReviewRating() / 5.0
                : 0.5;

        // Delta 계산
        double delta = 0.5 * priceMatchValue + 0.3 * conditionMatchValue + 0.2 * reviewScore - 0.5;

        // EMA 업데이트
        double newScore = ALPHA * previousScore + (1 - ALPHA) * delta;
        newScore = Math.max(0.0, Math.min(1.0, newScore));

        // 히스토리 기록
        recordHistory(storeId, request.getTransactionId(), previousScore, newScore,
                priceMatched, request.isConditionMatched(), request.getReviewRating());

        log.info("Trust score updated for store {}: {} -> {} (delta: {})",
                storeId, previousScore, newScore, delta);

        // 업데이트된 점수로 응답 생성
        return TrustScoreResponse.builder()
                .storeId(storeId)
                .storeName(currentScore.getStoreName())
                .trustScore(newScore)
                .trustFloor(scoreToFloor(newScore))
                .priceMatchRate(currentScore.getPriceMatchRate())
                .conditionMatchRate(currentScore.getConditionMatchRate())
                .avgRating(currentScore.getAvgRating())
                .reviewCount(currentScore.getReviewCount())
                .claimCount(currentScore.getClaimCount())
                .visitCount(currentScore.getVisitCount())
                .lastUpdatedAt(LocalDateTime.now())
                .grade(calculateGrade(newScore))
                .build();
    }

    /**
     * 매장 신뢰 점수 히스토리 조회
     */
    public TrustScoreHistoryResponse getTrustScoreHistory(String storeId) {
        TrustScoreResponse currentScore = getTrustScore(storeId);
        List<TrustScoreHistoryResponse.TrustScoreHistoryItem> history =
                historyMap.getOrDefault(storeId, new ArrayList<>());

        return TrustScoreHistoryResponse.builder()
                .storeId(storeId)
                .storeName(currentScore.getStoreName())
                .currentTrustScore(currentScore.getTrustScore())
                .currentTrustFloor(currentScore.getTrustFloor())
                .history(history)
                .build();
    }

    /**
     * 내부 점수(0~1)를 층(1~100)으로 변환
     */
    private int scoreToFloor(double score) {
        return (int) Math.round(score * 99) + 1;
    }

    /**
     * 신뢰 등급 계산
     */
    private TrustScoreResponse.TrustGrade calculateGrade(double trustScore) {
        if (trustScore >= 0.7) return TrustScoreResponse.TrustGrade.EXCELLENT;
        if (trustScore >= 0.5) return TrustScoreResponse.TrustGrade.GOOD;
        if (trustScore >= 0.3) return TrustScoreResponse.TrustGrade.NORMAL;
        if (trustScore >= 0.2) return TrustScoreResponse.TrustGrade.WARNING;
        return TrustScoreResponse.TrustGrade.POOR;
    }

    /**
     * 히스토리 기록
     */
    private void recordHistory(String storeId, String transactionId, double previousScore,
                               double newScore, boolean priceMatched, boolean conditionMatched,
                               Integer reviewRating) {
        TrustScoreHistoryResponse.TrustScoreHistoryItem item =
                TrustScoreHistoryResponse.TrustScoreHistoryItem.builder()
                        .transactionId(transactionId)
                        .previousScore(previousScore)
                        .newScore(newScore)
                        .scoreDelta(newScore - previousScore)
                        .priceMatched(priceMatched)
                        .conditionMatched(conditionMatched)
                        .reviewRating(reviewRating)
                        .recordedAt(LocalDateTime.now())
                        .build();

        historyMap.computeIfAbsent(storeId, k -> new ArrayList<>()).add(item);
    }

    /**
     * 매장을 찾지 못한 경우 기본 응답
     */
    private TrustScoreResponse createNotFoundResponse(String storeId) {
        return TrustScoreResponse.builder()
                .storeId(storeId)
                .storeName("매장 " + storeId)
                .trustScore(0.5)
                .trustFloor(50)
                .priceMatchRate(0.0)
                .conditionMatchRate(0.0)
                .avgRating(0.0)
                .reviewCount(0)
                .claimCount(0)
                .visitCount(0)
                .lastUpdatedAt(LocalDateTime.now())
                .grade(TrustScoreResponse.TrustGrade.GOOD)
                .build();
    }

    /**
     * Map에서 int 값 추출
     */
    private int getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Map에서 double 값 추출
     */
    private double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
