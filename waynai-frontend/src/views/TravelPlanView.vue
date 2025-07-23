<template>
  <div class="travel-plan-view">
    <div class="container">
      <div class="header">
        <h1 class="title">AI 여행 계획 생성</h1>
        <p class="subtitle">목적지와 여행 정보를 입력하면 AI가 맞춤형 여행 계획을 만들어드립니다</p>
      </div>

      <div class="search-section">
        <div class="travel-plan-form">
          <!-- 목적지 정보 -->
          <div class="form-section">
            <h3 class="section-title">목적지 정보</h3>
            <div class="form-row">
              <div class="form-group">
                <label for="area">지역</label>
                <select v-model="selectedArea" @change="updateSigunguOptions" class="form-select">
                  <option value="">지역을 선택하세요</option>
                  <option value="서울">서울</option>
                  <option value="부산">부산</option>
                  <option value="대구">대구</option>
                  <option value="인천">인천</option>
                  <option value="광주">광주</option>
                  <option value="대전">대전</option>
                  <option value="울산">울산</option>
                  <option value="세종">세종</option>
                  <option value="경기">경기도</option>
                  <option value="강원">강원도</option>
                  <option value="충북">충청북도</option>
                  <option value="충남">충청남도</option>
                  <option value="전북">전라북도</option>
                  <option value="전남">전라남도</option>
                  <option value="경북">경상북도</option>
                  <option value="경남">경상남도</option>
                  <option value="제주">제주도</option>
                </select>
              </div>
              <div class="form-group">
                <label for="sigungu">시군구</label>
                <select v-model="selectedSigungu" class="form-select">
                  <option value="">시군구를 선택하세요</option>
                  <option v-for="sigungu in sigunguOptions" :key="sigungu" :value="sigungu">
                    {{ sigungu }}
                  </option>
                </select>
              </div>
            </div>
          </div>

          <!-- 여행 정보 -->
          <div class="form-section">
            <h3 class="section-title">여행 정보</h3>
            <div class="form-row">
              <div class="form-group">
                <label for="duration">여행 일정</label>
                <select v-model="selectedDuration" class="form-select">
                  <option value="">일정을 선택하세요</option>
                  <option value="1">당일치기</option>
                  <option value="2">1박2일</option>
                  <option value="3">2박3일</option>
                  <option value="4">3박4일</option>
                </select>
              </div>
              <div class="form-group">
                <label for="theme">여행 테마</label>
                <select v-model="selectedTheme" class="form-select">
                  <option value="">테마를 선택하세요</option>
                  <option value="문화">문화/역사</option>
                  <option value="자연">자연/풍경</option>
                  <option value="음식">음식/맛집</option>
                  <option value="쇼핑">쇼핑/시장</option>
                  <option value="레저">레저/액티비티</option>
                  <option value="힐링">힐링/휴식</option>
                </select>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label for="transportation">이동 방법</label>
                <div class="checkbox-group">
                  <label class="checkbox-item">
                    <input type="checkbox" v-model="transportation" value="기차">
                    <span>기차</span>
                  </label>
                  <label class="checkbox-item">
                    <input type="checkbox" v-model="transportation" value="도보">
                    <span>도보</span>
                  </label>
                  <label class="checkbox-item">
                    <input type="checkbox" v-model="transportation" value="자동차">
                    <span>자동차</span>
                  </label>
                  <label class="checkbox-item">
                    <input type="checkbox" v-model="transportation" value="전철">
                    <span>전철</span>
                  </label>
                  <label class="checkbox-item">
                    <input type="checkbox" v-model="transportation" value="버스">
                    <span>버스</span>
                  </label>
                  <label class="checkbox-item">
                    <input type="checkbox" v-model="transportation" value="택시">
                    <span>택시</span>
                  </label>
                </div>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label for="keywords">키워드</label>
                <input
                  v-model="keywords"
                  type="text"
                  placeholder="원하는 키워드를 입력하세요 (예: 맛집, 카페, 공원)"
                  class="form-input"
                />
              </div>
            </div>
          </div>

          <!-- 추가 정보 -->
          <div class="form-section">
            <h3 class="section-title">추가 정보</h3>
            <div class="form-row">
              <div class="form-group">
                <label for="budget">예산</label>
                <select v-model="selectedBudget" class="form-select">
                  <option value="">예산을 선택하세요</option>
                  <option value="저렴">저렴 (10만원 이하)</option>
                  <option value="보통">보통 (10-30만원)</option>
                  <option value="고급">고급 (30만원 이상)</option>
                </select>
              </div>
              <div class="form-group">
                <label for="companion">동반자</label>
                <select v-model="selectedCompanion" class="form-select">
                  <option value="">동반자를 선택하세요</option>
                  <option value="혼자">혼자</option>
                  <option value="커플">커플</option>
                  <option value="가족">가족</option>
                  <option value="친구">친구</option>
                  <option value="단체">단체</option>
                </select>
              </div>
            </div>
          </div>

          <!-- 검색 버튼 -->
          <div class="form-actions">
            <button @click="generateTravelPlan" class="submit-button" :disabled="!isFormValid">
              여행 계획 생성하기
            </button>
          </div>
        </div>
        
        <!-- 로딩 상태 -->
        <div v-if="shouldShowLoading" class="loading-container">
          <div class="loading-spinner"></div>
          <p class="loading-text">AI가 여행 계획을 생성하고 있습니다...</p>
        </div>
        
        <!-- 결과 표시 -->
        <transition name="fade">
          <SearchResult v-if="shouldShowResult" />
        </transition>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import SearchInput from '@/components/SearchInput.vue';
