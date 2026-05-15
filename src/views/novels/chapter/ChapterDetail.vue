<script setup lang="ts">
import { ref, onMounted, computed, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getChapter, getNovelById } from "@/api/novelService.ts";
import type { ChapterResponseDto, ChapterShortResponseDto } from "@/types/novel.ts";
import { useSmartScroll } from "@/api/commentService.ts";
import {useCommentStore} from "@/components/chat/commentStore.ts";
import type {CommentResponseDto} from "@/types/comment.ts";
import QuoteTooltip from "@/components/QuoteTooltip.vue";
const route = useRoute();
const router = useRouter();
const chatStore = useCommentStore(); // Инициализируем стор

const chapter = ref<ChapterResponseDto | null>(null);
const chaptersList = ref<ChapterShortResponseDto[]>([]);
const isLoading = ref(true);

const nId = computed(() => Number(route.params.novelId));
const cId = computed(() => Number(route.params.chapterId));
const { scrollToTarget } = useSmartScroll();

// Поиск по тексту (оставляем, это логика читки)
watch(() => route.query.q, async (newText) => {
  if (!newText) return;
  await nextTick();
  scrollToTarget();
}, { immediate: true });

const fetchData = async () => {
  try {
    isLoading.value = true;
    chapter.value = await getChapter(nId.value, cId.value);
    if (chaptersList.value.length === 0) {
      const novelData = await getNovelById(nId.value);
      chaptersList.value = (novelData.chapters || []).sort((a, b) => a.chapterNumber - b.chapterNumber);
    }
  } catch (error) {
    console.error("Ошибка при загрузке главы:", error);
  } finally {
    isLoading.value = false;
    window.scrollTo(0, 0);
  }
};

// Функция открытия чата теперь — это просто вызов стора
const toggleComments = (blockId: number | null) => {
  if (!blockId) return;
  chatStore.openChat(blockId, 'BLOCK');
};

watch(() => route.params.chapterId, (newId) => { if (newId) fetchData(); });

onMounted(fetchData);

// Вычисляемые свойства для навигации
const chapterNumber = computed(() => {
  const index = chaptersList.value.findIndex(c => c.id === cId.value);
  return index !== -1 ? index + 1 : '';
});
const prevChapter = computed(() => {
  const index = chaptersList.value.findIndex(c => c.id === cId.value);
  return index > 0 ? chaptersList.value[index - 1] : null;
});
const nextChapter = computed(() => {
  const index = chaptersList.value.findIndex(c => c.id === cId.value);
  return index !== -1 && index < chaptersList.value.length - 1 ? chaptersList.value[index + 1] : null;
});
const navigateTo = (id: number) => router.push(`/novels/${nId.value}/chapter/${id}`);
</script>

<template>
  <div class="reader-page">
    <div v-if="isLoading" class="loader">Загрузка главы...</div>

    <div v-else-if="chapter" class="reader-container" :class="{ 'with-sidebar': chatStore.isOpen }">
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
          :key="block.id ?? index"
          class="content-block-wrapper"
          :class="{ 'active-block': chatStore.activeTargetId === block.id }"
        >
          <div class="block-main">
            <p v-if="block.type === 'TEXT'" class="text-block">{{ block.content }}</p>
            <div v-else-if="block.type === 'IMAGE'" class="image-block">
              <img :src="block.content" alt="Иллюстрация" loading="lazy" />
            </div>
          </div>

          <div class="block-actions">
            <button
              class="comment-trigger"
              :class="{ 'is-active': chatStore.activeTargetId === block.id }"
              @click="toggleComments(block.id)"
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

  </div>

</template>

<style scoped>
/* Этот селектор красит именно тот текст, который мы нашли выше */
::highlight(search-results) {
  background-color: rgba(255, 213, 79, 0.7);
  color: black;
  text-decoration: underline;
}
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

.sidebar-input input {
  width: 100%;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-main);
  color: var(--text-header);
  outline: none;
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



.date-separator span {
  padding: 0 10px;
  font-weight: bold;
}


</style>
