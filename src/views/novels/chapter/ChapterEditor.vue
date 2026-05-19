<script setup lang="ts">
import {computed, nextTick, onMounted, onUnmounted, ref} from 'vue';
import {useRouter} from 'vue-router';
import {
  createChapter,
  getChapter,
  updateChapter,
  updateChapterPublishTime
} from '@/api/novelService';
import type {ChapterRequestDto} from '@/types/novel';
import {useCommentStore} from "@/components/chat/commentStore.ts";


const props = defineProps<{ novelId: string | number; chapterId?: string | number; }>();
const router = useRouter();
const chatStore = useCommentStore(); 


const form = ref<ChapterRequestDto>({ title: '', blocks: [] });
const activeBlockIndex = ref<number | null>(null);
const showMenuIndex = ref<number | null>(null);


const toggleComments = (blockId: number | null) => {
  if (!blockId) return;
  
  chatStore.openChat(blockId, 'BLOCK');
  window.dispatchEvent(new CustomEvent('open-messenger'));
};


// Vue Router передает параметры маршрута в строковом формате. Кастим к Number для соответствия типам данных API бэкенда.
const nId = computed(() => Number(props.novelId));
const cId = computed(() => props.chapterId ? Number(props.chapterId) : null);
const isEditMode = computed(() => !!cId.value);
const isLoading = ref(false);
const isSaving = ref(false);
const draggedItemIndex = ref<number | null>(null);


const showDatePicker = ref(false);
const selectedPublishDate = ref('');
const currentStatus = ref('DRAFT');
const isPublishing = ref(false);






onMounted(async () => {
  if (isEditMode.value) {
    isLoading.value = true;
    try {
      const data = await getChapter(nId.value, cId.value!);
      form.value.title = data.title;
      currentStatus.value = data.status || 'DRAFT';
      if (data.publishedAt) {
        selectedPublishDate.value = data.publishedAt.slice(0, 16); 
      }
      form.value.blocks = (data.blocks || []).sort((a, b) => a.sequenceOrder - b.sequenceOrder);

      
      // Vue обновляет DOM асинхронно. Используем nextTick, чтобы дождаться перерисовки элементов textarea и корректно рассчитать их высоту на основе scrollHeight.
      await nextTick();
      document.querySelectorAll('textarea.block-textarea').forEach(el => {
        adjustHeight({ target: el });
      });
    } catch (e) {
      console.error("Ошибка загрузки:", e);
    } finally {
      isLoading.value = false;
    }
  } else {
    
    form.value.blocks.push({ id: null, type: 'TEXT', content: '', sequenceOrder: 1 });
  }
});

const closeMenuWithDelay = () => {
  setTimeout(() => {
    showMenuIndex.value = null;
  }, 100);
};

onMounted(() => {
  window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') showMenuIndex.value = null;
  });
});

const toggleMenu = (index: number) => {
  if (showMenuIndex.value === index) {
    showMenuIndex.value = null;
  } else {
    showMenuIndex.value = index;
  }
};

const adjustHeight = (event: any) => {
  const el = event.target;
  el.style.height = 'auto';
  el.style.height = el.scrollHeight + 'px';
};

const reorderBlocks = () => {
  form.value.blocks.forEach((b, i) => b.sequenceOrder = i + 1);
};

const handleEnter = (index: number, event: KeyboardEvent) => {
  if (event.shiftKey) return; 

  event.preventDefault();
  const newBlock = {
    id: null,
    type: 'TEXT',
    content: '',
    sequenceOrder: index + 2
  };

  form.value.blocks.splice(index + 1, 0, newBlock);
  reorderBlocks();

  // Vue обновит DOM только в конце тика. Вызываем focus() внутри nextTick, так как новый textarea еще физически не отрендерен на странице.
  nextTick(() => {
    const allTextareas = document.querySelectorAll('.block-textarea');
    (allTextareas[index + 1] as HTMLElement)?.focus();
  });
};

const handleBackspace = (index: number, event: KeyboardEvent) => {
  const block = form.value.blocks[index];
  if (block && block.content === '' && form.value.blocks.length > 1) {
    event.preventDefault();
    form.value.blocks.splice(index, 1);
    reorderBlocks();

    // Аналогично, переводим фокус на предыдущий элемент в nextTick и перемещаем курсор ввода в конец существующего текста.
    nextTick(() => {
      const allTextareas = document.querySelectorAll('.block-textarea');
      const prevEl = allTextareas[index - 1] as HTMLElement;
      if (prevEl) {
        prevEl.focus();
        
        if (prevEl instanceof HTMLTextAreaElement) {
          prevEl.setSelectionRange(prevEl.value.length, prevEl.value.length);
        }
      }
    });
  }
};

