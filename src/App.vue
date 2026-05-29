<script setup lang="ts">
import { RouterView } from 'vue-router'
import ToastContainer from "@/components/toast/ToastContainer.vue";
import { useAuthStore } from "@/api/auth.ts";
import { useLibraryStore } from "@/components/library/libraryStore.ts";
import { onMounted, onUnmounted, provide, ref, watch } from "vue";
import { useSocialStore } from "@/components/social/socialStore.ts";
import { useMessengerStore } from "@/components/chat/messengerStore.ts";
import UserContextMenu from "@/components/menu/UserContextMenu.vue"
import router from "@/router";
import { getAllGenres, getAllTags } from "@/api/novelService.ts";
import { useProofOfWork } from "@/composables/useProofOfWork.ts";
import apiClient from "@/api/axios.ts";

declare global {
  interface Window {
    __INITIAL_CHALLENGE__?: {
      skip: string;
      salt: string;
      difficulty: string;
    };
  }
}

const authStore = useAuthStore();
const libraryStore = useLibraryStore();
const socialStore = useSocialStore();
const messengerStore = useMessengerStore();

// Флаг готовности приложения (блокирует преждевременные запросы из watch)
const isAppReady = ref(false);

type UserContextMenuInstance = InstanceType<typeof UserContextMenu>;

const openUserProfile = (userId: number) => {
  router.push(`/profile/${userId}`);
};
const globalContextMenu = ref<UserContextMenuInstance | null>(null);

provide('openUserMenu', (event: MouseEvent, userId: number, username: string) => {
  globalContextMenu.value?.openMenu(event, userId, username);
});
provide('openUserProfile', openUserProfile);

// Функционал загрузки пользовательских данных вынесен в отдельный метод
async function loadUserData() {
  await Promise.all([
    messengerStore.loadMyChats(),
    libraryStore.fetchMyStatuses(),
    socialStore.fetchSocialGraph()
  ]);
  if (authStore.userDetails?.id) {
    messengerStore.initGlobalSocket(authStore.userDetails.id);
  }
  const savedChatId = localStorage.getItem('active_conversation_id');
  if (savedChatId) {
    messengerStore.openChat(Number(savedChatId));
  }
}

watch(
  () => authStore.isAuthenticated,
  async (isAuth) => {
    // ЕСЛИ ПРИЛОЖЕНИЕ ЕЩЕ НЕ ПРОШЛО PoW — ИГНОРИРУЕМ ЗАПРОСЫ
    if (!isAppReady.value) return;

    if (isAuth) {
      await loadUserData();
    } else {
      libraryStore.clearLibrary();
      socialStore.clearGraph();
      messengerStore.clearAndDisconnect();
    }
  },
  { immediate: true }
);

const handleVisibilityChange = () => {
  if (document.visibilityState === 'visible' && authStore.isAuthenticated && isAppReady.value) {
    console.log('Пользователь вернулся на сайт, обновляем библиотеку в фоне...');
    libraryStore.fetchMyStatuses(true);
    socialStore.fetchSocialGraph(true);
  }
};

async function initializeApplicationData() {
  try {
    // Грузим общие метаданные для всех (жанры, теги)
    await Promise.all([getAllGenres(), getAllTags()]);

    // Включаем флаг готовности
    isAppReady.value = true;

    // Если пользователь уже был авторизован, точечно догружаем его личные данные
    if (authStore.isAuthenticated) {
      await loadUserData();
    }
  } catch (e) {
    console.error("Ошибка предзагрузки метаданных:", e);
  }
}

onMounted(async () => {
  document.addEventListener('visibilitychange', handleVisibilityChange);

  const challengeData = window.__INITIAL_CHALLENGE__;
  let salt = "";
  let difficulty = 0;
  let skip = false;

  // Если OpenResty по какой-то причине не отдал объект или мы в dev режиме (Vite)
  if (!challengeData || !challengeData.skip || challengeData.skip === "WEN_POW_SKIP") {
    console.warn("Данные PoW защиты не найдены или запущен локальный сервер. Запрашиваем челлендж с бэкенда...");
    try {
      const res = await apiClient.get('/auth/public/challenge');
      const skipHeader = res.headers['x-skip-challenge'];
      if (skipHeader === 'true') {
        console.log("[WénLib] Бэкенд разрешил пропуск PoW.");
        await initializeApplicationData();
        return;
      }
      salt = res.headers['x-challenge-salt'] as string;
      difficulty = parseInt(res.headers['x-challenge-difficulty'] as string, 10);
      skip = false;
    } catch (err) {
      console.error("Не удалось получить челлендж с бэкенда:", err);
      // Если бэкенд не отвечает или выдает ошибку, пробуем продолжить
      await initializeApplicationData();
      return;
    }
  } else {
    salt = challengeData.salt;
    difficulty = parseInt(challengeData.difficulty, 10);
    skip = challengeData.skip === "true";
  }

  // СЦЕНАРИЙ А: Кука гостя валидна
  if (skip) {
    console.log("[WénLib] Кука валидна. Доступ разрешен без решения задач.");
    await initializeApplicationData();
    return;
  }

  // СЦЕНАРИЙ Б: Требуется вычисление PoW
  console.log(`[WénLib] Требуется проверка PoW. Сложность: ${difficulty}`);

  try {
    const { solveChallenge } = useProofOfWork();
    const nonce = await solveChallenge(salt, difficulty);

    const response = await apiClient.post('/auth/public/verify-challenge', {
      salt: salt,
      nonce: nonce
    });

    if (response.status === 200) {
      console.log("[WénLib] Проверка PoW успешно пройдена!");
      // Только после успешного 200-го статуса разблокируем приложение и стягиваем данные
      await initializeApplicationData();
    }
  } catch (error) {
    console.error("Ошибка верификации PoW:", error);
  }
});

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange);
});
</script>

<template>
  <template v-if="isAppReady">
    <RouterView />
    <UserContextMenu ref="globalContextMenu" />
    <ToastContainer />
  </template>

  <div v-else class="app-loading-screen">
    <div class="spinner"></div>
    <p>Проверка безопасности WénLib...</p>
  </div>
</template>

<style>
body, html {
  background-color: var(--bg-main);
  color: var(--text-header);
}
.app-loading-screen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: var(--bg-main);
  color: var(--text-header);
  font-family: var(--main-font);
}
/* Стили для простейшего спиннера */
.spinner {
  width: 50px;
  height: 50px;
  border: 5px solid rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  border-top-color: var(--libris-primary, #3b82f6);
  animation: spin 1s ease-in-out infinite;
  margin-bottom: 20px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
