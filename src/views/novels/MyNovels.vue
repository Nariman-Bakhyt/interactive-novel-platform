<template>
  <div class="container my-novels-page">
    <div class="header-actions">
      <h1>Мои произведения</h1>
      <button class="btn-create" @click="router.push('/novels/create')">
        <span class="icon">+</span> Создать новеллу
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
            :src="novel.coverUrl || DEFAULT_COVER"
            alt="Обложка"
            class="card-cover"
          >
          <div class="status-badge" :class="novel.status.toLowerCase()">
            {{ novel.status }}
          </div>
        </div>
        <div class="novel-stats">
          <span title="Рейтинг">⭐️ {{ calculateRating(novel) }}</span>
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
import {onMounted, ref} from 'vue';
import {useRouter} from 'vue-router';
import {getMyNovels} from '@/api/novelService';
import type {NovelResponseDto} from '@/types/novel';
import {DEFAULT_COVER} from '@/utils/media';

const router = useRouter();
const novels = ref<NovelResponseDto[]>([]); 
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
  padding: 100px 24px 60px;
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
  font-size: 2.25rem;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.btn-create {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background-color: var(--btn-plus);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.2s, transform 0.2s;
}

.btn-create:hover {
  background-color: var(--btn-plus-hover);
  transform: translateY(-2px);
}

.novels-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 32px;
}

.novel-card {
  background: var(--bg-dropdown);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  transition: transform 0.3s, box-shadow 0.3s, border-color 0.3s;
  display: flex;
  flex-direction: column;
}

.novel-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px var(--shadow-color);
  border-color: var(--btn-plus);
}

.novel-stats {
  display: flex;
  gap: 16px;
  margin: 16px 20px 0;
  font-size: 0.85rem;
  color: var(--text-muted);
  border-bottom: 1px solid var(--border-subtle);
  padding-bottom: 12px;
}

.novel-stats span {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}


.cover-wrapper {
  position: relative;
  height: 340px;
  overflow: hidden;
}

.card-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s;
}

.novel-card:hover .card-cover {
  transform: scale(1.05);
}

.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  background: rgba(0,0,0,0.75);
  backdrop-filter: blur(4px);
  letter-spacing: 0.05em;
}

.status-badge.published { color: #10b981; border: 1px solid rgba(16, 185, 129, 0.3); } 
.status-badge.draft { color: #f59e0b; border: 1px solid rgba(245, 158, 11, 0.3); } 
.status-badge.in_progress{color: #e4e4e7 ; border: 1px solid rgba(228, 228, 231, 0.3); }
.card-content {
  padding: 20px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.novel-title {
  margin: 0 0 8px 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-header);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.novel-description {
  font-size: 0.95rem;
  color: var(--text-muted);
  margin-bottom: 24px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-word;
}

.card-actions {
  margin-top: auto;
}

.btn-edit {
  width: 100%;
  padding: 12px;
  background: var(--bg-main);
  color: var(--text-header);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.95rem;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-edit:hover {
  background: var(--hover-dropdowb);
  border-color: var(--text-muted);
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: var(--bg-dropdown);
  border-radius: 16px;
  border: 1px dashed var(--border-color);
}

.empty-icon { font-size: 4rem; margin-bottom: 24px; }

.empty-state h2 {
  margin: 0 0 12px;
  font-size: 1.5rem;
}

.empty-state p {
  color: var(--text-muted);
  margin-bottom: 24px;
}

.loader-container {
  text-align: center;
  padding: 100px;
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
</style>
