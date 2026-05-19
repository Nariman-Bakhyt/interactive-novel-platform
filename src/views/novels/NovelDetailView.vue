<script setup lang="ts">
import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {getNovelById} from "@/api/novelService.ts";
import type {ChapterShortResponseDto, NovelResponseDto} from "@/types/novel.ts";
import type {AllRatingResponseDto} from "@/types/rating.ts";
import {DEFAULT_COVER} from "@/utils/media.ts";
import {deleteRating, getRatings, setRating} from "@/api/ratingService.ts";
import type {CommentResponseDto} from "@/types/comment.ts";
import {deleteComment, getComments} from "@/api/commentService.ts";
import {
  activeSubscriptions,
  sendMessage,
  subscribeToTopic,
  unsubscribeFromTopic
} from "@/api/stompService.ts";
const route = useRoute();
const router = useRouter();
const novel = ref<NovelResponseDto | null>(null);
const isLoading = ref(true);
const chaptersList = ref<ChapterShortResponseDto[]>([]);

const fetchNovelData = async () => {
  const rawId = route.params.id;

  if (!rawId) {
    console.warn("Ждем появления ID в роутере...");
    return;
  }

  const id = Number(rawId);

  if (isNaN(id)) {
    console.error("ID не является числом:", rawId);
    isLoading.value = false;
    return;
  }
  try {
    isLoading.value = true;
    const data = await getNovelById(id);
    novel.value = data.novel ;
    chaptersList.value = data.chapters || [];

  } catch (error) {
    console.error("Ошибка при загрузке новеллы:", error);
  } finally {
    isLoading.value = false;
  }
};

const fetchComments = async () => {
  if (isCommentsLoading.value || isCommentsLastPage.value || !novel.value) return;

  isCommentsLoading.value = true;
  try {
    const topicId = `novel.${novel.value.id}`;
    const response = await getComments({ novelId: novel.value.id }, commentsPage.value, PAGE_SIZE, 'timestamp,asc');

    const newItems = response.content || [];

    if (newItems.length > 0) {
      if (!commentsMap.value[topicId]) commentsMap.value[topicId] = [];
      commentsMap.value[topicId].push(...newItems);

      isCommentsLastPage.value = response.last;

      if (!isCommentsLastPage.value) {
        commentsPage.value++;
      }
    } else {
      isCommentsLastPage.value = true;
    }
  } catch (e) {
    console.error(e);
    isCommentsLastPage.value = true;
  } finally {
    setTimeout(() => {
      isCommentsLoading.value = false;
    }, 200);
  }
};

const setupBottomObserver = (targetRef: HTMLElement | null, loadMoreFn: () => void, isLoadingRef: any, isLastRef: any) => {
  // Отключаем старого наблюдателя, если перешли на другую вкладку
  if (bottomObserver) bottomObserver.disconnect();
  if (!targetRef) return;

  bottomObserver = new IntersectionObserver(([entry]) => {
    if (entry && entry.isIntersecting && !isLoadingRef.value && !isLastRef.value) {
      loadMoreFn();
    }
  }, { threshold: 0.1 });

  bottomObserver.observe(targetRef);
};

onUnmounted(() => {
  if (novel.value) {
    const id = novel.value.id;
    unsubscribeFromTopic(`/topic/novel.${id}`);
    unsubscribeFromTopic(`/topic/novel.${id}.ratings`);
  }
  if (bottomObserver) bottomObserver.disconnect();
});

type Tab = 'chapters'| 'comments' | 'ratings';
const activeTab= ref<Tab>('chapters');
const ratingsList = ref<AllRatingResponseDto[]>([]);

const commentsMap = ref<Record<string, CommentResponseDto[]>>({});

const ratingsPage = ref(0);
const isRatingsLastPage = ref(false);
const isRatingsLoading = ref(false);
const ratingsTrigger = ref<HTMLElement | null>(null); // Невидимый див рейтингов

// --- Состояние для Комментариев ---
const commentsPage = ref(0);
const isCommentsLastPage = ref(false);
const isCommentsLoading = ref(false);
const commentsTrigger = ref<HTMLElement | null>(null); // Невидимый див комментариев

// --- Общий Observer ---
let bottomObserver: IntersectionObserver | null = null;
const PAGE_SIZE = 20; // Удобно для дебага (поставь 3 для проверки автозаполнения)

