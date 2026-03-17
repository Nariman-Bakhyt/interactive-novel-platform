<script setup lang="ts">
import { ref, onMounted, computed , watch ,nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getChapter, getNovelById } from "@/api/novelService.ts";
import type { ChapterResponseDto, ChapterShortResponseDto } from "@/types/novel.ts";
import {deleteComment, getComments} from "@/api/commentService.ts";
import {
  activeSubscriptions,
  sendMessage,
  subscribeToTopic,
  unsubscribeFromTopic
} from "@/api/stompService.ts";
import type {CommentResponseDto} from "@/types/comment.ts";

const route = useRoute();
const router = useRouter();

const chapter = ref<ChapterResponseDto | null>(null);
const chaptersList = ref<ChapterShortResponseDto[]>([]);
const isLoading = ref(true);
const activeCommentsBlockId = ref<number | null>(null);
const nId = computed(() => Number(route.params.novelId));
const cId = computed(() => Number(route.params.chapterId));
const comments = ref<any[]>([]);

const fetchData = async () => {
  try {
    isLoading.value = true;
    const nId = Number(route.params.novelId);
    const cId = Number(route.params.chapterId);

    // 1. Загружаем главу
    chapter.value = await getChapter(nId, cId);

    // 2. Загружаем список глав (если он еще не загружен)
    if (chaptersList.value.length === 0) {
      const novelData = await getNovelById(nId);
      chaptersList.value = (novelData.chapters || []).sort((a, b) => a.chapterNumber - b.chapterNumber);
    }
  } catch (error) {
    console.error("Ошибка при загрузке главы:", error);
  } finally {
    isLoading.value = false;
    window.scrollTo(0, 0); // Прокрутка вверх при каждой новой главе
  }
};
const initStickyObserver = () => {
  const container = commentsListRef.value;
  if (!container) return;

  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        // Находим заголовок, который идет СРАЗУ за этим маячком
        const header = entry.target.nextElementSibling;
        const badge = header?.querySelector('.date-badge');
        if (!badge) return;

        // Элемент считается "в тексте", если его персональный маячок виден.
        // Это не зависит от пикселей, это зависит от физического положения в DOM.
        const isInText = entry.isIntersecting;

        badge.toggleAttribute('data-in-text', isInText);
      });
    },
    {
      root: container,
      threshold: [0, 1],
      rootMargin: '0px' // Теперь строго по границе контейнера
    }
  );

  // Следим за маячками
  container.querySelectorAll('.sticky-sentinel').forEach((el) => observer.observe(el));
};




watch(comments, () => {
  nextTick(() => {
    // Небольшая задержка, чтобы DOM точно успел отрисоваться
    setTimeout(initStickyObserver, 100);
  });
}, { deep: true });
watch(
  () => route.params.chapterId,
  (newId) => {
    if (newId) {
      fetchData(); // Если ID изменился — запускаем загрузку заново
    }
  }
);
const commentsListRef = ref<HTMLElement | null>(null);
const scrollToBottom = async () => {
  await nextTick();
  if (commentsListRef.value) {
    commentsListRef.value.scrollTop = commentsListRef.value.scrollHeight;
  }
};
const groupedComments = computed(() => {
  const groups: Record<string, CommentResponseDto[]> = {};

  comments.value.forEach(comment => {
    const date = new Date(comment.timestamp).toLocaleDateString();
    if (!groups[date]) {
      groups[date] = [];
    }
    groups[date].push(comment);
  });

  return groups;
});
const isToday = (dateStr: string) => {
  return new Date().toLocaleDateString() === dateStr;
};

const formatTime = (timestamp: string) => {
  return new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};
const isScrolling = ref(false);
let scrollTimeout: number | null = null;

const handleScroll = () => {
  isScrolling.value = true;

  // При каждом движении пересчитываем положение маячков
  initStickyObserver();

  if (scrollTimeout) clearTimeout(scrollTimeout);

  scrollTimeout = window.setTimeout(() => {
    isScrolling.value = false;
  }, 1500); // Исчезнет через 1.5 секунды после остановки
};
onMounted(fetchData);

