# WaynAI — 사용자 준비 TODO

> 이 문서는 **사용자(오승현) 가 직접 수행해야 하는** 외부 작업 체크리스트입니다.
> 코드로 해결 가능한 항목은 여기에 넣지 않고 `PLAN.md` 에만 기록합니다.
> 각 항목 옆 `[우선순위]` 는 현재 앱/서비스 완성도에 주는 영향도 기준.
>
> 최종 갱신: 2026-04-22

---

## 1. 모바일 네이티브 빌드 도구 설치  `[P0 - 가장 먼저]`

현재 `flutter build web` 만 성공 중. iOS/Android 실기기·에뮬레이터 빌드에는 툴체인이 필요합니다.

### 1-A. Android Studio (가장 쉽고 빠름)
- [ ] https://developer.android.com/studio 에서 다운로드
- [ ] 설치 후 첫 실행 — SDK/Platform-Tools/Emulator 자동 다운로드 (~10GB, 약 30분)
- [ ] 터미널에서 라이선스 동의:
      ```bash
      flutter doctor --android-licenses
      ```
- [ ] 에뮬레이터 하나 생성 (AVD Manager → Pixel 7, API 34 권장)
- [ ] 검증: `flutter doctor` 에서 `[✓] Android toolchain` 확인
- [ ] 검증: `cd waynai-mobile && flutter run -d android` 실행

### 1-B. Xcode (iOS, macOS 필수)
- [ ] App Store 에서 **Xcode** 설치 (~40GB, 1-2시간)
- [ ] 첫 실행 후:
      ```bash
      sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer
      sudo xcodebuild -runFirstLaunch
      ```
- [ ] CocoaPods 설치:
      ```bash
      sudo gem install cocoapods
      ```
- [ ] 검증: `flutter doctor` 에서 `[✓] Xcode` 확인
- [ ] 검증: `cd waynai-mobile && flutter run -d "iPhone 15"` 실행

---

## 2. 관광공사 KorService2 활용 신청  `[P0 - 백엔드 품질 직결]`

> 현재 백엔드는 `KorService2 → KorService1 → 네이버 Local` 3단 폴백.
> 1·2단계 모두 서비스키 미승인(403/500) 이라 항상 네이버 Local 로 떨어집니다.
> 승인 받으면 별도 코드 수정 없이 **관광공사 공식 데이터**(contentid/썸네일/카테고리)가 자동 사용됨.

- [ ] https://www.data.go.kr 접속 → 로그인
- [ ] "**한국관광공사_국문 관광정보 서비스_GW**" 검색 → **활용신청**
  - API 카테고리에서 `KorService2` 쪽을 선택
  - 사용 목적 예시: "WaynAI — AI 여행 어시스턴트. 사용자 좌표 기반 주변 관광지·음식점·숙박 추천 및 RAG 데이터 소스로 활용"
  - 트래픽 예상: 10,000 건/일 정도로 신청
- [ ] 마이페이지 → 활용신청 목록에서 "**승인 완료**" 확인 (보통 자동 승인 1~2시간)
- [ ] 승인 후: 별도 작업 없이 백엔드 재호출만 해보면 `spotCount` 가 관광공사 데이터로 대체되는 것을 확인
      ```bash
      curl -X POST http://localhost:8080/api/gps/nearby \
        -H 'Content-Type: application/json' \
        -d '{"lat":37.5665,"lng":126.9780,"radiusMeters":1500,"limit":5,"contentTypeId":"12"}'
      ```

---

## 3. Google Maps API Key  `[P1 - 지도 기능 도입 시]`

> 현재 Nearby 화면은 리스트 UI. 지도 위 마커를 쓰려면 키 필요.

- [ ] https://console.cloud.google.com — 프로젝트 생성 (이름: `waynai`)
- [ ] **"API 및 서비스 → 라이브러리"** 에서 다음 활성화:
  - [ ] Maps SDK for iOS
  - [ ] Maps SDK for Android
  - [ ] Maps JavaScript API (웹 타겟용)
  - [ ] Places API (선택)
- [ ] **"사용자 인증 정보 → API 키 만들기"** 로 키 발급
- [ ] 키 제한 설정 (권장):
  - iOS 앱: Bundle ID `com.waynai.waynaiMobile`
  - Android 앱: Package `com.waynai.waynai_mobile` + SHA-1 등록
  - HTTP 리퍼러: `localhost:*`, 배포 도메인
- [ ] 결제 카드 등록 (Google Maps 는 무료 쿼터 후 과금)
- [ ] 발급받은 **API Key 값을 공유해주시면** `google_maps_flutter` 통합 진행

---

## 4. FCM 푸시 알림 (Firebase)  `[P2 - 근처 맛집 푸시 기능]`