const checkTriggerVisibility = (triggerEl: HTMLElement | null, fetchFn: () => void, isLast: boolean) => {
  if (!isLast && triggerEl) {
    const rect = triggerEl.getBoundingClientRect();
    if (rect.top <= window.innerHeight) {
      setTimeout(fetchFn, 100);
    }
  }
};


const fetchRatings = async () => {
  // КРИТИЧЕСКАЯ БЛОКИРОВКА: если уже грузим или страниц больше нет - СТОП
  if (isRatingsLoading.value || isRatingsLastPage.value || !novel.value) return;

  isRatingsLoading.value = true;
  try {
    const response = await getRatings(novel.value.id, ratingsPage.value, PAGE_SIZE, 'timestamp,asc');

    const newItems = response.allRatings.content || [];

    if (newItems.length > 0) {
      ratingsList.value.push(...newItems);
      // Проверяем: это реально последняя страница?
      // Сравниваем текущую страницу с общим количеством страниц
      isRatingsLastPage.value = (response.allRatings.number + 1) >= response.allRatings.totalPages;

      if (!isRatingsLastPage.value) {
        ratingsPage.value++;
      }
    } else {
      // Если контент пустой - это точно конец
      isRatingsLastPage.value = true;
    }

  } catch (error) {
    console.error("Ошибка:", error);
    isRatingsLastPage.value = true; // Останавливаем при ошибке, чтобы не спамить
  } finally {
    // Даем небольшую задержку перед разблокировкой, чтобы DOM успел "отдвинуть" триггер
    setTimeout(() => {
      isRatingsLoading.value = false;
    }, 200);
  }
};

const handleTabChange = async (tab: Tab) => {
  if (!novel.value) return;
  activeTab.value = tab;
  await nextTick();
    if (tab === 'comments' ) {
      const topicId = `novel.${novel.value.id}`;
      const wsTopic = `/topic/${topicId}`;
      if (!activeSubscriptions.has(wsTopic)) {
        subscribeToTopic<any>(wsTopic, (wsEvent) => {
          const type = wsEvent.type;
          const payload = wsEvent.payload;
          if (type === 'COMMENT_DELETED') {
            if (commentsMap.value[topicId]) {
              commentsMap.value[topicId] = commentsMap.value[topicId].filter(c => c.id !== payload.id);
            }
            return;
          }

          if (type === 'COMMENT_CREATED') {
            if (!commentsMap.value[topicId]) {
              commentsMap.value[topicId] = [];
            }
            const exists = commentsMap.value[topicId].some(c => c.id === payload.id);
            if (!exists) {
              commentsMap.value[topicId].unshift(payload);
            }
          }
        });
      }

    if (!commentsMap.value[topicId] || commentsMap.value[topicId].length === 0) {
      await fetchComments();
    }
    setupBottomObserver(commentsTrigger.value, fetchComments, isCommentsLoading, isCommentsLastPage);
  }
  if (tab === 'ratings' ) {
    const ratingsTopic = `/topic/novel.${novel.value.id}.ratings`;

    if (!activeSubscriptions.has(ratingsTopic)) {
      subscribeToTopic<any>(ratingsTopic, (wsEvent) => {
        const type = wsEvent.type;
        const data = wsEvent.payload;

        if (novel.value) {
          novel.value.totalScore = data.totalScore;
          novel.value.ratingCount = data.ratingCount;
        }

        if (type === 'RATING_DELETED') {
          ratingsList.value = ratingsList.value.filter(r => r.ratingId !== data.ratingId);
          return;
        }

        if (type === 'RATING_CREATED') {
          const oldReviewIndex = ratingsList.value.findIndex(
            r => r.username === data.username
          );

          if (oldReviewIndex !== -1) {
            ratingsList.value.splice(oldReviewIndex, 1);
          }

          const newReview = {
            ratingId: data.ratingId,
            content: data.commentText || '',
            username: data.username,
            timestamp: data.timestamp,
            score: data.score
          };
          ratingsList.value.unshift(newReview);
        }
      });
    }

    if (ratingsList.value.length === 0) {
      await fetchRatings();
    }
    // Вешаем обсервер на див рейтингов
    setupBottomObserver(ratingsTrigger.value, fetchRatings, isRatingsLoading, isRatingsLastPage);
  }

};

const isRatingModalOpen = ref(false);
const newRating = ref({
  score: 5,
  commentText: ''
});
const isSubmitting = ref(false);

