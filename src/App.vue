<script setup lang="ts">
import {RouterView} from 'vue-router'
import ToastContainer from "@/components/toast/ToastContainer.vue";
import {useAuthStore} from "@/api/auth.ts";
import {useLibraryStore} from "@/components/library/libraryStore.ts";
import {onMounted, onUnmounted, provide, ref, watch} from "vue";
import {useSocialStore} from "@/components/social/socialStore.ts";
import {useMessengerStore} from "@/components/chat/messengerStore.ts";

const authStore = useAuthStore();
const libraryStore = useLibraryStore();
const socialStore = useSocialStore();
const messengerStore = useMessengerStore();

import UserContextMenu from "@/components/menu/UserContextMenu.vue"
import router from "@/router";

type UserContextMenuInstance = InstanceType<typeof UserContextMenu>;

const openUserProfile = (userId: number) => {
  router.push(`/profile/${userId}`);
};
const globalContextMenu = ref<UserContextMenuInstance | null>(null);

provide('openUserMenu', (event: MouseEvent, userId: number, username: string) => {
  globalContextMenu.value?.openMenu(event, userId, username);
});
provide('openUserProfile', openUserProfile);
watch(
  () => authStore.isAuthenticated,
  async (isAuth) => {
    if (isAuth) {
      await messengerStore.loadMyChats();
      await libraryStore.fetchMyStatuses();
      await socialStore.fetchSocialGraph();
      if (authStore.userDetails?.id) {
        messengerStore.initGlobalSocket(authStore.userDetails.id);
      }
    } else {
      libraryStore.clearLibrary();
      socialStore.clearGraph();
      messengerStore.clearAndDisconnect();

    }
  },
  { immediate: true }
);

const handleVisibilityChange = () => {
  if (document.visibilityState === 'visible' && authStore.isAuthenticated) {
    console.log('Пользователь вернулся на сайт, обновляем библиотеку в фоне...');
    libraryStore.fetchMyStatuses(true);
    socialStore.fetchSocialGraph(true);
  }
};

onMounted(() => {
  document.addEventListener('visibilitychange', handleVisibilityChange);
});

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange);
});

</script>

<template>
  <RouterView />
  <UserContextMenu ref="globalContextMenu" />
  <ToastContainer />
</template>

<style>
/* Глобальные стили можно писать здесь или в assets/main.css */
body, html {
  background-color: var(--bg-main);
  color: var(--text-header);
}
</style>
