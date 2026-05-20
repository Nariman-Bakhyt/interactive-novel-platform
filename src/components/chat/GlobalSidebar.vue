<script setup lang="ts">
import {computed, inject, nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
import {useCommentStore} from "@/components/chat/commentStore.ts";
import {useMessengerStore} from "@/components/chat/messengerStore.ts";
import {type ConversationResponseDto, ConversationType} from "@/types/chat.ts";
import {useAuthStore} from "@/api/auth.ts";
import ChatCreationModal from './ChatCreationModal.vue';
import {useToastStore} from "@/components/toast/toastStore.ts";

const commentStore = useCommentStore();
const messengerStore = useMessengerStore();
const authStore = useAuthStore();
const toastStore = useToastStore();

const miniTab = ref<'CHATS' | 'COMMENTS'>('CHATS');
const openUserMenu = inject('openUserMenu') as (event: MouseEvent, userId: number, username: string) => void;

const isSidebarExpanded = ref(false);
const searchQuery = ref('');
const showChatInfo = ref(false);

const isMessengerVisible = ref(false);

onMounted(() => {
  window.addEventListener('open-messenger', () => {
    isMessengerVisible.value = true;
  });
});
onUnmounted(() => {
  window.removeEventListener('open-messenger', () => {
    isMessengerVisible.value = true;
  });
});

const filteredConversations = computed(() => {
  let list = messengerStore.conversations;
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase();
    list = list.filter(c => c.title.toLowerCase().includes(q));
  }
  return list;
});

onMounted(() => {
  window.addEventListener('keydown', closeActivePanels);
});

const handleChatClick = (conversationId: number) => {
  messengerStore.openChat(conversationId);
  showChatInfo.value = false;
  const chat = messengerStore.conversations.find(c => c.id === conversationId);
  if (chat && hasUnread(chat)) {
    messengerStore.markAsRead(conversationId);
  }
};

const menu = (event: MouseEvent, chat: ConversationResponseDto) => {
  if (chat.type !== ConversationType.GROUP) {
    const user = chat.members.find(c => c.userId !== authStore.userDetails?.id);
    if (user) openUserMenu(event, user.userId, user.username);
  }
};

const activeContext = computed(() => {
  if (commentStore.isOpen) {
    return {
      type: 'COMMENT',
      title: 'Обсуждение',
      subtitle: commentStore.targetType === 'BLOCK' ? 'Блок' : 'Глава',
      groupedItems: commentStore.groupedComments,
      isLoadingMore: commentStore.isLoadingMore,
      isLastPage: commentStore.isLastPage,
      isSending: commentStore.isSending,
      close: commentStore.closeChat,
      loadMore: commentStore.loadMoreComments,
      send: commentStore.send,
      remove: commentStore.removeComment,
      items: commentStore.groupedComments.value,
    };
  } else if (messengerStore.activeConversationId) {
    const chat = messengerStore.currentChat;
    return {
      type: 'CHAT',
      title: chat?.title || 'Личные сообщения',
      subtitle: chat?.type === ConversationType.GROUP ? 'Группа' : 'Приватный',
      groupedItems: messengerStore.groupedMessages,
      isLoadingMore: messengerStore.isLoadingMore,
      isLastPage: messengerStore.isLastPage,
      isSending: messengerStore.isSending,
      close: messengerStore.closeChat,
      loadMore: messengerStore.loadMoreMessages,
      send: (payload: { content: string, file?: File | null }) =>
        messengerStore.send(payload.content, payload.file ? [payload.file] : []),
      remove: messengerStore.removeMsg,
      items: messengerStore.groupedMessages.value,
    };
  }
  return null;
});

const setListRef = (el: any) => {
  if (commentStore.isOpen) commentStore.commentsListRef = el;
  if (messengerStore.activeConversationId) messengerStore.messagesListRef = el;
};

const newCommentText = ref('');
const selectedFile = ref<File | null>(null);
const fileInput = ref<HTMLInputElement | null>(null);
const zoomedImageUrl = ref<string | null>(null);
const zoomLevel = ref(1);

const isScrolling = ref(false);
let scrollTimeout: number | null = null;
const topObserverTarget = ref<HTMLElement | null>(null);
let topObserver: IntersectionObserver | null = null;
const showChatOptions = ref(false);

const showNewChatModal = ref(false);
const modalPurpose = ref<'CREATE' | 'ADD_MEMBERS'>('CREATE');

watch(() => commentStore.isOpen, (isOpen) => {
  if (isOpen) { messengerStore.closeChat(); miniTab.value = 'COMMENTS'; }
});

