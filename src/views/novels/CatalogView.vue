<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRouter } from "vue-router";
import { searchNovels, getAllGenres, getAllTags } from "@/api/novelService.ts";
import type { NovelResponseDto, NovelSearchRequestDto, TagOrGenreResponseDto } from "@/types/novel.ts";

const router = useRouter();
const novels = ref<NovelResponseDto[]>([]);
const genres = ref<TagOrGenreResponseDto[]>([]);
const tags = ref<TagOrGenreResponseDto[]>([]);
const isLoading = ref(true);

const currentPage = ref(0);
const totalPages = ref(1);

// Объект фильтров: добавлены minRating и maxRating
const filters = ref<NovelSearchRequestDto>({
  title: null,
  includedGenreIds: [],
  excludedGenreIds: [],
  includedTagIds: [],
  excludedTagIds: [],
  status: null,
  minRating: 0,
  maxRating: 5
});

const fetchMetadata = async () => {
  try {
    const [genresData, tagsData] = await Promise.all([getAllGenres(), getAllTags()]);
    genres.value = genresData;
    tags.value = tagsData;
  } catch (e) {
    console.error("Ошибка загрузки метаданных", e);
  }
};

const prepareFilters = (rawFilters: NovelSearchRequestDto) => {
  const cleanFilters: any = {};

  Object.entries(rawFilters).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      // Игнорируем дефолтные значения рейтинга, чтобы не нагружать запрос
      if (key === 'minRating' && value === 0) return;
      if (key === 'maxRating' && value === 5) return;

      if (Array.isArray(value)) {
        if (value.length > 0) cleanFilters[key] = value;
      } else {
        cleanFilters[key] = value;
      }
    }
  });

  return cleanFilters;
};

