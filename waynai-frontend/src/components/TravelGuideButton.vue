<template>
  <div class="travel-guide-container">
    <!-- 상담사 버튼 -->
    <button 
      class="guide-button"
      @click="toggleGuide"
      :class="{ 'active': isGuideOpen }"
    >
      <div class="guide-icon">🤖</div>
      <span class="guide-text">여행 가이드</span>
    </button>

    <!-- 상담 채팅창 -->
    <div v-if="isGuideOpen" class="guide-chat">
      <div class="chat-header">
        <h3>{{ t('travel_guide.title') }}</h3>
        <button class="close-button" @click="toggleGuide">×</button>
      </div>
      
      <div class="chat-messages" ref="chatMessages">
        <div 
          v-for="(message, index) in messages" 
          :key="index"
          :class="['message', message.type]"
        >
          <div class="message-content">{{ message.content }}</div>
          <div class="message-time">{{ message.time }}</div>
        </div>
      </div>
      
      <div class="chat-input">
        <input
          v-model="userInput"
          @keyup.enter="sendMessage"
          :placeholder="t('travel_guide.placeholder')"
          class="input-field"
        />
        <button @click="sendMessage" class="send-button">{{ t('travel_guide.send') }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue';
import { useLanguageStore } from '@/stores/language';

const languageStore = useLanguageStore();
const { t } = languageStore;

const isGuideOpen = ref(false);
const userInput = ref('');
const messages = ref([
  {
    type: 'bot',
    content: t('travel_guide.welcome'),
    time: new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
  }
]);

const chatMessages = ref<HTMLElement>();

const toggleGuide = () => {
  isGuideOpen.value = !isGuideOpen.value;
  if (isGuideOpen.value) {
    nextTick(() => {
      scrollToBottom();
    });
  }
};

const sendMessage = () => {
  if (!userInput.value.trim()) return;
  
  // 사용자 메시지 추가
  messages.value.push({
    type: 'user',
    content: userInput.value,
    time: new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
  });
  
  const userMessage = userInput.value;
  userInput.value = '';
  
  // 봇 응답 시뮬레이션
  setTimeout(() => {
    const botResponse = generateBotResponse(userMessage);
    messages.value.push({
      type: 'bot',
      content: botResponse,
      time: new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
    });
    
    nextTick(() => {
      scrollToBottom();
    });
  }, 1000);
};

const generateBotResponse = (userMessage: string): string => {
  const responses = [
    '좋은 질문이네요! 더 자세한 정보를 알려드릴게요.',
    '그 부분에 대해 도움을 드릴 수 있습니다.',
    '여행 계획을 세우는 데 도움이 될 것 같아요.',
    '추천해드릴 수 있는 곳들이 있어요.',
    '그 지역에 대한 정보를 찾아보겠습니다.'
  ];
  
  return responses[Math.floor(Math.random() * responses.length)];
};

const scrollToBottom = () => {
  if (chatMessages.value) {
    chatMessages.value.scrollTop = chatMessages.value.scrollHeight;
  }
};

onMounted(() => {
  // 초기 스크롤
  nextTick(() => {
    scrollToBottom();
  });
});
</script>

<style scoped>
.travel-guide-container {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  z-index: 1000;
}

.guide-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 50px;
  padding: 1rem 1.5rem;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
  font-weight: 600;
}

/* 다크모드에서 가이드 버튼 */
.dark .guide-button {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.4);
}

.guide-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 25px rgba(102, 126, 234, 0.5);
}

.guide-button.active {
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
}

.dark .guide-button.active {
  background: linear-gradient(135deg, #1e40af 0%, #1d4ed8 100%);
}

.guide-icon {
  font-size: 1.5rem;
}

.guide-text {
  font-size: 0.9rem;
}

.guide-chat {
  position: absolute;
  bottom: 100%;
  right: 0;
  width: 350px;
  height: 500px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  margin-bottom: 1rem;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 16px 16px 0 0;
  transition: background 0.3s ease;
}

/* 다크모드에서 채팅 헤더 */
.dark .chat-header {
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
}

.chat-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
}

.close-button {
  background: none;
  border: none;
  color: white;
  font-size: 1.5rem;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-messages {
  flex: 1;
  padding: 1rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.message {
  display: flex;
  flex-direction: column;
  max-width: 80%;
}

.message.bot {
  align-self: flex-start;
}

.message.user {
  align-self: flex-end;
}

.message-content {
  padding: 0.75rem 1rem;
  border-radius: 16px;
  font-size: 0.9rem;
  line-height: 1.4;
}

.message.bot .message-content {
  background: #f1f5f9;
  color: #374151;
}

.message.user .message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  transition: background 0.3s ease;
}

/* 다크모드에서 사용자 메시지 */
.dark .message.user .message-content {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}

.message-time {
  font-size: 0.75rem;
  color: #9ca3af;
  margin-top: 0.25rem;
  align-self: flex-end;
}

.chat-input {
  display: flex;
  gap: 0.5rem;
  padding: 1rem;
  border-top: 1px solid #e5e7eb;
}

.input-field {
  flex: 1;
  padding: 0.75rem;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 0.9rem;
  outline: none;
  transition: border-color 0.3s ease;
}

.input-field:focus {
  border-color: #667eea;
}

.send-button {
  padding: 0.75rem 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: transform 0.2s ease;
}

/* 다크모드에서 전송 버튼 */
.dark .send-button {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}

.send-button:hover {
  transform: translateY(-1px);
}

/* 모바일 대응 */
@media (max-width: 768px) {
  .travel-guide-container {
    bottom: 1rem;
    right: 1rem;
  }
  
  .guide-chat {
    width: 300px;
    height: 400px;
  }
  
  .guide-text {
    display: none;
  }
  
  .guide-button {
    padding: 1rem;
    border-radius: 50%;
  }
}
</style> 