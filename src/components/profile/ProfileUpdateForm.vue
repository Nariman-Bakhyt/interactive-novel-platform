<script setup lang="ts">
import { updateProfileApi,changePasswordApi } from '@/api/profileService.ts';
import type {UserUpdateRequestDto, ChangePasswordRequestDto } from '@/types/user';
import type {ProfileResponseDto, UserResponseDto} from '@/types/auth';
import { defineProps, defineEmits, reactive, ref } from 'vue';
import {useAuthStore} from "@/api/auth.ts";

// --- PROPS и ЭМИТЫ ---
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

const updateProfileDetails = async () => {
  profileMessage.value = '';

  if (profileData.newUsername === props.initialUsername && profileData.newEmail === props.initialEmail) {
    profileMessage.value = 'Данные не изменены.';
    isProfileError.value = false;
    return;
  }

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
  }
};

const changePassword = async () => {
  passwordMessage.value = '';

  if (passwordData.oldPassword === passwordData.newPassword) {
    passwordMessage.value = 'Новый пароль должен отличаться от старого.';
    isPasswordError.value = true;
    return;
  }

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
  }
};
</script>

<template>
  <div class="profile-forms">

    <form @submit.prevent="updateProfileDetails">
      <h3>Личные данные</h3>

      <div class="form-group">
        <label for="newUsername">Логин</label>
        <input type="text" id="newUsername" v-model="profileData.newUsername" required>
      </div>

      <div class="form-group">
        <label for="newEmail">Email</label>
        <input type="email" id="newEmail" v-model="profileData.newEmail" required>
      </div>

      <button type="submit" class="btn btn-success">Сохранить изменения</button>
      <p v-if="profileMessage" :class="{ 'error': isProfileError, 'success': !isProfileError }">{{ profileMessage }}</p>
    </form>

    <div class="divider"></div>

    <form @submit.prevent="changePassword">
      <h3>Сменить пароль</h3>

      <div class="form-group">
        <label for="oldPassword">Старый пароль</label>
        <input type="password" id="oldPassword" v-model="passwordData.oldPassword" required>
      </div>

      <div class="form-group">
        <label for="newPassword">Новый пароль</label>
        <input type="password" id="newPassword" v-model="passwordData.newPassword" required>
      </div>

      <button type="submit" class="btn btn-warning">Изменить пароль</button>
      <p v-if="passwordMessage" :class="{ 'error': isPasswordError, 'success': !isPasswordError }">{{ passwordMessage }}</p>
    </form>
  </div>
</template>
<style scoped>
.profile-forms {
  margin-top: 20px;
}
.form-group {
  margin-bottom: 15px;
}
.form-group label {
  display: block;
  font-weight: bold;
  margin-bottom: 5px;
}
.form-group input {
  width: 100%;
  padding: 8px;
  box-sizing: border-box;
  border: 1px solid #ccc;
  border-radius: 4px;
}
.btn {
  padding: 10px 15px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 10px;
}
.btn-success {
  background-color: #42b883; /* Цвет Vue/успеха */
  color: white;
}
.btn-warning {
  background-color: #ff9800; /* Желтый/предупреждение */
  color: white;
}
.error {
  color: red;
  margin-top: 10px;
}
.success {
  color: green;
  margin-top: 10px;
}
.divider {
  height: 1px;
  background: linear-gradient(to right, transparent, #444, transparent);
  margin: 10px 0;
}
</style>
