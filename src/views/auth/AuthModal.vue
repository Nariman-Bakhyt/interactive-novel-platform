<script setup lang="ts">
import { ref, watch } from 'vue';
import { useAuthStore } from "@/api/auth.ts";
import axios from "axios";
import {VerificationTokenType} from "@/types/auth.ts";


const emit = defineEmits(['close']);
const authStore = useAuthStore();

// --- СОСТОЯНИЕ ВКЛАДОК И ШАГОВ ---
const activeTab = ref<'login' | 'register'>('login');
const loginStep = ref<'DEFAULT' | 'EMAIL_INPUT' | 'CODE_VERIFY'>('DEFAULT');
const registerStep = ref<'DEFAULT' | 'CODE_VERIFY'>('DEFAULT');

// --- ДАННЫЕ ФОРМЫ ---
const username = ref('');
const password = ref('');
const email = ref('');
const code = ref(''); // Поле для 6-значного кода
const pendingUserId = ref<number | null>(null); // ID для подтверждения регистрации

// --- UI СОСТОЯНИЯ ---
const successMessage = ref('');
const errorMessage = ref('');
const isLoading = ref(false);

// Очистка при переключении вкладок
watch(activeTab, () => {
  loginStep.value = 'DEFAULT';
  registerStep.value = 'DEFAULT';
  errorMessage.value = '';
  successMessage.value = '';
  code.value = '';
});

// ==========================================
// ЛОГИКА ВХОДА (LOGIN)
// ==========================================

// 1. Обычный вход по паролю
const handleLogin = async () => {
  errorMessage.value = '';
  isLoading.value = true;
  try {
    // В authStore.login передаем username (он может быть и email-ом на бэке) и пароль
    await authStore.login({ username: username.value, password: password.value });
    emit('close');
  } catch (err: any) {
    errorMessage.value = err.response?.data?.message || 'Неверный логин или пароль';
  } finally {
    isLoading.value = false;
  }
};

// 2. Запрос кода для входа по Email
const handleRequestLoginCode = async () => {
  errorMessage.value = '';
  isLoading.value = true;
  try {
    await authStore.requestLoginByEmail({ email: email.value });
    loginStep.value = 'CODE_VERIFY';
    successMessage.value = `Код отправлен на ${email.value}`;
  } catch (err: any) {
    errorMessage.value = err.response?.data?.message || 'Ошибка отправки кода';
  } finally {
    isLoading.value = false;
  }
};

// 3. Подтверждение входа по коду
const handleVerifyLoginCode = async () => {
  errorMessage.value = '';
  isLoading.value = true;
  try {
    await authStore.verifyLoginCode({ email: email.value, code: code.value });
    emit('close');
  } catch (err: any) {
    errorMessage.value = err.response?.data?.message || 'Неверный или просроченный код';
  } finally {
    isLoading.value = false;
  }
};

// ==========================================
// ЛОГИКА РЕГИСТРАЦИИ (REGISTER)
// ==========================================

// 1. Отправка данных регистрации
const handleRegister = async () => {
  successMessage.value = '';
  errorMessage.value = '';
  isLoading.value = true;

  try {
    const userId = await authStore.register({
      username: username.value,
      password: password.value,
      email: email.value
    });
    pendingUserId.value = userId; // Сохраняем ID, который вернул бэкенд
    registerStep.value = 'CODE_VERIFY'; // Переключаем на шаг ввода кода
    successMessage.value = 'Код подтверждения отправлен на почту!';
  } catch (err: any) {
    errorMessage.value = err.response?.data?.message || 'Ошибка при регистрации.';
  } finally {
    isLoading.value = false;
  }
};

// 2. Подтверждение регистрации кодом
const handleVerifyRegister = async () => {
  errorMessage.value = '';
  isLoading.value = true;
  try {
    await authStore.verifyCode({
      userId: pendingUserId.value,
      type: VerificationTokenType.REGISTRATION_CONFIRMATION,
      code: code.value
    });

    // После успешной верификации перекидываем на вкладку входа
    successMessage.value = 'Регистрация завершена! Теперь войдите в аккаунт.';
    activeTab.value = 'login';
    username.value = email.value; // Подставляем email для удобства
    password.value = '';
  } catch (err: any) {
    errorMessage.value = err.response?.data?.message || 'Неверный код';
  } finally {
    isLoading.value = false;
  }
};
</script>