// Твой массив сообщений для текущего блока

const unsubscribeFromCurrentBlock = () => {
  if (activeCommentsBlockId.value !== null) {
    const block = chapter.value?.blocks[activeCommentsBlockId.value];

    if (block && block.id) {
      const topic = `/topic/block.${block.id}`;
      unsubscribeFromTopic(topic);

      console.log(`Отписались от: ${topic}`);
    }

    comments.value = [];
  }
};


const toggleComments = async (index: number, blockId: number | null) => {
  if (!blockId) return;

  if (activeCommentsBlockId.value === index) {
    unsubscribeFromCurrentBlock();
    activeCommentsBlockId.value = null;
    comments.value = [];
    return;
  }

  if (activeCommentsBlockId.value !== null) {
    unsubscribeFromCurrentBlock();
  }

  activeCommentsBlockId.value = index;
  const wsTopic = `/topic/block.${blockId}`;

  if (!activeSubscriptions.has(wsTopic)) {
    subscribeToTopic<any>(wsTopic, (newComment) => {
      if (newComment.deleted) {
        comments.value = comments.value.filter(c => c.id !== newComment.id);
        return;
      }
      const exists = comments.value.some(c => c.id === newComment.id);
      if (!exists) {
        comments.value.push(newComment);
      }
    });
  }

  try {
    const history = await getComments({ blockId });
    comments.value = history.content;
    scrollToBottom();
  } catch (error) {
    console.error("Ошибка загрузки истории:", error);
  }
};

// Определяем текущий номер главы для отображения
const chapterNumber = computed(() => {
  const index = chaptersList.value.findIndex(c => c.id === cId.value);
  return index !== -1 ? index + 1 : '';
});

// Навигация
const prevChapter = computed(() => {
  const index = chaptersList.value.findIndex(c => c.id === cId.value);
  return index > 0 ? chaptersList.value[index - 1] : null;
});

const nextChapter = computed(() => {
  const index = chaptersList.value.findIndex(c => c.id === cId.value);
  return index !== -1 && index < chaptersList.value.length - 1 ? chaptersList.value[index + 1] : null;
});

const navigateTo = (id: number) => {
  router.push(`/novels/${nId.value}/chapter/${id}`);
};

const newCommentText = ref('');
const submitComment = () => {
  if (!newCommentText.value.trim() || activeCommentsBlockId.value === null) return;
  const currentBlock = chapter.value?.blocks[activeCommentsBlockId.value];
  if (!currentBlock?.id) return;
  sendMessage('/app/comment.send', {
    blockId: currentBlock.id,
    content: newCommentText.value
  });
  newCommentText.value = ''; // Очистка поля
};
const handleKeydown = (e: KeyboardEvent) => {
  if (e.shiftKey) return; // Shift + Enter — перенос строки

  if (e.key === 'Enter') {
    if (window.innerWidth > 400) { // На десктопе отправляем по Enter
      e.preventDefault();
      submitComment();
    }
  }
};

// --- Контекстное меню (Удаление) ---
const contextMenu = ref({
  show: false,
  x: 0,
  y: 0,
  targetId: null as number | null
});

const openContextMenu = (e: MouseEvent, id: number) => {
  e.preventDefault();
  e.stopPropagation();

  contextMenu.value = {
    show: true,
    x: e.clientX,
    y: e.clientY,
    targetId: id
  };

  const close = () => {
    contextMenu.value.show = false;
    document.removeEventListener('click', close);
  };

  setTimeout(() => {
    document.addEventListener('click', close);
  }, 50);
};

const handleDelete = async () => {
  if (!contextMenu.value.targetId) return;

  try {
    // Вызываем API удаления (бэкенд должен отправить по WS событие "deleted": true)
    await deleteComment(contextMenu.value.targetId);
    contextMenu.value.show = false;
  } catch (error) {
    console.error("Ошибка удаления:", error);
    alert("Вы не можете удалить этот комментарий");
  }
};

