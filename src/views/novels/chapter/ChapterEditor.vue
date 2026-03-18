<script setup lang="ts">
import { ref, onMounted, computed, nextTick ,watch} from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  createChapter,
  updateChapter,
  getChapter
} from '@/api/novelService';
import { deleteComment, getComments } from "@/api/commentService.ts";
import { activeSubscriptions, sendMessage, subscribeToTopic, unsubscribeFromTopic } from "@/api/stompService.ts";
import type { ChapterRequestDto } from '@/types/novel';
import type { CommentResponseDto } from "@/types/comment.ts";

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

const activeCommentsBlockId = ref<number | null>(null);
const comments = ref<CommentResponseDto[]>([]);
const newCommentText = ref('');
const commentsListRef = ref<HTMLElement | null>(null);
const isScrolling = ref(false);
let scrollTimeout: number | null = null;

const contextMenu = ref({
  show: false,
  x: 0,
  y: 0,
  targetId: null as number | null
});

const groupedComments = computed(() => {
  const groups: Record<string, CommentResponseDto[]> = {};
  comments.value.forEach(comment => {
    const date = new Date(comment.timestamp).toLocaleDateString();
    if (!groups[date]) groups[date] = [];
    groups[date].push(comment);
  });
  return groups;
});

const isToday = (dateStr: string) => new Date().toLocaleDateString() === dateStr;
const formatTime = (timestamp: string) => new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

const scrollToBottom = async () => {
  await nextTick();
  if (commentsListRef.value) {
    commentsListRef.value.scrollTop = commentsListRef.value.scrollHeight;
  }
};
const unsubscribeFromCurrentBlock = () => {
  if (activeCommentsBlockId.value !== null) {
    const block = form.value.blocks[activeCommentsBlockId.value];
    if (block && block.id) {
      const topic = `/topic/block.${block.id}`;
      unsubscribeFromTopic(topic);
    }
    comments.value = [];
  }
};

const toggleComments = async (index: number, blockId: number | null) => {
  if (!blockId) return;

  if (activeCommentsBlockId.value === index) {
    unsubscribeFromCurrentBlock();
    activeCommentsBlockId.value = null;
    return;
  }

  if (activeCommentsBlockId.value !== null) unsubscribeFromCurrentBlock();

  activeCommentsBlockId.value = index;
  const wsTopic = `/topic/block.${blockId}`;

  if (!activeSubscriptions.has(wsTopic)) {
    subscribeToTopic<any>(wsTopic, (newComment) => {
      if (newComment.deleted) {
        comments.value = comments.value.filter(c => c.id !== newComment.id);
      } else if (!comments.value.some(c => c.id === newComment.id)) {
        comments.value.push(newComment);
        scrollToBottom();
      }
    });
  }

  try {
    const history = await getComments({ blockId });
    comments.value = history.content;
    scrollToBottom();
  } catch (error) {
    console.error("Ошибка загрузки истории:", error);
  }
};

const submitComment = () => {
  if (!newCommentText.value.trim() || activeCommentsBlockId.value === null) return;
  const currentBlock = form.value.blocks[activeCommentsBlockId.value];
  if (!currentBlock?.id) return;

  sendMessage('/app/comment.send', {
    blockId: currentBlock.id,
    content: newCommentText.value
  });
  newCommentText.value = '';
};

const initStickyObserver = () => {
  const container = commentsListRef.value;
  if (!container) return;
  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      const header = entry.target.nextElementSibling;
      const badge = header?.querySelector('.date-badge');
      if (badge) badge.toggleAttribute('data-in-text', entry.isIntersecting);
    });
  }, { root: container, threshold: [0, 1] });
  container.querySelectorAll('.sticky-sentinel').forEach((el) => observer.observe(el));
};

const handleScroll = () => {
  isScrolling.value = true;
  initStickyObserver();
  if (scrollTimeout) clearTimeout(scrollTimeout);
  scrollTimeout = window.setTimeout(() => { isScrolling.value = false; }, 1500);
};

