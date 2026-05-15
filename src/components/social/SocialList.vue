<script setup lang="ts">
import {ref, onMounted, onUnmounted, nextTick, inject} from "vue";
import RelationshipButton from "@/components/social/RelationshipButton.vue";

const props = defineProps<{
  fetchFn: (page: number, size: number) => Promise<any>;
}>();

const users = ref<any[]>([]);
const page = ref(0);
const size = 3;
const isLast = ref(false);
const isLoading = ref(false);
const observerTarget = ref<HTMLElement | null>(null);
const openUserMenu = inject('openUserMenu') as (event: MouseEvent, userId: number, username: string) => void;
const openUserProfile = inject('openUserProfile') as (userId: number) => void;
const loadMore = async () => {
  // 1. Проверка блокировки
  if (isLoading.value || isLast.value) return;

  isLoading.value = true;
  try {
    const data = await props.fetchFn(page.value, size);

    if (data && data.content) {
      users.value.push(...data.content);
      // Важно: проверяем по totalPages, если используем Spring PagedModel
      isLast.value = (data.page.number + 1) >= data.page.totalPages || data.content.length === 0;
      page.value++;
      await nextTick();
    }
  } catch (error) {
    console.error("Ошибка загрузки списка:", error);
    isLast.value = true; // Останавливаем при ошибке
  } finally {
    // 2. Сбрасываем флаг ДО того, как проверять видимость дива
    isLoading.value = false;
  }

  if (!isLast.value && observerTarget.value) {
    const rect = observerTarget.value.getBoundingClientRect();
    if (rect.top <= window.innerHeight) {
      setTimeout(() => loadMore(), 50);
    }
  }
};
let observer: IntersectionObserver;

onMounted(() => {
  loadMore();
  observer = new IntersectionObserver(([entry]) => {
    if (entry && entry.isIntersecting) loadMore();
  }, { threshold: 1.0 });

  if (observerTarget.value) observer.observe(observerTarget.value);
});

onUnmounted(() => observer?.disconnect());
</script>

<template>
  <div class="list-container">
    <div v-for="user in users" :key="user.userId" class="user-item"
         @click="openUserProfile(user.userId)"
         @contextmenu.prevent="openUserMenu($event, user.userId, user.username)"
    >
      <img :src="user.avatarUrl || 'http://127.0.0.1:9000/interactive-novel-assets/avatars/default-avatar.png'" class="mini-avatar">

      <div class="user-content">
        <span class="username">{{ user.username }}</span>
        <div class="actions-wrapper">
          <RelationshipButton :userId="user.userId" />
        </div>
      </div>
    </div>

    <div ref="observerTarget" class="scroll-trigger">
      <p v-if="isLoading">Загрузка...</p>
      <p v-if="isLast && users.length === 0">Список пуст</p>
    </div>
  </div>
</template>

<style scoped>


.user-item:hover {
  background: var(--hover-dropdowb); /* Подсвечиваем при наведении */
}
/* Сюда вставь те самые стили "50% и сдвиг вправо", которые мы сделали в прошлом шаге */
.list-container {
  display: flex;
  flex-direction: column;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0; /* Убрал боковые отступы, чтобы на странице смотрелось лучше */
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.mini-avatar {
  width: 45px;
  height: 45px;
  border-radius: 50%;
  object-fit: cover;
}

.user-content {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  flex: 1;
}

.username {
  flex: 1;
  font-size: 1rem;
  font-weight: 600;
  color: #e0e0e0;
}

.actions-wrapper {
  width: 55%;
  display: flex;
  justify-content: flex-end;
}

.scroll-trigger {
  padding: 20px;
  text-align: center;
  color: #888;
}

/* Настройки RelationshipButton для 3-х линий внутри списка */
:deep(.relationship-actions) {
  display: flex !important;
  flex-direction: column !important;
  gap: 4px !important;
  width: 100% !important;
}

:deep(.relationship-actions button) {
  width: 100% !important;
  height: 28px !important;
  font-size: 0.75rem !important;
  padding: 0 8px !important;
  justify-content: center !important;
}

:deep(.action-row) {
  display: flex !important;
  gap: 4px !important;
  width: 100% !important;
}

:deep(.btn-block-action) {
  height: 24px !important;
  background: transparent !important;
  border: 1px solid rgba(231, 76, 60, 0.2) !important;
}
</style>
