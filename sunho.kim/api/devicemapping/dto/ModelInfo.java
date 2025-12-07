package com.nofee.api.test.devicemapping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 모델 정보 추출 결과 (정확한 매칭을 위해)
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

    /**
     * 상품명에서 모델 정보 추출
     */
    public static ModelInfo extract(String name) {
        if (name == null) {
            return ModelInfo.builder().brand(Brand.OTHER).build();
        }

        String lower = name.toLowerCase().replaceAll("\\s+", " ").trim();

        // 갤럭시 시리즈
        if (lower.matches(".*(?:갤럭시|galaxy).*")) {
            return extractGalaxy(lower);
        }

        // 아이폰 시리즈
        if (lower.matches(".*(?:아이폰|iphone).*")) {
            return extractIphone(lower);
        }

        return ModelInfo.builder().brand(Brand.OTHER).build();
    }

    private static ModelInfo extractGalaxy(String lower) {
        // Z 폴드
        Pattern foldPattern = Pattern.compile("(?:z\\s*)?폴드\\s*(\\d+)|z?\\s*fold\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher foldMatcher = foldPattern.matcher(lower);
        if (foldMatcher.find()) {
            String num = foldMatcher.group(1) != null ? foldMatcher.group(1) : foldMatcher.group(2);
            return ModelInfo.builder().brand(Brand.GALAXY).series("Fold" + (num != null ? num : "")).build();
        }

        // Z 플립
        Pattern flipPattern = Pattern.compile("(?:z\\s*)?플립\\s*(\\d+)|z?\\s*flip\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher flipMatcher = flipPattern.matcher(lower);
        if (flipMatcher.find()) {
            String num = flipMatcher.group(1) != null ? flipMatcher.group(1) : flipMatcher.group(2);
            return ModelInfo.builder().brand(Brand.GALAXY).series("Flip" + (num != null ? num : "")).build();
        }

        // 퀀텀 시리즈
        Pattern quantumPattern = Pattern.compile("퀀텀\\s*(\\d+)|quantum\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher quantumMatcher = quantumPattern.matcher(lower);
        if (quantumMatcher.find()) {
            String num = quantumMatcher.group(1) != null ? quantumMatcher.group(1) : quantumMatcher.group(2);
            return ModelInfo.builder().brand(Brand.GALAXY).series("Quantum" + num).build();
        }

        // A 시리즈
        Pattern aPattern = Pattern.compile("a\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher aMatcher = aPattern.matcher(lower);
        if (aMatcher.find()) {
            return ModelInfo.builder().brand(Brand.GALAXY).series("A" + aMatcher.group(1)).build();
        }

        // S 시리즈 + 변형
        Pattern sPattern = Pattern.compile("s\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher sMatcher = sPattern.matcher(lower);
        if (sMatcher.find()) {
            String num = sMatcher.group(1);
            String variant = null;
            if (lower.matches(".*(?:울트라|ultra).*")) variant = "Ultra";
            else if (lower.matches(".*(?:엣지|edge).*")) variant = "Edge";
            else if (lower.matches(".*(?:플러스|\\+|plus).*")) variant = "+";
            else if (lower.matches(".*fe.*")) variant = "FE";
            return ModelInfo.builder().brand(Brand.GALAXY).series("S" + num).variant(variant).build();
        }

        // 점프 시리즈
        Pattern jumpPattern = Pattern.compile("점프\\s*(\\d+)|jump\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher jumpMatcher = jumpPattern.matcher(lower);
        if (jumpMatcher.find()) {
            String num = jumpMatcher.group(1) != null ? jumpMatcher.group(1) : jumpMatcher.group(2);
            return ModelInfo.builder().brand(Brand.GALAXY).series("Jump" + num).build();
        }

        // 버디 시리즈
        Pattern buddyPattern = Pattern.compile("버디\\s*(\\d+)|buddy\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher buddyMatcher = buddyPattern.matcher(lower);
        if (buddyMatcher.find()) {
            String num = buddyMatcher.group(1) != null ? buddyMatcher.group(1) : buddyMatcher.group(2);
            return ModelInfo.builder().brand(Brand.GALAXY).series("Buddy" + num).build();
        }

        // M 시리즈
        Pattern mPattern = Pattern.compile("m\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher mMatcher = mPattern.matcher(lower);
        if (mMatcher.find()) {
            return ModelInfo.builder().brand(Brand.GALAXY).series("M" + mMatcher.group(1)).build();
        }

        return ModelInfo.builder().brand(Brand.GALAXY).build();
    }

    private static ModelInfo extractIphone(String lower) {
        String variant = null;
        String series = null;

        // Air
        if (lower.matches(".*(?:에어|air).*")) {
            variant = "Air";
            series = "Air";
        }
        // Pro Max
        else if (lower.matches(".*(?:프로\\s*맥스|pro\\s*max).*")) {
            variant = "ProMax";
        }
        // Pro
        else if (lower.matches(".*(?:프로|pro).*")) {
            variant = "Pro";
        }
        // Plus
        else if (lower.matches(".*(?:플러스|\\+|plus).*")) {
            variant = "+";
        }
        // Mini
        else if (lower.matches(".*(?:미니|mini).*")) {
            variant = "Mini";
        }
        // SE
        else if (lower.matches(".*se.*")) {
            variant = "SE";
            series = "SE";
        }
        // e 모델
        else if (lower.matches(".*\\d+\\s*e\\b.*")) {
            variant = "e";
        }

        // 숫자 추출 (Air, SE가 아닌 경우)
        if (series == null) {
            Pattern numPattern = Pattern.compile("(\\d+)");
            Matcher numMatcher = numPattern.matcher(lower);
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
