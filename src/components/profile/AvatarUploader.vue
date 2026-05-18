<script setup lang = "ts">
import {ref, defineProps, defineEmits, watch, computed} from 'vue';
import {uploadAvatar} from "@/api/profileService.ts";
import type {ProfileResponseDto, UserResponseDto} from '@/types/auth';
import {useAuthStore} from "@/api/auth.ts";
const props = defineProps<{
  initialAvatarUrl: string | null;
}>();
const emit = defineEmits<{
  (e: 'avatar-updated',userDto: ProfileResponseDto): void
}>();

const currentAvatarUrl = ref(props.initialAvatarUrl);
const fileInput = ref<HTMLInputElement | null>(null);
const message = ref('');
const isError = ref(false);
const authStore = useAuthStore();
const isUploading = ref(false); // Новое состояние

const handleFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;

  if (!validateFile(file)) {
    input.value = '';
    return;
  }

  isUploading.value = true; // Запускаем лоадер
  try {
    const updatedUserDto = await uploadAvatar(file);
    currentAvatarUrl.value = updatedUserDto.avatarUrl;
    message.value = 'Аватар обновлен';
    isError.value = false;
    authStore.setDetails(updatedUserDto);
    emit('avatar-updated', updatedUserDto);
  } catch (error: any) {
    isError.value = true;
    message.value = error.message || 'Ошибка загрузки';
  } finally {
    isUploading.value = false; // Выключаем лоадер
    input.value = '';
  }
};
watch(() => props.initialAvatarUrl, (newUrl) => {
  currentAvatarUrl.value = newUrl;
});

const triggerFileUpload = () => {
  fileInput.value?.click();
};

const validateFile = (file: File): boolean => {
  const ALLOWED_MIME_TYPES = ['image/jpeg', 'image/png', 'image/gif'];
  const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

  if (file.size > MAX_FILE_SIZE_BYTES) {
    message.value = `Файл слишком большой. Максимальный размер: 5 МБ.`;
    isError.value = true;
    return false;
  }

  if (!ALLOWED_MIME_TYPES.includes(file.type)) {
    message.value = 'Недопустимый формат файла. Разрешены только JPG, PNG, GIF.';
    isError.value = true;
    return false;
  }

  isError.value = false;
  message.value = '';
  return true;
};


const handleDeleteAvatar = async () => {
  if (!confirm('Вы уверены, что хотите удалить аватар?')) return;



  try {
    const updatedUserDto = await uploadAvatar(null);

    currentAvatarUrl.value = updatedUserDto.avatarUrl;
    message.value = 'Аватар успешно удален.';
    authStore.setDetails(updatedUserDto);
    emit('avatar-updated', updatedUserDto);
  } catch (error: any) {
    isError.value = true;
    message.value = error.message || 'Ошибка удаления.';
  }
};

const avatarDisplayUrl = computed(() => {
  return authStore.userDetails?.avatarUrl ||'';
});
</script>
<template>
  <div class="avatar-uploader">
    <div class="avatar-container" @click="triggerFileUpload" :class="{ 'loading': isUploading }">
      <img
        :src="avatarDisplayUrl"
        alt="Аватар пользователя"
        class="user-avatar-main"
      >
      <div class="avatar-overlay">
        <span class="overlay-icon">📷</span>
        <span class="overlay-text">Изменить</span>
      </div>

      <div v-if="isUploading" class="spinner-container">
        <div class="loader"></div>
      </div>
    </div>

    <input
      type="file"
      ref="fileInput"
      @change="handleFileChange"
      accept="image/jpeg, image/png, image/gif"
      style="display: none;"
    >

    <div class="info-zone">
      <p v-if="message" :class="['message-text', { 'error-msg': isError, 'success-msg': !isError }]">
        {{ message }}
      </p>

      <div class="actions">
        <button
          v-if="currentAvatarUrl"
          @click.stop="handleDeleteAvatar"
          class="btn-delete"
          :disabled="isUploading"
        >
          Удалить текущее фото
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.avatar-uploader {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
}

.avatar-container {
  position: relative;
  width: 140px;
  height: 140px;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  border: 4px solid var(--btn-plus);
  transition: transform 0.2s ease, border-color 0.2s ease;
  box-shadow: 0 4px 12px var(--shadow-color);
}

.avatar-container:hover {
  transform: scale(1.05);
  border-color: var(--btn-plus-hover);
}

.user-avatar-main {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* Оверлей при наведении */
.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
  color: white;
  backdrop-filter: blur(2px);
}

.avatar-container:hover .avatar-overlay {
  opacity: 1;
}

.overlay-icon { font-size: 2rem; margin-bottom: 4px;}
.overlay-text { font-size: 0.9rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em;}

.spinner-container {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(2px);
}

.loader {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(255,255,255,0.3);
  border-top: 3px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.info-zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 40px; /* Чтобы не прыгало при появлении текста */
}

.message-text {
  font-size: 0.9rem;
  margin: 0 0 8px;
  font-weight: 500;
  text-align: center;
}
.error-msg { color: #ef4444; } /* red-500 */
.success-msg { color: #10b981; } /* emerald-500 */

.btn-delete {
  background: transparent;
  color: var(--text-muted);
  border: none;
  font-size: 0.9rem;
  cursor: pointer;
  padding: 6px 12px;
  transition: color 0.2s;
  font-weight: 500;
}

.btn-delete:hover:not(:disabled) {
  color: #ef4444;
  text-decoration: underline;
}

.btn-delete:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
