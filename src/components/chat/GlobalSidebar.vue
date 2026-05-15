<script setup lang="ts">
import { computed, inject, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useCommentStore } from "@/components/chat/commentStore.ts";
import router from "@/router";
import { useMessengerStore } from "@/components/chat/messengerStore.ts";
import { type ConversationResponseDto, ConversationType, ConversationMembersRole } from "@/types/chat.ts";
import { useAuthStore } from "@/api/auth.ts";
import ChatCreationModal from './ChatCreationModal.vue';
import { useToastStore } from "@/components/toast/toastStore.ts"; // Для уведомлений

const commentStore = useCommentStore();
const messengerStore = useMessengerStore();
const authStore = useAuthStore();
const toastStore = useToastStore();

const miniTab = ref<'CHATS' | 'COMMENTS'>('CHATS');
const openUserMenu = inject('openUserMenu') as (event: MouseEvent, userId: number, username: string) => void;

// === НОВЫЕ СОСТОЯНИЯ ===
const isSidebarExpanded = ref(false); // Выдвинут ли левый сайдбар
const searchQuery = ref(''); // Поиск по чатам
const showChatInfo = ref(false); // Открыта ли инфа о чате
const newMemberId = ref<number | null>(null); // ID для добавления нового юзера

// === ФИЛЬТРАЦИЯ ЧАТОВ ===
const filteredConversations = computed(() => {
  let list = messengerStore.conversations;
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase();
    list = list.filter(c => c.title.toLowerCase().includes(q));
  }
  return list;
});

onMounted(() => {
  messengerStore.loadMyChats();
  window.addEventListener('keydown', closeActivePanels);
});

const lastScrollTop = ref(0);
const isHeaderVisible = ref(true);

const handleSidebarScroll = (e: Event) => {
  const target = e.target as HTMLElement;
  const currentScroll = target.scrollTop;

  // Вызываем твою старую логику (для бейджиков даты и т.д.)
  handleScroll();

  // Логика скрытия шапки:
  // Если скроллим вниз больше чем на 10px и ушли от верха чата
  if (currentScroll > lastScrollTop.value && currentScroll > 60) {
    isHeaderVisible.value = false;
  } else {
    isHeaderVisible.value = true;
  }

  lastScrollTop.value = currentScroll;
};

const handleChatClick = (conversationId: number) => {
  messengerStore.openChat(conversationId);
  showChatInfo.value = false; // Закрываем инфу при смене чата
  const chat = messengerStore.conversations.find(c => c.id === conversationId);
  if (chat && hasUnread(chat)) {
    messengerStore.markAsRead(conversationId);
  }
};