import SearchResult from '@/components/SearchResult.vue';
import { useSearchStore } from '@/stores/search';
import { computed, ref } from 'vue';

const searchStore = useSearchStore();
const searchState = searchStore.state;

// 폼 데이터
const selectedArea = ref('');
const selectedSigungu = ref('');
const selectedDuration = ref('');
const selectedTheme = ref('');
const selectedBudget = ref('');
const selectedCompanion = ref('');
const transportation = ref([]);
const keywords = ref('');

// 시군구 옵션
const sigunguOptions = ref<string[]>([]);

// 시군구 데이터 매핑
const sigunguData: Record<string, string[]> = {
  '서울': ['종로구', '중구', '용산구', '성동구', '광진구', '동대문구', '중랑구', '성북구', '강북구', '도봉구', '노원구', '은평구', '서대문구', '마포구', '양천구', '강서구', '구로구', '금천구', '영등포구', '동작구', '관악구', '서초구', '강남구', '송파구', '강동구'],
  '부산': ['부산중구', '부산서구', '부산동구', '영도구', '부산진구', '동래구', '부산남구', '부산북구', '해운대구', '사하구', '금정구', '부산강서구', '연제구', '수영구', '사상구', '기장군'],
  '대구': ['대구중구', '대구동구', '대구서구', '대구남구', '대구북구', '수성구', '달서구', '달성군', '군위군'],
  '인천': ['인천중구', '인천동구', '미추홀구', '연수구', '인천남동구', '부평구', '계양구', '인천서구', '강화군', '옹진군'],
  '광주': ['광주동구', '광주서구', '광주남구', '광주북구', '광산구'],
  '대전': ['대전동구', '대전중구', '대전서구', '유성구', '대덕구'],
  '울산': ['울산중구', '울산남구', '울산동구', '울산북구', '울주군'],
  '세종': ['세종특별자치시'],
  '경기': ['수원시', '성남시', '의정부시', '안양시', '부천시', '광명시', '평택시', '동두천시', '안산시', '고양시', '과천시', '구리시', '남양주시', '오산시', '시흥시', '군포시', '의왕시', '하남시', '용인시', '파주시', '이천시', '안성시', '김포시', '화성시', '광주시', '양주시', '포천시', '여주시', '연천군', '가평군', '양평군'],
  '강원': ['춘천시', '원주시', '강릉시', '동해시', '태백시', '속초시', '삼척시', '홍천군', '횡성군', '영월군', '평창군', '정선군', '철원군', '화천군', '양구군', '인제군', '고성군', '양양군'],
  '충북': ['청주시', '충주시', '제천시', '보은군', '옥천군', '영동군', '증평군', '진천군', '괴산군', '음성군', '단양군'],
  '충남': ['천안시', '공주시', '보령시', '아산시', '서산시', '논산시', '계룡시', '당진시', '금산군', '부여군', '서천군', '청양군', '홍성군', '예산군', '태안군'],
  '전북': ['전주시', '군산시', '익산시', '정읍시', '남원시', '김제시', '완주군', '진안군', '무주군', '장수군', '임실군', '순창군', '고창군', '부안군'],
  '전남': ['목포시', '여수시', '순천시', '나주시', '광양시', '담양군', '곡성군', '구례군', '고흥군', '보성군', '화순군', '장흥군', '강진군', '해남군', '영암군', '무안군', '함평군', '영광군', '장성군', '완도군', '진도군', '신안군'],
  '경북': ['포항시', '경주시', '김천시', '안동시', '구미시', '영주시', '영천시', '상주시', '문경시', '경산시', '군위군', '의성군', '청송군', '영양군', '영덕군', '청도군', '고령군', '성주군', '칠곡군', '예천군', '봉화군', '울진군', '울릉군'],
  '경남': ['창원시', '진주시', '통영시', '사천시', '김해시', '밀양시', '거제시', '양산시', '의령군', '함안군', '창녕군', '고성군', '남해군', '하동군', '산청군', '함양군', '거창군', '합천군'],
  '제주': ['제주시', '서귀포시']
};

