package com.waynai.demo.service;

import com.waynai.demo.dto.TouristSpotDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TouristSpotService {
    
    public List<TouristSpotDto> getTouristSpotsByDestination(String destination, String theme) {
        log.info("Fetching tourist spots for destination: {} and theme: {}", destination, theme);
        
        // 실제 구현에서는 외부 API나 데이터베이스에서 조회
        // 현재는 샘플 데이터 반환
        return createSampleSpots(destination, theme);
    }
    
    public List<TouristSpotDto> getAllTouristSpots() {
        log.info("Fetching all tourist spots");
        
        List<TouristSpotDto> allSpots = new ArrayList<>();
        allSpots.addAll(createSampleSpots("서울", "문화"));
        allSpots.addAll(createSampleSpots("부산", "자연"));
        allSpots.addAll(createSampleSpots("제주", "자연"));
        
        return allSpots;
    }
    
    public TouristSpotDto getTouristSpotById(String spotId) {
        log.info("Fetching tourist spot by ID: {}", spotId);
        
        // 실제 구현에서는 데이터베이스에서 조회
        return createSampleSpots("서울", "문화").stream()
                .filter(spot -> spot.getName().contains("경복궁"))
                .findFirst()
                .orElse(null);
    }
    
    public List<TouristSpotDto> searchTouristSpots(String keyword) {
        log.info("Searching tourist spots with keyword: {}", keyword);
        
        List<TouristSpotDto> allSpots = getAllTouristSpots();
        
        return allSpots.stream()
                .filter(spot -> spot.getName().contains(keyword) || 
                               spot.getDescription().contains(keyword) ||
                               spot.getCategory().contains(keyword))
                .collect(Collectors.toList());
    }
    
    private List<TouristSpotDto> createSampleSpots(String destination, String theme) {
        List<TouristSpotDto> spots = new ArrayList<>();
        
        if ("서울".equals(destination) && "문화".equals(theme)) {
            spots.add(TouristSpotDto.builder()
                    .name("경복궁")
                    .description("조선왕조의 정궁으로 아름다운 전통 건축물을 감상할 수 있습니다.")
                    .address("서울특별시 종로구 사직로 161")
                    .latitude(37.5796)
                    .longitude(126.9770)
                    .category("문화재")
                    .imageUrl("https://example.com/gyeongbokgung.jpg")
                    .estimatedDuration(120)
                    .rating(4.5)
                    .openingHours("09:00-18:00")
                    .contactInfo("02-3700-3900")
                    .build());
            
            spots.add(TouristSpotDto.builder()
                    .name("창덕궁")
                    .description("유네스코 세계문화유산으로 지정된 아름다운 궁궐입니다.")
                    .address("서울특별시 종로구 율곡로 99")
                    .latitude(37.5794)
                    .longitude(126.9910)
                    .category("문화재")
                    .imageUrl("https://example.com/changdeokgung.jpg")
                    .estimatedDuration(90)
                    .rating(4.3)
                    .openingHours("09:00-17:00")
                    .contactInfo("02-3668-2300")
                    .build());
        } else if ("부산".equals(destination) && "자연".equals(theme)) {
            spots.add(TouristSpotDto.builder()
                    .name("해운대 해수욕장")
                    .description("부산의 대표적인 해수욕장으로 아름다운 해변을 즐길 수 있습니다.")
                    .address("부산광역시 해운대구 해운대해변로 264")
                    .latitude(35.1586)
                    .longitude(129.1603)
                    .category("자연")
                    .imageUrl("https://example.com/haeundae.jpg")
                    .estimatedDuration(180)
                    .rating(4.7)
                    .openingHours("24시간")
                    .contactInfo("051-749-5700")
                    .build());
        }
        
        return spots;
    }
} 