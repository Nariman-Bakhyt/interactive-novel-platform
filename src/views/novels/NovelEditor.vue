<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {
  createNovel,
  getAllGenres,
  getAllTags,
  getMyNovel,
  updateNovel,
  uploadNovelCover
} from '@/api/novelService';
import type {
  ChapterShortResponseDto,
  NovelRequestDto,
  TagOrGenreResponseDto
} from "@/types/novel.ts";
import {useToastStore} from "@/components/toast/toastStore.ts";

const route = useRoute();
const router = useRouter();
const toastStore = useToastStore();
// Состояния
const novelId = computed(() => route.params.id);
const isEditMode = computed(() => !!novelId.value);
const isEditingNow = ref(false);
const isLoading = ref(false);
const isSaving = ref(false);
const isUploadingCover = ref(false);

const defaultCover = 'http://127.0.0.1:9000/interactive-novel-assets/covers/default-cover.png';
const fileInput = ref<HTMLInputElement | null>(null);
const chaptersList = ref<ChapterShortResponseDto[]>([]);
const allGenres = ref<TagOrGenreResponseDto[]>([]);
const allTags = ref<TagOrGenreResponseDto[]>([]);

const form = ref({
  title: '',
  status: 'DRAFT',
  description: '',
  cover: '',
  coverImage: null as File | null,
  genres: [] as TagOrGenreResponseDto[],
  tags: [] as TagOrGenreResponseDto[]
});
const statusOptions = [
  { value: 'DRAFT', label: 'Черновик' },
  { value: 'IN_PROGRESS', label: 'В процессе' },
  { value: 'COMPLETED', label: 'Завершено' },
  { value: 'HIATUS', label: 'Перерыв' },
  { value: 'ARCHIVED', label: 'В архиве' }
];

onUnmounted(() => {
  if (form.value.cover && form.value.cover.startsWith('blob:')) {
    URL.revokeObjectURL(form.value.cover);
  }
});

// Загрузка данных
onMounted(async () => {
  try {

    const [genresData, tagsData] = await Promise.all([
      getAllGenres(),
      getAllTags()
    ]);
    allGenres.value = genresData;
    allTags.value = tagsData;

    if (isEditMode.value) {
      isLoading.value = true;
      const data = await getMyNovel(Number(novelId.value));
      form.value = {
        title: data.novel.title,
        status: data.novel.status,
        description: data.novel.description,
        cover: data.novel.coverUrl,
        coverImage: null,
        genres: data.novel.genres || [],
        tags: data.novel.tags || []
      };
      chaptersList.value = data.chapters || [];
      isEditingNow.value = false;
    }
  } catch (e) {
    console.error("Ошибка инициализации:", e);
  } finally {
    isLoading.value = false;
  }
});

const toggleItem = (item: TagOrGenreResponseDto, listName: 'genres' | 'tags') => {
  const index = form.value[listName].findIndex(i => i.id === item.id);
  if (index === -1) {
    form.value[listName].push(item);
  } else {
    form.value[listName].splice(index, 1);
  }
};

const isSelected = (id: number, listName: 'genres' | 'tags') => {
  return form.value[listName].some(i => i.id === id);
};

// Сохранение текстовых данных
const saveNovel = async () => {
  isSaving.value = true;

  // Собираем payload строго по DTO
  const payload: NovelRequestDto = {
    title: form.value.title,
    status: (isEditMode.value ? form.value.status : 'DRAFT') as string,
    description: form.value.description,
    coverImage: form.value.coverImage, // Передаем файл, если он есть
    genres: form.value.genres.map(g => g.id),
    tags: form.value.tags.map(t => t.id)
  };
  try {
    if (isEditMode.value) {
      await updateNovel(payload, Number(novelId.value));
      isEditingNow.value = false;
    } else {
      const newNovel = await createNovel(payload);
      router.push(`/novels/${newNovel.id}/edit`);
    }
  } catch (e) {
    toastStore.error("Ошибка при сохранении");
  } finally {
    isSaving.value = false;
  }
};

// Загрузка файла обложки
const triggerFileUpload = () => fileInput.value?.click();

