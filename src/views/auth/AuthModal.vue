<script setup lang="ts">
import {ref, watch} from 'vue';
import {useAuthStore} from "@/api/auth.ts";
import {VerificationTokenType} from "@/types/auth.ts";


const emit = defineEmits(['close']);
const authStore = useAuthStore();


const activeTab = ref<'login' | 'register'>('login');
const loginStep = ref<'DEFAULT' | 'EMAIL_INPUT' | 'CODE_VERIFY'>('DEFAULT');
const registerStep = ref<'DEFAULT' | 'CODE_VERIFY'>('DEFAULT');


const username = ref('');
const password = ref('');
const email = ref('');
const code = ref(''); 
const pendingUserId = ref<number | null>(null); 


const successMessage = ref('');
const errorMessage = ref('');
const isLoading = ref(false);


watch(activeTab, () => {
  loginStep.value = 'DEFAULT';
  registerStep.value = 'DEFAULT';
  errorMessage.value = '';
  successMessage.value = '';
  code.value = '';
});






const getErrorMessage = (err: any, fallback: string): string => {
  return err.response?.data?.detailedMessage ||
         err.response?.data?.message ||
         err.response?.data?.error ||
         (typeof err.response?.data === 'string' && err.response.data.trim() ? err.response.data : '') ||
         fallback;
};

const handleLogin = async () => {
  errorMessage.value = '';
  isLoading.value = true;
  try {
    await authStore.login({ username: username.value, password: password.value });
    emit('close');
  } catch (err: any) {
    errorMessage.value = getErrorMessage(err, 'Неверный логин или пароль');
  } finally {
    isLoading.value = false;
  }
};


const handleRequestLoginCode = async () => {
  errorMessage.value = '';
  isLoading.value = true;
  try {
    await authStore.requestLoginByEmail({ email: email.value });
    loginStep.value = 'CODE_VERIFY';
    successMessage.value = `Код отправлен на ${email.value}`;
  } catch (err: any) {
    errorMessage.value = getErrorMessage(err, 'Ошибка отправки кода');
  } finally {
    isLoading.value = false;
  }
};


const handleVerifyLoginCode = async () => {
  errorMessage.value = '';
  isLoading.value = true;
  try {
    await authStore.verifyLoginCode({ email: email.value, code: code.value });
    emit('close');
  } catch (err: any) {
    errorMessage.value = getErrorMessage(err, 'Неверный или просроченный код');
  } finally {
    isLoading.value = false;
  }
};


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
    pendingUserId.value = userId; 
    registerStep.value = 'CODE_VERIFY'; 
    successMessage.value = 'Код подтверждения отправлен на почту!';
  } catch (err: any) {
    errorMessage.value = getErrorMessage(err, 'Ошибка при регистрации.');
  } finally {
    isLoading.value = false;
  }
};