const changeBlockType = (index: number, type: 'TEXT' | 'IMAGE') => {
  const block = form.value.blocks[index];
  if (block) {
    block.type = type;
  }
  showMenuIndex.value = null;
};



// Используем нативный HTML5 Drag and Drop API для переупорядочивания реактивного массива блоков во избежание подключения тяжелых внешних библиотек.
const onDragStart = (index: number) => {
  draggedItemIndex.value = index;
};

const onDrop = (toIndex: number) => {
  if (draggedItemIndex.value === null || draggedItemIndex.value === toIndex) return;

  const movedItem = form.value.blocks.splice(draggedItemIndex.value, 1)[0]!;

  form.value.blocks.splice(toIndex, 0, movedItem);
  draggedItemIndex.value = null;
  reorderBlocks();
};

const handleSave = async () => {
  isSaving.value = true;
  reorderBlocks();
  try {
    if (isEditMode.value) {
      await updateChapter(nId.value, cId.value!, form.value);
      alert("Изменения сохранены!");
    } else {
      await createChapter(nId.value, form.value);
      router.push(`/novels/${nId.value}/edit`);
    }

  } catch {
    alert("Ошибка сохранения");
  } finally {
    isSaving.value = false;
  }
};

const handlePublishAction = async (action: 'NOW' | 'DRAFT' | 'SCHEDULE') => {
  if (!isEditMode.value) {
    alert('Сначала сохраните главу!');
    return;
  }

  if (action === 'SCHEDULE') {
    if (!showDatePicker.value) {
      showDatePicker.value = true;
      return;
    }
    if (!selectedPublishDate.value) {
      alert('Выберите дату и время!');
      return;
    }
  }

  isPublishing.value = true;
  let publishTime: string | null = null;

  if (action === 'NOW') {
    publishTime = new Date().toISOString();
  } else if (action === 'SCHEDULE') {
    publishTime = new Date(selectedPublishDate.value).toISOString();
  } else if (action === 'DRAFT') {
    publishTime = null;
  }

  try {
    const updatedChapter = await updateChapterPublishTime(nId.value, cId.value!, publishTime);
    currentStatus.value = updatedChapter.status;
    showDatePicker.value = false;
    alert(action === 'DRAFT' ? 'Глава снята с публикации' : 'Настройки публикации сохранены!');
  } catch (e) {
    console.error(e);
    alert('Ошибка обновления статуса публикации');
  } finally {
    isPublishing.value = false;
  }
};

const isToolbarVisible = ref(true);
let lastScrollTop = 0;
const scrollThreshold = 8; 

// Алгоритм скрытия/отображения плавающего тулбара при скролле.
// scrollThreshold защищает от дребезга (jitter) при микроколебаниях тачпада или мыши.
// Проверка на TEXTAREA предотвращает скрытие тулбара при вертикальном скролле длинного текста внутри текстового блока.
const handleScroll = (event: Event) => {
  
  const target = event.target as HTMLElement;

  
  if (target && target.tagName === 'TEXTAREA') return;

  
  const currentScroll = window.pageYOffset
    || document.documentElement.scrollTop
    || document.body.scrollTop
    || (target ? target.scrollTop : 0);

  
  if (currentScroll <= 30) {
    isToolbarVisible.value = true;
    return;
  }

  
  if (Math.abs(currentScroll - lastScrollTop) <= scrollThreshold) {
    return;
  }

  if (currentScroll > lastScrollTop) {
    
    isToolbarVisible.value = false;
  } else {
    
    isToolbarVisible.value = true;
  }

  lastScrollTop = currentScroll;
};

onMounted(() => {
  
  window.addEventListener('scroll', handleScroll, true);
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll, true);
});

</script>

