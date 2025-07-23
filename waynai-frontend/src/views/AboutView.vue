<template>
  <div class="about">
    <div class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">WaynAI 소개</h1>
        <p class="hero-subtitle">"당신의 길을 함께 찾는 여행 파트너"</p>
      </div>
    </div>

    <div class="content-section">
      <div class="container">
        <div class="brand-story">
          <h2 class="section-title">브랜드 스토리</h2>
          <div class="story-content">
            <div class="story-text">
              <h3>🌐 WaynAI — 브랜드 소개</h3>
              <p>
                <strong>WaynAI</strong>는 다음 단어들의 조합에서 탄생한 이름입니다:
              </p>
              <ul>
                <li><strong>Way</strong>: 길, 방향, 여정 — 여행의 시작과 끝을 모두 포괄</li>
                <li><strong>AI</strong>: 인공지능 — 사용자의 취향과 상황에 맞춘 맞춤형 여행 안내자</li>
              </ul>
              <p>
                "Way + n + AI"는 "Where are you going?" 또는 "Which way?" 같은 여정을 떠올리게 하고,
                동시에 "Way + AI"는 AI가 길을 안내한다는 이미지를 직관적으로 전달합니다.
              </p>
            </div>
            <div class="story-visual">
              <div class="brand-logo">
                <div class="logo-icon">
                  <svg width="64" height="64" viewBox="0 0 24 24" fill="none">
                    <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z" fill="currentColor"/>
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" fill="currentColor"/>
                  </svg>
                </div>
                <h3>WaynAI</h3>
              </div>
            </div>
          </div>
        </div>

        <div class="concept-section">
          <h2 class="section-title">✨ 브랜드 콘셉트</h2>
          <div class="concept-card">
            <div class="concept-icon">🧠</div>
            <div class="concept-content">
              <h3>"당신의 길을 함께 찾는 여행 파트너, WaynAI"</h3>
              <p>
                WaynAI는 단순한 정보 검색 툴이 아닌, 여행자의 목적, 취향, 일정, 감성에 따라 
                여정을 설계해주는 AI 기반 여행 동반자입니다. 말 그대로 "어디로 갈까?"를 
                고민할 때, WaynAI가 방향을 제시해줍니다.
              </p>
            </div>
          </div>
        </div>

        <div class="features-section">
          <h2 class="section-title">🧠 주요 기능</h2>
          <div class="features-grid">
            <div class="feature-item">
              <div class="feature-icon">🤖</div>
              <h4>AI 여행 플래너</h4>
              <p>날짜, 예산, 관심사 입력 → 최적의 여행 일정 자동 생성</p>
            </div>
            <div class="feature-item">
              <div class="feature-icon">💬</div>
              <h4>대화형 여행 가이드</h4>
              <p>여행지 설명, 역사/문화 맥락, 음식 추천 등 GPT 기반 자연어 Q&A</p>
            </div>
            <div class="feature-item">
              <div class="feature-icon">🎯</div>
              <h4>현지 맞춤 추천</h4>
              <p>날씨, 시간대, 계절, 현재 트렌드 기반 추천 (RAG 기반)</p>
            </div>
            <div class="feature-item">
              <div class="feature-icon">🗺️</div>
              <h4>루트 최적화</h4>
              <p>지도 기반 추천 코스 안내, 교통 정보까지 포함</p>
            </div>
          </div>
        </div>

        <div class="target-section">
          <h2 class="section-title">🧭 타깃 사용자</h2>
          <div class="target-grid">
            <div class="target-card">
              <div class="target-icon">🌍</div>
              <h4>외국인 관광객</h4>
              <p>맞춤 코스 & 문화 설명이 필요함</p>
            </div>
            <div class="target-card">
              <div class="target-icon">🇰🇷</div>
              <h4>한국인 자유여행자</h4>
              <p>일정 설계와 정보 탐색을 편하게 하고 싶은 Z세대</p>
            </div>
            <div class="target-card">
              <div class="target-icon">📱</div>
              <h4>콘텐츠 크리에이터</h4>
              <p>감성적 장소 추천, 동선 최적화 등 필요</p>
            </div>
            <div class="target-card">
              <div class="target-icon">💻</div>
              <h4>디지털 노마드</h4>
              <p>장기 체류자를 위한 지역 기반 추천과 일정 최적화</p>
            </div>
          </div>
        </div>

        <div class="slogan-section">
          <h2 class="section-title">📣 슬로건</h2>
          <div class="slogan-grid">
            <div class="slogan-item">"AI가 이끄는 당신만의 여행 길"</div>
            <div class="slogan-item">"모르는 길도 걱정 없이, WaynAI와 함께"</div>
            <div class="slogan-item">"당신의 다음 목적지, AI가 알고 있어요"</div>
            <div class="slogan-item">"길을 묻는 여행자에게, AI로 답하다"</div>
            <div class="slogan-item">"Every Way, Your Way — with WaynAI"</div>
          </div>
        </div>

        <div class="brand-style-section">
          <h2 class="section-title">🎨 브랜드 스타일</h2>
          <div class="style-grid">
            <div class="style-item">
              <h4>로고</h4>
              <p>지도 핀 + 뇌(혹은 AI 아이콘) 결합형 아이콘</p>
            </div>
            <div class="style-item">
              <h4>컬러</h4>
              <p>딥블루 + 미드그린 or 테크틱한 민트톤</p>
            </div>
            <div class="style-item">
              <h4>UI 톤</h4>
              <p>간결한 인터페이스, 감성적인 이미지 강조 (파스텔/모노톤 가능)</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.about {
  min-height: 100vh;
}

