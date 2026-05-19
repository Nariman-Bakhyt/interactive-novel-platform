<script setup lang="ts">
import AppHeader from '@/views/AppHeader.vue';
import {RouterView, useRoute} from 'vue-router';
import GlobalSidebar from "@/components/chat/GlobalSidebar.vue";
import QuoteTooltip from "@/components/QuoteTooltip.vue";
import {nextTick, watch} from "vue";
import {useSmartScroll} from "@/api/commentService.ts";

const route = useRoute();
const { scrollToTarget } = useSmartScroll();
watch(
  () => route.fullPath, 
  async () => {
    if (!route.query.q) {
      if (typeof CSS !== 'undefined' && CSS.highlights) {
        CSS.highlights.delete("search-results");
      }
      return;
    }

    
    if (typeof CSS !== 'undefined' && CSS.highlights) {
      CSS.highlights.delete("search-results");
    }

    await nextTick();
    await scrollToTarget();
  },
  { immediate: true }
);
</script>

<template>
  <div class="app-layout">
    
    <header class="app-header">
      <AppHeader />
    </header>

    
    <main class="main-container">
      <div class="router-view-container">
        <RouterView />
      </div>

      
      <GlobalSidebar />

      <QuoteTooltip />
    </main>
  </div>
</template>

<style scoped>

.app-layout {
  display: flex;
  flex-direction: column;
  height: 100vh; 
  overflow: hidden; 
  background-color: var(--bg-main);
}

.app-header {
  height: 60px;
  flex-shrink: 0; 
  background: var(--bg-header);
  border-bottom: 1px solid var(--border-color);
  z-index: 100; 
}

.main-container {
  display: flex;
  flex: 1; 
  overflow: hidden; 
}

.router-view-container {
  flex: 1; 
  overflow-y: auto; 
  position: relative;
  
  scroll-behavior: smooth;
}
</style>
