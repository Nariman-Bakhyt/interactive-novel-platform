<template>
  <div class="container my-novels-page">
    <div class="header-actions">
      <h1>Мои произведения</h1>
      <button class="btn-create" @click="router.push('/novels/create')">
        + Создать новеллу
      </button>
    </div>

    <div v-if="isLoading" class="loader-container">
      <div class="spinner"></div>
      <p>Загрузка ваших новелл...</p>
    </div>

    <div v-else-if="novels.length === 0" class="empty-state">
      <div class="empty-icon">📚</div>
      <h2>У вас пока нет новелл</h2>
      <p>Начните свой творческий путь прямо сейчас!</p>
      <button class="btn-create" @click="router.push('/novels/create')">Написать первую</button>
    </div>

    <div v-else class="novels-grid">
      <div v-for="novel in novels" :key="novel.id" class="novel-card">
        <div class="cover-wrapper">
          <img
            :src="novel.coverUrl || 'http://127.0.0.1:9000/interactive-novel-assets/Cover/default-cover.png'"
            alt="Обложка"
            class="card-cover"
          >
          <div class="status-badge" :class="novel.status.toLowerCase()">
            {{ novel.status }}
          </div>
        </div>
        <div class="novel-stats">
          <span title="Ререйтинг">⭐️ {{ calculateRating(novel) }}</span>
          <span title="Просмотры">👁‍🗨 {{ novel.viewCount }}</span>
          <span title="Главы">📑 {{ novel.chapterCount }}</span>
        </div>
        <div class="card-content">
          <h3 class="novel-title" :title="novel.title">{{ novel.title }}</h3>
          <p class="novel-description">{{ truncateText(novel.description, 60) }}</p>

          <div class="card-actions">
            <button @click="router.push(`/novels/${novel.id}/edit`)" class="btn-edit">
              ⚙️ Управление
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getMyNovels } from '@/api/novelService';
import type { NovelResponseDto } from '@/types/novel';

const router = useRouter();
const novels = ref<NovelResponseDto[]>([]); // Указываем тип массива
const isLoading = ref(true);

const truncateText = (text: string, length: number) => {
  if (!text) return '';
  return text.length > length ? text.substring(0, length) + '...' : text;
};

onMounted(async () => {
  try {
    const response = await getMyNovels();
    novels.value = response.content;
  } catch (e) {
    console.error("Ошибка загрузки новелл:", e);
  } finally {
    isLoading.value = false;
  }
});

const calculateRating = (novel: NovelResponseDto) => {
  if (!novel.ratingCount || novel.ratingCount === 0) return '0.0';
  return (novel.totalScore / novel.ratingCount).toFixed(1);
};
</script>

<style scoped>
.my-novels-page {
  padding: 100px 20px 40px;
  max-width: 1200px;
  margin: 0 auto;
  min-height: 100vh;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
}

h1 {
  color: var(--text-header);
  margin: 0;
}

.btn-create {
  padding: 12px 24px;
  background-color: #42b883; /* Цвет Vue, всегда выглядит хорошо */
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: bold;
  cursor: pointer;
  transition: 0.3s;
}

.btn-create:hover {
  background-color: #33a06f;
  transform: translateY(-2px);
}

.novels-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 30px;
}

.novel-card {
  background: var(--bg-dropdown);
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  transition: 0.3s;
  display: flex;
  flex-direction: column;
}

.novel-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0,0,0,0.2);
}

.novel-stats {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
  font-size: 0.85rem;
  color: var(--text-muted);
  border-top: 1px solid var(--border-subtle);
  padding-top: 10px;
}

.novel-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}


.cover-wrapper {
  position: relative;
  height: 320px;
}

.card-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.status-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: bold;
  text-transform: uppercase;
  background: rgba(0,0,0,0.7);
}

.status-badge.published { color: #42b883; border: 1px solid #42b883; }
.status-badge.draft { color: #ff9800; border: 1px solid #ff9800; }
.status-badge.in_progress{color: white ; border: 1px solid white; }
.card-content {
  padding: 20px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.novel-title {
  margin: 0 0 10px 0;
  font-size: 1.2rem;
  color: var(--text-header);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.novel-description {
  font-size: 0.9rem;
  color: #888;
  margin-bottom: 20px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-word;
}

.btn-edit {
  width: 100%;
  padding: 10px;
  background: var(--btn-plus);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
  transition: 0.2s;
  margin-top: auto;
}

.btn-edit:hover {
  filter: brightness(1.2);
}

.empty-state {
  text-align: center;
  padding: 60px;
  background: var(--bg-dropdown);
  border-radius: 12px;
  border: 1px dashed var(--border-color);
}

.empty-icon { font-size: 4rem; margin-bottom: 20px; }

.loader-container {
  text-align: center;
  padding: 100px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(66, 184, 131, 0.1);
  border-left-color: #42b883;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
