package com.waynai.demo.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AreaCodeUtil {
    
    // 지역 코드 매핑
    private static final Map<String, String> AREA_CODES = new HashMap<>();
    private static final Map<String, String> SIGUNGU_CODES = new HashMap<>();
    
    static {
        // 지역 코드 초기화
        AREA_CODES.put("서울", "11");
        AREA_CODES.put("서울특별시", "11");
        AREA_CODES.put("부산", "26");
        AREA_CODES.put("부산광역시", "26");
        AREA_CODES.put("대구", "27");
        AREA_CODES.put("대구광역시", "27");
        AREA_CODES.put("인천", "28");
        AREA_CODES.put("인천광역시", "28");
        AREA_CODES.put("광주", "29");
        AREA_CODES.put("광주광역시", "29");
        AREA_CODES.put("대전", "30");
        AREA_CODES.put("대전광역시", "30");
        AREA_CODES.put("울산", "31");
        AREA_CODES.put("울산광역시", "31");
        AREA_CODES.put("세종", "36");
        AREA_CODES.put("세종특별자치시", "36");
        AREA_CODES.put("경기", "41");
        AREA_CODES.put("경기도", "41");
        AREA_CODES.put("충북", "43");
        AREA_CODES.put("충청북도", "43");
        AREA_CODES.put("충남", "44");
        AREA_CODES.put("충청남도", "44");
        AREA_CODES.put("전북", "45");
        AREA_CODES.put("전라북도", "45");
        AREA_CODES.put("전남", "46");
        AREA_CODES.put("전라남도", "46");
        AREA_CODES.put("경북", "47");
        AREA_CODES.put("경상북도", "47");
        AREA_CODES.put("경남", "48");
        AREA_CODES.put("경상남도", "48");
        AREA_CODES.put("제주", "50");
        AREA_CODES.put("제주특별자치도", "50");
        AREA_CODES.put("강원", "51");
        AREA_CODES.put("강원특별자치도", "51");
        
        // 시군구 코드 초기화 (주요 지역만)
        // 서울
        SIGUNGU_CODES.put("종로구", "11110");
        SIGUNGU_CODES.put("중구", "11140");
        SIGUNGU_CODES.put("용산구", "11170");
        SIGUNGU_CODES.put("성동구", "11200");
        SIGUNGU_CODES.put("광진구", "11215");
        SIGUNGU_CODES.put("동대문구", "11230");
        SIGUNGU_CODES.put("중랑구", "11260");
        SIGUNGU_CODES.put("성북구", "11290");
        SIGUNGU_CODES.put("강북구", "11305");
        SIGUNGU_CODES.put("도봉구", "11320");
        SIGUNGU_CODES.put("노원구", "11350");
        SIGUNGU_CODES.put("은평구", "11380");
        SIGUNGU_CODES.put("서대문구", "11410");
        SIGUNGU_CODES.put("마포구", "11440");
        SIGUNGU_CODES.put("양천구", "11470");
        SIGUNGU_CODES.put("강서구", "11500");
        SIGUNGU_CODES.put("구로구", "11530");
        SIGUNGU_CODES.put("금천구", "11545");
        SIGUNGU_CODES.put("영등포구", "11560");
        SIGUNGU_CODES.put("동작구", "11590");
        SIGUNGU_CODES.put("관악구", "11620");
        SIGUNGU_CODES.put("서초구", "11650");
        SIGUNGU_CODES.put("강남구", "11680");
        SIGUNGU_CODES.put("송파구", "11710");
        SIGUNGU_CODES.put("강동구", "11740");
        
        // 부산
        SIGUNGU_CODES.put("부산중구", "26110");
        SIGUNGU_CODES.put("부산서구", "26140");
        SIGUNGU_CODES.put("부산동구", "26170");
        SIGUNGU_CODES.put("영도구", "26200");
        SIGUNGU_CODES.put("부산진구", "26230");
        SIGUNGU_CODES.put("동래구", "26260");
        SIGUNGU_CODES.put("부산남구", "26290");
        SIGUNGU_CODES.put("부산북구", "26320");
        SIGUNGU_CODES.put("해운대구", "26350");
        SIGUNGU_CODES.put("사하구", "26380");
        SIGUNGU_CODES.put("금정구", "26410");
        SIGUNGU_CODES.put("부산강서구", "26440");
        SIGUNGU_CODES.put("연제구", "26470");
        SIGUNGU_CODES.put("수영구", "26500");
        SIGUNGU_CODES.put("사상구", "26530");
        SIGUNGU_CODES.put("기장군", "26710");
        
        // 대구
        SIGUNGU_CODES.put("대구중구", "27110");
        SIGUNGU_CODES.put("대구동구", "27140");
        SIGUNGU_CODES.put("대구서구", "27170");
        SIGUNGU_CODES.put("대구남구", "27200");
        SIGUNGU_CODES.put("대구북구", "27230");
        SIGUNGU_CODES.put("수성구", "27260");
        SIGUNGU_CODES.put("달서구", "27290");
        SIGUNGU_CODES.put("달성군", "27710");
        SIGUNGU_CODES.put("군위군", "27720");
        
        // 인천
        SIGUNGU_CODES.put("인천중구", "28110");
        SIGUNGU_CODES.put("인천동구", "28140");
        SIGUNGU_CODES.put("미추홀구", "28177");
        SIGUNGU_CODES.put("연수구", "28185");
        SIGUNGU_CODES.put("인천남동구", "28200");
        SIGUNGU_CODES.put("부평구", "28237");
        SIGUNGU_CODES.put("계양구", "28245");
        SIGUNGU_CODES.put("인천서구", "28260");
        SIGUNGU_CODES.put("강화군", "28710");
        SIGUNGU_CODES.put("옹진군", "28720");
        
        // 광주
        SIGUNGU_CODES.put("광주동구", "29110");
        SIGUNGU_CODES.put("광주서구", "29140");
        SIGUNGU_CODES.put("광주남구", "29155");
        SIGUNGU_CODES.put("광주북구", "29170");
        SIGUNGU_CODES.put("광산구", "29200");
        
        // 대전
        SIGUNGU_CODES.put("대전동구", "30110");
        SIGUNGU_CODES.put("대전중구", "30140");
        SIGUNGU_CODES.put("대전서구", "30170");
        SIGUNGU_CODES.put("유성구", "30200");
        SIGUNGU_CODES.put("대덕구", "30230");
        
        // 울산
        SIGUNGU_CODES.put("울산중구", "31110");
        SIGUNGU_CODES.put("울산남구", "31140");
        SIGUNGU_CODES.put("울산동구", "31170");
        SIGUNGU_CODES.put("울산북구", "31200");
        SIGUNGU_CODES.put("울주군", "31710");
        
        // 세종
        SIGUNGU_CODES.put("세종특별자치시", "36110");
        
        // 경기도
        SIGUNGU_CODES.put("수원시장안구", "41111");
        SIGUNGU_CODES.put("수원시권선구", "41113");
        SIGUNGU_CODES.put("수원시팔달구", "41115");
        SIGUNGU_CODES.put("수원시영통구", "41117");
        SIGUNGU_CODES.put("성남시수정구", "41131");
        SIGUNGU_CODES.put("성남시중원구", "41133");
        SIGUNGU_CODES.put("성남시분당구", "41135");
        SIGUNGU_CODES.put("의정부시", "41150");
        SIGUNGU_CODES.put("안양시만안구", "41171");
        SIGUNGU_CODES.put("안양시동안구", "41173");
        SIGUNGU_CODES.put("부천시원미구", "41192");
        SIGUNGU_CODES.put("부천시소사구", "41194");
        SIGUNGU_CODES.put("부천시오정구", "41196");
        SIGUNGU_CODES.put("광명시", "41210");
        SIGUNGU_CODES.put("평택시", "41220");
        SIGUNGU_CODES.put("동두천시", "41250");
        SIGUNGU_CODES.put("안산시상록구", "41271");
        SIGUNGU_CODES.put("안산시단원구", "41273");
        SIGUNGU_CODES.put("고양시덕양구", "41281");
        SIGUNGU_CODES.put("고양시일산동구", "41285");
        SIGUNGU_CODES.put("고양시일산서구", "41287");
        SIGUNGU_CODES.put("과천시", "41290");
        SIGUNGU_CODES.put("구리시", "41310");
        SIGUNGU_CODES.put("남양주시", "41360");
        SIGUNGU_CODES.put("오산시", "41370");
        SIGUNGU_CODES.put("시흥시", "41390");
        SIGUNGU_CODES.put("군포시", "41410");
        SIGUNGU_CODES.put("의왕시", "41430");
        SIGUNGU_CODES.put("하남시", "41450");
        SIGUNGU_CODES.put("용인시처인구", "41461");
        SIGUNGU_CODES.put("용인시기흥구", "41463");
        SIGUNGU_CODES.put("용인시수지구", "41465");
        SIGUNGU_CODES.put("파주시", "41480");
        SIGUNGU_CODES.put("이천시", "41500");
        SIGUNGU_CODES.put("안성시", "41550");
        SIGUNGU_CODES.put("김포시", "41570");
        SIGUNGU_CODES.put("화성시", "41590");
        SIGUNGU_CODES.put("광주시", "41610");
        SIGUNGU_CODES.put("양주시", "41630");
        SIGUNGU_CODES.put("포천시", "41650");
        SIGUNGU_CODES.put("여주시", "41670");
        SIGUNGU_CODES.put("연천군", "41800");
        SIGUNGU_CODES.put("가평군", "41820");
        SIGUNGU_CODES.put("양평군", "41830");
        
        // 충청북도
        SIGUNGU_CODES.put("청주시상당구", "43111");
        SIGUNGU_CODES.put("청주시서원구", "43112");
        SIGUNGU_CODES.put("청주시흥덕구", "43113");
        SIGUNGU_CODES.put("청주시청원구", "43114");
        SIGUNGU_CODES.put("충주시", "43130");
        SIGUNGU_CODES.put("제천시", "43150");
        SIGUNGU_CODES.put("보은군", "43720");
        SIGUNGU_CODES.put("옥천군", "43730");
        SIGUNGU_CODES.put("영동군", "43740");
        SIGUNGU_CODES.put("증평군", "43745");
        SIGUNGU_CODES.put("진천군", "43750");
        SIGUNGU_CODES.put("괴산군", "43760");
        SIGUNGU_CODES.put("음성군", "43770");
        SIGUNGU_CODES.put("단양군", "43800");
        
        // 충청남도
        SIGUNGU_CODES.put("천안시동남구", "44131");
        SIGUNGU_CODES.put("천안시서북구", "44133");
        SIGUNGU_CODES.put("공주시", "44150");
        SIGUNGU_CODES.put("보령시", "44180");
        SIGUNGU_CODES.put("아산시", "44200");
        SIGUNGU_CODES.put("서산시", "44210");
        SIGUNGU_CODES.put("논산시", "44230");
        SIGUNGU_CODES.put("계룡시", "44250");
        SIGUNGU_CODES.put("당진시", "44270");
        SIGUNGU_CODES.put("금산군", "44710");
        SIGUNGU_CODES.put("부여군", "44760");
        SIGUNGU_CODES.put("서천군", "44770");
        SIGUNGU_CODES.put("청양군", "44790");
        SIGUNGU_CODES.put("홍성군", "44800");
        SIGUNGU_CODES.put("예산군", "44810");
        SIGUNGU_CODES.put("태안군", "44825");
        
        // 전라북도
        SIGUNGU_CODES.put("전주시완산구", "52111");
        SIGUNGU_CODES.put("전주시덕진구", "52113");
        SIGUNGU_CODES.put("군산시", "52130");
        SIGUNGU_CODES.put("익산시", "52140");
        SIGUNGU_CODES.put("정읍시", "52180");
        SIGUNGU_CODES.put("남원시", "52190");
        SIGUNGU_CODES.put("김제시", "52210");
        SIGUNGU_CODES.put("완주군", "52710");
        SIGUNGU_CODES.put("진안군", "52720");
        SIGUNGU_CODES.put("무주군", "52730");
        SIGUNGU_CODES.put("장수군", "52740");
        SIGUNGU_CODES.put("임실군", "52750");
        SIGUNGU_CODES.put("순창군", "52770");
        SIGUNGU_CODES.put("고창군", "52790");
        SIGUNGU_CODES.put("부안군", "52800");
        
        // 전라남도
        SIGUNGU_CODES.put("목포시", "46110");
        SIGUNGU_CODES.put("여수시", "46130");
        SIGUNGU_CODES.put("순천시", "46150");
        SIGUNGU_CODES.put("나주시", "46170");
        SIGUNGU_CODES.put("광양시", "46230");
        SIGUNGU_CODES.put("담양군", "46710");
        SIGUNGU_CODES.put("곡성군", "46720");
        SIGUNGU_CODES.put("구례군", "46730");
        SIGUNGU_CODES.put("고흥군", "46770");
        SIGUNGU_CODES.put("보성군", "46780");
        SIGUNGU_CODES.put("화순군", "46790");
        SIGUNGU_CODES.put("장흥군", "46800");
        SIGUNGU_CODES.put("강진군", "46810");
        SIGUNGU_CODES.put("해남군", "46820");
        SIGUNGU_CODES.put("영암군", "46830");
        SIGUNGU_CODES.put("무안군", "46840");
        SIGUNGU_CODES.put("함평군", "46860");
        SIGUNGU_CODES.put("영광군", "46870");
        SIGUNGU_CODES.put("장성군", "46880");
        SIGUNGU_CODES.put("완도군", "46890");
        SIGUNGU_CODES.put("진도군", "46900");
        SIGUNGU_CODES.put("신안군", "46910");
        
        // 경상북도
        SIGUNGU_CODES.put("포항시남구", "47111");
        SIGUNGU_CODES.put("포항시북구", "47113");
        SIGUNGU_CODES.put("경주시", "47130");
        SIGUNGU_CODES.put("김천시", "47150");
        SIGUNGU_CODES.put("안동시", "47170");
        SIGUNGU_CODES.put("구미시", "47190");
        SIGUNGU_CODES.put("영주시", "47210");
        SIGUNGU_CODES.put("영천시", "47230");
        SIGUNGU_CODES.put("상주시", "47250");
        SIGUNGU_CODES.put("문경시", "47280");
        SIGUNGU_CODES.put("경산시", "47290");
        SIGUNGU_CODES.put("의성군", "47730");
        SIGUNGU_CODES.put("청송군", "47750");
        SIGUNGU_CODES.put("영양군", "47760");
        SIGUNGU_CODES.put("영덕군", "47770");
        SIGUNGU_CODES.put("청도군", "47820");
        SIGUNGU_CODES.put("고령군", "47830");
        SIGUNGU_CODES.put("성주군", "47840");
        SIGUNGU_CODES.put("칠곡군", "47850");
        SIGUNGU_CODES.put("예천군", "47900");
        SIGUNGU_CODES.put("봉화군", "47920");
        SIGUNGU_CODES.put("울진군", "47930");
        SIGUNGU_CODES.put("울릉군", "47940");
        
        // 경상남도
        SIGUNGU_CODES.put("창원시의창구", "48121");
        SIGUNGU_CODES.put("창원시성산구", "48123");
        SIGUNGU_CODES.put("창원시마산합포구", "48125");
        SIGUNGU_CODES.put("창원시마산회원구", "48127");
        SIGUNGU_CODES.put("창원시진해구", "48129");
        SIGUNGU_CODES.put("진주시", "48170");
        SIGUNGU_CODES.put("통영시", "48220");
        SIGUNGU_CODES.put("사천시", "48240");
        SIGUNGU_CODES.put("김해시", "48250");
        SIGUNGU_CODES.put("밀양시", "48270");
        SIGUNGU_CODES.put("거제시", "48310");
        SIGUNGU_CODES.put("양산시", "48330");
        SIGUNGU_CODES.put("의령군", "48720");
        SIGUNGU_CODES.put("함안군", "48730");
        SIGUNGU_CODES.put("창녕군", "48740");
        SIGUNGU_CODES.put("고성군", "48820");
        SIGUNGU_CODES.put("남해군", "48840");
        SIGUNGU_CODES.put("하동군", "48860");
        SIGUNGU_CODES.put("산청군", "48870");
        SIGUNGU_CODES.put("함양군", "48880");
        SIGUNGU_CODES.put("거창군", "48890");
        SIGUNGU_CODES.put("합천군", "48890");
        
        // 제주특별자치도
        SIGUNGU_CODES.put("제주시", "50110");
        SIGUNGU_CODES.put("서귀포시", "50130");
        
        // 강원특별자치도
        SIGUNGU_CODES.put("춘천시", "51110");
        SIGUNGU_CODES.put("원주시", "51130");
        SIGUNGU_CODES.put("강릉시", "51150");
        SIGUNGU_CODES.put("동해시", "51170");
        SIGUNGU_CODES.put("태백시", "51190");
        SIGUNGU_CODES.put("속초시", "51210");
        SIGUNGU_CODES.put("삼척시", "51230");
        SIGUNGU_CODES.put("홍천군", "51720");
        SIGUNGU_CODES.put("횡성군", "51730");
        SIGUNGU_CODES.put("영월군", "51750");
        SIGUNGU_CODES.put("평창군", "51760");
        SIGUNGU_CODES.put("정선군", "51770");
        SIGUNGU_CODES.put("철원군", "51780");
        SIGUNGU_CODES.put("화천군", "51790");
        SIGUNGU_CODES.put("양구군", "51800");
        SIGUNGU_CODES.put("인제군", "51810");
        SIGUNGU_CODES.put("고성군", "51820");
        SIGUNGU_CODES.put("양양군", "51830");
    }
    
    /**
     * 지역명으로 지역 코드 조회
     */
    public String getAreaCode(String areaName) {
        if (areaName == null || areaName.trim().isEmpty()) {
            return null;
        }
        
        String normalizedName = areaName.trim();
        String areaCode = AREA_CODES.get(normalizedName);
        
        if (areaCode == null) {
            // 부분 매칭 시도
            for (Map.Entry<String, String> entry : AREA_CODES.entrySet()) {
                if (entry.getKey().contains(normalizedName) || normalizedName.contains(entry.getKey())) {
                    areaCode = entry.getValue();
                    break;
                }
            }
        }
        
        log.debug("Area code lookup for '{}': {}", areaName, areaCode);
        return areaCode;
    }
    
    /**
     * 시군구명으로 시군구 코드 조회
     */
    public String getSignguCode(String signguName) {
        if (signguName == null || signguName.trim().isEmpty()) {
            return null;
        }
        
        String normalizedName = signguName.trim();
        String signguCode = SIGUNGU_CODES.get(normalizedName);
        
        if (signguCode == null) {
            // 부분 매칭 시도
            for (Map.Entry<String, String> entry : SIGUNGU_CODES.entrySet()) {
                if (entry.getKey().contains(normalizedName) || normalizedName.contains(entry.getKey())) {
                    signguCode = entry.getValue();
                    break;
                }
            }
        }
        
        log.debug("Signgu code lookup for '{}': {}", signguName, signguCode);
        return signguCode;
    }
    
    /**
     * 지역명과 시군구명으로 코드 조회
     */
    public AreaCodeInfo getAreaCodeInfo(String areaName, String signguName) {
        String areaCode = getAreaCode(areaName);
        String signguCode = getSignguCode(signguName);
        
        return AreaCodeInfo.builder()
                .areaCode(areaCode)
                .signguCode(signguCode)
                .areaName(areaName)
                .signguName(signguName)
                .build();
    }
    
    /**
     * 키워드에서 지역 정보 추출
     */
    public AreaCodeInfo extractAreaInfoFromKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        
        String normalizedKeyword = keyword.trim();
        
        // 지역명과 시군구명을 함께 찾기
        for (Map.Entry<String, String> areaEntry : AREA_CODES.entrySet()) {
            if (normalizedKeyword.contains(areaEntry.getKey())) {
                String areaCode = areaEntry.getValue();
                String areaName = areaEntry.getKey();
                
                // 해당 지역의 시군구 찾기
                for (Map.Entry<String, String> signguEntry : SIGUNGU_CODES.entrySet()) {
                    if (normalizedKeyword.contains(signguEntry.getKey())) {
                        return AreaCodeInfo.builder()
                                .areaCode(areaCode)
                                .signguCode(signguEntry.getValue())
                                .areaName(areaName)
                                .signguName(signguEntry.getKey())
                                .build();
                    }
                }
                
                // 시군구가 없으면 지역만 반환
                return AreaCodeInfo.builder()
                        .areaCode(areaCode)
                        .areaName(areaName)
                        .build();
            }
        }
        
        return null;
    }
    
    /**
     * 지역명과 시군구명으로 AreaCodeInfo 찾기
     */
    public AreaCodeInfo findAreaCodeByName(String areaName, String signguName) {
        String areaCode = getAreaCode(areaName);
        String signguCode = getSignguCode(signguName);
        
        if (areaCode != null) {
            return AreaCodeInfo.builder()
                    .areaCode(areaCode)
                    .signguCode(signguCode)
                    .areaName(areaName)
                    .signguName(signguName)
                    .build();
        }
        
        return null;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class AreaCodeInfo {
        private String areaCode;
        private String signguCode;
        private String areaName;
        private String signguName;
    }
} 