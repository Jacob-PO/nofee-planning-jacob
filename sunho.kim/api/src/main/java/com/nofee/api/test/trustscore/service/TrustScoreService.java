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
 * 신뢰 높이(Trust Score) 서비스 - 실제 DB 데이터 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrustScoreService {

    private final TrustScoreMapper trustScoreMapper;

    private static final double ALPHA = 0.95;
    private static final int PRICE_EPSILON = 1000;

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
                String storeIdStr = String.valueOf(storeNo);
                Map<String, Object> reviewStats = trustScoreMapper.selectReviewStats(storeNo);
                results.add(buildTrustScoreResponse(storeIdStr, storeInfo, reviewStats));
            }

            // DB에 없는 매장 처리
            for (String sid : storeIds) {
                boolean found = results.stream().anyMatch(r -> r.getStoreId().equals(sid));
                if (!found) {
                    results.add(createNotFoundResponse(sid));
                }
            }
        } catch (Exception e) {
            log.error("Error fetching trust scores: {}", e.getMessage());
            List<TrustScoreResponse> fallback = new ArrayList<>();
            for (String sid : storeIds) {
                fallback.add(getTrustScore(sid));
            }
            return fallback;
        }

        return results;
    }

    private TrustScoreResponse buildTrustScoreResponse(String storeId, Map<String, Object> storeInfo, Map<String, Object> reviewStats) {
        String storeName = (String) storeInfo.get("storeName");
        int reviewCount = getIntValue(storeInfo, "reviewCount");
        double avgRating = getDoubleValue(storeInfo, "avgRating");
        int viewCount = getIntValue(storeInfo, "viewCount");
        int favoriteCount = getIntValue(storeInfo, "favoriteCount");
        int complaintCount = getIntValue(storeInfo, "complaintCount");

        int totalReviews = reviewStats != null ? getIntValue(reviewStats, "totalReviews") : reviewCount;
        double calculatedAvgRating = reviewStats != null ? getDoubleValue(reviewStats, "avgRating") : avgRating;
        int positiveReviews = reviewStats != null ? getIntValue(reviewStats, "positiveReviews") : 0;
        int negativeReviews = reviewStats != null ? getIntValue(reviewStats, "negativeReviews") : 0;

        if (calculatedAvgRating > 0) {
            avgRating = calculatedAvgRating;
        }
        if (totalReviews > reviewCount) {
            reviewCount = totalReviews;
        }

        double trustScore = calculateTrustScore(avgRating, reviewCount, complaintCount, viewCount, favoriteCount, positiveReviews, negativeReviews);

        return TrustScoreResponse.builder()
                .storeId(storeId)
                .storeName(storeName != null ? storeName : "매장 " + storeId)
                .trustScore(trustScore)
                .trustFloor(scoreToFloor(trustScore))
                .priceMatchRate(0.85)
                .conditionMatchRate(0.90)
                .avgRating(avgRating)
                .reviewCount(reviewCount)
                .claimCount(complaintCount)
                .visitCount(viewCount)
                .lastUpdatedAt(LocalDateTime.now())
                .grade(calculateGrade(trustScore))
                .build();
    }

    /**
     * 실제 데이터 기반 신뢰 점수 계산
     * T = 0.6 × (리뷰 품질 점수) + 0.4 × (신뢰 요소)
     */
    private double calculateTrustScore(double avgRating, int reviewCount, int complaintCount,
                                       int viewCount, int favoriteCount, int positiveReviews, int negativeReviews) {
        if (reviewCount == 0 && avgRating == 0) {
            return 0.5;
        }

        double ratingScore = Math.min(avgRating / 5.0, 1.0);
        double reviewCountBonus = Math.min(1.0, reviewCount / 20.0);
        double reviewQualityScore = ratingScore * 0.7 + reviewCountBonus * 0.3;

        double claimPenalty = Math.max(0, 1.0 - complaintCount * 0.15);
        double positiveRatio = 1.0;
        if (positiveReviews + negativeReviews > 0) {
            positiveRatio = (double) positiveReviews / (positiveReviews + negativeReviews);
        }
        double favoriteBonus = Math.min(0.1, favoriteCount * 0.01);

        double trustFactor = claimPenalty * 0.6 + positiveRatio * 0.3 + favoriteBonus + 0.1;
        trustFactor = Math.min(1.0, trustFactor);

        double trustScore = 0.6 * reviewQualityScore + 0.4 * trustFactor;
        trustScore = Math.max(0.0, Math.min(1.0, trustScore));
        return Math.round(trustScore * 100) / 100.0;
    }

    public TrustScoreResponse updateTrustScore(TrustScoreUpdateRequest request) {
        String storeId = request.getStoreId();
        TrustScoreResponse currentScore = getTrustScore(storeId);
        double previousScore = currentScore.getTrustScore();

        boolean priceMatched = Math.abs(request.getActualInstallmentPrice() - request.getSnapshotInstallmentPrice()) < PRICE_EPSILON;
        double priceMatchValue = priceMatched ? 1.0 : 0.0;
        double conditionMatchValue = request.isConditionMatched() ? 1.0 : 0.0;
        double reviewScore = request.getReviewRating() != null ? request.getReviewRating() / 5.0 : 0.5;

        double delta = 0.5 * priceMatchValue + 0.3 * conditionMatchValue + 0.2 * reviewScore - 0.5;
        double newScore = ALPHA * previousScore + (1 - ALPHA) * delta;
        newScore = Math.max(0.0, Math.min(1.0, newScore));

        recordHistory(storeId, request.getTransactionId(), previousScore, newScore,
                priceMatched, request.isConditionMatched(), request.getReviewRating());

        log.info("Trust score updated for store {}: {} -> {} (delta: {})", storeId, previousScore, newScore, delta);

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

    private int scoreToFloor(double score) {
        return (int) Math.round(score * 99) + 1;
    }

    private TrustScoreResponse.TrustGrade calculateGrade(double trustScore) {
        if (trustScore >= 0.7) return TrustScoreResponse.TrustGrade.EXCELLENT;
        if (trustScore >= 0.5) return TrustScoreResponse.TrustGrade.GOOD;
        if (trustScore >= 0.3) return TrustScoreResponse.TrustGrade.NORMAL;
        if (trustScore >= 0.2) return TrustScoreResponse.TrustGrade.WARNING;
        return TrustScoreResponse.TrustGrade.POOR;
    }

    private void recordHistory(String storeId, String transactionId, double previousScore,
                               double newScore, boolean priceMatched, boolean conditionMatched, Integer reviewRating) {
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

    private int getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); }
        catch (NumberFormatException e) { return 0; }
    }

    private double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(value.toString()); }
        catch (NumberFormatException e) { return 0.0; }
    }
}
