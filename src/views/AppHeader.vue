<script setup lang="ts">
import { useAuthStore } from '@/api/auth.ts'; // Используем ваш Pinia Store
import { useRouter } from 'vue-router';
import AuthModal from "@/views/auth/AuthModal.vue";
import {computed, inject, onMounted, ref, watch, onUnmounted, nextTick} from "vue";
import {useThemeStore} from "@/api/theme.ts";
import {searchNovels} from "@/api/novelService.ts";
import {searchUsers} from "@/api/profileService.ts";

const router = useRouter();
const authStore = useAuthStore();
const showCreateMenu = ref(false);
const showDropdown = ref(false);
const themeStore = useThemeStore();

const searchContainerRef = ref<HTMLElement | null>(null);
const searchResultsRef = ref<HTMLElement | null>(null);

onMounted(() => {
  themeStore.applyTheme();
  document.addEventListener('click', handleClickOutside);
  document.addEventListener('keydown', handleEscKey);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
  document.removeEventListener('keydown', handleEscKey);
});

const handleClickOutside = (event: MouseEvent) => {
  if (isSearchOpen.value && searchContainerRef.value && !searchContainerRef.value.contains(event.target as Node)) {
    closeSearch();
  }
};

const handleEscKey = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && isSearchOpen.value) {
    closeSearch();
  }
};


const isSearchOpen = ref(false);
const searchQuery = ref('');
const searchType = ref<'novels' | 'users'>('novels');
const searchResults = ref<any[]>([]);
const isSearching = ref(false);

const searchPage = ref(0);
const searchIsLastPage = ref(false);

let searchTimeout: number;


const openUserMenu = inject('openUserMenu') as (event: MouseEvent, userId: number, username: string) => void;

const fetchSearchResults = async (page: number, append = false) => {
  if (searchQuery.value.length < 2) return;

  isSearching.value = true;
  try {
    let data;
    if (searchType.value === 'novels') {
      // УВЕЛИЧИЛИ ДО 12, чтобы контейнер в 300px точно переполнялся и появлялся скролл
      data = await searchNovels(searchQuery.value, page, 12);
    } else {
      data = await searchUsers(searchQuery.value, page, 12);
    }

    if (append) {
      searchResults.value = [...searchResults.value, ...data.content];
    } else {
      searchResults.value = data.content;
      // При новом поиске прокручиваем наверх
      await nextTick();
      if (searchResultsRef.value) {
        searchResultsRef.value.scrollTop = 0;
      }
    }

    // БЕЗОПАСНАЯ ПРОВЕРКА (как в Catalog.vue)
    const totalPages = data.page?.totalPages ||  1;
    searchIsLastPage.value = page >= totalPages - 1;

    searchPage.value = page;
  } catch (e) {
    console.error(e);
  } finally {
    isSearching.value = false;
  }
};


// Живой поиск
watch(searchQuery, (newQuery) => {
  clearTimeout(searchTimeout);
  if (newQuery.length < 2) {
    searchResults.value = [];
    return;
  }

  searchTimeout = window.setTimeout(() => {
    fetchSearchResults(0);
  }, 500);
});

watch(searchType, () => {
    if(searchQuery.value.length >= 2){
        fetchSearchResults(0);
    } else {
        searchResults.value = [];
    }
})

const handleSearchScroll = async (e: Event) => {
  const target = e.target as HTMLElement;
  // Скролл может быть не до самого пикселя из-за масштабирования, поэтому используем небольшую погрешность
  const bottom = Math.abs(target.scrollHeight - target.scrollTop - target.clientHeight) < 20;

  if (bottom && !isSearching.value && !searchIsLastPage.value) {
    await fetchSearchResults(searchPage.value + 1, true);
  }
};


const closeSearch = () => {
  isSearchOpen.value = false;
  searchQuery.value = '';
  searchResults.value = [];
};

const handleResultClick = (id: number) => {
  const path = searchType.value === 'novels' ? `/novel/${id}` : `/profile/${id}`;
  router.push(path);
  closeSearch();
};


// Функция для выхода
const handleLogout = () => {
  authStore.logout();
  showDropdown.value = false;
  router.push('/'); // Перенаправляем на главную страницу после выхода
};
if (!authStore.userDetails ) {
  authStore.fetchUserDetails();
}

