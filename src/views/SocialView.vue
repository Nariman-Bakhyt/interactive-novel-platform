<script setup lang="ts">
import { ref } from 'vue';
import {
  getFollowers,
  getFollowing,
  getFriends,
  getCloseFriends,
  getIncomingRequest,
  getOutgoingRequests,
  getMyBlackList
} from "@/api/socialService";

import SocialList from "@/components/social/SocialList.vue";

interface SocialTab {
  id: string;
  label: string;
  fn: (page: number, size: number) => Promise<any>;
}

// Добавили все функции и логично отсортировали вкладки
const tabs: SocialTab[] = [
  { id: 'friends', label: 'Друзья', fn: getFriends },
  {
    id: 'close',
    label: 'Близкие друзья',
    fn: async () => {
      const data = await getCloseFriends();
      return { content: data, last: true };
    }
  },
  { id: 'followers', label: 'Подписчики', fn: getFollowers },
  { id: 'following', label: 'Подписки', fn: getFollowing },
  { id: 'incoming', label: 'Входящие заявки', fn: getIncomingRequest },
  { id: 'outgoing', label: 'Исходящие заявки', fn: getOutgoingRequests },
  { id: 'blacklist', label: 'Черный список', fn: getMyBlackList },
];

const activeTab = ref<SocialTab>(tabs[0] as SocialTab);

const setTab = (tab: SocialTab) => {
  activeTab.value = tab;
};
</script>

<template>
  <div class="social-page">
    <aside class="social-sidebar">
      <h2>Связи</h2>
      <nav>
        <button
          v-for="tab in tabs"
          :key="tab.id"
          :class="['tab-btn', { active: activeTab.id === tab.id }]"
          @click="setTab(tab)"
        >
          {{ tab.label }}
        </button>
      </nav>
    </aside>

    <main class="social-content">
      <header class="content-header">
        <h1>{{ activeTab.label }}</h1>
      </header>

      <SocialList
        :key="activeTab.id"
        :fetchFn="activeTab.fn"
      />
    </main>
  </div>
</template>

<style scoped>
.social-page {
  display: grid;
  grid-template-columns: 250px 1fr;
  gap: 30px;
  max-width: 1200px;
  margin: 40px auto;
  padding: 0 20px;
}
.social-sidebar {
  background: #2c2c2c;
  padding: 20px;
  border-radius: 16px;
  height: fit-content;
  position: sticky;
  top: 100px;
}
.tab-btn {
  width: 100%;
  text-align: left;
  padding: 12px 15px;
  background: transparent;
  color: #aaa;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
  margin-bottom: 5px;
}
.tab-btn:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #fff;
}
.tab-btn.active {
  background: #3498db;
  color: #fff;
}
.social-content {
  background: #2c2c2c;
  border-radius: 16px;
  padding: 30px;
  min-height: 600px;
}
.content-header h1 {
  margin: 0 0 25px 0;
  font-size: 1.5rem;
}
</style>
