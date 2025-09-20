import { defineStore } from 'pinia';
import { ref, reactive } from 'vue';
import streamService from '@/services/streamService';

export interface StreamState {
  isStreaming: boolean;
  currentData: string;
  isComplete: boolean;
  error: string | null;
  abortController: AbortController | null;
}

export const useStreamStore = defineStore('stream', () => {
  const state = reactive<StreamState>({
    isStreaming: false,
    currentData: '',
    isComplete: false,
    error: null,
    abortController: null
  });

  const startTravelPlanStream = async (query: string) => {
    // 중복 호출 방지
    if (state.isStreaming) {
      console.log('이미 스트림이 진행 중입니다. 중복 호출을 차단합니다.');
      return;
    }

    try {
      // 이전 스트림이 있으면 중단
      if (state.abortController) {
        state.abortController.abort();
      }

      // 상태 초기화
      state.isStreaming = true;
      state.currentData = '';
      state.isComplete = false;
      state.error = null;

      console.log('여행 계획 스트림 시작:', query);

      const controller = await streamService.generateTravelPlan(
        query,
        (chunk: string) => {
          // 데이터 수신 시 즉시 화면에 반영
          console.log('스트림 스토어에서 데이터 수신:', {
            chunk: chunk,
            chunkLength: chunk.length,
            beforeAppend: state.currentData.length,
            afterAppend: state.currentData.length + chunk.length
          });
          state.currentData += chunk;
          console.log('현재 전체 데이터:', state.currentData);
        },
        () => {
          // 완료 시
          state.isStreaming = false;
          state.isComplete = true;
          state.abortController = null;
          console.log('여행 계획 스트림 완료');
          // 외부 컴포넌트의 isGenerating 해제를 위한 이벤트 발생
          window.dispatchEvent(new CustomEvent('streamComplete'));
        },
        (error: Error) => {
          // 오류 시
          state.isStreaming = false;
          state.error = error.message;
          state.abortController = null;
          console.error('여행 계획 스트림 오류:', error);
          // 외부 컴포넌트의 isGenerating 해제를 위한 이벤트 발생
          window.dispatchEvent(new CustomEvent('streamError'));
        }
      );

      state.abortController = controller;

    } catch (error) {
      console.error('여행 계획 스트림 시작 실패:', error);
      state.isStreaming = false;
      state.error = error instanceof Error ? error.message : '알 수 없는 오류';
    }
  };

  const startChatStream = async (message: string, sessionId: string = 'default-session') => {
    try {
      // 이전 스트림이 있으면 중단
      if (state.abortController) {
        state.abortController.abort();
      }

      // 상태 초기화
      state.isStreaming = true;
      state.currentData = '';
      state.isComplete = false;
      state.error = null;

      console.log('채팅 스트림 시작:', message);

      const controller = await streamService.sendChatMessage(
        message,
        sessionId,
        (chunk: string) => {
          // 데이터 수신 시 즉시 화면에 반영
          state.currentData += chunk;
          console.log('채팅 스트림 데이터 수신:', chunk);
        },
        () => {
          // 완료 시
          state.isStreaming = false;
          state.isComplete = true;
          state.abortController = null;
          console.log('채팅 스트림 완료');
        },
        (error: Error) => {
          // 오류 시
          state.isStreaming = false;
          state.error = error.message;
          state.abortController = null;
          console.error('채팅 스트림 오류:', error);
        }
      );

      state.abortController = controller;

    } catch (error) {
      console.error('채팅 스트림 시작 실패:', error);
      state.isStreaming = false;
      state.error = error instanceof Error ? error.message : '알 수 없는 오류';
    }
  };

  const stopStream = () => {
    if (state.abortController) {
      state.abortController.abort();
      state.abortController = null;
    }
    state.isStreaming = false;
    console.log('스트림 중단');
  };

  const clearStream = () => {
    stopStream();
    state.currentData = '';
    state.isComplete = false;
    state.error = null;
  };

  const formatMarkdown = (text: string): string => {
    if (!text) return '';
    
    console.log('마크다운 처리 시작:', {
      originalLength: text.length,
      preview: text.substring(0, 200) + '...',
      hasDataPrefix: text.includes('data:')
    });
    
    // 텍스트 정리 (data: 접두사도 한 번 더 정리)
    let cleanedText = cleanDataPrefix(text.trim());
    
    // 마크다운을 HTML로 변환
    let html = cleanedText
      // 제목 처리 (더 강력한 패턴)
      .replace(/^#{1}\s+(.*$)/gim, '<h1 class="markdown-h1">$1</h1>')
      .replace(/^#{2}\s+(.*$)/gim, '<h2 class="markdown-h2">$1</h2>')
      .replace(/^#{3}\s+(.*$)/gim, '<h3 class="markdown-h3">$1</h3>')
      .replace(/^#{4}\s+(.*$)/gim, '<h4 class="markdown-h4">$1</h4>')
      .replace(/^#{5}\s+(.*$)/gim, '<h5 class="markdown-h5">$1</h5>')
      .replace(/^#{6}\s+(.*$)/gim, '<h6 class="markdown-h6">$1</h6>')
      // 강조 처리
      .replace(/\*\*(.*?)\*\*/g, '<strong class="markdown-bold">$1</strong>')
      .replace(/\*(.*?)\*/g, '<em class="markdown-italic">$1</em>')
      .replace(/`(.*?)`/g, '<code class="markdown-code">$1</code>')
      .replace(/~~(.*?)~~/g, '<del class="markdown-strikethrough">$1</del>')
      // 링크 처리
      .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" class="markdown-link" target="_blank" rel="noopener noreferrer">$1</a>')
      // 구분선 처리
      .replace(/^---$/gim, '<hr class="markdown-hr">')
      .replace(/^\*\*\*$/gim, '<hr class="markdown-hr">')
      // 인용문 처리
      .replace(/^>\s*(.*$)/gim, '<blockquote class="markdown-blockquote">$1</blockquote>');
    
    // 리스트 처리 (더 정확한 패턴)
    html = html
      .replace(/^[\s]*[-*+]\s+(.*$)/gim, '<li class="markdown-list-item">$1</li>')
      .replace(/^[\s]*(\d+)[.)]\s+(.*$)/gim, '<li class="markdown-list-item markdown-ordered">$1. $2</li>');
    
    // 줄바꿈 처리
    html = html
      .replace(/\n\n+/g, '</p><p class="markdown-paragraph">')
      .replace(/\n/g, '<br class="markdown-linebreak">');
    
    // 문단 래핑
    if (!html.startsWith('<')) {
      html = '<p class="markdown-paragraph">' + html + '</p>';
    }
    
    // 리스트 그룹화 (순서없는 리스트)
    html = html.replace(/(<li class="markdown-list-item">(?!.*markdown-ordered).*?<\/li>)/g, '<ul class="markdown-list">$1</ul>');
    html = html.replace(/<\/ul><ul class="markdown-list">/g, '');
    
    // 순서있는 리스트 그룹화
    html = html.replace(/(<li class="markdown-list-item markdown-ordered">.*?<\/li>)/g, '<ol class="markdown-ordered-list">$1</ol>');
    html = html.replace(/<\/ol><ol class="markdown-ordered-list">/g, '');
    
    // 인용문 정리
    html = html.replace(/<\/blockquote><blockquote class="markdown-blockquote">/g, '<br>');
    
    // 이모지 색상 및 스타일 추가
    html = html
      .replace(/📅/g, '<span class="emoji emoji-calendar">📅</span>')
      .replace(/💰/g, '<span class="emoji emoji-money">💰</span>')
      .replace(/🎯/g, '<span class="emoji emoji-target">🎯</span>')
      .replace(/🏛️/g, '<span class="emoji emoji-building">🏛️</span>')
      .replace(/📍/g, '<span class="emoji emoji-location">📍</span>')
      .replace(/🍜/g, '<span class="emoji emoji-food">🍜</span>')
      .replace(/🚇/g, '<span class="emoji emoji-transport">🚇</span>')
      .replace(/💡/g, '<span class="emoji emoji-idea">💡</span>')
      .replace(/🗺️/g, '<span class="emoji emoji-map">🗺️</span>')
      .replace(/📋/g, '<span class="emoji emoji-checklist">📋</span>')
      .replace(/📝/g, '<span class="emoji emoji-note">📝</span>')
      .replace(/🚗/g, '<span class="emoji emoji-car">🚗</span>')
      .replace(/🏨/g, '<span class="emoji emoji-hotel">🏨</span>')
      .replace(/✈️/g, '<span class="emoji emoji-plane">✈️</span>')
      .replace(/🏖️/g, '<span class="emoji emoji-beach">🏖️</span>')
      .replace(/⛰️/g, '<span class="emoji emoji-mountain">⛰️</span>')
      .replace(/🌸/g, '<span class="emoji emoji-cherry">🌸</span>')
      .replace(/🍱/g, '<span class="emoji emoji-bento">🍱</span>')
      .replace(/🎎/g, '<span class="emoji emoji-traditional">🎎</span>');
    
    console.log('마크다운 처리 완료:', {
      htmlLength: html.length,
      hasHeadings: html.includes('markdown-h'),
      hasLists: html.includes('markdown-list'),
      hasLinks: html.includes('markdown-link')
    });
    
    return html;
  };

  // data: 접두사 정리 함수
  const cleanDataPrefix = (text: string): string => {
    if (!text) return text;
    
    // 모든 줄에서 data: 접두사 제거
    const lines = text.split('\n');
    const cleanedLines = lines.map(line => {
      // 줄의 시작에 있는 data: 제거
      return line.replace(/^data:\s*/, '');
    });
    
    return cleanedLines.join('\n');
  };

  // 일반 데이터 설정 메서드 (스트림이 아닌 일반 응답용)
  const setData = (data: string) => {
    console.log('원본 데이터:', {
      dataLength: data?.length || 0,
      hasData: !!data && data.trim().length > 0,
      startsWithData: data?.startsWith('data:'),
      sampleLines: data?.split('\n').slice(0, 3)
    });
    
    // 모든 줄의 data: 접두사 제거
    let cleanedData = cleanDataPrefix(data);
    
    console.log('정리된 데이터:', {
      originalLength: data?.length || 0,
      cleanedLength: cleanedData?.length || 0,
      cleanedSampleLines: cleanedData?.split('\n').slice(0, 3),
      hasRemainingDataPrefix: cleanedData?.includes('data:')
    });
    
    state.currentData = cleanedData;
    state.isStreaming = false;
    state.isComplete = true;
    state.error = null;
  };

  const setError = (error: string) => {
    console.log('에러 설정:', error);
    state.error = error;
    state.isStreaming = false;
    state.isComplete = false;
  };

  const setStreaming = (streaming: boolean) => {
    console.log('로딩 상태 설정:', streaming);
    state.isStreaming = streaming;
    if (streaming) {
      state.currentData = '';
      state.error = null;
      state.isComplete = false;
    }
  };

  return {
    state,
    startTravelPlanStream,
    startChatStream,
    stopStream,
    clearStream,
    formatMarkdown,
    setData,
    setError,
    setStreaming
  };
});
