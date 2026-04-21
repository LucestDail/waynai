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
/* ========== AboutView — Material Design 3 ========== */
.about {
  min-height: calc(100vh - 64px);
  background: var(--m3-background);
  color: var(--m3-on-background);
}

.hero-section {
  background: linear-gradient(
    180deg,
    var(--m3-primary-container) 0%,
    color-mix(in srgb, var(--m3-primary-container) 40%, var(--m3-background)) 100%
  );
  padding: clamp(3rem, 8vw, 6rem) 2rem clamp(2.5rem, 6vw, 4rem);
  text-align: center;
  color: var(--m3-on-primary-container);
}

.hero-title {
  font: var(--m3-display-medium);
  color: var(--m3-on-primary-container);
  margin: 0 0 0.75rem;
  letter-spacing: -0.015em;
}

.hero-subtitle {
  font: var(--m3-title-medium);
  color: var(--m3-on-primary-container);
  opacity: 0.88;
  margin: 0;
}

.content-section {
  padding: clamp(2.5rem, 5vw, 4rem) 1.5rem;
  background: var(--m3-background);
}

.container { max-width: 1200px; margin: 0 auto; }

.section-title {
  font: var(--m3-headline-medium);
  color: var(--m3-on-surface);
  margin: 0 0 1.75rem;
  text-align: center;
  letter-spacing: -0.01em;
}

/* Shared card pattern */
.brand-story,
.concept-section,
.features-section,
.target-section,
.slogan-section,
.brand-style-section {
  margin-bottom: 3rem;
}

.story-content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 2.5rem;
  align-items: center;
  background: var(--m3-surface-container-low);
  padding: clamp(1.75rem, 3vw, 2.5rem);
  border-radius: var(--m3-shape-xl);
  box-shadow: var(--m3-elev-1);
}

.story-text h3 {
  font: var(--m3-title-large);
  color: var(--m3-primary);
  margin: 0 0 0.75rem;
}
.story-text p,
.story-text li {
  color: var(--m3-on-surface-variant);
  font: var(--m3-body-large);
  line-height: 1.6;
  margin-bottom: 0.75rem;
}
.story-text ul { margin: 0.75rem 0; padding-left: 1.25rem; }
.story-text strong { color: var(--m3-on-surface); font-weight: 600; }

.story-visual { display: flex; justify-content: center; align-items: center; }
.brand-logo { text-align: center; }
.logo-icon {
  background: var(--m3-primary);
  color: var(--m3-on-primary);
  border-radius: var(--m3-shape-lg);
  padding: 1rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.75rem;
  box-shadow: var(--m3-elev-2);
}
.logo-icon svg { color: currentColor; }
.brand-logo h3 {
  font: var(--m3-headline-small);
  color: var(--m3-on-surface);
  margin: 0;
}

.concept-card {
  background: var(--m3-tertiary-container);
  color: var(--m3-on-tertiary-container);
  padding: clamp(1.75rem, 3vw, 2.5rem);
  border-radius: var(--m3-shape-xl);
  box-shadow: var(--m3-elev-1);
  display: flex;
  align-items: center;
  gap: 1.5rem;
}
.concept-icon { font-size: 3rem; flex-shrink: 0; line-height: 1; }
.concept-content h3 {
  font: var(--m3-title-large);
  color: inherit;
  margin: 0 0 0.5rem;
}
.concept-content p {
  color: inherit;
  opacity: 0.92;
  font: var(--m3-body-large);
  line-height: 1.6;
  margin: 0;
}

.features-grid,
.target-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 1rem;
}

.feature-item,
.target-card {
  background: var(--m3-surface-container-low);
  padding: 1.75rem;
  border-radius: var(--m3-shape-lg);
  box-shadow: var(--m3-elev-1);
  text-align: center;
  transition: box-shadow var(--m3-motion-medium), transform var(--m3-motion-medium);
}
.feature-item:hover,
.target-card:hover { box-shadow: var(--m3-elev-2); transform: translateY(-2px); }

.feature-icon,
.target-icon {
  font-size: 2.25rem;
  margin-bottom: 0.75rem;
  line-height: 1;
}
.feature-item h4,
.target-card h4 {
  font: var(--m3-title-medium);
  color: var(--m3-on-surface);
  margin: 0 0 0.375rem;
}
.feature-item p,
.target-card p {
  color: var(--m3-on-surface-variant);
  font: var(--m3-body-medium);
  line-height: 1.5;
  margin: 0;
}

.slogan-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1rem;
}
.slogan-item {
  background: var(--m3-primary);
  color: var(--m3-on-primary);
  padding: 1.75rem;
  border-radius: var(--m3-shape-lg);
  text-align: center;
  font: var(--m3-title-medium);
  box-shadow: var(--m3-elev-1);
  transition: box-shadow var(--m3-motion-medium), transform var(--m3-motion-medium);
}
.slogan-item:nth-child(2) { background: var(--m3-secondary-container); color: var(--m3-on-secondary-container); }
.slogan-item:nth-child(3) { background: var(--m3-tertiary); color: var(--m3-on-tertiary); }
.slogan-item:nth-child(4) { background: var(--m3-tertiary-container); color: var(--m3-on-tertiary-container); }
.slogan-item:hover { box-shadow: var(--m3-elev-3); transform: translateY(-3px); }

.style-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 1rem;
}
.style-item {
  background: var(--m3-surface-container-low);
  padding: 1.75rem;
  border-radius: var(--m3-shape-lg);
  box-shadow: var(--m3-elev-1);
}
.style-item h4 {
  font: var(--m3-title-medium);
  color: var(--m3-primary);
  margin: 0 0 0.375rem;
}
.style-item p {
  color: var(--m3-on-surface-variant);
  font: var(--m3-body-medium);
  line-height: 1.5;
  margin: 0;
}

@media (max-width: 768px) {
  .hero-section { padding: 2.5rem 1.25rem 2rem; }
  .content-section { padding: 2rem 1rem; }
  .story-content {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }
  .concept-card { flex-direction: column; text-align: center; }
  .features-grid,
  .target-grid,
  .slogan-grid,
  .style-grid { grid-template-columns: 1fr; }
}
</style>