const submitRating = async () => {
  if (!novel.value || newRating.value.score === 0) return

  try {
    isSubmitting.value = true;
    const stats = await setRating(novel.value.id, {
      score: newRating.value.score,
      commentText: newRating.value.commentText
    });

    // Обновляем статистику новеллы на лету
    novel.value.totalScore = stats.totalScore;
    novel.value.ratingCount = stats.ratingCount;

    // Очищаем форму и закрываем
    newRating.value = { score: 5, commentText: '' };
    isRatingModalOpen.value = false;

    ratingsList.value = [];
    ratingsPage.value = 0; // было currentPage
    isRatingsLastPage.value = false; // было isLastPage
    await fetchRatings();

  } catch (error) {
    console.error("Ошибка при отправке отзыва:", error);
  } finally {
    isSubmitting.value = false;
  }
};

const newCommentText = ref('');
const isCommenting = ref(false);

const currentComments = computed(() => {
  if (!novel.value) return [];
  const topicId = `novel.${novel.value.id}`;
  return commentsMap.value[topicId] || [];
});
const submitComment = () => {
  if (!novel.value || !newCommentText.value.trim()) return;
  // Отправляем через наш универсальный метод в stompService
  sendMessage('/app/comment.send', {
    novelId: novel.value.id,
    content: newCommentText.value
  });

  // Очищаем поле
  newCommentText.value = '';
};


watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      fetchNovelData();
    }
  },
  { immediate: true }
);


const averageRating = computed(() => {
  if (!novel.value || !novel.value.ratingCount) return '0.0';
  return (novel.value.totalScore / novel.value.ratingCount).toFixed(1);
});
const startReading = () => {
  const currentNovel = novel.value; // сохраняем ссылку для стабильности типов
  const firstChapter = chaptersList.value[0];

  if (currentNovel && firstChapter) {
    router.push(`/novels/${currentNovel.id}/chapter/${firstChapter.id}`);
  } else {
    console.warn("Данные еще не загружены или глав нет");
  }
};

const handlePaste = (e: ClipboardEvent, target: 'rating' | 'comment') => {
  e.preventDefault();
  const text = e.clipboardData?.getData('text/plain');
  if (!text) return;

  if (target === 'rating') {
    newRating.value.commentText += text;
  } else if (target === 'comment') {
    newCommentText.value += text;
  }
};

const handleKeydown = (e: KeyboardEvent, type: 'comment' | 'rating') => {
  // 1. Если нажат Shift + Enter — ничего не делаем, браузер просто перенесет строку
  if (e.shiftKey) return;

  // 2. Если нажат Enter БЕЗ Shift
  if (e.key === 'Enter') {
    // Проверяем ширину экрана (обычно 768px - порог для планшетов/телефонов)
    if (window.innerWidth > 400) {
      e.preventDefault(); // Запрещаем перенос строки

      if (type === 'comment') {
        submitComment();
      } else {
        submitRating();
      }
    }
  }
};

const contextMenu = ref({
  show: false,
  x: 0,
  y: 0,
  targetId: null as number | null,
  targetType: '' as 'comment' | 'rating'
});

const openContextMenu = (e: MouseEvent, id: number, type: 'comment' | 'rating') => {
  e.preventDefault();
  e.stopPropagation(); // Критически важно

  contextMenu.value = {
    show: true,
    x: e.clientX,
    y: e.clientY,
    targetId: id,
    targetType: type
  };

  const close = () => {
    contextMenu.value.show = false;
    document.removeEventListener('click', close);
  };

  // Небольшая задержка перед регистрацией клика на закрытие
  setTimeout(() => {
    document.addEventListener('click', close);
  }, 50);
};

const handleDelete = async () => {
  const { targetId, targetType } = contextMenu.value;
  if (!targetId || !novel.value) return;

  try {
    if (targetType === 'comment') {
      await deleteComment(targetId);
    } else {
      await deleteRating(novel.value.id,targetId);
    }
    contextMenu.value.show = false;
  } catch (error) {
    console.error("Ошибка удаления:", error);
    alert("Вы не можете удалить этот элемент");
  }
};


</script>

