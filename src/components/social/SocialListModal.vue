<script setup lang="ts">


import {onMounted, onUnmounted, ref} from "vue";
import RelationshipButton from "@/components/social/RelationshipButton.vue";

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
  isLast.value = (data.page.number + 1) >= data.page.totalPages || data.content.length === 0;
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
          <button @click="$emit('close')">✕</button>
        </header>

        <div class="list-container">
          <div v-for="user in users" :key="user.userId" class="user-item">
            <img :src="user.avatarUrl || 'http://127.0.0.1:9000/interactive-novel-assets/avatars/default-avatar.png'" class="mini-avatar">
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
            <p v-if="isLoading">Загрузка...</p>
          </div>
        </div>
      </div>
    </div>
  </template>
<style scoped>
/* Затемнение фона с эффектом размытия */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

/* Контейнер самого окна */
.modal-content {
  background: #2c2c2c;
  width: 100%;
  max-width: 650px;
  max-height: 80vh;
  border-radius: 16px;
  border: 1px solid #3d3d3d;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
  overflow: hidden; /* Чтобы скролл был только внутри списка */
}

/* Шапка модалки */
header {
  padding: 20px;
  border-bottom: 1px solid #3d3d3d;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

header h3 {
  margin: 0;
  font-size: 1.2rem;
  color: #e0e0e0;
}

.btn-close {
  background: none;
  border: none;
  color: #888;
  font-size: 1.5rem;
  cursor: pointer;
  transition: color 0.2s;
}

.btn-close:hover {
  color: #fff;
}

/* Область списка со скроллом */
.list-container {
  flex: 1;
  overflow-y: auto;
  padding: 10px 20px;

  /* Стилизация скроллбара (для Chrome/Safari) */
}

.list-container::-webkit-scrollbar {
  width: 6px;
}

.list-container::-webkit-scrollbar-thumb {
  background: #4d4d4d;
  border-radius: 10px;
}

/* Элемент пользователя в списке */
/* Уменьшаем вертикальные отступы у самого элемента списка */
.user-item {
  display: flex;
  align-items: center; /* Центрируем аватарку относительно контента */
  gap: 12px;
  padding: 10px 15px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
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
  font-size: 0.95rem;
  font-weight: 600;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Обертка для кнопок */
.actions-wrapper {
  width: 55%; /* Вот твои ~50% ширины */
  display: flex;
  justify-content: flex-end;
}

.user-item:last-child {
  border-bottom: none;
}

.mini-avatar {
  width: 45px;
  height: 45px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #1a1a1a;
}
:deep(.relationship-actions) {
  display: flex !important;
  flex-direction: column !important; /* Кнопки всё еще друг под другом (линии) */
  gap: 4px !important;
  width: 100% !important; /* 100% от родителя (т.е. от 55% wrapper-а) */
}

:deep(.relationship-actions button) {
  width: 100% !important;
  height: 28px !important;    /* Компактная высота */
  font-size: 0.75rem !important;
  padding: 0 8px !important;
  justify-content: center !important;
}

:deep(.action-row) {
  display: flex !important;
  gap: 4px !important;
  width: 100% !important;
}
/* Секция загрузки внизу */
.scroll-trigger {
  padding: 20px;
  text-align: center;
  color: #888;
  font-size: 0.9rem;
}

/* Адаптив под мобилки */
@media (max-width: 480px) {
  .modal-content {
    max-height: 90vh;
  }
}
</style>