watch(() => messengerStore.activeConversationId, (id) => {
  if (id !== null) { commentStore.closeChat(); miniTab.value = 'CHATS'; }
});

watch(() => activeContext.value, async (newContext, oldContext) => {
  if (newContext && !oldContext) {
    await nextTick();
    setTimeout(() => { initStickyObserver(); initHistoryObserver(); }, 300);
  } else if (!newContext) {
    if (topObserver) topObserver.disconnect();
  }
});
watch(() => messengerStore.groupedMessages.value, (newItems) => {
  const activeId = messengerStore.activeConversationId;
  if (activeId && newItems && newItems.length > 0) {
    const chat = messengerStore.conversations.find(c => c.id === activeId);
    if (chat && hasUnread(chat)) {
      messengerStore.markAsRead(activeId);
    }
  }
}, { deep: true });

const initHistoryObserver = () => {
  if (!topObserverTarget.value) return;
  topObserver = new IntersectionObserver(async ([entry]) => {
    if (entry && entry.isIntersecting && activeContext.value && !activeContext.value.isLoadingMore && !activeContext.value.isLastPage) {
      const container = commentStore.isOpen ? commentStore.commentsListRef : messengerStore.messagesListRef;
      if (!container) return;
      const oldScrollHeight = container.scrollHeight;
      await activeContext.value.loadMore();
      await nextTick();
      const newScrollHeight = container.scrollHeight;
      container.scrollTop += (newScrollHeight - oldScrollHeight);
    }
  }, { root: commentStore.isOpen ? commentStore.commentsListRef : messengerStore.messagesListRef, threshold: 0.1 });
  topObserver.observe(topObserverTarget.value);
};

const initStickyObserver = () => {
  const container = commentStore.isOpen ? commentStore.commentsListRef : messengerStore.messagesListRef;
  if (!container) return;
  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      const header = entry.target.nextElementSibling;
      const badge = header?.querySelector('.date-badge');
      if (badge) badge.toggleAttribute('data-in-text', entry.isIntersecting);
    });
  }, { root: container, threshold: [0, 1] });
  container.querySelectorAll('.sticky-sentinel').forEach((el) => observer.observe(el));
};

const triggerFileSelect = () => fileInput.value?.click();
const onFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement;
  selectedFile.value = target.files?.[0] ?? null;
};

const handleSend = async () => {
  if (!activeContext.value || activeContext.value.isSending) return;
  await activeContext.value.send({ content: newCommentText.value, file: selectedFile.value });
  newCommentText.value = '';
  selectedFile.value = null;
};

const formatTime = (ts: string) => new Date(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

const handleScroll = () => {
  isScrolling.value = true;
  if (scrollTimeout) clearTimeout(scrollTimeout);
  scrollTimeout = window.setTimeout(() => { isScrolling.value = false; }, 1500);
};

const zoomImage = (url: string) => {
  zoomedImageUrl.value = url;
  zoomLevel.value = 1;
};
const closeZoom = () => {
  zoomedImageUrl.value = null;
  zoomLevel.value = 1;
};
const zoomIn = () => { zoomLevel.value += 0.25; };
const zoomOut = () => { if (zoomLevel.value > 0.5) zoomLevel.value -= 0.25; };

const onTyping = () => { if (activeContext.value?.type === 'CHAT') messengerStore.notifyTyping(); };

const hasUnread = (chat: any) => {
  if (!chat.lastMessageAt) return false;
  if (!chat.lastReadAt) return true;

  const lastMsg = new Date(chat.lastMessageAt).getTime();
  const lastRead = new Date(chat.lastReadAt).getTime();

  return lastMsg > lastRead;
};

const currentChat = computed(() => {
  if (!messengerStore.activeConversationId) return null;
  return messengerStore.conversations.find(c => c.id === messengerStore.activeConversationId) || null;
});

const handleDeleteChat = async () => {
  if (confirm("Вы уверены, что хотите удалить этот чат для себя?")) {
    await messengerStore.removeConversation(messengerStore.activeConversationId!);
    showChatOptions.value = false;
  }
};

const openNewChatModal = () => {
  modalPurpose.value = 'CREATE';
  showNewChatModal.value = true;
};
const openAddMembersModal = () => {
  modalPurpose.value = 'ADD_MEMBERS';
  showNewChatModal.value = true;
};
const handleMembersAdded = async (userIds: number[]) => {
  if (!messengerStore.activeConversationId) return;
  await messengerStore.addMemberToGroup(messengerStore.activeConversationId, userIds);
  toastStore.success("Участники добавлены!");
};

const closeActivePanels = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    if (showNewChatModal.value) { showNewChatModal.value = false; return; }
    if (zoomedImageUrl.value) { closeZoom(); return; }
    if (showChatInfo.value) { showChatInfo.value = false; return; }
    if (commentStore.isOpen) { commentStore.closeChat(); }
    else if (messengerStore.activeConversationId) { messengerStore.closeChat(); }
  }
};

