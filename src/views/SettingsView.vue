<script setup lang="ts">
import {onMounted, reactive, ref, computed} from 'vue';
import {useAuthStore} from '@/api/auth';
import {PrivacyLevel} from '@/types/user';
import {changePasswordApi} from '@/api/profileService';
import {VerificationTokenType} from '@/types/auth';

const authStore = useAuthStore();

const activeTab = ref('privacy');

const form = reactive({
  canSendMessage: PrivacyLevel.NOBODY,
  libraryPrivacy: PrivacyLevel.NOBODY,
});

const isSaving = ref(false);
const saveMessage = ref({ text: '', type: '' });

// Password Change
const oldPassword = ref('');
const newPassword = ref('');
const confirmPassword = ref('');
const passwordError = ref('');
const passwordSuccess = ref('');
const isChangingPassword = ref(false);

const handlePasswordChange = async () => {
  passwordError.value = '';
  passwordSuccess.value = '';

  if (!oldPassword.value || !newPassword.value || !confirmPassword.value) {
    passwordError.value = 'Заполните все поля';
    return;
  }
  if (newPassword.value !== confirmPassword.value) {
    passwordError.value = 'Пароли не совпадают';
    return;
  }

  isChangingPassword.value = true;
  try {
    await changePasswordApi({
      oldPassword: oldPassword.value,
      newPassword: newPassword.value
    });
    passwordSuccess.value = 'Пароль успешно изменен!';
    oldPassword.value = '';
    newPassword.value = '';
    confirmPassword.value = '';
  } catch (err: any) {
    passwordError.value = err.response?.data?.message || 'Неверный текущий пароль или ошибка сервера';
  } finally {
    isChangingPassword.value = false;
  }
};

// Reset Password via Email Code
const resetNewPassword = ref('');
const resetConfirmPassword = ref('');
const verificationCode = ref('');
const isResetCodeSent = ref(false);
const resetError = ref('');
const resetSuccess = ref('');
const isResetting = ref(false);

const handleRequestResetCode = async () => {
  resetError.value = '';
  resetSuccess.value = '';

  if (!resetNewPassword.value || !resetConfirmPassword.value) {
    resetError.value = 'Заполните поля нового пароля';
    return;
  }
  if (resetNewPassword.value !== resetConfirmPassword.value) {
    resetError.value = 'Пароли не совпадают';
    return;
  }

  const userId = authStore.userDetails?.id;
  if (!userId) {
    resetError.value = 'Не удалось определить пользователя. Попробуйте обновить страницу.';
    return;
  }

  isResetting.value = true;
  try {
    await authStore.requestPasswordReset({
      userId: userId,
      newPassword: resetNewPassword.value
    });
    isResetCodeSent.value = true;
    resetSuccess.value = 'Код подтверждения отправлен на вашу почту!';
  } catch (err: any) {
    resetError.value = err.response?.data?.message || 'Ошибка отправки кода подтверждения';
  } finally {
    isResetting.value = false;
  }
};

const handleVerifyResetCode = async () => {
  resetError.value = '';
  resetSuccess.value = '';

  if (!verificationCode.value) {
    resetError.value = 'Введите код подтверждения';
    return;
  }

  const userId = authStore.userDetails?.id;
  if (!userId) {
    resetError.value = 'Не удалось определить пользователя';
    return;
  }

  isResetting.value = true;
  try {
    await authStore.verifyCode({
      userId: userId,
      code: verificationCode.value,
      type: VerificationTokenType.PASSWORD_RESET
    });
    resetSuccess.value = 'Пароль успешно изменен!';
    resetNewPassword.value = '';
    resetConfirmPassword.value = '';
    verificationCode.value = '';
    isResetCodeSent.value = false;
  } catch (err: any) {
    resetError.value = err.response?.data?.message || 'Неверный или просроченный код';
  } finally {
    isResetting.value = false;
  }
};

// Email Update
const newEmail = ref('');
const emailVerificationCode = ref('');
const isEmailCodeSent = ref(false);
const emailError = ref('');
const emailSuccess = ref('');
const isEmailUpdating = ref(false);

