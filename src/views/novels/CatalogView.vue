<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref, watch} from 'vue';
import {useRouter} from "vue-router";
import {findAllNovels} from "@/api/novelService.ts";
import {searchUsers} from "@/api/profileService.ts";
import type {
  NovelResponseDto,
  NovelSearchRequestDto,
  TagOrGenreResponseDto
} from "@/types/novel.ts";
import NovelCard from "@/components/NovelCard.vue";
import {DEFAULT_AVATAR} from "@/utils/media.ts";

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
  authorId: null,
  includedGenreIds: [],
  excludedGenreIds: [],
  includedTagIds: [],
  excludedTagIds: [],
  status: null,
  minRating: null,
  maxRating: null
});

const sort = ref('lastChapterAddedAt,desc');

const isGenreModalOpen = ref(false);
const isTagModalOpen = ref(false);
const pagesCache = ref<Record<number, NovelResponseDto[]>>({});


const authorSearchQuery = ref('');
const authorSearchResults = ref<any[]>([]);
const isAuthorSearching = ref(false);
const selectedAuthor = ref<any>(null);
const authorSearchPage = ref(0);
const authorSearchIsLastPage = ref(false);
const authorDropdownRef = ref<HTMLElement | null>(null);
const relativeWrapperRef = ref<HTMLElement | null>(null);

let authorSearchTimeout: number;

const fetchAuthors = async (page: number, append = false) => {
  if (authorSearchQuery.value.length < 2) return;

  isAuthorSearching.value = true;
  try {
    const data = await searchUsers(authorSearchQuery.value, page, 10);

    if (append) {
      authorSearchResults.value = [...authorSearchResults.value, ...data.content];
    } else {
      authorSearchResults.value = data.content;
    }


    const totalPages = data.page?.totalPages || 1;
    authorSearchIsLastPage.value = page >= totalPages - 1;

    authorSearchPage.value = page;
  } catch (e) {
    console.error(e);
  } finally {
    isAuthorSearching.value = false;
  }
};

watch(authorSearchQuery, (newQuery) => {
  if (selectedAuthor.value && selectedAuthor.value.username === newQuery) {
    return;
  }

  clearTimeout(authorSearchTimeout);
  if (newQuery.length < 2) {
    authorSearchResults.value = [];
    if (newQuery.length === 0) {
       selectedAuthor.value = null;
       filters.value.authorId = null;
    }
    return;
  }

  authorSearchTimeout = window.setTimeout(() => {
    fetchAuthors(0);
  }, 500);
});

const handleAuthorScroll = async (e: Event) => {
  const target = e.target as HTMLElement;
  const bottom = Math.abs(target.scrollHeight - target.scrollTop - target.clientHeight) < 20;

  if (bottom && !isAuthorSearching.value && !authorSearchIsLastPage.value) {
    await fetchAuthors(authorSearchPage.value + 1, true);
  }
};

const selectAuthor = (author: any) => {
  selectedAuthor.value = author;
  authorSearchQuery.value = author.username;
  filters.value.authorId = author.id;
  authorSearchResults.value = [];
};


const fetchMetadata = () => {
  genres.value = JSON.parse(localStorage.getItem('genres') || '[]');
  tags.value = JSON.parse(localStorage.getItem('tags') || '[]');
};


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
    const response = await findAllNovels(cleanedDto, page, 12, sort.value);

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
    authorId: null,
    includedGenreIds: [],
    excludedGenreIds: [],
    includedTagIds: [],
    excludedTagIds: [],
    status: null,
    minRating: null,
    maxRating: null
  };
  sort.value = 'lastChapterAddedAt,desc';
  genreSearchQuery.value = '';
  tagSearchQuery.value = '';
  authorSearchQuery.value = '';
  selectedAuthor.value = null;
  authorSearchResults.value = [];
  applyFilters();
};


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

const handleClickOutsideAuthor = (event: MouseEvent) => {
  if (authorSearchResults.value.length > 0 && relativeWrapperRef.value && !relativeWrapperRef.value.contains(event.target as Node)) {
    authorSearchResults.value = [];
  }
};

const handleEscKeyAuthor = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && authorSearchResults.value.length > 0) {
    authorSearchResults.value = [];
  }
};

onMounted(() => {
  fetchMetadata();
  fetchFilteredNovels();
  isMobile.value = window.innerWidth <= 768;
  document.addEventListener('click', handleClickOutsideAuthor);
  document.addEventListener('keydown', handleEscKeyAuthor);
});