onMounted(fetchData);
</script>

<template>
  <div class="reader-page">
    <div v-if="isLoading" class="loader">Загрузка главы...</div>

    <div v-else-if="chapter" class="reader-container" :class="{ 'with-sidebar': activeCommentsBlockId !== null }">
      <nav class="reader-nav">
        <button @click="router.push(`/novel/${nId}`)" class="btn-back">
          <span>🏠</span> К списку глав
        </button>
      </nav>

      <header class="chapter-header">
        <p class="chapter-meta">Глава {{ chapterNumber }}</p>
        <h1 class="chapter-title">{{ chapter.title }}</h1>
        <div class="divider"></div>
      </header>

      <article class="chapter-content">
        <div
          v-for="(block, index) in chapter.blocks"
          :key="block.id ?? block.sequenceOrder"
          class="content-block-wrapper"
          :class="{ 'active-block': activeCommentsBlockId === index }"
        >
          <div class="block-main">
            <p v-if="block.type === 'TEXT'" class="text-block">
              {{ block.content }}
            </p>

            <div v-else-if="block.type === 'IMAGE'" class="image-block">
              <img :src="block.content" alt="Иллюстрация" loading="lazy" />
            </div>
          </div>

          <div class="block-actions">
            <button
              class="comment-trigger"
              :class="{ 'is-active': activeCommentsBlockId === index }"
              @click="toggleComments(index, block.id)"
            >
              💬
            </button>
          </div>
        </div>
      </article>

      <footer class="reader-footer">
        <button :disabled="!prevChapter" @click="navigateTo(prevChapter!.id)" class="nav-btn">← Назад</button>
        <span class="chapter-info">Глава {{ chapterNumber }}</span>
        <button :disabled="!nextChapter" @click="navigateTo(nextChapter!.id)" class="nav-btn primary">Вперед →</button>
      </footer>
    </div>

    <Transition name="slide">
      <div v-if="activeCommentsBlockId !== null" class="comments-sidebar">
        <div class="sidebar-header">
          <h3>Обсуждение блока</h3>
          <button class="close-btn" @click="activeCommentsBlockId = null">✕</button>
        </div>

        <div class="comments-list" ref="commentsListRef" @scroll = "handleScroll">
          <div v-for="(group, date) in groupedComments" :key="date" class="comment-group">
            <div class="sticky-sentinel"></div>
            <div class="date-sticky-header" :class="{ 'is-scrolling-active': isScrolling }">
              <span class="date-badge">{{ isToday(date) ? 'Сегодня' : date }}</span>
            </div>

            <div
              v-for="comment in group"
              :key="comment.id"
              class="comment-item"
              @contextmenu="(e) => openContextMenu(e, comment.id)"
            >
              <div class="comment-bubble">
                <div class="comment-item-header">
                  <span class="user-badge">{{ comment.username }}</span>
                </div>
                <p class="comment-body">{{ comment.content }}</p>
                <div class="comment-footer">
                  <span class="comment-date">{{ formatTime(comment.timestamp) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="sidebar-input-area">
      <textarea
        v-model="newCommentText"
        @keydown="handleKeydown"
        placeholder="Написать комментарий... (Enter для отправки)"
        rows="1"
      ></textarea>
          <button
            class="send-btn"
            :disabled="!newCommentText.trim()"
            @click="submitComment"
          >
            ▲
          </button>
        </div>
      </div>
    </Transition>
    <Teleport to="body">
      <div
        v-if="contextMenu.show"
        class="context-menu"
        :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }"
      >
        <div class="menu-item delete" @click="handleDelete">
          🗑 Удалить
        </div>
      </div>
    </Teleport>
  </div>
</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Onest:wght@400..700&display=swap');

.reader-page {
  min-height: 100vh;
  background-color: var(--bg-editor-page);
  color: var(--text-header);
  padding: 40px 20px 100px;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: all 0.3s ease;
}

.reader-container {
  width: 100%;
  max-width: 720px; /* Сузили с 800px для удобства чтения */
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.content-block-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  margin-bottom: 0;
  group: block;
}

.block-actions {
  position: absolute;
  right: -50px; /* Выносим кнопку за пределы текста */
  opacity: 0;
  transition: opacity 0.2s, transform 0.2s;
}

.content-block-wrapper:hover .block-actions,
.content-block-wrapper.active-block .block-actions {
  opacity: 1;
}

.comment-trigger {
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  border-radius: 50%;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px var(--shadow-color);
  transition: all 0.2s;
}

.comment-trigger:hover {
  transform: scale(1.1);
  border-color: var(--btn-plus);
}

.comment-trigger.is-active {
  background: var(--btn-plus);
  color: white;
  border-color: var(--btn-plus);
}

/* Сайдбар */
.comments-sidebar {
  position: fixed;
  top: 0;
  right: 0;
  width: 350px;
  height: 100vh;
  background-color: var(--bg-editor-sheet);
  border-left: 1px solid var(--border-subtle);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  box-shadow: -10px 0 30px rgba(0,0,0,0.1);
}

.sidebar-header {
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-subtle);
}