const fetchFilteredNovels = async (page = 0) => {
  try {
    isLoading.value = true;
    currentPage.value = page;

    const cleanedDto = prepareFilters(filters.value);
    const response = await searchNovels(cleanedDto, page, 12);

    novels.value = response.content;
    totalPages.value = response.totalPages;
  } catch (error) {
    console.error("Ошибка поиска:", error);
  } finally {
    isLoading.value = false;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
};

const toggleGenre = (id: number) => {
  const index = filters.value.includedGenreIds!.indexOf(id);
  if (index > -1) filters.value.includedGenreIds!.splice(index, 1);
  else filters.value.includedGenreIds!.push(id);
  fetchFilteredNovels(0);
};

const resetFilters = () => {
  filters.value = {
    title: '',
    includedGenreIds: [],
    excludedGenreIds: [],
    includedTagIds: [],
    excludedTagIds: [],
    status: '',
    minRating: 0,
    maxRating: 5
  };
  fetchFilteredNovels(0);
};

onMounted(() => {
  fetchMetadata();
  fetchFilteredNovels();
});

let debounceTimer: number;
watch(() => filters.value.title, () => {
  clearTimeout(debounceTimer);
  debounceTimer = window.setTimeout(() => fetchFilteredNovels(0), 500);
});
</script>

<template>
  <div class="catalog-page">
    <div class="container">
      <header class="catalog-header">
        <h1>Каталог <span class="accent">новелл</span></h1>
      </header>

      <div class="catalog-layout">
        <aside class="sidebar">
          <div class="filter-card">
            <div class="card-header">
              <h3>Фильтры</h3>
              <button class="btn-text" @click="resetFilters">Сбросить</button>
            </div>

            <div class="filter-body">
              <div class="filter-item">
                <label>Поиск</label>
                <input v-model="filters.title" type="text" placeholder="Название..." class="input-field" />
              </div>

              <div class="filter-item">
                <label>Статус</label>
                <select v-model="filters.status" @change="fetchFilteredNovels(0)" class="input-field">
                  <option value="">Все</option>
                  <option value="IN_PROGRESS">В процессе</option>
                  <option value="COMPLETED">Завершено</option>
                  <option value="HIATUS">Перерыв</option>
                </select>
              </div>

              <div class="filter-item">
                <div class="label-row">
                  <label>Мин. рейтинг</label>
                  <span class="value-tag">{{ filters.minRating }}</span>
                </div>
                <input
                  v-model.number="filters.minRating"
                  type="range" min="0" max="5" step="0.1"
                  class="range-field"
                  @change="fetchFilteredNovels(0)"
                />
              </div>

              <div class="filter-item">
                <div class="label-row">
                  <label>Макс. рейтинг</label>
                  <span class="value-tag">{{ filters.maxRating }}</span>
                </div>
                <input
                  v-model.number="filters.maxRating"
                  type="range" min="0" max="5" step="0.1"
                  class="range-field"
                  @change="fetchFilteredNovels(0)"
                />
              </div>

              <div class="filter-item">
                <label>Жанры</label>
                <div class="genre-grid">
                  <button
                    v-for="genre in genres"
                    :key="genre.id"
                    :class="['genre-chip', { active: filters.includedGenreIds?.includes(genre.id) }]"
                    @click="toggleGenre(genre.id)"
                  >
                    {{ genre.name }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </aside>

        <main class="content">
          <div v-if="isLoading" class="loader">
            <div class="spinner"></div>
            <p>Загрузка историй...</p>
          </div>

          <div v-else-if="novels.length > 0">
            <div class="novels-grid">
              <div
                v-for="novel in novels"
                :key="novel.id"
                class="novel-card"
                @click="router.push(`/novel/${novel.id}`)"
              >
                <div class="cover-box">
                  <img :src="novel.coverUrl || 'http://127.0.0.1:9000/interactive-novel-assets/Cover/default-cover.png'" alt="cover" />
                  <div class="badge">{{ novel.status }}</div>
                </div>
                <div class="info-box">
                  <h4>{{ novel.title }}</h4>
                  <div class="meta">⭐ {{ (novel.totalScore / (novel.ratingCount || 1)).toFixed(1) }}</div>
                </div>
              </div>
            </div>

            <div v-if="totalPages > 1" class="pagination">
              <button :disabled="currentPage === 0" @click="fetchFilteredNovels(currentPage - 1)">←</button>
              <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
              <button :disabled="currentPage >= totalPages - 1" @click="fetchFilteredNovels(currentPage + 1)">→</button>
            </div>
          </div>

          <div v-else class="empty">
            <p>Ничего не найдено. Попробуйте другие фильтры.</p>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Добавленные/измененные стили */
.label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.value-tag {
  background: #6366f1;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.75rem;
}

.range-field {
  width: 100%;
  accent-color: #6366f1;
  cursor: pointer;
}

/* Остальные стили из предыдущего кода */
.catalog-page { min-height: 100vh; background: var(--bg-main); padding: 40px 0; color: var(--text-header); }
.container { max-width: 1200px; margin: 0 auto; padding: 0 20px; }
.accent { color: #6366f1; }
.catalog-header h1 { font-size: 2.5rem; margin-bottom: 30px; }
.catalog-layout { display: grid; grid-template-columns: 280px 1fr; gap: 40px; }
.filter-card { background: var(--bg-dropdown); border-radius: 16px; padding: 20px; border: 1px solid var(--border-subtle); position: sticky; top: 100px; }
.card-header { display: flex; justify-content: space-between; margin-bottom: 20px; }
.btn-text { background: none; border: none; color: #6366f1; cursor: pointer; text-decoration: underline; }
.filter-item { margin-bottom: 20px; }
.filter-item label { display: block; font-size: 0.85rem; color: var(--text-muted); font-weight: 600; }
.input-field { width: 100%; padding: 10px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-main); color: var(--text-header); }
.genre-grid { display: flex; flex-wrap: wrap; gap: 6px; }
.genre-chip { padding: 4px 10px; border-radius: 6px; font-size: 0.75rem; background: var(--bg-main); border: 1px solid var(--border-color); color: var(--text-header); cursor: pointer; }
.genre-chip.active { background: #6366f1; color: white; border-color: #6366f1; }
.novels-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 20px; }
.novel-card { background: var(--bg-dropdown); border-radius: 12px; overflow: hidden; border: 1px solid var(--border-subtle); cursor: pointer; transition: 0.3s; }
.novel-card:hover { transform: translateY(-5px); box-shadow: 0 10px 20px rgba(0,0,0,0.2); }
.cover-box { position: relative; height: 280px; }
.cover-box img { width: 100%; height: 100%; object-fit: cover; }
.badge { position: absolute; top: 8px; left: 8px; background: rgba(0,0,0,0.7); font-size: 0.65rem; padding: 3px 6px; border-radius: 4px; color: white; }
.info-box { padding: 12px; }
.info-box h4 { font-size: 1rem; margin-bottom: 5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.meta { font-size: 0.8rem; color: #f1c40f; font-weight: bold; }
.pagination { display: flex; justify-content: center; align-items: center; gap: 15px; margin-top: 40px; }
.pagination button { padding: 8px 16px; border-radius: 8px; background: var(--bg-dropdown); border: 1px solid var(--border-color); color: var(--text-header); cursor: pointer; }
.pagination button:disabled { opacity: 0.3; }
.loader { text-align: center; padding: 100px 0; }
.spinner { width: 40px; height: 40px; border: 4px solid rgba(255,255,255,0.1); border-top-color: #6366f1; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 15px; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 900px) {
  .catalog-layout { grid-template-columns: 1fr; }
  .sidebar { position: static; }
}
</style>