const menu = (event: MouseEvent, chat: ConversationResponseDto) => {
  if (chat.type !== ConversationType.GROUP) {
    let user = chat.members.find(c => c.userId !== authStore.userDetails?.id);
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
      pendingQuote: commentStore.pendingQuote,
      close: commentStore.closeChat,
      loadMore: commentStore.loadMoreComments,
      send: commentStore.send,
      remove: commentStore.removeComment,
      clearQuote: commentStore.clearQuote,
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
      pendingQuote: null,
      close: messengerStore.closeChat,
      loadMore: messengerStore.loadMoreMessages,
      send: (payload: { content: string, file?: File | null }) =>
        messengerStore.send(payload.content, payload.file ? [payload.file] : []),
      remove: messengerStore.removeMsg,
      clearQuote: () => {},
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
const isScrolling = ref(false);
let scrollTimeout: number | null = null;
const contextMenu = ref({ show: false, x: 0, y: 0, targetId: null as number | null });
const topObserverTarget = ref<HTMLElement | null>(null);
let topObserver: IntersectionObserver | null = null;
const showChatOptions = ref(false);


const showNewChatModal = ref(false);
const modalPurpose = ref<'CREATE' | 'ADD_MEMBERS'>('CREATE');
const isMessengerVisible = ref(false); // По умолчанию скрыто, чтобы не мешать чтению

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

    // Помечаем прочитанным, только если есть новые сообщения
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

const openContextMenu = (e: MouseEvent, id: number) => {
  e.preventDefault();
  contextMenu.value = { show: true, x: e.clientX, y: e.clientY, targetId: id };
  const close = () => { contextMenu.value.show = false; document.removeEventListener('click', close); };
  setTimeout(() => document.addEventListener('click', close), 50);
};

const handleDelete = async () => {
  if (!contextMenu.value.targetId || !activeContext.value) return;
  await activeContext.value.remove(contextMenu.value.targetId);
  contextMenu.value.show = false;
};

const handleQuoteClick = (url: string) => {
  if (!url) return;
  router.push(url);
  if (window.innerWidth < 1000 && activeContext.value) activeContext.value.close();
};

const zoomImage = (url: string) => { zoomedImageUrl.value = url; };

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
  // Так как в твоем сторе метод принимает по одному, делаем цикл.
  // Идеально было бы сделать бэкенд на List<Long>, но и так сойдет:
  await messengerStore.addMemberToGroup(messengerStore.activeConversationId, userIds);
  toastStore.success("Участники добавлены!");
};

const closeActivePanels = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    if (showNewChatModal.value) { showNewChatModal.value = false; return; }
    if (zoomedImageUrl.value) { zoomedImageUrl.value = null; return; }
    if (showChatInfo.value) { showChatInfo.value = false; return; } // Закрываем инфу
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

// === НОВЫЕ ФУНКЦИИ УПРАВЛЕНИЯ ГРУППОЙ ===
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

onUnmounted(() => {
  window.removeEventListener('keydown', closeActivePanels);
  if (topObserver) topObserver.disconnect();
});
</script>

<template>
  <div class="omnichannel-wrapper">
    <Transition name="fade">
      <button
        v-if="!isMessengerVisible"
        class="floating-messenger-btn"
        @click="isMessengerVisible = true"
        title="Открыть чаты"
      >
        💬
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
              :class="{ 'header-hidden': !isHeaderVisible }"
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
                <div class="chat-avatar-wrapper"
                     style="width: 60px; height: 60px;"
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

            <div v-show="!showChatInfo" class="comments-list" :ref="setListRef" @scroll="handleSidebarScroll">
              <div ref="topObserverTarget" class="chat-history-trigger">
                <span v-if="activeContext.isLoadingMore" class="spinner-small history-spinner"></span>
              </div>

              <div v-for="(group, date) in activeContext.groupedItems" :key="date" class="comment-group">
                <div class="sticky-sentinel"></div>
                <div class="date-sticky-header" :class="{ 'is-scrolling-active': isScrolling }">
                  <span class="date-badge">{{ date }}</span>
                </div>

                <div v-for="item in group" :key="item.id" class="comment-item" @contextmenu="openContextMenu($event, item.id)">
                  <div class="comment-bubble">
                    <span class="user-badge">{{ ('username' in item) ? item.username : item.senderUsername }}</span>
                    <p class="comment-body">{{ item.content }}</p>
                    <div v-if="item.metadata?.images?.length" class="comment-images">
                      <img v-for="url in item.metadata.images" :key="url" :src="url" class="comment-img" @click="zoomImage(url)">
                    </div>
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
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M5 12h14" stroke-linecap="round" stroke-linejoin="round"/></svg>
              <span v-if="isSidebarExpanded" class="btn-label">Создать чат</span>
            </button>

            <!-- ОБНОВЛЕННАЯ СТРУКТУРА КНОПКИ ЧАТА -->
            <div
              v-for="chat in filteredConversations"
              :key="chat.id"
              class="chat-avatar-wrapper"
              :class="{ 'is-active': messengerStore.activeConversationId === chat.id, 'expanded': isSidebarExpanded }"
              @contextmenu.prevent="menu($event, chat)"
              @click="handleChatClick(chat.id)"
            >
              <!-- КРУГ: Фото/Буква + Непрочитанные. Свечение применяется сюда! -->
              <div class="avatar-circle-container" :style="getGlowStyle(chat.id, !!chat.avatarUrl)">
                <img v-if="chat.avatarUrl" :src="chat.avatarUrl" class="chat-avatar" />
                <div v-else class="chat-avatar-placeholder">
                  {{ chat.title.charAt(0).toUpperCase() }}
                </div>
                <span v-if="hasUnread(chat)" class="unread-badge"></span>
              </div>

              <!-- ТЕКСТ: Имя + Пин. Видно только при расширении -->
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

</template>

<style scoped>
.omnichannel-wrapper {
  position: relative;
  display: flex;
  flex-direction: row;
  height: 100%;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.chat-options-wrapper {
  position: relative;
}

.options-btn {
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 1.5rem;
  font-weight: bold;
  cursor: pointer;
  padding: 0 5px;
}
.options-btn:hover { color: var(--text-header); }

.chat-options-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background: var(--bg-main);
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
  display: flex;
  flex-direction: column;
  min-width: 150px;
  overflow: hidden;
  z-index: 100;
}

.chat-options-menu button {
  background: none;
  border: none;
  padding: 10px 15px;
  text-align: left;
  color: var(--text-header);
  cursor: pointer;
  font-size: 0.9rem;
}
.chat-options-menu button:hover { background: var(--bg-editor-sheet); }
.chat-options-menu button.danger { color: #ff4d4f; }


.mini-tabs {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 2px solid var(--text-muted);
  width: 100%;
  align-items: center;
}

.tab-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: var(--text-header);
  opacity: 0.4;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.2s;
}

.tab-btn:hover { opacity: 0.8; }
.tab-btn.active {
  opacity: 1;
  transform: scale(1.1);
}

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
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: #3b4b5e;
  border: none;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}