const handleRequestEmailCode = async () => {
  emailError.value = '';
  emailSuccess.value = '';

  if (!newEmail.value) {
    emailError.value = 'Введите новый email';
    return;
  }

  isEmailUpdating.value = true;
  try {
    await authStore.requestEmailUpdate({
      email: newEmail.value
    });
    isEmailCodeSent.value = true;
    emailSuccess.value = 'Код подтверждения отправлен на новую почту!';
  } catch (err: any) {
    emailError.value = err.response?.data?.message || 'Ошибка при отправке кода';
  } finally {
    isEmailUpdating.value = false;
  }
};

const handleVerifyEmailCode = async () => {
  emailError.value = '';
  emailSuccess.value = '';

  if (!emailVerificationCode.value) {
    emailError.value = 'Введите код подтверждения';
    return;
  }

  const userId = authStore.userDetails?.id;
  if (!userId) {
    emailError.value = 'Не удалось определить пользователя';
    return;
  }

  isEmailUpdating.value = true;
  try {
    await authStore.verifyCode({
      userId: userId,
      code: emailVerificationCode.value,
      type: VerificationTokenType.EMAIL_CHANGE
    });
    emailSuccess.value = 'Email успешно обновлен!';
    await authStore.fetchUserDetails();
    newEmail.value = '';
    emailVerificationCode.value = '';
    isEmailCodeSent.value = false;
  } catch (err: any) {
    emailError.value = err.response?.data?.message || 'Неверный или просроченный код';
  } finally {
    isEmailUpdating.value = false;
  }
};

onMounted(async () => {
  if (!authStore.userDetails) {
    await authStore.fetchUserDetails();
  }
  await authStore.fetchUserSettings();
  if (authStore.userSettings) {
    syncFormWithStore();
  }
});

const syncFormWithStore = () => {
  if (authStore.userSettings) {
    form.canSendMessage = authStore.userSettings.canSendMessage;
    form.libraryPrivacy = authStore.userSettings.libraryPrivacy;
  }
};

const handleSave = async () => {
  isSaving.value = true;
  saveMessage.value = { text: '', type: '' };

  try {
    await authStore.updateUserSettings({
      canSendMessage: form.canSendMessage,
      libraryPrivacy: form.libraryPrivacy
    });

    saveMessage.value = { text: 'Настройки успешно сохранены!', type: 'success' };
  } catch {
    saveMessage.value = { text: 'Ошибка при сохранении настроек', type: 'error' };
  } finally {
    isSaving.value = false;
    setTimeout(() => { saveMessage.value.text = ''; }, 3000);
  }
};
</script>

