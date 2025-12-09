package com.nofee.api.test.devicemapping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 모델 정보 추출 결과 (정확한 매칭을 위해)
 *
 * 성능 최적화: Regex 패턴을 static final로 프리컴파일하여
 * 매 호출마다 패턴을 재컴파일하는 오버헤드를 제거합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelInfo {

    public enum Brand {
        GALAXY, IPHONE, OTHER
    }

    private Brand brand;
    private String series;   // S25, A55, 16, Z Fold 등
    private String variant;  // 울트라, 프로, 맥스, 플러스, 엣지, FE, 에어 등

    // ==================== 프리컴파일된 Regex 패턴 ====================

    // 브랜드 감지용 패턴
    private static final Pattern GALAXY_PATTERN = Pattern.compile(".*(?:갤럭시|galaxy).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern IPHONE_PATTERN = Pattern.compile(".*(?:아이폰|iphone).*", Pattern.CASE_INSENSITIVE);

    // 갤럭시 시리즈 패턴
    private static final Pattern FOLD_PATTERN = Pattern.compile("(?:z\\s*)?폴드\\s*(\\d+)|z?\\s*fold\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLIP_PATTERN = Pattern.compile("(?:z\\s*)?플립\\s*(\\d+)|z?\\s*flip\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern QUANTUM_PATTERN = Pattern.compile("퀀텀\\s*(\\d+)|quantum\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern A_SERIES_PATTERN = Pattern.compile("a\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern S_SERIES_PATTERN = Pattern.compile("s\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JUMP_PATTERN = Pattern.compile("점프\\s*(\\d+)|jump\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BUDDY_PATTERN = Pattern.compile("버디\\s*(\\d+)|buddy\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern M_SERIES_PATTERN = Pattern.compile("m\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    // 갤럭시 S 시리즈 변형 패턴
    private static final Pattern ULTRA_PATTERN = Pattern.compile(".*(?:울트라|ultra).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern EDGE_PATTERN = Pattern.compile(".*(?:엣지|edge).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLUS_PATTERN = Pattern.compile(".*(?:플러스|\\+|plus).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern FE_PATTERN = Pattern.compile(".*fe.*", Pattern.CASE_INSENSITIVE);

    // 아이폰 변형 패턴
    private static final Pattern AIR_PATTERN = Pattern.compile(".*(?:에어|air).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRO_MAX_PATTERN = Pattern.compile(".*(?:프로\\s*맥스|pro\\s*max).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRO_PATTERN = Pattern.compile(".*(?:프로|pro).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern MINI_PATTERN = Pattern.compile(".*(?:미니|mini).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SE_PATTERN = Pattern.compile(".*se.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern E_MODEL_PATTERN = Pattern.compile(".*\\d+\\s*e\\b.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

    /**
     * 상품명에서 모델 정보 추출
     */
    public static ModelInfo extract(String name) {
        if (name == null) {
            return ModelInfo.builder().brand(Brand.OTHER).build();
        }

        String lower = name.toLowerCase().replaceAll("\\s+", " ").trim();

        // 갤럭시 시리즈
        if (GALAXY_PATTERN.matcher(lower).matches()) {
            return extractGalaxy(lower);
        }

        // 아이폰 시리즈
        if (IPHONE_PATTERN.matcher(lower).matches()) {
            return extractIphone(lower);
        }

        return ModelInfo.builder().brand(Brand.OTHER).build();
    }

    private static ModelInfo extractGalaxy(String lower) {
        // Z 폴드
        Matcher foldMatcher = FOLD_PATTERN.matcher(lower);
        if (foldMatcher.find()) {
            String num = foldMatcher.group(1) != null ? foldMatcher.group(1) : foldMatcher.group(2);
            return ModelInfo.builder().brand(Brand.GALAXY).series("Fold" + (num != null ? num : "")).build();
        }

        // Z 플립
        Matcher flipMatcher = FLIP_PATTERN.matcher(lower);
        if (flipMatcher.find()) {
            String num = flipMatcher.group(1) != null ? flipMatcher.group(1) : flipMatcher.group(2);
            return ModelInfo.builder().brand(Brand.GALAXY).series("Flip" + (num != null ? num : "")).build();
        }

        // 퀀텀 시리즈
        Matcher quantumMatcher = QUANTUM_PATTERN.matcher(lower);
        if (quantumMatcher.find()) {
            String num = quantumMatcher.group(1) != null ? quantumMatcher.group(1) : quantumMatcher.group(2);
            return ModelInfo.builder().brand(Brand.GALAXY).series("Quantum" + num).build();
        }

        // A 시리즈
        Matcher aMatcher = A_SERIES_PATTERN.matcher(lower);
        if (aMatcher.find()) {
            return ModelInfo.builder().brand(Brand.GALAXY).series("A" + aMatcher.group(1)).build();
        }

        // S 시리즈 + 변형
        Matcher sMatcher = S_SERIES_PATTERN.matcher(lower);
        if (sMatcher.find()) {
            String num = sMatcher.group(1);
            String variant = null;
            if (ULTRA_PATTERN.matcher(lower).matches()) variant = "Ultra";
            else if (EDGE_PATTERN.matcher(lower).matches()) variant = "Edge";
            else if (PLUS_PATTERN.matcher(lower).matches()) variant = "+";
            else if (FE_PATTERN.matcher(lower).matches()) variant = "FE";
            return ModelInfo.builder().brand(Brand.GALAXY).series("S" + num).variant(variant).build();
        }

        // 점프 시리즈
        Matcher jumpMatcher = JUMP_PATTERN.matcher(lower);
        if (jumpMatcher.find()) {
            String num = jumpMatcher.group(1) != null ? jumpMatcher.group(1) : jumpMatcher.group(2);
            return ModelInfo.builder().brand(Brand.GALAXY).series("Jump" + num).build();
        }

        // 버디 시리즈
        Matcher buddyMatcher = BUDDY_PATTERN.matcher(lower);
        if (buddyMatcher.find()) {
            String num = buddyMatcher.group(1) != null ? buddyMatcher.group(1) : buddyMatcher.group(2);
            return ModelInfo.builder().brand(Brand.GALAXY).series("Buddy" + num).build();
        }

        // M 시리즈
        Matcher mMatcher = M_SERIES_PATTERN.matcher(lower);
        if (mMatcher.find()) {
            return ModelInfo.builder().brand(Brand.GALAXY).series("M" + mMatcher.group(1)).build();
        }

        return ModelInfo.builder().brand(Brand.GALAXY).build();
    }

    private static ModelInfo extractIphone(String lower) {
        String variant = null;
        String series = null;

        // Air
        if (AIR_PATTERN.matcher(lower).matches()) {
            variant = "Air";
            series = "Air";
        }
        // Pro Max
        else if (PRO_MAX_PATTERN.matcher(lower).matches()) {
            variant = "ProMax";
        }
        // Pro
        else if (PRO_PATTERN.matcher(lower).matches()) {
            variant = "Pro";
        }
        // Plus
        else if (PLUS_PATTERN.matcher(lower).matches()) {
            variant = "+";
        }
        // Mini
        else if (MINI_PATTERN.matcher(lower).matches()) {
            variant = "Mini";
        }
        // SE
        else if (SE_PATTERN.matcher(lower).matches()) {
            variant = "SE";
            series = "SE";
        }
        // e 모델
        else if (E_MODEL_PATTERN.matcher(lower).matches()) {
            variant = "e";
        }

        // 숫자 추출 (Air, SE가 아닌 경우)
        if (series == null) {
            Matcher numMatcher = NUMBER_PATTERN.matcher(lower);
            if (numMatcher.find()) {
                series = numMatcher.group(1);
            }
        }

        return ModelInfo.builder().brand(Brand.IPHONE).series(series).variant(variant).build();
    }

    /**
     * 정확한 모델 매칭 확인
     */
    public boolean isExactMatch(ModelInfo other) {
        if (other == null) return false;
        if (this.brand != other.brand) return false;

        // 시리즈가 다르면 불일치
        if (this.series == null && other.series != null) return false;
        if (this.series != null && !this.series.equals(other.series)) return false;

        // 변형 체크
        if (this.variant == null && other.variant == null) return true;
        if (this.variant == null || other.variant == null) return false;
        return this.variant.equals(other.variant);
    }
}
