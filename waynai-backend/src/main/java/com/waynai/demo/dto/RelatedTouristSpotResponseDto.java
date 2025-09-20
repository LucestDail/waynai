package com.waynai.demo.dto;

import lombok.Data;

import java.util.List;

/**
 * 연관 관광지 조회 응답 DTO (파싱된 데이터)
 */
@Data
public class RelatedTouristSpotResponseDto {
    
    private boolean success;
    private String resultCode;
    private String resultMsg;
    private Integer pageNo;
    private Integer numOfRows;
    private Integer totalCount;
    private Integer itemCount;
    private List<RelatedTouristSpotDto> items;
    
    public RelatedTouristSpotResponseDto() {
        this.success = false;
    }
    
    public RelatedTouristSpotResponseDto(boolean success, String resultCode, String resultMsg) {
        this.success = success;
        this.resultCode = resultCode;
        this.resultMsg = resultMsg;
    }
    
    public static RelatedTouristSpotResponseDto success(RelatedTouristApiResponseDto apiResponse) {
        RelatedTouristSpotResponseDto response = new RelatedTouristSpotResponseDto();
        response.setSuccess(true);
        
        if (apiResponse != null && apiResponse.getResponse() != null) {
            RelatedTouristApiResponseDto.Response responseData = apiResponse.getResponse();
            
            // 헤더 정보
            if (responseData.getHeader() != null) {
                response.setResultCode(responseData.getHeader().getResultCode());
                response.setResultMsg(responseData.getHeader().getResultMsg());
            }
            
            // 바디 정보
            if (responseData.getBody() != null) {
                response.setPageNo(responseData.getBody().getPageNo());
                response.setNumOfRows(responseData.getBody().getNumOfRows());
                response.setTotalCount(responseData.getBody().getTotalCount());
                
                // 아이템 정보
                if (responseData.getBody().getItems() != null && 
                    responseData.getBody().getItems().getItem() != null) {
                    response.setItems(responseData.getBody().getItems().getItem());
                    response.setItemCount(responseData.getBody().getItems().getItem().size());
                } else {
                    response.setItemCount(0);
                }
            }
        }
        
        return response;
    }
    
    public static RelatedTouristSpotResponseDto error(String resultCode, String resultMsg) {
        return new RelatedTouristSpotResponseDto(false, resultCode, resultMsg);
    }
}