// --- УДАЛЕНИЕ ---
const openContextMenu = (e: MouseEvent, id: number) => {
  e.preventDefault();
  contextMenu.value = { show: true, x: e.clientX, y: e.clientY, targetId: id };
  const close = () => { contextMenu.value.show = false; document.removeEventListener('click', close); };
  setTimeout(() => document.addEventListener('click', close), 50);
};

const handleDelete = async () => {
  if (!contextMenu.value.targetId) return;
  try {
    await deleteComment(contextMenu.value.targetId);
    contextMenu.value.show = false;
  } catch (error) {
    alert("Ошибка удаления");
  }
};

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
const handleKeydown = (e: KeyboardEvent) => {
  if (e.shiftKey) return; // Shift + Enter — перенос строки

  if (e.key === 'Enter') {
    if (window.innerWidth > 400) { // На десктопе отправляем по Enter
      e.preventDefault();
      submitComment();
    }
  }
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
watch(comments, () => { nextTick(() => setTimeout(initStickyObserver, 100)); }, { deep: true });
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
            <div class="block-controls-right" v-if="activeBlockIndex === index || activeCommentsBlockId === index">

              <button
                v-if="block.id"
                class="comment-trigger-small"
                :class="{ 'is-active': activeCommentsBlockId === index }"
                @click="toggleComments(index, block.id)"
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
    <Transition name="slide">
      <div v-if="activeCommentsBlockId !== null" class="comments-sidebar">
        <div class="sidebar-header">
          <h3>Обсуждение блока</h3>
          <button class="close-btn" @click="activeCommentsBlockId = null">✕</button>
        </div>

        <div class="comments-list" ref="commentsListRef" @scroll = "handleScroll">
          <div v-for="(group, date) in groupedComments" :key="date" class="comment-group">
            <div class="sticky-sentinel"></div>
            <div class="date-sticky-header" :class="{ 'is-scrolling-active': isScrolling }">
              <span class="date-badge">{{ isToday(date) ? 'Сегодня' : date }}</span>
            </div>

            <div
              v-for="comment in group"
              :key="comment.id"
              class="comment-item"
              @contextmenu="(e) => openContextMenu(e, comment.id)"
            >
              <div class="comment-bubble">
                <div class="comment-item-header">
                  <span class="user-badge">{{ comment.username }}</span>
                </div>
                <p class="comment-body">{{ comment.content }}</p>
                <div class="comment-footer">
                  <span class="comment-date">{{ formatTime(comment.timestamp) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="sidebar-input-area">
      <textarea
        v-model="newCommentText"
        @keydown="handleKeydown"
        placeholder="Написать комментарий... (Enter для отправки)"
        rows="1"
      ></textarea>
          <button
            class="send-btn"
            :disabled="!newCommentText.trim()"
            @click="submitComment"
          >
            ▲
          </button>
        </div>
      </div>
    </Transition>
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
  max-width: 720px; /* Приравниваем к читалке */
  background-color: var(--bg-editor-sheet);
  padding: 60px 40px; /* Уменьшили отступы, чтобы текст был той же ширины */
  border-radius: 16px;
  box-shadow: 0 10px 40px var(--shadow-color);
  border: 1px solid var(--border-subtle);
  min-height: 80vh;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.3s;
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
  font-size: 2.5rem; /* Как .chapter-title в читалке */
  font-weight: 800;
  background: none;
  border: none;
  color: var(--text-header);
  outline: none;
  margin-bottom: 40px;
  text-align: center; /* Центрируем заголовок как в читалке */
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
  font-size: 1.25rem; /* Как .chapter-content в читалке */
  line-height: 1.8;   /* Как .chapter-content в читалке */
  resize: none;
  outline: none;
  padding: 6px 0;
  overflow: hidden;
  font-family: inherit;
  -webkit-font-smoothing: antialiased;
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
/* --- ПРАВАЯ ПАНЕЛЬ УПРАВЛЕНИЯ БЛОКОМ --- */
.side-control.right {
  width: 80px; /* Увеличили ширину, чтобы влезли две кнопки */
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding-right: 10px;
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
  font-size: 1.1rem;
  cursor: pointer;
  opacity: 0.4;
  transition: all 0.2s ease;
  padding: 4px;
}

.comment-trigger-small:hover,
.comment-trigger-small.is-active {
  opacity: 1;
  transform: scale(1.1);
}

.comment-trigger-small.is-active {
  filter: drop-shadow(0 0 5px var(--btn-plus));
}

/* --- САЙДБАР КОММЕНТАРИЕВ --- */
.comments-sidebar {
  position: fixed;
  top: 0;
  right: 0;
  width: 350px;
  height: 100vh;
  background-color: var(--bg-editor-sheet);
  border-left: 1px solid var(--border-subtle);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  box-shadow: -10px 0 30px rgba(0,0,0,0.1);
}

.sidebar-header {
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-subtle);
}

.sidebar-header h3 {
  margin: 0;
  font-size: 1.1rem;
  color: var(--text-header);
}

.close-btn {
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 1.2rem;
  cursor: pointer;
  transition: color 0.2s;
}

.close-btn:hover {
  color: var(--btn-plus);
}

/* --- СПИСОК КОММЕНТАРИЕВ --- */
.comments-list {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background-color: var(--bg-editor-sheet);
  scrollbar-width: thin;
  scrollbar-color: var(--border-subtle) transparent;
}

.comment-group {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* --- ЛИПКИЕ ДАТЫ --- */
.sticky-sentinel {
  position: absolute;
  top: 0;
  height: 1px;
  width: 100%;
}

.date-sticky-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  justify-content: center;
  margin: 10px 0;
}

.date-badge {
  background: var(--bg-main);
  border: 1px solid var(--border-subtle);
  color: var(--text-muted);
  font-size: 0.75rem;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 12px;
  opacity: 0;
  transform: translateY(-5px);
  transition: all 0.3s ease;
}

.date-badge[data-in-text],
.is-scrolling-active .date-badge {
  opacity: 1;
  transform: translateY(0);
}

/* --- ПУЗЫРЬКИ СООБЩЕНИЙ --- */
.comment-item {
  display: flex;
  flex-direction: column;
  max-width: 90%;
}

.comment-bubble {
  background: var(--bg-main);
  border: 1px solid var(--border-subtle);
  border-radius: 14px 14px 14px 4px;
  padding: 10px 12px;
  transition: background 0.2s;
}

.comment-bubble:hover {
  background: var(--hover-dropdowb); /* Использую твою переменную с опечаткой как в коде */
}

.user-badge {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--btn-plus);
  display: block;
  margin-bottom: 4px;
}

.comment-body {
  margin: 0;
  font-size: 0.95rem;
  line-height: 1.4;
  color: var(--text-header);
  white-space: pre-wrap;
}

.comment-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
}

