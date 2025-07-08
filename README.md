# WaynAI — AI 기반 여행 파트너

"당신의 길을 함께 찾는 여행 파트너, WaynAI"

WaynAI는 단순한 정보 검색 툴이 아닌, 여행자의 목적, 취향, 일정, 감성에 따라 여정을 설계해주는 AI 기반 여행 동반자입니다. 말 그대로 "어디로 갈까?"를 고민할 때, WaynAI가 방향을 제시해줍니다.

## 🌟 브랜드 의미

**WaynAI**는 다음 단어들의 조합에서 탄생한 이름입니다:

- **Way**: 길, 방향, 여정 — 여행의 시작과 끝을 모두 포괄
- **AI**: 인공지능 — 사용자의 취향과 상황에 맞춘 맞춤형 여행 안내자

"Way + n + AI"는 "Where are you going?" 또는 "Which way?" 같은 여정을 떠올리게 하고, 동시에 "Way + AI"는 AI가 길을 안내한다는 이미지를 직관적으로 전달합니다.

## 🚀 주요 기능

### AI 여행 플래너
- 날짜, 예산, 관심사 입력 → 최적의 여행 일정 자동 생성
- 개인화된 여행 경험 설계

### 대화형 여행 가이드 (LLM 기반)
- 여행지 설명, 역사/문화 맥락, 음식 추천 등
- GPT 기반으로 자연어로 질문-답변 가능

### 현지 맞춤 추천 (RAG 기반)
- 날씨, 시간대, 계절, 현재 트렌드 기반 추천
- 현지 리뷰, 블로그, 커뮤니티에서 신뢰도 높은 콘텐츠 추출

### 루트 최적화 / 지도 연동
- 지도 기반 추천 코스 안내, 교통 정보까지 포함
- 실시간 교통 상황 반영

## 🏗️ 시스템 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Vue Frontend  │    │  Spring Backend │    │  External APIs  │
│                 │    │                 │    │                 │
│ • Search Input  │◄──►│ • Search Service│◄──►│ • Gemini API    │
│ • Progress UI   │    │ • LLM Service   │    │ • Tourist API   │
│ • Results       │    │ • RAG System    │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 📁 프로젝트 구조

```
waynai/
├── waynai-backend/                # Spring Boot 백엔드
│   ├── src/main/java/com/waynai/demo/
│   │   ├── controller/            # REST API 컨트롤러
│   │   ├── service/               # 비즈니스 로직
│   │   ├── client/                # 외부 API 클라이언트
│   │   ├── dto/                   # 데이터 전송 객체
│   │   └── interceptor/           # 인터셉터
│   └── src/main/resources/
│       └── application.properties # 설정 파일
└── waynai-frontend/               # Vue 3 프론트엔드
    ├── src/
    │   ├── components/            # Vue 컴포넌트
    │   ├── services/              # API 서비스
    │   ├── stores/                # Pinia 스토어
    │   ├── types/                 # TypeScript 타입
    │   └── views/                 # 페이지 뷰
    └── package.json
```

## 🛠️ 기술 스택

### 백엔드
- **Java 17** + **Spring Boot 3.x**
- **Spring WebFlux** (리액티브 프로그래밍)
- **Gemini API** (Google AI)
- **Korea Tourism Organization API**
- **SSE** (Server-Sent Events)

### 프론트엔드
- **Vue 3** (Composition API)
- **TypeScript**
- **Pinia** (상태 관리)
- **Axios** (HTTP 클라이언트)
- **SSE** (실시간 데이터 스트리밍)

## 🚀 빠른 시작

### 1. 백엔드 실행

```bash
cd waynai-backend
./mvnw spring-boot:run
```

### 2. 프론트엔드 실행

```bash
cd waynai-frontend
npm install
npm run dev
```

### 3. 브라우저에서 접속

- 프론트엔드: http://localhost:5173
- 백엔드 API: http://localhost:8080

## 📋 API 엔드포인트

### 검색 API
- `POST /api/search/stream` - SSE 기반 실시간 검색
- `GET /api/search/health` - 서버 상태 확인

### 기존 API
- `POST /api/courses` - 여행 코스 생성
- `GET /api/courses/{id}` - 코스 조회
- `GET /api/tips` - 여행 팁 생성
- `GET /api/tourist-info/area/{areaCd}/{signguCd}` - 지역별 관광지 정보
- `GET /api/tourist-info/keyword/{keyword}` - 키워드별 관광지 정보