const getGlowStyle = (id: number, hasAvatar: boolean) => {
  const hue = (id * 137.5) % 360;
  const color = `hsl(${hue}, 75%, 55%)`;
  const glow = `hsla(${hue}, 75%, 55%, var(--aura-opacity, 0.4))`;
  return {
    '--chat-accent': color,
    '--chat-glow': glow,
    'background-color': hasAvatar ? 'transparent' : color,
    'border': `2px solid ${color}`,
    'box-shadow': `0 0 var(--aura-blur, 10px) ${glow}`
  };
};

const handleLeaveGroup = async () => {
  if(confirm("Вы уверены, что хотите покинуть группу?")) {
    await messengerStore.leaveGroupChat(messengerStore.activeConversationId!);
  }
}

const handleKickMember = async (userId: number) => {
  if(confirm("Исключить пользователя?")) {
    await messengerStore.kickMemberFromGroup(messengerStore.activeConversationId!, userId);
    toastStore.info("Участник исключен");
  }
}

const hasAnyUnread = computed(() => {
  return messengerStore.conversations.some(chat => hasUnread(chat));
});

const isMyMessage = (item: any) => {
  const senderId = item.senderId !== undefined ? item.senderId : item.userId;
  return authStore.userDetails?.id === senderId;
};

const contextMenu = ref({
  show: false,
  x: 0,
  y: 0,
  targetId: null as number | null
});

const openContextMenu = (e: MouseEvent, id: number, item: any) => {
  if (!isMyMessage(item)) {
    const chat = currentChat.value;
    const isChatAdmin = chat?.members.find(m => m.userId === authStore.userDetails?.id)?.role === 'ADMIN';
    if (!isChatAdmin) return;
  }

  e.preventDefault();
  e.stopPropagation();

  contextMenu.value = {
    show: true,
    x: e.clientX,
    y: e.clientY,
    targetId: id
  };

  const close = () => {
    contextMenu.value.show = false;
    document.removeEventListener('click', close);
  };

  setTimeout(() => {
    document.addEventListener('click', close);
  }, 50);
};

const handleDelete = async () => {
  const { targetId } = contextMenu.value;
  if (!targetId || !activeContext.value) return;

  try {
    await activeContext.value.remove(targetId);
    contextMenu.value.show = false;
  } catch (error) {
    console.error("Ошибка удаления:", error);
    toastStore.error("Вы не можете удалить это сообщение");
  }
};

onUnmounted(() => {
  window.removeEventListener('keydown', closeActivePanels);
  if (topObserver) topObserver.disconnect();
});
</script>