<template>
  <div class="novel-page-container">
    <div v-if="isLoading" class="loader-container">
      <div class="spinner"></div>
      <p>Загрузка мира...</p>
    </div>

    <div v-else-if="novel" class="notion-style-container">
      <button @click="router.back()" class="btn-minimal">
        <span class="icon">←</span> Назад
      </button>

      <header class="novel-header">
        <div class="cover-section">
          <img
            :src="novel.coverUrl || DEFAULT_COVER"
            :alt="novel.title"
            class="main-cover"
          />
        </div>

        <div class="info-section">
          <h1 class="main-title-input">{{ novel.title }}</h1>

          <div class="pop-stats">
            <span class="stat-item rating">
              <span class="icon">⭐</span> {{ averageRating }}
            </span>
            <span class="stat-item views">
              <span class="icon">👁‍🗨</span> {{ novel.viewCount || 0 }}
            </span>
            <span class="stat-item chapters">
              <span class="icon">📚</span> {{ novel.chapterCount || 0 }} глав
            </span>
          </div>

          <div class="pop-metadata">
            <div class="chips-row">
              <span v-for="genre in novel.genres" :key="genre.id" class="pop-chip genre">
                {{ genre.name }}
              </span>
            </div>
            <div class="chips-row">
              <span v-for="tag in novel.tags" :key="tag.id" class="pop-chip tag">
                #{{ tag.name }}
              </span>
            </div>
          </div>

          <p class="description-text">{{ novel.description }}</p>

          <div class="actions">
            <button
              class="btn-save-notion"
              @click="startReading"
              :disabled="chaptersList.length === 0"
            >
              Начать читать
            </button>
            <button class="btn-edit-main">В библиотеку</button>
          </div>
        </div>
      </header>

      <section class="content-section">
        <div class="tabs-nav">
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'chapters' }"
            @click="handleTabChange('chapters')"
          >
            📚 Главы <span class="tab-count">({{ chaptersList.length }})</span>
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'comments' }"
            @click="handleTabChange('comments')"
          >
            💬 Обсуждение
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'ratings' }"
            @click="handleTabChange('ratings')"
          >
            ⭐ Отзывы
          </button>
        </div>

        <div class="tab-content">

          <div v-if="activeTab === 'chapters'" class="chapters-list fade-in">
            <div v-if="chaptersList.length > 0">
              <div
                v-for="(chapter, index) in chaptersList"
                :key="chapter.id"
                class="chapter-item"
                @click="router.push(`/novels/${novel.id}/chapter/${chapter.id}`)"
              >
                <div class="ch-info">
                  <span class="ch-number">Глава {{ index + 1 }}</span>
                  <span class="ch-title">{{ chapter.title }}</span>
                </div>
                <span class="ch-icon">→</span>
              </div>
            </div>
            <div v-if="!isRatingsLoading && ratingsList.length === 0 && isRatingsLastPage" class="empty-state">
              Пока никто не оценил это произведение. Станьте первым!
            </div>
          </div>

          <div v-if="activeTab === 'comments'" class="comments-container fade-in">
            <div class="comment-input-area">
              <textarea
                v-model="newCommentText"
                @paste="(e) => handlePaste(e, 'comment')"
                @keydown="(e) => handleKeydown(e, 'comment')"
                placeholder="Написать комментарий..."
                class="comment-textarea-minimal"
              ></textarea>
              <div class="comment-actions">
                <button
                  class="btn-save-notion"
                  @click="submitComment"
                  :disabled="!newCommentText.trim()"
                >
                  Отправить
                </button>
              </div>
            </div>

            <div class="comments-list" v-if="currentComments.length > 0">
              <div
                v-for="comment in currentComments"
                :key="comment.id"
                class="comment-item action-wrapper"
                @contextmenu="(e) => openContextMenu(e, comment.id, 'comment')"
              >
                <div class="comment-header">
                  <span class="user-badge">{{ comment.username }}</span>
                  <span class="comment-date">{{ new Date(comment.timestamp).toLocaleString() }}</span>
                  <button class="mobile-action-btn" @click="openContextMenu($event, comment.id, 'comment')">⋮</button>
                </div>
                <p class="comment-body">{{ comment.content }}</p>
              </div>
            </div>
            <div ref="commentsTrigger" class="loading-anchor">
              <div v-if="isCommentsLoading" class="mini-spinner"></div>
              <p v-if="isCommentsLastPage" class="end-message">Больше комментариев нет.</p>
            </div>

            <div v-if="!isCommentsLoading && currentComments.length === 0 && isCommentsLastPage" class="empty-state">
              Здесь пока нет комментариев. Будьте первым, кто оставит след в истории!
            </div>
          </div>

          <div v-if="activeTab === 'ratings'" class="ratings-container fade-in">
            <div class="rating-header">
              <div class="average-big">{{ averageRating }}</div>
              <div class="rating-info">
                <p>На основе {{ novel.ratingCount }} оценок</p>
                <button class="btn-save-notion" @click="isRatingModalOpen = true">Написать отзыв</button>

                <div v-if="isRatingModalOpen" class="modal-overlay" @click.self="isRatingModalOpen = false">
                  <div class="modal-content fade-in">
                    <div class="modal-header-rating">
                      <h3>Оставить отзыв</h3>
                      <button class="close-btn" @click="isRatingModalOpen = false">&times;</button>
                    </div>

                    <div class="rating-selector">
                      <p>Ваша оценка:</p>
                      <div class="stars">
                        <span
                          v-for="star in 5"
                          :key="star"
                          class="star-icon"
                          :class="{ active: star <= newRating.score }"
                          @click="newRating.score = star"
                        >
                        ★
                      </span>
                      </div>
                    </div>

                    <textarea
                      v-model="newRating.commentText"
                      @paste="(e) => handlePaste(e, 'rating')"
                      @keydown="(e) => handleKeydown(e, 'rating')"
                      placeholder="Поделитесь вашим мнением о произведении..."
                      class="rating-textarea"
                    ></textarea>

                    <div class="modal-actions">
                      <button class="btn-minimal" @click="isRatingModalOpen = false">Отмена</button>
                      <button
                        class="btn-save-notion"
                        @click="submitRating"
                        :disabled="isSubmitting"
                      >
                        {{ isSubmitting ? 'Отправка...' : 'Опубликовать' }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="ratings-list">
              <div
                v-for="(rating, index) in ratingsList"
                :key="index"
                class="rating-item action-wrapper"
                @contextmenu="(e) => openContextMenu(e, rating.ratingId, 'rating')"
              >
                <div class="rating-item-header">
                  <span class="reviewer-name">{{ rating.username }}</span>
                  <span class="reviewer-score">⭐ {{ rating.score }}</span>
                  <button class="mobile-action-btn" @click="openContextMenu($event, rating.ratingId, 'rating')">⋮</button>
                  <span class="review-date">{{ new Date(rating.timestamp).toLocaleDateString() }}</span>
                </div>
                <p v-if="rating.content" class="review-text">{{ rating.content }}</p>
              </div>
            </div>

            <div ref="ratingsTrigger" class="loading-anchor">
              <div v-if="isRatingsLoading" class="mini-spinner"></div>
              <p v-if="isRatingsLastPage" class="end-message">Это все отзывы на данный момент.</p>
            </div>
          </div>

        </div>
      </section>
    </div>
  </div>
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

</template>

<style scoped>
.novel-page-container,
.rating-textarea,
.review-text,
.description-text {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: var(--text-muted);
  line-height: 1.6;
  font-size: 1rem;
}

.description-text {
  margin-bottom: 32px;
  white-space: pre-wrap;
}

.novel-page-container {
  min-height: 100vh;
  background-color: var(--bg-main);
  padding: 100px 24px 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.notion-style-container {
  width: 100%;
  max-width: 1000px;
  background-color: var(--bg-dropdown);
  padding: 48px 64px;
  border-radius: 16px;
  box-shadow: 0 4px 12px var(--shadow-color);
  border: 1px solid var(--border-color);
  transition: all 0.3s ease;
}

.novel-header {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 40px;
  margin-top: 16px;
  margin-bottom: 48px;
}

.main-cover {
  width: 100%;
  aspect-ratio: 2/3;
  object-fit: cover;
  border-radius: 12px;
  box-shadow: 0 8px 24px var(--shadow-color);
  border: 1px solid var(--border-color);
}

.main-title-input {
  border: none;
  font-size: 2.5rem;
  font-weight: 800;
  margin-bottom: 16px;
  line-height: 1.2;
  color: var(--text-header);
  letter-spacing: -0.02em;
}

.pop-stats {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
  font-weight: 600;
  color: var(--text-header);
  font-size: 0.95rem;
}

.stat-item { display: flex; align-items: center; gap: 6px; }
.rating .icon { color: #f59e0b; } /* amber-500 */

.pop-metadata {
  margin-bottom: 24px;
}

.chips-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.pop-chip {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 500;
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  color: var(--text-header);
}

.pop-chip.genre {
  background: rgba(16, 185, 129, 0.1);
  border-color: rgba(16, 185, 129, 0.2);
  color: #10b981;
}
.pop-chip.tag {
  background: rgba(99, 102, 241, 0.1);
  border-color: rgba(99, 102, 241, 0.2);
  color: #6366f1;
}

.actions { display: flex; gap: 16px; }

.chapters-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 24px;
}

.chapter-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: var(--bg-main);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid var(--border-color);
}

.chapter-item:hover {
  background: var(--hover-dropdowb);
  border-color: var(--text-muted);
}

.ch-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.ch-number {
  font-weight: 600;
  color: var(--btn-plus);
  font-size: 0.95rem;
  min-width: 80px;
}

.ch-title {
  color: var(--text-header);
  font-weight: 500;
}
.ch-icon {
  color: var(--text-muted);
  font-weight: bold;
  transition: transform 0.2s;
}
.chapter-item:hover .ch-icon {
  transform: translateX(4px);
  color: var(--text-header);
}

.loader-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 50vh;
  color: var(--text-muted);
}

