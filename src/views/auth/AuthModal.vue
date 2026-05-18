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
      <div class="modal-header-actions">
         <button class="close-btn" @click="$emit('close')">&times;</button>
      </div>
      <div class="tabs">
        <button :class="{active: activeTab === 'login'}" @click="activeTab = 'login'">
          Вход
        </button>
        <button :class="{ active: activeTab === 'register' }" @click="activeTab = 'register'">
          Регистрация
        </button>
      </div>

      <Transition name="fade">
        <div v-if="successMessage" class="message success">{{ successMessage }}</div>
      </Transition>
      <Transition name="fade">
        <div v-if="errorMessage" class="message error">{{ errorMessage }}</div>
      </Transition>

      <div v-if="activeTab === 'login'" class="form-content">

        <form v-if="loginStep === 'DEFAULT'" @submit.prevent="handleLogin">
          <input v-model="username" type="text" placeholder="Логин или Email" class="btn-width" required />
          <input v-model="password" type="password" placeholder="Пароль" class="btn-width" required />
          <button type="submit" class="submit-btn" :disabled="isLoading">
             {{ isLoading ? 'Вход...' : 'Войти' }}
          </button>

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
  z-index: 2000;
  backdrop-filter: blur(8px);
}

.modal-window {
  background: var(--bg-dropdown); /* Твоя CSS переменная */
  padding: 32px;
  border-radius: 16px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 20px 50px rgba(0,0,0,0.5);
  border: 1px solid var(--border-color);
  position: relative;
}

.modal-header-actions {
  position: absolute;
  top: 16px;
  right: 16px;
}
.close-btn {
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 1.5rem;
  cursor: pointer;
  line-height: 1;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}
.close-btn:hover {
  background: var(--hover-dropdowb);
  color: var(--text-header);
}

.tabs {
  display: flex; margin-bottom: 24px; border-bottom: 1px solid var(--border-color);
  gap: 16px;
}

.tabs button {
  flex: 1; padding: 12px; border: none; background: none;
  cursor: pointer; font-weight: 600; color: var(--text-muted); font-size: 1rem;
  transition: color 0.2s;
  position: relative;
}
.tabs button:hover {
  color: var(--text-header);
}

.tabs button.active {
  color: var(--btn-plus);
}
.tabs button.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 2px;
  background: var(--btn-plus);
  border-radius: 2px 2px 0 0;
}

input {
  width: 100%; margin-bottom: 16px; padding: 14px 16px;
  border: 1px solid var(--border-color); border-radius: 8px;
  background: var(--bg-main);
  color: var(--text-header);
  font-size: 0.95rem;
  transition: border-color 0.2s;
}
input:focus {
  outline: none;
  border-color: var(--btn-plus);
}
input::placeholder {
  color: var(--input-placeholder);
}

.code-input {
  text-align: center;
  letter-spacing: 0.5em;
  font-size: 1.25rem;
  font-weight: 700;
  padding: 16px;
}

.submit-btn {
  width: 100%; margin-top: 8px; padding: 14px;
  background-color: var(--btn-plus); color: white;
  border: none; border-radius: 8px; cursor: pointer; font-size: 1rem;
  font-weight: 600;
  transition: background 0.2s, transform 0.2s;
}

.submit-btn:hover:not(:disabled) {
  background-color: var(--btn-plus-hover);
  transform: translateY(-1px);
}

.submit-btn:disabled {
  opacity: 0.6; cursor: not-allowed;
}

.btn-width {
  box-sizing: border-box; display: block;
}

.link-container {
  margin-top: 20px; text-align: center;
}

.text-link {
  color: var(--btn-plus); font-size: 0.9rem; cursor: pointer; font-weight: 500; transition: color 0.2s;
}
.text-link:hover {
  text-decoration: underline;
  color: var(--btn-plus-hover);
}

.message {
  padding: 12px; border-radius: 8px; margin-bottom: 20px; font-size: 0.9rem; text-align: center;
  font-weight: 500;
}

.success { background-color: rgba(16, 185, 129, 0.1); color: #10b981; border: 1px solid rgba(16, 185, 129, 0.2); }
.error { background-color: rgba(239, 68, 68, 0.1); color: #ef4444; border: 1px solid rgba(239, 68, 68, 0.2); }
.info-text { font-size: 0.95rem; color: var(--text-muted); margin-bottom: 20px; text-align: center; line-height: 1.5;}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