<template>
  <div class="omnichannel-wrapper" :class="{ 'is-active': isMessengerVisible }">
    <Transition name="fade">
      <button
        v-if="!isMessengerVisible"
        class="floating-messenger-btn"
        @click="isMessengerVisible = true"
        title="Открыть чаты"
      >
        <span class="icon">💬</span>
        <span v-if="hasAnyUnread" class="global-unread-dot"></span>
      </button>
    </Transition>
    <Transition name="slide-right">
      <div v-show="isMessengerVisible" class="messenger-container">
        <Transition name="slide">
          <aside v-if="activeContext"
                 class="global-sidebar"
                 :class="{ 'is-shifted': isSidebarExpanded }"
          >
            <header
              class="sidebar-header"
            >
              <div
                class="header-info"
                :class="{ 'clickable': activeContext.type === 'CHAT' }"
                @click="activeContext.type === 'CHAT' ? showChatInfo = !showChatInfo : null"
              >
                <h3>
                  {{ activeContext.title }}
                  <span v-if="activeContext.type === 'CHAT'" class="dropdown-icon">
                {{ showChatInfo ? '▲' : '▼' }}
              </span>
                </h3>
                <span class="target-badge">{{ activeContext.subtitle }}</span>
              </div>

              <div class="header-actions">
                <div v-if="activeContext.type === 'CHAT'" class="chat-options-wrapper">
                  <button class="options-btn" @click="showChatOptions = !showChatOptions">⋮</button>
                  <div v-if="showChatOptions && currentChat" class="chat-options-menu">
                    <button @click="messengerStore.toggleSettings(messengerStore.activeConversationId!, !currentChat.isMuted, currentChat.isPinned)">
                      {{ currentChat.isMuted ? 'Включить звук' : 'Без звука' }}
                    </button>
                    <button @click="messengerStore.toggleSettings(messengerStore.activeConversationId!, currentChat.isMuted, !currentChat.isPinned)">
                      {{ currentChat.isPinned ? 'Открепить' : 'Закрепить' }}
                    </button>
                    <button class="danger" @click="handleDeleteChat">Удалить чат</button>
                  </div>
                </div>
                <button class="close-btn" @click="activeContext.close">✕</button>
              </div>
            </header>

            <div v-if="showChatInfo && currentChat" class="chat-info-panel">
              <div class="info-hero-row">
                <div class="chat-avatar-wrapper avatar-hero"
                     :style="getGlowStyle(currentChat.id, !!currentChat.avatarUrl)">
                  <img v-if="currentChat.avatarUrl" :src="currentChat.avatarUrl" class="chat-avatar" />
                  <div v-else class="chat-avatar-placeholder">{{ currentChat.title.charAt(0).toUpperCase() }}</div>
                </div>
                <div class="hero-text">
                  <h2>{{ currentChat.title }}</h2>
                  <p class="members-count">{{ currentChat.members.length }} участников</p>
                </div>
              </div>

              <div v-if="currentChat.type === 'GROUP'" class="group-management">
                <button class="add-btn-full" @click="openAddMembersModal">
                  + Добавить участников
                </button>

                <div class="members-list-section">
                  <h4>Участники</h4>
                  <div v-for="member in currentChat.members" :key="member.userId" class="member-card">
                    <div class="member-info-main">
                      <span class="member-name-text">{{ member.username }}</span>
                      <span v-if="member.role === 'ADMIN'" class="admin-badge-mini">Admin</span>
                    </div>

                    <button
                      v-if="member.userId !== authStore.userDetails?.id"
                      @click="handleKickMember(member.userId)"
                      class="member-kick-icon"
                      title="Исключить"
                    >✕</button>
                  </div>
                </div>

                <button class="leave-btn-outline" @click="handleLeaveGroup">Покинуть группу</button>
              </div>
            </div>

            <div v-show="!showChatInfo" class="comments-list" :ref="setListRef" @scroll="handleScroll">
              <div ref="topObserverTarget" class="chat-history-trigger">
                <span v-if="activeContext.isLoadingMore" class="spinner-small history-spinner"></span>
              </div>

              <div v-for="(group, date) in activeContext.groupedItems" :key="date" class="comment-group">
                <div class="sticky-sentinel"></div>
                <div class="date-sticky-header" :class="{ 'is-scrolling-active': isScrolling }">
                  <span class="date-badge">{{ date }}</span>
                </div>

                <div v-for="item in group" :key="item.id"
                     class="comment-item-wrapper"
                     :class="{ 'is-mine': isMyMessage(item), 'is-others': !isMyMessage(item) }">
                  <div class="comment-bubble"
                       :class="{ 'bubble-mine': isMyMessage(item), 'bubble-others': !isMyMessage(item) }"
                       @contextmenu.prevent="openContextMenu($event, item.id, item)">
                    <span v-if="!isMyMessage(item)" class="user-badge">{{ ('username' in item) ? item.username : item.senderUsername }}</span>
                    <div v-if="item.metadata?.images?.length" class="comment-images">
                      <img v-for="url in item.metadata.images" :key="url" :src="url" class="comment-img" @click.stop="zoomImage(url)">
                    </div>
                    <p class="comment-body">{{ item.content }}</p>
                    <div class="comment-footer"><span class="comment-date">{{ formatTime(item.timestamp) }}</span></div>
                  </div>
                </div>
              </div>
            </div>

            <div v-show="!showChatInfo" class="sidebar-input-area">
              <input type="file" ref="fileInput" @change="onFileChange" hidden accept="image/*">
              <div class="input-wrapper">
                <div class="input-row">
                  <button class="attach-btn" @click="triggerFileSelect" :class="{ 'has-file': !!selectedFile }">📷</button>
                  <textarea v-model="newCommentText" @keydown.enter.prevent="handleSend" @input="onTyping" placeholder="Написать..." rows="1"></textarea>
                  <button class="send-btn" :disabled="(!newCommentText.trim() && !selectedFile) || activeContext.isSending" @click="handleSend">
                    <span v-if="activeContext.isSending" class="spinner-small"></span><span v-else>▲</span>
                  </button>
                </div>
              </div>
            </div>
          </aside>
        </Transition>

        <Teleport to="body">
          <Transition name="fade">
            <ChatCreationModal
              v-if="showNewChatModal"
              :purpose="modalPurpose"
              :excludeIds="currentChat?.members.map(m => m.userId) || []"
              @close="showNewChatModal = false"
              @add-members="handleMembersAdded"
            />
          </Transition>
        </Teleport>

        <nav class="mini-sidebar" :class="{ 'is-expanded': isSidebarExpanded }">
          <div class="sidebar-top-actions">
            <button class="hide-messenger-btn"
                    title="Скрыть мессенджер"
                    @click="()=>{
                      if (messengerStore.activeConversationId) { messengerStore.closeChat(); }
                      isMessengerVisible = false ;}">
              ✕
            </button>
            <button class="toggle-expand-btn" @click="isSidebarExpanded = !isSidebarExpanded">
              {{ isSidebarExpanded ? '◀' : '▶' }}
            </button>
          </div>

          <div class="mini-tabs" :class="{ 'row-mode': isSidebarExpanded }">
            <button class="tab-btn" :class="{ active: miniTab === 'CHATS' }" @click="miniTab = 'CHATS'" title="Личные сообщения">💬 <span v-if="isSidebarExpanded" class="tab-label">Чаты</span></button>
            <button class="tab-btn" :class="{ active: miniTab === 'COMMENTS' }" @click="miniTab = 'COMMENTS'" title="Обсуждения">📚 <span v-if="isSidebarExpanded" class="tab-label">Книги</span></button>
          </div>

          <div v-if="miniTab === 'CHATS' && isSidebarExpanded" class="search-box">
            <input v-model="searchQuery" type="text" placeholder="Поиск чатов..." />
          </div>

          <div v-if="miniTab === 'CHATS'" class="avatar-list">
            <button class="action-btn-circle" :class="{ 'expanded-btn': isSidebarExpanded }" title="Новый чат" @click="openNewChatModal">
              <span class="icon" style="font-size: 1.25rem;">+</span>
              <span v-if="isSidebarExpanded" class="btn-label">Создать чат</span>
            </button>

            <div
              v-for="chat in filteredConversations"
              :key="chat.id"
              class="chat-avatar-wrapper"
              :class="{ 'is-active': messengerStore.activeConversationId === chat.id, 'expanded': isSidebarExpanded }"
              @contextmenu.prevent="menu($event, chat)"
              @click="handleChatClick(chat.id)"
            >
              <div class="avatar-circle-container" :style="getGlowStyle(chat.id, !!chat.avatarUrl)">
                <img v-if="chat.avatarUrl" :src="chat.avatarUrl" class="chat-avatar" />
                <div v-else class="chat-avatar-placeholder">
                  {{ chat.title.charAt(0).toUpperCase() }}
                </div>
                <span v-if="hasUnread(chat)" class="unread-badge"></span>
              </div>

              <div v-if="isSidebarExpanded" class="chat-info-text">
                <span class="chat-list-title">{{ chat.title }}</span>
                <span v-if="chat.isPinned" class="pin-indicator">📌</span>
              </div>
            </div>
          </div>
        </nav>
      </div>
    </Transition>
  </div>

  <Teleport to="body">
    <div v-if="zoomedImageUrl" class="image-lightbox" @click="closeZoom">
      <div class="zoom-controls" @click.stop>
        <button class="zoom-btn" @click.stop="zoomOut" title="Уменьшить">−</button>
        <span class="zoom-level">{{ Math.round(zoomLevel * 100) }}%</span>
        <button class="zoom-btn" @click.stop="zoomIn" title="Увеличить">+</button>
      </div>
      <button class="close-lightbox" @click.stop="closeZoom">✕</button>

      <div class="image-container" @click.stop>
        <img :src="zoomedImageUrl" class="full-image" :style="{ transform: `scale(${zoomLevel})` }" />
      </div>
    </div>
  </Teleport>
  <Teleport to="body">
    <div
      v-if="contextMenu.show"
      class="context-menu"
      :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }"
    >
      <div class="menu-item delete" @click="handleDelete">
        🗑 Удалить
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.omnichannel-wrapper {
  position: absolute;
  top: 60px;
  right: 0;
  height: calc(100vh - 60px);
  z-index: 1000;
  pointer-events: none;
}
.omnichannel-wrapper.is-active {
  position: relative;
  top: 0;
  height: 100%;
}
.messenger-container, .floating-messenger-btn {
  pointer-events: auto;
}
.messenger-container {
  display: flex;
  height: 100%;
}


