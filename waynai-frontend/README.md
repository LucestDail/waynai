# WaynAI Frontend

Vue 3 기반의 AI 여행 파트너 프론트엔드 애플리케이션입니다.

## 🌟 WaynAI 소개

"당신의 길을 함께 찾는 여행 파트너, WaynAI"

WaynAI는 단순한 정보 검색 툴이 아닌, 여행자의 목적, 취향, 일정, 감성에 따라 여정을 설계해주는 AI 기반 여행 동반자입니다. 말 그대로 "어디로 갈까?"를 고민할 때, WaynAI가 방향을 제시해줍니다.

## 🚀 주요 기능

### 🤖 AI 여행 플래너
- **개인화된 여행 설계**: 날짜, 예산, 관심사 입력 → 최적의 여행 일정 자동 생성
- **의도 분석**: 사용자 입력을 분석하여 키워드 기반 추천과 문장 기반 계획 생성으로 구분
- **질의 강화**: 원본 질의를 더 구체적이고 정확한 검색어로 강화

### 💬 대화형 여행 가이드
- **자연어 Q&A**: 여행지 설명, 역사/문화 맥락, 음식 추천 등 GPT 기반 답변
- **실시간 상호작용**: SSE(Server-Sent Events)를 통한 실시간 대화형 인터페이스

### 🎯 현지 맞춤 추천
- **RAG 기반 검색**: 외부 관광지 API를 활용한 최신 정보 기반 추천
- **맥락 기반 추천**: 날씨, 시간대, 계절, 현재 트렌드 반영

### 🗺️ 루트 최적화
- **지도 기반 코스**: 추천 코스 안내 및 교통 정보 제공
- **실시간 진행**: 단계별 검색 진행 상황을 시각적으로 표시

## 🎨 사용자 인터페이스

