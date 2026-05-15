<script setup lang="ts">
import { useSocialStore } from "@/components/social/socialStore.ts"


const props = defineProps<{
  userId: number;
}>();

const social = useSocialStore();
</script>

<template>
  <div class="relationship-actions" v-if="social.isLoaded">

    <template v-if="social.isBlocked(userId)">
      <button class="btn-block active" @click="social.toggleBlock(userId)">
        🚫 Разблокировать
      </button>
    </template>

    <template v-else>
      <div class="friend-actions">
        <template v-if="social.isFriend(userId)">
          <div class="action-row">
            <button class="btn-secondary" @click="social.removeFriendOrRequest(userId)">
              🤝 В друзьях (Удалить)
            </button>
            <button
              class="btn-star-minimal"
              @click="social.toggleCloseFriend(userId)"
              :title="social.isCloseFriend(userId) ? 'Убрать из близких' : 'Добавить в близкие'"
            >
              <span v-if="social.isCloseFriend(userId)" class="star-filled">⭐</span>
              <span v-else class="star-empty">☆</span>
            </button>
          </div>
        </template>

        <button v-else-if="social.isOutgoing(userId)" class="btn-secondary" @click="social.removeFriendOrRequest(userId)">
          ⏳ Заявка отправлена
        </button>

        <div v-else-if="social.isIncoming(userId)" class="action-row">
          <button class="btn-primary" @click="social.acceptFriendReq(userId)">Принять</button>
          <button class="btn-secondary" @click="social.removeFriendOrRequest(userId)">Отклонить</button>
        </div>

        <button v-else class="btn-primary" @click="social.sendFriendReq(userId)">
          ➕ Добавить в друзья
        </button>
      </div>

      <div class="follow-actions">
        <button
          :class="social.isFollowing(userId) ? 'btn-secondary' : 'btn-outline'"
          @click="social.toggleFollow(userId)"
        >
          {{ social.isFollowing(userId) ? '✓ Вы подписаны' : '👀 Подписаться' }}
        </button>
      </div>

      <button class="btn-block-action" @click="social.toggleBlock(userId)">
        🚫 Заблокировать
      </button>

    </template>
  </div>

  <div v-else class="loading-actions">
    <div class="skeleton-btn"></div>
    <div class="skeleton-btn"></div>
    <div class="skeleton-btn"></div>
  </div>
</template>

<style scoped>
.relationship-actions {
  display: flex;
  flex-direction: column;
  gap: 8px; /* Уменьшил до 8px для плотности */
  width: 100%;
}

.friend-actions, .follow-actions {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.action-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

/* ОБЩИЙ СТИЛЬ ДЛЯ ВСЕХ КНОПОК */
button {
  width: 100%;
  height: 42px; /* Фиксированная высота для идеальной одинаковости */
  padding: 0 10px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
}

/* СИНЯЯ (ГЛАВНАЯ) */
.btn-primary {
  background-color: #3498db;
  color: white;
}
.btn-primary:hover { background-color: #2980b9; }

/* СЕРАЯ (ВТОРИЧНАЯ) */
.btn-secondary {
  background-color: #3d3d3d;
  color: #e0e0e0;
}
.btn-secondary:hover { background-color: #4d4d4d; }

/* КОНТУРНАЯ */
.btn-outline {
  background-color: transparent;
  border: 1px solid #3498db;
  color: #3498db;
}
.btn-outline:hover { background-color: rgba(52, 152, 219, 0.1); }

/* КНОПКА БЛОКИРОВКИ (ОПАСНОСТЬ) */
.btn-block-action {
  background-color: #3d3d3d;
  color: #e74c3c;
  border: 1px solid rgba(231, 76, 60, 0.2);
}
.btn-block-action:hover {
  background-color: #c0392b;
  color: white;
}

/* КНОПКА РАЗБЛОКИРОВКИ (АКТИВНАЯ БЛОКИРОВКА) */
.btn-block.active {
  background-color: #e74c3c;
  color: white;
}

/* ЗВЕЗДОЧКА */
.btn-star-minimal {
  width: 45px; /* Фиксированная ширина, чтобы не растягивалась */
  min-width: 45px;
  background: #3d3d3d;
  font-size: 1.2rem;
}
.star-empty { color: #f1c40f; opacity: 0.5; }
.star-filled { filter: drop-shadow(0 0 2px rgba(241, 196, 15, 0.5)); }

/* ЗАГРУЗКА */
.skeleton-btn {
  height: 42px;
  background: linear-gradient(90deg, #2c2c2c 25%, #3d3d3d 50%, #2c2c2c 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  border-radius: 8px;
}
@keyframes loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
