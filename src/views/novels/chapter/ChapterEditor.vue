<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  createChapter,
  updateChapter,
  getChapter
} from '@/api/novelService';
import type { ChapterBlockRequestDto, ChapterRequestDto } from '@/types/novel';

const route = useRoute();
const router = useRouter();

const props = defineProps<{
  novelId: string | number;
  chapterId?: string | number;
}>();

const nId = computed(() => Number(props.novelId));
const cId = computed(() => props.chapterId ? Number(props.chapterId) : null);
const isEditMode = computed(() => !!cId.value);

const isLoading = ref(false);
const isSaving = ref(false);
const activeBlockIndex = ref<number | null>(null);
const showMenuIndex = ref<number | null>(null);
const draggedItemIndex = ref<number | null>(null);

const form = ref<ChapterRequestDto>({
  title: '',
  blocks: []
});

onMounted(async () => {
  if (isEditMode.value) {
    isLoading.value = true;
    try {
      const data = await getChapter(nId.value, cId.value!);
      form.value.title = data.title;
      form.value.blocks = (data.blocks || []).sort((a, b) => a.sequenceOrder - b.sequenceOrder);

      // Даем время DOM отрисоваться и подгоняем высоту всех textarea
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
    // Начальный пустой блок для новой главы
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
  if (event.shiftKey) return; // Позволяем перенос строки через Shift+Enter

  event.preventDefault();
  const newBlock = {
    id: null,
    type: 'TEXT',
    content: '',
    sequenceOrder: index + 2
  };

  form.value.blocks.splice(index + 1, 0, newBlock);
  reorderBlocks();

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

    nextTick(() => {
      const allTextareas = document.querySelectorAll('.block-textarea');
      const prevEl = allTextareas[index - 1] as HTMLElement;
      if (prevEl) {
        prevEl.focus();
        // Ставим курсор в конец текста предыдущего блока
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

// --- DRAG AND DROP (Только за handle) ---

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

  } catch (e) {
    alert("Ошибка сохранения");
  } finally {
    isSaving.value = false;
  }
};
</script>

<template>
  <div class="editor-page-wrapper">
    <div class="notion-style-container">
      <header class="editor-toolbar">
        <button @click="router.back()" class="btn-minimal">Назад</button>
        <button @click="handleSave" :disabled="isSaving" class="btn-save-notion">
          {{ isSaving ? 'Сохранение...' : 'Сохранить' }}
        </button>
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
            <div
              v-if="activeBlockIndex === index"
              class="drag-handle-notion"
              draggable="true"
              @dragstart="onDragStart(index)"
            >
              ☰
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
  background-color: var(--bg-editor-page); /* Используем переменную страницы */
  padding: 60px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: background-color 0.3s ease;
}

.notion-style-container {
  width: 100%;
  max-width: 850px;
  background-color: var(--bg-editor-sheet); /* Используем переменную листа */
  padding: 60px 80px;
  border-radius: 16px;
  box-shadow: 0 10px 40px var(--shadow-color);
  border: 1px solid var(--border-subtle);
  min-height: 80vh;
  transition: background-color 0.3s, border-color 0.3s;
}

.editor-toolbar {
  width: 100%;
  max-width: 850px;
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 10px 0;
}

.main-title-input {
  width: 100%;
  font-size: 3.2rem;
  font-weight: 800;
  background: none;
  border: none;
  color: var(--text-header);
  outline: none;
  margin-bottom: 40px;
  border-bottom: 1px solid var(--border-subtle);
  padding-bottom: 20px;
}

.canvas {
  display: flex;
  flex-direction: column;
}

.block-row {
  display: flex;
  align-items: flex-start;
  group: block;
  padding: 4px 0;
  position: relative;
  border-radius: 4px;
}

.block-row:hover {
  background: rgba(255, 255, 255, 0.02);
  border-radius: 4px;
}

.block-row.is-dragging {
  opacity: 0.2;
}

.side-control {
  width: 45px;
  min-height: 30px;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}

.plus-button {
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 1.4rem;
  cursor: pointer;
}

.plus-button:hover {
  color: var(--btn-plus);
}

.drag-handle-notion {
  font-size: 1.2rem;
  user-select: none;
  padding: 5px;
  cursor: grab;
  color: var(--text-muted);
  opacity: 0.4;
}

.drag-handle-notion:active { cursor: grabbing; }

.block-main-content {
  flex: 1;
  padding: 0 5px;
}

.block-textarea {
  width: 100%;
  background: none;
  border: none;
  color: var(--text-header);
  font-size: 1.15rem;
  line-height: 1.6;
  resize: none;
  outline: none;
  padding: 6px 0;
  overflow: hidden;
  font-family: inherit;
}

.block-textarea::placeholder {
  color: var(--text-muted);
  opacity: 0.5;
}

.image-block-wrapper {
  padding: 10px 0;
}

.image-url-input {

  padding: 8px 12px;
  font-size: 0.9rem;
  margin-bottom: 10px;
  width: 100%;
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  color: var(--text-header);
  border-radius: 6px;
}

.image-preview img {
  max-width: 100%;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
}

/* Всплывающее меню */
.type-selector-menu {
  position: absolute;
  top: 100%;
  left: 10px;
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 24px var(--shadow-color);
  overflow: hidden;
}

.type-selector-menu button {
  padding: 10px 20px;
  background: none;
  border: none;
  color: var(--text-header);
  text-align: left;
  cursor: pointer;
  white-space: nowrap;
}

.type-selector-menu button:hover {
  background: var(--hover-dropdowb);
}

.btn-save-notion {
  background: var(--btn-plus);
  color: #fff; /* Текст на кнопке лучше оставить белым для контраста */
  border: none;
  padding: 8px 20px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}
.btn-save-notion:hover {
  opacity: 0.9;
}
.btn-minimal {
  background: none;
  border: none;
  color: #888;
  cursor: pointer;
}

</style>
