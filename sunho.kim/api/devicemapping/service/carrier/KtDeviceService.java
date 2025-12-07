package com.nofee.api.test.devicemapping.service.carrier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nofee.api.test.devicemapping.dto.CarrierDevice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * KT 기기 리스트 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KtDeviceService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private static final int MAX_PAGES = 10;  // 최대 10페이지 = 120개

    public List<CarrierDevice> fetchDevices() {
        log.info("📱 KT 기기 리스트 조회 중...");
        List<CarrierDevice> devices = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();

        try {
            WebClient webClient = webClientBuilder.build();

            // 1. 세션 쿠키 획득
            String sessionResponse = webClient.get()
                .uri("https://shop.kt.com/smart/supportAmtList.do?channel=VS")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .header("Accept", "text/html")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            // 2. 여러 페이지 조회
            int pageNo = 1;

            while (pageNo <= MAX_PAGES) {
                String formData = String.format(
                    "prodNm=mobile&prdcCd=PL244N945&prodType=30&deviceType=HDP&makrCd=&sortProd=oBspnsrPunoDateDesc&spnsMonsType=2&dscnOptnCd=MT&sbscTypeCd=02&pageNo=%d",
                    pageNo);

                String response = webClient.post()
                    .uri("https://shop.kt.com/mobile/retvSuFuList.json")
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .header("Referer", "https://shop.kt.com/smart/supportAmtList.do?channel=VS")
                    .body(BodyInserters.fromValue(formData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

                JsonNode root = objectMapper.readTree(response);
                JsonNode items = root.path("LIST_DATA");

                if (!items.isArray() || items.size() == 0) break;

                for (JsonNode item : items) {
                    String code = item.path("hndsetModelNm").asText("");
                    if (code.isEmpty() || seenCodes.contains(code)) continue;
                    seenCodes.add(code);

                    String deviceName = item.path("petNm").asText("");

                    devices.add(CarrierDevice.builder()
                        .carrier(CarrierDevice.Carrier.KT)
                        .deviceCode(code)
                        .deviceName(deviceName)
                        .storage(CarrierDevice.extractStorage(deviceName))
                        .build());
                }

                // 마지막 페이지 확인
                JsonNode pageInfo = root.path("pageInfoBean");
                int totalPageCount = pageInfo.path("totalPageCount").asInt(1);
                if (pageNo >= totalPageCount) break;

                pageNo++;
                Thread.sleep(200);  // Rate limit
            }

            log.info("✅ KT {}개 기기 조회 완료", devices.size());
        } catch (Exception e) {
            log.error("❌ KT API 오류: {}", e.getMessage());
        }

        return devices;
    }
}
