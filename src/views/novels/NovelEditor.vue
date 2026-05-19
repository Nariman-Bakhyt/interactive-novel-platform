<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import type {
  ChapterShortResponseDto,
  ChapterStatus,
  NovelRequestDto,
  TagOrGenreResponseDto
} from "@/types/novel.ts";
import {useToastStore} from "@/components/toast/toastStore.ts";
import {DEFAULT_COVER} from "@/utils/media.ts";
import {
  createNovel,
  deleteChapter,
  deleteNovel,
  getAllGenres,
  getAllTags, getMyNovel, updateChapterPublishTime, updateNovel, uploadNovelCover
} from "@/api/novelService.ts";

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

const defaultCover = DEFAULT_COVER;
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
    allGenres.value = JSON.parse(localStorage.getItem('genres') || '[]');
    allTags.value = JSON.parse(localStorage.getItem('tags') || '[]');

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

const handleDeleteNovel = async () => {
  if (!confirm('Вы точно хотите удалить эту новеллу? Это действие нельзя отменить!')) return;

  isSaving.value = true;
  try {
    await deleteNovel(Number(novelId.value));
    toastStore.success('Новелла успешно удалена');
    router.push('/novels/my');
  } catch (e) {
    console.error(e);
    toastStore.error('Ошибка при удалении новеллы');
  } finally {
    isSaving.value = false;
  }
};

const handleDeleteChapter = async (chapterId: number, event: Event) => {
  event.stopPropagation(); // Чтобы не сработал переход на редактирование
  if (!confirm('Вы точно хотите удалить эту главу?')) return;

  try {
    await deleteChapter(Number(novelId.value), chapterId);
    toastStore.success('Глава удалена');
    chaptersList.value = chaptersList.value.filter(c => c.id !== chapterId);
  } catch (e) {
    console.error(e);
    toastStore.error('Ошибка при удалении главы');
  }
};

const activePublishMenuChapterId = ref<number | null>(null);
const showDatePickerForChapterId = ref<number | null>(null);
const chapterPublishDates = ref<Record<number, string>>({});
const isChapterPublishing = ref(false);

const toggleChapterPublishMenu = (chapterId: number) => {
  if (activePublishMenuChapterId.value === chapterId) {
    activePublishMenuChapterId.value = null;
    showDatePickerForChapterId.value = null;
  } else {
    activePublishMenuChapterId.value = chapterId;
    showDatePickerForChapterId.value = null;
    const chapter = chaptersList.value.find(c => c.id === chapterId);
    if (chapter && chapter.publishedAt) {
      chapterPublishDates.value[chapterId] = chapter.publishedAt.slice(0, 16);
    } else {
      chapterPublishDates.value[chapterId] = '';
    }
  }
};

const publishChapterInline = async (chapterId: number, action: 'NOW' | 'DRAFT' | 'SCHEDULE') => {
  if (action === 'SCHEDULE') {
    showDatePickerForChapterId.value = chapterId;
    return;
  }

  isChapterPublishing.value = true;
  let publishTime: string | null = null;
  if (action === 'NOW') {
    publishTime = new Date().toISOString();
  } else if (action === 'DRAFT') {
    publishTime = null;
  }

  try {
    const updatedChapter = await updateChapterPublishTime(Number(novelId.value), chapterId, publishTime);
    const chapter = chaptersList.value.find(c => c.id === chapterId);

    if (chapter) {
      chapter.status = updatedChapter.status;
      chapter.publishedAt = updatedChapter.publishedAt;
    }
    toastStore.success(action === 'DRAFT' ? 'Глава снята с публикации' : 'Глава опубликована');
    activePublishMenuChapterId.value = null;
    showDatePickerForChapterId.value = null;
  } catch (e) {
    console.error(e);
    toastStore.error('Ошибка изменения статуса публикации');
  } finally {
    isChapterPublishing.value = false;
  }
};