<template>
  <div class="settings-container">
    <h1 class="settings-title">Настройки</h1>

    <div class="settings-layout">
      <aside class="settings-sidebar">
        <button
          :class="['tab-btn', { active: activeTab === 'account' }]"
          @click="activeTab = 'account'"
        >
          👤 Аккаунт
        </button>
        <button
          :class="['tab-btn', { active: activeTab === 'privacy' }]"
          @click="activeTab = 'privacy'"
        >
          🔒 Приватность
        </button>
        <button
          :class="['tab-btn', { active: activeTab === 'appearance' }]"
          @click="activeTab = 'appearance'"
        >
          🎨 Внешний вид
        </button>
      </aside>

      <main class="settings-content">
        <section v-if="activeTab === 'privacy'" class="settings-section">
          <h2>Настройки приватности</h2>
          <p class="section-desc">Управляйте тем, кто может видеть вашу активность и взаимодействовать с вами.</p>

          <div class="setting-item">
            <div class="setting-info">
              <label>Кто может писать вам сообщения?</label>
              <span>Ограничьте круг лиц, которые могут отправлять вам личные сообщения.</span>
            </div>
            <select v-model="form.canSendMessage" class="custom-select">
              <option :value="PrivacyLevel.EVERYONE">Все пользователи</option>
              <option :value="PrivacyLevel.FOLLOWERS">Мои подписчики</option>
              <option :value="PrivacyLevel.FRIENDS">Только друзья</option>
              <option :value="PrivacyLevel.BEST_FRIENDS">Близкие друзья</option>
              <option :value="PrivacyLevel.NOBODY">Никто</option>
            </select>
          </div>

          <div class="setting-item">
            <div class="setting-info">
              <label>Видимость библиотеки</label>
              <span>Кто может просматривать список ваших новелл?</span>
            </div>
            <select v-model="form.libraryPrivacy" class="custom-select">
              <option :value="PrivacyLevel.EVERYONE">Все пользователи</option>
              <option :value="PrivacyLevel.FOLLOWERS">Мои подписчики</option>
              <option :value="PrivacyLevel.FRIENDS">Только друзья</option>
              <option :value="PrivacyLevel.BEST_FRIENDS">Близкие друзья</option>
              <option :value="PrivacyLevel.NOBODY">Только я</option>
            </select>
          </div>
        </section>

        <section v-if="activeTab === 'account'" class="settings-section">
          <h2>Данные аккаунта</h2>
          <p class="section-desc">Управляйте безопасностью вашего аккаунта: меняйте пароль обычным способом или сбрасывайте его через почту.</p>

          <div class="account-grid">
            <div class="account-card">
              <div class="card-header-block">
                <h3>Обычная смена пароля</h3>
                <p class="card-desc">Требуется ввести ваш текущий пароль.</p>
              </div>

              <div class="card-body-block">
                <div class="form-group">
                  <label>Текущий пароль</label>
                  <input v-model="oldPassword" type="password" class="form-input" placeholder="Введите старый пароль" />
                </div>
                <div class="form-group">
                  <label>Новый пароль</label>
                  <input v-model="newPassword" type="password" class="form-input" placeholder="Минимум 6 символов" />
                </div>
                <div class="form-group">
                  <label>Подтвердите новый пароль</label>
                  <input v-model="confirmPassword" type="password" class="form-input" placeholder="Повторите пароль" />
                </div>

                <div v-if="passwordError" class="error-text">{{ passwordError }}</div>
                <div v-if="passwordSuccess" class="success-text">{{ passwordSuccess }}</div>
              </div>

              <div class="card-actions-block">
                <button @click="handlePasswordChange" :disabled="isChangingPassword" class="action-btn">
                  {{ isChangingPassword ? 'Изменение...' : 'Обновить пароль' }}
                </button>
              </div>
            </div>

            <div class="account-card">
              <div class="card-header-block">
                <h3>Сброс пароля по почте</h3>
                <p class="card-desc">Используйте, если забыли текущий пароль или вошли по коду.</p>
              </div>

              <div v-if="!isResetCodeSent" class="card-inner-flow">
                <div class="card-body-block">
                  <div class="form-group">
                    <label>Новый пароль</label>
                    <input v-model="resetNewPassword" type="password" class="form-input" placeholder="Минимум 6 символов" />
                  </div>
                  <div class="form-group">
                    <label>Подтвердите новый пароль</label>
                    <input v-model="resetConfirmPassword" type="password" class="form-input" placeholder="Повторите пароль" />
                  </div>

                  <div v-if="resetError" class="error-text">{{ resetError }}</div>
                  <div v-if="resetSuccess" class="success-text">{{ resetSuccess }}</div>
                </div>

                <div class="card-actions-block">
                  <button @click="handleRequestResetCode" :disabled="isResetting" class="action-btn">
                    {{ isResetting ? 'Отправка...' : 'Отправить код на почту' }}
                  </button>
                </div>
              </div>

              <div v-else class="card-inner-flow">
                <div class="card-body-block">
                  <div class="form-group">
                    <label>Код из письма</label>
                    <input v-model="verificationCode" type="text" class="form-input code-input" placeholder="000000" maxlength="6" />
                  </div>

                  <div v-if="resetError" class="error-text">{{ resetError }}</div>
                  <div v-if="resetSuccess" class="success-text">{{ resetSuccess }}</div>
                </div>

                <div class="card-actions-block">
                  <div class="btn-group">
                    <button @click="handleVerifyResetCode" :disabled="isResetting" class="action-btn">
                      {{ isResetting ? 'Проверка...' : 'Подтвердить код' }}
                    </button>
                    <button @click="isResetCodeSent = false" class="cancel-btn">
                      Назад
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div class="account-card">
              <div class="card-header-block">
                <h3>Смена Email</h3>
                <p class="card-desc">Текущий адрес: <strong>{{ authStore.userDetails?.email || 'не указан' }}</strong></p>
              </div>

              <div v-if="!isEmailCodeSent" class="card-inner-flow">
                <div class="card-body-block">
                  <div class="form-group">
                    <label>Новый email</label>
                    <input v-model="newEmail" type="email" class="form-input" placeholder="example@domain.com" />
                  </div>

                  <div v-if="emailError" class="error-text">{{ emailError }}</div>
                  <div v-if="emailSuccess" class="success-text">{{ emailSuccess }}</div>
                </div>

                <div class="card-actions-block">
                  <button @click="handleRequestEmailCode" :disabled="isEmailUpdating" class="action-btn">
                    {{ isEmailUpdating ? 'Отправка...' : 'Отправить код' }}
                  </button>
                </div>
              </div>

              <div v-else class="card-inner-flow">
                <div class="card-body-block">
                  <div class="form-group">
                    <label>Код подтверждения</label>
                    <input v-model="emailVerificationCode" type="text" class="form-input code-input" placeholder="000000" maxlength="6" />
                  </div>

                  <div v-if="emailError" class="error-text">{{ emailError }}</div>
                  <div v-if="emailSuccess" class="success-text">{{ emailSuccess }}</div>
                </div>

                <div class="card-actions-block">
                  <div class="btn-group">
                    <button @click="handleVerifyEmailCode" :disabled="isEmailUpdating" class="action-btn">
                      {{ isEmailUpdating ? 'Проверка...' : 'Подтвердить код' }}
                    </button>
                    <button @click="isEmailCodeSent = false" class="cancel-btn">
                      Назад
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section v-if="activeTab === 'appearance'" class="settings-section">
          <h2>Внешний вид</h2>
          <p>Раздел в разработке. Здесь будет кастомизация темы.</p>
        </section>

        <div class="settings-footer">
          <transition name="fade">
            <span v-if="saveMessage.text" :class="['status-msg', saveMessage.type]">
              {{ saveMessage.text }}
            </span>
          </transition>
          <button
            @click="handleSave"
            :disabled="isSaving"
            class="save-btn"
          >
            {{ isSaving ? 'Сохранение...' : 'Сохранить изменения' }}
          </button>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.settings-container {
  max-width: 1000px;
  margin: 100px auto 60px;
  padding: 0 24px;
  color: var(--text-header);
}