@media (max-width: 850px) {
  .notion-style-container { padding: 32px 24px; }
  .novel-header { grid-template-columns: 1fr; text-align: center; }
  .main-cover { max-width: 240px; margin: 0 auto; }
  .pop-stats, .chips-row, .actions { justify-content: center; }
}

.btn-minimal {
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  padding: 8px 12px;
  margin-bottom: 24px;
  margin-left: -12px;
  border-radius: 6px;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-minimal:hover {
  background: var(--hover-dropdowb);
  color: var(--text-header);
}

/* --- Кнопка "Начать читать" (Основная/Акцентная) --- */
.btn-save-notion {
  background-color: var(--btn-plus);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.2s, transform 0.2s;
}

.btn-save-notion:hover:not(:disabled) {
  background-color: var(--btn-plus-hover);
  transform: translateY(-1px);
}

.btn-save-notion:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* --- Кнопка "В библиотеку" (Второстепенная) --- */
.btn-edit-main {
  background-color: var(--bg-main);
  color: var(--text-header);
  border: 1px solid var(--border-color);
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-edit-main:hover {
  background-color: var(--hover-dropdowb);
  border-color: var(--text-muted);
}


/* Спиннер загрузки */
.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-subtle);
  border-top-color: var(--btn-plus);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.content-section {
  margin-top: 48px;
  border-top: 1px solid var(--border-color);
  padding-top: 32px;
}

.tabs-nav {
  display: flex;
  gap: 32px;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 32px;
}

.tab-btn {
  background: none;
  border: none;
  padding: 12px 4px;
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--text-muted);
  cursor: pointer;
  position: relative;
  transition: color 0.2s ease;
}