.comment-date {
  font-size: 0.7rem;
  color: var(--text-muted);
}

/* --- ПОЛЕ ВВОДА --- */
.sidebar-input-area {
  padding: 15px;
  border-top: 1px solid var(--border-subtle);
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.sidebar-input-area textarea {
  flex: 1;
  background: var(--bg-main);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  padding: 10px;
  color: var(--text-header);
  resize: none;
  font-family: inherit;
  outline: none;
}

.send-btn {
  background: var(--btn-plus);
  color: white;
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* --- КОНТЕКСТНОЕ МЕНЮ --- */
.context-menu {
  position: fixed;
  background: var(--bg-editor-sheet);
  border: 1px solid var(--border-subtle);
  box-shadow: 0 5px 15px var(--shadow-color);
  border-radius: 8px;
  z-index: 10000;
  min-width: 140px;
  padding: 5px;
}

.menu-item {
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 0.9rem;
  transition: background 0.2s;
}

.menu-item.delete {
  color: #ff4d4f;
}

.menu-item:hover {
  background: var(--bg-main);
}

/* Анимация появления сайдбара */
.slide-enter-active, .slide-leave-active {
  transition: transform 0.3s ease;
}
.slide-enter-from, .slide-leave-to {
  transform: translateX(100%);
}
</style>
