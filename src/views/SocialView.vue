<script setup lang="ts">
import {ref} from 'vue';
import {
  getCloseFriends,
  getFollowers,
  getFollowing,
  getFriends,
  getIncomingRequest,
  getMyBlackList,
  getOutgoingRequests
} from "@/api/socialService";

import SocialList from "@/components/social/SocialList.vue";

interface SocialTab {
  id: string;
  label: string;
  fn: (page: number, size: number) => Promise<any>;
}


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
  grid-template-columns: 280px 1fr;
  gap: 32px;
  max-width: 1200px;
  margin: 100px auto 60px;
  padding: 0 24px;
}
.social-sidebar {
  background: var(--bg-dropdown);
  padding: 24px;
  border-radius: 16px;
  height: fit-content;
  position: sticky;
  top: 100px;
  border: 1px solid var(--border-color);
  box-shadow: 0 4px 12px var(--shadow-color);
}
.social-sidebar h2 {
  margin: 0 0 20px;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-header);
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}
.tab-btn {
  width: 100%;
  text-align: left;
  padding: 12px 16px;
  background: transparent;
  color: var(--text-muted);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
  margin-bottom: 6px;
  font-size: 0.95rem;
}
.tab-btn:hover {
  background: var(--hover-dropdowb);
  color: var(--text-header);
}
.tab-btn.active {
  background: var(--btn-plus);
  color: white;
  font-weight: 600;
}
.social-content {
  background: var(--bg-dropdown);
  border-radius: 16px;
  padding: 32px;
  min-height: 600px;
  border: 1px solid var(--border-color);
  box-shadow: 0 4px 12px var(--shadow-color);
}
.content-header h1 {
  margin: 0 0 32px 0;
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-header);
}
</style>