.close-btn {
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 1.2rem;
  cursor: pointer;
}

.comments-list {
  position: relative;
  flex: 1;
  overflow-y: auto;
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background-color: var(--bg-editor-sheet);
}
.comment-group {
  position: relative;
  display: flex;
  flex-direction: column;
}
.date-sticky-header {
  position: sticky;
  top: -1px;
  z-index: 20;
  display: flex;
  justify-content: center;
  padding: 10px 0;
  pointer-events: none;
}
.sticky-sentinel {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  visibility: hidden;
  pointer-events: none;
}

/* Сама плашка с текстом даты */
.date-badge {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--border-subtle);
  color: #444;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 4px 14px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);

  /* По умолчанию (для закрепленной сверху) — скрываем */
  opacity: 0;
  transform: translateY(-5px);
  transition: opacity 0.4s ease, transform 0.4s ease;
}


.date-badge[data-in-text] {
  opacity: 1;
  transform: translateY(0);
}

.date-badge[data-in-text] {
  opacity: 1;
  transform: translateY(0);
}

/* СОСТОЯНИЕ 2: Дата "прилипла" к верху */
/* Она видна ТОЛЬКО если активен класс скроллинга у родителя */
.date-sticky-header.is-scrolling-active .date-badge:not([data-in-text]) {
  opacity: 1;
  transform: translateY(0);
}

/* Дополнительно: когда дата прилипла, можно сделать фон чуть плотнее */
.date-sticky-header:not(.is-scrolling-active) .date-badge:not([data-in-text]) {
  opacity: 0;
  transform: translateY(-8px);
}
.sidebar-input {
  padding: 20px;
  border-top: 1px solid var(--border-subtle);
}

.sidebar-input input {
  width: 100%;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-main);
  color: var(--text-header);
  outline: none;
}

/* Анимация сайдбара */
.slide-enter-active, .slide-leave-active { transition: transform 0.3s ease; }
.slide-enter-from, .slide-leave-to { transform: translateX(100%); }

.empty-msg {
  text-align: center;
  color: var(--text-muted);
  margin-top: 40px;
  font-size: 0.9rem;
}

/* Адаптив: на мобилках прячем смещение */
@media (max-width: 1000px) {
  .reader-container.with-sidebar { transform: none; }
  .block-actions { right: 0; }
  .comments-sidebar { width: 100%; }
}
.block-main {
  margin: 0 0 0 0;
  flex: 1;
}

.reader-nav {
  margin-bottom: 40px;
}

.btn-back {
  background: none;
  border: none;
  color: var(--btn-plus);
  cursor: pointer;
  font-size: 1rem;
  display: flex;
  align-items: center;
  gap: 8px;
}

.chapter-header {
  text-align: center;
  margin-bottom: 60px;
}

.chapter-meta {
  color: var(--btn-plus);
  text-transform: uppercase;
  letter-spacing: 2px;
  font-weight: bold;
  font-size: 0.9rem;
  margin-bottom: 10px;
}

