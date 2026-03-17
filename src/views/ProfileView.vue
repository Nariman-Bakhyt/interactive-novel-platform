<script setup lang="ts">
import { onMounted, ref ,computed} from 'vue';
import {useAuthStore} from "@/api/auth.ts";
import {useRoute} from "vue-router";
import ProfileModal from '@/components/profile/ProfileModal.vue';
import type {UserResponseDto} from "@/types/auth.ts";
import router from "@/router";


const authStore = useAuthStore();
const isLoading = ref(true);
const showAuthModal = ref(false);
const route = useRoute();
const showProfileModal = ref(false);

if (route.query.redirect) {
  showAuthModal.value = true;
}

onMounted(async () => {
  try {
    await authStore.fetchUserDetails();
  }
  catch(error){
    console.error("Не удалось загрузить профиль.");
  }finally {
    isLoading.value = false;
  }
})

const avatarDisplayUrl = computed(() => {
  const url = authStore.userDetails?.avatarUrl;

  if (!url) {
    return 'http://127.0.0.1:9000/interactive-novel-assets/avatars/default-avatar.png';
  }
  return `${url}?t=${authStore.avatarTimestamp}`;
});

const setStoreDetails = (updatedUserDto: UserResponseDto) => {
  authStore.setDetails(updatedUserDto);
}

</script>
<template>
  <div class="profile-page">
    <div v-if="isLoading" class="loading-container">
      <div class="spinner"></div>
      <p>Загрузка данных профиля...</p>
    </div>

    <div v-else-if="authStore.userDetails" class="profile-layout">
      <aside class="profile-sidebar">
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <img :src="avatarDisplayUrl" alt="Аватар" class="profile-avatar">
          </div>
          <h2 class="profile-name">{{ authStore.userDetails.username }}</h2>
          <p class="profile-role">Автор</p>
        </div>

        <button @click="showProfileModal = true" class="btn-edit-full">
          <span>⚙️</span> Настроить профиль
        </button>
      </aside>

      <main class="profile-main">
        <section class="info-card">
          <h3>Личная информация</h3>
          <div class="info-grid">
            <div class="info-item">
              <label>User ID</label>
              <span>#{{ authStore.userDetails.id }}</span>
            </div>
            <div class="info-item">
              <label>Электронная почта</label>
              <span>{{ authStore.userDetails.email }}</span>
            </div>
          </div>
        </section>

        <section class="stats-row">
          <div class="stat-box">
            <span class="stat-value">0</span>
            <span class="stat-label">Новелл</span>
          </div>
          <div class="stat-box">
            <span class="stat-value">0</span>
            <span class="stat-label">Подписчиков</span>
          </div>
        </section>
      </main>
    </div>

    <div v-else class="error-container">
      <p>Упс! Не удалось загрузить данные.</p>
      <button @click="router.push('/')" class="btn-secondary">Вернуться на главную</button>
    </div>

    <ProfileModal v-model:isVisible="showProfileModal" />
  </div>
</template>
<style scoped>
.profile-page {
  max-width: 1000px;
  margin: 80px auto 40px; /* Отступ сверху под фиксированный хедер */
  padding: 0 20px;
  color: #e0e0e0;
}

.profile-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 30px;
}

/* Сайдбар с аватаром */
.profile-sidebar {
  background: #2c2c2c;
  padding: 30px;
  border-radius: 16px;
  text-align: center;
  height: fit-content;
  border: 1px solid #3d3d3d;
}

.avatar-wrapper {
  position: relative;
  width: 160px;
  height: 160px;
  margin: 0 auto 20px;
}

.profile-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid black;
}


.profile-name {
  margin: 10px 0 5px;
  font-size: 1.5rem;
}

.profile-role {
  color: #888;
  font-size: 0.9rem;
  margin-bottom: 25px;
}

.btn-edit-full {
  width: 100%;
  padding: 12px;
  background: #3d3d3d;
  border: none;
  border-radius: 8px;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: background 0.2s;
}

.btn-edit-full:hover {
  background: #4d4d4d;
}

/* Основной контент */
.info-card {
  background: #2c2c2c;
  padding: 30px;
  border-radius: 16px;
  border: 1px solid #3d3d3d;
  margin-bottom: 20px;
}

.info-card h3 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 1.2rem;
  border-bottom: 1px solid #3d3d3d;
  padding-bottom: 10px;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.info-item label {
  display: block;
  color: #888;
  font-size: 0.8rem;
  margin-bottom: 5px;
}

.info-item span {
  font-weight: 500;
}

/* Секция статистики */
.stats-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.stat-box {
  background: #2c2c2c;
  padding: 20px;
  border-radius: 16px;
  text-align: center;
  border: 1px solid #3d3d3d;
}

.stat-value {
  display: block;
  font-size: 1.8rem;
  font-weight: bold;
  color: #3498db;
}

.stat-label {
  color: #888;
  font-size: 0.85rem;
}

/* Адаптивность для мобилок */
@media (max-width: 768px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
}
</style>
