<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref ,watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {getNovelById} from "@/api/novelService.ts";
import type {ChapterShortResponseDto, NovelResponseDto} from "@/types/novel.ts";
import type {AllRatingResponseDto} from "@/types/rating.ts";
import {deleteRating, getRatings, setRating} from "@/api/RatingService.ts";
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


onUnmounted(() => {
  if (novel.value) {
    const id = novel.value.id;
    unsubscribeFromTopic(`/topic/novel.${id}`);
    unsubscribeFromTopic(`/topic/novel.${id}.ratings`);
  }
});

type Tab = 'chapters'| 'comments' | 'ratings';
const activeTab= ref<Tab>('chapters');
const ratingsList = ref<AllRatingResponseDto[]>([]);
const isRatingsLoading = ref(false);
const currentPage = ref(0);
const isLastPage = ref(false);
const loadMoreTrigger = ref<HTMLElement | null>(null);
const commentsMap = ref<Record<string, CommentResponseDto[]>>({});

const fetchRatings = async () => {
  if (isRatingsLoading.value || isLastPage.value || !novel.value) return;

  try {
    isRatingsLoading.value = true;
    const response = await getRatings(novel.value.id, currentPage.value, 20,'timestamp,desc');
    novel.value.totalScore = response.totalScore;
    novel.value.ratingCount = response.ratingCount;
    ratingsList.value.push(...response.allRatings.content);

    isLastPage.value = response.allRatings.last;
    if (!isLastPage.value) {
      currentPage.value++;
    }
  } catch (error) {
    console.error("Ошибка загрузки отзывов:", error);
  } finally {
    isRatingsLoading.value = false;
  }
};

