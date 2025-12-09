package com.nofee.api.test.carrierintegration.util;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * JsonNode 값 추출 유틸리티
 *
 * 각 통신사 서비스에서 중복 사용되던 JSON 파싱 로직을 통합
 */
public final class JsonNodeUtils {

    private JsonNodeUtils() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }

    /**
     * JsonNode에서 문자열 값 추출
     *
     * @param node  JSON 노드
     * @param field 필드명
     * @return 문자열 값 또는 null
     */
    public static String getTextValue(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode fieldNode = node.get(field);
        return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asText() : null;
    }

    /**
     * JsonNode에서 정수 값 추출 (숫자가 아닌 문자 제거 후 파싱)
     *
     * @param node  JSON 노드
     * @param field 필드명
     * @return 정수 값 또는 null
     */
    public static Integer getIntValue(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode fieldNode = node.get(field);
        if (fieldNode != null && !fieldNode.isNull()) {
            try {
                String value = fieldNode.asText().replaceAll("[^0-9]", "");
                return value.isEmpty() ? null : Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * JsonNode에서 Long 값 추출 (숫자가 아닌 문자 제거 후 파싱)
     *
     * @param node  JSON 노드
     * @param field 필드명
     * @return Long 값 또는 null
     */
    public static Long getLongValue(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode fieldNode = node.get(field);
        if (fieldNode != null && !fieldNode.isNull()) {
            try {
                String value = fieldNode.asText().replaceAll("[^0-9]", "");
                return value.isEmpty() ? null : Long.parseLong(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
