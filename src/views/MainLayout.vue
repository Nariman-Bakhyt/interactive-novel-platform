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
  () => route.fullPath, // Следим за всем путем, включая все параметры
  async () => {
    if (!route.query.q) {
      if (typeof CSS !== 'undefined' && CSS.highlights) {
        CSS.highlights.delete("search-results");
      }
      return;
    }

    // Принудительно удаляем старую подсветку перед новым поиском
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
    <!-- Шапка всегда сверху, фиксирована по высоте -->
    <header class="app-header">
      <AppHeader />
    </header>

    <!-- Основная область под шапкой -->
    <main class="main-container">
      <div class="router-view-container">
        <RouterView />
      </div>

      <!-- Сайдбар теперь просто колонка справа -->
      <GlobalSidebar />

      <QuoteTooltip />
    </main>
  </div>
</template>

<style scoped>
/* Главный контейнер на весь экран */
.app-layout {
  display: flex;
  flex-direction: column;
  height: 100vh; /* Ровно высота окна */
  overflow: hidden; /* Запрещаем общий скролл страницы */
  background-color: var(--bg-main);
}

.app-header {
  height: 60px;
  flex-shrink: 0; /* Шапка не сжимается */
  background: var(--bg-header);
  border-bottom: 1px solid var(--border-color);
  z-index: 100; /* Чтобы тень падала поверх контента */
}

.main-container {
  display: flex;
  flex: 1; /* Занимает всё пространство от 60px до низа экрана */
  overflow: hidden; /* Скролл будет только внутри колонок */
}

.router-view-container {
  flex: 1; /* Основной контент (новелла) занимает максимум места */
  overflow-y: auto; /* Скролл только здесь */
  position: relative;
  /* Плавный скролл */
  scroll-behavior: smooth;
}
</style>