<template>
  <div class="editor-page-wrapper">
    <div class="notion-style-container">
      <header class="editor-toolbar"
              :class="{ 'toolbar-hidden': !isToolbarVisible }"
      >
        <div class="toolbar-left">
          <button @click="router.back()" class="btn-minimal">
            <span class="icon">←</span> Назад
          </button>
        </div>

        <div class="toolbar-right">
          <div class="publish-controls" v-if="isEditMode">
            <span class="status-badge" :class="currentStatus.toLowerCase()">{{ currentStatus }}</span>

            <div class="publish-actions">
              <button class="btn-publish" @click="handlePublishAction('NOW')" :disabled="isPublishing">Опубликовать</button>

              <div class="schedule-wrapper">
                <button class="btn-schedule" @click="handlePublishAction('SCHEDULE')" :disabled="isPublishing">
                  {{ showDatePicker ? 'Подтвердить' : 'Запланировать' }}
                </button>
                <input
                  v-if="showDatePicker"
                  type="datetime-local"
                  v-model="selectedPublishDate"
                  class="date-picker-input"
                />
              </div>

              <button v-if="currentStatus !== 'DRAFT'" class="btn-draft" @click="handlePublishAction('DRAFT')" :disabled="isPublishing">Снять</button>
            </div>
          </div>

          <button @click="handleSave" :disabled="isSaving" class="btn-save-notion">
            {{ isSaving ? 'Сохранение...' : 'Сохранить' }}
          </button>
        </div>
      </header>

      <input
        v-model="form.title"
        class="main-title-input"
        placeholder="Заголовок главы"
      />

      <div class="canvas">
        <div
          v-for="(block, index) in form.blocks"
          :key="index"
          class="block-row"
          :class="{ 'is-dragging': draggedItemIndex === index }"
          @mouseenter="activeBlockIndex = index"
          @mouseleave="activeBlockIndex = null"
          @dragover.prevent
          @drop="onDrop(index)"
        >
          <div class="side-control left">
            <button
              v-if="activeBlockIndex === index || showMenuIndex === index"
              class="plus-button"
              @click.stop="toggleMenu(index)"
              @blur="closeMenuWithDelay"
            >+</button>

            <Transition name="pop">
              <div
                v-if="showMenuIndex === index"
                class="type-selector-menu"
                @mousedown.prevent
              >
                <button @click="changeBlockType(index, 'TEXT')">📝 Текст</button>
                <button @click="changeBlockType(index, 'IMAGE')">🖼 Изображение</button>
              </div>
            </Transition>
          </div>

          <div class="block-main-content">
            <textarea
              v-if="block.type === 'TEXT'"
              v-model="block.content"
              class="block-textarea"
              placeholder="Введите текст или нажмите Enter..."
              rows="1"
              @input="adjustHeight"
              @keydown.enter="handleEnter(index, $event)"
              @keydown.backspace="handleBackspace(index, $event)"
            ></textarea>

            <div v-else-if="block.type === 'IMAGE'" class="image-block-wrapper">
              <input v-model="block.content" class="image-url-input" placeholder="Вставьте прямую ссылку на фото..." />
              <div v-if="block.content" class="image-preview">
                <img :src="block.content" alt="Preview" />
              </div>
            </div>
          </div>

          <div class="side-control right">
            <div class="block-controls-right" v-if="activeBlockIndex === index || chatStore.activeTargetId === block.id">

              
              <button
                v-if="block.id"
                class="comment-trigger-small"
                :class="{ 'is-active': chatStore.activeTargetId === block.id }"
                @click.stop="toggleComments(block.id)"
                title="Обсудить блок"
              >
                💬
              </button>

              <div
                class="drag-handle-notion"
                draggable="true"
                @dragstart="onDragStart(index)"
                title="Перетащить"
              >
                ☰
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.editor-page-wrapper {
  min-height: 100vh;
  background-color: var(--bg-editor-page); 
  padding: 80px 24px 100px;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: background-color 0.3s ease;
}

.notion-style-container {
  width: 100%;
  max-width: 860px;
  background-color: var(--bg-editor-sheet);
  
  padding: 48px 64px 64px;
  border-radius: 24px;
  box-shadow: 0 4px 12px var(--shadow-color);
  border: 1px solid var(--border-color);
  min-height: 80vh;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.3s;
}

.editor-toolbar {
  position: sticky;
  top: 0;
  
  background: var(--bg-editor-sheet);
  z-index: 100;

  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;

  
  height: 56px;
  padding: 16px 0;
  margin-bottom: 32px;
  border-bottom: 1px solid var(--border-color);

  
  transition:
    height 0.25s cubic-bezier(0.4, 0, 0.2, 1),
    padding 0.25s cubic-bezier(0.4, 0, 0.2, 1),
    margin-bottom 0.25s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.2s ease,
    border-color 0.2s ease;

  overflow: hidden; 
  opacity: 1;
}


.editor-toolbar.toolbar-hidden {
  height: 0;                 
  padding-top: 0;            
  padding-bottom: 0;
  margin-bottom: 0;          
  opacity: 0;                
  border-color: transparent; 
  pointer-events: none;      
  transform: none;           
}

.toolbar-left, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.publish-controls {
  display: flex;
  flex-direction: row; 
  align-items: center;
  gap: 16px;
  background: var(--bg-main);
  padding: 8px 16px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
}

.publish-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.schedule-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-picker-input {
  padding: 8px;
  border-radius: 6px;
  border: 1px solid var(--border-color);
  background: var(--bg-dropdown);
  color: var(--text-header);
}

