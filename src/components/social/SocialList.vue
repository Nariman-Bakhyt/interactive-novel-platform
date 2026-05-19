<script setup lang="ts">
import {ref, onMounted, onUnmounted, nextTick, inject} from "vue";
import RelationshipButton from "@/components/social/RelationshipButton.vue";
import { DEFAULT_AVATAR } from "@/utils/media.ts";

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
      isLast.value = data.last;
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
      <img :src="user.avatarUrl || DEFAULT_AVATAR" class="mini-avatar">

      <div class="user-content">
        <span class="username">{{ user.username }}</span>
        <div class="actions-wrapper">
          <RelationshipButton :userId="user.userId" />
        </div>
      </div>
    </div>

    <div ref="observerTarget" class="scroll-trigger">
      <div v-if="isLoading" class="spinner-small"></div>
      <p v-if="isLast && users.length === 0" class="empty-state">Список пуст</p>
    </div>
  </div>
</template>

<style scoped>

.list-container {
  display: flex;
  flex-direction: column;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 8px;
  transition: all 0.2s ease;
  cursor: pointer;
  border: 1px solid transparent;
}

.user-item:hover {
  background: var(--hover-dropdowb); /* Подсвечиваем при наведении */
  border-color: var(--border-color);
}

.mini-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--border-color);
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
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--text-header);
}

.actions-wrapper {
  width: 200px;
  display: flex;
  justify-content: flex-end;
}

.scroll-trigger {
  padding: 24px;
  display: flex;
  justify-content: center;
  color: var(--text-muted);
}

.empty-state {
  font-size: 0.95rem;
  font-style: italic;
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

/* Настройки RelationshipButton для списка */
:deep(.relationship-actions) {
  display: flex !important;
  flex-direction: column !important;
  gap: 8px !important;
  width: 100% !important;
}

:deep(.relationship-actions button) {
  width: 100% !important;
  height: 32px !important;
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
</style>