<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-window">
      <div class="tabs">
        <button :class="{active: activeTab === 'login'}" @click="activeTab = 'login'">
          Вход
        </button>
        <button :class="{ active: activeTab === 'register' }" @click="activeTab = 'register'">
          Регистрация
        </button>
      </div>

      <div v-if="successMessage" class="message success">{{ successMessage }}</div>
      <div v-if="errorMessage" class="message error">{{ errorMessage }}</div>

      <div v-if="activeTab === 'login'" class="form-content">

        <form v-if="loginStep === 'DEFAULT'" @submit.prevent="handleLogin">
          <input v-model="username" type="text" placeholder="Логин или Email" class="btn-width" required />
          <input v-model="password" type="password" placeholder="Пароль" class="btn-width" required />
          <button type="submit" class="submit-btn" :disabled="isLoading">Войти</button>

          <div class="link-container">
            <span class="text-link" @click="loginStep = 'EMAIL_INPUT'">Войти по Email (без пароля)</span>
          </div>
        </form>

        <form v-else-if="loginStep === 'EMAIL_INPUT'" @submit.prevent="handleRequestLoginCode">
          <input v-model="email" type="email" placeholder="Введите ваш Email" class="btn-width" required />
          <button type="submit" class="submit-btn" :disabled="isLoading">Получить код</button>
          <div class="link-container">
            <span class="text-link" @click="loginStep = 'DEFAULT'">Назад к паролю</span>
          </div>
        </form>

        <form v-else-if="loginStep === 'CODE_VERIFY'" @submit.prevent="handleVerifyLoginCode">
          <input v-model="code" type="text" placeholder="6-значный код" class="btn-width code-input" maxlength="6" required />
          <button type="submit" class="submit-btn" :disabled="isLoading">Подтвердить и войти</button>
          <div class="link-container">
            <span class="text-link" @click="loginStep = 'EMAIL_INPUT'">Изменить email</span>
          </div>
        </form>
      </div>

      <div v-else class="form-content">

        <form v-if="registerStep === 'DEFAULT'" @submit.prevent="handleRegister">
          <input v-model="username" type="text" placeholder="Придумайте логин" class="btn-width" required />
          <input v-model="email" type="email" placeholder="Email" class="btn-width" required />
          <input v-model="password" type="password" placeholder="Придумайте пароль" class="btn-width" required />
          <button type="submit" class="submit-btn" :disabled="isLoading">Зарегистрироваться</button>
        </form>

        <form v-else-if="registerStep === 'CODE_VERIFY'" @submit.prevent="handleVerifyRegister">
          <p class="info-text">Мы отправили код на {{ email }}. Введите его для завершения регистрации.</p>
          <input v-model="code" type="text" placeholder="6-значный код" class="btn-width code-input" maxlength="6" required />
          <button type="submit" class="submit-btn" :disabled="isLoading">Завершить регистрацию</button>
        </form>

      </div>
    </div>
  </div>
</template>

<style scoped>

.modal-backdrop {
  position: fixed; top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0, 0, 0, 0.6);
  display: flex; justify-content: center; align-items: center;
  z-index: 1000;
}

.modal-window {
  background: var(--bg-header); /* Твоя CSS переменная */
  padding: 2rem;
  border-radius: 12px;
  width: 350px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.3);
}

.tabs {
  display: flex; margin-bottom: 20px; border-bottom: 1px solid #ddd;
}

.tabs button {
  flex: 1; padding: 10px; border: none; background: none;
  cursor: pointer; font-weight: bold; color: #888; /* Тусклый цвет для неактивных */
}

.tabs button.active {
  color: #42b883; border-bottom: 2px solid #42b883;
}

input {
  width: 100%; margin-bottom: 10px; padding: 10px;
  border: 1px solid #ccc; border-radius: 4px;
}

.code-input {
  text-align: center;
  letter-spacing: 5px;
  font-size: 1.2rem;
  font-weight: bold;
}

.submit-btn {
  width: 100%; margin-top: 5px; padding: 10px;
  background-color: #42b883; color: white;
  border: none; border-radius: 4px; cursor: pointer; font-size: 1rem;
}

.submit-btn:disabled {
  background-color: #a0d8c1; cursor: not-allowed;
}

.btn-width {
  box-sizing: border-box; display: block;
}

.link-container {
  margin-top: 15px; text-align: center;
}

.text-link {
  color: #42b883; font-size: 0.9rem; cursor: pointer; text-decoration: underline;
}

.message {
  padding: 10px; border-radius: 4px; margin-bottom: 15px; font-size: 0.9rem; text-align: center;
}

.success { background-color: #e6f4ea; color: #1e8e3e; }
.error { background-color: #fce8e6; color: #d93025; }
.info-text { font-size: 0.9rem; color: #555; margin-bottom: 15px; text-align: center;}
</style>
