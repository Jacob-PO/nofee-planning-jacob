package com.nofee.api.test.devicemapping.service.carrier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nofee.api.test.devicemapping.dto.CarrierDevice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

/**
 * LGU+ 기기 리스트 조회 서비스
 *
 * API: /uhdc/fo/prdv/mdlbsufu/v2/mdlb-sufu-list
 * - 인증 불필요
 * - rowSize=1000으로 전체 데이터 한 번에 조회 가능
 * - shwd 파라미터로 키워드 검색 지원
 *
 * @see <a href="https://www.lguplus.com/mobile/financing-model">LGU+ 공시지원금</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LguDeviceService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    // 5G 프리미어 에센셜 85,000원 (기본 요금제)
    private static final String DEFAULT_PLAN_CODE = "LPZ0000409";

    /**
     * LGU+ 기기 목록 조회
     *
     * @param planCode 요금제 코드 (기본: LPZ0000409)
     * @param joinTypeCode 개통유형 코드 (1=기변, 2=번이, 3=신규)
     * @return 기기 목록
     */
    public List<CarrierDevice> fetchDevices(String planCode, String joinTypeCode) {
        log.info("📱 LGU+ 기기 리스트 조회 중... (요금제: {}, 가입유형: {})", planCode, joinTypeCode);
        List<CarrierDevice> devices = new ArrayList<>();

        try {
            WebClient webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();

            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .scheme("https")
                    .host("www.lguplus.com")
                    .path("/uhdc/fo/prdv/mdlbsufu/v2/mdlb-sufu-list")
                    .queryParam("urcMblPpCd", planCode)
                    .queryParam("urcHphnEntrPsblKdCd", joinTypeCode)
                    .queryParam("rowSize", "1000")
                    .queryParam("sortOrd", "00")  // 지원금 높은 순
                    .build())
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse -> {
                    log.error("❌ LGU+ API 응답 에러: {}", clientResponse.statusCode());
                    return clientResponse.bodyToMono(String.class)
                        .map(body -> new RuntimeException("LGU+ API 에러: " + body));
                })
                .bodyToMono(String.class)
                .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.path("dvicMdlbSufuDtoList");

            if (items.isArray()) {
                for (JsonNode item : items) {
                    String deviceCode = item.path("urcTrmMdlCd").asText("");
                    String deviceName = item.path("urcTrmMdlNm").asText("");
                    String deviceType = item.path("urcTrmKndEngNm").asText("");

                    // 핸드폰만 필터링 (태블릿, 워치 제외)
                    if (!deviceType.isEmpty() && !deviceType.equals("5g-phone") && !deviceType.equals("4g-phone")) {
                        continue;
                    }

                    devices.add(CarrierDevice.builder()
                        .carrier(CarrierDevice.Carrier.LGU)
                        .deviceCode(deviceCode)
                        .deviceName(deviceName)
                        .storage(CarrierDevice.extractStorage(deviceName))
                        .build());
                }
            }

            log.info("✅ LGU+ {}개 기기 조회 완료", devices.size());
        } catch (Exception e) {
            log.error("❌ LGU+ API 오류: {}", e.getMessage());
        }

        return devices;
    }

    /**
     * 기본 설정으로 기기 목록 조회 (번호이동 기준)
     */
    public List<CarrierDevice> fetchDevices() {
        return fetchDevices(DEFAULT_PLAN_CODE, "2");  // 번호이동
    }
}
