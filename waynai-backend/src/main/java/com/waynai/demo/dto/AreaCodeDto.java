package com.waynai.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 지역 코드 정보 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaCodeDto {
    
    /**
     * 지역 코드 (2자리)
     */
    private String areaCode;
    
    /**
     * 지역명
     */
    private String areaName;
    
    /**
     * 시군구 코드 (5자리)
     */
    private String sigunguCode;
    
    /**
     * 시군구명
     */
    private String sigunguName;
}
