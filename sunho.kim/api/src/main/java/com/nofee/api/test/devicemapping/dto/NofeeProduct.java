package com.nofee.api.test.devicemapping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 노피 상품 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NofeeProduct {
    private String productGroupCode;
    private String productGroupNm;
    private String manufacturerCode;
}