## 🔧 설정

### 백엔드 설정 (application.properties)

```properties
# Gemini API
gemini.api.key=your-gemini-api-key
gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent

# Tourist API
tourist.api.base-url=http://api.visitkorea.or.kr/openapi/service/rest/KorService
tourist.api.service-key=your-tourist-api-key
```

### 프론트엔드 설정

백엔드 서버 URL은 `src/services/searchService.ts`에서 수정 가능:

```typescript
const API_BASE_URL = 'http://localhost:8080';
```

## 💡 사용 예시

### 1. 키워드 기반 검색
```
입력: "경복궁"
결과: 경복궁 주변 관광지 추천 및 관련 정보
```

### 2. 문장 기반 검색
```
입력: "서울에서 3일간 문화 여행하고 싶어요"
결과: 상세한 3일 여행 계획 및 일정
```

## 🔄 검색 프로세스

1. **의도 분석**: 사용자 입력을 분석하여 키워드/문장 구분
2. **질의 강화**: 원본 질의를 더 구체적인 검색어로 강화
3. **지식 검색**: 외부 API를 통한 관광지 정보 수집
4. **결과 생성**: LLM을 활용한 최종 여행 계획 생성

## 📊 실시간 진행 상황

SSE를 통해 각 단계별 진행 상황을 실시간으로 확인할 수 있습니다:

- 🔍 의도 분석 중
- ⚡ 질의 강화 중
- 🔎 지식 검색 중
- ✨ 결과 생성 중
- ✅ 완료

## 🎨 UI/UX 특징

- **실시간 피드백**: 검색 과정을 단계별로 시각화
- **반응형 디자인**: 모든 디바이스에서 최적화된 경험
- **직관적인 인터페이스**: 사용자 친화적인 검색 및 결과 표시
- **결과 다운로드**: 생성된 계획을 파일로 저장 가능

## 🧭 타깃 사용자

- **외국인 관광객**: 맞춤 코스 & 문화 설명이 필요함
- **한국인 자유여행자**: 일정 설계와 정보 탐색을 편하게 하고 싶은 Z세대
- **콘텐츠 크리에이터**: 감성적 장소 추천, 동선 최적화 등 필요
- **장기 체류 디지털 노마드**: 지역 기반 추천과 일정 최적화

## 🎨 브랜드 스타일

- **로고**: 지도 핀 + 뇌(혹은 AI 아이콘) 결합형 아이콘
- **컬러**: 딥블루 + 미드그린 or 테크틱한 민트톤
- **UI 톤**: 간결한 인터페이스, 감성적인 이미지 강조 (파스텔/모노톤 가능)

## 📣 슬로건

- "AI가 이끄는 당신만의 여행 길"
- "모르는 길도 걱정 없이, WaynAI와 함께"
- "당신의 다음 목적지, AI가 알고 있어요"
- "길을 묻는 여행자에게, AI로 답하다"
- "Every Way, Your Way — with WaynAI"

## 🔒 보안

- API 키는 환경 변수로 관리
- CORS 설정으로 프론트엔드 접근 허용
- 입력 검증 및 에러 핸들링

## 🧪 테스트

### 백엔드 테스트
```bash
cd waynai-backend
./mvnw test
```

### 프론트엔드 테스트
```bash
cd waynai-frontend
npm run test
```

## 📈 성능 최적화

- **리액티브 프로그래밍**: Spring WebFlux로 비동기 처리
- **SSE**: 실시간 데이터 스트리밍으로 대기 시간 최소화
- **RAG**: 외부 API를 통한 최신 정보 활용
- **캐싱**: 반복 요청에 대한 응답 시간 단축

## 🚧 향후 개선 계획

- [ ] 더 정교한 지역 코드 매핑
- [ ] 사용자 피드백 시스템
- [ ] 개인화된 추천 알고리즘
- [ ] 다국어 지원
- [ ] 모바일 앱 개발
- [ ] 소셜 기능 (여행 계획 공유)

## 📝 라이선스

MIT License

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📞 문의

프로젝트에 대한 문의사항이 있으시면 이슈를 생성해 주세요. 