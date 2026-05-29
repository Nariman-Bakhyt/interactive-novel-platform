import {defineStore} from 'pinia';
import {computed, nextTick, ref} from 'vue';
import {
  addUserToGroup,
  createGroupChat,
  deleteChatForUser,
  deleteMessage,
  getChatMessages,
  getMyChats,
  getOrCreatePrivateChat,
  kickUser,
  leaveGroup,
  markConversationAsRead,
  sendMessage,
  sendTypingStatus,
  toggleChatSettings
} from "@/api/chatService";
import {activateStomp, isConnected, deactivateStomp, subscribeToTopic, unsubscribeFromTopic} from "@/api/stompService";
import {
  type ConversationResponseDto,
  type CreateGroupRequest,
  type MessageResponseDto,
  type SendMessageRequestDto,
  WsEventType
} from "@/types/chat.ts";
import type {WsEventDto} from "@/types/ws.ts";
import {useToastStore} from "@/components/toast/toastStore.ts";
import type {SocialEventType} from "@/types/social.ts";
import {useSocialStore} from "@/components/social/socialStore.ts";
import {useAuthStore} from "@/api/auth.ts";

export const useMessengerStore = defineStore('messenger', () => {

  const toastStore = useToastStore();
  const socialStore = useSocialStore();
  const authStore = useAuthStore();
  
  
  
  const handleError = (context: string, err: any) => {
    
    console.error(`[${context}]`, err);

    
    if (err.response) {
      if (err.response.status >= 400 && err.response.status < 500) {
        
        toastStore.error(err.response.data?.detailedMessage || "Действие отклонено");
      } else if (err.response.status >= 500) {
        
        toastStore.error("Что-то пошло не так на сервере. Мы уже чиним!");
      }
    } else {
      
      toastStore.error("Проверьте подключение к сети");
    }
  };

  
  const conversations = ref<ConversationResponseDto[]>([]);
  const isConversationsLoading = ref(false);

  
  const activeConversationId = ref<number | null>(null);
  const messages = ref<MessageResponseDto[]>([]);
  const messagesListRef = ref<HTMLElement | null>(null);
  const isSending = ref(false);

  
  const currentPage = ref(0);
  const isLastPage = ref(false);
  const isLoadingMore = ref(false);
  const pageSize = 30;

  
  const typingUsers = ref<Record<number, string>>({}); 
  let typingTimeout: ReturnType<typeof setTimeout> | null = null;

  const globalSocketTopic = ref<string | null>(null);
  let connectionCheckInterval: ReturnType<typeof setInterval> | null = null;
  
  const initGlobalSocket = (myUserId: number) => {
    const topic = `/topic/user.${myUserId}`;
    if (globalSocketTopic.value === topic) {
      console.log(`[WebSocket] Уже подключены к своему каналу: ${topic}`);
      return;
    }
    if (globalSocketTopic.value) {
      unsubscribeFromTopic(globalSocketTopic.value);
    }
    
    globalSocketTopic.value = topic;
    subscribeToTopic<WsEventDto<any>>(topic, (event) => {
      switch (event.domain) {
        case 'CHAT':
          handleChatDomainEvent(event);
          break;
        case 'SOCIAL':
          socialStore.handleSocialEvent({
            type: event.type as SocialEventType,
            payload: event.payload
          });
          break;
        case 'SYSTEM':
          toastStore.info(event.payload.message || 'Системное уведомление');
          break;
      }
    });

    // Периодическая проверка раз в 5 секунд: если соединение упало (например, из-за сна устройства), 
    // принудительно вызываем activateStomp() для возобновления связи.
    if (connectionCheckInterval) clearInterval(connectionCheckInterval);
    connectionCheckInterval = setInterval(() => {
      if (!isConnected.value) {
        console.warn(`[WebSocket] Обнаружена потеря соединения с ${topic}. Принудительное переподключение...`);
        activateStomp();
      }
    }, 5000);
  };

  const handleChatDomainEvent = (event: WsEventDto<any>) => {
    const payload = event.payload;

    if (event.type === WsEventType.CHAT_UPDATED || event.type === WsEventType.NEW_MESSAGE) {
      const targetChatId = payload.conversationId ;
      const index = conversations.value.findIndex(c => c.id === targetChatId);

      if (index !== -1) {
        const chat = conversations.value[index];
        if (chat) {
          chat.lastMessagePreview = payload.content ? payload.content : "Вложение";
          chat.lastMessageAt = payload.timestamp;

          if (payload.senderId === authStore.userDetails?.id) {
            chat.lastReadAt = payload.timestamp;
          } else if (activeConversationId.value === targetChatId) {
            markAsRead(targetChatId);
          }

          conversations.value.splice(index, 1);
          conversations.value.unshift(chat);
        }
      } else {
        loadMyChats();
      }
    }
    if (event.type === "READ_UPDATE") {
      const { conversationId, lastReadAt } =payload;

      const chat = conversations.value.find(c => c.id === conversationId);
      if(chat) {
        chat.lastReadAt = lastReadAt;
      }
    }

    if (event.type === WsEventType.MESSAGE_DELETED) {
      const messageId = payload.messageId;
      const conversationId = payload.conversationId;
      if (activeConversationId.value === conversationId) {
        messages.value = messages.value.filter(m => m.id !== messageId);
      }
    }
  };

  
  
  
  const scrollToBottom = async (force = false) => {
    await nextTick();
    if (messagesListRef.value) {
      const el = messagesListRef.value;
      const isNearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 150;
      if (force || isNearBottom) {
        el.scrollTop = el.scrollHeight;
      }
    }
  };

  const loadMyChats = async () => {
    isConversationsLoading.value = true;
    try {
      const result = await getMyChats(0, 50);
      conversations.value = result.content;
      currentChatsPage.value = 0;
    } catch (e) {
      handleError("loadMyChats", e);
      throw e;
    } finally {
      isConversationsLoading.value = false;
    }
  };

  const openChat = async (conversationId: number) => {
    if (activeConversationId.value === conversationId) return;

    if (activeConversationId.value) {
      unsubscribeFromTopic(`/topic/chat.${activeConversationId.value}`);
    }

    activeConversationId.value = conversationId;
    localStorage.setItem('active_conversation_id', conversationId.toString());

    currentPage.value = 0;
    isLastPage.value = false;
    messages.value = [];
    typingUsers.value = {};

    const topic = `/topic/chat.${conversationId}`;
    subscribeToTopic<any>(topic, (event) => {
      if (event.type === 'NEW_MESSAGE') {
        const msg = event.payload as MessageResponseDto;
        if (!messages.value.some(m => m.id === msg.id)) {
          messages.value.push(msg);
          scrollToBottom();
        }
      } else if (event.type === 'MESSAGE_DELETED') {
        messages.value = messages.value.filter(m => m.id !== event.payload.messageId);
      } else if (event.type === 'USER_TYPING') {
        typingUsers.value[event.payload.userId] = event.payload.username;
        if (typingTimeout) clearTimeout(typingTimeout);
        typingTimeout = setTimeout(() => { typingUsers.value = {}; }, 3000);
      }
    });

    try {
      const history = await getChatMessages(conversationId, currentPage.value, pageSize);
      messages.value = history.content ? [...history.content].reverse() : [];
      isLastPage.value = history.last;
      
      await nextTick();
      if (messagesListRef.value) {
        const savedScroll = localStorage.getItem(`scroll_chat_${conversationId}`);
        if (savedScroll) {
          const scrollTopVal = parseInt(savedScroll, 10);
          messagesListRef.value.scrollTop = scrollTopVal;
          setTimeout(() => {
            if (messagesListRef.value) messagesListRef.value.scrollTop = scrollTopVal;
          }, 100);
          setTimeout(() => {
            if (messagesListRef.value) messagesListRef.value.scrollTop = scrollTopVal;
          }, 300);
        } else {
          messagesListRef.value.scrollTop = messagesListRef.value.scrollHeight;
        }
      }
    } catch (e) {
      handleError("openChat", e);
      throw e;
    }
  };

  const isLastChatsPage = ref(false);
  const currentChatsPage = ref(0);

  const loadMoreChats = async () => {
    if (isConversationsLoading.value || isLastChatsPage.value) return;

    isConversationsLoading.value = true;
    try {
      currentChatsPage.value++;
      const result = await getMyChats(currentChatsPage.value, 20);

      if (result.content && result.content.length > 0) {
        const newChats = result.content.filter(
          newChat => !conversations.value.some(existing => existing.id === newChat.id)
        );
        conversations.value = [...conversations.value, ...newChats];
        isLastChatsPage.value = (result.page.number + 1) >= result.page.totalPages;
      } else {
        isLastChatsPage.value = true;
      }
    } catch (e) {
      handleError("loadMoreChats", e);
      currentChatsPage.value--; 
      throw e;
    } finally {
      isConversationsLoading.value = false;
    }
  };

  const loadMoreMessages = async () => {
    if (isLoadingMore.value || isLastPage.value || !activeConversationId.value) return;

    isLoadingMore.value = true;
    try {
      currentPage.value++;
      const history = await getChatMessages(activeConversationId.value, currentPage.value, pageSize);

      if (history.content) {
        const oldScrollHeight = messagesListRef.value?.scrollHeight || 0;
        messages.value = [...history.content.reverse(), ...messages.value];
        isLastPage.value = history.last;

        await nextTick();
        if (messagesListRef.value) {
          messagesListRef.value.scrollTop = messagesListRef.value.scrollHeight - oldScrollHeight;
        }
      }
    } catch (e) {
      handleError("loadMoreMessages", e);
      currentPage.value--; 
      throw e;
    } finally {
      isLoadingMore.value = false;
    }
  };

  const send = async (content: string, files: File[] = []) => {
    const targetIdAtStart = activeConversationId.value;
    if (!targetIdAtStart || isSending.value) return;

    isSending.value = true;
    const dto: SendMessageRequestDto = {
      conversationId: targetIdAtStart,
      content: content,
      type: files.length > 0 ? 'IMAGE' : 'PLAIN'
    };

    try {
      await sendMessage(targetIdAtStart, dto, files);
    } catch (err: any) {
      handleError("send", err);
      
      if (err.response?.status === 403) {
        const chat = conversations.value.find(c => c.id === targetIdAtStart);
        if (chat) chat.blocked = true;
      }
      throw err;
    } finally {
      isSending.value = false;
    }
  };

  const removeMsg = async (messageId: number) => {
    try {
      await deleteMessage(messageId);
    } catch (e: any) {
      handleError("removeMsg", e);
      throw e;
    }
  };

  const notifyTyping = () => {
    if (activeConversationId.value) {
      sendTypingStatus(activeConversationId.value);
    }
  };

  const groupedMessages = computed(() => {
    const groups: Record<string, MessageResponseDto[]> = {};
    messages.value.forEach(msg => {
      const date = new Date(msg.timestamp).toLocaleDateString();
      if (!groups[date]) groups[date] = [];
      groups[date].push(msg);
    });
    return groups;
  });

  const closeChat = () => {
    if (activeConversationId.value) {
      unsubscribeFromTopic(`/topic/chat.${activeConversationId.value}`);
    }
    activeConversationId.value = null;
    localStorage.removeItem('active_conversation_id');
    messages.value = [];
  };

  const startPrivateChat = async (targetUserId: number) => {
    if (socialStore.isBlocked(targetUserId)) {
      toastStore.error("Нельзя начать чат с заблокированным пользователем");
      return;
    }
    try {
      const newChat = await getOrCreatePrivateChat(targetUserId);
      if (!conversations.value.some(c => c.id === newChat.id)) {
        conversations.value.unshift(newChat);
      }
      await openChat(newChat.id);
    } catch (error: any) {
      handleError("startPrivateChat", error);
      throw error;
    }
  };

  const removeConversation = async (conversationId: number) => {
    try {
      await deleteChatForUser(conversationId);
      conversations.value = conversations.value.filter(c => c.id !== conversationId);
      if (activeConversationId.value === conversationId) {
        closeChat();
      }
    } catch (e: any) {
      handleError("removeConversation", e);
      throw e;
    }
  };

  const leaveGroupChat = async (conversationId: number) => {
    try {
      await leaveGroup(conversationId);
      conversations.value = conversations.value.filter(c => c.id !== conversationId);
      if (activeConversationId.value === conversationId) closeChat();
    } catch (e: any) {
      handleError("leaveGroupChat", e);
      throw e;
    }
  };

  const toggleSettings = async (conversationId: number, isMuted: boolean, isPinned: boolean) => {
    try {
      await toggleChatSettings({conversationId,  isMuted, isPinned });

      const chat = conversations.value.find(c => c.id === conversationId);
      if (chat) {
        chat.isMuted = isMuted;
        chat.isPinned = isPinned;

        if (isPinned) {
          conversations.value.sort((a, b) => {
            if (a.isPinned === b.isPinned) {
              return new Date(b.lastMessageAt).getTime() - new Date(a.lastMessageAt).getTime();
            }
            return a.isPinned ? -1 : 1;
          });
        }
      }
    } catch (e: any) {
      handleError("toggleSettings", e);
      throw e;
    }
  };

  const clearAndDisconnect = () => {
    if (connectionCheckInterval) {
      clearInterval(connectionCheckInterval);
      connectionCheckInterval = null;
    }
    deactivateStomp();
    activeConversationId.value = null;
    globalSocketTopic.value = null;
    localStorage.removeItem('active_conversation_id');
    conversations.value = [];
    messages.value = [];
    typingUsers.value = {};
    currentPage.value = 0;
    isLastPage.value = false;
    console.log('🧹 MessengerStore: Данные очищены, сокет закрыт');
  };

  const currentChat = computed(() => {
    if (!activeConversationId.value) return null;
    return conversations.value.find(c => c.id === activeConversationId.value) || null;
  });

  const createGroup = async (payload: CreateGroupRequest) => {
    
    const hasBlocked = payload.memberIds.some(id => socialStore.isBlocked(id));
    if (hasBlocked) {
      toastStore.error("В группу нельзя добавить заблокированных пользователей");
      throw new Error("Blocked user in list");
    }

    try {
      
      const newGroup = await createGroupChat(payload);
      conversations.value.unshift(newGroup);
      await openChat(newGroup.id);
      return newGroup;
    } catch (e: any) {
      handleError("createGroup", e);
      throw e;
    }
  };

  const addMemberToGroup = async (conversationId: number, targetUserIds: number[]) => {
    const finalIds = targetUserIds.filter(targetId => {
      if (socialStore.isBlocked(targetId)) {
        toastStore.error(`Пользователь ${targetId} заблокирован и будет удален из списка`);
        return false; 
      }
      return true; 
    });
    if (finalIds.length === 0) {
      const errorMsg = "Нет валидных пользователей для добавления";
      toastStore.error(errorMsg);
      
      throw new Error(errorMsg);
    }
    try {
      const response = await addUserToGroup(conversationId, finalIds);

      const chat = conversations.value.find(c => c.id === conversationId);
      if (chat && response) {
        chat.members = response.members;
      }

    } catch (e: any) {
      handleError("addMemberToGroup", e);
      throw e;
    }
  };

  const kickMemberFromGroup = async (conversationId: number, targetUserId: number) => {
    try {
      const chat = conversations.value.find(c => c.id === conversationId);
      await kickUser(conversationId, targetUserId);

      if (chat ) {
        chat.members = chat.members.filter(m => m.userId !== targetUserId);
      }
    } catch (e: any) {
      handleError("kickMemberFromGroup", e);
      throw e;
    }
  };
  const markAsRead = async (id:number) => {
    await markConversationAsRead(id);
  };

  return {
    conversations, isConversationsLoading,
    activeConversationId, messages, messagesListRef, isSending,
    isLoadingMore, isLastPage, typingUsers, groupedMessages, closeChat, loadMoreChats,
    initGlobalSocket, loadMyChats, openChat, loadMoreMessages, send, removeMsg, notifyTyping,
    startPrivateChat, removeConversation, leaveGroupChat, toggleSettings, clearAndDisconnect, currentChat,
    createGroup, addMemberToGroup, kickMemberFromGroup,markAsRead
  };
});
