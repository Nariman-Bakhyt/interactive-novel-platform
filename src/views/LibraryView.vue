<script setup lang="ts">
import { ref, onMounted, computed, watch, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/api/auth';
import { getUserLibrary } from '@/api/libraryService';
import { LibraryStatus, type UserLibraryResponseDto } from '@/types/library';
import NovelCard from '@/components/NovelCard.vue';

const authStore = useAuthStore();
const router = useRouter();

const libraryItems = ref<UserLibraryResponseDto[]>([]);
const isLoading = ref(true);
const activeTab = ref<LibraryStatus | 'ALL'>('ALL');
const currentPage = ref(0);
const totalPages = ref(1);

const isMobile = ref(window.innerWidth <= 768);
const handleResize = () => {
  isMobile.value = window.innerWidth <= 768;
};

const tabs: { value: LibraryStatus | 'ALL', label: string }[] = [
  { value: 'ALL', label: 'Все книги' },
  { value: LibraryStatus.READING, label: 'Читаю' },
  { value: LibraryStatus.PLANNING, label: 'В планах' },
  { value: LibraryStatus.COMPLETED, label: 'Прочитано' },
  { value: LibraryStatus.DROPPED, label: 'Брошено' },
];

const fetchLibrary = async (page = 0) => {
  if (!authStore.userDetails?.id) return;
  isLoading.value = true;
  try {
    const res = await getUserLibrary(authStore.userDetails.id, page, 20);
    libraryItems.value = res.content;
    totalPages.value = res.page?.totalPages || 1;
    currentPage.value = page;
  } catch (error) {
    console.error('Failed to fetch library', error);
  } finally {
    isLoading.value = false;
  }
};

const filteredItems = computed(() => {
  if (activeTab.value === 'ALL') return libraryItems.value;
  return libraryItems.value.filter(item => item.status === activeTab.value);
});

const currentTabLabel = computed(() => {
  return tabs.find(t => t.value === activeTab.value)?.label || 'Библиотека';
});

const getTabIcon = (status: LibraryStatus | 'ALL') => {
  switch(status) {
    case 'ALL': return 'library_books';
    case LibraryStatus.READING: return 'menu_book';
    case LibraryStatus.PLANNING: return 'pending_actions';
    case LibraryStatus.COMPLETED: return 'task_alt';
    case LibraryStatus.DROPPED: return 'cancel';
    default: return 'book';
  }
};

watch(() => authStore.userDetails, () => {
  if (authStore.userDetails?.id) {
    fetchLibrary(0);
  }
});

onMounted(() => {
  window.addEventListener('resize', handleResize);
  if (authStore.userDetails?.id) {
    fetchLibrary(0);
  }
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});

const goToNovel = (novelId: number) => {
  router.push(`/novel/${novelId}`);
};
</script>

<template>
  <div class="libris-layout">
    <!-- Desktop Sidebar -->
    <nav class="sidebar hidden-mobile">
      <div class="sidebar-header">
        <div class="avatar-box">
          <img :src="authStore.userDetails?.avatarUrl || '/default-avatar.png'" alt="User avatar">
        </div>
        <div class="header-text">
          <h2>Личная Библиотека</h2>
          <p>Всего книг: {{ libraryItems.length }}</p>
        </div>
      </div>

      <div class="sidebar-nav">
        <a
          v-for="tab in tabs"
          :key="tab.value"
          class="nav-item"
          :class="{ 'active': activeTab === tab.value }"
          @click.prevent="activeTab = tab.value"
          href="#"
        >
          <div v-if="activeTab === tab.value" class="active-indicator"></div>
          <span class="material-symbols-outlined nav-icon">{{ getTabIcon(tab.value) }}</span>
          <span class="nav-label">{{ tab.label }}</span>
        </a>
      </div>
    </nav>

    <!-- Main Content Area -->
    <main class="main-content">
      <!-- TopAppBar removed per user request -->

      <!-- Mobile Nav Row -->
      <nav class="mobile-nav visible-mobile">
        <a
          v-for="tab in tabs"
          :key="'mob-' + tab.value"
          class="mob-nav-item"
          :class="{ 'active': activeTab === tab.value }"
          @click.prevent="activeTab = tab.value"
          href="#"
        >
          {{ tab.label }}
        </a>
      </nav>

      <!-- Content Scroll Area -->
      <div class="content-scroll">
        <div v-if="isLoading" class="loading-state">
          <span class="spinner"></span> Загрузка библиотеки...
        </div>

        <div v-else-if="filteredItems.length === 0" class="empty-state">
          <span class="material-symbols-outlined empty-icon">menu_book</span>
          <h3>Здесь пока пусто</h3>
          <p>В этой категории нет новелл.</p>
          <button class="primary-btn" @click="router.push('/novels')">Перейти в каталог</button>
        </div>

        <div v-else class="book-grid">
          <NovelCard
            v-for="item in filteredItems"
            :key="item.novel.id"
            :novel="item.novel"
            :isMobile="isMobile"
            @click="goToNovel"
          />
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap');

/* Color Palette dynamically mapped to main.css */
:root {
  --lb-bg: var(--bg-main);
  --lb-surface: var(--bg-dropdown);
  --lb-surface-container: var(--bg-dropdown);
  --lb-surface-variant: var(--hover-dropdowb);
  --lb-outline-variant: var(--surface-glass-border);
  --lb-outline: var(--text-muted);
  
  --lb-primary: #818cf8;
  --lb-primary-dim: rgba(129, 140, 248, 0.15);
  --lb-secondary-container: var(--hover-dropdowb);
  --lb-on-secondary-container: var(--text-header);
  
  --lb-text-main: var(--text-header);
  --lb-text-muted: var(--text-muted);
  
  --lb-badge-bg: rgba(217, 119, 33, 0.15);
  --lb-badge-text: #d97721;
  --lb-badge-border: rgba(217, 119, 33, 0.3);
}

.libris-layout {
  display: flex;
  height: calc(100vh - 60px); /* Account for AppHeader */
  background-color: var(--lb-bg, #09090b);
  color: var(--lb-text-main, #e5e1e4);
  font-family: 'Be Vietnam Pro', var(--main-font), sans-serif;
  overflow: hidden;
}

/* ── Sidebar (Desktop) ── */
.sidebar {
  width: 256px;
  background-color: var(--lb-surface-container, #201f22);
  border-right: 1px solid var(--lb-outline-variant, #464554);
  display: flex;
  flex-direction: column;
  padding: 16px;
  gap: 16px;
  flex-shrink: 0;
  z-index: 10;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 8px;
  margin-bottom: 24px;
}
.avatar-box {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--lb-surface-variant, #353437);
  border: 1px solid var(--lb-outline-variant, #464554);
  overflow: hidden;
}
.avatar-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.header-text h2 {
  font-size: 1.1rem;
  font-weight: 900;
  color: var(--lb-primary, #c0c1ff);
  margin: 0;
  line-height: 1.2;
}
.header-text p {
  font-size: 0.85rem;
  color: var(--lb-text-muted, #c7c4d7);
  margin: 0;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 16px;
  border-radius: 8px;
  text-decoration: none;
  color: var(--lb-text-muted, #c7c4d7);
  transition: background-color 0.2s, color 0.2s;
  position: relative;
  overflow: hidden;
}
.nav-item:hover {
  background-color: var(--lb-surface-variant, #353437);
}
.nav-item:hover .nav-icon {
  color: var(--lb-primary, #c0c1ff);
}
.nav-item.active {
  background-color: var(--lb-secondary-container, #4b4b52);
  color: var(--lb-on-secondary-container, #bcbbc3);
  font-weight: 600;
}
.nav-item.active .nav-icon {
  color: var(--lb-primary, #c0c1ff);
  font-variation-settings: 'FILL' 1;
}
.active-indicator {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background-color: var(--lb-primary, #c0c1ff);
  border-radius: 4px 0 0 4px;
}
.nav-label {
  font-size: 1.1rem;
}

/* ── Main Content Area ── */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

/* ── Content Scroll Area & Grid ── */
.content-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  max-width: 1280px;
  width: 100%;
  margin: 0 auto;
}

.book-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 1.75rem;
}



/* ── Loading & Empty States ── */
.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 400px;
  color: var(--lb-text-muted);
}
.empty-icon {
  font-size: 4rem;
  margin-bottom: 16px;
  opacity: 0.5;
}
.empty-state h3 {
  font-size: 1.4rem;
  margin: 0 0 8px;
  color: var(--lb-text-main);
}
.empty-state p {
  margin: 0 0 24px;
}
.primary-btn {
  background-color: var(--lb-primary, #c0c1ff);
  color: #1000a9;
  border: none;
  padding: 10px 24px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}
.primary-btn:hover {
  opacity: 0.9;
}

/* ── Mobile Layout Overrides ── */
.visible-mobile { display: none; }

@media (max-width: 1024px) {
  .hidden-mobile { display: none !important; }
  .visible-mobile { display: flex; }

  .libris-layout {
    flex-direction: column;
  }

  .mobile-brand {
    font-size: 1.5rem;
    font-weight: 700;
    color: var(--lb-primary, #c0c1ff);
  }
  
  .mobile-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    overflow: hidden;
    border: 1px solid var(--lb-outline-variant, #464554);
    margin-left: 8px;
  }
  .mobile-avatar img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .mobile-nav {
    display: flex;
    overflow-x: auto;
    padding: 12px 16px;
    border-bottom: 1px solid var(--lb-outline-variant, #464554);
    background-color: #18181b;
    gap: 8px;
  }
  .mobile-nav::-webkit-scrollbar {
    display: none;
  }
  .mob-nav-item {
    padding: 8px 16px;
    border-radius: 999px;
    font-size: 0.85rem;
    font-weight: 600;
    white-space: nowrap;
    text-decoration: none;
    color: var(--lb-text-muted, #c7c4d7);
    border: 1px solid transparent;
  }
  .mob-nav-item.active {
    background-color: #3f3f46;
    color: var(--lb-primary, #c0c1ff);
    border-color: var(--lb-outline-variant, #464554);
  }

  .content-scroll {
    padding: 16px;
  }
  .book-grid {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 1.25rem;
  }
}
</style>