.tab-btn:hover {
  color: var(--text-header);
}

.tab-btn.active {
  color: var(--btn-plus);
}

.tab-count {
  font-weight: 500;
  font-size: 0.9em;
  opacity: 0.8;
}

/* Линия под активным табом */
.tab-btn.active::after {
  content: "";
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: var(--btn-plus);
  border-radius: 2px 2px 0 0;
}

.fade-in {
  animation: fadeIn 0.3s ease-in;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.rating-header {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 32px;
  background: var(--bg-main);
  border-radius: 16px;
  margin-bottom: 24px;
  border: 1px solid var(--border-color);
}

.average-big {
  font-size: 4rem;
  font-weight: 800;
  color: #f59e0b; /* amber-500 */
  line-height: 1;
  letter-spacing: -0.05em;
}

.rating-info p {
  color: var(--text-muted);
  margin-bottom: 16px;
  font-size: 0.95rem;
}
.ratings-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 24px;
}

.rating-item {
  padding: 24px;
  background: var(--bg-main);
  border-radius: 12px;
  border: 1px solid var(--border-color);
}

.rating-item-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.reviewer-name { font-weight: 600; color: var(--text-header); }
.reviewer-score { color: #f59e0b; font-weight: bold; }
.review-date { color: var(--text-muted); font-size: 0.85rem; margin-left: auto; }

.review-text {
  color: var(--text-header);
  font-size: 1rem;
  line-height: 1.6;
  margin: 0;
}

/* Стили для бесконечной загрузки */
.loading-anchor {
  padding: 40px 0;
  display: flex;
  justify-content: center;
  min-height: 50px;
}

.mini-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid var(--border-subtle);
  border-top-color: var(--btn-plus);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.end-message {
  color: var(--text-muted);
  font-size: 0.9rem;
}
/* Модальное окно */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  backdrop-filter: blur(8px);
}