onUnmounted(() => {
    document.removeEventListener('click', handleClickOutsideAuthor);
    document.removeEventListener('keydown', handleEscKeyAuthor);
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
                <label>Поиск по названию</label>
                <div class="relative-wrapper">
                  <input v-model="filters.title" type="text" placeholder="Название..." class="input-field" @keyup.enter="applyFilters"/>
                </div>
              </div>

              <div class="filter-item relative-wrapper" ref="relativeWrapperRef">
                <label>Поиск по автору</label>
                <input
                  v-model="authorSearchQuery"
                  type="text"
                  placeholder="Имя автора..."
                  class="input-field"
                  @focus="authorSearchResults.length > 0 ? null : (authorSearchQuery.length >= 2 ? fetchAuthors(0) : null)"
                />
                <div v-if="authorSearchResults.length > 0" class="autocomplete-dropdown scrollbar" ref="authorDropdownRef" @scroll="handleAuthorScroll">
                  <div
                    v-for="author in authorSearchResults"
                    :key="author.id"
                    class="autocomplete-item"
                    @click="selectAuthor(author)"
                  >
                    <img :src="author.avatarUrl || DEFAULT_AVATAR" class="author-avatar-sm" />
                    <span>{{ author.username }}</span>
                  </div>
                  <div v-if="isAuthorSearching" class="autocomplete-loading">Загрузка...</div>
                </div>
              </div>

              <div class="filter-item">
                <label>Статус</label>
                <div class="relative-wrapper">
                  <select v-model="filters.status" class="input-field custom-select">
                    <option :value="null">Любой</option>
                    <option value="IN_PROGRESS">В процессе</option>
                    <option value="COMPLETED">Завершено</option>
                    <option value="HIATUS">Перерыв</option>
                  </select>
                </div>
              </div>

              <div class="filter-item">
                <label>Рейтинг</label>
                <div class="rating-range">
                  <div class="relative-wrapper flex-1">
                    <input v-model.number="filters.minRating" type="number" min="0" max="5" step="0.1" placeholder="От" class="input-field small-input"/>
                  </div>
                  <span class="rating-separator">-</span>
                  <div class="relative-wrapper flex-1">
                    <input v-model.number="filters.maxRating" type="number" min="0" max="5" step="0.1" placeholder="До" class="input-field small-input"/>
                  </div>
                </div>
              </div>

              <div class="filter-item">
                <label>Сортировка</label>
                <div class="relative-wrapper">
                  <select v-model="sort" class="input-field custom-select sort-select" @change="applyFilters">
                    <option value="lastChapterAddedAt,desc">По дате обновления</option>
                    <option value="averageRating,desc">По рейтингу</option>
                    <option value="viewCount,desc">По просмотрам</option>
                    <option value="chapterCount,desc">По количеству глав</option>
                    <option value="publicationDate,desc">По дате публикации</option>
                  </select>
                </div>
              </div>

              <div class="filter-item">
                <label>Категории</label>
                <div class="categories-stack">
                  <button class="modal-trigger-btn" @click="isGenreModalOpen = true">
                    <span class="emoji-icon">🎭</span>
                    <span class="font-body-md">Жанры ({{ filters.includedGenreIds.length + filters.excludedGenreIds.length }})</span>
                  </button>

                  <button class="modal-trigger-btn" @click="isTagModalOpen = true">
                    <span class="emoji-icon">#</span>
                    <span class="font-body-md">Теги ({{ filters.includedTagIds.length + filters.excludedTagIds.length }})</span>
                  </button>
                </div>
              </div>

              <div class="apply-btn-wrapper">
                <button class="btn-apply" @click="applyFilters">Показать результаты</button>
              </div>
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
            <div class="empty-icon">🔍</div>
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

.catalog-page {
  min-height: 100vh;
  background: var(--bg-main);
  padding: 0px 0 60px;
  color: var(--text-header);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.catalog-header {
  margin-bottom: 40px;
}
.catalog-header h1 {
  font-size: 2.5rem;
  font-weight: 800;
  margin: 0;
  letter-spacing: -0.02em;
}
.accent {
  color: var(--btn-plus);
}

.catalog-layout {
  display: flex;
  gap: 40px;
  align-items: stretch;
}

.sidebar {
  width: 260px;
  flex-shrink: 0;
}

.content {
  flex-grow: 1;
}


.filter-card {
  background: var(--bg-dropdown);
  border-radius: 16px;
  padding: 16px;
  border: 1px solid var(--border-color);
  box-shadow: 0 4px 12px var(--shadow-color);

  position: sticky;
  top: 24px;
  max-height: calc(100vh - 48px);
  overflow-y: auto;
  scrollbar-width: thin;
}

.filter-card::-webkit-scrollbar {
  width: 6px;
}

.filter-card::-webkit-scrollbar-thumb {
  background-color: var(--border-color);
  border-radius: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  align-items: center;
}
.card-header h3 {
  margin: 0;
  font-size: 0.9rem;
  font-weight: 700;
}

.filter-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  margin-bottom: 0;
}

.filter-item label {
  display: block;
  font-size: x-small;
  color: var(--text-muted);
  font-weight: 600;
  margin: 0;
  letter-spacing: 0.01em;
}

.input-field {
  width: 100%;
  padding: 4px 0px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: var(--bg-main);
  color: var(--text-header);
  font-size: 0.8rem;
  transition: border-color var(--transition-base), box-shadow var(--transition-base);
}
.input-field:focus {
  outline: none;
  border-color: var(--btn-plus);
  box-shadow: 0 0 0 3px var(--primary-glow); /* Твой фирменный глоу при фокусе */
}
.input-field::placeholder {
  color: var(--input-placeholder);
}
.input-field.small-input::-webkit-outer-spin-button,
.input-field.small-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.input-field.small-input {
  -moz-appearance: textfield;
  text-align: center;
}
.rating-range {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
}

.flex-1 { flex: 1; }
.rating-separator { color: var(--text-muted); font-size: 0.8rem; }

.small-input {
  width: 100%;
}

.custom-select {
  appearance: none;
  background-image: url("data:image/svg+xml;charset=US-ASCII,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22292.4%22%20height%3D%22292.4%22%3E%3Cpath%20fill%3D%22%23a1a1aa%22%20d%3D%22M287%2069.4a17.6%2017.6%200%200%200-13-5.4H18.4c-5%200-9.3%201.8-12.9%205.4A17.6%2017.6%200%200%200%200%2082.2c0%205%201.8%209.3%205.4%2012.9l128%20127.9c3.6%203.6%207.8%205.4%2012.8%205.4s9.2-1.8%2012.8-5.4L287%2095c3.5-3.5%205.4-7.8%205.4-12.8%200-5-1.9-9.2-5.5-12.8z%22%2F%3E%3C%2Fsvg%3E");
  background-repeat: no-repeat;
  background-position: right 12px top 50%;
  background-size: 10px auto;
  cursor: pointer;
  padding-right: 28px;
  text-overflow: ellipsis;
}

.sort-select {
  background-image: url("data:image/svg+xml;charset=US-ASCII,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%2024%2024%22%20fill%3D%22none%22%20stroke%3D%22%23a1a1aa%22%20stroke-width%3D%222%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%3E%3Cline%20x1%3D%224%22%20y1%3D%226%22%20x2%3D%2220%22%20y2%3D%226%22%3E%3C%2Fline%3E%3Cline%20x1%3D%224%22%20y1%3D%2212%22%20x2%3D%2214%22%20y2%3D%2212%22%3E%3C%2Fline%3E%3Cline%20x1%3D%224%22%20y1%3D%2218%22%20x2%3D%228%22%20y2%3D%2218%22%3E%3C%2Fline%3E%3Cpolyline%20points%3D%2214%2015%2017%2018%2020%2015%22%3E%3C%2Fpolyline%3E%3Cline%20x1%3D%2217%22%20y1%3D%2218%22%20x2%3D%2217%22%20y2%3D%2212%22%3E%3C%2Fline%3E%3C%2Fsvg%3E");
  background-size: 16px auto;
}

.relative-wrapper {
  position: relative;
  width: 100%;
}

.small-input {
  width: 100%;
}

.appearance-none {
  appearance: none;
  -webkit-appearance: none;
}

.relative-wrapper {
  position: relative;
  width: 100%;
}

.select-icon {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  pointer-events: none;
}

.autocomplete-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  width: 90%;
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: 0 4px 12px var(--shadow-color);
  z-index: 100;
  max-height: 200px;
  overflow-y: auto;
  margin-top: 4px;
}

.autocomplete-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.autocomplete-item:hover {
  background: var(--hover-dropdowb);
}

.author-avatar-sm {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
}
.autocomplete-loading {
  padding: 8px;
  text-align: center;
  color: var(--text-muted);
  font-size: 0.85rem;
}



.categories-stack {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.modal-trigger-btn {
  width: 100%;
  padding: 8px 12px;
  background: var(--bg-header);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  color: var(--text-header);
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-weight: 500;
  font-size: 0.8rem;
}

.emoji-icon {
  font-size: 1.1rem;
}

.modal-trigger-btn:hover {
  background: var(--hover-dropdowb);
  border-color: var(--btn-plus);
}
.modal-trigger-btn:active {
  transform: scale(0.98);
}


.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}

.modal-content {
  background: var(--bg-dropdown);
  width: 95%;
  max-width: 600px;
  border-radius: 16px;
  padding: 32px;
  border: 1px solid var(--border-color);
  box-shadow: 0 20px 50px rgba(0,0,0,0.5);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
}

.modal-header { display: flex; justify-content: space-between; margin-bottom: 24px; align-items: center;}
.modal-header h3 { margin: 0; font-size: 1.5rem; }

.close-btn {
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 2rem;
  cursor: pointer;
  line-height: 1;
  padding: 0 8px;
  border-radius: 8px;
  transition: background 0.2s, color 0.2s;
}
.close-btn:hover {
  background: var(--hover-dropdowb);
  color: var(--text-header);
}

.modal-search {
  width: 100%;
  padding: 12px 1px;
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  color: var(--text-header);
  margin-bottom: 24px;
  font-size: 0.95rem;
}
.modal-search:focus { outline: none; border-color: var(--btn-plus); }

.modal-scroll-area {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  overflow-y: auto;
  padding: 4px;
  margin-bottom: 24px;
  flex-grow: 1;
}


.triple-chip {
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 0.9rem;
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  color: var(--text-header);
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}
.triple-chip:hover {
  border-color: var(--text-muted);
}

.triple-chip.included {
  background: rgba(16, 185, 129, 0.15);
  border-color: #10b981;
  color: #10b981;
}

.triple-chip.excluded {
  background: rgba(239, 68, 68, 0.15);
  border-color: #ef4444;
  color: #ef4444;
}


.apply-btn-wrapper {
  margin-top: 4px;
}

.btn-apply {
  width: 100%;
  padding: 10px;
  background: var(--btn-plus);
  color: white;
  border: none;
  border-radius: 12px;
  font-weight: 600;
  font-size: 0.85rem;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.2s;
}
.btn-apply:hover {
  opacity: 0.9;
}
.btn-apply:active {
  transform: scale(0.95);
}

.btn-apply-modal {
  width: 100%;
  padding: 14px;
  background: var(--btn-plus);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-apply-modal:hover {
  background: var(--btn-plus-hover);
}

.btn-text {
  background: none; border: none; color: var(--btn-plus);
  cursor: pointer; font-weight: 500; font-size: 0.85rem;
  padding: 4px 8px; border-radius: 4px; transition: background 0.2s;
}
.btn-text:hover { background: var(--hover-dropdowb); }


.novels-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 32px;
}

.pagination {
  display: flex; justify-content: center; align-items: center;
  gap: 12px; margin-top: 60px;
}

.pagination button {
  width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 8px;
  background: var(--bg-dropdown); border: 1px solid var(--border-color);
  color: var(--text-header); cursor: pointer;
  font-weight: 600; transition: all 0.2s;
}
.pagination button:hover:not(:disabled) {
  border-color: var(--text-muted);
}
.pagination button:disabled {
  opacity: 0.5; cursor: not-allowed;
}

.pagination button.active {
  background: var(--btn-plus); color: white; border-color: var(--btn-plus);
}

.empty {
  text-align: center;
  padding: 80px 20px;
  background: var(--bg-dropdown);
  border-radius: 16px;
  border: 1px dashed var(--border-color);
}
.empty-icon {
  font-size: 3rem; margin-bottom: 16px; opacity: 0.5;
}
.empty p { color: var(--text-muted); font-size: 1.1rem; }

.loader { text-align: center; padding: 100px 20px; color: var(--text-muted); }
.spinner {
  width: 40px; height: 40px; border: 3px solid var(--border-subtle);
  border-top-color: var(--btn-plus); border-radius: 50%;
  animation: spin 1s linear infinite; margin: 0 auto 20px;
}
@keyframes spin { to { transform: rotate(360deg); } }


@media (max-width: 600px) {
  .catalog-layout { flex-direction: column; }
  .sidebar { width: 100%; position: static; }
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.scrollbar {
  scrollbar-width: thin;
  scrollbar-color: var(--border-color) transparent;
}
</style>