.mini-sidebar {
  width: 76px;
  background-color: var(--bg-dropdown);
  border-left: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 20;
}
.mini-sidebar.is-expanded {
  width: 260px;
  align-items: stretch;
  padding: 16px;
}
.sidebar-top-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
  align-items: center;
}
.mini-sidebar.is-expanded .sidebar-top-actions {
  flex-direction: row;
  justify-content: space-between;
}
.hide-messenger-btn, .toggle-expand-btn {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 1.2rem;
  transition: color 0.2s;
}
.hide-messenger-btn:hover, .toggle-expand-btn:hover {
  color: var(--text-header);
}
.mini-tabs {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
  width: 100%;
  align-items: center;
}
.mini-tabs.row-mode {
  flex-direction: row;
  justify-content: center;
  gap: 24px;
}
.tab-btn {
  background: none;
  border: none;
  font-size: 1.35rem;
  color: var(--text-muted);
  cursor: pointer;
  transition: opacity 0.2s, transform 0.2s;
  opacity: 0.5;
  display: flex;
  align-items: center;
  gap: 8px;
}
.tab-btn:hover { opacity: 1; color: var(--text-header); }
.tab-btn.active {
  opacity: 1;
  transform: scale(1.1);
  color: var(--btn-plus);
}
.tab-label { font-size: 0.95rem; font-weight: 500; }
.search-box {
  margin-bottom: 16px;
  width: 100%;
}
.search-box input {
  width: 100%;
  padding: 10px 14px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: var(--bg-main);
  color: var(--text-header);
  outline: none;
  transition: border-color 0.2s;
}
.search-box input:focus { border-color: var(--btn-plus); }
.avatar-list {
  flex: 1;
  width: 100%;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  scrollbar-width: none;
}
.avatar-list::-webkit-scrollbar { display: none; }
.action-btn-circle {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background-color: var(--bg-main);
  border: 1px dashed var(--border-color);
  color: var(--text-header);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}
