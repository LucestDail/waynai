<script setup lang="ts">
import SearchInput from '@/components/SearchInput.vue';
import SearchProgress from '@/components/SearchProgress.vue';
import SearchResult from '@/components/SearchResult.vue';
</script>

<template>
  <div class="home">
    <div class="hero-section">
      <div class="hero-content">
        <div class="hero-badge">
          <span class="badge-text">✨ AI 기반 여행 파트너</span>
        </div>
        <h1 class="hero-title">당신의 여행 길을<br>함께 찾아드립니다</h1>
        <p class="hero-subtitle">
          WaynAI는 단순한 정보 검색이 아닌, 당신의 취향과 상황에 맞춘 
          <strong>맞춤형 여행 동반자</strong>입니다
        </p>
        <div class="hero-features">
          <div class="feature-item">
            <div class="feature-icon">🧠</div>
            <span>AI 여행 플래너</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">🗺️</div>
            <span>루트 최적화</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">💬</div>
            <span>대화형 가이드</span>
          </div>
        </div>
      </div>
      <div class="hero-visual">
        <div class="floating-card card-1">
          <div class="card-icon">📍</div>
          <div class="card-content">
            <h4>경복궁</h4>
            <p>조선왕조의 정궁</p>
          </div>
        </div>
        <div class="floating-card card-2">
          <div class="card-icon">🍜</div>
          <div class="card-content">
            <h4>명동칼국수</h4>
            <p>현지 맛집 추천</p>
          </div>
        </div>
        <div class="floating-card card-3">
          <div class="card-icon">🚇</div>
          <div class="card-content">
            <h4>지하철 3호선</h4>
            <p>최적 경로 안내</p>
          </div>
        </div>
      </div>
    </div>

    <div class="search-section">
      <div class="search-container">
        <div class="search-header">
          <h2 class="search-title">어디로 가고 싶으신가요?</h2>
          <p class="search-description">
            키워드나 여행 계획을 자유롭게 입력해보세요. AI가 당신만의 특별한 여행을 설계해드립니다.
          </p>
        </div>
        <SearchInput />
        <SearchProgress />
        <SearchResult />
      </div>
    </div>

    <div class="features-section">
      <div class="features-container">
        <div class="section-header">
          <h2 class="section-title">WaynAI만의 특별한 기능</h2>
          <p class="section-subtitle">AI 기술로 구현하는 차별화된 여행 경험</p>
        </div>
        
        <div class="features-grid">
          <div class="feature-card">
            <div class="feature-icon-wrapper">
              <div class="feature-icon">🤖</div>
            </div>
            <h3 class="feature-title">AI 여행 플래너</h3>
            <p class="feature-description">
              날짜, 예산, 관심사를 입력하면 AI가 최적의 여행 일정을 자동으로 생성합니다. 
              개인화된 여행 경험을 설계해드립니다.
            </p>
          </div>

          <div class="feature-card">
            <div class="feature-icon-wrapper">
              <div class="feature-icon">💬</div>
            </div>
            <h3 class="feature-title">대화형 여행 가이드</h3>
            <p class="feature-description">
              자연어로 자유롭게 질문하세요. 여행지 설명, 역사/문화 맥락, 
              음식 추천까지 GPT 기반으로 답변해드립니다.
            </p>
          </div>

          <div class="feature-card">
            <div class="feature-icon-wrapper">
              <div class="feature-icon">🎯</div>
            </div>
            <h3 class="feature-title">현지 맞춤 추천</h3>
            <p class="feature-description">
              날씨, 시간대, 계절, 현재 트렌드를 고려한 현지 맞춤 추천. 
              신뢰도 높은 현지 정보를 바탕으로 정확한 추천을 제공합니다.
            </p>
          </div>

          <div class="feature-card">
            <div class="feature-icon-wrapper">
              <div class="feature-icon">🗺️</div>
            </div>
            <h3 class="feature-title">루트 최적화</h3>
            <p class="feature-description">
              지도 기반 추천 코스와 실시간 교통 정보를 제공합니다. 
              효율적인 동선으로 더 많은 곳을 둘러볼 수 있습니다.
            </p>
          </div>
        </div>
      </div>
    </div>

    <div class="cta-section">
      <div class="cta-container">
        <h2 class="cta-title">지금 바로 시작해보세요</h2>
        <p class="cta-description">
          "어디로 갈까?" 고민할 때, WaynAI가 방향을 제시해드립니다
        </p>
        <div class="cta-buttons">
          <button class="cta-button primary">여행 계획 시작하기</button>
          <button class="cta-button secondary">더 알아보기</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home {
  min-height: 100vh;
}

