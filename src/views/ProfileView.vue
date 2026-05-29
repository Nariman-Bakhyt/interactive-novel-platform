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

        <div v-else-if="authStore.isAuthenticated" class="social-actions">
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
          <div :class="['stat-box', { clickable: isMyProfile }]" @click="openFriends">
            <span class="stat-value">{{ profileData.friendsCount }}</span>
            <span class="stat-label">Друзей</span>
          </div>
          <div :class="['stat-box', { clickable: isMyProfile }]" @click="openFollowers">
            <span class="stat-value">{{ profileData.followersCount || 0 }}</span>
            <span class="stat-label">Подписчиков</span>
          </div>
          <div :class="['stat-box', { clickable: isMyProfile }]" @click="openFollowing">
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
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.profile-page {
  max-width: 1000px;
  margin: 92px auto 60px;
  padding: 0 24px;
  color: var(--text-header);
  animation: fadeInUp 0.5s cubic-bezier(0.4,0,0.2,1) both;
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}

.profile-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 28px;
  align-items: start;
}

/* ── Sidebar — glassmorphism ── */
.profile-sidebar {
  background: var(--bg-dropdown);
  backdrop-filter: blur(20px) saturate(150%);
  padding: 32px;
  border-radius: var(--card-radius);
  text-align: center;
  height: fit-content;
  border: 1px solid var(--surface-glass-border);
  box-shadow: var(--shadow-card);
  position: sticky;
  top: 76px;
}

/* ── Avatar with gradient ring ── */
.avatar-wrapper {
  position: relative;
  width: 156px;
  height: 156px;
  margin: 0 auto 20px;
}
/* Gradient ring via border trick */
.avatar-wrapper::before {
  content: '';
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  background: var(--gradient-primary);
  z-index: 0;
  opacity: 0.85;
  transition: opacity var(--transition-base);
}
.avatar-wrapper:hover::before {
  opacity: 1;
  box-shadow: 0 0 28px var(--primary-glow-lg);
}
.profile-avatar {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid var(--bg-dropdown);
  box-shadow: 0 0 20px var(--primary-glow);
  transition: box-shadow var(--transition-base);
}
.avatar-wrapper:hover .profile-avatar {
  box-shadow: 0 0 36px var(--primary-glow-lg);
}

.profile-name {
  margin: 0 0 4px;
  font-size: 1.65rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.profile-role {
  color: var(--text-muted);
  font-size: 0.9rem;
  margin-bottom: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.profile-role::before {
  content: '';
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--btn-plus);
  box-shadow: 0 0 6px var(--primary-glow);
}

/* ── Profile action buttons ── */
.profile-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.btn-edit-full {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 11px 16px;
  background: var(--surface-glass);
  border: 1px solid var(--surface-glass-border);
  border-radius: 10px;
  color: var(--text-header);
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: background var(--transition-base), border-color var(--transition-base), transform var(--transition-base);
  white-space: nowrap;
}
.btn-edit-full:hover {
  background: var(--hover-dropdowb);
  border-color: var(--btn-plus);
  transform: translateY(-1px);
}
.btn-edit-full span { font-size: 1.1rem; }

/* ── Info card ── */
.info-card {
  background: var(--bg-dropdown);
  backdrop-filter: blur(16px);
  padding: 28px;
  border-radius: var(--card-radius);
  border: 1px solid var(--surface-glass-border);
  margin-bottom: 24px;
  box-shadow: var(--shadow-card);
}
.info-card h3 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 1.15rem;
  font-weight: 700;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 14px;
  position: relative;
}
.info-card h3::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 32px;
  height: 2px;
  background: var(--gradient-primary);
  border-radius: 2px;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.info-item label {
  display: block;
  color: var(--text-muted);
  font-size: 0.75rem;
  margin-bottom: 5px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.info-item span {
  font-weight: 500;
  font-size: 1rem;
}

/* ── Stats row ── */
.stats-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.stat-box {
  background: var(--bg-dropdown);
  backdrop-filter: blur(16px);
  padding: 22px;
  border-radius: var(--card-radius);
  text-align: center;
  border: 1px solid var(--surface-glass-border);
  box-shadow: var(--shadow-card);
  transition: transform var(--transition-base), box-shadow var(--transition-base), border-color var(--transition-base);
}
.stat-box.clickable { cursor: pointer; }
.stat-box.clickable:hover {
  transform: translateY(-4px);
  border-color: var(--btn-plus);
  box-shadow: var(--shadow-glow), var(--shadow-card);
}

/* Gradient stat value */
.stat-value {
  display: block;
  font-size: 2rem;
  font-weight: 800;
  margin-bottom: 4px;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.02em;
}
.stat-label {
  color: var(--text-muted);
  font-size: 0.85rem;
  font-weight: 500;
}

/* ── Loading / error ── */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 24px;
  gap: 16px;
  color: var(--text-muted);
}
.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-color);
  border-top-color: var(--btn-plus);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.error-container {
  text-align: center;
  padding: 80px 24px;
  color: var(--text-muted);
}
.btn-secondary {
  margin-top: 16px;
  padding: 10px 22px;
  background: var(--surface-glass);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-header);
  cursor: pointer;
  font-weight: 500;
  transition: background var(--transition-base);
}
.btn-secondary:hover { background: var(--hover-dropdowb); }

@media (max-width: 768px) {
  .profile-layout { grid-template-columns: 1fr; }
  .profile-sidebar { position: static; }
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}
</style>
