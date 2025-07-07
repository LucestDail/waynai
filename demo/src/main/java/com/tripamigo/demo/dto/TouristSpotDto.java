package com.tripamigo.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TouristSpotDto {
    
    @NotBlank(message = "관광지 이름은 필수입니다")
    private String name;
    
    @NotBlank(message = "관광지 설명은 필수입니다")
    private String description;
    
    @NotBlank(message = "관광지 주소는 필수입니다")
    private String address;
    
    @NotNull(message = "위도는 필수입니다")
    private Double latitude;
    
    @NotNull(message = "경도는 필수입니다")
    private Double longitude;
    
    private String category;
    private String imageUrl;
    private Integer estimatedDuration; // 분 단위
    private Double rating;
    private String openingHours;
    private String contactInfo;
} 