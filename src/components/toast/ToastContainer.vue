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
        <span v-if="toast.type === 'success'">✅</span>
        <span v-if="toast.type === 'error'">❌</span>
        <span v-if="toast.type === 'info'">ℹ️</span>

        <p>{{ toast.text }}</p>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
/* Контейнер фиксируем в правом нижнем (или верхнем) углу поверх всего сайта */
.toast-container {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none; /* Чтобы клики проходили сквозь пустой контейнер */
}

.toast-message {
  pointer-events: auto; /* Сами тосты кликабельны (для закрытия) */
  min-width: 250px;
  padding: 12px 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: white;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
  cursor: pointer;
}

/* Цвета для разных типов */
.toast-message.success { background-color: var(--btn-plus, #42b883); }
.toast-message.error { background-color: #e74c3c; }
.toast-message.info { background-color: #3498db; }

.toast-message p { margin: 0; }

/* Анимация (TransitionGroup) */
.toast-anim-enter-active,
.toast-anim-leave-active {
  transition: all 0.3s ease;
}
.toast-anim-enter-from {
  opacity: 0;
  transform: translateX(100%); /* Выезжает справа */
}
.toast-anim-leave-to {
  opacity: 0;
  transform: translateX(100%); /* Уезжает вправо */
}
</style>
