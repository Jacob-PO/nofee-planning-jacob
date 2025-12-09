package com.nofee.api.test.trustscore.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 신뢰 점수 계산을 위한 DB Mapper
 */
@Mapper
public interface TrustScoreMapper {

    /**
     * 매장 기본 정보 조회 (tb_store)
     * - review: 리뷰 수
     * - review_avg: 평균 평점
     * - complaint: 신고 수
     * - view: 조회수
     */
    Map<String, Object> selectStoreInfo(@Param("storeNo") Integer storeNo);

    /**
     * 여러 매장 기본 정보 일괄 조회
     */
    List<Map<String, Object>> selectStoreInfoList(@Param("storeNos") List<Integer> storeNos);

    /**
     * 매장 리뷰 통계 조회 (tb_review_store_phone)
     * - 평균 평점
     * - 리뷰 수
     */
    Map<String, Object> selectReviewStats(@Param("storeNo") Integer storeNo);
}
