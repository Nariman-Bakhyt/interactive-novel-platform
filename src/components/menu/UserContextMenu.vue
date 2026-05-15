<script setup lang="ts">
import {ref, computed, onMounted, onUnmounted, nextTick} from 'vue';
import { useSocialStore } from "@/components/social/socialStore.ts";
import { useRouter } from "vue-router";
import {useMessengerStore} from "@/components/chat/messengerStore.ts";

const social = useSocialStore();
const messenger = useMessengerStore();
const router = useRouter();
const menuElement = ref<HTMLElement | null>(null);

// Глобальное состояние меню
const isVisible = ref(false);
const x = ref(0);
const y = ref(0);
const targetUserId = ref<number | null>(null);
const targetUsername = ref<string>("");

// Экспортируем метод открытия, чтобы его можно было вызывать из любого места
const openMenu = async (event: MouseEvent, userId: number, username: string) => {
  event.preventDefault();
  targetUserId.value = userId;
  targetUsername.value = username;
  isVisible.value = true;

  // Ждем, пока Vue добавит элемент в DOM
  await nextTick();

  if (menuElement.value) {
    const menuWidth = menuElement.value.offsetWidth;
    const menuHeight = menuElement.value.offsetHeight;
    const padding = 10; // Отступ от края экрана

    let posX = event.clientX;
    let posY = event.clientY;

    // Проверка правой границы: если меню выходит за край, сдвигаем его влево
    if (posX + menuWidth > window.innerWidth) {
      posX = window.innerWidth - menuWidth - padding;
    }

    // Проверка нижней границы: если меню выходит вниз, сдвигаем его вверх
    if (posY + menuHeight > window.innerHeight) {
      posY = window.innerHeight - menuHeight - padding;
    }

    // Дополнительная защита: если кликнули слишком близко к левому или верхнему краю
    x.value = Math.max(padding, posX);
    y.value = Math.max(padding, posY);
  }
};

// Закрытие по клику куда угодно
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

// --- ДЕЙСТВИЯ ---

const handleWriteMessage = async () => {
  if (!targetUserId.value) return;
  // Используем твой готовый метод из messengerStore
  await messenger.startPrivateChat(targetUserId.value);
  closeMenu();
  // Если у тебя чат на отдельной странице, можно сделать: router.push('/chat')
};

const handleEsc = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && isVisible.value) {
    closeMenu();
    e.stopImmediatePropagation();
  }
};
// Чтобы сделать этот компонент доступным глобально,
// мы прокидываем openMenu наружу
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

        <!-- 1. Написать сообщение -->
        <button class="menu-item highlight" @click="handleWriteMessage">
          💬 Написать сообщение
        </button>

        <div class="menu-divider"></div>

        <!-- 2. Блокировка (Если заблокирован, показываем только разблокировку) -->
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
  background: var(--bg-editor-sheet, #2c2c2c);
  border: 1px solid var(--border-subtle, #3d3d3d);
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.5);
  min-width: 200px;
  display: flex;
  flex-direction: column;
  padding: 4px;
  z-index: 99999;
}

.menu-header {
  padding: 8px 12px;
  font-size: 0.85rem;
  color: #888;
  border-bottom: 1px solid #3d3d3d;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu-item {
  background: none;
  border: none;
  color: #e0e0e0;
  padding: 10px 12px;
  text-align: left;
  font-size: 0.9rem;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.1s;
}

.menu-item:hover { background: #3d3d3d; }
.menu-item.highlight { color: #3498db; font-weight: bold; }
.menu-item.danger { color: #e74c3c; }
.menu-item.danger:hover { background: rgba(231, 76, 60, 0.1); }
.menu-item.success { color: #2ecc71; }

.menu-divider {
  height: 1px;
  background: #3d3d3d;
  margin: 4px 0;
}

.fade-fast-enter-active, .fade-fast-leave-active { transition: opacity 0.1s, transform 0.1s; }
.fade-fast-enter-from, .fade-fast-leave-to { opacity: 0; transform: scale(0.95); }
</style>
