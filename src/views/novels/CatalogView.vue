<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from "vue-router";
import { findAllNovels, getAllGenres, getAllTags } from "@/api/novelService.ts";
import type { NovelResponseDto, NovelSearchRequestDto, TagOrGenreResponseDto } from "@/types/novel.ts";
import NovelCard from "@/components/NovelCard.vue";

const router = useRouter();
const novels = ref<NovelResponseDto[]>([]);
const genres = ref<TagOrGenreResponseDto[]>([]);
const tags = ref<TagOrGenreResponseDto[]>([]);
const isLoading = ref(true);
const currentPage = ref(0);
const totalPages = ref(1);
const isMobile = ref(false);

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

const isGenreModalOpen = ref(false);
const isTagModalOpen = ref(false);
const pagesCache = ref<Record<number, NovelResponseDto[]>>({});

// --- МЕТАДАННЫЕ ---
const fetchMetadata = async () => {
  try {
    const [genresData, tagsData] = await Promise.all([getAllGenres(), getAllTags()]);
    genres.value = genresData;
    tags.value = tagsData;
  } catch (e) {
    console.error("Ошибка загрузки метаданных", e);
  }
};

// --- ЛОГИКА ФИЛЬТРАЦИИ ---
const prepareFilters = (rawFilters: NovelSearchRequestDto) => {
  const cleanFilters: any = {};
  const keys: (keyof NovelSearchRequestDto)[] = [
    'title', 'authorId', 'status', 'includedGenreIds',
    'excludedGenreIds', 'includedTagIds', 'excludedTagIds',
    'minRating', 'maxRating'
  ];

  keys.forEach(key => {
    const value = rawFilters[key];
    if (value === null || value === undefined || value === '') return;
    if (key === 'minRating' && value === 0) return;
    if (key === 'maxRating' && value === 5) return;
    if (Array.isArray(value)) {
      if (value.length > 0) cleanFilters[key] = value;
      return;
    }
    cleanFilters[key] = value;
  });
  return cleanFilters;
};

const fetchFilteredNovels = async (page = 0, isPrefetch = false) => {
  if (!isPrefetch && pagesCache.value[page]) {
    novels.value = pagesCache.value[page];
    currentPage.value = page;
    window.scrollTo({ top: 0, behavior: 'smooth' });
    if (page < totalPages.value - 1 && !pagesCache.value[page + 1]) fetchFilteredNovels(page + 1, true);
    return;
  }

  try {
    if (!isPrefetch) isLoading.value = true;
    const cleanedDto = prepareFilters(filters.value);
    const response = await findAllNovels(cleanedDto, page, 12);

    pagesCache.value[page] = response.content;

    if (!isPrefetch) {
      novels.value = response.content;
      currentPage.value = page;
      totalPages.value = response.page?.totalPages || response.totalPages || 1;
      window.scrollTo({ top: 0, behavior: 'smooth' });
      if (page < totalPages.value - 1 && !pagesCache.value[page + 1]) fetchFilteredNovels(page + 1, true);
    }
  } catch (error) {
    console.error("Ошибка поиска:", error);
  } finally {
    if (!isPrefetch) isLoading.value = false;
  }
};

const applyFilters = () => {
  pagesCache.value = {};
  fetchFilteredNovels(0);
};

const resetFilters = () => {
  filters.value = {
    title: '',
    includedGenreIds: [],
    excludedGenreIds: [],
    includedTagIds: [],
    excludedTagIds: [],
    status: null,
    minRating: 0,
    maxRating: 5
  };
  genreSearchQuery.value = '';
  tagSearchQuery.value = '';
  applyFilters();
};

// --- ТРОЙНОЙ КЛИК И ПОИСК В МОДАЛКАХ ---
const toggleMetadata = (id: number, type: 'genre' | 'tag') => {
  const incKey = type === 'genre' ? 'includedGenreIds' : 'includedTagIds';
  const excKey = type === 'genre' ? 'excludedGenreIds' : 'excludedTagIds';
  const incIndex = filters.value[incKey]!.indexOf(id);
  const excIndex = filters.value[excKey]!.indexOf(id);

  if (incIndex > -1) {
    filters.value[incKey]!.splice(incIndex, 1);
    filters.value[excKey]!.push(id);
  } else if (excIndex > -1) {
    filters.value[excKey]!.splice(excIndex, 1);
  } else {
    filters.value[incKey]!.push(id);
  }
};

