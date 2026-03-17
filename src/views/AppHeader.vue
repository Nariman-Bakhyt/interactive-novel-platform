<script setup lang="ts">
import { useAuthStore } from '@/api/auth.ts'; // Используем ваш Pinia Store
import { useRouter } from 'vue-router';
import AuthModal from "@/views/auth/AuthModal.vue";
import {computed, onMounted, ref} from "vue";
import {useThemeStore} from "@/api/theme.ts";


const showAuthModal = ref(false);
const router = useRouter();
const authStore = useAuthStore();
const showCreateMenu = ref(false);
const showDropdown = ref(false);
const themeStore = useThemeStore();
onMounted(() => {
  themeStore.applyTheme();
});
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
</script>

<template>
  <header class="main-header">
    <RouterLink to="/" class="logo">Novels Platform</RouterLink>

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
        <button class="login-button-corner" @click="showAuthModal = true">
          Войти
        </button>
      </div>
    </nav>
  </header>
  <Transition name="fade">
    <AuthModal v-if="showAuthModal" @close="showAuthModal = false" />
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
</style>