const confirmPublishChapterInline = async (chapterId: number) => {
  const dateStr = chapterPublishDates.value[chapterId];
  if (!dateStr) {
    toastStore.error('Выберите дату и время!');
    return;
  }

  isChapterPublishing.value = true;
  const publishTime = new Date(dateStr).toISOString();

  try {
    const updatedChapter = await updateChapterPublishTime(Number(novelId.value), chapterId, publishTime);
    const chapter = chaptersList.value.find(c => c.id === chapterId);

    if (chapter) {
      chapter.status = updatedChapter.status;
      chapter.publishedAt = updatedChapter.publishedAt;
    }
    toastStore.success('Глава успешно запланирована');
    activePublishMenuChapterId.value = null;
    showDatePickerForChapterId.value = null;
  } catch (e) {
    console.error(e);
    toastStore.error('Ошибка планирования публикации');
  } finally {
    isChapterPublishing.value = false;
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

                  <div class="ch-publish-inline" @click.stop>
                    <span class="ch-status-badge pointer" :class="chapter.status?.toLowerCase()" @click="toggleChapterPublishMenu(chapter.id)">
                      {{ chapter.status }} ⚙️
                    </span>

                    <div v-if="activePublishMenuChapterId === chapter.id" class="mini-publish-menu">
                      <button class="btn-mini-pub now" @click="publishChapterInline(chapter.id, 'NOW')" :disabled="isChapterPublishing">Опубликовать сейчас</button>
                      <button class="btn-mini-pub schedule" @click="publishChapterInline(chapter.id, 'SCHEDULE')" :disabled="isChapterPublishing">Запланировать</button>
                      <button v-if="chapter.status !== 'DRAFT'" class="btn-mini-pub draft" @click="publishChapterInline(chapter.id, 'DRAFT')" :disabled="isChapterPublishing">Снять</button>

                      <div v-if="showDatePickerForChapterId === chapter.id" class="mini-datepicker-wrapper">
                        <input type="datetime-local" v-model="chapterPublishDates[chapter.id]" class="mini-date-input" />
                        <button class="btn-confirm-mini" @click="confirmPublishChapterInline(chapter.id)" :disabled="isChapterPublishing">OK</button>
                      </div>
                    </div>
                  </div>

                  <button class="btn-delete-chapter" @click="handleDeleteChapter(chapter.id, $event)" title="Удалить главу">
                    🗑️
                  </button>
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
                  <label>Статус</label>
                  <div class="status-selector">
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
                <button v-if="isEditMode" type="button" @click="handleDeleteNovel" class="btn-delete-novel">🗑️ Удалить новеллу</button>
                <div class="right-actions">
                  <button v-if="isEditMode" type="button" @click="isEditingNow = false" class="btn-secondary">Отмена</button>
                  <button type="submit" class="btn-primary" :disabled="isSaving">
                    {{ isSaving ? 'Сохранение...' : (isEditMode ? 'Сохранить изменения' : 'Опубликовать новеллу') }}
                  </button>
                </div>
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
  padding: 100px 24px 60px;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

.novel-view-wrapper {
  display: flex;
  justify-content: center;
  width: 100%;
}

.novel-view-card {
  background: var(--bg-dropdown);
  padding: 48px;
  border-radius: 24px;
  width: 100%;
  max-width: 800px;
  display: flex;
  flex-direction: column;
  align-items: center; /* Центрируем всё содержимое */
  text-align: center;
  box-shadow: 0 4px 12px var(--shadow-color);
  border: 1px solid var(--border-color);
  transition: all 0.3s ease;
}

.status-badge {
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 6px 16px;
  border-radius: 8px;
  font-weight: 700;
  display: inline-block;
  margin-bottom: 24px;
}
.status-badge.completed { background: rgba(16, 185, 129, 0.1); color: #10b981; border: 1px solid rgba(16, 185, 129, 0.2);}
.status-badge.in_progress { background: rgba(59, 130, 246, 0.1); color: #3b82f6; border: 1px solid rgba(59, 130, 246, 0.2);}
.status-badge.draft { background: rgba(161, 161, 170, 0.1); color: #a1a1aa; border: 1px solid rgba(161, 161, 170, 0.2);}

.novel-main-title {
  margin: 0 0 24px;
  font-size: 2.5rem;
  font-weight: 800;
  color: var(--text-header);
  letter-spacing: -0.02em;
}

.card-cover-preview {
  width: 260px;
  height: 390px;
  object-fit: cover;
  border-radius: 12px;
  margin: 0 0 32px;
  border: 1px solid var(--border-color);
  box-shadow: 0 12px 32px var(--shadow-color);
  transition: transform 0.3s ease;
}
.card-cover-preview:hover {
  transform: scale(1.02);
}

.description-text {
  color: var(--text-muted);
  line-height: 1.6;
  white-space: pre-wrap;
  text-align: left; /* Аннотацию лучше читать слева */
  word-break: break-word;
  overflow-wrap: anywhere;
  max-width: 100%;
}

.novel-editor-layout {
  display: block;
  width: 100%;
}

.novel-form-main {
  background:  var(--bg-dropdown);
  padding: 48px;
  border-radius: 24px;
  border: 1px solid var(--border-color);
  box-shadow: 0 4px 12px var(--shadow-color);
  transition: all 0.3s ease;
}

.form-header {
  margin-bottom: 40px;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 24px;
}

.form-header h1 {
  font-size: 2.25rem;
  font-weight: 800;
  margin: 0 0 12px 0;
  color: var(--text-header);
  letter-spacing: -0.02em;
}

.subtitle {
  color: var(--text-muted);
  font-size: 1.1rem;
  margin: 0;
}

.novel-form-grid {
  display: grid;
  grid-template-columns: 1fr 340px; /* Основной контент шире */
  gap: 48px;
}

.form-group {
  margin-bottom: 32px;
  text-align: left;
}

.form-group label {
  display: block;
  margin-bottom: 12px;
  color: var(--text-header);
  font-weight: 600;
  font-size: 1.05rem;
}

.form-group input, .form-group textarea {
  width: 100%;
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  color: var(--text-header);
  padding: 16px;
  border-radius: 12px;
  box-sizing: border-box;
  font-family: inherit;
  font-size: 1rem;
  transition: all 0.2s ease;
}
.form-group input:focus, .form-group textarea:focus {
  outline: none;
  border-color: var(--btn-plus);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}
.form-group input::placeholder, .form-group textarea::placeholder {
  color: var(--input-placeholder);
}

.large-input {
  font-size: 1.25rem !important;
  font-weight: 600;
  padding: 18px !important;
}

textarea {
  line-height: 1.6;
  resize: vertical;
}

/* Правая колонка (Sidebar) */
.form-sidebar-right {
  display: flex;
  flex-direction: column;
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
  background: var(--bg-main);
}

.cover-upload-zone:hover {
  border-color: var(--btn-plus);
}

.upload-preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}
.cover-upload-zone:hover .upload-preview-img {
  transform: scale(1.05);
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
  backdrop-filter: blur(2px);
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
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 500;
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s ease;
}

.tag-option:hover, .status-btn:hover {
  border-color: var(--btn-plus);
  color: var(--text-header);
  background: var(--hover-dropdowb);
}

.tag-option.active {
  background: rgba(99, 102, 241, 0.1);
  color: var(--btn-plus);
  border-color: var(--btn-plus);
}

.tag-option.genre.active {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
  border-color: #10b981;
}

.status-btn.active {
  background: var(--btn-plus);
  color: white;
  border-color: var(--btn-plus);
}

/* Кнопки действий */
.form-full-width-actions {
  grid-column: span 2;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 24px;
  padding-top: 32px;
  border-top: 1px solid var(--border-color);
}

.right-actions {
  display: flex;
  gap: 16px;
}

.btn-delete-novel {
  background: transparent;
  color: #ef4444; /* red-500 */
  border: 1px solid #ef4444;
  padding: 16px 24px;
  border-radius: 12px;
  font-size: 1.05rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-delete-novel:hover {
  background: rgba(239, 68, 68, 0.1);
}

.btn-primary {
  background: var(--btn-plus);
  color: white;
  padding: 16px 32px;
  border-radius: 12px;
  font-size: 1.05rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary:hover:not(:disabled) {
  background: var(--btn-plus-hover);
  transform: translateY(-2px);
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  padding: 16px 32px;
  background: transparent;
  color: var(--text-header);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  cursor: pointer;
  font-size: 1.05rem;
  font-weight: 600;
  transition: all 0.2s;
}
.btn-secondary:hover {
  background: var(--hover-dropdowb);
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
  margin-top: 16px;
  padding: 12px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: #ef4444; /* red-500 */
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-remove-link:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.1);
  border-color: #ef4444;
}

.btn-remove-link:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.novel-metadata-chips {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 32px;
  width: 100%;
}
.chip-group {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}
.chip {
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 500;
  border: 1px solid var(--border-color);
  background: var(--bg-main);
  color: var(--text-muted);
}
.genre-chip {
  background: rgba(16, 185, 129, 0.1);
  border-color: rgba(16, 185, 129, 0.2);
  color: #10b981;
}
.tag-chip {
  background: rgba(99, 102, 241, 0.1);
  border-color: rgba(99, 102, 241, 0.2);
  color: #6366f1;
}

.novel-content {
  width: 100%;
  text-align: left;
}

.full-description {
  color: var(--text-muted);
  line-height: 1.6;
  white-space: pre-wrap;
  text-align: left;
  width: 100%;
  margin-bottom: 32px;
  font-size: 1rem;
}
.section-title {
  margin: 0 0 16px;
  color: var(--text-header);
  font-size: 1.5rem;
  font-weight: 700;
  text-align: left;
}
.divider {
  width: 100%;
  border: 0;
  border-top: 1px solid var(--border-color);
  margin: 32px 0;
}
.table-of-contents {
  width: 100%;
  text-align: left;
}

.toc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chapter-count {
  font-size: 0.95rem;
  color: var(--text-muted);
  font-weight: 500;
}

.chapters-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chapter-item {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  background: var(--bg-main);
  border-radius: 12px;
  border: 1px solid var(--border-color);
  transition: all 0.2s ease;
  cursor: pointer;
}

.ch-title {
  flex-grow: 1;
  font-weight: 600;
  color: var(--text-header);
}

.ch-status-badge {
  font-size: 0.75rem;
  padding: 4px 10px;
  border-radius: 6px;
  font-weight: 700;
  text-transform: uppercase;
  margin-right: 16px;
}
.ch-status-badge.published { background: rgba(16, 185, 129, 0.1); color: #10b981; border: 1px solid rgba(16, 185, 129, 0.2); }
.ch-status-badge.scheduled { background: rgba(59, 130, 246, 0.1); color: #3b82f6; border: 1px solid rgba(59, 130, 246, 0.2); }
.ch-status-badge.draft { background: rgba(161, 161, 170, 0.1); color: #a1a1aa; border: 1px solid rgba(161, 161, 170, 0.2); }

.btn-delete-chapter {
  background: none;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  opacity: 0.5;
  transition: opacity 0.2s, transform 0.2s;
  margin-right: 12px;
}
.btn-delete-chapter:hover {
  opacity: 1;
  transform: scale(1.1);
  user-select: none;
}

.chapter-item:hover {
  background: var(--hover-dropdowb);
  border-color: var(--text-muted);
  transform: translateX(4px);
}

.ch-number {
  font-weight: 600;
  color: var(--btn-plus);
  margin-right: 16px;
  min-width: 80px;
}

.ch-title {
  flex-grow: 1;
  color: var(--text-header);
  font-weight: 500;
}


.ch-icon {
  opacity: 0.5;
  transition: opacity 0.2s, transform 0.2s;
}
.chapter-item:hover .ch-icon {
  opacity: 1;
  transform: scale(1.1);
}

.view-footer {
  display: flex;
  gap: 16px;
  margin-top: 48px;
  width: 100%;
}

.btn-edit-main {
  flex: 1;
  padding: 16px;
  background: var(--bg-main);
  color: var(--text-header);
  border-radius: 12px;
  border: 1px solid var(--border-color);
  cursor: pointer;
  font-weight: 600;
  font-size: 1.05rem;
  transition: all 0.2s ease;
}
.btn-edit-main:hover {
  background: var(--hover-dropdowb);
  border-color: var(--text-muted);
}

.btn-add-chapter {
  flex: 1;
  padding: 16px;
  background: var(--btn-plus);
  color: white;
  border-radius: 12px;
  border: none;
  cursor: pointer;
  font-weight: 600;
  font-size: 1.05rem;
  transition: background 0.2s, transform 0.2s;
}
.btn-add-chapter:hover {
  background: var(--btn-plus-hover);
  transform: translateY(-2px);
}

/* Стили для инлайн публикации */
.ch-publish-inline {
  position: relative;
  margin-right: 16px;
  display: flex;
  align-items: center;
}

.ch-status-badge.pointer {
  cursor: pointer;
  transition: transform 0.2s, background-color 0.2s;
}
.ch-status-badge.pointer:hover {
  transform: scale(1.05);
}

.mini-publish-menu {
  position: absolute;
  bottom: 100%;
  right: 0;
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: 0 8px 24px var(--shadow-color);
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  z-index: 100;
  margin-bottom: 8px;
  min-width: 180px;
}

.btn-mini-pub {
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  text-align: left;
  transition: background-color 0.2s;
}
.btn-mini-pub.now {
  background: #10b981;
  color: white;
}
.btn-mini-pub.now:hover { background: #059669; }

.btn-mini-pub.schedule {
  background: #3b82f6;
  color: white;
}
.btn-mini-pub.schedule:hover { background: #2563eb; }

.btn-mini-pub.draft {
  background: transparent;
  color: var(--text-muted);
  border: 1px solid var(--border-color);
}
.btn-mini-pub.draft:hover { background: var(--hover-dropdowb); color: var(--text-header); }

.mini-datepicker-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  border-top: 1px solid var(--border-color);
  padding-top: 6px;
}

.mini-date-input {
  padding: 6px;
  border-radius: 4px;
  border: 1px solid var(--border-color);
  background: var(--bg-main);
  color: var(--text-header);
  font-size: 0.8rem;
}

.btn-confirm-mini {
  background: var(--btn-plus);
  color: white;
  border: none;
  padding: 6px 10px;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
}
.btn-confirm-mini:hover {
  background: var(--btn-plus-hover);
}
</style>