- [ ] https://console.firebase.google.com — 프로젝트 생성 (이름: `waynai`)
- [ ] **iOS 앱 등록**
  - Bundle ID: `com.waynai.waynaiMobile`
  - `GoogleService-Info.plist` 다운로드 → `waynai-mobile/ios/Runner/` 에 배치
  - [Apple Developer](https://developer.apple.com) → Keys 에서 **APNs 인증 키(.p8)** 발급
  - Firebase 콘솔 → 프로젝트 설정 → Cloud Messaging → APNs 인증 키 업로드
- [ ] **Android 앱 등록**
  - Package name: `com.waynai.waynai_mobile`
  - `google-services.json` 다운로드 → `waynai-mobile/android/app/` 에 배치
- [ ] **백엔드용 서비스 계정**
  - Firebase 콘솔 → 프로젝트 설정 → 서비스 계정 → "새 비공개 키 생성"
  - JSON 파일 다운로드 → `.env` 에 `FIREBASE_CREDENTIALS_PATH` 로 등록
- [ ] 위 파일들을 전달해주시면 Flutter `firebase_messaging` + 백엔드 `FirebaseAdmin SDK` 통합 진행

---

## 5. 인증/로그인 정책 결정  `[P1 - 동행자 공유 등 Phase 2 전반의 선행]`

> 백엔드 인증 모듈 + Flutter 로그인 화면을 붙이기 전에 **어떤 방식을 쓸지 결정**이 필요합니다.

- [ ] 로그인 방식 선택 (복수 선택 가능):
  - [ ] **이메일+비밀번호** — 준비물 없음. BCrypt + JWT 자체 발급
  - [ ] **구글 로그인** — Google Cloud OAuth 2.0 클라이언트 ID 3종 (iOS/Android/Web)
  - [ ] **애플 로그인** — Apple Developer 유료 계정($99/년) + Sign in with Apple 활성화
  - [ ] **카카오 로그인** — https://developers.kakao.com 앱 등록, 네이티브 앱 키 발급
  - [ ] **네이버 로그인** — https://developers.naver.com (이미 검색 API 로 사용 중)
        → "네아로(네이버 아이디로 로그인)" 서비스 추가 신청
- [ ] 게스트 모드 허용 여부: [ ] 예 / [ ] 아니오
- [ ] 다국어(영어/일본어/중국어) 지원 여부: [ ] 예 / [ ] 아니오

---

## 6. 앱 스토어 배포 준비  `[P3 - 실제 출시 직전]`

### iOS — App Store
- [ ] Apple Developer Program 등록 ($99/년)
- [ ] App Store Connect 에 앱 레코드 생성
- [ ] 앱 아이콘 1024×1024 (PNG, 알파 없음)
- [ ] 스크린샷: 6.7" / 6.5" / 5.5" 각 5장 이상
- [ ] 앱 설명 (한국어/영어)
- [ ] 개인정보 처리방침 URL
- [ ] 앱 심사 테스트 계정(이메일+비번)

### Android — Google Play
- [ ] Google Play Console 등록 ($25, 1회 결제)
- [ ] 앱 서명 키(keystore) 생성 및 보관
- [ ] 앱 아이콘 512×512, 피처 그래픽 1024×500
- [ ] 스크린샷: 폰 최소 2장
- [ ] 개인정보 처리방침 URL (필수)
- [ ] 콘텐츠 등급 설문

---

## 7. 운영 인프라  `[P3 - 베타/출시 시]`

- [ ] 프로덕션 도메인 (예: `api.waynai.app`, `www.waynai.app`)
- [ ] SSL 인증서 (Let's Encrypt 또는 ACM 자동)
- [ ] EC2/Cloud Run 등 백엔드 호스팅 비용 계획
- [ ] Gemini API 유료 쿼터 상향 (Google AI Studio)
- [ ] 모니터링 (Sentry 프로젝트 DSN, 또는 Datadog/Grafana)
- [ ] 백엔드 에러 알림 채널 (Slack/Discord Webhook URL)

---

## 📋 지금 당장 해야 할 최소 3개

1. ✅ **Android Studio 설치** — 30분, 에뮬레이터 시연용
2. ✅ **KorService2 활용신청** — 클릭 몇 번 + 대기 1~2시간, 백엔드 데이터 품질 급상승
3. ✅ **Xcode 설치 (옵션, macOS 라면)** — 수시간, 디스크 40GB

나머지(Maps/FCM/로그인/스토어/운영) 는 해당 기능이 실제로 필요한 시점에 진행하시면 됩니다.

---

## ✅ 사용자 준비 없이 제가 바로 진행 가능한 것

사용자가 준비할 필요 없이 제가 언제든 이어서 처리 가능합니다:

- [ ] **자동 주기 폴링 붙이기** — `nearby_screen` 에 60~120초 주기 재호출 + `geolocator.movementStream` 연결 (300m 이동 시 재호출)
- [ ] **웹 브라우저에서 SSE 스트림 end-to-end 검증** — `flutter run -d chrome` 으로 실 동작 확인
- [ ] **백엔드 `/api/gps/nearby` 캐시 (60초)** — 같은 좌표 재호출 시 외부 API 비용 절감
- [ ] **에러/로딩 UX 개선** — shimmer 스켈레톤, 에러 시 재시도 버튼 등
- [ ] **오프라인 캐시** — 마지막 플랜/주변 결과를 로컬(Hive/sqflite) 저장

"해줘" 라고 말씀만 주시면 바로 착수합니다.
