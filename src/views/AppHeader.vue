<script setup lang="ts">
import {useAuthStore} from '@/api/auth.ts'; 
import {useRouter} from 'vue-router';

import {computed, inject, nextTick, onMounted, onUnmounted, ref, watch} from "vue";
import {useThemeStore} from "@/api/theme.ts";
import {searchNovels} from "@/api/novelService.ts";
import {searchUsers} from "@/api/profileService.ts";
import {DEFAULT_AVATAR, DEFAULT_COVER} from "@/utils/media.ts";
import AuthModal from "@/views/auth/AuthModal.vue";

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
      
      data = await searchNovels(searchQuery.value, page, 12);
    } else {
      data = await searchUsers(searchQuery.value, page, 12);
    }

    if (append) {
      searchResults.value = [...searchResults.value, ...data.content];
    } else {
      searchResults.value = data.content;
      
      await nextTick();
      if (searchResultsRef.value) {
        searchResultsRef.value.scrollTop = 0;
      }
    }

    
    const totalPages = data.page?.totalPages ||  1;
    searchIsLastPage.value = page >= totalPages - 1;

    searchPage.value = page;
  } catch (e) {
    console.error(e);
  } finally {
    isSearching.value = false;
  }
};



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



const handleLogout = () => {
  authStore.logout();
  showDropdown.value = false;
  router.push('/'); 
};
if (!authStore.userDetails ) {
  authStore.fetchUserDetails();
}


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
    return DEFAULT_AVATAR;
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
    <RouterLink to="/" class="logo">WénLib</RouterLink>
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
                :src="res.coverUrl || res.avatarUrl || (searchType === 'novels' ? DEFAULT_COVER : DEFAULT_AVATAR)"
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
              <button @click="router.push('/library'); showDropdown = false;">Библиотека</button>
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
/* ── Theme toggle ── */
.theme-toggle {
  background: none;
  border: none;
  font-size: 1.15rem;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: background var(--transition-base);
  display: flex;
  align-items: center;
  justify-content: center;
}
.theme-toggle:hover {
  background: var(--hover-dropdowb);
}

/* ── Menu wrapper ── */
.menu-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  height: 60px;
}

/* ── Logo — gradient text ── */
.logo {
  text-decoration: none;
  font-size: 1.3rem;
  font-weight: 800;
  letter-spacing: -0.03em;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 0 12px var(--primary-glow));
}

/* ── User status row ── */
.user-status {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
  height: 100%;
}

/* ── Plus button ── */
.plus-btn {
  background: var(--gradient-primary);
  color: #fff;
  border: none;
  width: 34px;
  height: 34px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px var(--primary-glow);
  transition: box-shadow var(--transition-base), transform var(--transition-base);
}
.plus-btn:hover {
  box-shadow: 0 6px 20px var(--primary-glow-lg);
  transform: translateY(-2px) scale(1.05);
}
.plus-icon {
  font-size: 18px;
  font-weight: 600;
  line-height: 1;
}