const handleDeleteCover = async () => {
  if (!confirm('Вы уверены, что хотите полностью удалить обложку?')) return;

  // Если мы на этапе создания, просто стираем локальные данные
  if (!isEditMode.value) {
    form.value.coverImage = null;
    form.value.cover = '';
    return;
  }

  // Если это режим редактирования, шлем запрос на удаление
  isUploadingCover.value = true;
  try {
    const updatedNovel = await uploadNovelCover(Number(novelId.value), null);
    form.value.cover = updatedNovel.coverUrl;
    toastStore.success('Обложка успешно удалена');
  } catch (e: any) {
    console.error("Ошибка при удалении:", e);
    toastStore.error(e.message || 'Не удалось удалить обложку');
  } finally {
    isUploadingCover.value = false;
  }
};

const handleCoverUpload = async (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file) return;

  // 1. Показываем локальное превью пользователю сразу
  if (form.value.cover.startsWith('blob:')) {
    URL.revokeObjectURL(form.value.cover);
  }
  form.value.cover = URL.createObjectURL(file);

  // 2. Разделяем логику для редактирования и создания
  if (isEditMode.value && novelId.value) {
    // Если новелла уже существует — загружаем в MinIO сразу (по твоему API)
    isUploadingCover.value = true;
    try {
      const updatedNovel = await uploadNovelCover(Number(novelId.value), file);
      form.value.cover = updatedNovel.coverUrl;
    } catch (e) {
      toastStore.error("Ошибка при загрузке обложки");
    } finally {
      isUploadingCover.value = false;
    }
  } else {
    // Если мы только создаем новеллу — просто запоминаем файл в форму
    form.value.coverImage = file;
  }
};

const truncate = (text: string, length: number) => {
  if (!text) return '';
  return text.length > length ? text.substring(0, length) + '...' : text;
};
</script>

<template>
  <div class="editor-page-container">
    <div class="container editor-page">
      <div v-if="isLoading" class="loading-state">
        <div class="spinner"></div>
        <p>Загрузка...</p>
      </div>

      <Transition name="fade-slide" mode="out-in">
        <div v-if="isEditMode && !isEditingNow" class="novel-view-wrapper" key="view">
          <div class="novel-view-card">
            <header class="novel-header">
              <span :class="['status-badge', form.status.toLowerCase()]">{{ form.status }}</span>
              <h1 class="novel-main-title">{{ form.title }}</h1>
              <img :src="form.cover || defaultCover" class="card-cover-preview">
            </header>

            <div class="novel-metadata-chips">
              <div class="chip-group">
                <span v-for="g in form.genres" :key="g.id" class="chip genre-chip">{{ g.name }}</span>
              </div>
              <div class="chip-group">
                <span v-for="t in form.tags" :key="t.id" class="chip tag-chip">#{{ t.name }}</span>
              </div>
            </div>

            <div class="novel-content">
              <h3 class="section-title">Аннотация</h3>
              <div class="full-description">
                {{ form.description || 'Описание пока не заполнено...' }}
              </div>
            </div>

            <hr class="divider" />

            <div class="table-of-contents">
              <div class="toc-header">
                <h3 class="section-title">Оглавление</h3>
                <span class="chapter-count">Всего глав: {{ chaptersList.length }}</span>
              </div>

              <div v-if="chaptersList.length > 0" class="chapters-list">
                <div
                  v-for="chapter in chaptersList"
                  :key="chapter.id"
                  class="chapter-item"
                  @click="router.push(`/novels/${novelId}/chapters/${chapter.id}/edit`)"
                >
                  <span class="ch-number">Глава {{ chapter.chapterNumber }}:</span>
                  <span class="ch-title">{{ chapter.title }}</span>
                  <span class="ch-icon">📖</span>
                </div>
              </div>
              <div v-else class="no-chapters">
                <p>В этой новелле пока нет глав.</p>
              </div>
            </div>

            <div class="view-footer">
              <button @click="isEditingNow = true" class="btn-edit-main">✏️ Настройки новеллы</button>
              <button @click="router.push(`/novels/${novelId}/chapters/create`)" class="btn-add-chapter">
                ➕ Добавить главу
              </button>
            </div>
          </div>
        </div>

        <div v-else class="novel-editor-layout" key="edit">
          <main class="novel-form-main">
            <header class="form-header">
              <h1>{{ isEditMode ? 'Настройки произведения' : 'Создание новой истории' }}</h1>
              <p class="subtitle">Заполните данные, чтобы читатели могли найти вашу новеллу</p>
            </header>

            <form @submit.prevent="saveNovel" class="novel-form-grid">
              <div class="form-content-left">
                <div class="form-group">
                  <label>Название произведения</label>
                  <input v-model="form.title" type="text" placeholder="Введите захватывающее название..." required class="large-input" />
                </div>

                <div class="form-group">
                  <label>Аннотация (описание)</label>
                  <textarea v-model="form.description" rows="15" placeholder="О чем ваша история? Коротко опишите завязку и главных героев..." required></textarea>
                </div>
              </div>

              <aside class="form-sidebar-right">
                <div class="form-group">
                  <label>Обложка</label>
                  <div class="cover-upload-zone" @click="triggerFileUpload">
                    <img :src="form.cover || defaultCover" class="upload-preview-img">
                    <div class="upload-overlay">
                      <span>{{ isUploadingCover ? 'Загрузка...' : 'Изменить обложку' }}</span>
                    </div>
                  </div>
                  <button
                    v-if="form.cover && form.cover !== defaultCover"
                    type="button"
                    class="btn-remove-link"
                    @click="handleDeleteCover"
                    :disabled="isUploadingCover"
                  >
                    <span class="icon">🗑️</span> Удалить текущую обложку
                  </button>
                  <input type="file" ref="fileInput" @change="handleCoverUpload" accept="image/*" hidden>
                </div>

                <div class="form-group" v-if="isEditMode">
                  <label>Статус</label> <div class="status-selector">
                  <button
                    v-for="status in statusOptions"
                    :key="status.value"
                    type="button"
                    :class="['status-btn', { active: form.status === status.value }]"
                    @click="form.status = status.value"
                  >
                    {{ status.label }}
                  </button>
                </div>
                </div>

                <div class="form-group">
                  <label>Жанры</label>
                  <div class="tags-selector">
                    <button
                      v-for="genre in allGenres"
                      :key="genre.id"
                      type="button"
                      :class="['tag-option', 'genre', { active: isSelected(genre.id, 'genres') }]"
                      @click="toggleItem(genre, 'genres')"
                    >
                      {{ genre.name }}
                    </button>
                  </div>
                </div>

                <div class="form-group">
                  <label>Теги</label>
                  <div class="tags-selector">
                    <button
                      v-for="tag in allTags"
                      :key="tag.id"
                      type="button"
                      :class="['tag-option', { active: isSelected(tag.id, 'tags') }]"
                      @click="toggleItem(tag, 'tags')"
                    >
                      #{{ tag.name }}
                    </button>
                  </div>
                </div>
              </aside>

              <div class="form-full-width-actions">
                <button v-if="isEditMode" type="button" @click="isEditingNow = false" class="btn-secondary">Отмена</button>
                <button type="submit" class="btn-primary" :disabled="isSaving">
                  {{ isSaving ? 'Сохранение...' : (isEditMode ? 'Сохранить изменения' : 'Опубликовать новеллу') }}
                </button>
              </div>
            </form>
          </main>
        </div>
      </Transition>
    </div>
  </div>