.action-btn-circle:hover { background-color: #4a5c73; }


.empty-mini-tab {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.global-sidebar {
  position: absolute;
  right: 70px;
  top: 0;
  z-index: 20;
  width: 350px;
  height: 100%;
  background-color: var(--bg-editor-sheet);
  border-left: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  box-shadow: -10px 0 30px rgba(0,0,0,0.1);
  transition: right 0.3s cubic-bezier(0.4, 0, 0.2, 1), width 0.3s;
}

.global-sidebar.is-shifted {
  right: 250px; /* Отступ, когда меню расширено */
}

.sidebar-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-subtle);
  transition: transform 0.3s ease;
}
.header-hidden {
  transform: translateY(-100%);
}

.header-info h3 { margin: 0; font-size: 1.1rem; color: var(--text-header); }
.target-badge { font-size: 0.7rem; color: var(--btn-plus); text-transform: uppercase; font-weight: bold; }
.close-btn { background: none; border: none; color: var(--text-muted); font-size: 1.2rem; cursor: pointer; }

.comments-list {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background-color: var(--bg-editor-sheet);
  scrollbar-width: thin;
}

.comment-item {
  display: flex;
  flex-direction: column;
  max-width: 95%;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.comment-bubble {
  background: var(--bg-main);
  border: 1px solid var(--border-subtle);
  border-radius: 14px 14px 14px 4px;
  padding: 10px 12px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  transition: background 0.2s ease;
}

.comment-body {
  margin: 0;
  font-size: 0.95rem;
  line-height: 1.5;
  color: var(--text-header);
}

.user-badge { font-weight: 700; color: var(--btn-plus); font-size: 0.85rem; margin-bottom: 4px; display: block; }
.comment-images { margin-top: 10px; display: flex; flex-wrap: wrap; gap: 6px; }
.comment-img {
  max-width: 100%;
  max-height: 200px;
  border-radius: 8px;
  cursor: zoom-in;
  object-fit: cover;
}

.date-sticky-header {
  position: sticky;
  top: -1px;
  z-index: 20;
  display: flex;
  justify-content: center;
  padding: 8px 0;
}
.date-badge {
  background: var(--bg-main);
  border: 1px solid var(--border-subtle);
  color: var(--text-muted);
  font-size: 0.75rem;
  padding: 4px 12px;
  border-radius: 12px;
  opacity: 0;
  transition: opacity 0.3s;
}
.date-badge[data-in-text], .is-scrolling-active .date-badge { opacity: 1; }

.sidebar-input-area {
  padding: 12px 16px 24px;
  border-top: 1px solid var(--border-subtle);
}
.input-row { display: flex; align-items: flex-end; gap: 10px; }
.input-wrapper textarea {
  flex: 1;
  background: var(--bg-main);
  border: 1px solid var(--border-subtle);
  border-radius: 18px;
  padding: 10px 14px;
  color: var(--text-header);
  resize: none;
  font-size: 0.95rem;
  outline: none;
}
.attach-btn { background: none; border: none; font-size: 1.3rem; cursor: pointer; opacity: 0.5; }
.attach-btn.has-file { opacity: 1; color: var(--btn-plus); }
.send-btn {
  background: var(--btn-plus);
  color: white;
  border: none;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  cursor: pointer;
}

.image-lightbox {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.9);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: zoom-out;
}
.full-image { max-width: 90%; max-height: 90%; border-radius: 4px; box-shadow: 0 0 30px rgba(0,0,0,0.5); }
.close-lightbox {
  position: absolute;
  top: 20px;
  right: 20px;
  background: none;
  border: none;
  color: white;
  font-size: 2rem;
  cursor: pointer;
}

