<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'; // Используем ваш Pinia Store
import { useRouter } from 'vue-router';
import AuthModal from "@/views/AuthModal.vue";
import {ref} from "vue";

const showAuthModal = ref(false);
const router = useRouter();
const authStore = useAuthStore();

const showDropdown = ref(false);

// Функция для выхода
const handleLogout = () => {
  authStore.logout();
  showDropdown.value = false;
  router.push('/'); // Перенаправляем на главную страницу после выхода
};

// Функция для перехода в профиль
const goToProfile = () => {
  showDropdown.value = false;
  router.push('/profile');
};
const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value;
};
const closeDropdownWithDelay = () => {
  showDropdown.value = false;
};

</script>

<template>
  <header class="main-header">
    <RouterLink to="/" class="logo">Novels Platform</RouterLink>

    <nav>
      <div v-if="authStore.isAuthenticated" class="user-status">
        <div
          class="dropdown-trigger"
          @click="toggleDropdown"
          @blur="closeDropdownWithDelay"
        >
          {{ authStore.user }} 👋
        </div>
        <div v-if="showDropdown" class="dropdown-menu">
          <button @click="goToProfile">Профиль</button>
          <button @click="router.push('/settings'); showDropdown = false;">Настройки</button>
          <div class="separator"></div>
          <button @click="handleLogout" class="logout-item">Выход</button>
        </div>
      </div>

      <div v-else class="user-status">
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
.logo {
  color: white;
  text-decoration: none;
  font-size: 1.5rem;
  font-weight: bold;
}
.user-status {
  display: flex;
  align-items: center;
}


.login-button-corner {
  /* Стили кнопки */
  padding: 10px 20px;
  margin: 0px 40px 0px 0px;
  background-color: #42b883;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}


.main-header {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 30px;
  z-index: 1000;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 30px;
  background-color: #282828;
  color: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.logo {
  color: white;
  text-decoration: none;
  font-size: 1.5rem;
  font-weight: bold;
}

.user-status {
  position: relative;
  display: flex;
  align-items: center;

}

.dropdown-trigger {
  cursor: pointer;
  padding: 8px 20px;
  margin: 0 40px 0 0;
  border-radius: 4px;
  transition: background 0.2s;
  outline: none; /* Убираем стандартную синюю обводку */
}

.dropdown-menu {
  position: absolute;
  top: 100%; /* Помещаем меню сразу под триггером */
  right: 0;
  margin-top: 10px; /* Небольшой отступ */
  border-radius: 6px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  background-color: #282828;
  z-index: 1010; /* Убедимся, что меню сверху */
  min-width: 150px;
  display: flex;
  flex-direction: column;

}
.login-button-corner {
  /* Стили кнопки */
  padding: 10px 20px;
  margin: 0px 40px 0px 0px;
  background-color: #42b883;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}
</style>