const handleTabChange = async (tab: Tab) => {
  if (!novel.value) return;
  activeTab.value = tab;

  if (tab === 'comments' ) {
    const topicId = `novel.${novel.value.id}`;
    const wsTopic = `/topic/${topicId}`;
    if (!activeSubscriptions.has(wsTopic)) {
      subscribeToTopic<any>(wsTopic, (newComment) => {
        if (newComment.deleted) {
          if (commentsMap.value[topicId]) {
            commentsMap.value[topicId] = commentsMap.value[topicId].filter(c => c.id !== newComment.id);
          }
          return;
        }

        if (!commentsMap.value[topicId]) {
          commentsMap.value[topicId] = [];
        }
        const exists = commentsMap.value[topicId].some(c => c.id === newComment.id);
        if (!exists) {
          commentsMap.value[topicId].unshift(newComment);
        }
      });
    }

    if (!commentsMap.value[topicId] || commentsMap.value[topicId].length === 0) {
      try {
        const history = await getComments({ novelId: novel.value.id });
        commentsMap.value[topicId] = history.content;
      } catch (e) {
        console.error("Ошибка загрузки истории комментариев", e);
      }
    }

  }
  if (tab === 'ratings' ) {
    const ratingsTopic = `/topic/novel.${novel.value.id}.ratings`;

    if (!activeSubscriptions.has(ratingsTopic)) {
      subscribeToTopic<any>(ratingsTopic, (data) => {

        if (novel.value) {
          if (data.deleted) {
            if ( data.score !== undefined) {
              novel.value.totalScore -= data.score;
              novel.value.ratingCount -= 1;
            }else {
              const localRating = ratingsList.value.find(r => r.ratingId === data.id);
              if (localRating) {
                novel.value.totalScore -= localRating.score;
              }
              novel.value.ratingCount -= 1;
            }
            ratingsList.value = ratingsList.value.filter(r => r.ratingId !== data.id);

            return;
          }
          novel.value.totalScore = data.totalScore;
          novel.value.ratingCount = data.ratingCount;
        }
        const oldReviewIndex = ratingsList.value.findIndex(
          r => r.username === data.username
        );

        if (oldReviewIndex !== -1) {
          ratingsList.value.splice(oldReviewIndex, 1);
        }

        if (data.score !== undefined && data.username && data.timestamp) {
          const newReview = {
            ratingId: data.ratingId,
            content: data.content || '', // Если текста нет, ставим пустую строку
            username: data.username,
            timestamp: data.timestamp,
            score: data.score
          };
          ratingsList.value.unshift(newReview);
        }
      });
    }

    await fetchRatings();
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

    // Обнуляем список отзывов, чтобы пользователь увидел свой новый отзыв при перезагрузке
    ratingsList.value = [];
    currentPage.value = 0;
    isLastPage.value = false;
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
      <button @click="router.back()" class="btn-minimal">← Назад</button>

      <header class="novel-header">
        <div class="cover-section">
          <img
            :src="novel.coverUrl || 'http://127.0.0.1:9000/interactive-novel-assets/Cover/default-cover.png'"
            :alt="novel.title"
            class="main-cover"
          />
        </div>

        <div class="info-section">
          <h1 class="main-title-input">{{ novel.title }}</h1>

          <div class="pop-stats">
            <span class="stat-item rating">⭐ {{ averageRating }}</span>
            <span class="stat-item views">👁‍🗨 {{ novel.viewCount || 0 }}</span>
            <span class="stat-item chapters">📚 {{ novel.chapterCount || 0 }} глав</span>
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
            📚 Главы ({{ chaptersList.length }})
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
                <span class="ch-number">Глава {{ index + 1 }}</span>
                <span class="ch-title">{{ chapter.title }}</span>
                <span class="ch-icon">→</span>
              </div>
            </div>
            <div v-else class="empty-state">Главы еще не добавлены.</div>
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

            <div v-else class="empty-state">
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
                    <h3>Оставить отзыв</h3>

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

            <div ref="loadMoreTrigger" class="loading-anchor">
              <div v-if="isRatingsLoading" class="mini-spinner"></div>
              <p v-if="isLastPage" class="end-message">Это все отзывы на данный момент.</p>
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
  line-height: 1.8;
  font-size: 1.05rem;
  margin-bottom: 35px;
  white-space: pre-wrap;
}

.novel-page-container {
  min-height: 100vh;
  background-color: var(--bg-editor-page);
  padding: 60px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.notion-style-container {
  width: 100%;
  max-width: 900px;
  background-color: var(--bg-editor-sheet);
  padding: 60px 80px;
  border-radius: 16px;
  box-shadow: 0 10px 40px var(--shadow-color);
  border: 1px solid var(--border-subtle);
  transition: all 0.3s ease;
}

.novel-header {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 40px;
  margin-top: 20px;
  margin-bottom: 50px;
}

.main-cover {
  width: 100%;
  aspect-ratio: 2/3;
  object-fit: cover;
  border-radius: 12px;
  box-shadow: 0 10px 30px var(--shadow-color);
  border: 1px solid var(--border-subtle);
}

.main-title-input {
  border: none;
  font-size: 2.8rem;
  margin-bottom: 15px;
  line-height: 1.1;
}

.pop-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
  font-weight: 600;
  color: var(--text-header);
}

.stat-item { display: flex; align-items: center; gap: 6px; }
.rating { color: #f1c40f; }

.pop-metadata {
  margin-bottom: 25px;
}

.chips-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.pop-chip {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.85rem;
  border: 1px solid var(--border-color);
  background: var(--border-subtle);
}

.pop-chip.genre { color: #42b883; border-color: #42b883; }

.actions { display: flex; gap: 15px; }


.chapters-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 20px;
}

.chapter-item {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  background: var(--bg-main);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.chapter-item:hover {
  background: rgba(66, 184, 131, 0.1);
  border-color: rgba(66, 184, 131, 0.3);
  transform: translateX(8px);
}

.ch-number {
  font-weight: bold;
  color: #42b883;
  margin-right: 20px;
}

.ch-title { flex: 1; color: var(--text-header); }
.ch-icon { color: var(--text-muted); opacity: 0.5; }

.loader-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 50vh;
  color: var(--text-muted);
}

@media (max-width: 850px) {
  .notion-style-container { padding: 30px; }
  .novel-header { grid-template-columns: 1fr; text-align: center; }
  .main-cover { max-width: 280px; margin: 0 auto; }
  .pop-stats, .chips-row, .actions { justify-content: center; }
}

.btn-minimal {
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 0.9rem;
  cursor: pointer;
  padding: 8px 0;
  margin-bottom: 20px;
  transition: color 0.2s ease;
  display: flex;
  align-items: center;
  gap: 5px;
}

.btn-minimal:hover {
  color: #42b883; /* Твой акцентный зеленый */
}

/* --- Кнопка "Начать читать" (Основная/Акцентная) --- */
.btn-save-notion {
  background-color: #42b883;
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 14px rgba(66, 184, 131, 0.4);
}

.btn-save-notion:hover {
  background-color: #3aa373;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(66, 184, 131, 0.5);
}

.btn-save-notion:active {
  transform: translateY(0);
}

/* --- Кнопка "В библиотеку" (Второстепенная) --- */
.btn-edit-main {
  background-color: transparent;
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
  background-color: var(--bg-main);
  border-color: var(--text-muted);
  transform: translateY(-2px);
}


/* Спиннер загрузки */
.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--border-subtle);
  border-top: 4px solid #42b883;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
/* --- Кнопка "Назад" (Minimal) --- */
.btn-minimal {
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 0.9rem;
  cursor: pointer;
  padding: 8px 0;
  margin-bottom: 20px;
  transition: color 0.2s ease;
  display: flex;
  align-items: center;
  gap: 5px;
}

.btn-minimal:hover {
  color: #42b883; /* Твой акцентный зеленый */
}

/* --- Кнопка "Начать читать" (Основная/Акцентная) --- */
.btn-save-notion {
  background-color: #42b883;
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 14px rgba(66, 184, 131, 0.4);
}

.btn-save-notion:hover {
  background-color: #3aa373;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(66, 184, 131, 0.5);
}

.btn-save-notion:active {
  transform: translateY(0);
}

/* --- Кнопка "В библиотеку" (Второстепенная) --- */
.btn-edit-main {
  background-color: transparent;
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
  background-color: var(--bg-main);
  border-color: var(--text-muted);
  transform: translateY(-2px);
}


/* Спиннер загрузки */
.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--border-subtle);
  border-top: 4px solid #42b883;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.content-section {
  margin-top: 40px;
  border-top: 1px solid var(--border-subtle);
  padding-top: 20px;
}