.action-btn-circle:hover {
  background: var(--hover-dropdowb);
  border-style: solid;
  border-color: var(--text-muted);
}
.action-btn-circle.expanded-btn {
  width: 100%;
  border-radius: 12px;
  height: 48px;
}
.btn-label { font-size: 0.95rem; font-weight: 500; }

.chat-avatar-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px;
  border-radius: 16px;
  cursor: pointer;
  transition: background 0.2s;
  width: calc(100% - 12px);
  justify-content: center;
}
.chat-avatar-wrapper.expanded { justify-content: flex-start; padding: 8px 12px; width: 100%; }
.chat-avatar-wrapper:hover, .chat-avatar-wrapper.is-active { background: var(--hover-dropdowb); }
.chat-avatar-wrapper.avatar-hero { width: 64px; height: 64px; border-radius: 50%; cursor: default; background: transparent !important; border: 2px solid var(D--chat-accent); }
.chat-avatar-wrapper.avatar-hero:hover { background: transparent; }

.avatar-circle-container {
  position: relative;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.chat-avatar { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
.chat-avatar-placeholder { font-size: 1.3rem; font-weight: bold; color: white; }
.unread-badge {
  position: absolute;
  top: 0;
  right: 0;
  width: 14px;
  height: 14px;
  background-color: #ef4444;
  border-radius: 50%;
  border: 2px solid var(--bg-dropdown);
}

.chat-info-text { display: flex; flex-direction: column; overflow: hidden; white-space: nowrap; flex: 1; }
.chat-list-title { font-size: 0.95rem; color: var(--text-header); font-weight: 600; text-overflow: ellipsis; overflow: hidden; }
.pin-indicator { font-size: 0.8rem; margin-top: 2px; }


.global-sidebar {
  width: 360px;
  height: 100%;
  background-color: var(--bg-editor-sheet);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  box-shadow: -4px 0 16px var(--shadow-color);
  transition: width 0.3s;
  border-right: 1px solid var(--border-color);
}
.sidebar-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-dropdown);
  z-index: 10;
  position: sticky;
  top: 0;
}
.header-info h3 { margin: 0; font-size: 1.15rem; color: var(--text-header); font-weight: 600; display: flex; align-items: center; gap: 8px;}
.header-info.clickable { cursor: pointer; }
.dropdown-icon { font-size: 0.75rem; color: var(--text-muted); }
.target-badge { font-size: 0.75rem; color: var(--btn-plus); text-transform: uppercase; font-weight: bold; margin-top: 4px; display: inline-block; }
.header-actions { display: flex; align-items: center; gap: 12px; }
.options-btn { background: none; border: none; color: var(--text-muted); font-size: 1.5rem; font-weight: bold; cursor: pointer; padding: 4px 8px; border-radius: 6px;}
.options-btn:hover { background: var(--hover-dropdowb); color: var(--text-header); }
.close-btn { background: none; border: none; color: var(--text-muted); font-size: 1.25rem; cursor: pointer; line-height: 1; padding: 4px;}
.close-btn:hover { color: var(--text-header); }

