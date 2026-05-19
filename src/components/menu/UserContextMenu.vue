<script setup lang="ts">
import {nextTick, onMounted, onUnmounted, ref} from 'vue';
import {useSocialStore} from "@/components/social/socialStore.ts";
import {useMessengerStore} from "@/components/chat/messengerStore.ts";

const social = useSocialStore();
const messenger = useMessengerStore();
const menuElement = ref<HTMLElement | null>(null);


const isVisible = ref(false);
const x = ref(0);
const y = ref(0);
const targetUserId = ref<number | null>(null);
const targetUsername = ref<string>("");


const openMenu = async (event: MouseEvent, userId: number, username: string) => {
  event.preventDefault();
  targetUserId.value = userId;
  targetUsername.value = username;
  isVisible.value = true;

  
  await nextTick();

  if (menuElement.value) {
    const menuWidth = menuElement.value.offsetWidth;
    const menuHeight = menuElement.value.offsetHeight;
    const padding = 10; 

    let posX = event.clientX;
    let posY = event.clientY;

    
    if (posX + menuWidth > window.innerWidth) {
      posX = window.innerWidth - menuWidth - padding;
    }

    
    if (posY + menuHeight > window.innerHeight) {
      posY = window.innerHeight - menuHeight - padding;
    }

    
    x.value = Math.max(padding, posX);
    y.value = Math.max(padding, posY);
  }
};


const closeMenu = () => {
  isVisible.value = false;
  targetUserId.value = null;
};

onMounted(() => {
  document.addEventListener('click', closeMenu);
  window.addEventListener('keydown', handleEsc, { capture: true });
});
onUnmounted(() => {
  document.removeEventListener('click', closeMenu);
  window.removeEventListener('keydown', handleEsc, { capture: true });
});



const handleWriteMessage = async () => {
  if (!targetUserId.value) return;
  
  await messenger.startPrivateChat(targetUserId.value);
  closeMenu();
  
};

const handleEsc = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && isVisible.value) {
    closeMenu();
    e.stopImmediatePropagation();
  }
};


defineExpose({ openMenu });
</script>

<template>
  <Teleport to="body">
    <Transition name="fade-fast">
      <div
        v-if="isVisible && targetUserId && social.isLoaded"
        ref="menuElement"
        class="context-menu"
        :style="{ top: y + 'px', left: x + 'px' }"
        @click.stop
      >
        <div class="menu-header">
          <span>{{ targetUsername }}</span>
        </div>

        
        <button class="menu-item highlight" @click="handleWriteMessage">
          💬 Написать сообщение
        </button>

        <div class="menu-divider"></div>

        
        <template v-if="social.isBlocked(targetUserId)">
          <button class="menu-item danger" @click="social.toggleBlock(targetUserId)">
            🔓 Разблокировать
          </button>
        </template>

        <!-- 3. Основные действия (Если НЕ заблокирован) -->
        <template v-else>
          <!-- Подписка -->
          <button class="menu-item" @click="social.toggleFollow(targetUserId)">
            {{ social.isFollowing(targetUserId) ? '👀 Отписаться' : '👀 Подписаться' }}
          </button>

          <!-- Дружба -->
          <template v-if="social.isFriend(targetUserId)">
            <button class="menu-item" @click="social.removeFriendOrRequest(targetUserId)">
              🤝 Удалить из друзей
            </button>
            <button class="menu-item" @click="social.toggleCloseFriend(targetUserId)">
              {{ social.isCloseFriend(targetUserId) ? '☆ Убрать из близких' : '⭐ В лучшие друзья' }}
            </button>
          </template>

          <button v-else-if="social.isOutgoing(targetUserId)" class="menu-item" @click="social.removeFriendOrRequest(targetUserId)">
            ⏳ Отменить заявку
          </button>

          <template v-else-if="social.isIncoming(targetUserId)">
            <button class="menu-item success" @click="social.acceptFriendReq(targetUserId)">✓ Принять в друзья</button>
            <button class="menu-item danger" @click="social.removeFriendOrRequest(targetUserId)">✕ Отклонить</button>
          </template>

          <button v-else class="menu-item" @click="social.sendFriendReq(targetUserId)">
            ➕ Добавить в друзья
          </button>

          <div class="menu-divider"></div>

          <!-- Блокировка -->
          <button class="menu-item danger" @click="social.toggleBlock(targetUserId)">
            🚫 Заблокировать
          </button>
        </template>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.context-menu {
  position: fixed;
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  box-shadow: 0 10px 30px var(--shadow-color);
  min-width: 220px;
  display: flex;
  flex-direction: column;
  padding: 8px;
  z-index: 99999;
}

.menu-header {
  padding: 10px 14px;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-muted);
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu-item {
  background: none;
  border: none;
  color: var(--text-header);
  padding: 10px 14px;
  text-align: left;
  font-size: 0.95rem;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
}

.menu-item:hover { background: var(--hover-dropdowb); }
.menu-item.highlight { color: var(--btn-plus); font-weight: 600;}
.menu-item.highlight:hover { background: rgba(99, 102, 241, 0.1); }
.menu-item.danger { color: #ef4444; }
.menu-item.danger:hover { background: rgba(239, 68, 68, 0.1); }
.menu-item.success { color: #10b981; }
.menu-item.success:hover { background: rgba(16, 185, 129, 0.1); }

.menu-divider {
  height: 1px;
  background: var(--border-color);
  margin: 8px 0;
}

.fade-fast-enter-active, .fade-fast-leave-active { transition: opacity 0.15s, transform 0.15s; }
.fade-fast-enter-from, .fade-fast-leave-to { opacity: 0; transform: scale(0.95); }
</style>
