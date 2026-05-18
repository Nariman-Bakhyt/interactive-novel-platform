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
  // Открываем сайдбар если он был скрыт
  window.dispatchEvent(new CustomEvent('open-messenger'));
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
    <div v-if="isLoading" class="loader-container">
      <div class="spinner"></div>
      <p>Загрузка страницы...</p>
    </div>

    <!-- Убрали класс "with-sidebar" и логику сдвига текста -->
    <div v-else-if="chapter" class="reader-container">
      <nav class="reader-nav">
        <button @click="router.push(`/novel/${nId}`)" class="btn-back">
          <span class="icon">←</span> Оглавление
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
          @click="toggleComments(block.id)"
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
              @click.stop="toggleComments(block.id)"
            >
              💬
            </button>
          </div>
        </div>
      </article>

      <footer class="reader-footer">
        <button :disabled="!prevChapter" @click="navigateTo(prevChapter!.id)" class="nav-btn">
           <span class="icon">←</span> Предыдущая
        </button>
        <span class="chapter-info">Глава {{ chapterNumber }} / {{ chaptersList.length }}</span>
        <button :disabled="!nextChapter" @click="navigateTo(nextChapter!.id)" class="nav-btn primary">
          Следующая <span class="icon">→</span>
        </button>
      </footer>
    </div>
  </div>
</template>

<style scoped>
::highlight(search-results) {
  background-color: #fcd34d !important; /* Яркий желтый (tailwind) */
  color: #000 !important;
  border-radius: 2px;
}

.reader-page {
  min-height: 100vh;
  background-color: var(--bg-editor-page);
  color: var(--text-header);
  padding: 80px 24px 100px; /* Увеличен отступ сверху */
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: all 0.3s ease;
}

.reader-container {
  width: 100%;
  max-width: 760px; /* Чуть шире для современного вида */
  background: var(--bg-editor-sheet);
  padding: 48px 64px;
  border-radius: 24px;
  box-shadow: 0 4px 12px var(--shadow-color);
  border: 1px solid var(--border-color);
  /* Убрали transition: transform, так как сдвига больше нет */
}

/* Удален класс .reader-container.with-sidebar { transform: translateX(-150px); } */

.content-block-wrapper {
  position: relative;
  display: flex;
  align-items: flex-start;
  margin-bottom: 0;
  padding: 8px 16px; /* Добавлен горизонтальный паддинг для кликабельной зоны */
  border-radius: 8px;
  transition: background-color 0.2s;
  cursor: pointer; /* Делаем весь блок визуально кликабельным */
}

.content-block-wrapper:hover {
  background-color: rgba(161, 161, 170, 0.05); /* Очень слабый фон при наведении */
}

.content-block-wrapper.active-block {
  background-color: rgba(99, 102, 241, 0.08);
}


.block-main {
  margin: 0;
  flex: 1;
  width: 100%; /* Убедимся, что контент занимает всю ширину */
}

/* Стили для кнопки комментариев */
.block-actions {
  position: absolute;
  right: -56px;
  top: 50%;
  transform: translateY(-50%) translateX(-10px);
  opacity: 0;
  transition: opacity 0.2s, transform 0.2s;
}

.content-block-wrapper:hover .block-actions,
.content-block-wrapper.active-block .block-actions {
  opacity: 1;
  transform: translateY(-50%) translateX(0);
}

.comment-trigger {
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  border-radius: 50%;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px var(--shadow-color);
  transition: all 0.2s;
  font-size: 1.1rem;
}

.comment-trigger:hover {
  transform: scale(1.1);
  border-color: var(--btn-plus);
  color: var(--btn-plus);
}

.comment-trigger.is-active {
  background: var(--btn-plus);
  color: white;
  border-color: var(--btn-plus);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}

.reader-nav {
  margin-bottom: 48px;
}

.btn-back {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 1.05rem;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  margin-left: -12px;
  border-radius: 8px;
  transition: all 0.2s ease;
}
.btn-back:hover {
  color: var(--text-header);
  background: var(--hover-dropdowb);
}

.chapter-header {
  text-align: center;
  margin-bottom: 64px;
}

.chapter-meta {
  color: var(--btn-plus);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-weight: 700;
  font-size: 0.95rem;
  margin-bottom: 16px;
}

.chapter-title {
  font-size: 2.75rem;
  font-weight: 800;
  margin-bottom: 32px;
  letter-spacing: -0.02em;
  color: var(--text-header);
  line-height: 1.2;
}

.divider {
  width: 48px;
  height: 4px;
  background-color: var(--btn-plus);
  margin: 0 auto;
  border-radius: 4px;
}

/* Стили текста для комфортного чтения */
.chapter-content {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  font-size: 1.25rem; /* Оптимальный размер для чтения */
  line-height: 1.8;
  color: var(--text-header);
}

.text-block {
  margin-bottom: 1.5em; /* Отступы между абзацами */
  white-space: pre-wrap;
  word-wrap: break-word;
  color: var(--text-header);
  opacity: 0.9; /* Слегка смягчить контраст */
}

.image-block {
  margin: 3rem 0;
  display: flex;
  justify-content: center;
}

.image-block img {
  max-width: 100%;
  border-radius: 12px;
  box-shadow: 0 8px 24px var(--shadow-color);
  border: 1px solid var(--border-color);
}

.reader-footer {
  margin-top: 80px;
  padding-top: 40px;
  border-top: 1px solid var(--border-color);
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
  font-size: 1rem;
  font-weight: 600;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.nav-btn.primary {
  background: var(--btn-plus);
  color: white;
  border: none;
}

.nav-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px var(--shadow-color);
}

.nav-btn.primary:hover:not(:disabled) {
  background: var(--btn-plus-hover);
}

.chapter-info {
  font-weight: 500;
  color: var(--text-muted);
  font-size: 0.95rem;
}

.loader-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 50vh;
  color: var(--text-muted);
}
.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-subtle);
  border-top-color: var(--btn-plus);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 24px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 900px) {
  .reader-container {
    padding: 32px 24px;
    border-radius: 16px;
  }
  .block-actions { right: -8px; } /* Сдвигаем кнопку комментариев внутрь на мобилках */
}

@media (max-width: 600px) {
  .chapter-title { font-size: 2rem; }
  .chapter-content { font-size: 1.15rem; }
  .reader-footer { flex-direction: column; gap: 24px; }
  .nav-btn { width: 100%; justify-content: center; }
}
</style>
