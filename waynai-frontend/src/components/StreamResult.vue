<template>
  <div class="stream-result">
    <!-- Error -->
    <div v-if="streamState.error" class="error-container">
      <div class="error-icon">!</div>
      <div>
        <h3 class="error-title">오류가 발생했습니다</h3>
        <p class="error-message">{{ streamState.error }}</p>
      </div>
      <button @click="handleRetry" class="retry-button">다시 시도</button>
    </div>

    <!-- Structured plan -->
    <div v-else-if="plan" class="result-card">
      <div class="browser-bar" aria-hidden="true">
        <span class="dot dot-r"></span>
        <span class="dot dot-y"></span>
        <span class="dot dot-g"></span>
        <div class="url-pill">waynai.app / plan / {{ plan.destination || 'trip' }}</div>
      </div>

      <header class="plan-header">
        <div class="plan-meta">
          <span class="eyebrow">AI 여행 계획</span>
          <h2 class="plan-title">{{ plan.destination || plan.theme || '맞춤 여행' }}</h2>
          <p class="plan-sub" v-if="plan.summary">{{ plan.summary }}</p>
        </div>
        <div class="plan-chips">
          <span v-if="plan.duration" class="chip">{{ plan.duration }}</span>
          <span v-if="plan.theme" class="chip">{{ plan.theme }}</span>
          <span v-if="plan.budget" class="chip">예산 · {{ plan.budget }}</span>
          <span v-if="streamState.progress.model" class="chip chip-mono">{{ streamState.progress.model }}</span>
        </div>
      </header>

      <div class="plan-actions">
        <button @click="copyToClipboard" class="action-button" :disabled="isCopying">
          <span v-if="isCopying" class="spinner-mini"></span>
          <span v-else>클립보드 복사</span>
        </button>
        <button @click="downloadPDF" class="action-button primary" :disabled="isGeneratingPDF">
          <span v-if="isGeneratingPDF" class="spinner-mini"></span>
          <span v-else>PDF 다운로드</span>
        </button>
      </div>

      <ol class="timeline" v-if="plan.itinerary && plan.itinerary.length">
        <li v-for="(day, idx) in plan.itinerary" :key="idx" class="day-block">
          <div class="day-mark">
            <span class="day-number">Day {{ day.day ?? idx + 1 }}</span>
          </div>
          <div class="day-body">
            <h3 class="day-title">{{ day.title || `Day ${day.day ?? idx + 1}` }}</h3>
            <p v-if="day.overview" class="day-overview">{{ day.overview }}</p>

            <ul class="spot-list" v-if="day.spots && day.spots.length">
              <li v-for="(spot, i) in day.spots" :key="i" class="spot">
                <span class="spot-time">{{ spot.visitTime || '—' }}</span>
                <div class="spot-meat">
                  <strong class="spot-name">{{ spot.name }}</strong>
                  <span v-if="spot.address" class="spot-addr">{{ spot.address }}</span>
                  <span v-if="spot.activity" class="spot-activity">{{ spot.activity }}</span>
                  <span v-if="spot.notes" class="spot-notes">{{ spot.notes }}</span>
                </div>
              </li>
            </ul>

            <ul class="bullet" v-else-if="day.activities && day.activities.length">
              <li v-for="(act, i) in day.activities" :key="i">{{ act }}</li>
            </ul>

            <div class="day-foot" v-if="day.meals || day.estimatedCost || day.transportation">
              <span v-if="day.transportation"><em>이동</em> {{ day.transportation }}</span>
              <span v-if="day.meals"><em>식사</em> {{ day.meals }}</span>
              <span v-if="day.estimatedCost"><em>예산</em> {{ day.estimatedCost }}</span>
            </div>
          </div>
        </li>
      </ol>

      <section v-if="plan.tips && plan.tips.length" class="tips-block">
        <h4 class="tips-title">여행 팁</h4>
        <ul class="bullet">
          <li v-for="(tip, i) in plan.tips" :key="i">{{ tip }}</li>
        </ul>
      </section>
    </div>

    <!-- Fallback: raw AI text -->
    <div v-else-if="hasText" class="result-card bubble-card">
      <div class="browser-bar" aria-hidden="true">
        <span class="dot dot-r"></span>
        <span class="dot dot-y"></span>
        <span class="dot dot-g"></span>
        <div class="url-pill">waynai · AI stream</div>
      </div>
      <div class="plan-actions">
        <button @click="copyToClipboard" class="action-button" :disabled="isCopying">
          <span v-if="isCopying" class="spinner-mini"></span>
          <span v-else>클립보드 복사</span>
        </button>
        <button @click="downloadPDF" class="action-button primary" :disabled="isGeneratingPDF">
          <span v-if="isGeneratingPDF" class="spinner-mini"></span>
          <span v-else>PDF 다운로드</span>
        </button>
      </div>
      <div class="ai-bubble">
        <div class="ai-avatar">AI</div>
        <div class="stream-text" v-html="formattedData"></div>
      </div>
    </div>

    <!-- Empty -->
    <div v-else class="empty-state">
      <span class="empty-tag">Ready</span>
      <h4 class="empty-title">여행 계획을 생성해보세요</h4>
      <p class="empty-desc">한 줄 질의만으로 관광공사·네이버·Gemini 3가 조율된 실시간 계획이 완성됩니다.</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useStreamStore } from '@/stores/stream';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';

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

