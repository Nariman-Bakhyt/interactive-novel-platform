<script setup lang="ts">
  import {ref} from 'vue';
  import {useAuthStore} from "@/api/auth.ts";
  import {useRouter} from "vue-router";

  const emit = defineEmits(['close'])

  const authStore = useAuthStore();
  const router = useRouter();

  const activeTab = ref<'login' | 'register'>('login');

  const username = ref('');
  const password = ref('');
  const email = ref('');
  const successMessage = ref('');
  const errorMessage = ref('');

  const handleLogin = async () => {
    try {
      await authStore.login({ username: username.value, password: password.value });
      emit('close');
    }
    catch(err) {
      alert(err);
    }
  }

  const handleRegister = async () => {
    successMessage.value = '';
    errorMessage.value = '';
    try{
      await authStore.register({ username: username.value, password: password.value ,email: email.value });
      successMessage.value = 'Аккаунт успешно зарегистрирован! Теперь вы можете войти.';
    }
    catch(err) {
      if (err.response && err.response.data && err.response.data.message) {
        // Если это ошибка Axios с сообщением от сервера
        errorMessage.value = err.response.data.message;
      } else if (err.message) {
        // Если это общая ошибка (например, Network Error)
        errorMessage.value = `Произошла ошибка: ${err.message}`;
      } else {
        errorMessage.value = 'Неизвестная ошибка при регистрации.';
      }
    }
  };
</script>

<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-window">
      <div class="tabs">
        <button
          :class="{active: activeTab === 'login'}"
          @click="activeTab = 'login'"
        >
          Вход
        </button>
        <button
          :class="{ active: activeTab === 'register' }"
          @click="activeTab = 'register'"
        >
          Регистрация
        </button>
      </div>
      <div v-if="activeTab === 'login'" class="form-content">
        <form @submit.prevent="handleLogin">
          <input v-model="username" type="text" placeholder="Логин" class="btn-width" required />
          <input v-model="password" type="password" placeholder="Пароль" class="btn-width" required />
          <button type="submit" class="submit-btn ">Войти</button>
        </form>
      </div>
      <div v-else class="form-content">
        <div v-if="successMessage" class="message success">
          {{ successMessage }}
        </div>

        <div v-if="errorMessage" class="message error">
          {{ errorMessage }}
        </div>

        <form @submit.prevent="handleRegister">
          <input v-model="username" type="text" placeholder="Придумайте логин" class="btn-width" required />
          <input v-model="email" type="email" placeholder="Email" class="btn-width" required />
          <input v-model="password" type="password" placeholder="Придумайте пароль" class="btn-width" required />
          <button type="submit" class="submit-btn">Зарегистрироваться</button>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  top: 0; left: 0;
  width: 100%; height: 100%;
  background: rgba(0, 0, 0, 0.6); /* Темный фон */
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-window {
  background: var(--bg-header);
  padding: 2rem;
  border-radius: 12px;
  width: 350px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.3);
}

.tabs {
  display: flex;
  margin-bottom: 20px;
  border-bottom: 1px solid #ddd;
}

.tabs button {
  flex: 1;
  padding: 10px;
  border: none;
  background: none;
  cursor: pointer;
  font-weight: bold;
  color: var(--text-header);
}

.tabs button.active {
  color: #42b883;
  border-bottom: 2px solid #42b883;
}

input {
  width: 100%;
  margin-bottom: 10px;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.submit-btn {
  width: 100%;
  margin-top: 5px;
  padding: 10px;
  background-color: #42b883;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
}
.btn-width{
  width: 100%;
  box-sizing: border-box;
  margin: 0 0 10px 0;
  display: block;
}
</style>
