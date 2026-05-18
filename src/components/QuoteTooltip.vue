<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import {useCommentStore} from "@/components/chat/commentStore.ts";

const route = useRoute();
const chatStore = useCommentStore();

const showButton = ref(false);
const btnPos = ref({ top: 0, left: 0 });
const selectedText = ref('');

const updateSelection = () => {
  const selection = window.getSelection();
  const text = selection?.toString().trim();

  // Если текста нет или он слишком короткий — скрываем
  if (!text || text.length < 3) {
    showButton.value = false;
    return;
  }

  const range = selection?.getRangeAt(0);
  const rect = range?.getBoundingClientRect();

  if (rect && rect.width > 0) {
    selectedText.value = text;

    // Рассчитываем позицию (центрируем над выделением)
    btnPos.value = {
      top: rect.top + window.scrollY - 48, // Чуть выше
      left: rect.left + window.scrollX + (rect.width / 2) - 60 // Центрируем по кнопке
    };
    showButton.value = true;
    console.log("Кнопка должна появиться тут:", btnPos.value);
  }
};

const createQuoteComment = () => {
  const selection = window.getSelection();
  if (!selection || selection.rangeCount === 0) return;

  let self = selection.anchorNode?.parentElement;
  if (!self) return;

  // 1. Берем класс самого элемента
  const selfClass = self.classList[0] || 'node';

  // 2. Ищем соседа сверху (предыдущий элемент)
  let prev = self.previousElementSibling;
  let prevClass = "";

  if (prev) {
    prevClass = prev.classList[0] || "";
  } else {
    // Если соседа нет, берем класс родителя как контекст
    prevClass = self.parentElement?.classList[0] || "root";
  }

  // 3. Считаем индекс среди элементов, у которых ТАКОЙ ЖЕ сосед
  // Это сужает поиск до минимума
  const allElements = Array.from(document.querySelectorAll(`.${selfClass}`));
  const filteredByContext = allElements.filter(el => {
    const p = el.previousElementSibling;
    return p ? p.classList.contains(prevClass) : el.parentElement?.classList.contains(prevClass);
  });

  const index = filteredByContext.indexOf(self);

  // Формируем URL: q=текст, c=свой_класс, p=класс_соседа, i=индекс_в_контексте
  const anchorUrl = `${route.path}?c=${selfClass}&p=${prevClass}&i=${index}`;

  chatStore.setQuoteMode({
    text: selectedText.value,
    url: anchorUrl
  });

  showButton.value = false;
  window.getSelection()?.removeAllRanges();
};

onMounted(() => {
  // mouseup для десктопа, чтобы кнопка появлялась когда отпустили мышь
  document.addEventListener('mouseup', updateSelection);
  // На случай если выделение сбросили кликом в пустоту
  document.addEventListener('selectionchange', () => {
    if (!window.getSelection()?.toString().trim()) {
      showButton.value = false;
    }
  });
});

onUnmounted(() => {
  document.removeEventListener('mouseup', updateSelection);
});
</script>

<template>
  <Teleport to="body">
    <button
      v-if="showButton"
      class="quote-floating-btn"
      :style="{
        top: btnPos.top + 'px',
        left: btnPos.left + 'px',
        display: showButton ? 'flex' : 'none'
      }"
      @mousedown.stop="createQuoteComment"
    >
      <span class="icon">❞</span> Цитировать
    </button>
  </Teleport>
</template>

<style scoped>
.quote-floating-btn {
  position: absolute;
  z-index: 999999; /* Максимальный приоритет */
  background: var(--bg-dropdown);
  color: var(--text-header);
  border: 1px solid var(--border-color);
  padding: 8px 16px;
  border-radius: 24px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 15px var(--shadow-color);
  white-space: nowrap;
  pointer-events: auto;
  user-select: none;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.quote-floating-btn:hover {
  background: var(--btn-plus);
  color: white;
  transform: translateY(-2px);
  border-color: var(--btn-plus);
  box-shadow: 0 6px 20px rgba(0,0,0,0.3);
}

.icon {
  font-size: 1.25rem;
  line-height: 1;
}
</style>