// Функция для перехода в профиль
const goToProfile = () => {
  showDropdown.value = false;
  router.push('/profile');
};
const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value;
  if(showCreateMenu.value) showCreateMenu.value = false;
};
const toggleCreateMenu = () => {
  showCreateMenu.value = !showCreateMenu.value;
  if (showCreateMenu.value) showDropdown.value = false;
}
const closeCreateMenuWithDelay = () => {
  setTimeout(() => {
    showCreateMenu.value = false;
  }, 200);
};
const closeDropdownWithDelay = () => {
  setTimeout(() => {
    showDropdown.value = false;
  }, 200);
};
const avatarDisplayUrl = computed(() => {
  const url = authStore.userDetails?.avatarUrl;
  if (!url) {
    return 'http://127.0.0.1:9000/interactive-novel-assets/avatars/default-avatar.png';
  }
  return `${url}?t=${authStore.avatarTimestamp}`;
});

const menu = (event: MouseEvent,res:any) => {
  if(res.username){
    openUserMenu(event, res.id, res.username)
  }
}

</script>

<template>
  <header class="main-header">
    <RouterLink to="/" class="logo">Novels Platform</RouterLink>
    <div class="header-right">
      <div class="search-container" :class="{ 'is-open': isSearchOpen }" ref="searchContainerRef">
        <button class="search-trigger" @click.stop="isSearchOpen = true" v-if="!isSearchOpen">
          <span class="icon">🔍</span>
        </button>
        <div v-if="isSearchOpen" class="search-bar-wrapper" @click.stop>
          <select v-model="searchType" class="search-type-select">
            <option value="novels">📖 Новеллы</option>
            <option value="users">👤 Люди</option>
          </select>
          <input
            v-model="searchQuery"
            type="text"
            :placeholder="searchType === 'novels' ? 'Найти новеллу...' : 'Найти пользователя...'"
            class="search-input"
            autoFocus
          />
          <button class="close-search" @click="closeSearch">✕</button>

          <div v-if="searchResults.length > 0" class="search-results scrollbar" ref="searchResultsRef" @scroll="handleSearchScroll">
            <div
              v-for="res in searchResults"
              :key="res.id"
              class="result-item"
              @click="handleResultClick(res.id)"
              @contextmenu.prevent="menu($event,res)"
            >
              <img
                :src="res.coverUrl || res.avatarUrl || (searchType === 'novels'
                ? 'http://127.0.0.1:9000/interactive-novel-assets/covers/default-cover.png'
                : 'http://127.0.0.1:9000/interactive-novel-assets/avatars/default-avatar.png')"
                class="res-thumb"
              >
              <div class="res-info">
                <span class="res-name">{{ res.title || res.username }}</span>
                <span class="res-sub">{{ searchType === 'novels' ? res.status : 'Пользователь' }}</span>
              </div>
            </div>
            <div v-if="isSearching" class="search-loading">Загрузка...</div>
          </div>
        </div>
      </div>
      <nav>
        <div v-if="authStore.isAuthenticated" class="user-status">

          <div class="menu-wrapper">
            <button
              class="icon-button plus-btn"
              @click="toggleCreateMenu"
              @blur="closeCreateMenuWithDelay"
            >
              <span class="plus-icon">+</span>
            </button>

            <div v-if="showCreateMenu" class="dropdown-menu create-menu">
              <button @click="router.push('/novels/create'); showCreateMenu = false;">
                ✨ Создать новеллу
              </button>
              <button @click="router.push('/my-novels'); showCreateMenu = false;">
                📚 Мои новеллы
              </button>
            </div>
          </div>
          <nav>
            <button @click="themeStore.toggleTheme" class="theme-toggle">
              {{ themeStore.isDark ? '🌙' : '☀️' }}
            </button>
          </nav>
          <div class="menu-wrapper" >
            <div
              class="dropdown-trigger"
              tabindex="0"
              @click="toggleDropdown"
              @blur="closeDropdownWithDelay"
            >
              <span class="username">{{ authStore.user }}</span>
              <img :src="avatarDisplayUrl" alt="Аватар" class="user-avatar-lg">
            </div>

            <div v-if="showDropdown" class="dropdown-menu profile-menu">
              <button @click="goToProfile">Профиль</button>
              <button @click="router.push('/social'); showDropdown = false;">Сообщество</button>
              <button @click="router.push('/settings'); showDropdown = false;">Настройки</button>
              <div class="separator"></div>
              <button @click="handleLogout" class="logout-item">Выход</button>
            </div>
          </div>
        </div>

        <div v-else class="user-status">
          <nav>
            <button @click="themeStore.toggleTheme" class="theme-toggle">
              {{ themeStore.isDark ? '🌙' : '☀️' }}
            </button>
          </nav>
          <button class="login-button-corner" @click="authStore.showAuthModal = true">
            Войти
          </button>
        </div>
      </nav>
    </div>
  </header>
  <Transition name="fade">
    <AuthModal v-if="authStore.showAuthModal" @close="authStore.showAuthModal = false" />
  </Transition>
