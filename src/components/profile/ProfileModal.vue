<script setup lang="ts">
import { defineProps, defineEmits, onMounted, onUnmounted } from 'vue';
import { useAuthStore } from '@/api/auth.ts';
import type {ProfileResponseDto, UserResponseDto} from '@/types/auth';
import AvatarUploader from './AvatarUploader.vue';
import ProfileUpdateForm from './ProfileUpdateForm.vue';

const authStore = useAuthStore();

const props = defineProps<{
  isVisible: boolean;
}>();

const emit = defineEmits(['update:isVisible']);

const closeModal = () => {
  emit('update:isVisible', false);
};

// Закрытие по нажатию Esc (хороший тон для UX)
const handleEsc = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && props.isVisible) closeModal();
};

onMounted(() => window.addEventListener('keydown', handleEsc));
onUnmounted(() => window.removeEventListener('keydown', handleEsc));

const handleAvatarUpdate = (updatedUserDto: ProfileResponseDto) => {
  authStore.setDetails(updatedUserDto);
};

const handleProfileUpdate = (updatedUserDto: ProfileResponseDto) => {
  authStore.setDetails(updatedUserDto);
};
</script>

<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="isVisible" class="modal-overlay" @click.self="closeModal">
        <div class="modal-content">
          <button class="close-x" @click="closeModal" aria-label="Закрыть">&times;</button>

          <header class="modal-header">
            <h2>Настройки профиля</h2>
          </header>

          <main class="modal-body">
            <section class="section-upload">
              <AvatarUploader
                :initial-avatar-url="authStore.userDetails?.avatarUrl || null"
                @avatar-updated="handleAvatarUpdate"
              />
            </section>

            <div class="divider"></div>

            <section class="section-form">
              <ProfileUpdateForm
                :initial-username="authStore.userDetails?.username || ''"
                :initial-email="authStore.userDetails?.email || ''"
                @profile-updated="handleProfileUpdate"
              />
            </section>
          </main>

          <footer class="modal-footer">
            <button @click="closeModal" class="btn-close">Готово</button>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
<style scoped>

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.85);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
  backdrop-filter: blur(4px);
}

/* Контент модалки */
.modal-content {
  background: var(--bg-main);
  color: var(--text-header);
  padding: 40px;
  border-radius: 16px;
  width: 95%;
  max-width: 550px;
  position: relative;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.6);
  border: 1px solid #333;
}

/* Заголовок */
.modal-header h2 {
  margin: 0;
  font-size: 1.8rem;
  color: var(--text-header);
}
.subtitle {
  color: #888;
  font-size: 0.9rem;
  margin-top: 5px;
  margin-bottom: 25px;
}

.close-x {
  position: absolute;
  top: 20px;
  right: 20px;
  font-size: 28px;
  background: none;
  border: none;
  color: #666;
  cursor: pointer;
  line-height: 1;
  transition: color 0.2s;
}
.close-x:hover { color: #fff; }

/* Разделитель */
.divider {
  height: 1px;
  background: linear-gradient(to right, transparent, #444, transparent);
  margin: 10px 0;
}

.modal-footer {
  margin-top: 30px;
  display: flex;
  justify-content: flex-end;
}

.btn-close {
  padding: 10px 25px;
  background: #3d3d3d;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: background 0.2s;
}
.btn-close:hover { background: #555; }

.modal-fade-enter-active, .modal-fade-leave-active {
  transition: all 0.3s ease;
}

.modal-fade-enter-from, .modal-fade-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

.modal-content::-webkit-scrollbar { width: 6px; }
.modal-content::-webkit-scrollbar-thumb {
  background: #444;
  border-radius: 10px;
}
</style>
