package com.waynai.demo.controller;

import com.waynai.demo.dto.AreaCodeDto;
import com.waynai.demo.dto.TouristSpotResponseDto;
import com.waynai.demo.dto.RelatedTouristSpotResponseDto;
import com.waynai.demo.service.TouristInfoService;
import com.waynai.demo.service.RelatedTouristInfoService;
import com.waynai.demo.service.LLMService;
import com.waynai.demo.service.ChatService;
import com.waynai.demo.client.GeminiApiClient;
import com.waynai.demo.dto.ChatRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 테스트용 컨트롤러
 * API 연동 테스트 및 데이터 검증을 위한 엔드포인트 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    
    private final TouristInfoService touristInfoService;
    private final RelatedTouristInfoService relatedTouristInfoService;
    private final LLMService llmService;
    private final ChatService chatService;
    private final GeminiApiClient geminiApiClient;
    
    /**
     * 무작위 관광지 조회 테스트
     * @return 테스트 결과
     */
    @GetMapping("/random-tourist-spots")
    public ResponseEntity<Map<String, Object>> testRandomTouristSpots() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("=== 무작위 관광지 조회 테스트 시작 ===");
            
            // 무작위 관광지 조회
            TouristSpotResponseDto response = touristInfoService.getRandomTouristSpots(1, 10);
            
            if (response != null) {
                result.put("success", response.isSuccess());
                result.put("resultCode", response.getResultCode());
                result.put("resultMsg", response.getResultMsg());
                result.put("totalCount", response.getTotalCount());
                result.put("numOfRows", response.getNumOfRows());
                result.put("pageNo", response.getPageNo());
                result.put("itemCount", response.getItemCount());
                
                if (response.getItems() != null) {
                    result.put("items", response.getItems());
                } else {
                    result.put("items", List.of());
                }
                
                log.info("테스트 성공 - 결과 코드: {}, 총 개수: {}", 
                        response.getResultCode(), response.getTotalCount());
            } else {
                result.put("success", false);
                result.put("error", "응답 데이터가 null입니다");
                log.error("응답 데이터가 null입니다");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("테스트 실패", e);
        }
        
        log.info("=== 무작위 관광지 조회 테스트 완료 ===");
        return ResponseEntity.ok(result);
    }
    
    /**
     * 특정 지역의 무작위 시군구 관광지 조회 테스트
     * @param areaCode 지역 코드
     * @return 테스트 결과
     */
    @GetMapping("/random-sigungu-tourist-spots/{areaCode}")
    public ResponseEntity<Map<String, Object>> testRandomSigunguTouristSpots(@PathVariable String areaCode) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("=== 지역 {} 의 무작위 시군구 관광지 조회 테스트 시작 ===", areaCode);
            
            // 특정 지역의 무작위 시군구 관광지 조회
            TouristSpotResponseDto response = touristInfoService.getRandomSigunguTouristSpots(areaCode, 1, 10);
            
            if (response != null) {
                result.put("success", response.isSuccess());
                result.put("areaCode", areaCode);
                result.put("resultCode", response.getResultCode());
                result.put("resultMsg", response.getResultMsg());
                result.put("totalCount", response.getTotalCount());
                result.put("numOfRows", response.getNumOfRows());
                result.put("pageNo", response.getPageNo());
                result.put("itemCount", response.getItemCount());
                
                if (response.getItems() != null) {
                    result.put("items", response.getItems());
                } else {
                    result.put("items", List.of());
                }
                
                log.info("테스트 성공 - 지역코드: {}, 결과 코드: {}, 총 개수: {}", 
                        areaCode, response.getResultCode(), response.getTotalCount());
            } else {
                result.put("success", false);
                result.put("error", "응답 데이터가 null입니다");
                log.error("응답 데이터가 null입니다");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("테스트 실패", e);
        }
        
        log.info("=== 지역 {} 의 무작위 시군구 관광지 조회 테스트 완료 ===", areaCode);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 지역 코드 정보 조회 테스트
     * @return 테스트 결과
     */
    @GetMapping("/area-codes")
    public ResponseEntity<Map<String, Object>> testAreaCodes() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("=== 지역 코드 정보 조회 테스트 시작 ===");
            
            List<AreaCodeDto> areaCodes = touristInfoService.getAllAreaCodes();
            
            result.put("success", true);
            result.put("totalCount", areaCodes.size());
            result.put("areaCodes", areaCodes);
            
            log.info("테스트 성공 - 총 지역 코드 개수: {}", areaCodes.size());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("테스트 실패", e);
        }
        
        log.info("=== 지역 코드 정보 조회 테스트 완료 ===");
        return ResponseEntity.ok(result);
    }
    
    /**
     * 서울 지역의 시군구 목록 조회 테스트
     * @return 테스트 결과
     */
    @GetMapping("/seoul-sigungu")
    public ResponseEntity<Map<String, Object>> testSeoulSigungu() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("=== 서울 지역 시군구 조회 테스트 시작 ===");
            
            List<AreaCodeDto> seoulSigungu = touristInfoService.getSigunguByAreaName("서울");
            
            result.put("success", true);
            result.put("totalCount", seoulSigungu.size());
            result.put("sigunguList", seoulSigungu);
            
            log.info("테스트 성공 - 서울 시군구 개수: {}", seoulSigungu.size());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("테스트 실패", e);
        }
        
        log.info("=== 서울 지역 시군구 조회 테스트 완료 ===");
        return ResponseEntity.ok(result);
    }
    
    /**
     * 지역기반 연관 관광지 조회 테스트
     * @return 테스트 결과
     */
    @GetMapping("/area-based-related-spots")
    public ResponseEntity<Map<String, Object>> testAreaBasedRelatedSpots() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("=== 지역기반 연관 관광지 조회 테스트 시작 ===");
            
            // 지역기반 연관 관광지 조회
            RelatedTouristSpotResponseDto response = relatedTouristInfoService.getAreaBasedRelatedSpots("11", "11530", 1, 10);
            
            if (response != null) {
                result.put("success", response.isSuccess());
                result.put("resultCode", response.getResultCode());
                result.put("resultMsg", response.getResultMsg());
                result.put("totalCount", response.getTotalCount());
                result.put("numOfRows", response.getNumOfRows());
                result.put("pageNo", response.getPageNo());
                result.put("itemCount", response.getItemCount());
                
                if (response.getItems() != null) {
                    result.put("items", response.getItems());
                } else {
                    result.put("items", List.of());
                }
                
                log.info("테스트 성공 - 결과 코드: {}, 총 개수: {}", 
                        response.getResultCode(), response.getTotalCount());
            } else {
                result.put("success", false);
                result.put("error", "응답 데이터가 null입니다");
                log.error("응답 데이터가 null입니다");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("테스트 실패", e);
        }
        
        log.info("=== 지역기반 연관 관광지 조회 테스트 완료 ===");
        return ResponseEntity.ok(result);
    }
    
    /**
     * 키워드 검색 연관 관광지 조회 테스트
     * @return 테스트 결과
     */
    @GetMapping("/keyword-search-related-spots")
    public ResponseEntity<Map<String, Object>> testKeywordSearchRelatedSpots() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("=== 키워드 검색 연관 관광지 조회 테스트 시작 ===");
            
            // 키워드 검색 연관 관광지 조회
            RelatedTouristSpotResponseDto response = relatedTouristInfoService.searchKeywordRelatedSpots("51", "51130", "뮤지엄산", 1, 10);
            
            if (response != null) {
                result.put("success", response.isSuccess());
                result.put("resultCode", response.getResultCode());
                result.put("resultMsg", response.getResultMsg());
                result.put("totalCount", response.getTotalCount());
                result.put("numOfRows", response.getNumOfRows());
                result.put("pageNo", response.getPageNo());
                result.put("itemCount", response.getItemCount());
                
                if (response.getItems() != null) {
                    result.put("items", response.getItems());
                } else {
                    result.put("items", List.of());
                }
                
                log.info("테스트 성공 - 결과 코드: {}, 총 개수: {}", 
                        response.getResultCode(), response.getTotalCount());
            } else {
                result.put("success", false);
                result.put("error", "응답 데이터가 null입니다");
                log.error("응답 데이터가 null입니다");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("테스트 실패", e);
        }
        
        log.info("=== 키워드 검색 연관 관광지 조회 테스트 완료 ===");
        return ResponseEntity.ok(result);
    }
    
    /**
     * LLM 일반 질의응답 테스트
     * @param query 사용자 질의
     * @return 스트림 응답
     */
    @GetMapping(value = "/llm/general", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> testLLMGeneral(@RequestParam String query) {
        log.info("=== LLM 일반 질의응답 테스트 시작 ===");
        log.info("질의: {}", query);
        
        return llmService.askGeneral(query)
                .doOnNext(chunk -> log.info("스트림 응답: {}", chunk))
                .doOnComplete(() -> log.info("=== LLM 일반 질의응답 테스트 완료 ==="))
                .doOnError(error -> log.error("LLM 테스트 오류", error));
    }
    
    /**
     * LLM 여행 가이드 테스트
     * @param query 사용자 질의
     * @return 스트림 응답
     */
    @GetMapping(value = "/llm/travel-guide", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> testLLMTravelGuide(@RequestParam String query) {
        log.info("=== LLM 여행 가이드 테스트 시작 ===");
        log.info("질의: {}", query);
        
        return Mono.fromCallable(() -> touristInfoService.getRandomTouristSpots(1, 5))
                .flatMapMany(response -> {
                    if (response.isSuccess()) {
                        return llmService.askTravelGuide(query, response.getItems())
                                .doOnNext(chunk -> log.info("스트림 응답: {}", chunk))
                                .doOnComplete(() -> log.info("=== LLM 여행 가이드 테스트 완료 ==="))
                                .doOnError(error -> log.error("LLM 테스트 오류", error));
                    } else {
                        return Flux.just("죄송합니다. 관광지 정보를 가져올 수 없습니다: " + response.getResultMsg());
                    }
                });
    }
    
    /**
     * LLM 관광지 정보 테스트
     * @param query 사용자 질의
     * @return 스트림 응답
     */
    @GetMapping(value = "/llm/tourist-info", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> testLLMTouristInfo(@RequestParam String query) {
        log.info("=== LLM 관광지 정보 테스트 시작 ===");
        log.info("질의: {}", query);
        
        return Mono.fromCallable(() -> touristInfoService.getRandomTouristSpots(1, 5))
                .flatMapMany(response -> {
                    if (response.isSuccess()) {
                        return llmService.askTouristInfo(query, response.getItems())
                                .doOnNext(chunk -> log.info("스트림 응답: {}", chunk))
                                .doOnComplete(() -> log.info("=== LLM 관광지 정보 테스트 완료 ==="))
                                .doOnError(error -> log.error("LLM 테스트 오류", error));
                    } else {
                        return Flux.just("죄송합니다. 관광지 정보를 가져올 수 없습니다: " + response.getResultMsg());
                    }
                });
    }
    
    /**
     * LLM 연관 관광지 테스트
     * @param query 사용자 질의
     * @return 스트림 응답
     */
    @GetMapping(value = "/llm/related-spots", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> testLLMRelatedSpots(@RequestParam String query) {
        log.info("=== LLM 연관 관광지 테스트 시작 ===");
        log.info("질의: {}", query);
        
        return Mono.fromCallable(() -> relatedTouristInfoService.getRandomRelatedTouristSpots(1, 5))
                .flatMapMany(response -> {
                    if (response.isSuccess()) {
                        return llmService.askRelatedSpots(query, response.getItems())
                                .doOnNext(chunk -> log.info("스트림 응답: {}", chunk))
                                .doOnComplete(() -> log.info("=== LLM 연관 관광지 테스트 완료 ==="))
                                .doOnError(error -> log.error("LLM 테스트 오류", error));
                    } else {
                        return Flux.just("죄송합니다. 연관 관광지 정보를 가져올 수 없습니다: " + response.getResultMsg());
                    }
                });
    }
    
    /**
     * 사용 가능한 프롬프트 목록 조회
     * @return 프롬프트 목록
     */
    @GetMapping("/llm/prompts")
    public ResponseEntity<Map<String, Object>> testLLMPrompts() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("=== LLM 프롬프트 목록 조회 테스트 시작 ===");
            
            String[] prompts = llmService.getAvailablePrompts();
            
            result.put("success", true);
            result.put("totalCount", prompts.length);
            result.put("prompts", prompts);
            
            log.info("테스트 성공 - 사용 가능한 프롬프트 개수: {}", prompts.length);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("테스트 실패", e);
        }
        
        log.info("=== LLM 프롬프트 목록 조회 테스트 완료 ===");
        return ResponseEntity.ok(result);
    }
    
    /**
     * 채팅 테스트 (간단한 메시지)
     * @param message 사용자 메시지
     * @return 스트림 응답
     */
    @GetMapping(value = "/chat/simple", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> testChatSimple(@RequestParam String message) {
        log.info("=== 채팅 테스트 시작 (간단) ===");
        log.info("메시지: {}", message);
        
        ChatRequestDto request = new ChatRequestDto();
        request.setMessage(message);
        request.setSessionId("test-session");
        
        return chatService.processChatMessage(request)
                .doOnNext(chunk -> log.info("채팅 스트림 응답: {}", chunk))
                .doOnComplete(() -> log.info("=== 채팅 테스트 완료 (간단) ==="))
                .doOnError(error -> log.error("채팅 테스트 오류", error));
    }
    
    /**
     * 채팅 테스트 (지역 기반)
     * @param message 사용자 메시지
     * @param areaCode 지역 코드
     * @param sigunguCode 시군구 코드
     * @return 스트림 응답
     */
    @GetMapping(value = "/chat/area", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> testChatArea(
            @RequestParam String message,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String sigunguCode) {
        
        log.info("=== 채팅 테스트 시작 (지역 기반) ===");
        log.info("메시지: {}, 지역: {}, 시군구: {}", message, areaCode, sigunguCode);
        
        ChatRequestDto request = new ChatRequestDto();
        request.setMessage(message);
        request.setSessionId("test-session");
        request.setAreaCode(areaCode);
        request.setSigunguCode(sigunguCode);
        
        return chatService.processChatMessage(request)
                .doOnNext(chunk -> log.info("채팅 스트림 응답: {}", chunk))
                .doOnComplete(() -> log.info("=== 채팅 테스트 완료 (지역 기반) ==="))
                .doOnError(error -> log.error("채팅 테스트 오류", error));
    }
    
    /**
     * Gemini 클라이언트 초기화 테스트
     * @return 초기화 상태
     */
    @GetMapping("/gemini/init")
    public ResponseEntity<Map<String, Object>> testGeminiInit() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("=== Gemini 클라이언트 초기화 테스트 시작 ===");
            
            // Gemini 클라이언트 초기화 시도
            geminiApiClient.initializeClient();
            
            result.put("success", true);
            result.put("message", "Gemini 클라이언트 초기화 성공");
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("=== Gemini 클라이언트 초기화 테스트 성공 ===");
            
        } catch (Exception e) {
            log.error("Gemini 클라이언트 초기화 실패", e);
            
            result.put("success", false);
            result.put("message", "Gemini 클라이언트 초기화 실패: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            result.put("timestamp", System.currentTimeMillis());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Gemini 간단한 텍스트 생성 테스트
     * @param prompt 프롬프트
     * @return 생성된 텍스트
     */
    @GetMapping(value = "/gemini/simple", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> testGeminiSimple(@RequestParam String prompt) {
        log.info("=== Gemini 간단한 텍스트 생성 테스트 시작 ===");
        log.info("프롬프트: {}", prompt);
        
        return geminiApiClient.generateText(prompt)
                .flatMapMany(text -> Flux.just(text))
                .doOnNext(text -> log.info("Gemini 응답: {}", text))
                .doOnComplete(() -> log.info("=== Gemini 간단한 텍스트 생성 테스트 완료 ==="))
                .doOnError(error -> log.error("Gemini 텍스트 생성 오류", error));
    }
    
    /**
     * Gemini 스트림 텍스트 생성 테스트
     * @param prompt 프롬프트
     * @return 스트림으로 생성된 텍스트
     */
    @GetMapping(value = "/gemini/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> testGeminiStream(@RequestParam String prompt) {
        log.info("=== Gemini 스트림 텍스트 생성 테스트 시작 ===");
        log.info("프롬프트: {}", prompt);
        
        return geminiApiClient.generateTextStream(prompt)
                .doOnNext(chunk -> log.info("Gemini 스트림 응답: {}", chunk))
                .doOnComplete(() -> log.info("=== Gemini 스트림 텍스트 생성 테스트 완료 ==="))
                .doOnError(error -> log.error("Gemini 스트림 텍스트 생성 오류", error));
    }
}