const plan = computed(() => streamState.plan);
const hasText = computed(() => !!streamState.currentData && streamState.currentData.trim().length > 0);

const formattedData = computed(() => {
  if (!streamState.currentData) return '';
  return formatMarkdown(streamState.currentData);
});

const handleRetry = () => {
  window.location.reload();
};

const copyToClipboard = async () => {
  const source = plan.value ? JSON.stringify(plan.value, null, 2) : streamState.currentData;
  if (!source) return;
  isCopying.value = true;
  try {
    const text = plan.value ? source : stripHtmlTags(source);
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
    } else {
      await fallbackCopyTextToClipboard(text);
    }
    showToast('여행 계획이 클립보드에 복사되었습니다.');
  } catch (error) {
    console.error('복사 실패:', error);
    showToast('복사에 실패했습니다. 다시 시도해주세요.');
  } finally {
    isCopying.value = false;
  }
};

const downloadPDF = async () => {
  isGeneratingPDF.value = true;
  try {
    const element = (document.querySelector('.timeline') || document.querySelector('.stream-text')) as HTMLElement | null;
    if (!element) throw new Error('PDF 생성할 요소가 없습니다.');
    const canvas = await html2canvas(element, { scale: 2, useCORS: true, allowTaint: true, backgroundColor: '#f5f0e8' });
    const imgData = canvas.toDataURL('image/png');
    const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    const imgWidth = 210;
    const pageHeight = 295;
    const imgHeight = (canvas.height * imgWidth) / canvas.width;
    let heightLeft = imgHeight;
    let position = 0;
    pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight);
    heightLeft -= pageHeight;
    while (heightLeft >= 0) {
      position = heightLeft - imgHeight;
      pdf.addPage();
      pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight);
      heightLeft -= pageHeight;
    }
    const fileName = `waynai_plan_${new Date().toISOString().split('T')[0]}.pdf`;
    pdf.save(fileName);
    showToast('PDF가 다운로드되었습니다.');
  } catch (error) {
    console.error('PDF 생성 실패:', error);
    showToast('PDF 생성에 실패했습니다.');
  } finally {
    isGeneratingPDF.value = false;
  }
};

const stripHtmlTags = (html: string): string => {
  const temp = document.createElement('div');
  temp.innerHTML = html;
  return temp.textContent || temp.innerText || '';
};

const fallbackCopyTextToClipboard = (text: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.cssText = 'position:fixed;top:0;left:0;opacity:0;z-index:-1';
    document.body.appendChild(textArea);
    try {
      textArea.focus();
      textArea.select();
      if (textArea.setSelectionRange) textArea.setSelectionRange(0, 99999);
      const ok = document.execCommand('copy');
      ok ? resolve() : reject(new Error('copy failed'));
    } catch (err) {
      reject(err);
    } finally {
      document.body.removeChild(textArea);
    }
  });
};

const showToast = (message: string) => {
  const toast = document.createElement('div');
  toast.textContent = message;
  toast.style.cssText = `
    position: fixed; top: 24px; right: 24px;
    background: #1a3a4a; color: #f5f0e8;
    padding: 12px 20px; border-radius: 12px;
    box-shadow: 0 12px 30px -12px rgba(26,58,74,0.55);
    z-index: 10000; font-family: 'DM Sans', system-ui, sans-serif; font-size: 0.875rem;
  `;
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 2800);
};
</script>