.hero-section {
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
  padding: 6rem 2rem 4rem;
  text-align: center;
  color: white;
}

.hero-title {
  font-size: 3.5rem;
  font-weight: 700;
  margin-bottom: 1rem;
  background: linear-gradient(135deg, #ffffff 0%, #e0e7ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 1.25rem;
  opacity: 0.9;
  color: #e0e7ff;
}

.content-section {
  padding: 4rem 2rem;
  background: #f8fafc;
  transition: background 0.3s ease;
}

/* 다크모드에서 콘텐츠 섹션 */
.dark .content-section {
  background: #0f172a;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.section-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: #1e3c72;
  margin-bottom: 2rem;
  text-align: center;
}

.brand-story {
  margin-bottom: 4rem;
}

.story-content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 3rem;
  align-items: center;
  background: white;
  padding: 3rem;
  border-radius: 20px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  transition: background 0.3s ease;
}

/* 다크모드에서 스토리 콘텐츠 */
.dark .story-content {
  background: #1e293b;
}

.story-text h3 {
  font-size: 1.5rem;
  color: #1e3c72;
  margin-bottom: 1rem;
  transition: color 0.3s ease;
}

.story-text p {
  color: #6b7280;
  line-height: 1.6;
  margin-bottom: 1rem;
  transition: color 0.3s ease;
}

/* 다크모드에서 스토리 텍스트 */
.dark .story-text h3 {
  color: #f8fafc;
}

.dark .story-text p {
  color: #cbd5e1;
}

.story-text ul {
  margin: 1rem 0;
  padding-left: 1.5rem;
}

.story-text li {
  color: #6b7280;
  line-height: 1.6;
  margin-bottom: 0.5rem;
}

.story-visual {
  display: flex;
  justify-content: center;
  align-items: center;
}

.brand-logo {
  text-align: center;
}

.logo-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 1rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1rem;
  transition: background 0.3s ease;
}

/* 다크모드에서 로고 아이콘 */
.dark .logo-icon {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}

.logo-icon svg {
  color: white;
}

.brand-logo h3 {
  font-size: 2rem;
  font-weight: 700;
  color: #1e3c72;
  margin: 0;
  transition: color 0.3s ease;
}

/* 다크모드에서 브랜드 로고 */
.dark .brand-logo h3 {
  color: #f8fafc;
}

.concept-section {
  margin-bottom: 4rem;
}

.concept-card {
  background: white;
  padding: 3rem;
  border-radius: 20px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 2rem;
  transition: background 0.3s ease;
}

/* 다크모드에서 컨셉 카드 */
.dark .concept-card {
  background: #1e293b;
}

.concept-icon {
  font-size: 3rem;
  flex-shrink: 0;
}