const updateSigunguOptions = () => {
  if (selectedArea.value && sigunguData[selectedArea.value]) {
    sigunguOptions.value = sigunguData[selectedArea.value];
  } else {
    sigunguOptions.value = [];
  }
  selectedSigungu.value = '';
};

const generateTravelPlan = () => {
  if (!isFormValid.value) return;
  
  // 키워드와 테마를 합쳐서 요청
  const searchQuery = `${selectedArea.value} ${selectedSigungu.value} ${keywords.value} ${selectedTheme.value}`.trim();
  
  // 검색 스토어에 요청
  searchStore.startSearch({
    query: searchQuery,
    destination: `${selectedArea.value} ${selectedSigungu.value}`,
    days: parseInt(selectedDuration.value)
  });
};

const isFormValid = computed(() => {
  return selectedArea.value && selectedSigungu.value && selectedDuration.value && selectedTheme.value;
});



const shouldShowResult = computed(() => {
  return (searchState.result !== null && searchState.result !== undefined) || 
         searchState.error || 
         searchState.currentStatus === 'completed';
});

const shouldShowLoading = computed(() => {
  return searchState.isSearching;
});
</script>

<style scoped>
.travel-plan-view {
  min-height: 100vh;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
  padding: 2rem 1rem;
  transition: background 0.3s ease;
}

/* 다크모드에서 여행 계획 배경 */
.dark .travel-plan-view {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  text-align: center;
  margin-bottom: 3rem;
  color: white;
}

.title {
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 1rem;
  background: linear-gradient(135deg, #ffffff 0%, #e0e7ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  font-size: 1.2rem;
  opacity: 0.9;
  color: #e0e7ff;
}

.search-section {
  background: white;
  border-radius: 16px;
  padding: 2rem;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  transition: background 0.3s ease;
}

/* 다크모드에서 검색 섹션 */
.dark .search-section {
  background: #1e293b;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.3), 0 10px 10px -5px rgba(0, 0, 0, 0.2);
}

.travel-plan-form {
  max-width: 800px;
  margin: 0 auto;
}

.form-section {
  margin-bottom: 2rem;
  padding: 1.5rem;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.section-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e3c72;
  margin-bottom: 1rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group label {
  font-weight: 600;
  color: #374151;
  font-size: 0.9rem;
}

.form-select,
.form-input {
  padding: 0.75rem;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 0.9rem;
  outline: none;
  transition: border-color 0.3s ease;
  background: white;
  color: #374151;
}

.form-select:focus,
.form-input:focus {
  border-color: #667eea;
}

/* 다크모드에서 폼 요소 스타일 */
.dark .form-select,
.dark .form-input {
  background: #1e293b;
  border-color: #475569;
  color: #f1f5f9;
}

.dark .form-select:focus,
.dark .form-input:focus {
  border-color: #60a5fa;
}

.dark .form-group label {
  color: #e2e8f0;
}

.dark .section-title {
  color: #f8fafc;
}

.dark .form-section {
  background: #1e293b;
  border-color: #475569;
}

.checkbox-group {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 0.5rem;
}

.checkbox-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 6px;
  transition: background-color 0.3s ease;
}

.checkbox-item:hover {
  background: #f3f4f6;
}

/* 다크모드에서 체크박스 아이템 */
.dark .checkbox-item:hover {
  background: #374151;
}

.checkbox-item input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #667eea;
}

.form-actions {
  text-align: center;
  margin-top: 2rem;
}

.submit-button {
  padding: 1rem 2rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease;
}

/* 다크모드에서 제출 버튼 */
.dark .submit-button {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}

.submit-button:hover:not(:disabled) {
  transform: translateY(-2px);
}

.submit-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading-container {
  text-align: center;
  padding: 3rem 1rem;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f4f6;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  color: #6b7280;
  font-size: 1.1rem;
  transition: color 0.3s ease;
}

/* 다크모드에서 로딩 텍스트 */
.dark .loading-text {
  color: #9ca3af;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

/* 모바일 대응 */
@media (max-width: 768px) {
  .travel-plan-view {
    padding: 1rem 0.5rem;
  }
  
  .title {
    font-size: 2rem;
  }
  
  .subtitle {
    font-size: 1rem;
  }
  
  .search-section {
    padding: 1.5rem;
    margin: 0 0.5rem;
  }
  
  .header {
    margin-bottom: 2rem;
  }
  
  .form-row {
    grid-template-columns: 1fr;
  }
  
  .checkbox-group {
    grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
  }
  
  .form-section {
    padding: 1rem;
  }
}

@media (max-width: 480px) {
  .title {
    font-size: 1.8rem;
  }
  
  .search-section {
    padding: 1rem;
  }
}
</style> 