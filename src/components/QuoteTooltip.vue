<script setup lang="ts">
import {onMounted, onUnmounted, ref} from 'vue';
import {useRoute} from 'vue-router';
import {useCommentStore} from "@/components/chat/commentStore.ts";

const route = useRoute();
const chatStore = useCommentStore();

const showButton = ref(false);
const btnPos = ref({ top: 0, left: 0 });
const selectedText = ref('');

const updateSelection = () => {
  const selection = window.getSelection();
  const text = selection?.toString().trim();

  
  if (!text || text.length < 3) {
    showButton.value = false;
    return;
  }

  const range = selection?.getRangeAt(0);
  const rect = range?.getBoundingClientRect();

  if (rect && rect.width > 0) {
    selectedText.value = text;

    
    btnPos.value = {
      top: rect.top + window.scrollY - 48, 
      left: rect.left + window.scrollX + (rect.width / 2) - 60 
    };
    showButton.value = true;
    console.log("Кнопка должна появиться тут:", btnPos.value);
  }
};

const createQuoteComment = () => {
  const selection = window.getSelection();
  if (!selection || selection.rangeCount === 0) return;

  const self = selection.anchorNode?.parentElement;
  if (!self) return;

  
  const selfClass = self.classList[0] || 'node';

  
  const prev = self.previousElementSibling;
  let prevClass = "";

  if (prev) {
    prevClass = prev.classList[0] || "";
  } else {
    
    prevClass = self.parentElement?.classList[0] || "root";
  }

  
  
  const allElements = Array.from(document.querySelectorAll(`.${selfClass}`));
  const filteredByContext = allElements.filter(el => {
    const p = el.previousElementSibling;
    return p ? p.classList.contains(prevClass) : el.parentElement?.classList.contains(prevClass);
  });

  const index = filteredByContext.indexOf(self);

  
  const anchorUrl = `${route.path}?c=${selfClass}&p=${prevClass}&i=${index}`;

  chatStore.setQuoteMode({
    text: selectedText.value,
    url: anchorUrl
  });

  showButton.value = false;
  window.getSelection()?.removeAllRanges();
};

onMounted(() => {
  
  document.addEventListener('mouseup', updateSelection);
  
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
  z-index: 999999; 
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
