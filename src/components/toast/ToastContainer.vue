<script setup lang="ts">
import { useToastStore } from '@/components/toast/toastStore.ts';

const toastStore = useToastStore();
</script>

<template>
  <div class="toast-container">
    <TransitionGroup name="toast-anim">
      <div
        v-for="toast in toastStore.toasts"
        :key="toast.id"
        class="toast-message"
        :class="toast.type"
        @click="toastStore.removeToast(toast.id)"
      >
        <span v-if="toast.type === 'success'" class="icon">✅</span>
        <span v-if="toast.type === 'error'" class="icon">❌</span>
        <span v-if="toast.type === 'info'" class="icon">ℹ️</span>

        <p>{{ toast.text }}</p>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
/* Контейнер фиксируем в правом нижнем (или верхнем) углу поверх всего сайта */
.toast-container {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 12px;
  pointer-events: none; /* Чтобы клики проходили сквозь пустой контейнер */
}

.toast-message {
  pointer-events: auto; /* Сами тосты кликабельны (для закрытия) */
  min-width: 280px;
  padding: 16px 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: white;
  font-weight: 600;
  font-size: 0.95rem;
  box-shadow: 0 10px 30px rgba(0,0,0,0.3);
  cursor: pointer;
  border: 1px solid rgba(255,255,255,0.1);
  backdrop-filter: blur(8px);
}

.icon {
  font-size: 1.25rem;
  line-height: 1;
}

/* Цвета для разных типов */
.toast-message.success { background-color: rgba(16, 185, 129, 0.9); border-color: rgba(16, 185, 129, 1); } /* emerald */
.toast-message.error { background-color: rgba(239, 68, 68, 0.9); border-color: rgba(239, 68, 68, 1); } /* red */
.toast-message.info { background-color: rgba(59, 130, 246, 0.9); border-color: rgba(59, 130, 246, 1); } /* blue */

.toast-message p { margin: 0; line-height: 1.4; }

/* Анимация (TransitionGroup) */
.toast-anim-enter-active,
.toast-anim-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.toast-anim-enter-from {
  opacity: 0;
  transform: translateX(100%) scale(0.95); /* Выезжает справа с небольшим скейлом */
}
.toast-anim-leave-to {
  opacity: 0;
  transform: translateX(100%); /* Уезжает вправо */
}
</style>