.btn-publish {
  background: #10b981;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-publish:hover { background: #059669; transform: translateY(-1px); }

.btn-schedule {
  background: #3b82f6;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-schedule:hover { background: #2563eb; transform: translateY(-1px); }

.btn-draft {
  background: transparent;
  color: var(--text-muted);
  border: 1px solid var(--border-color);
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-draft:hover { background: var(--hover-dropdowb); color: var(--text-header); }

.status-badge {
  font-size: 0.8rem;
  padding: 4px 12px;
  border-radius: 6px;
  font-weight: 700;
  text-transform: uppercase;
}
.status-badge.published { background: rgba(16, 185, 129, 0.1); color: #10b981; }
.status-badge.scheduled { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.status-badge.draft { background: rgba(161, 161, 170, 0.1); color: #a1a1aa; }

.main-title-input {
  width: 100%;
  font-size: 2.5rem;
  font-weight: 800;
  background: none;
  border: none;
  color: var(--text-header);
  outline: none;
  margin-bottom: 32px;
  text-align: left; 
  letter-spacing: -0.02em;
}
.main-title-input::placeholder {
  color: var(--input-placeholder);
}

.canvas {
  display: flex;
  flex-direction: column;
}

.block-row {
  display: flex;
  align-items: flex-start;
  padding: 8px 0;
  position: relative;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.block-row:hover {
  background-color: rgba(161, 161, 170, 0.05); 
}

.block-row.is-dragging {
  opacity: 0.3;
}

.side-control {
  width: 48px;
  min-height: 32px;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}

.plus-button {
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 1.5rem;
  cursor: pointer;
  transition: color 0.2s, transform 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 4px;
}

.plus-button:hover {
  color: var(--btn-plus);
  background: var(--hover-dropdowb);
  transform: scale(1.1);
}

.drag-handle-notion {
  font-size: 1.25rem;
  user-select: none;
  padding: 4px 8px;
  cursor: grab;
  color: var(--text-muted);
  opacity: 0.5;
  transition: opacity 0.2s, color 0.2s;
  border-radius: 4px;
}
.drag-handle-notion:hover {
  opacity: 1;
  color: var(--text-header);
  background: var(--hover-dropdowb);
}

.drag-handle-notion:active { cursor: grabbing; }

.block-main-content {
  flex: 1;
  padding: 0 8px;
}

.block-textarea {
  width: 100%;
  background: none;
  border: none;
  color: var(--text-header);
  font-size: 1.25rem; 
  line-height: 1.8;   
  resize: none;
  outline: none;
  padding: 8px 0;
  overflow: hidden;
  font-family: inherit;
  -webkit-font-smoothing: antialiased;
}

.block-textarea::placeholder {
  color: var(--input-placeholder);
}

.image-block-wrapper {
  padding: 12px 0;
}

.image-url-input {
  padding: 12px 16px;
  font-size: 0.95rem;
  margin-bottom: 16px;
  width: 100%;
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  color: var(--text-header);
  border-radius: 8px;
  transition: border-color 0.2s;
}
.image-url-input:focus {
  outline: none;
  border-color: var(--btn-plus);
}

.image-preview img {
  max-width: 100%;
  border-radius: 12px;
  box-shadow: 0 8px 24px var(--shadow-color);
  border: 1px solid var(--border-color);
}


.type-selector-menu {
  position: absolute;
  top: 100%;
  left: 8px;
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 24px var(--shadow-color);
  overflow: hidden;
  padding: 4px;
}

.type-selector-menu button {
  padding: 8px 16px;
  background: none;
  border: none;
  color: var(--text-header);
  text-align: left;
  cursor: pointer;
  white-space: nowrap;
  font-size: 0.95rem;
  border-radius: 4px;
  transition: background 0.2s;
}

.type-selector-menu button:hover {
  background: var(--hover-dropdowb);
}

.btn-save-notion {
  background: var(--btn-plus);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.95rem;
  cursor: pointer;
  transition: background 0.2s, transform 0.2s;
}
.btn-save-notion:hover:not(:disabled) {
  background: var(--btn-plus-hover);
  transform: translateY(-1px);
}
.btn-save-notion:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-minimal {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.2s ease;
}
.btn-minimal:hover {
  color: var(--text-header);
  background: var(--hover-dropdowb);
}


.side-control.right {
  width: 80px; 
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding-right: 8px;
}
.block-row:hover .side-control.right,
.block-row.active-block .side-control.right {
  opacity: 1;
}

.block-controls-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-trigger-small {
  background: none;
  border: none;
  font-size: 1.15rem;
  cursor: pointer;
  opacity: 0.4;
  transition: all 0.2s ease;
  padding: 4px;
  border-radius: 4px;
}

.comment-trigger-small:hover {
  opacity: 1;
  background: var(--hover-dropdowb);
}
.comment-trigger-small.is-active {
  opacity: 1;
  color: var(--btn-plus);
}

</style>
