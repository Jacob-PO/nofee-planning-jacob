package com.nofee.api.test.devicemapping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 통신사 기기 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarrierDevice {

    public enum Carrier {
        SKT, KT, LGU
    }

    private Carrier carrier;
    private String deviceCode;
    private String deviceName;
    private String storage;

    /**
     * 기기명에서 저장용량 추출
     */
    public static String extractStorage(String deviceName) {
        if (deviceName == null) return "";

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*(GB|TB|G\\b)",
            java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(deviceName);

        if (matcher.find()) {
            String num = matcher.group(1);
            String unit = matcher.group(2).toUpperCase();
            if (unit.equals("G")) unit = "GB";
            return num + unit;
        }
        return "";
    }

    /**
     * 저장용량을 GB 단위 숫자로 변환 (비교용)
     */
    public static int parseStorageToGB(String storage) {
        if (storage == null || storage.isEmpty()) return Integer.MAX_VALUE;

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*(GB|TB|G\\b)",
            java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(storage);

        if (matcher.find()) {
            int num = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2).toUpperCase();
            if (unit.equals("TB")) return num * 1024;
            return num;
        }
        return Integer.MAX_VALUE;
    }
}
