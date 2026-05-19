<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
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
  inset: 0;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
  backdrop-filter: blur(8px);
  padding: 24px;
}

/* Контент модалки */
.modal-content {
  background: var(--bg-dropdown);
  color: var(--text-header);
  padding: 32px;
  border-radius: 20px;
  width: 100%;
  max-width: 500px;
  position: relative;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5);
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
}

/* Заголовок */
.modal-header {
  margin-bottom: 24px;
}
.modal-header h2 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-header);
}

.close-x {
  position: absolute;
  top: 24px;
  right: 24px;
  font-size: 1.75rem;
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  line-height: 1;
  transition: color 0.2s, background 0.2s;
  padding: 4px 8px;
  border-radius: 6px;
}
.close-x:hover {
  color: var(--text-header);
  background: var(--hover-dropdowb);
}

/* Разделитель */
.divider {
  height: 1px;
  background: var(--border-color);
  margin: 24px 0;
}

.modal-footer {
  margin-top: 32px;
  display: flex;
  justify-content: flex-end;
}

.btn-close {
  padding: 12px 24px;
  background: var(--btn-plus);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: background 0.2s, transform 0.2s;
}
.btn-close:hover {
  background: var(--btn-plus-hover);
  transform: translateY(-1px);
}

.modal-fade-enter-active, .modal-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.modal-fade-enter-from, .modal-fade-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

.modal-content::-webkit-scrollbar { width: 6px; }
.modal-content::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 10px;
}
</style>