</template>
<style scoped>
.editor-page-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  background-color: var(--bg-main);
  transition: background-color 0.3s ease;
}

.editor-page {
  padding-top: 80px;
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
}

.novel-view-wrapper {
  display: flex;
  justify-content: center;
  width: 100%;
}

.novel-view-card {
  background: var(--bg-dropdown);
  padding: 40px;
  border-radius: 20px;
  width: 100%;
  max-width: 700px;
  display: flex;
  flex-direction: column;
  align-items: center; /* Центрируем всё содержимое */
  text-align: center;

  border: 1px solid var(--border-subtle);
  transition: background-color 0.3s ease, border-color 0.3s ease, transform 0.2s ease;
}

.card-cover-preview {
  width: 240px;
  height: 340px;
  object-fit: cover;
  border-radius: 12px;
  margin: 20px 0;
  border: 1px solid var(--border-color);
  box-shadow: 0 10px 30px var(--shadow-color);
  transition: border-color 0.3s ease;
}

.description-text {
  color: var(--bg-profile);
  line-height: 1.7;
  white-space: pre-wrap;
  text-align: center; /* Центрируем текст аннотации в просмотре */
  word-break: break-word;
  overflow-wrap: anywhere;
  max-width: 100%;
}

.novel-editor-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 40px;
  align-items: start;
}

.novel-preview-sidebar {
  position: sticky;
  top: 100px;
}

.mini-card {
  background: var(--bg-dropdown);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  border: 1px solid var(--border-subtle);
  transition: background-color 0.3s ease, border-color 0.3s ease, transform 0.2s ease;
}

.mini-card:hover { transform: translateY(-5px); }

.image-container {
  width: 100%;
  height: 220px;
  position: relative;
}

