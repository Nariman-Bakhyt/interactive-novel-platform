<script setup lang="ts">
import {useSocialStore} from "@/components/social/socialStore.ts"


defineProps<{
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
  gap: 12px;
  width: 100%;
}

.friend-actions, .follow-actions {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.action-row {
  display: flex;
  gap: 12px;
  width: 100%;
}


button {
  width: 100%;
  height: 44px; 
  padding: 0 16px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.95rem;
  cursor: pointer;
  transition: background 0.2s, transform 0.2s;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
}

button:hover {
  transform: translateY(-1px);
}


.btn-primary {
  background-color: var(--btn-plus);
  color: white;
}
.btn-primary:hover { background-color: var(--btn-plus-hover); }


.btn-secondary {
  background-color: var(--bg-dropdown);
  color: var(--text-header);
  border: 1px solid var(--border-color);
}
.btn-secondary:hover { background-color: var(--hover-dropdowb); border-color: var(--text-muted); }


.btn-outline {
  background-color: transparent;
  border: 1px solid var(--btn-plus);
  color: var(--btn-plus);
}
.btn-outline:hover { background-color: rgba(99, 102, 241, 0.1); }


.btn-block-action {
  background-color: transparent;
  color: #ef4444; 
  border: 1px solid rgba(239, 68, 68, 0.3);
}
.btn-block-action:hover {
  background-color: rgba(239, 68, 68, 0.1);
}


.btn-block.active {
  background-color: #ef4444;
  color: white;
}
.btn-block.active:hover { background-color: #dc2626; }


.btn-star-minimal {
  width: 44px; 
  min-width: 44px;
  padding: 0;
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  font-size: 1.2rem;
  line-height: 1;
}
.btn-star-minimal:hover {
  background: var(--hover-dropdowb);
  border-color: var(--text-muted);
}
.star-empty { color: #f59e0b; opacity: 0.7; }
.star-filled { color: #f59e0b; filter: drop-shadow(0 0 4px rgba(245, 158, 11, 0.4)); }


.loading-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}
.skeleton-btn {
  height: 44px;
  background: linear-gradient(90deg, var(--bg-dropdown) 25%, var(--border-color) 50%, var(--bg-dropdown) 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  border-radius: 8px;
}
@keyframes loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