const genreSearchQuery = ref('');
const filteredGenres = computed(() => genres.value.filter(g => g.name.toLowerCase().includes(genreSearchQuery.value.toLowerCase())));

const tagSearchQuery = ref('');
const filteredTags = computed(() => {
  return tags.value.filter(t =>
    t.name.toLowerCase().includes(tagSearchQuery.value.toLowerCase())
  );
});

const goToNovelDetail = (id: number) => {
  router.push({ name: 'NovelDetail', params: { id: id.toString() } });
};

onMounted(() => {
  fetchMetadata();
  fetchFilteredNovels();
  isMobile.value = window.innerWidth <= 768;
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
                <input v-model="filters.title" type="text" placeholder="Название..." class="input-field" @keyup.enter="applyFilters"/>
              </div>

              <div class="filter-item">
                <label>Статус</label>
                <select v-model="filters.status" class="input-field">
                  <option :value="null">Любой</option>
                  <option value="IN_PROGRESS">📖 В процессе</option>
                  <option value="COMPLETED">✅ Завершено</option>
                  <option value="HIATUS">☕ Перерыв</option>
                </select>
              </div>

              <div class="filter-item">
                <label>Категории</label>
                <button class="modal-trigger-btn" @click="isGenreModalOpen = true">
                  🎭 Жанры ({{ filters.includedGenreIds.length + filters.excludedGenreIds.length }})
                </button>

                <button class="modal-trigger-btn" @click="isTagModalOpen = true">
                  # Теги ({{ filters.includedTagIds.length + filters.excludedTagIds.length }})
                </button>
              </div>

              <button class="btn-apply" @click="applyFilters">Показать результаты</button>
            </div>
          </div>
        </aside>

        <main class="content">
          <div v-if="isLoading" class="loader">
            <div class="spinner"></div>
            <p>Ищем лучшие истории...</p>
          </div>

          <div v-else-if="novels.length > 0">
            <div class="novels-grid">
              <NovelCard
                v-for="novel in novels"
                :key="novel.id"
                :novel="novel"
                :is-mobile="isMobile"
                @click="goToNovelDetail"
              />
            </div>

            <div v-if="totalPages > 1" class="pagination">
              <button :disabled="currentPage === 0" @click="fetchFilteredNovels(currentPage - 1)">←</button>
              <button
                v-for="page in totalPages"
                :key="page"
                class="page-num"
                :class="{ active: currentPage === page - 1 }"
                @click="fetchFilteredNovels(page - 1)"
              >
                {{ page }}
              </button>
              <button :disabled="currentPage >= totalPages - 1" @click="fetchFilteredNovels(currentPage + 1)">→</button>
            </div>
          </div>

          <div v-else class="empty">
            <p>Ничего не найдено. Попробуйте изменить фильтры.</p>
          </div>
        </main>
      </div>
    </div>

    <Transition name="fade">
      <div v-if="isGenreModalOpen" class="modal-overlay" @click.self="isGenreModalOpen = false">
        <div class="modal-content">
          <div class="modal-header">
            <h3>Выбор жанров</h3>
            <button class="close-btn" @click="isGenreModalOpen = false">&times;</button>
          </div>
          <input v-model="genreSearchQuery" type="text" placeholder="Поиск жанра..." class="modal-search" />
          <div class="modal-scroll-area">
            <button
              v-for="genre in filteredGenres"
              :key="genre.id"
              class="triple-chip"
              :class="{
                'included': filters.includedGenreIds?.includes(genre.id),
                'excluded': filters.excludedGenreIds?.includes(genre.id)
              }"
              @click="toggleMetadata(genre.id, 'genre')"
            >
              {{ genre.name }}
            </button>
          </div>
          <button class="btn-apply-modal" @click="isGenreModalOpen = false">Готово</button>
        </div>
      </div>
    </Transition>
    <Transition name="fade">
      <div v-if="isTagModalOpen" class="modal-overlay" @click.self="isTagModalOpen = false">
        <div class="modal-content">
          <div class="modal-header">
            <h3>Выбор тегов</h3>
            <button class="close-btn" @click="isTagModalOpen = false">&times;</button>
          </div>

          <input
            v-model="tagSearchQuery"
            type="text"
            placeholder="Поиск тега..."
            class="modal-search"
          />

          <div class="modal-scroll-area">
            <button
              v-for="tag in filteredTags"
              :key="tag.id"
              class="triple-chip"
              :class="{
            'included': filters.includedTagIds?.includes(tag.id),
            'excluded': filters.excludedTagIds?.includes(tag.id)
          }"
              @click="toggleMetadata(tag.id, 'tag')"
            >
              #{{ tag.name }}
            </button>
          </div>

          <button class="btn-apply-modal" @click="isTagModalOpen = false">Готово</button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
