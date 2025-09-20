<template>
  <div class="stream-result">
    <!-- 로딩 상태는 HomeView에서 처리하므로 여기서는 제거 -->

    <!-- 에러 상태 -->
    <div v-if="streamState.error" class="error-container">
      <div class="error-content">
        <div class="error-icon">❌</div>
        <h3 class="error-title">오류가 발생했습니다</h3>
        <p class="error-message">{{ streamState.error }}</p>
        <button @click="handleRetry" class="retry-button">다시 시도</button>
      </div>
    </div>

    <!-- 데이터 표시 -->
    <div v-else-if="streamState.currentData && streamState.currentData.trim().length > 0" class="stream-content">
      <!-- 액션 버튼들 -->
      <div class="action-buttons">
        <button @click="copyToClipboard" class="action-button copy-button" :disabled="isCopying">
          <span v-if="isCopying" class="loading-spinner-small"></span>
          <span v-else class="button-icon">📋</span>
          <span class="button-text">{{ isCopying ? '복사 중...' : '복사' }}</span>
        </button>
        <button @click="downloadPDF" class="action-button pdf-button" :disabled="isGeneratingPDF">
          <span v-if="isGeneratingPDF" class="loading-spinner-small"></span>
          <span v-else class="button-icon">📄</span>
          <span class="button-text">{{ isGeneratingPDF ? '생성 중...' : 'PDF 다운로드' }}</span>
        </button>
      </div>
      
      <div class="stream-text" v-html="formattedData"></div>
    </div>

    <!-- 빈 상태 (데이터도 에러도 없을 때만) -->
    <div v-else class="empty-state">
      <div class="empty-content">
        <div class="empty-icon">🗺️</div>
        <h4 class="empty-title">여행 계획을 생성해보세요</h4>
        <p class="empty-description">원하는 지역이나 키워드를 입력하면 AI가 맞춤형 여행 계획을 생성해드립니다.</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useStreamStore } from '@/stores/stream';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';

// jsPDF 타입 선언
declare module 'jspdf' {
  interface jsPDF {
    addImage(imageData: string, format: string, x: number, y: number, width: number, height: number): void;
    addPage(): void;
    save(filename: string): void;
  }
}

const streamStore = useStreamStore();
const streamState = streamStore.state;
const { formatMarkdown } = streamStore;

const isCopying = ref(false);
const isGeneratingPDF = ref(false);

const formattedData = computed(() => {
  if (!streamState.currentData) return '';
  return formatMarkdown(streamState.currentData);
});

const handleRetry = () => {
  // 재시도 로직은 부모 컴포넌트에서 처리
  window.location.reload();
};

const copyToClipboard = async () => {
  if (!streamState.currentData) return;
  
  isCopying.value = true;
  
  try {
    // HTML 태그를 제거하고 순수 텍스트만 복사
    const textContent = stripHtmlTags(streamState.currentData);
    
    // 모바일 환경에서 더 안정적인 복사 방법 사용
    if (navigator.clipboard && window.isSecureContext) {
      // 최신 브라우저에서 Clipboard API 사용
      await navigator.clipboard.writeText(textContent);
    } else {
      // 구형 브라우저나 모바일에서 fallback 방법 사용
      await fallbackCopyTextToClipboard(textContent);
    }
    
    // 성공 메시지 표시 (간단한 토스트)
    showToast('여행 계획이 클립보드에 복사되었습니다!');
  } catch (error) {
    console.error('복사 실패:', error);
    showToast('복사에 실패했습니다. 다시 시도해주세요.');
  } finally {
    isCopying.value = false;
  }
};

const downloadPDF = async () => {
  if (!streamState.currentData) return;
  
  isGeneratingPDF.value = true;
  
  try {
    // PDF 생성을 위한 HTML 요소 선택
    const element = document.querySelector('.stream-text') as HTMLElement;
    if (!element) {
      throw new Error('PDF 생성할 요소를 찾을 수 없습니다.');
    }
    
    // HTML2Canvas로 캔버스 생성
    const canvas = await html2canvas(element, {
      scale: 2, // 고해상도
      useCORS: true,
      allowTaint: true,
      backgroundColor: '#ffffff',
      width: element.scrollWidth,
      height: element.scrollHeight
    });
    
    // PDF 생성
    const imgData = canvas.toDataURL('image/png');
    const pdf = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4'
    });
    
    const imgWidth = 210; // A4 너비
    const pageHeight = 295; // A4 높이
    const imgHeight = (canvas.height * imgWidth) / canvas.width;
    let heightLeft = imgHeight;
    
    let position = 0;
    
    // 첫 페이지 추가
    pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight);
    heightLeft -= pageHeight;
    
    // 추가 페이지가 필요한 경우
    while (heightLeft >= 0) {
      position = heightLeft - imgHeight;
      pdf.addPage();
      pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight);
      heightLeft -= pageHeight;
    }
    
    // PDF 다운로드
    const fileName = `여행계획_${new Date().toISOString().split('T')[0]}.pdf`;
    pdf.save(fileName);
    
    showToast('PDF가 다운로드되었습니다!');
  } catch (error) {
    console.error('PDF 생성 실패:', error);
    showToast('PDF 생성에 실패했습니다. 다시 시도해주세요.');
  } finally {
    isGeneratingPDF.value = false;
  }
};