</template>

<style scoped>
.theme-toggle{
  background:none;
  border:none;
  font-size: 1.2rem;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: background 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}
.theme-toggle:hover {
  background: var(--hover-dropdowb);
}

.menu-wrapper{
  position: relative; /* Чтобы меню знало, где лево/право кнопки */
  display: flex;
  align-items: center;
  height: 60px;
}
.logo {
  color: var(--text-header);
  text-decoration: none;
  font-size: 1.25rem;
  font-weight: 800;
  letter-spacing: -0.02em;
}
.user-status {
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  height: 100%;

}

.plus-btn{
  background: var(--btn-plus); /* Красивый синий цвет */
  color: #fff;
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s, background 0.2s;
}
.plus-btn:hover {
  background: var(--btn-plus-hover);
  transform: translateY(-1px);
}

.plus-icon {
  font-size: 18px;
  font-weight: 600;
  line-height: 1;
}
.dropdown-menu {
  position: absolute;
  top: 60px;
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: 0 4px 12px var(--shadow-color);
  z-index: 100;
  min-width: 200px;
  overflow: hidden;
  padding: 4px;
}

.create-menu {
  left: auto;
  right: 0;
}
.profile-menu{
  right: 0;
  left: auto;
}

.dropdown-menu button {
  width: 100%;
  padding: 8px 12px;
  background: none;
  border: none;
  color: var(--text-header);
  text-align: left;
  cursor: pointer;
  font-size: 14px;
  border-radius: 6px;
  transition: background 0.2s, color 0.2s;
}

.dropdown-menu button:hover {
  background: var(--hover-dropdowb);
}

.separator {
  height: 1px;
  background: var(--border-color);
  margin: 4px 0;
}

.main-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  padding: 0 24px;
  height: 60px;
  z-index: 1000;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: var(--bg-header);
  color: var(--text-header);
  border-bottom: 1px solid var(--border-color);
}


.dropdown-trigger {
  border-radius: 20px;
  padding: 4px 12px 4px 4px;
  transition: background 0.2s;
  cursor: pointer;
  outline: none;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid transparent;
}
.dropdown-trigger:hover {
  background: var(--hover-dropdowb);
  border-color: var(--border-color);
}
.username {
  font-weight: 500;
  font-size: 0.9rem;
  padding-left: 4px;
}

.user-avatar-lg{
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--border-color);
}


.login-button-corner {
  padding: 8px 16px;
  background-color: var(--btn-plus);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  transition: background 0.2s;
}
.login-button-corner:hover {
  background-color: var(--btn-plus-hover);
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-container {
  display: flex;
  align-items: center;
  transition: all 0.3s ease;
}

.search-trigger {
  background: none;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}
.search-trigger:hover {
  background: var(--hover-dropdowb);
}

.search-bar-wrapper {
  display: flex;
  align-items: center;
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 4px 12px;
  width: 400px; /* Ширина развернутого поиска */
  position: relative;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from { width: 0; opacity: 0; }
  to { width: 400px; opacity: 1; }
}

.search-input {
  background: none;
  border: none;
  color: var(--text-header);
  padding: 6px 8px;
  width: 100%;
  outline: none;
  font-size: 0.9rem;
}
.search-input::placeholder {
  color: var(--input-placeholder);
}

.search-type-select {
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 0.85rem;
  cursor: pointer;
  outline: none;
  margin-right: 8px;
  font-weight: 500;
}

.search-results {
  position: absolute;
  top: 100%;
  margin-top: 8px;
  left: 0;
  right: 0;
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: 0 4px 12px var(--shadow-color);
  max-height: 300px;
  overflow-y: auto;
  padding: 4px;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  cursor: pointer;
  transition: background 0.2s;
  border-radius: 6px;
}

.result-item:hover {
  background: var(--hover-dropdowb);
}

.res-thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
}

.res-info {
  display: flex;
  flex-direction: column;
}

.res-name { font-size: 0.9rem; font-weight: 500; }
.res-sub { font-size: 0.75rem; color: var(--text-muted); }

.search-loading {
  padding: 8px;
  text-align: center;
  color: var(--text-muted);
  font-size: 0.85rem;
}

.close-search {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 4px;
  font-size: 1.1rem;
  display: flex;
  align-items: center;
  justify-content: center;
}
.close-search:hover {
  color: var(--text-header);
}

.scrollbar {
  scrollbar-width: thin;
  scrollbar-color: var(--border-color) transparent;
}
</style>