.mini-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border: 1px solid var(--border-color);
  box-shadow: 0 10px 30px var(--shadow-color);
  transition: border-color 0.3s ease;
}

.mini-info {
  padding: 20px;
  width: 100%;
  box-sizing: border-box;
}

.mini-info h4 {
  margin: 10px 0 10px 0;
  color: var(--text-header);
  transition: color 0.3s ease;
  font-size: 1.2rem;
}

.mini-desc {
  font-size: 0.9rem;
  line-height: 1.4;
  margin: 0;
  color: var(--text-muted);
  transition: color 0.3s ease;
}


.novel-form-main {
  background:  var(--bg-dropdown);

  padding: 40px;
  border-radius: 20px;
  border: 1px solid var(--border-subtle);
  transition: background-color 0.3s ease, border-color 0.3s ease, transform 0.2s ease;
}

.novel-form-main h1 {
  color: var(--text-header);
  transition: color 0.3s ease;
  margin-top: 0;
  margin-bottom: 30px;
  text-align: left; /* Заголовок формы можно оставить слева */
}

.form-group {
  margin-bottom: 25px;
  text-align: left;
}

.form-group label {
  display: block;
  margin-bottom: 10px;
  color: var(--text-header);
  transition: color 0.3s ease;
}

.form-group input, .form-group textarea {
  width: 100%;
  background: var(--bg-main); /* Используем фон приложения для контраста */
  border: 1px solid var(--border-color);
  color: var(--text-header);
  padding: 15px;
  border-radius: 10px;
  box-sizing: border-box;
  transition: background-color 0.3s ease, border-color 0.3s ease, color 0.3s ease;
}
.form-group input::placeholder, .form-group textarea::placeholder {
  color: var(--text-muted);
  opacity: 0.6;
}
.form-actions {
  display: flex;
  gap: 15px;
}

.btn-save {
  background: #42b883;
  color: white;
  border: none;
  padding: 15px 30px;
  border-radius: 10px;
  cursor: pointer;
  font-weight: bold;
  flex: 1;
}

.btn-cancel {
  padding: 15px 25px;
  border-radius: 10px;
  cursor: pointer;
  background: var(--bg-main);
  color: var(--text-header);
  border: 1px solid var(--border-color);
  transition: all 0.3s ease;
}

@media (max-width: 900px) {
  .novel-editor-layout {
    grid-template-columns: 1fr;
  }
  .novel-preview-sidebar {
    position: static;
    margin-bottom: 30px;
  }
}

.novel-metadata-chips {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}
.chip-group {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}
.chip {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.85rem;
  border: 1px solid var(--border-color);
  background: var(--border-subtle);
  color: var(--text-muted);
  transition: all 0.3s ease;
}
.genre-chip { color: #42b883; border-color: #42b883; }
.tag-chip { color: #aaa; }

/* Сетка выбора в форме */
.selector-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 5px;
}
.selector-btn {
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  color: var(--text-muted);
  transition: all 0.3s ease;
}
.selector-btn:hover { background: var(--hover-dropdowb); }
.selector-btn.active {
  background: #42b883;
  color: white;
  border-color: #42b883;
}
.tag-btn.active {
  background: #2980b9;
  border-color: #2980b9;
}
.full-description {
  color: var(--text-muted);
  transition: color 0.3s ease;
  line-height: 1.8;
  white-space: pre-wrap; /* Сохраняет переносы строк из базы */
  text-align: left; /* Аннотацию лучше читать слева */
  width: 100%;
  margin-bottom: 30px;
}
.section-title {
  margin-bottom: 15px;
  color: var(--text-header);
  transition: color 0.3s ease;
  font-size: 1.3rem;
  text-align: left;
}
.divider {
  width: 100%;
  border: 0;
  border-top: 1px solid var(--border-subtle);
  margin: 20px 0;
}
.table-of-contents {
  width: 100%;
  text-align: left;
}

.toc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chapter-count {
  font-size: 0.9rem;
  color: var(--text-muted);
}

.chapters-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 15px;
}

.chapter-item {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  background: var(--bg-main);
  border-radius: 10px;
  border: 1px solid transparent;
  transition: all 0.3s ease;
  cursor: pointer;
  user-select: none;
}

.chapter-item:hover {
  background: rgba(66, 184, 131, 0.1); /* Цвет Vue с прозрачностью */
  border-color: rgba(66, 184, 131, 0.3);
  transform: translateX(5px); /* Легкий сдвиг вправо при наведении */
}