const stripHtmlTags = (html: string): string => {
  const temp = document.createElement('div');
  temp.innerHTML = html;
  return temp.textContent || temp.innerText || '';
};

// 모바일 환경을 위한 fallback 복사 함수
const fallbackCopyTextToClipboard = (text: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    // 임시 textarea 요소 생성
    const textArea = document.createElement('textarea');
    textArea.value = text;
    
    // 화면에서 보이지 않도록 설정
    textArea.style.position = 'fixed';
    textArea.style.top = '0';
    textArea.style.left = '0';
    textArea.style.width = '2em';
    textArea.style.height = '2em';
    textArea.style.padding = '0';
    textArea.style.border = 'none';
    textArea.style.outline = 'none';
    textArea.style.boxShadow = 'none';
    textArea.style.background = 'transparent';
    textArea.style.opacity = '0';
    textArea.style.zIndex = '-1';
    
    // DOM에 추가
    document.body.appendChild(textArea);
    
    try {
      // iOS Safari에서 작동하도록 수정
      textArea.focus();
      textArea.select();
      
      // iOS에서 select()가 작동하지 않을 경우를 대비
      if (textArea.setSelectionRange) {
        textArea.setSelectionRange(0, 99999);
      }
      
      // 복사 실행
      const successful = document.execCommand('copy');
      
      if (successful) {
        resolve();
      } else {
        reject(new Error('복사 명령이 실패했습니다.'));
      }
    } catch (err) {
      reject(err);
    } finally {
      // 임시 요소 제거
      document.body.removeChild(textArea);
    }
  });
};

const showToast = (message: string) => {
  // 간단한 토스트 메시지 구현
  const toast = document.createElement('div');
  toast.textContent = message;
  toast.style.cssText = `
    position: fixed;
    top: 20px;
    right: 20px;
    background: #4CAF50;
    color: white;
    padding: 12px 24px;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    z-index: 10000;
    font-size: 14px;
    font-weight: 500;
    animation: slideIn 0.3s ease-out;
  `;
  
  // 애니메이션 CSS 추가
  const style = document.createElement('style');
  style.textContent = `
    @keyframes slideIn {
      from { transform: translateX(100%); opacity: 0; }
      to { transform: translateX(0); opacity: 1; }
    }
  `;
  document.head.appendChild(style);
  
  document.body.appendChild(toast);
  
  // 3초 후 제거
  setTimeout(() => {
    toast.remove();
    style.remove();
  }, 3000);
};
</script>