<style scoped>
.stream-result {
  max-width: 960px;
  margin: 2rem auto 0;
  color: var(--wa-text-dark);
}

/* ---- Card frame ---- */
.result-card {
  background: var(--wa-cream);
  border: 1px solid color-mix(in srgb, var(--wa-sand) 55%, transparent);
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 30px 60px -30px color-mix(in srgb, var(--wa-ocean) 40%, transparent);
}

.browser-bar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  background: color-mix(in srgb, var(--wa-sand) 65%, var(--wa-cream));
  border-bottom: 1px solid color-mix(in srgb, var(--wa-sand) 55%, transparent);
}
.dot { width: 10px; height: 10px; border-radius: 50%; }
.dot-r { background: #e27763; }
.dot-y { background: #e3b34b; }
.dot-g { background: #87a590; }
.url-pill {
  margin-left: auto;
  font-family: 'SF Mono', Menlo, monospace;
  font-size: 0.75rem;
  color: var(--wa-text-mid);
  background: var(--wa-warm);
  border: 1px solid color-mix(in srgb, var(--wa-sand) 55%, transparent);
  padding: 4px 12px;
  border-radius: 999px;
}

/* ---- Plan header ---- */
.plan-header {
  display: flex;
  justify-content: space-between;
  gap: 1.5rem;
  padding: 2rem 2rem 1.25rem;
  flex-wrap: wrap;
}
.plan-meta { display: flex; flex-direction: column; gap: 0.5rem; max-width: 640px; }
.eyebrow {
  font-family: var(--wa-font-sans);
  font-size: 0.6875rem;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--wa-terra);
}
.plan-title {
  font-family: var(--wa-font-serif);
  font-size: clamp(1.75rem, 2vw + 1rem, 2.5rem);
  color: var(--wa-ocean);
  letter-spacing: -0.01em;
  margin: 0;
  line-height: 1.15;
}
.plan-sub {
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
  color: var(--wa-text-mid);
  line-height: 1.55;
  margin: 0;
}

.plan-chips { display: flex; flex-wrap: wrap; gap: 0.5rem; align-items: flex-start; }
.chip {
  display: inline-flex;
  align-items: center;
  padding: 0.375rem 0.75rem;
  border-radius: 999px;
  background: var(--wa-warm);
  border: 1px solid color-mix(in srgb, var(--wa-sand) 55%, transparent);
  font-family: var(--wa-font-sans);
  font-size: 0.75rem;
  color: var(--wa-text-dark);
}
.chip-mono {
  font-family: 'SF Mono', Menlo, monospace;
  font-size: 0.6875rem;
  background: var(--wa-ocean);
  color: var(--wa-cream);
  border-color: var(--wa-ocean);
}

/* ---- Actions ---- */
.plan-actions {
  display: flex;
  gap: 0.625rem;
  padding: 0 2rem 1.25rem;
  flex-wrap: wrap;
}
.action-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 1.25rem;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--wa-ocean) 25%, transparent);
  background: var(--wa-warm);
  color: var(--wa-ocean);
  font-family: var(--wa-font-sans);
  font-size: 0.8125rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 150ms ease;
}
.action-button:hover:not(:disabled) { background: var(--wa-ocean); color: var(--wa-cream); }
.action-button.primary {
  background: var(--wa-ocean);
  color: var(--wa-cream);
  border-color: var(--wa-ocean);
}
.action-button.primary:hover:not(:disabled) { background: var(--wa-dusk); }
.action-button:disabled { opacity: 0.55; cursor: not-allowed; }
.spinner-mini {
  width: 14px;
  height: 14px;
  border: 2px solid color-mix(in srgb, currentColor 30%, transparent);
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 900ms linear infinite;
}