### 브랜드 디자인
- **WaynAI 브랜드 컬러**: 딥블루(#1e3c72) + 미드그린 + 테크틱한 민트톤
- **로고**: 지도 핀 + 뇌(AI 아이콘) 결합형 아이콘
- **UI 톤**: 간결한 인터페이스, 감성적인 이미지 강조

### 반응형 디자인
- **모바일 최적화**: 모든 디바이스에서 최적화된 경험
- **직관적인 인터페이스**: 사용자 친화적인 검색 및 결과 표시
- **결과 다운로드**: 생성된 계획을 파일로 저장 가능

## 🛠️ 기술 스택

- **Vue 3**: Composition API 기반
- **TypeScript**: 타입 안전성 보장
- **Pinia**: 상태 관리
- **Vue Router**: 라우팅
- **Axios**: HTTP 클라이언트
- **SSE**: 실시간 데이터 스트리밍

## 📦 설치 및 실행

### 1. 의존성 설치
```bash
npm install
```

### 2. 개발 서버 실행
```bash
npm run dev
```

### 3. 빌드
```bash
npm run build
```

### 4. 프로덕션 미리보기
```bash
npm run preview
```

## 📁 프로젝트 구조

```
src/
├── components/          # Vue 컴포넌트
│   ├── SearchInput.vue     # 검색 입력 컴포넌트
│   ├── SearchProgress.vue  # 검색 진행 상황 컴포넌트
│   └── SearchResult.vue    # 검색 결과 컴포넌트
├── services/           # API 서비스
│   └── searchService.ts    # 검색 관련 API 호출
├── stores/             # Pinia 스토어
│   └── search.ts           # 검색 상태 관리
├── types/              # TypeScript 타입 정의
│   └── search.ts           # 검색 관련 타입
├── views/              # 페이지 뷰
│   ├── HomeView.vue        # 메인 홈 페이지
│   └── AboutView.vue       # 브랜드 소개 페이지
└── assets/             # 정적 자산
    ├── base.css            # WaynAI 디자인 시스템
    └── main.css            # 전역 스타일
```

## 🧭 타깃 사용자

- **외국인 관광객**: 맞춤 코스 & 문화 설명이 필요함
- **한국인 자유여행자**: 일정 설계와 정보 탐색을 편하게 하고 싶은 Z세대
- **콘텐츠 크리에이터**: 감성적 장소 추천, 동선 최적화 등 필요
- **디지털 노마드**: 장기 체류자를 위한 지역 기반 추천과 일정 최적화

## 🎨 디자인 시스템

### WaynAI 브랜드 컬러
```css
/* Primary Colors */
--waynai-primary: #1e3c72;
--waynai-primary-light: #2a5298;
--waynai-primary-dark: #152a4f;

/* Secondary Colors */
--waynai-secondary: #667eea;
--waynai-secondary-light: #764ba2;
--waynai-secondary-dark: #5a67d8;

/* Accent Colors */
--waynai-accent: #10b981;
--waynai-accent-light: #34d399;
--waynai-accent-dark: #059669;
```

### 타이포그래피
- **폰트**: Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto
- **크기**: xs(0.75rem) ~ 5xl(3rem)
- **두께**: Light(300) ~ Extrabold(800)

## 📣 슬로건

- "AI가 이끄는 당신만의 여행 길"
- "모르는 길도 걱정 없이, WaynAI와 함께"
- "당신의 다음 목적지, AI가 알고 있어요"
- "길을 묻는 여행자에게, AI로 답하다"
- "Every Way, Your Way — with WaynAI"

## 🔧 API 연동

### 백엔드 연결
- 기본 URL: `http://localhost:8080`
- SSE 엔드포인트: `/api/search/stream`
- 헬스체크: `/api/search/health`

### 검색 요청 예시
```typescript
const request: SearchRequest = {
  query: "서울에서 3일간 문화 여행하고 싶어요",
  destination: "서울",
  theme: "문화",
  days: 3
};
```

### 검색 응답 예시
```typescript
const response: SearchResponse = {
  status: "completed",
  message: "검색 완료",
  step: "course_generation",
  data: "검색 결과 데이터",
  timestamp: "2024-01-01T12:00:00",
  progress: ["의도 분석 완료", "질의 강화 완료", ...]
};
```

## 🗂️ 상태 관리

### Pinia 스토어 (search.ts)
- `state`: 검색 상태 정보
- `startSearch()`: 검색 시작
- `stopSearch()`: 검색 중단
- `clearSearch()`: 검색 초기화
- `checkHealth()`: 서버 상태 확인

## 🚀 개발 가이드

### 새 컴포넌트 추가
1. `src/components/` 디렉토리에 Vue 파일 생성
2. TypeScript 타입 정의 추가
3. 필요한 경우 스토어에 상태 추가
4. WaynAI 디자인 시스템에 맞는 스타일링 적용

### API 연동
1. `src/services/` 디렉토리에 서비스 파일 생성
2. Axios를 사용한 HTTP 요청 구현
3. 에러 핸들링 추가
4. 타입 정의 업데이트

### 상태 관리
1. Pinia 스토어에 새로운 상태 추가
2. 액션과 게터 정의
3. 컴포넌트에서 스토어 사용

## 📱 배포

### 빌드
```bash
npm run build
```

### 배포 파일
- `dist/` 디렉토리에 정적 파일 생성
- Nginx, Apache 등 웹 서버에 배포 가능

## 🔍 문제 해결

### 일반적인 문제
1. **CORS 오류**: 백엔드에서 CORS 설정 확인
2. **SSE 연결 실패**: 네트워크 연결 및 서버 상태 확인
3. **타입 오류**: TypeScript 타입 정의 확인

### 디버깅
- 브라우저 개발자 도구 사용
- Vue DevTools 확장 프로그램 활용
- 네트워크 탭에서 API 요청/응답 확인

## 📄 라이선스

MIT License

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📞 문의

프로젝트에 대한 문의사항이 있으시면 이슈를 생성해 주세요.
