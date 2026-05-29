<script setup lang="ts">
import { useCommentStore } from '@/components/chat/commentStore.ts';
import { useAuthStore } from '@/api/auth.ts';
import { computed, ref } from 'vue';

const commentStore = useCommentStore();
const authStore = useAuthStore();

const newComment = ref('');
const isSending = ref(false);

const isMyMessage = (item: any) => {
  const senderId = item.senderId !== undefined ? item.senderId : item.userId;
  return authStore.userDetails?.id === senderId;
};

const formatTime = (ts: string) => new Date(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

const handleSend = async () => {
  if (!newComment.value.trim() || isSending.value) return;
  isSending.value = true;
  try {
    const chapterId = commentStore.currentChapterContext?.id;
    if (chapterId) {
      const activeIdBackup = commentStore.activeTargetId;
      const targetTypeBackup = commentStore.targetType;

      commentStore.activeTargetId = chapterId;
      commentStore.targetType = 'CHAPTER';

      await commentStore.send({ content: newComment.value });
      newComment.value = '';

      commentStore.activeTargetId = activeIdBackup;
      commentStore.targetType = targetTypeBackup;
    }
  } finally {
    isSending.value = false;
  }
};

const openInSidebar = () => {
  if (commentStore.currentChapterContext) {
    commentStore.openChat(commentStore.currentChapterContext.id, 'CHAPTER');
    window.dispatchEvent(new CustomEvent('open-messenger'));
  }
};
</script>

<template>
  <div class="chapter-comments-section">
    <div class="section-header">
      <h3>Обсуждение главы ({{ commentStore.chapterComments.length }})</h3>
      <button class="open-sidebar-btn" @click="openInSidebar">
        Открыть в панели 💬
      </button>
    </div>

    <div class="comments-list">
      <div v-if="commentStore.chapterComments.length === 0" class="no-comments">
        Пока нет комментариев. Будьте первым!
      </div>

      <div v-for="item in commentStore.chapterComments" :key="item.id"
           class="comment-item-wrapper"
           :class="{ 'is-mine': isMyMessage(item), 'is-others': !isMyMessage(item) }">
        <div class="comment-bubble"
             :class="{ 'bubble-mine': isMyMessage(item), 'bubble-others': !isMyMessage(item) }">
          <span v-if="!isMyMessage(item)" class="user-badge">{item.username}</span>
          <div v-if="item.metadata?.images?.length" class="comment-images">
            <img v-for="url in item.metadata.images" :key="url" :src="url" class="comment-img">
          </div>
          <p class="comment-body">{{ item.content }}</p>
          <div class="comment-footer"><span class="comment-date">{{ formatTime(item.timestamp) }}</span></div>
        </div>
      </div>
    </div>

    <div class="comment-input-area">
      <textarea
        v-model="newComment"
        @keydown.enter.prevent="handleSend"
        placeholder="Написать комментарий..."
        rows="2"
      ></textarea>
      <button class="send-btn" @click="handleSend" :disabled="isSending || !newComment.trim()">
        <span v-if="isSending" class="spinner-small"></span>
        <span v-else>Отправить</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.chapter-comments-section {
  margin-top: 64px;
  padding-top: 32px;
  border-top: 1px solid var(--border-color);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-header h3 {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-header);
  margin: 0;
}

.open-sidebar-btn {
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  color: var(--text-header);
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.open-sidebar-btn:hover {
  background: var(--hover-dropdowb);
  border-color: var(--btn-plus);
  color: var(--btn-plus);
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
  max-height: 500px;
  overflow-y: auto;
  padding-right: 8px;
}

.no-comments {
  text-align: center;
  color: var(--text-muted);
  font-style: italic;
  padding: 32px 0;
}

.comment-item-wrapper {
  display: flex;
  width: 100%;
}

.comment-item-wrapper.is-mine {
  justify-content: flex-end;
}

.comment-item-wrapper.is-others {
  justify-content: flex-start;
}

.comment-bubble {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 16px;
  position: relative;
  word-break: break-word;
}

.bubble-mine {
  background-color: var(--btn-plus);
  color: white;
  border-bottom-right-radius: 4px;
}

.bubble-others {
  background-color: var(--bg-dropdown);
  color: var(--text-header);
  border: 1px solid var(--border-color);
  border-bottom-left-radius: 4px;
}

.user-badge {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--btn-plus);
  margin-bottom: 4px;
  display: block;
}

.bubble-mine .user-badge {
  color: rgba(255,255,255,0.8);
}

.comment-body {
  margin: 0 0 4px 0;
  line-height: 1.4;
  white-space: pre-wrap;
}

.comment-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.comment-img {
  max-width: 100%;
  max-height: 200px;
  border-radius: 8px;
  object-fit: cover;
}

.comment-footer {
  display: flex;
  justify-content: flex-end;
}

.comment-date {
  font-size: 0.75rem;
  opacity: 0.7;
}

.comment-input-area {
  display: flex;
  gap: 12px;
  background: var(--bg-dropdown);
  padding: 16px;
  border-radius: 16px;
  border: 1px solid var(--border-color);
}

.comment-input-area textarea {
  flex: 1;
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  color: var(--text-header);
  border-radius: 8px;
  padding: 12px;
  resize: none;
  font-family: inherit;
  font-size: 1rem;
}

.comment-input-area textarea:focus {
  outline: none;
  border-color: var(--btn-plus);
}

.send-btn {
  background: var(--btn-plus);
  color: white;
  border: none;
  border-radius: 8px;
  padding: 0 24px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-btn:hover:not(:disabled) {
  background: var(--btn-plus-hover);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spinner-small {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
