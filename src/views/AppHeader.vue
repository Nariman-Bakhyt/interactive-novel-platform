<script setup lang="ts">
import { useAuthStore } from '@/api/auth.ts'; // Используем ваш Pinia Store
import { useRouter } from 'vue-router';
import AuthModal from "@/views/auth/AuthModal.vue";
import {computed, inject, onMounted, ref, watch} from "vue";
import {useThemeStore} from "@/api/theme.ts";
import {searchNovels} from "@/api/novelService.ts";
import {searchUsers} from "@/api/profileService.ts";

const router = useRouter();
const authStore = useAuthStore();
const showCreateMenu = ref(false);
const showDropdown = ref(false);
const themeStore = useThemeStore();

onMounted(() => {
  themeStore.applyTheme();
});

const isSearchOpen = ref(false);
const searchQuery = ref('');
const searchType = ref<'novels' | 'users'>('novels');
const searchResults = ref<any[]>([]);
const isSearching = ref(false);

let searchTimeout: number;


const openUserMenu = inject('openUserMenu') as (event: MouseEvent, userId: number, username: string) => void;

// Живой поиск
watch(searchQuery, (newQuery) => {
  clearTimeout(searchTimeout);
  if (newQuery.length < 2) {
    searchResults.value = [];
    return;
  }

  isSearching.value = true;
  searchTimeout = window.setTimeout(async () => {
    try {
      if (searchType.value === 'novels') {
        const data = await searchNovels(newQuery, 0, 5);
        searchResults.value = data.content;
      } else {
        const data = await searchUsers(newQuery, 0, 5);
        searchResults.value = data.content;
      }
    } catch (e) {
      console.error(e);
    } finally {
      isSearching.value = false;
    }
  }, 500);
});

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
      <div class="search-container" :class="{ 'is-open': isSearchOpen }">
        <button class="search-trigger" @click="isSearchOpen = !isSearchOpen" v-if="!isSearchOpen">
          🔍
        </button>

        <div v-if="isSearchOpen" class="search-bar-wrapper">
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

          <div v-if="searchResults.length > 0" class="search-results">
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
  font-size: 1.5rem;
  cursor: pointer;
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
  font-size: 1.5rem;
  font-weight: bold;
}
.user-status {
  display: flex;
  align-items: center;
  gap: 20px;
  position: relative;
  height: 100%;

}

.plus-btn{
  background: var(--btn-plus); /* Красивый синий цвет */
  color: var(--text-header);
  border: none;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s, background 0.2s;
}
.plus-btn:hover {
  background: var(--btn-plus);
  transform: scale(1.1);
}

.plus-icon {
  font-size: 20px;
  font-weight: bold;
  line-height: 1;
}
.dropdown-menu {
  position: absolute;
  top: 60px;
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.5);
  z-index: 100;
  min-width: 180px;
  overflow: hidden;
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
  padding: 10px 15px;
  background: none;
  border: none;
  color: var(--text-header);
  text-align: left;
  cursor: pointer;
  font-size: 14px;
}

.dropdown-menu button:hover {
  background: var(--hover-dropdowb);
}

.separator {
  height: 1px;
  background: var(--border-color);
  margin: 5px 0;
}

.main-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  padding: 0 30px;
  height: 60px;
  z-index: 1000;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: var(--bg-header);
  color: var(--text-header);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}


.dropdown-trigger {
  border-radius: 4px;
  transition: background 0.2s;
  cursor: pointer;
  outline: none;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}
.user-avatar-lg{
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #ccc;
}


.login-button-corner {
  padding: 10px 20px;
  background-color: #42b883;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
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
}

.search-bar-wrapper {
  display: flex;
  align-items: center;
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  padding: 2px 10px;
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
  padding: 8px;
  width: 100%;
  outline: none;
}

.search-type-select {
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 0.8rem;
  cursor: pointer;
  outline: none;
  margin-right: 5px;
}

.search-results {
  position: absolute;
  top: 45px;
  left: 0;
  right: 0;
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.3);
  max-height: 300px;
  overflow-y: auto;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  cursor: pointer;
  transition: background 0.2s;
}

.result-item:hover {
  background: var(--hover-dropdowb);
}

.res-thumb {
  width: 35px;
  height: 45px;
  object-fit: cover;
  border-radius: 4px;
}

.res-info {
  display: flex;
  flex-direction: column;
}

.res-name { font-size: 0.9rem; font-weight: 600; }
.res-sub { font-size: 0.75rem; color: var(--text-muted); }

.close-search {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 5px;
}
</style>
