<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useAuthStore} from "@/api/auth.ts";
import ProfileModal from '@/components/profile/ProfileModal.vue';

import type {ProfileResponseDto} from "@/types/auth.ts";
import RelationshipButton from "@/components/social/RelationshipButton.vue";
import {getFollowers, getFollowing, getFriends} from "@/api/socialService.ts";
import SocialListModal from "@/components/social/SocialListModal.vue";

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();

const isLoading = ref(true);
const showProfileModal = ref(false);

const profileData = ref<ProfileResponseDto | null>(null);

const isMyProfile = computed(() => {
  if (!authStore.isInitialized) return null;
  const targetId = route.params.id;
  if (!targetId) return true;
  return Number(targetId) === authStore.userDetails?.id;
});

watch(() => authStore.userDetails, (newDetails) => {
  if (isMyProfile.value && newDetails) {
    profileData.value = newDetails as ProfileResponseDto;
  }
}, { deep: true });

const loadProfile = async () => {
  isLoading.value = true;

  try {
    while (!authStore.isInitialized) {
      await new Promise(resolve => setTimeout(resolve, 50));
    }
    if (isMyProfile.value) {
      await authStore.fetchUserDetails();
      profileData.value = authStore.userDetails as ProfileResponseDto;
    } else {
      const targetId = Number(route.params.id);
      if (!targetId) {
        console.error("ID пользователя не найден");
        return;
      }
      profileData.value = await authStore.fetchUserDetailsById(targetId);
    }
  } catch (error) {
    console.error("Не удалось загрузить профиль.", error);
    profileData.value = null;
  } finally {
    isLoading.value = false;
  }
};

onMounted(loadProfile);
// ВАЖНО: Следим за URL. Если пользователь перейдет с чужого профиля
// на другой чужой профиль, страница должна обновиться
watch(() => route.params.id, loadProfile);

const avatarDisplayUrl = computed(() => {
  return profileData.value?.avatarUrl || '';
});


const activeModal = ref<{ title: string, fn: any } | null>(null);

const openFriends = () => {
  if (isMyProfile.value){
    activeModal.value = { title: 'Друзья', fn: getFriends };
    activeModal.value = {
      title: 'Друзья',
      fn: (page: number, size: number) => getFriends(page, size)
    };
  }
};

const openFollowers = () => {
  if (isMyProfile.value){
    activeModal.value = {
      title: 'Подписчики',
      fn: (page: number, size: number) => getFollowers(page, size)
    };
  }
};
const openFollowing = () => {
  if (isMyProfile.value){
    activeModal.value = {
      title: 'Подписки',
      fn: (page: number, size: number) => getFollowing(page, size)
    };
  }
};



</script>
<template>
  <div class="profile-page">
    <div v-if="isLoading" class="loading-container">
      <div class="spinner"></div>
      <p>Загрузка данных профиля...</p>
    </div>

    <div v-else-if="profileData" class="profile-layout">
      <aside class="profile-sidebar">
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <img :src="avatarDisplayUrl" alt="Аватар" class="profile-avatar">
          </div>
          <h2 class="profile-name">{{ profileData.username }}</h2>
          <p class="profile-role">Автор</p>
        </div>

        <div v-if="isMyProfile" class="profile-actions">
          <button @click="router.push('/social')" class="btn-edit-full">
            <span>👥</span> Сообщество
          </button>

          <button @click="showProfileModal = true" class="btn-edit-full">
            <span>⚙️</span> Настроить профиль
          </button>
        </div>

        <div v-else class="social-actions">
          <RelationshipButton :userId="profileData.id" />
        </div>
      </aside>

      <main class="profile-main">
        <section class="info-card">
          <h3>Личная информация</h3>
          <div class="info-grid">
            <div class="info-item">
              <label>User ID</label>
              <span>#{{ profileData.id }}</span>
            </div>

            <div v-if="isMyProfile" class="info-item">
              <label>Электронная почта</label>
              <span>{{ profileData.email }}</span>
            </div>
          </div>
        </section>

        <section class="stats-row">
          <div class="stat-box">
            <span class="stat-value">{{ profileData.novelsCount || 0 }}</span>
            <span class="stat-label">Новелл</span>
          </div>
          <div class="stat-box" @click="openFriends">
            <span class="stat-value">{{ profileData.friendsCount }}</span>
            <span class="stat-label">Друзей</span>
          </div>
          <div class="stat-box" @click="openFollowers">
            <span class="stat-value">{{ profileData.followersCount || 0 }}</span>
            <span class="stat-label">Подписчиков</span>
          </div>
          <div class="stat-box" @click="openFollowing">
            <span class="stat-value">{{ profileData.followingCount || 0 }}</span>
            <span class="stat-label">Подписки</span>
          </div>
          <SocialListModal
          v-if="activeModal"
          :title="activeModal.title"
          :fetchFn="activeModal.fn"
          @close="activeModal = null"
        />
        </section>
      </main>
    </div>

    <div v-else class="error-container">
      <p>Упс! Не удалось загрузить данные пользователя.</p>
      <button @click="router.push('/')" class="btn-secondary">Вернуться на главную</button>
    </div>

    <ProfileModal v-if="isMyProfile" v-model:isVisible="showProfileModal" />
  </div>

</template>
<style scoped>

.social-actions {
  margin-top: 15px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

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
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px; /* Зазор между иконкой и текстом внутри кнопки */
  padding: 10px 20px;
  background-color: var(--bg-main); /* Или тот цвет, что на скрине */
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  color: var(--text-header);
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn-edit-full:hover {
  background-color: var(--hover-dropdowb); /* Подсветка при наведении */
  border-color: var(--text-muted);
  transform: translateY(-1px);
}

.btn-edit-full span {
  font-size: 1.1rem; /* Иконка чуть покрупнее текста */
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

.profile-actions {
  display: flex;
  gap: 12px; /* Тот самый зазор между кнопками */
  margin-bottom: 20px; /* Отступ снизу от блока кнопок */
  flex-wrap: wrap; /* Чтобы на мобилках они могли перенестись */
}

/* Адаптивность для мобилок */
@media (max-width: 768px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
}
</style>