.context-menu {
  position: fixed;
  background: var(--bg-editor-sheet);
  border: 1px solid var(--border-subtle);
  box-shadow: 0 5px 15px var(--shadow-color);
  border-radius: 8px;
  z-index: 10000;
  min-width: 140px;
  padding: 4px;
}
.menu-item { padding: 8px 12px; cursor: pointer; border-radius: 6px; font-size: 0.9rem; }
.menu-item.delete { color: #ff4d4f; }
.menu-item:hover { background: var(--bg-main); }

.slide-enter-active, .slide-leave-active { transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.slide-enter-from, .slide-leave-to { transform: translateX(100%); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.spinner-small {
  width: 18px; height: 18px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.quote-container {
  display: flex;
  gap: 10px;
  background: var(--bg-editor-page);
  padding: 8px 12px;
  border-radius: 6px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: background 0.2s;
  border: 1px solid var(--border-subtle);
}
.quote-container:hover { background: var(--hover-dropdowb); }
.quote-line { width: 3px; background: var(--btn-plus); border-radius: 2px; }
.quote-text {
  margin: 0; font-size: 0.85rem; color: var(--text-muted); font-style: italic;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}

.quote-preview-box {
  background: var(--bg-main); border-left: 3px solid var(--btn-plus);
  padding: 8px 12px; margin-bottom: 10px; border-radius: 4px; display: flex;
  justify-content: space-between; align-items: flex-start; animation: slideUp 0.2s ease-out;
}
.quote-preview-content { display: flex; gap: 8px; }
.quote-mark { font-size: 1.5rem; line-height: 1; color: var(--btn-plus); font-family: serif; }
.quote-preview-content p {
  margin: 0; font-size: 0.85rem; color: var(--text-muted); font-style: italic;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}

.chat-history-trigger {
  width: 100%; min-height: 20px; display: flex; justify-content: center; align-items: center; padding: 10px 0;
}
.history-spinner { border-color: rgba(100, 100, 100, 0.3); border-top-color: var(--text-muted); }

@keyframes slideUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}


/* === РАСШИРЯЕМЫЙ САЙДБАР === */


.sidebar-top-actions {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  margin-bottom: 15px;
}


.hide-messenger-btn,
.toggle-expand-btn {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;

  /* Создаем одинаковый квадратный контейнер для иконок */
  width: 32px;
  height: 32px;

  /* Центрируем символ внутри кнопки */
  display: flex;
  align-items: center;
  justify-content: center;

  /* Сбрасываем лишние отступы и выравниваем шрифт */
  padding: 0;
  font-size: 1.2rem;
  line-height: 1;
  transition: color 0.2s, transform 0.1s;
}

.toggle-expand-btn {
  /* Если ▶ кажется выше/ниже, можно точечно поправить: */
  padding-bottom: 2px;
}

.hide-messenger-btn:hover { color: #ff4757; }
.toggle-expand-btn:hover { color: var(--btn-plus); }

.search-box {
  width: 100%; padding: 0 5px; margin-bottom: 15px;
}
.search-box input {
  width: 100%; padding: 8px 12px; border-radius: 8px; border: 1px solid var(--border-subtle);
  background: var(--bg-main); color: var(--text-header); outline: none;
}

.mini-tabs.row-mode { flex-direction: row; justify-content: space-around; }
.tab-label { font-size: 0.9rem; font-weight: bold; margin-left: 8px; }


.expanded-btn { width: 100%; border-radius: 8px; justify-content: flex-start; padding-left: 12px; }
.btn-label { margin-left: 15px; font-weight: bold; }

/* === ИНФО О ЧАТЕ (Кликабельный хедер) === */
.header-info.clickable {
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}
.header-info.clickable:hover { background: var(--bg-main); }
.dropdown-icon { font-size: 0.8rem; color: var(--text-muted); margin-left: 5px; }

.chat-info-panel {
  flex: 1;
  background: var(--bg-editor-sheet);
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
  animation: slideUp 0.3s ease-out;
}
.info-hero { text-align: center; display: flex; flex-direction: column; align-items: center; }

.members-count { color: var(--text-muted); font-size: 0.9rem; }

.group-management {
  background: var(--bg-main);
  border-radius: 12px;
  padding: 15px;
  border: 1px solid var(--border-subtle);
}
.add-member-box { display: flex; gap: 10px; margin-bottom: 20px; }
.member-input {
  flex: 1; background: var(--bg-editor-sheet); border: 1px solid var(--border-subtle);
  padding: 8px 12px; border-radius: 6px; color: var(--text-header); outline: none;
}
.add-btn { background: var(--btn-plus); color: white; border: none; padding: 0 15px; border-radius: 6px; cursor: pointer; }
.add-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.members-list { display: flex; flex-direction: column; gap: 10px; margin-bottom: 20px; }
.member-item { display: flex; justify-content: space-between; align-items: center; padding: 5px 0; border-bottom: 1px solid var(--border-subtle); }
.admin-badge { background: #f1c40f; color: #000; font-size: 0.7rem; padding: 2px 6px; border-radius: 4px; margin-left: 10px; font-weight: bold; }
.kick-btn { background: none; border: none; color: #ff4d4f; cursor: pointer; font-weight: bold; }

.danger-btn.outline {
  width: 100%; background: transparent; border: 1px solid #ff4d4f; color: #ff4d4f;
  padding: 10px; border-radius: 8px; cursor: pointer; transition: all 0.2s;
}
.danger-btn.outline:hover { background: #ff4d4f; color: white; }

/* Основной контейнер инфо-панели */
.chat-info-panel {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  background-color: var(--bg-editor-sheet);
  height: 100%;
  overflow-y: auto;
}

/* ГЕРОЙ-СЕКЦИЯ (Ряд: Фото | Текст) */
.info-hero-row {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--bg-main);
  padding: 16px;
  border-radius: 12px;
  border: 1px solid var(--border-subtle);
}

.hero-text h2 {
  margin: 0;
  font-size: 1.2rem;
  color: var(--text-header);
}

.members-count {
  margin: 4px 0 0;
  font-size: 0.85rem;
  color: var(--btn-plus);
  font-weight: 700;
}

/* УПРАВЛЕНИЕ */
.add-btn-full {
  width: 100%;
  padding: 12px;
  background: var(--btn-plus);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: bold;
  cursor: pointer;
  margin-bottom: 20px;
  transition: opacity 0.2s;
}
.add-btn-full:hover { opacity: 0.9; }

.members-list-section h4 {
  margin-bottom: 12px;
  color: var(--text-muted);
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.member-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-subtle);
}

.member-info-main { display: flex; align-items: center; gap: 8px; }
.member-name-text { color: var(--text-header); font-size: 0.95rem; }

.admin-badge-mini {
  background: rgba(241, 196, 15, 0.2);
  color: #f1c40f;
  font-size: 0.65rem;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 800;
}

.member-kick-icon {
  background: none;
  border: none;
  color: #ff4757;
  cursor: pointer;
  font-size: 1.1rem;
  transition: transform 0.2s;
}
.member-kick-icon:hover { transform: scale(1.2); }

.leave-btn-outline {
  margin-top: auto;
  width: 100%;
  padding: 12px;
  background: transparent;
  border: 1px solid #ff4757;
  color: #ff4757;
  border-radius: 8px;
  cursor: pointer;
  font-weight: bold;
}
.leave-btn-outline:hover { background: #ff4757; color: white; }

/* === САЙДБАР (С учетом мобилок) === */
.mini-sidebar {
  width: 70px;
  height: 100%;
  background-color: var(--bg-editor-sheet);
  border-left: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 10px;
  flex-shrink: 0;
  z-index: 21;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.mini-sidebar.is-expanded {
  width: 250px;
  align-items: stretch;
  padding-left: 8px;
  padding-right: 8px;
}

@media (max-width: 768px) {
  /* Окно самой переписки */
  .global-sidebar,
  .global-sidebar.is-shifted {
    position: fixed !important; /* Отвязываем от родителя */
    top: 0 !important;
    right: 0 !important;        /* Прижимаем к правому краю */
    width: 100vw !important;    /* Ширина на весь экран */
    height: 100vh !important;   /* Высота на весь экран */
    z-index: 10000 !important;  /* Выносим поверх всего сайта */
    border-left: none;
  }

  /* Если открыт сам чат (переписка), прячем список чатов под него */
  .global-sidebar ~ .mini-sidebar {
    /* На мобилке нет смысла показывать список, если открыт чат */
    opacity: 0;
    pointer-events: none;
  }
}

/* === ОБЩАЯ КНОПКА ЧАТА === */
.chat-avatar-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px;
  border-radius: 50%; /* В свернутом виде круглая */
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  width: 56px;
  margin: 0 auto;
}

.chat-avatar-wrapper:not(.expanded):hover {
  transform: scale(1.08); /* Увеличиваем только в свернутом виде */
}

/* === КНОПКА ЧАТА В РАСШИРЕННОМ ВИДЕ === */
.chat-avatar-wrapper.expanded {
  width: 100%;
  height: 56px;
  border-radius: 10px; /* Смягченные углы, как в ТГ */
  justify-content: flex-start;
  padding: 0 10px;
  background: transparent;
  gap: 12px;
  margin: 0; /* Убираем центровку auto */
}

.chat-avatar-wrapper.expanded:hover {
  background: rgba(255, 255, 255, 0.05); /* Легкий фон при наведении, без прыжков */
}

/* === КОНТЕЙНЕР АВАТАРА (Здесь теперь свечение!) === */
.avatar-circle-container {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative; /* Для точки непрочитанных */
  transition: transform 0.2s, box-shadow 0.3s;
}

/* Применяем свечение только к кругу, если чат активен */
.chat-avatar-wrapper.is-active .avatar-circle-container {
  /* Формат: var(--свойство, дефолтный_цвет) */
  box-shadow: 0 0 calc(var(--aura-blur, 8px) * 1.5) var(--chat-accent, #2980b9) !important;
  border: 2px solid var(--chat-accent, #2980b9);
  transform: scale(1.05);
}

/* === САМ АВАТАР И ЗАГЛУШКА === */
.chat-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.chat-avatar-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center; /* Центрируем букву идеально */
  color: white;
  font-weight: 800;
  font-size: 1.2rem;
  line-height: 1; /* Убирает смещение по вертикали */
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.4);
  user-select: none;
}

/* === ИНФО-БЛОК (Текст и Пин) === */
.chat-info-text {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: space-between;
  min-width: 0; /* ВАЖНО: без этого многоточие (ellipsis) не работает во флексах */
}

.chat-list-title {
  color: var(--text-header);
  font-weight: 500;
  font-size: 0.95rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis; /* Длинные имена обрезаются ... */
}

/* === ИНДИКАТОРЫ === */
.unread-badge {
  position: absolute;
  top: -2px; /* Привязана к краю круглого контейнера */
  right: -2px;
  width: 12px;
  height: 12px;
  background-color: var(--unread-dot, #ff4757);
  border: 2px solid var(--bg-main, #1a1a1a);
  border-radius: 50%;
  z-index: 2;
  pointer-events: none;
}

.pin-indicator {
  font-size: 0.9rem;
  opacity: 0.7;
  flex-shrink: 0;
  margin-left: 8px; /* Отступ от текста */
}
/* === ПЛАВАЮЩАЯ КНОПКА === */
.floating-messenger-btn {
  position: fixed;
  bottom: 20px;
  right: 20px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--btn-plus, #2980b9);
  color: white;
  border: none;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.4);
  cursor: pointer;
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.floating-messenger-btn:hover {
  transform: scale(1.1) translateY(-5px);
}

.global-unread-dot {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 14px;
  height: 14px;
  background: #ff4757;
  border: 2px solid var(--btn-plus, #2980b9);
  border-radius: 50%;
}

/* === ОБЕРТКА ВСЕГО МЕССЕНДЖЕРА === */
.messenger-container {

}

/* Кнопка закрытия внутри сайдбара */


/* Раскидываем кнопки управления в шапке сайдбара */
.sidebar-top-actions {
  width: 100%;
  display: flex;
  justify-content: space-between; /* Крестик слева, стрелка справа */
  align-items: center;
  padding: 10px 10px 0 10px;
  margin-bottom: 15px;
}

/* === АНИМАЦИЯ СКРЫТИЯ/ПОЯВЛЕНИЯ ВСЕГО БЛОКА === */
.slide-right-enter-active,
.slide-right-leave-active {
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(100%); /* Увозит весь мессенджер вправо за пределы экрана */
}
</style>