.modal-content {
  background: var(--bg-dropdown);
  padding: 32px;
  border-radius: 16px;
  width: 100%;
  max-width: 500px;
  box-shadow: 0 20px 50px rgba(0,0,0,0.5);
  border: 1px solid var(--border-color);
}

.modal-header-rating {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.modal-header-rating h3 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-header);
}
.close-btn {
  background: none; border: none; color: var(--text-muted); font-size: 1.5rem; cursor: pointer; padding: 4px; border-radius: 4px; line-height: 1; transition: background 0.2s, color 0.2s;
}
.close-btn:hover { background: var(--hover-dropdowb); color: var(--text-header); }

/* Выбор звезд */
.rating-selector {
  margin-bottom: 24px;
}

.rating-selector p {
  margin: 0 0 8px;
  font-weight: 500;
  color: var(--text-header);
}

.stars {
  display: flex;
  gap: 8px;
  font-size: 2rem;
  cursor: pointer;
}

.star-icon {
  color: var(--border-color);
  transition: transform 0.2s ease, color 0.2s ease;
  line-height: 1;
}

.star-icon.active {
  color: #f59e0b;
}

.star-icon:hover {
  transform: scale(1.1);
}

/* Поле ввода */
.rating-textarea {
  width: 100%;
  height: 120px;
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 16px;
  color: var(--text-header);
  font-family: inherit;
  resize: vertical;
  margin-bottom: 24px;
  outline: none;
  font-size: 0.95rem;
  line-height: 1.5;
  transition: border-color 0.2s;
}

.rating-textarea:focus {
  border-color: var(--btn-plus);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
}
/* Поле ввода комментария */
.comment-input-area {
  margin-bottom: 32px;
  background: var(--bg-main);
  padding: 16px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
}

.comment-textarea-minimal {
  width: 100%;
  min-height: 60px;
  background: transparent;
  border: none;
  resize: vertical;
  color: var(--text-header);
  font-size: 0.95rem;
  outline: none;
  margin-bottom: 12px;
  font-family: inherit;
}
.comment-textarea-minimal::placeholder {
  color: var(--input-placeholder);
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
}
.comment-actions .btn-save-notion {
  padding: 8px 20px;
  font-size: 0.95rem;
}

/* Элементы списка комментариев */
.comment-item {
  padding: 20px;
  border-bottom: 1px solid var(--border-color);
}
.comment-item:last-child {
  border-bottom: none;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.user-badge {
  font-weight: 600;
  color: var(--text-header);
}

.comment-date {
  font-size: 0.85rem;
  color: var(--text-muted);
}

.comment-body {
  color: var(--text-header);
  line-height: 1.5;
  white-space: pre-wrap;
  margin: 0;
  font-size: 0.95rem;
}

/* Контекстное меню */
.context-menu {
  position: fixed;
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  box-shadow: 0 4px 12px var(--shadow-color);
  border-radius: 8px;
  padding: 4px;
  z-index: 9999;
  min-width: 160px;
}

.menu-item {
  padding: 10px 16px;
  cursor: pointer;
  font-size: 0.9rem;
  border-radius: 6px;
  transition: background 0.2s;
  font-weight: 500;
}

.menu-item.delete {
  color: #ef4444; /* red-500 */
}

.menu-item:hover {
  background: var(--hover-dropdowb);
}

/* Кнопка для мобильных */
.mobile-action-btn {
  display: none; /* Скрыта по умолчанию */
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 1.25rem;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  margin-left: auto;
  line-height: 1;
}
.mobile-action-btn:hover {
  background: var(--hover-dropdowb);
  color: var(--text-header);
}

.action-wrapper {
  position: relative;
}

/* На мобильных устройствах (ширина < 768px) */
@media (max-width: 768px) {
  .mobile-action-btn {
    display: block; /* Показываем только на мобилках */
  }
}
.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-muted);
  background: var(--bg-main);
  border-radius: 12px;
  border: 1px dashed var(--border-color);
  margin-top: 20px;
}
</style>