.concept-content h3 {
  font-size: 1.5rem;
  color: #1e3c72;
  margin-bottom: 1rem;
  transition: color 0.3s ease;
}

.concept-content p {
  color: #6b7280;
  line-height: 1.6;
  margin: 0;
  transition: color 0.3s ease;
}

/* 다크모드에서 컨셉 콘텐츠 */
.dark .concept-content h3 {
  color: #f8fafc;
}

.dark .concept-content p {
  color: #cbd5e1;
}

.features-section {
  margin-bottom: 4rem;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 2rem;
}

.feature-item {
  background: white;
  padding: 2rem;
  border-radius: 16px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  text-align: center;
  transition: transform 0.3s ease, background 0.3s ease;
}

/* 다크모드에서 기능 아이템 */
.dark .feature-item {
  background: #1e293b;
}

.feature-item:hover {
  transform: translateY(-4px);
}

.feature-icon {
  font-size: 2.5rem;
  margin-bottom: 1rem;
}

.feature-item h4 {
  font-size: 1.25rem;
  color: #1e3c72;
  margin-bottom: 0.5rem;
  transition: color 0.3s ease;
}

.feature-item p {
  color: #6b7280;
  line-height: 1.5;
  margin: 0;
  transition: color 0.3s ease;
}

/* 다크모드에서 기능 아이템 텍스트 */
.dark .feature-item h4 {
  color: #f8fafc;
}

.dark .feature-item p {
  color: #cbd5e1;
}

.target-section {
  margin-bottom: 4rem;
}

.target-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 2rem;
}

.target-card {
  background: white;
  padding: 2rem;
  border-radius: 16px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  text-align: center;
  transition: transform 0.3s ease, background 0.3s ease;
}

.target-card:hover {
  transform: translateY(-4px);
}

.target-icon {
  font-size: 2.5rem;
  margin-bottom: 1rem;
}

.target-card h4 {
  font-size: 1.25rem;
  color: #1e3c72;
  margin-bottom: 0.5rem;
  transition: color 0.3s ease;
}

.target-card p {
  color: #6b7280;
  line-height: 1.5;
  margin: 0;
  transition: color 0.3s ease;
}

/* 다크모드에서 타겟 카드 */
.dark .target-card {
  background: #1e293b;
}

.dark .target-card h4 {
  color: #f8fafc;
}

.dark .target-card p {
  color: #cbd5e1;
}

.slogan-section {
  margin-bottom: 4rem;
}

.slogan-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
}

.slogan-item {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 2rem;
  border-radius: 16px;
  text-align: center;
  font-size: 1.125rem;
  font-weight: 500;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease, background 0.3s ease;
}

/* 다크모드에서 슬로건 아이템 */
.dark .slogan-item {
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
}

.slogan-item:hover {
  transform: translateY(-4px);
}

.brand-style-section {
  margin-bottom: 2rem;
}

.style-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
}

.style-item {
  background: white;
  padding: 2rem;
  border-radius: 16px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  transition: background 0.3s ease;
}

.style-item h4 {
  font-size: 1.25rem;
  color: #1e3c72;
  margin-bottom: 0.5rem;
  transition: color 0.3s ease;
}

.style-item p {
  color: #6b7280;
  line-height: 1.5;
  margin: 0;
  transition: color 0.3s ease;
}

/* 다크모드에서 스타일 아이템 */
.dark .style-item {
  background: #1e293b;
}

.dark .style-item h4 {
  color: #f8fafc;
}

.dark .style-item p {
  color: #cbd5e1;
}

@media (max-width: 768px) {
  .hero-section {
    padding: 3rem 1rem;
  }

  .hero-title {
    font-size: 2.5rem;
  }

  .content-section {
    padding: 2rem 1rem;
  }

  .section-title {
    font-size: 2rem;
  }

  .story-content {
    grid-template-columns: 1fr;
    gap: 2rem;
    padding: 2rem;
  }

  .concept-card {
    flex-direction: column;
    text-align: center;
    padding: 2rem;
  }

  .features-grid,
  .target-grid {
    grid-template-columns: 1fr;
  }

  .slogan-grid {
    grid-template-columns: 1fr;
  }

  .style-grid {
    grid-template-columns: 1fr;
  }
}
</style>