.settings-title {
  font-size: 2.25rem;
  margin-bottom: 32px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.settings-layout {
  display: flex;
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  min-height: 500px;
  overflow: hidden;
  box-shadow: 0 4px 12px var(--shadow-color);
}

.settings-sidebar {
  width: 260px;
  background: rgba(0, 0, 0, 0.1);
  border-right: 1px solid var(--border-color);
  padding: 24px 16px;
}

.tab-btn {
  width: 100%;
  padding: 14px 20px;
  border: none;
  background: none;
  color: var(--text-muted);
  text-align: left;
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.2s;
  border-radius: 8px;
  margin-bottom: 8px;
  font-weight: 500;
}

.tab-btn:hover {
  background: var(--hover-dropdowb);
  color: var(--text-header);
}

.tab-btn.active {
  background: var(--btn-plus);
  color: white;
  font-weight: 600;
}

.settings-content {
  flex: 1;
  padding: 40px;
  display: flex;
  flex-direction: column;
}

.settings-section h2 {
  margin: 0 0 12px;
  font-size: 1.5rem;
  font-weight: 700;
}

.section-desc {
  color: var(--text-muted);
  margin-bottom: 32px;
  font-size: 0.95rem;
  line-height: 1.5;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 0;
  border-bottom: 1px solid var(--border-color);
}

.setting-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-width: 60%;
}

