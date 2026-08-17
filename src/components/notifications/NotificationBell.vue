<template>
  <div class="notification-bell-container">
    <button @click="toggleDropdown" class="bell-button" aria-label="Уведомления">
      <svg class="bell-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"></path>
      </svg>
      <span v-if="unreadCount > 0" class="unread-badge">
        {{ unreadCount }}
      </span>
    </button>

    <div v-if="isOpen" class="notifications-dropdown">
      <div class="dropdown-header">
        <h3 class="dropdown-title">Уведомления</h3>
        <button v-if="unreadCount > 0" @click="markAllRead" class="mark-all-btn">Прочитать всё</button>
      </div>
      <div class="notifications-list scrollbar">
        <div v-if="notifications.length === 0" class="empty-state">
          Нет уведомлений
        </div>
        <div v-for="notif in notifications" :key="notif.id" 
             class="notification-item"
             :class="{ 'unread': !notif.isRead }"
             @click="handleNotificationClick(notif)">
          <div class="notification-content">
            <div class="avatar-wrapper">
              <img v-if="notif.senderAvatar" :src="notif.senderAvatar" class="avatar-img" alt="" />
              <div v-else class="avatar-placeholder">
                {{ notif.senderName ? notif.senderName.charAt(0) : 'S' }}
              </div>
            </div>
            <div class="text-wrapper">
              <p class="sender-name">{{ notif.senderName }}</p>
              <p class="notification-message">
                <span v-if="notif.type === 'NEW_NOVEL'">
                  опубликовал новую новеллу «{{ notif.metadata?.novelTitle }}»
                </span>
                <span v-else-if="notif.type === 'NEW_CHAPTER'">
                  добавил главу {{ notif.metadata?.chapterNumber }} в «{{ notif.metadata?.novelTitle }}»
                </span>
              </p>
              <p class="created-at">
                {{ formatTime(notif.createdAt) }}
              </p>
            </div>
          </div>
        </div>
        
        <div v-if="!isLastPage" class="load-more-container">
          <button @click="loadMore" :disabled="isLoading" class="load-more-btn">
            {{ isLoading ? 'Загрузка...' : 'Загрузить ещё' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, onUnmounted } from 'vue';
import { useNotificationStore } from './notificationStore';
import { useRouter } from 'vue-router';
import type { NotificationResponseDto } from '@/types/notification';

const store = useNotificationStore();
const router = useRouter();

const isOpen = ref(false);

const notifications = computed(() => store.notifications);
const unreadCount = computed(() => store.unreadCount);
const isLastPage = computed(() => store.isLastPage);
const isLoading = computed(() => store.isLoading);

onMounted(() => {
    store.fetchNotifications(0);
    document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
    document.removeEventListener('click', handleClickOutside);
});

const toggleDropdown = () => {
    isOpen.value = !isOpen.value;
};

const handleClickOutside = (event: MouseEvent) => {
    const target = event.target as HTMLElement;
    if (!target.closest('.notification-bell-container')) {
        isOpen.value = false;
    }
};

const markAllRead = () => {
    store.markAllAsRead();
};

const loadMore = () => {
    store.fetchNotifications(store.currentPage + 1);
};

const formatTime = (timeStr: string) => {
    try {
        return new Date(timeStr).toLocaleString('ru-RU', {
            day: 'numeric',
            month: 'short',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch {
        return timeStr;
    }
};

const handleNotificationClick = async (notif: NotificationResponseDto) => {
    if (!notif.isRead) {
        await store.markAsRead(notif.id);
    }
    isOpen.value = false;
    
    if (notif.type === 'NEW_NOVEL') {
        const novelId = notif.metadata?.novelId;
        if (novelId) {
            router.push(`/novel/${novelId}`);
        }
    } else if (notif.type === 'NEW_CHAPTER') {
        const novelId = notif.metadata?.novelId;
        const chapterId = notif.metadata?.chapterId;
        if (novelId && chapterId) {
            router.push(`/novels/${novelId}/chapter/${chapterId}`);
        } else if (novelId) {
            router.push(`/novel/${novelId}`);
        }
    }
};
</script>

<style scoped>
.notification-bell-container {
  position: relative;
  display: flex;
  align-items: center;
}

.bell-button {
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  transition: background var(--transition-base), color var(--transition-base);
  outline: none;
}

.bell-button:hover {
  background: var(--hover-dropdowb);
  color: var(--text-header);
}

.bell-icon {
  width: 22px;
  height: 22px;
}

.unread-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  background-color: var(--unread-dot, #ef4444);
  color: #ffffff;
  font-size: 0.7rem;
  font-weight: 700;
  min-width: 16px;
  height: 16px;
  border-radius: 9999px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  border: 2px solid var(--bg-header);
  box-shadow: 0 0 8px rgba(239, 68, 68, 0.4);
}

.notifications-dropdown {
  position: absolute;
  top: 50px;
  right: 0;
  background: var(--bg-dropdown);
  backdrop-filter: blur(24px) saturate(160%);
  border: 1px solid var(--surface-glass-border);
  border-radius: 12px;
  box-shadow: var(--shadow-elevated);
  z-index: 1000;
  width: 320px;
  overflow: hidden;
  animation: dropdownIn 0.18s var(--transition-base) both;
}

@keyframes dropdownIn {
  from { opacity: 0; transform: translateY(-6px) scale(0.97); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

.dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.dropdown-title {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--text-header);
  margin: 0;
}

.mark-all-btn {
  background: none;
  border: none;
  font-size: 0.78rem;
  color: var(--btn-plus);
  cursor: pointer;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
  transition: background var(--transition-base), color var(--transition-base);
}

.mark-all-btn:hover {
  background: var(--hover-dropdowb);
}

.notifications-list {
  max-height: 360px;
  overflow-y: auto;
}

.empty-state {
  padding: 24px;
  text-align: center;
  color: var(--text-muted);
  font-size: 0.88rem;
}

.notification-item {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-subtle);
  cursor: pointer;
  transition: background var(--transition-base);
}

.notification-item:hover {
  background: var(--hover-dropdowb);
}

.notification-item.unread {
  background: rgba(99, 102, 241, 0.05);
}

.notification-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.avatar-wrapper {
  flex-shrink: 0;
}

.avatar-img {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--border-color);
  color: var(--text-header);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.9rem;
}

.text-wrapper {
  flex: 1;
  min-width: 0;
}

.sender-name {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--text-header);
  margin: 0 0 2px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notification-message {
  font-size: 0.82rem;
  color: var(--text-muted);
  margin: 0 0 4px 0;
  line-height: 1.35;
}

.created-at {
  font-size: 0.72rem;
  color: var(--text-muted);
  opacity: 0.7;
  margin: 0;
}

.load-more-container {
  padding: 10px;
  text-align: center;
}

.load-more-btn {
  background: none;
  border: none;
  color: var(--btn-plus);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 6px;
  transition: background var(--transition-base);
}

.load-more-btn:hover {
  background: var(--hover-dropdowb);
}

.scrollbar {
  scrollbar-width: thin;
  scrollbar-color: var(--primary-glow) transparent;
}
</style>
