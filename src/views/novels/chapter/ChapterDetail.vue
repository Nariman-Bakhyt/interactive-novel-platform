<script setup lang="ts">
import {computed, nextTick, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {getChapter, getNovelById} from "@/api/novelService.ts";
import type {ChapterResponseDto, ChapterShortResponseDto} from "@/types/novel.ts";
import {useSmartScroll} from "@/api/commentService.ts";
import {useCommentStore} from "@/components/chat/commentStore.ts";
import ChapterComments from '@/components/chat/ChapterComments.vue';
import { onUnmounted } from 'vue';
import { onBeforeRouteLeave, onBeforeRouteUpdate } from 'vue-router';

const route = useRoute();
const router = useRouter();
const chatStore = useCommentStore();

const chapter = ref<ChapterResponseDto | null>(null);
const chaptersList = ref<ChapterShortResponseDto[]>([]);
const isLoading = ref(true);

const nId = computed(() => Number(route.params.novelId));
const cId = computed(() => Number(route.params.chapterId));
const { scrollToTarget } = useSmartScroll();


watch(() => route.query.q, async (newText) => {
  if (!newText) return;
  await nextTick();
  scrollToTarget();
}, { immediate: true });

let scrollTimeout: any = null;
let isLeaving = false;

const saveCurrentScroll = () => {
  if (!isLoading.value && chapter.value && !isLeaving) {
    const scrollPos = window.scrollY || document.documentElement.scrollTop || document.body.scrollTop || 0;
    localStorage.setItem(`scroll_n_${nId.value}_c_${cId.value}`, scrollPos.toString());
  }
};

onBeforeRouteLeave(() => {
  saveCurrentScroll();
  isLeaving = true;
});

onBeforeRouteUpdate(() => {
  saveCurrentScroll();
});

const handleVisibilityChange = () => {
  if (document.visibilityState === 'hidden') {
    saveCurrentScroll();
  }
};

const handleScroll = () => {
  if (scrollTimeout || isLeaving) return;
  scrollTimeout = setTimeout(() => {
    saveCurrentScroll();
    scrollTimeout = null;
  }, 200);
};

const fetchData = async () => {
  try {
    isLoading.value = true;
    chapter.value = await getChapter(nId.value, cId.value);
    if (chaptersList.value.length === 0) {
      const novelData = await getNovelById(nId.value);
      chaptersList.value = (novelData.chapters || []).sort((a, b) => a.chapterNumber - b.chapterNumber);
      await chatStore.setContext(nId.value, novelData.novel.title, cId.value, chapter.value.title);
    } else {
      const novelTitle = chaptersList.value.length > 0 ? "Новелла" : "Новелла";
      await chatStore.setContext(nId.value, novelTitle, cId.value, chapter.value.title);
    }
  } catch (error) {
    console.error("Ошибка при загрузке главы:", error);
  } finally {
    isLoading.value = false;
    nextTick(() => {
      const saved = localStorage.getItem(`scroll_n_${nId.value}_c_${cId.value}`);
      if (saved && !route.query.q) {
        const y = parseInt(saved, 10);
        
        // Если это перезагрузка страницы (F5), браузер сам восстановит скролл, не вмешиваемся
        const navEntries = performance.getEntriesByType('navigation');
        if (navEntries.length > 0 && (navEntries[0] as PerformanceNavigationTiming).type === 'reload') {
          return;
        }

        let userScrolled = false;
        const cancelRestore = () => { userScrolled = true; };
        window.addEventListener('wheel', cancelRestore, { once: true });
        window.addEventListener('touchstart', cancelRestore, { once: true });

        setTimeout(async () => {
          const images = document.querySelectorAll('.chapter-content img');
          const promises = Array.from(images).map(img => {
            const htmlImg = img as HTMLImageElement;
            if (htmlImg.complete) return Promise.resolve();
            return new Promise(resolve => {
              htmlImg.addEventListener('load', resolve, { once: true });
              htmlImg.addEventListener('error', resolve, { once: true });
            });
          });
          
          await Promise.all(promises);
          
          if (!userScrolled) {
            window.scrollTo({ top: y, behavior: 'instant' });
          }
          
          window.removeEventListener('wheel', cancelRestore);
          window.removeEventListener('touchstart', cancelRestore);
        }, 50);
      } else if (!route.query.q) {
        window.scrollTo(0, 0);
      }
    });
  }
};

onMounted(() => {
  fetchData();
  window.addEventListener('scroll', handleScroll, { passive: true });
  document.addEventListener('visibilitychange', handleVisibilityChange);
});

onUnmounted(() => {
  saveCurrentScroll();
  chatStore.clearContext();
  window.removeEventListener('scroll', handleScroll);
  document.removeEventListener('visibilitychange', handleVisibilityChange);
  if (scrollTimeout) clearTimeout(scrollTimeout);
});


const toggleComments = (blockId: number | null) => {
  if (!blockId) return;
  chatStore.openChat(blockId, 'BLOCK');

  window.dispatchEvent(new CustomEvent('open-messenger'));
};

watch(() => route.params.chapterId, (newId) => { if (newId) fetchData(); });


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

      <ChapterComments />
    </div>
  </div>
</template>

<style scoped>
::highlight(search-results) {
  background-color: #fcd34d !important;
  color: #000 !important;
  border-radius: 2px;
}

.reader-page {
  min-height: 100vh;
  background-color: var(--bg-editor-page);
  color: var(--text-header);
  padding: 80px 24px 100px;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: all 0.3s ease;
}

.reader-container {
  width: 100%;
  max-width: 760px;
  background: var(--bg-editor-sheet);
  padding: 48px 64px;
  border-radius: 24px;
  box-shadow: 0 4px 12px var(--shadow-color);
  border: 1px solid var(--border-color);

}



.content-block-wrapper {
  position: relative;
  display: flex;
  align-items: flex-start;
  margin-bottom: 0;
  padding: 8px 16px;
  border-radius: 8px;
  transition: background-color 0.2s;
  cursor: pointer;
}

.content-block-wrapper:hover {
  background-color: rgba(161, 161, 170, 0.05);
}

.content-block-wrapper.active-block {
  background-color: rgba(99, 102, 241, 0.08);
}


.block-main {
  margin: 0;
  flex: 1;
  width: 100%;
}


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


.chapter-content {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  font-size: 1.25rem;
  line-height: 1.8;
  color: var(--text-header);
}

.text-block {
  margin-bottom: 1.5em;
  white-space: pre-wrap;
  word-wrap: break-word;
  color: var(--text-header);
  opacity: 0.9;
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
  .block-actions { right: -8px; }
}

@media (max-width: 600px) {
  .chapter-title { font-size: 2rem; }
  .chapter-content { font-size: 1.15rem; }
  .reader-footer { flex-direction: column; gap: 24px; }
  .nav-btn { width: 100%; justify-content: center; }
}
</style>
