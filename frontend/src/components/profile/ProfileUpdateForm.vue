<script setup lang="ts">
import {changePasswordApi, updateProfileApi} from '@/api/profileService.ts';
import type {ChangePasswordRequestDto, UserUpdateRequestDto} from '@/types/user';
import type {ProfileResponseDto} from '@/types/auth';
import {reactive, ref} from 'vue';
import {useAuthStore} from "@/api/auth.ts";


const props = defineProps<{
  initialUsername: string;
  initialEmail: string;
}>();

const emit = defineEmits<{
  (e: 'profile-updated', userDto: ProfileResponseDto): void;
  (e: 'password-changed'): void;
}>();

const profileData: UserUpdateRequestDto = reactive({
  newUsername: props.initialUsername,
  newEmail: props.initialEmail,
});

const passwordData: ChangePasswordRequestDto = reactive({
  oldPassword: '',
  newPassword: '',
});

const profileMessage = ref('');
const isProfileError = ref(false);
const passwordMessage = ref('');
const isPasswordError = ref(false);
const authStore = useAuthStore();
const isSavingProfile = ref(false);
const isChangingPassword = ref(false);

const updateProfileDetails = async () => {
  profileMessage.value = '';

  if (profileData.newUsername === props.initialUsername && profileData.newEmail === props.initialEmail) {
    profileMessage.value = 'Данные не изменены.';
    isProfileError.value = false;
    return;
  }

  isSavingProfile.value = true;
  try {
    console.log('Данные для отправки:', profileData)
    const updatedUserDto = await updateProfileApi(profileData);

    profileMessage.value = `Профиль успешно обновлен!`;
    isProfileError.value = false;
    authStore.setDetails(updatedUserDto);
    emit('profile-updated', updatedUserDto);

  } catch (error: any) {
    isProfileError.value = true;
    passwordMessage.value = '';

    profileMessage.value = error.message || 'Ошибка обновления профиля.';
  } finally {
    isSavingProfile.value = false;
  }
};

const changePassword = async () => {
  passwordMessage.value = '';

  if (passwordData.oldPassword === passwordData.newPassword) {
    passwordMessage.value = 'Новый пароль должен отличаться от старого.';
    isPasswordError.value = true;
    return;
  }

  isChangingPassword.value = true;
  try {
    await changePasswordApi(passwordData);

    passwordMessage.value = `Пароль успешно изменен!`;
    isPasswordError.value = false;

    passwordData.oldPassword = '';
    passwordData.newPassword = '';

    emit('password-changed');

  } catch (error: any) {
    isPasswordError.value = true;
    profileMessage.value = '';

    passwordMessage.value = error.message || 'Ошибка смены пароля.';
  } finally {
    isChangingPassword.value = false;
  }
};
</script>

<template>
  <div class="profile-forms">

    <form @submit.prevent="updateProfileDetails" class="form-section">
      <h3>Личные данные</h3>

      <div class="form-group">
        <label for="newUsername">Логин</label>
        <input type="text" id="newUsername" v-model="profileData.newUsername" required>
      </div>

      <div class="form-group">
        <label for="newEmail">Email</label>
        <input type="email" id="newEmail" v-model="profileData.newEmail" required>
      </div>

      <div class="form-actions">
        <button type="submit" class="btn btn-primary" :disabled="isSavingProfile">
          {{ isSavingProfile ? 'Сохранение...' : 'Сохранить изменения' }}
        </button>
      </div>
      <p v-if="profileMessage" :class="['message', { 'error': isProfileError, 'success': !isProfileError }]">{{ profileMessage }}</p>
    </form>

    <div class="divider"></div>

    <form @submit.prevent="changePassword" class="form-section">
      <h3>Сменить пароль</h3>

      <div class="form-group">
        <label for="oldPassword">Старый пароль</label>
        <input type="password" id="oldPassword" v-model="passwordData.oldPassword" required>
      </div>

      <div class="form-group">
        <label for="newPassword">Новый пароль</label>
        <input type="password" id="newPassword" v-model="passwordData.newPassword" required>
      </div>

      <div class="form-actions">
        <button type="submit" class="btn btn-warning" :disabled="isChangingPassword">
          {{ isChangingPassword ? 'Изменение...' : 'Изменить пароль' }}
        </button>
      </div>
      <p v-if="passwordMessage" :class="['message', { 'error': isPasswordError, 'success': !isPasswordError }]">{{ passwordMessage }}</p>
    </form>
  </div>
</template>
<style scoped>
.profile-forms {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-section h3 {
  margin: 0 0 16px;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-header);
}

.form-group {
  margin-bottom: 16px;
}
.form-group label {
  display: block;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-header);
  font-size: 0.95rem;
}
.form-group input {
  width: 100%;
  padding: 12px 16px;
  box-sizing: border-box;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-main);
  color: var(--text-header);
  font-size: 0.95rem;
  transition: border-color 0.2s;
  font-family: inherit;
}
.form-group input:focus {
  outline: none;
  border-color: var(--btn-plus);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.95rem;
  transition: background 0.2s, transform 0.2s;
}
.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background-color: var(--btn-plus);
  color: white;
}
.btn-primary:hover:not(:disabled) {
  background-color: var(--btn-plus-hover);
  transform: translateY(-1px);
}

.btn-warning {
  background-color: #f59e0b; 
  color: white;
}
.btn-warning:hover:not(:disabled) {
  background-color: #d97706; 
  transform: translateY(-1px);
}

.message {
  margin-top: 16px;
  font-size: 0.9rem;
  font-weight: 500;
  padding: 12px;
  border-radius: 8px;
}
.error {
  color: #ef4444; 
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
}
.success {
  color: #10b981; 
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.2);
}

.divider {
  height: 1px;
  background: var(--border-color);
  margin: 8px 0;
}
</style>