/* ---- Timeline ---- */
.timeline {
  list-style: none;
  padding: 0 2rem 2rem;
  margin: 0;
  position: relative;
}
.timeline::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: calc(2rem + 18px);
  width: 2px;
  background: linear-gradient(180deg, var(--wa-terra) 0%, var(--wa-sage) 100%);
  border-radius: 999px;
  opacity: 0.5;
}
.day-block {
  display: grid;
  grid-template-columns: 48px 1fr;
  gap: 1.25rem;
  padding: 1.25rem 0;
  position: relative;
}
.day-mark {
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
}
.day-number {
  font-family: var(--wa-font-sans);
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--wa-cream);
  background: var(--wa-ocean);
  padding: 0.375rem 0.625rem;
  border-radius: 999px;
  position: relative;
  z-index: 1;
  white-space: nowrap;
  box-shadow: 0 4px 10px -3px color-mix(in srgb, var(--wa-ocean) 55%, transparent);
}
.day-body {
  background: var(--wa-warm);
  border-radius: 20px;
  padding: 1.25rem 1.5rem;
  border: 1px solid color-mix(in srgb, var(--wa-sand) 50%, transparent);
}
.day-title {
  font-family: var(--wa-font-serif);
  font-size: 1.375rem;
  color: var(--wa-ocean);
  margin: 0 0 0.375rem;
  font-weight: 500;
  letter-spacing: -0.01em;
}
.day-overview {
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
  color: var(--wa-text-mid);
  line-height: 1.55;
  margin: 0 0 0.875rem;
}

.spot-list { list-style: none; padding: 0; margin: 0.5rem 0 0; display: flex; flex-direction: column; gap: 0.625rem; }
.spot {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 0.75rem;
  padding: 0.75rem 0.875rem;
  background: var(--wa-cream);
  border-radius: 14px;
  border: 1px solid color-mix(in srgb, var(--wa-sand) 45%, transparent);
}
.spot-time {
  font-family: 'SF Mono', Menlo, monospace;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--wa-terra);
  align-self: flex-start;
  padding-top: 2px;
}
.spot-meat { display: flex; flex-direction: column; gap: 3px; }
.spot-name {
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
  color: var(--wa-text-dark);
  font-weight: 600;
}
.spot-addr, .spot-activity, .spot-notes {
  font-family: var(--wa-font-sans);
  font-size: 0.8125rem;
  color: var(--wa-text-mid);
  line-height: 1.45;
}
.spot-activity { color: var(--wa-sage); }

.day-foot {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-top: 0.875rem;
  padding-top: 0.875rem;
  border-top: 1px dashed color-mix(in srgb, var(--wa-sand) 70%, transparent);
  font-family: var(--wa-font-sans);
  font-size: 0.8125rem;
  color: var(--wa-text-mid);
}
.day-foot em {
  font-style: normal;
  font-weight: 600;
  color: var(--wa-ocean);
  margin-right: 6px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  font-size: 0.6875rem;
}

.bullet { list-style: disc; padding-left: 1.25rem; margin: 0.5rem 0 0; font-family: var(--wa-font-sans); font-size: 0.9375rem; color: var(--wa-text-dark); }
.bullet li { margin: 4px 0; line-height: 1.5; }

.tips-block {
  margin: 0 2rem 2rem;
  padding: 1.25rem 1.5rem;
  background: color-mix(in srgb, var(--wa-sage) 18%, var(--wa-warm));
  border-radius: 18px;
  border: 1px solid color-mix(in srgb, var(--wa-sage) 30%, transparent);
}
.tips-title {
  font-family: var(--wa-font-serif);
  font-size: 1.125rem;
  color: var(--wa-ocean);
  margin: 0 0 0.5rem;
  font-weight: 500;
  font-style: italic;
}

