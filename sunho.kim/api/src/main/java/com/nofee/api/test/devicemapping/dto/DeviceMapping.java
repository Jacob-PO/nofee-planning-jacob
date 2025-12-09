package com.nofee.api.test.devicemapping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 노피 상품 - 통신사 기기 매핑 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceMapping {

    private String nofeeProductCode;
    private String nofeeProductName;

    private String sktDeviceCode;
    private String sktDeviceName;

    private String ktDeviceCode;
    private String ktDeviceName;

    private String lguDeviceCode;
    private String lguDeviceName;

    private String mappedAt;
    private String confidence;  // high, medium, low
}
