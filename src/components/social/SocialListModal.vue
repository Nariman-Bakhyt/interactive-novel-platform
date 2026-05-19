<script setup lang="ts">
import {onMounted, onUnmounted, ref} from "vue";
import RelationshipButton from "@/components/social/RelationshipButton.vue";
import { DEFAULT_AVATAR } from "@/utils/media.ts";

const props = defineProps<{
  title: string;
  // Функция-запрос, которую мы передадим сверху
  fetchFn: (page: number, size: number) => Promise<any>;
}>();

const emit = defineEmits(['close']);

const users = ref<any[]>([]);
const page = ref(0);
const size = 20;
const isLast = ref(false);
const isLoading = ref(false);
const observerTarget = ref<HTMLElement | null>(null);

const loadMore = async () => {
  if (isLoading.value || isLast.value) return;

  isLoading.value = true;
  try {
    const data = await props.fetchFn(page.value, size);
    users.value.push(...data.content); // В Spring Data обычно данные в поле .content
    isLast.value = data.last;
    page.value++;
  } finally {
    isLoading.value = false;
  }
};

// Intersection Observer для бесконечного скролла
let observer: IntersectionObserver;

onMounted(() => {
  loadMore(); // Первая загрузка

  observer = new IntersectionObserver(([entry]) => {
    if (entry && entry.isIntersecting) {
      loadMore();
    }
  }, { threshold: 1.0 });

  if (observerTarget.value) observer.observe(observerTarget.value);
});

onUnmounted(() => observer.disconnect());
</script>

<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">
      <header>
        <h3>{{ title }}</h3>
        <button class="btn-close" @click="$emit('close')">✕</button>
      </header>

      <div class="list-container">
        <div v-for="user in users" :key="user.userId" class="user-item">
          <img :src="user.avatarUrl || DEFAULT_AVATAR" class="mini-avatar">
          <div class="user-content">
            <div class="user-header">
              <span class="username">{{ user.username }}</span>
            </div>

            <div class="actions-wrapper">
              <RelationshipButton :userId="user.userId" />
            </div>
          </div>
        </div>

        <div ref="observerTarget" class="scroll-trigger">
          <div v-if="isLoading" class="spinner-small"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Затемнение фона с эффектом размытия */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 24px;
}

/* Контейнер самого окна */
.modal-content {
  background: var(--bg-dropdown);
  width: 100%;
  max-width: 500px;
  max-height: 85vh;
  border-radius: 16px;
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5);
  overflow: hidden; /* Чтобы скролл был только внутри списка */
}

/* Шапка модалки */
header {
  padding: 24px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

header h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-header);
}

.btn-close {
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 1.5rem;
  cursor: pointer;
  transition: color 0.2s, background 0.2s;
  line-height: 1;
  padding: 4px;
  border-radius: 4px;
}

.btn-close:hover {
  color: var(--text-header);
  background: var(--hover-dropdowb);
}

/* Область списка со скроллом */
.list-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}

/* Элемент пользователя в списке */
.user-item {
  display: flex;
  align-items: center; /* Центрируем аватарку относительно контента */
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid transparent;
  margin-bottom: 8px;
  transition: all 0.2s ease;
}
.user-item:hover {
  background: var(--hover-dropdowb);
  border-color: var(--border-color);
}

.user-content {
  display: flex;
  flex-direction: row; /* Имя и кнопки в одной строке */
  align-items: center;
  justify-content: space-between; /* Имя влево, кнопки вправо */
  flex: 1;
}

.username {
  flex: 1; /* Занимает оставшееся место слева */
  font-size: 1.05rem;
  font-weight: 600;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--text-header);
}

/* Обертка для кнопок */
.actions-wrapper {
  width: 200px;
  display: flex;
  justify-content: flex-end;
}

.mini-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--border-color);
}
:deep(.relationship-actions) {
  display: flex !important;
  flex-direction: column !important; /* Кнопки всё еще друг под другом (линии) */
  gap: 8px !important;
  width: 100% !important;
}

:deep(.relationship-actions button) {
  width: 100% !important;
  height: 32px !important;    /* Компактная высота */
  font-size: 0.85rem !important;
  padding: 0 12px !important;
  justify-content: center !important;
  border-radius: 6px !important;
  font-weight: 600 !important;
}

:deep(.action-row) {
  display: flex !important;
  gap: 8px !important;
  width: 100% !important;
}
:deep(.btn-block-action) {
  height: 32px !important;
  background: transparent !important;
  border: 1px solid rgba(239, 68, 68, 0.3) !important;
  color: #ef4444 !important;
}
:deep(.btn-block-action:hover) {
  background: rgba(239, 68, 68, 0.1) !important;
}

/* Секция загрузки внизу */
.scroll-trigger {
  padding: 24px;
  display: flex;
  justify-content: center;
  color: var(--text-muted);
}
.spinner-small {
  width: 24px;
  height: 24px;
  border: 3px solid var(--border-subtle);
  border-top-color: var(--btn-plus);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Адаптив под мобилки */
@media (max-width: 480px) {
  .modal-content {
    max-height: 90vh;
  }
}
</style>