.chat-options-wrapper { position: relative; }
.chat-options-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  box-shadow: 0 4px 20px var(--shadow-color);
  display: flex;
  flex-direction: column;
  min-width: 180px;
  overflow: hidden;
  z-index: 100;
}
.chat-options-menu button {
  background: none;
  border: none;
  padding: 12px 16px;
  text-align: left;
  color: var(--text-header);
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 500;
}
.chat-options-menu button:hover { background: var(--hover-dropdowb); }
.chat-options-menu button.danger { color: #ef4444; }

.chat-info-panel {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 32px;
}
.info-hero-row { display: flex; align-items: center; gap: 20px; }
.hero-text h2 { margin: 0 0 4px; font-size: 1.4rem; color: var(--text-header); font-weight: 700; }
.members-count { margin: 0; font-size: 0.9rem; color: var(--text-muted); font-weight: 500; }

.group-management { display: flex; flex-direction: column; gap: 24px; flex: 1; }
.add-btn-full {
  width: 100%;
  padding: 12px;
  border-radius: 12px;
  font-size: 0.95rem;
  cursor: pointer;
  font-weight: 600;
  background: var(--btn-plus);
  color: white;
  border: none;
  transition: all 0.2s;
}
.add-btn-full:hover { background: var(--btn-plus-hover); transform: translateY(-1px); }
.members-list-section h4 { margin: 0 0 16px; color: var(--text-muted); font-size: 0.9rem; text-transform: uppercase; letter-spacing: 0.5px; }
.member-card { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid var(--border-color); }
.member-card:last-child { border-bottom: none; }
.member-info-main { display: flex; align-items: center; gap: 10px; }
.member-name-text { color: var(--text-header); font-size: 1rem; font-weight: 500; }
.admin-badge-mini { background: var(--btn-plus); color: white; font-size: 0.65rem; padding: 2px 6px; border-radius: 6px; font-weight: bold; }
.member-kick-icon { background: none; border: none; color: var(--text-muted); cursor: pointer; font-size: 1.1rem; padding: 4px; }
.member-kick-icon:hover { color: #ef4444; }

.leave-btn-outline {
  background: none;
  border: 1px solid #ef4444;
  color: #ef4444;
  margin-top: auto;
  width: 100%;
  padding: 12px;
  border-radius: 12px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.leave-btn-outline:hover { background: #ef4444; color: white; }


.comments-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  scrollbar-width: thin;
  scrollbar-color: var(--border-color) transparent;
}
.comment-item-wrapper { display: flex; width: 100%; margin-bottom: 16px; }
.comment-item-wrapper.is-mine { justify-content: flex-end; }
.comment-item-wrapper.is-others { justify-content: flex-start; }
.comment-bubble { max-width: 80%; padding: 12px 16px; box-shadow: 0 2px 8px var(--shadow-color); word-break: break-word; overflow-wrap: anywhere; }
.bubble-mine { background: var(--btn-plus); color: white; border: 1px solid rgba(255,255,255,0.1); border-radius: 18px 18px 4px 18px; }
.bubble-mine .comment-body { color: white; }
.bubble-mine .comment-date { color: rgba(255,255,255,0.7); }
.bubble-others { background: var(--bg-dropdown); border: 1px solid var(--border-color); border-radius: 18px 18px 18px 4px; }
.comment-body { margin: 0; font-size: 0.95rem; line-height: 1.5; color: var(--text-header); }
.user-badge { font-weight: 700; color: var(--btn-plus); font-size: 0.85rem; margin-bottom: 6px; display: block; }
.comment-images { margin-top: 12px; display: flex; flex-wrap: wrap; gap: 8px; }
.comment-img { max-width: 100%; max-height: 200px; border-radius: 12px; cursor: zoom-in; object-fit: cover; border: 1px solid var(--border-color); }
.comment-footer { display: flex; justify-content: flex-end; margin-top: 6px; }
.comment-date { font-size: 0.75rem; color: var(--text-muted); }

.date-sticky-header { position: sticky; top: -1px; z-index: 20; display: flex; justify-content: center; padding: 12px 0; }
.date-badge {
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  color: var(--text-muted);
  font-size: 0.75rem;
  padding: 4px 14px;
  border-radius: 16px;
  font-weight: 600;
  opacity: 0;
  transition: opacity 0.3s;
  box-shadow: 0 2px 8px var(--shadow-color);
}
.date-badge[data-in-text], .is-scrolling-active .date-badge { opacity: 1; }

.sidebar-input-area {
  padding: 16px 20px 24px;
  border-top: 1px solid var(--border-color);
  background: var(--bg-dropdown);
}
.input-row { display: flex; align-items: flex-end; gap: 12px; }
.input-wrapper textarea {
  flex: 1;
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  padding: 12px 16px;
  color: var(--text-header);
  resize: none;
  font-size: 0.95rem;
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s, box-shadow 0.2s;
  max-height: 120px;
}
.input-wrapper textarea:focus { border-color: var(--btn-plus); box-shadow: 0 0 0 2px rgba(var(--btn-plus-rgb, 100, 100, 255), 0.2); }
.attach-btn { background: none; border: none; font-size: 1.4rem; cursor: pointer; color: var(--text-muted); transition: color 0.2s; padding: 8px;}
.attach-btn:hover { color: var(--text-header); }
.attach-btn.has-file { color: var(--btn-plus); }
.send-btn {
  background: var(--btn-plus);
  color: white;
  border: none;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.2s, transform 0.2s;
}
.send-btn:hover:not(:disabled) { background: var(--btn-plus-hover); transform: translateY(-2px); }
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.floating-messenger-btn {
  position: fixed;
  bottom: 24px;
  right: 24px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background-color: var(--btn-plus);
  color: white;
  border: none;
  box-shadow: 0 4px 16px var(--shadow-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.75rem;
  cursor: pointer;
  z-index: 1000;
  transition: transform 0.2s, box-shadow 0.2s;
}
.floating-messenger-btn:hover { transform: scale(1.05); box-shadow: 0 6px 20px var(--shadow-color); }
.global-unread-dot {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 12px;
  height: 12px;
  background-color: #ef4444;
  border-radius: 50%;
  border: 2px solid var(--btn-plus);
}

.slide-enter-active, .slide-leave-active, .slide-right-enter-active, .slide-right-leave-active { transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.slide-enter-from, .slide-leave-to, .slide-right-enter-from, .slide-right-leave-to { transform: translateX(100%); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }


.image-lightbox {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
  z-index: 99999;
  pointer-events: auto;
}
.image-container {
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: visible;
}
.full-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  transition: transform 0.2s cubic-bezier(0.2, 0, 0, 1);
  transform-origin: center center;
}
.zoom-controls {
  position: absolute;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(0, 0, 0, 0.6);
  padding: 8px 16px;
  border-radius: 24px;
  color: white;
  z-index: 100000;
  backdrop-filter: blur(4px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}
.zoom-btn {
  background: rgba(255, 255, 255, 0.1);
  border: none;
  color: white;
  font-size: 1.5rem;
  cursor: pointer;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.2s;
  line-height: 1;
}
.zoom-btn:hover { background: rgba(255, 255, 255, 0.3); }
.zoom-level {
  font-size: 0.95rem;
  font-weight: 500;
  min-width: 48px;
  text-align: center;
  user-select: none;
}
.close-lightbox {
  position: absolute;
  top: 24px;
  right: 24px;
  background: rgba(0, 0, 0, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: white;
  font-size: 1.5rem;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100000;
  transition: background 0.2s;
}
.close-lightbox:hover { background: rgba(255, 255, 255, 0.3); }

.context-menu {
  position: fixed;
  background: var(--bg-dropdown);
  border: 1px solid var(--border-color);
  box-shadow: 0 4px 12px var(--shadow-color);
  border-radius: 8px;
  padding: 4px;
  z-index: 9999;
  min-width: 160px;
}

.menu-item {
  padding: 10px 16px;
  cursor: pointer;
  font-size: 0.9rem;
  border-radius: 6px;
  transition: background 0.2s;
  font-weight: 500;
  color: var(--text-header);
}

.menu-item:hover {
  background: var(--hover-dropdowb);
}

.menu-item.delete {
  color: #ef4444;
}
</style>