.hero-section {
  padding: 4rem 2rem;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4rem;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
  min-height: 80vh;
}

.hero-content {
  color: white;
}

.hero-badge {
  margin-bottom: 2rem;
}

.badge-text {
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-size: 0.875rem;
  font-weight: 500;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.hero-title {
  font-size: 3.5rem;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 1.5rem;
  background: linear-gradient(135deg, #ffffff 0%, #e0e7ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 1.25rem;
  line-height: 1.6;
  margin-bottom: 2rem;
  opacity: 0.9;
  color: #e0e7ff;
}

.hero-features {
  display: flex;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: rgba(255, 255, 255, 0.1);
  padding: 0.75rem 1rem;
  border-radius: 12px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.feature-icon {
  font-size: 1.25rem;
}

.hero-visual {
  position: relative;
  height: 400px;
}

.floating-card {
  position: absolute;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  gap: 1rem;
  animation: float 6s ease-in-out infinite;
}

.card-1 {
  top: 0;
  left: 0;
  animation-delay: 0s;
}

.card-2 {
  top: 50%;
  right: 0;
  animation-delay: 2s;
}

.card-3 {
  bottom: 0;
  left: 20%;
  animation-delay: 4s;
}

.card-icon {
  font-size: 2rem;
}

.card-content h4 {
  margin: 0 0 0.25rem 0;
  font-weight: 600;
  color: #1e3c72;
}

.card-content p {
  margin: 0;
  font-size: 0.875rem;
  color: #6b7280;
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-20px); }
}

.search-section {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 4rem 2rem;
  margin: 2rem auto;
  border-radius: 24px;
  max-width: 1200px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.search-container {
  max-width: 800px;
  margin: 0 auto;
}

.search-header {
  text-align: center;
  margin-bottom: 3rem;
}

.search-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: #1e3c72;
  margin-bottom: 1rem;
}

.search-description {
  font-size: 1.125rem;
  color: #6b7280;
  line-height: 1.6;
}

.features-section {
  padding: 6rem 2rem;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

.features-container {
  max-width: 1200px;
  margin: 0 auto;
}

.section-header {
  text-align: center;
  margin-bottom: 4rem;
}

.section-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: #1e3c72;
  margin-bottom: 1rem;
}

.section-subtitle {
  font-size: 1.125rem;
  color: #6b7280;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 2rem;
}

.feature-card {
  background: white;
  padding: 2.5rem;
  border-radius: 20px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  text-align: center;
}

.feature-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.feature-icon-wrapper {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.5rem;
}

.feature-icon {
  font-size: 2rem;
}

.feature-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1e3c72;
  margin-bottom: 1rem;
}

.feature-description {
  color: #6b7280;
  line-height: 1.6;
}

.cta-section {
  padding: 6rem 2rem;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
  text-align: center;
  color: white;
}

.cta-container {
  max-width: 600px;
  margin: 0 auto;
}

.cta-title {
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 1rem;
}

.cta-description {
  font-size: 1.125rem;
  opacity: 0.9;
  margin-bottom: 2rem;
  line-height: 1.6;
}

.cta-buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
}

.cta-button {
  padding: 1rem 2rem;
  border-radius: 12px;
  font-weight: 600;
  font-size: 1rem;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
  display: inline-block;
}

.cta-button.primary {
  background: white;
  color: #1e3c72;
}

.cta-button.primary:hover {
  background: #f8fafc;
  transform: translateY(-2px);
}

.cta-button.secondary {
  background: transparent;
  color: white;
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.cta-button.secondary:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.5);
}

@media (max-width: 768px) {
  .hero-section {
    grid-template-columns: 1fr;
    gap: 2rem;
    padding: 2rem 1rem;
    text-align: center;
  }

  .hero-title {
    font-size: 2.5rem;
  }

  .hero-subtitle {
    font-size: 1.125rem;
  }

  .hero-features {
    justify-content: center;
  }

  .hero-visual {
    height: 300px;
  }

  .floating-card {
    padding: 1rem;
  }

  .search-section {
    margin: 1rem;
    padding: 2rem 1rem;
  }

  .search-title {
    font-size: 2rem;
  }

  .features-section {
    padding: 3rem 1rem;
  }

  .section-title {
    font-size: 2rem;
  }

  .feature-card {
    padding: 2rem;
  }

  .cta-section {
    padding: 3rem 1rem;
  }

  .cta-title {
    font-size: 2rem;
  }

  .cta-buttons {
    flex-direction: column;
    align-items: center;
  }
}
</style>
