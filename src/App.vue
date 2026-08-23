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

// Флаг готовности приложения (блокирует запросы и watch до прохождения проверки)
const isAppReady = ref(false);
let isPoWInProgress = false;

type UserContextMenuInstance = InstanceType<typeof UserContextMenu>;

const openUserProfile = (userId: number) => {
  router.push(`/profile/${userId}`);
};
const globalContextMenu = ref<UserContextMenuInstance | null>(null);

provide('openUserMenu', (event: MouseEvent, userId: number, username: string) => {
  globalContextMenu.value?.openMenu(event, userId, username);
});
provide('openUserProfile', openUserProfile);

// Загрузка пользовательских данных после готовности приложения
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
    // Не даем сработать вочу до того как пройдет PoW и приложение загрузится
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
    console.log('Пользователь вернулся во вкладку, обновляем контекст в фоне...');
    libraryStore.fetchMyStatuses(true);
    socialStore.fetchSocialGraph(true);
  }
};

async function initializeApplicationData() {
  if (isAppReady.value) return;
  try {
    // Загрузка общих метаданных для всех (жанры, теги)
    await Promise.all([getAllGenres(), getAllTags()]);

    // Включаем флаг готовности
    isAppReady.value = true;

    // Если пользователь уже авторизован, загружаем его данные
    if (authStore.isAuthenticated) {
      await loadUserData();
    }
  } catch (e) {
    console.error("Ошибка предзагрузки метаданных:", e);
    isAppReady.value = true;
  }
}

const getHeaderFromResponse = (res: any, name: string): string => {
  let val: any = undefined;
  if (res.headers && typeof (res.headers as any).get === 'function') {
    val = (res.headers as any).get(name) || (res.headers as any).get(name.toLowerCase());
  } else if (res.headers) {
    val = (res.headers as any)[name] || (res.headers as any)[name.toLowerCase()];
  }
  return val != null ? String(val) : "";
};

onMounted(async () => {
  if (isPoWInProgress || isAppReady.value) return;
  isPoWInProgress = true;

  document.addEventListener('visibilitychange', handleVisibilityChange);

  const challengeData = window.__INITIAL_CHALLENGE__;
  let salt = "";
  let difficulty = 0;
  let skip = false;

  // Если OpenResty не передал данные или это локальный запуск в dev режиме (Vite)
  if (!challengeData || !challengeData.skip || challengeData.skip === "WEN_POW_SKIP") {
    console.warn("Данные PoW защиты не найдены или запущен локальный сервер. Запрашиваем челлендж с бэкенда...");
    try {
      const res = await apiClient.get('/auth/public/challenge');

      const isSkip = res.data?.skip === true || getHeaderFromResponse(res, 'x-skip-challenge') === 'true';
      if (isSkip) {
        console.log("[WénLib] Пропуск проверки PoW (валидная сессия/гость).");
        await initializeApplicationData();
        isPoWInProgress = false;
        return;
      }

      salt = res.data?.salt || getHeaderFromResponse(res, 'x-challenge-salt');
      difficulty = parseInt(res.data?.difficulty || getHeaderFromResponse(res, 'x-challenge-difficulty') || "2", 10);
      skip = false;
    } catch (err) {
      console.error("Не удалось получить челлендж с бэкенда:", err);
      await initializeApplicationData();
      isPoWInProgress = false;
      return;
    }
  } else {
    salt = challengeData.salt;
    difficulty = parseInt(challengeData.difficulty, 10);
    skip = challengeData.skip === "true";
  }

  // СЦЕНАРИЙ 1: Кука уже валидна
  if (skip) {
    console.log("[WénLib] Кука валидна. Доступ открыт без решения PoW.");
    await initializeApplicationData();
    isPoWInProgress = false;
    return;
  }

  // Если соль отсутствует, не пытаемся решать пустой челлендж
  if (!salt) {
    console.warn("[WénLib] Соль PoW отсутствует, пропускаем проверку...");
    await initializeApplicationData();
    isPoWInProgress = false;
    return;
  }

  // СЦЕНАРИЙ 2: Вычисление и отправка PoW
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
      await initializeApplicationData();
    }
  } catch (error) {
    console.error("Ошибка верификации PoW, пробуем повторный запрос со свежей солью:", error);
    try {
      const retryRes = await apiClient.get('/auth/public/challenge');
      const isRetrySkip = retryRes.data?.skip === true || getHeaderFromResponse(retryRes, 'x-skip-challenge') === 'true';
      if (isRetrySkip) {
        await initializeApplicationData();
        return;
      }

      const retrySalt = retryRes.data?.salt || getHeaderFromResponse(retryRes, 'x-challenge-salt');
      const retryDiff = parseInt(retryRes.data?.difficulty || getHeaderFromResponse(retryRes, 'x-challenge-difficulty') || "2", 10);

      if (retrySalt) {
        const { solveChallenge } = useProofOfWork();
        const retryNonce = await solveChallenge(retrySalt, retryDiff);
        const retryVerifyRes = await apiClient.post('/auth/public/verify-challenge', {
          salt: retrySalt,
          nonce: retryNonce
        });
        if (retryVerifyRes.status === 200) {
          console.log("[WénLib] Повторная проверка PoW успешно пройдена!");
        }
      }
    } catch (retryErr) {
      console.error("Повторная попытка верификации PoW не удалась:", retryErr);
    } finally {
      await initializeApplicationData();
    }
  } finally {
    isPoWInProgress = false;
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
    <p>Загрузка системы WénLib...</p>
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
