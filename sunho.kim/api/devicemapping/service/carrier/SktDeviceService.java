package com.nofee.api.test.devicemapping.service.carrier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nofee.api.test.devicemapping.dto.CarrierDevice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SKT 기기 리스트 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SktDeviceService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private static final String PLAN_CODE = "NA00007790";  // 5GX 프라임 89,000원
    private static final String JOIN_TYPE_CODE = "20";     // 번호이동

    public List<CarrierDevice> fetchDevices() {
        log.info("📱 SKT 기기 리스트 조회 중...");
        List<CarrierDevice> devices = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();

        try {
            WebClient webClient = webClientBuilder.build();

            String noticeUrl = String.format(
                "https://shop.tworld.co.kr/notice?modelNwType=5G&scrbTypCd=%s&prodId=%s&saleYn=Y",
                JOIN_TYPE_CODE, PLAN_CODE);

            String html = webClient.get()
                .uri(noticeUrl)
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .header("Accept", "text/html")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            // parseObject([...]) 패턴 찾기
            String startPattern = "_this.products = parseObject([";
            int startIdx = html.indexOf(startPattern);

            if (startIdx == -1) {
                log.error("[SKT] Could not find parseObject pattern");
                return devices;
            }

            int arrayStart = startIdx + startPattern.length();

            // 괄호 카운팅으로 배열 끝 찾기
            int depth = 1;
            int endIdx = arrayStart;
            for (int i = arrayStart; i < html.length() && depth > 0; i++) {
                if (html.charAt(i) == '[') depth++;
                else if (html.charAt(i) == ']') depth--;
                endIdx = i;
            }

            String content = html.substring(arrayStart, endIdx);
            JsonNode items = objectMapper.readTree("[" + content + "]");

            for (JsonNode item : items) {
                String code = item.path("modelCd").asText("");
                if (code.isEmpty() || seenCodes.contains(code)) continue;
                seenCodes.add(code);

                String productNm = item.path("productNm").asText("");
                String productMem = item.path("productMem").asText("");
                String deviceName = (productNm + " " + productMem).trim();

                devices.add(CarrierDevice.builder()
                    .carrier(CarrierDevice.Carrier.SKT)
                    .deviceCode(code)
                    .deviceName(deviceName)
                    .storage(productMem)
                    .build());
            }

            log.info("✅ SKT {}개 기기 조회 완료", devices.size());
        } catch (Exception e) {
            log.error("❌ SKT API 오류: {}", e.getMessage());
        }

        return devices;
    }
}