const handleVerifyRegister = async () => {
  errorMessage.value = '';
  isLoading.value = true;
  try {
    await authStore.verifyCode({
      userId: pendingUserId.value,
      type: VerificationTokenType.REGISTRATION_CONFIRMATION,
      code: code.value
    });

    successMessage.value = 'Регистрация завершена! Теперь войдите в аккаунт.';
    activeTab.value = 'login';
    username.value = email.value; 
    password.value = '';
  } catch (err: any) {
    errorMessage.value = getErrorMessage(err, 'Неверный код');
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
          <input v-model="username" type="text" placeholder="Придумайте логин (от 4 символов)" minlength="4" maxlength="50" class="btn-width" required />
          <input v-model="email" type="email" placeholder="Email" class="btn-width" required />
          <input v-model="password" type="password" placeholder="Придумайте пароль (от 8 символов)" minlength="8" class="btn-width" required />
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

/* ── Backdrop ── */
.modal-backdrop {
  position: fixed;
  top: 0; left: 0;
  width: 100%; height: 100%;
  background: rgba(0, 0, 0, 0.72);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
  backdrop-filter: blur(12px) saturate(120%);
  animation: backdropIn 0.2s ease both;
}
@keyframes backdropIn {
  from { opacity: 0; }
  to   { opacity: 1; }
}

/* ── Modal window — glassmorphism ── */
.modal-window {
  background: var(--bg-dropdown);
  backdrop-filter: blur(28px) saturate(160%);
  padding: 36px;
  border-radius: 20px;
  width: 100%;
  max-width: 420px;
  box-shadow: var(--shadow-elevated), 0 0 0 1px rgba(99,102,241,0.15);
  border: 1px solid var(--surface-glass-border);
  position: relative;
  animation: modalIn 0.25s cubic-bezier(0.4,0,0.2,1) both;
}
@keyframes modalIn {
  from { opacity: 0; transform: scale(0.95) translateY(8px); }
  to   { opacity: 1; transform: scale(1) translateY(0); }
}

/* ── Close button ── */
.modal-header-actions {
  position: absolute;
  top: 16px;
  right: 16px;
}
.close-btn {
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 1.4rem;
  cursor: pointer;
  line-height: 1;
  padding: 6px;
  border-radius: 8px;
  transition: background var(--transition-base), color var(--transition-base);
  display: flex;
  align-items: center;
  justify-content: center;
}
.close-btn:hover {
  background: var(--hover-dropdowb);
  color: var(--text-header);
}

/* ── Tabs ── */
.tabs {
  display: flex;
  margin-bottom: 28px;
  border-bottom: 1px solid var(--surface-glass-border);
  gap: 8px;
}
.tabs button {
  flex: 1;
  padding: 12px;
  border: none;
  background: none;
  cursor: pointer;
  font-weight: 600;
  color: var(--text-muted);
  font-size: 0.95rem;
  font-family: var(--main-font);
  transition: color var(--transition-base);
  position: relative;
}
.tabs button:hover { color: var(--text-header); }
.tabs button.active { color: #818cf8; }
.tabs button.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 2px;
  background: var(--gradient-primary);
  border-radius: 2px 2px 0 0;
}

/* ── Form inputs ── */
input {
  width: 100%;
  margin-bottom: 14px;
  padding: 13px 16px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.2);
  color: var(--text-header);
  font-size: 0.95rem;
  font-family: var(--main-font);
  transition: border-color var(--transition-base), box-shadow var(--transition-base);
  outline: none;
  box-sizing: border-box;
  display: block;
}
input:focus {
  border-color: var(--btn-plus);
  box-shadow: 0 0 0 3px var(--primary-glow);
}
input::placeholder { color: var(--input-placeholder); }

/* Code input */
.code-input {
  text-align: center;
  letter-spacing: 0.6em;
  font-size: 1.3rem;
  font-weight: 700;
  padding: 16px;
}

/* ── Submit button — gradient ── */
.submit-btn {
  width: 100%;
  margin-top: 6px;
  padding: 14px;
  background: var(--gradient-primary);
  color: white;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 700;
  font-family: var(--main-font);
  box-shadow: 0 4px 16px var(--primary-glow);
  transition: box-shadow var(--transition-base), transform var(--transition-base);
  box-sizing: border-box;
  display: block;
}
.submit-btn:hover:not(:disabled) {
  box-shadow: 0 8px 28px var(--primary-glow-lg);
  transform: translateY(-2px);
}
.submit-btn:active:not(:disabled) { transform: translateY(0); }
.submit-btn:disabled { opacity: 0.55; cursor: not-allowed; }

/* ── Links ── */
.link-container {
  margin-top: 18px;
  text-align: center;
}
.text-link {
  color: #818cf8;
  font-size: 0.88rem;
  cursor: pointer;
  font-weight: 500;
  transition: color var(--transition-base);
}
.text-link:hover {
  color: var(--btn-plus);
  text-decoration: underline;
}

/* ── Messages ── */
.message {
  padding: 12px 16px;
  border-radius: 10px;
  margin-bottom: 18px;
  font-size: 0.88rem;
  text-align: center;
  font-weight: 500;
  backdrop-filter: blur(6px);
}
.success {
  background-color: rgba(16, 185, 129, 0.1);
  color: #10b981;
  border: 1px solid rgba(16, 185, 129, 0.2);
}
.error {
  background-color: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.info-text {
  font-size: 0.9rem;
  color: var(--text-muted);
  margin-bottom: 18px;
  text-align: center;
  line-height: 1.55;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.btn-width { box-sizing: border-box; display: block; }
</style>