.tabs-nav {
  display: flex;
  gap: 30px;
  border-bottom: 1px solid var(--border-subtle);
  margin-bottom: 30px;
}

.tab-btn {
  background: none;
  border: none;
  padding: 15px 5px;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-muted);
  cursor: pointer;
  position: relative;
  transition: all 0.2s ease;
}

.tab-btn:hover {
  color: #42b883;
}

.tab-btn.active {
  color: #42b883;
}

/* Линия под активным табом */
.tab-btn.active::after {
  content: "";
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 3px;
  background-color: #42b883;
  border-radius: 10px 10px 0 0;
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
  gap: 30px;
  padding: 20px;
  background: var(--bg-main);
  border-radius: 12px;
  margin-bottom: 20px;
}

.average-big {
  font-size: 3.5rem;
  font-weight: 800;
  color: #f1c40f;
}

.rating-info p {
  color: var(--text-muted);
  margin-bottom: 10px;
}
.ratings-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 20px;
}

.rating-item {
  padding: 20px;
  background: var(--bg-main);
  border-radius: 12px;
  border: 1px solid var(--border-subtle);
}

.rating-item-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.reviewer-name { font-weight: 700; color: var(--text-header); }
.reviewer-score { color: #f1c40f; font-weight: bold; }
.review-date { color: var(--text-muted); font-size: 0.85rem; margin-left: auto; }

.review-text {
  color: var(--text-header);
  font-size: 1.05rem;
  line-height: 1.7;
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
  border-top: 3px solid #42b883;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.end-message {
  color: var(--text-muted);
  font-style: italic;
  font-size: 0.9rem;
}
/* Модальное окно */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-content {
  background: var(--bg-editor-sheet);
  padding: 40px;
  border-radius: 20px;
  width: 100%;
  max-width: 500px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
  border: 1px solid var(--border-subtle);
}

.modal-content h3 {
  margin-bottom: 25px;
  font-size: 1.5rem;
  color: var(--text-header);
}

/* Выбор звезд */
.rating-selector {
  margin-bottom: 20px;
}

.stars {
  display: flex;
  gap: 8px;
  font-size: 2rem;
  cursor: pointer;
  margin-top: 10px;
}

.star-icon {
  color: var(--border-color);
  transition: transform 0.2s ease, color 0.2s ease;
}

.star-icon.active {
  color: #f1c40f;
  transform: scale(1.1);
}

.star-icon:hover {
  transform: scale(1.2);
}

/* Поле ввода */
.rating-textarea {
  width: 100%;
  height: 150px;
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 15px;
  color: var(--text-header);
  font-family: inherit;
  resize: none;
  margin-bottom: 25px;
  outline: none;
  font-size: 1rem;
  line-height: 1.6;
}

.rating-textarea:focus {
  border-color: #42b883;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 20px;
}
/* Поле ввода комментария */
.comment-input-area {
  margin-bottom: 30px;
  background: var(--bg-main);
  padding: 20px;
  border-radius: 12px;
  border: 1px solid var(--border-subtle);
}

.comment-textarea-minimal {
  width: 100%;
  min-height: 80px;
  background: transparent;
  border: none;
  resize: none;
  color: var(--text-header);
  font-size: 1rem;
  outline: none;
  margin-bottom: 10px;
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
}

/* Элементы списка комментариев */
.comment-item {
  padding: 15px 0;
  border-bottom: 1px solid var(--border-subtle);
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.user-badge {
  font-weight: 700;
  color: #42b883;
}

.comment-date {
  font-size: 0.8rem;
  color: var(--text-muted);
}

.comment-body {
  color: var(--text-header);
  line-height: 1.5;
  white-space: pre-wrap;
}

/* Контекстное меню */
.context-menu {
  position: fixed;
  background: var(--bg-editor-sheet);
  border: 1px solid var(--border-subtle);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  border-radius: 8px;
  padding: 4px;
  z-index: 9999;
  min-width: 150px;
}

.menu-item {
  padding: 8px 12px;
  cursor: pointer;
  font-size: 0.9rem;
  border-radius: 4px;
  transition: background 0.2s;
}

.menu-item.delete {
  color: #eb5757;
}

.menu-item:hover {
  background: var(--bg-main);
}

/* Кнопка для мобильных */
.mobile-action-btn {
  display: none; /* Скрыта по умолчанию */
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 1.2rem;
  padding: 0 10px;
  cursor: pointer;
  margin-left: auto;
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
</style>