.ch-number {
  font-weight: bold;
  color: #42b883;
  margin-right: 10px;
  min-width: 85px;
}

.ch-title {
  flex-grow: 1;
  color: var(--text-header);
  transition: color 0.3s ease;
}


.ch-icon {
  opacity: 0.3;
  transition: opacity 0.2s;
}
.chapter-item:hover .ch-icon {
  opacity: 1;
}

.view-footer {
  display: flex;
  gap: 20px;
  margin-top: 40px;
  width: 100%;
}

.btn-edit-main {
  flex: 1;
  padding: 12px;
  background: var(--bg-main);
  color: var(--text-header);
  border-radius: 8px;
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.3s ease;
}
.btn-edit-main:hover {
  background: var(--hover-dropdowb);
}

.btn-add-chapter {
  flex: 1;
  padding: 12px;
  background: #42b883;
  color: white;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-weight: bold;
}
/* Обновленная сетка редактора */
.novel-editor-layout {
  display: block; /* Убираем старую сетку */
  width: 100%;
}

.novel-form-main {
  background: var(--bg-dropdown);
  padding: 50px;
  border-radius: 24px;
  border: 1px solid var(--border-subtle);
  box-shadow: 0 20px 50px rgba(0,0,0,0.2);
}

.form-header {
  margin-bottom: 40px;
  border-bottom: 1px solid var(--border-subtle);
  padding-bottom: 20px;
}

.form-header h1 {
  font-size: 2.2rem;
  margin: 0 0 10px 0;
  color: var(--text-header);
}

.subtitle {
  color: var(--text-muted);
  font-size: 1.1rem;
}

.novel-form-grid {
  display: grid;
  grid-template-columns: 1fr 340px; /* Основной контент шире */
  gap: 50px;
}

/* Левая колонка */
.large-input {
  font-size: 1.5rem !important;
  font-weight: 600;
  padding: 18px !important;
}

textarea {
  font-size: 1.05rem;
  line-height: 1.6;
  resize: vertical;
}

/* Правая колонка (Sidebar) */
.form-sidebar-right {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.cover-upload-zone {
  position: relative;
  width: 100%;
  aspect-ratio: 2/3;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  border: 2px dashed var(--border-color);
  transition: all 0.3s ease;
}

.cover-upload-zone:hover {
  border-color: #42b883;
}

.upload-preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  color: white;
  font-weight: 600;
}

.cover-upload-zone:hover .upload-overlay {
  opacity: 1;
}

/* Стили кнопок выбора (Жанры/Теги) */
.tags-selector, .status-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-option, .status-btn {
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 0.85rem;
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s ease;
}

.tag-option:hover, .status-btn:hover {
  border-color: #42b883;
  color: var(--text-header);
}

.tag-option.active {
  background: #34495e;
  color: white;
  border-color: #34495e;
}

.tag-option.genre.active {
  background: #42b883;
  border-color: #42b883;
}

.status-btn.active {
  background: var(--text-header);
  color: var(--bg-main);
  border-color: var(--text-header);
}

/* Кнопки действий */
.form-full-width-actions {
  grid-column: span 2;
  display: flex;
  justify-content: flex-end;
  gap: 20px;
  margin-top: 30px;
  padding-top: 30px;
  border-top: 1px solid var(--border-subtle);
}

.btn-primary {
  background: #42b883;
  color: white;
  padding: 16px 40px;
  border-radius: 12px;
  font-size: 1.1rem;
  font-weight: bold;
  border: none;
  cursor: pointer;
  transition: transform 0.2s, background 0.2s;
}

.btn-primary:hover {
  background: #3aa373;
  transform: translateY(-2px);
}

.btn-secondary {
  padding: 16px 30px;
  background: transparent;
  color: var(--text-muted);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  cursor: pointer;
}

@media (max-width: 1000px) {
  .novel-form-grid {
    grid-template-columns: 1fr;
  }
  .form-full-width-actions {
    grid-column: span 1;
  }
  .form-sidebar-right {
    order: -1; /* Обложка и настройки сверху на мобилках */
  }
}
.btn-remove-link {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  margin-top: 12px;
  padding: 8px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: #ff4757; /* Цвет ошибки/удаления */
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-remove-link:hover {
  background: rgba(255, 71, 87, 0.1);
  border-color: #ff4757;
}

.btn-remove-link:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-remove-link .icon {
  font-size: 1.1rem;
}
</style>