/* ── Dropdown menu — glassmorphism ── */
.dropdown-menu {
  position: absolute;
  top: 60px;
  background: var(--bg-dropdown);
  backdrop-filter: blur(24px) saturate(160%);
  border: 1px solid var(--surface-glass-border);
  border-radius: 12px;
  box-shadow: var(--shadow-elevated);
  z-index: 100;
  min-width: 200px;
  overflow: hidden;
  padding: 6px;
  animation: dropdownIn 0.18s var(--transition-base) both;
}
@keyframes dropdownIn {
  from { opacity: 0; transform: translateY(-6px) scale(0.97); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}
.create-menu  { left: auto; right: 0; }
.profile-menu { right: 0; left: auto; }

.dropdown-menu button {
  width: 100%;
  padding: 9px 14px;
  background: none;
  border: none;
  color: var(--text-header);
  text-align: left;
  cursor: pointer;
  font-size: 0.9rem;
  border-radius: 8px;
  transition: background var(--transition-base), color var(--transition-base);
}
.dropdown-menu button:hover {
  background: var(--hover-dropdowb);
}
.separator {
  height: 1px;
  background: var(--border-color);
  margin: 4px 0;
  opacity: 0.6;
}

/* ── Main header — backdrop-blur ── */
.main-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  padding: 0 28px;
  height: 60px;
  z-index: 1000;
  display: flex;
  justify-content: space-between;
  align-items: center;
  /* glass effect */
  background: rgba(24, 24, 27, 0.82);
  backdrop-filter: blur(18px) saturate(180%);
  -webkit-backdrop-filter: blur(18px) saturate(180%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  color: var(--text-header);
  /* subtle bottom glow line */
  box-shadow: 0 1px 0 rgba(99, 102, 241, 0.12), 0 4px 16px rgba(0,0,0,0.3);
}
[data-theme="light"] .main-header {
  background: rgba(255, 255, 255, 0.88);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 0 rgba(79, 70, 229, 0.08), 0 4px 16px rgba(0,0,0,0.06);
}

/* ── Dropdown trigger ── */
.dropdown-trigger {
  border-radius: 24px;
  padding: 4px 10px 4px 4px;
  transition: background var(--transition-base), border-color var(--transition-base);
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
  font-size: 0.88rem;
  padding-left: 4px;
}

/* ── Avatar with indigo ring ── */
.user-avatar-lg {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid transparent;
  box-shadow: 0 0 0 2px var(--btn-plus), 0 0 8px var(--primary-glow);
  transition: box-shadow var(--transition-base);
}
.dropdown-trigger:hover .user-avatar-lg {
  box-shadow: 0 0 0 2px #818cf8, 0 0 14px var(--primary-glow-lg);
}

/* ── Login button ── */
.login-button-corner {
  padding: 8px 18px;
  background: var(--gradient-primary);
  color: white;
  border: none;
  border-radius: 9px;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.9rem;
  box-shadow: 0 4px 12px var(--primary-glow);
  transition: box-shadow var(--transition-base), transform var(--transition-base);
}
.login-button-corner:hover {
  box-shadow: 0 6px 20px var(--primary-glow-lg);
  transform: translateY(-1px);
}

/* ── Header right ── */
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* ── Search ── */
.search-container {
  display: flex;
  align-items: center;
  transition: all var(--transition-slow);
}
.search-trigger {
  background: none;
  border: none;
  font-size: 1.1rem;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background var(--transition-base);
}
.search-trigger:hover {
  background: var(--hover-dropdowb);
}

/* Search bar — glassmorphism */
.search-bar-wrapper {
  display: flex;
  align-items: center;
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 4px 10px;
  width: 400px;
  position: relative;
  animation: slideIn 0.25s var(--transition-base);
  transition: border-color var(--transition-base), box-shadow var(--transition-base);
}
.search-bar-wrapper:focus-within {
  border-color: var(--btn-plus);
  box-shadow: 0 0 0 3px var(--primary-glow);
}
@keyframes slideIn {
  from { width: 0; opacity: 0; }
  to   { width: 400px; opacity: 1; }
}
.search-input {
  background: none;
  border: none;
  color: var(--text-header);
  padding: 6px 8px;
  width: 100%;
  outline: none;
  font-size: 0.9rem;
  font-family: var(--main-font);
}
.search-input::placeholder {
  color: var(--input-placeholder);
}
.search-type-select {
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 0.82rem;
  cursor: pointer;
  outline: none;
  margin-right: 6px;
  font-weight: 500;
}

/* Search results dropdown — glassmorphism */
.search-results {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  background: var(--bg-dropdown);
  backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 12px;
  box-shadow: var(--shadow-elevated);
  max-height: 320px;
  overflow-y: auto;
  padding: 6px;
  animation: dropdownIn 0.18s var(--transition-base) both;
}
.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 10px;
  cursor: pointer;
  transition: background var(--transition-base);
  border-radius: 8px;
}
.result-item:hover {
  background: var(--hover-dropdowb);
}
.res-thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
}
.res-info { display: flex; flex-direction: column; }
.res-name { font-size: 0.88rem; font-weight: 500; }
.res-sub  { font-size: 0.72rem; color: var(--text-muted); }
.search-loading {
  padding: 10px;
  text-align: center;
  color: var(--text-muted);
  font-size: 0.85rem;
}

/* Close search button */
.close-search {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 4px 6px;
  font-size: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: color var(--transition-base), background var(--transition-base);
}
.close-search:hover {
  color: var(--text-header);
  background: var(--hover-dropdowb);
}

.scrollbar {
  scrollbar-width: thin;
  scrollbar-color: rgba(99,102,241,0.4) transparent;
}
</style>