.chapter-title {
  font-size: 2.5rem;
  font-weight: 800;
  margin-bottom: 20px;
}

.divider {
  width: 60px;
  height: 4px;
  background-color: var(--btn-plus);
  margin: 0 auto;
  border-radius: 2px;
}

/* Стили текста для комфортного чтения */
.chapter-content {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  font-size: 1.25rem;
  line-height: 1.8;
  color: var(--text-header);
}

.text-block {
  margin-bottom: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.image-block {
  margin: 3rem 0;
  display: flex;
  justify-content: center;
}

.image-block img {
  max-width: 100%;
  border-radius: 8px;
  box-shadow: 0 10px 30px var(--shadow-color);
}

.reader-footer {
  margin-top: 80px;
  padding-top: 40px;
  border-top: 1px solid var(--border-subtle);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nav-btn {
  padding: 12px 24px;
  border-radius: 8px;
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  color: var(--text-header);
  cursor: pointer;
  transition: 0.2s;
}

.nav-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.nav-btn.primary {
  background: var(--btn-plus);
  color: white;
  border: none;
}

.nav-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  filter: brightness(1.1);
}

.chapter-info {
  font-weight: bold;
  color: var(--text-muted);
}

@media (max-width: 600px) {
  .chapter-title { font-size: 1.8rem; }
  .chapter-content { font-size: 1.1rem; }
  .reader-footer { flex-direction: column; gap: 20px; }
}

.sidebar-input-area {
  padding: 12px 16px 24px;
  border-top: 1px solid var(--border-subtle);
  display: flex;
  align-items: flex-end;
  gap: 12px;
  background: var(--bg-editor-sheet);
}

.sidebar-input-area textarea {
  flex: 1;
  background: var(--bg-main);
  border: 1px solid var(--border-subtle);
  border-radius: 18px; /* Округлый ввод */
  padding: 10px 16px;
  color: var(--text-header);
  resize: none;
  font-size: 0.95rem;
  line-height: 1.4;
  outline: none;
  max-height: 120px;
  transition: border-color 0.2s ease;
}

.sidebar-input-area textarea:focus {
  border-color: var(--btn-plus);
}

.send-btn {
  background: var(--btn-plus);
  color: white;
  border: none;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  flex-shrink: 0;
  margin-bottom: 2px;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
  filter: brightness(1.1);
}

.send-btn:active:not(:disabled) {
  transform: scale(0.95);
}

/* Контекстное меню (как в твоем примере) */
.context-menu {
  position: fixed;
  background: var(--bg-editor-sheet);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  padding: 4px;
  z-index: 9999;
  min-width: 120px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--border-subtle);
}

.menu-item.delete {
  color: #eb5757;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 4px;
}

.menu-item:hover {
  background: var(--bg-main);
}
.date-separator {
  display: flex;
  align-items: center;
  text-align: center;
  margin: 20px 0;
  color: var(--text-muted);
  font-size: 0.8rem;
}

.date-separator::before,
.date-separator::after {
  content: '';
  flex: 1;
  border-bottom: 1px solid var(--border-subtle);
}

.date-separator span {
  padding: 0 10px;
  font-weight: bold;
}

.comment-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start; /* Все сообщения слева */
  max-width: 90%;
}
.comment-bubble {
  background: var(--bg-main);
  border: 1px solid var(--border-subtle);
  border-radius: 14px 14px 14px 4px; /* Скругление как в мессенджерах */
  padding: 8px 12px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  position: relative;
  transition: transform 0.2s ease;
}

.comment-bubble:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.08);
}
.comment-item-header {
  display: flex;
  margin-bottom: 4px;
}

.user-badge {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--btn-plus); /* Выделяем автора цветом темы */
}
.comment-body {
  font-size: 0.95rem;
  line-height: 1.4;
  color: var(--text-header);
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
}

/* Футер сообщения (Время) */
.comment-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 2px;
}

.comment-date {
  font-size: 0.7rem;
  color: var(--text-muted);
  font-weight: 500;
}

</style>