<style scoped>
.stream-result {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

/* 로딩 상태 */
.loading-container {
  text-align: center;
  padding: 40px 20px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  font-size: 16px;
  color: #666;
  margin: 0;
}

/* 에러 상태 */
.error-container {
  text-align: center;
  padding: 40px 20px;
}

.error-content {
  max-width: 400px;
  margin: 0 auto;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 20px;
}

.error-title {
  font-size: 24px;
  color: #e74c3c;
  margin: 0 0 10px 0;
}

.error-message {
  color: #666;
  margin: 0 0 20px 0;
}

.retry-button {
  background: #3498db;
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
}

.retry-button:hover {
  background: #2980b9;
}

/* 데이터 표시 */
.stream-content {
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  margin: 2rem 0;
  border: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.stream-content:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

/* 액션 버튼들 */
.action-buttons {
  display: flex;
  gap: 1rem;
  padding: 1rem 1.5rem;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  border-bottom: 1px solid #e2e8f0;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.action-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 120px;
  justify-content: center;
}

.copy-button {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
}

.copy-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.pdf-button {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
}

.pdf-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
}

.action-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.button-icon {
  font-size: 1rem;
}

.button-text {
  font-size: 0.875rem;
}

.loading-spinner-small {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 다크모드에서 액션 버튼들 */
.dark .action-buttons {
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
  border-bottom-color: #475569;
}

.dark .copy-button {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}

.dark .pdf-button {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
}

/* 라이트모드 기본 스타일 - 모바일 최적화 */
.stream-text {
  padding: 20px;
  line-height: 1.5;
  color: var(--waynai-text-primary);
  font-size: 0.75rem;
  transition: color 0.3s ease;
}

/* ===== 라이트모드 마크다운 스타일 - 모바일 최적화 ===== */
.stream-text .markdown-h1 {
  color: var(--waynai-text-primary);
  border-bottom: 1px solid #3182ce;
  padding-bottom: 5px;
  margin-bottom: 8px;
  font-size: 0.9rem;
  font-weight: 700;
  text-align: center;
  position: relative;
  transition: color 0.3s ease;
}

.stream-text .markdown-h1::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 50%;
  transform: translateX(-50%);
  width: 25px;
  height: 1px;
  background: linear-gradient(90deg, #3182ce, #63b3ed);
  border-radius: 1px;
}

.stream-text .markdown-h2 {
  color: var(--waynai-text-primary);
  margin-top: 12px;
  margin-bottom: 8px;
  font-size: 0.8rem;
  font-weight: 600;
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  padding: 8px 10px;
  border-radius: 6px;
  border-left: 2px solid #3182ce;
  position: relative;
  transition: color 0.3s ease;
}

.stream-text .markdown-h2::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 2px;
  background: linear-gradient(180deg, #3182ce, #63b3ed);
  border-radius: 0 1px 1px 0;
}

.stream-text .markdown-h3 {
  color: var(--waynai-text-primary);
  margin-top: 10px;
  margin-bottom: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 6px 8px;
  background-color: #f8f9fa;
  border-radius: 4px;
  border-left: 2px solid #68d391;
  transition: color 0.3s ease;
}

.stream-text .markdown-h4 {
  color: var(--waynai-text-primary);
  margin-top: 8px;
  margin-bottom: 5px;
  font-size: 0.7rem;
  font-weight: 600;
}

.stream-text .markdown-h5, .stream-text .markdown-h6 {
  color: var(--waynai-text-primary);
  margin-top: 6px;
  margin-bottom: 4px;
  font-size: 0.65rem;
  font-weight: 600;
}

.stream-text .markdown-paragraph {
  color: var(--waynai-text-primary);
  margin-bottom: 10px;
  line-height: 1.5;
}

.stream-text .markdown-bold {
  color: var(--waynai-text-primary);
  font-weight: 700;
}

.stream-text .markdown-italic {
  color: var(--waynai-text-primary);
  font-style: italic;
}

.stream-text .markdown-code {
  background-color: #f8f9fa;
  color: #e74c3c;
  padding: 1px 4px;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.7em;
  border: 1px solid #e9ecef;
}

.stream-text .markdown-strikethrough {
  text-decoration: line-through;
  color: var(--waynai-text-primary);
}

.stream-text .markdown-link {
  color: #3498db;
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: all 0.3s ease;
}

.stream-text .markdown-link:hover {
  color: #2980b9;
  border-bottom-color: #2980b9;
}

.stream-text .markdown-list, .stream-text .markdown-ordered-list {
  margin: 10px 0;
  padding-left: 0;
  background-color: #f8f9fa;
  border-radius: 6px;
  padding: 8px 10px;
  border-left: 2px solid #4299e1;
  font-size: 0.8rem;
}

.stream-text .markdown-list-item {
  margin: 3px 0;
  line-height: 1.4;
  position: relative;
  padding-left: 15px;
  font-size: 0.8rem;
}

.stream-text .markdown-list-item::before {
  content: '•';
  color: #4299e1;
  font-weight: bold;
  position: absolute;
  left: 0;
  top: 0;
  font-size: 1em;
}

.stream-text .markdown-ordered-list {
  counter-reset: item;
}

.stream-text .markdown-ordered-list .markdown-list-item {
  display: block;
  margin: 2px 0;
}

.stream-text .markdown-ordered-list .markdown-list-item:before {
  content: counter(item) ". ";
  counter-increment: item;
  font-weight: bold;
  color: #3498db;
  font-size: 1em;
}

.stream-text .markdown-blockquote {
  border-left: 3px solid #3498db;
  background-color: #f8f9fa;
  margin: 12px 0;
  padding: 10px 15px;
  font-style: italic;
  color: #5a6c7d;
}

.stream-text .markdown-hr {
  border: none;
  border-top: 1px solid #ecf0f1;
  margin: 20px 0;
  background: linear-gradient(90deg, transparent, #3498db, transparent);
  height: 1px;
}

.stream-text .markdown-linebreak {
  line-height: 1.4;
}

/* 이모지 스타일 */
.stream-text .emoji {
  font-size: 1.2em;
  margin: 0 2px;
  display: inline-block;
  transition: transform 0.2s ease;
}

.stream-text .emoji:hover {
  transform: scale(1.1);
}

.stream-text .emoji-calendar { color: #3498db; }
.stream-text .emoji-money { color: #27ae60; }
.stream-text .emoji-target { color: #e74c3c; }
.stream-text .emoji-building { color: #8e44ad; }
.stream-text .emoji-location { color: #e67e22; }
.stream-text .emoji-food { color: #f39c12; }
.stream-text .emoji-transport { color: #16a085; }
.stream-text .emoji-idea { color: #f1c40f; }
.stream-text .emoji-map { color: #2c3e50; }
.stream-text .emoji-checklist { color: #34495e; }
.stream-text .emoji-note { color: #7f8c8d; }
.stream-text .emoji-car { color: #e67e22; }
.stream-text .emoji-hotel { color: #9b59b6; }
.stream-text .emoji-plane { color: #3498db; }
.stream-text .emoji-beach { color: #1abc9c; }
.stream-text .emoji-mountain { color: #27ae60; }
.stream-text .emoji-cherry { color: #e91e63; }
.stream-text .emoji-bento { color: #ff9800; }
.stream-text .emoji-traditional { color: #795548; }

/* ===== 다크모드 스타일 (배경 및 특수 요소만) ===== */
.dark .stream-content {
  background: rgba(30, 41, 59, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.dark .stream-content:hover {
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
}

/* 다크모드 특수 스타일 */
.dark .stream-text .markdown-h1 {
  border-bottom-color: #60a5fa;
}

.dark .stream-text .markdown-h1::after {
  background: linear-gradient(90deg, #60a5fa, #93c5fd);
}

.dark .stream-text .markdown-h2 {
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
  border-left-color: #60a5fa;
}

.dark .stream-text .markdown-h2::before {
  background: linear-gradient(180deg, #60a5fa, #93c5fd);
}

.dark .stream-text .markdown-h3 {
  background-color: #1e293b;
  border-left-color: #68d391;
}

.dark .stream-text .markdown-code {
  background-color: #1e293b;
  color: #fbbf24;
  border-color: #374151;
}

.dark .stream-text .markdown-link {
  color: #60a5fa;
}

.dark .stream-text .markdown-link:hover {
  color: #93c5fd;
}

.dark .stream-text .markdown-list,
.dark .stream-text .markdown-ordered-list {
  background-color: #1e293b;
  border-left-color: #60a5fa;
}

.dark .stream-text .markdown-list-item::before {
  color: #60a5fa;
}

.dark .stream-text .markdown-ordered-list .markdown-list-item:before {
  color: #60a5fa;
}

.dark .stream-text .markdown-blockquote {
  background-color: #1e293b;
  border-left-color: #60a5fa;
  color: #cbd5e1;
}

.dark .stream-text .markdown-hr {
  border-top-color: #374151;
  background: linear-gradient(90deg, transparent, #60a5fa, transparent);
}

/* 빈 상태 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-content {
  max-width: 400px;
  margin: 0 auto;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.empty-title {
  font-size: 24px;
  color: #2c3e50;
  margin: 0 0 10px 0;
}

.empty-description {
  color: #7f8c8d;
  margin: 0;
  line-height: 1.5;
}

/* 모바일 대응 */
@media (max-width: 768px) {
  .action-buttons {
    flex-direction: column;
    gap: 0.75rem;
    padding: 1rem;
  }
  
  .action-button {
    min-width: auto;
    width: 100%;
    padding: 1rem;
    font-size: 1rem;
  }
  
  .button-text {
    font-size: 1rem;
  }
  
  .stream-text {
    padding: 15px;
    font-size: 0.8rem;
  }
  
  .stream-text .markdown-h1 {
    font-size: 1rem;
  }
  
  .stream-text .markdown-h2 {
    font-size: 0.9rem;
  }
  
  .stream-text .markdown-h3 {
    font-size: 0.85rem;
  }
}

@media (max-width: 480px) {
  .action-buttons {
    padding: 0.75rem;
  }
  
  .action-button {
    padding: 0.875rem;
    font-size: 0.9rem;
  }
  
  .stream-text {
    padding: 12px;
    font-size: 0.75rem;
  }
}
</style>