/* ---- Fallback bubble ---- */
.ai-bubble {
  display: grid;
  grid-template-columns: 44px 1fr;
  gap: 0.875rem;
  padding: 1.5rem 2rem 2rem;
}
.ai-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--wa-ocean);
  color: var(--wa-cream);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--wa-font-serif);
  font-style: italic;
  font-weight: 500;
}
.stream-text {
  background: var(--wa-warm);
  border-radius: 18px;
  padding: 1.25rem 1.5rem;
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
  color: var(--wa-text-dark);
  line-height: 1.65;
  border: 1px solid color-mix(in srgb, var(--wa-sand) 45%, transparent);
}
.stream-text :deep(.markdown-h1),
.stream-text :deep(.markdown-h2),
.stream-text :deep(.markdown-h3) {
  font-family: var(--wa-font-serif);
  color: var(--wa-ocean);
  font-weight: 500;
  margin: 1rem 0 0.5rem;
  line-height: 1.25;
}
.stream-text :deep(.markdown-h1) { font-size: 1.5rem; }
.stream-text :deep(.markdown-h2) { font-size: 1.25rem; }
.stream-text :deep(.markdown-h3) { font-size: 1.0625rem; }
.stream-text :deep(.markdown-bold) { color: var(--wa-ocean); }
.stream-text :deep(.markdown-italic) { color: var(--wa-terra); font-style: italic; }
.stream-text :deep(.markdown-code) {
  background: var(--wa-cream);
  border: 1px solid color-mix(in srgb, var(--wa-sand) 55%, transparent);
  color: var(--wa-terra);
  padding: 2px 6px;
  border-radius: 6px;
  font-family: 'SF Mono', Menlo, monospace;
  font-size: 0.85em;
}
.stream-text :deep(.markdown-list),
.stream-text :deep(.markdown-ordered-list) {
  background: var(--wa-cream);
  border-left: 3px solid var(--wa-terra);
  padding: 0.75rem 1rem;
  border-radius: 10px;
  margin: 0.75rem 0;
}
.stream-text :deep(.markdown-blockquote) {
  background: color-mix(in srgb, var(--wa-sage) 20%, var(--wa-warm));
  border-left: 3px solid var(--wa-sage);
  padding: 0.75rem 1rem;
  border-radius: 10px;
  color: var(--wa-text-mid);
  font-style: italic;
}
.stream-text :deep(.markdown-hr) {
  border: none;
  border-top: 1px dashed color-mix(in srgb, var(--wa-sand) 70%, transparent);
  margin: 1.25rem 0;
}
.stream-text :deep(.markdown-link) {
  color: var(--wa-terra);
  border-bottom: 1px solid color-mix(in srgb, var(--wa-terra) 40%, transparent);
  text-decoration: none;
}

/* ---- Error ---- */
.error-container {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 1.25rem;
  align-items: center;
  padding: 1.5rem;
  background: color-mix(in srgb, var(--wa-terra) 12%, var(--wa-cream));
  border: 1px solid color-mix(in srgb, var(--wa-terra) 30%, transparent);
  border-radius: 20px;
}
.error-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--wa-terra);
  color: var(--wa-cream);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--wa-font-serif);
  font-size: 1.5rem;
  font-weight: 600;
}
.error-title {
  font-family: var(--wa-font-serif);
  font-size: 1.125rem;
  color: var(--wa-ocean);
  margin: 0;
  font-weight: 500;
}
.error-message {
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
  color: var(--wa-text-mid);
  margin: 4px 0 0;
}
.retry-button {
  padding: 0.625rem 1.25rem;
  border-radius: 999px;
  background: var(--wa-terra);
  color: var(--wa-cream);
  border: none;
  font-family: var(--wa-font-sans);
  font-size: 0.8125rem;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
}
.retry-button:hover { background: color-mix(in srgb, var(--wa-terra) 80%, #000); }

/* ---- Empty ---- */
.empty-state {
  text-align: center;
  padding: 3rem 1.5rem;
  background: var(--wa-cream);
  border: 1px dashed color-mix(in srgb, var(--wa-sand) 70%, transparent);
  border-radius: 24px;
}
.empty-tag {
  display: inline-block;
  font-family: var(--wa-font-sans);
  font-size: 0.6875rem;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--wa-terra);
  background: color-mix(in srgb, var(--wa-terra) 12%, transparent);
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  margin-bottom: 0.75rem;
}
.empty-title {
  font-family: var(--wa-font-serif);
  font-size: 1.5rem;
  color: var(--wa-ocean);
  margin: 0 0 0.5rem;
  font-weight: 500;
}
.empty-desc {
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
  color: var(--wa-text-mid);
  line-height: 1.55;
  max-width: 520px;
  margin: 0 auto;
}

@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

@media (max-width: 768px) {
  .plan-header { padding: 1.5rem 1.25rem 1rem; }
  .plan-actions { padding: 0 1.25rem 1rem; }
  .timeline { padding: 0 1.25rem 1.5rem; }
  .timeline::before { left: calc(1.25rem + 12px); }
  .day-block { grid-template-columns: 36px 1fr; gap: 0.75rem; }
  .day-number { font-size: 0.6875rem; padding: 0.25rem 0.5rem; }
  .day-body { padding: 1rem; border-radius: 16px; }
  .spot { grid-template-columns: 64px 1fr; }
  .ai-bubble { grid-template-columns: 36px 1fr; padding: 1.25rem; gap: 0.625rem; }
  .ai-avatar { width: 36px; height: 36px; }
}
</style>
