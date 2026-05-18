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
          <div class="stat-box clickable" @click="openFriends">
            <span class="stat-value">{{ profileData.friendsCount }}</span>
            <span class="stat-label">Друзей</span>
          </div>
          <div class="stat-box clickable" @click="openFollowers">
            <span class="stat-value">{{ profileData.followersCount || 0 }}</span>
            <span class="stat-label">Подписчиков</span>
          </div>
          <div class="stat-box clickable" @click="openFollowing">
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
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.profile-page {
  max-width: 1000px;
  margin: 100px auto 60px; /* Отступ сверху под фиксированный хедер */
  padding: 0 24px;
  color: var(--text-header);
}

.profile-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 32px;
}

/* Сайдбар с аватаром */
.profile-sidebar {
  background: var(--bg-dropdown);
  padding: 32px;
  border-radius: 16px;
  text-align: center;
  height: fit-content;
  border: 1px solid var(--border-color);
  box-shadow: 0 4px 12px var(--shadow-color);
}

.avatar-wrapper {
  position: relative;
  width: 180px;
  height: 180px;
  margin: 0 auto 24px;
}

.profile-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid var(--border-color);
}


.profile-name {
  margin: 0 0 4px;
  font-size: 1.75rem;
  font-weight: 700;
}

.profile-role {
  color: var(--text-muted);
  font-size: 1rem;
  margin-bottom: 32px;
}

.btn-edit-full {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px; /* Зазор между иконкой и текстом внутри кнопки */
  padding: 12px 20px;
  background-color: var(--bg-main); /* Или тот цвет, что на скрине */
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-header);
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn-edit-full:hover {
  background-color: var(--hover-dropdowb); /* Подсветка при наведении */
  border-color: var(--text-muted);
}

.btn-edit-full span {
  font-size: 1.25rem; /* Иконка чуть покрупнее текста */
}

/* Основной контент */
.info-card {
  background: var(--bg-dropdown);
  padding: 32px;
  border-radius: 16px;
  border: 1px solid var(--border-color);
  margin-bottom: 32px;
  box-shadow: 0 4px 12px var(--shadow-color);
}

.info-card h3 {
  margin-top: 0;
  margin-bottom: 24px;
  font-size: 1.25rem;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 16px;
  font-weight: 600;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.info-item label {
  display: block;
  color: var(--text-muted);
  font-size: 0.85rem;
  margin-bottom: 6px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-item span {
  font-weight: 500;
  font-size: 1.1rem;
}

/* Секция статистики */
.stats-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.stat-box {
  background: var(--bg-dropdown);
  padding: 24px;
  border-radius: 16px;
  text-align: center;
  border: 1px solid var(--border-color);
  box-shadow: 0 4px 12px var(--shadow-color);
  transition: transform 0.2s, border-color 0.2s;
}

.stat-box.clickable {
  cursor: pointer;
}

.stat-box.clickable:hover {
  transform: translateY(-2px);
  border-color: var(--btn-plus);
}

.stat-value {
  display: block;
  font-size: 2rem;
  font-weight: 700;
  color: var(--btn-plus);
  margin-bottom: 4px;
}

.stat-label {
  color: var(--text-muted);
  font-size: 0.9rem;
  font-weight: 500;
}

.profile-actions {
  display: flex;
  flex-direction: column;
  gap: 12px; /* Тот самый зазор между кнопками */
}

/* Адаптивность для мобилок */
@media (max-width: 768px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
}
</style>