/* --- 1. ГЛОБАЛЬНЫЙ МАКЕТ (Сайдбар слева, контент справа) --- */
.catalog-page {
  min-height: 100vh;
  background: var(--bg-main);
  padding: 40px 0;
  color: var(--text-header);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.catalog-layout {
  display: flex;
  gap: 30px;
  align-items: flex-start;
}

.sidebar {
  width: 300px;
  flex-shrink: 0;
  position: sticky;
  top: 90px;
}

.content {
  flex-grow: 1;
}

/* --- 2. КАРТОЧКА ФИЛЬТРОВ В САЙДБАРЕ --- */
.filter-card {
  background: var(--bg-dropdown);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid var(--border-subtle);
}

.card-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  align-items: center;
}

.filter-item { margin-bottom: 20px; }

.filter-item label {
  display: block;
  font-size: 0.85rem;
  color: var(--text-muted);
  font-weight: 600;
  margin-bottom: 8px;
}

.input-field {
  width: 100%;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-main);
  color: var(--text-header);
}

/* Кнопки вызова модалок в сайдбаре */
.modal-trigger-btn {
  width: 100%;
  padding: 12px;
  margin-bottom: 10px;
  background: var(--bg-app);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: white;
  text-align: left;
  cursor: pointer;
  transition: 0.2s;
}

.modal-trigger-btn:hover {
  border-color: #6366f1;
  background: var(--hover-dropdowb);
}

/* --- 3. МОДАЛЬНОЕ ОКНО --- */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(5px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}

.modal-content {
  background: var(--bg-dropdown);
  width: 95%;
  max-width: 600px;
  border-radius: 20px;
  padding: 30px;
  border: 1px solid var(--border-color);
  box-shadow: 0 20px 50px rgba(0,0,0,0.5);
}

.modal-header { display: flex; justify-content: space-between; margin-bottom: 20px; }

.modal-search {
  width: 100%;
  padding: 12px;
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: white;
  margin-bottom: 20px;
}

.modal-scroll-area {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  max-height: 400px;
  overflow-y: auto;
  padding: 10px;
}

/* --- 4. ТРОЙНЫЕ ЧИПЫ (Внутри модалки) --- */
.triple-chip {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 0.9rem;
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  color: var(--text-header);
  cursor: pointer;
  transition: all 0.2s;
}

.triple-chip.included {
  background: rgba(66, 184, 131, 0.2);
  border-color: #42b883;
  color: #42b883;
}

.triple-chip.excluded {
  background: rgba(231, 76, 60, 0.2);
  border-color: #e74c3c;
  color: #e74c3c;
}

/* --- 5. КНОПКИ И УТИЛИТЫ --- */
.btn-apply {
  width: 100%;
  padding: 14px;
  background: #6366f1;
  color: white;
  border: none;
  border-radius: 10px;
  font-weight: bold;
  cursor: pointer;
}

.btn-text {
  background: none; border: none; color: #6366f1;
  cursor: pointer; text-decoration: underline; font-weight: bold;
}

/* --- 6. СЕТКА НОВЕЛЛ И ПАГИНАЦИЯ --- */
.novels-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 40px;
}

.pagination {
  display: flex; justify-content: center; align-items: center;
  gap: 10px; margin-top: 40px;
}

.pagination button {
  padding: 8px 16px; border-radius: 8px;
  background: var(--bg-dropdown); border: 1px solid var(--border-color);
  color: var(--text-header); cursor: pointer;
}

.pagination button.active {
  background: #6366f1; color: white; border-color: #6366f1;
}

/* --- 7. АДАПТИВ --- */
@media (max-width: 900px) {
  .catalog-layout { flex-direction: column; }
  .sidebar { width: 100%; position: static; }
}

.fade-enter-active, .fade-slide-enter-active { transition: all 0.3s ease; }
.fade-enter-from, .fade-slide-enter-from { opacity: 0; }
</style>