.setting-info label {
  font-weight: 600;
  font-size: 1.05rem;
}

.setting-info span {
  font-size: 0.85rem;
  color: var(--text-muted);
  line-height: 1.4;
}


.custom-select {
  padding: 10px 36px 10px 16px;
  border-radius: 8px;
  background: var(--bg-header);
  color: var(--text-header);
  border: 1px solid var(--border-color);
  min-width: 220px;
  font-size: 0.95rem;
  appearance: none;
  background-image: url("data:image/svg+xml;charset=US-ASCII,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22292.4%22%20height%3D%22292.4%22%3E%3Cpath%20fill%3D%22%23a1a1aa%22%20d%3D%22M287%2069.4a17.6%2017.6%200%200%200-13-5.4H18.4c-5%200-9.3%201.8-12.9%205.4A17.6%2017.6%200%200%200%200%2082.2c0%205%201.8%209.3%205.4%2012.9l128%20127.9c3.6%203.6%207.8%205.4%2012.8%205.4s9.2-1.8%2012.8-5.4L287%2095c3.5-3.5%205.4-7.8%205.4-12.8%200-5-1.9-9.2-5.5-12.8z%22%2F%3E%3C%2Fsvg%3E");
  background-repeat: no-repeat;
  background-position: right 16px top 50%;
  background-size: 10px auto;
  cursor: pointer;
  transition: border-color 0.2s;
}

.custom-select:hover, .custom-select:focus {
  border-color: var(--btn-plus);
  outline: none;
}




.settings-footer {
  margin-top: auto;
  padding-top: 32px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 24px;
}

.save-btn {
  padding: 12px 28px;
  background: var(--btn-plus);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 1rem;
  transition: background 0.2s, transform 0.2s;
}
.save-btn:hover:not(:disabled) {
  background: var(--btn-plus-hover);
  transform: translateY(-1px);
}

.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.status-msg { font-size: 0.95rem; font-weight: 500; }
.status-msg.success { color: #10b981; }
.status-msg.error { color: #ef4444; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.account-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 28px;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .account-grid {
    grid-template-columns: 1fr;
  }
}

.account-card {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 28px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 420px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
}

.card-header-block {
  margin-bottom: 20px;
}

.card-header-block h3 {
  margin: 0 0 6px;
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-header);
}

.card-desc {
  font-size: 0.85rem;
  color: var(--text-muted);
  line-height: 1.4;
  margin: 0;
}

.card-inner-flow {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  justify-content: space-between;
}

.card-body-block {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex-grow: 1;
  justify-content: center;
  margin-bottom: 24px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.form-input {
  padding: 11px 14px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-header);
  color: var(--text-header);
  font-size: 0.95rem;
  outline: none;
  transition: border-color 0.2s;
}

.form-input:focus {
  border-color: var(--btn-plus);
}

.code-input {
  text-align: center;
  font-size: 1.25rem;
  letter-spacing: 6px;
  font-weight: bold;
  text-transform: uppercase;
}

.error-text {
  color: #ef4444;
  font-size: 0.85rem;
  margin-top: 4px;
  font-weight: 500;
}

.success-text {
  color: #10b981;
  font-size: 0.85rem;
  margin-top: 4px;
  font-weight: 500;
}

.card-actions-block {
  margin-top: auto;
}

.action-btn {
  width: 100%;
  padding: 12px 20px;
  background: var(--btn-plus);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.95rem;
  transition: background 0.2s, transform 0.1s;
}

.action-btn:hover:not(:disabled) {
  background: var(--btn-plus-hover);
}

.action-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-group {
  display: flex;
  gap: 12px;
  width: 100%;
}

.btn-group .action-btn {
  flex: 2;
}

.cancel-btn {
  flex: 1;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.05);
  color: var(--text-header);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.95rem;
  transition: background 0.2s, transform 0.1s;
  text-align: center;
}

.cancel-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.cancel-btn:active {
  transform: scale(0.98);
}
</style>
