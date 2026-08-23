import {defineStore} from 'pinia';
import {computed, nextTick, ref} from 'vue';
import {createComment, deleteComment, getComments, formatPreciseTime, sentCommentsTimestamps} from "@/api/commentService.ts";
import {activeSubscriptions, subscribeToTopic, unsubscribeFromTopic} from "@/api/stompService.ts";
import type {CommentResponseDto} from "@/types/comment.ts";

export const useCommentStore = defineStore('chat', () => {
  const isOpen = ref(false);
  const comments = ref<CommentResponseDto[]>([]);
  const activeTargetId = ref<number | null>(null);
  const targetType = ref<'BLOCK' | 'CHAPTER' | 'NOVEL'>('NOVEL');
  const commentsListRef = ref<HTMLElement | null>(null);
  const isSending = ref(false);
  const pendingQuote = ref<{ text: string, url: string } | null>(null);
  const savedQuote = localStorage.getItem('pending_quote');

  const currentNovelContext = ref<{ id: number, title: string } | null>(null);
  const currentChapterContext = ref<{ id: number, title: string } | null>(null);
  const chapterComments = ref<CommentResponseDto[]>([]);
  let contextCallback: ((wsEvent: any) => void) | null = null;
  let chatCallback: ((wsEvent: any) => void) | null = null;

  const currentPage = ref(0);
  const isLastPage = ref(false);
  const isLoadingMore = ref(false);
  const size = 30;
  const setQuoteMode = (quote: { text: string, url: string }) => {
    pendingQuote.value = quote;
    localStorage.setItem('pending_quote', JSON.stringify(quote));
  };

  const clearQuote = () => {
    pendingQuote.value = null;
    localStorage.removeItem('pending_quote');
  };
  window.addEventListener('storage', (event) => {
    if (event.key === 'pending_quote') {
      pendingQuote.value = event.newValue ? JSON.parse(event.newValue) : null;
    }
  });

  const groupedComments = computed(() => {
    const groups: Record<string, CommentResponseDto[]> = {};
    comments.value.forEach(comment => {
      const date = new Date(comment.timestamp).toLocaleDateString();
      if (!groups[date]) groups[date] = [];
      groups[date].push(comment);
    });
    return groups;
  });

  const scrollToBottom = async (force = false) => {
    await nextTick();
    if (commentsListRef.value) {
      const el = commentsListRef.value;
      const isNearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 150;
      if (force || isNearBottom) {
        el.scrollTop = el.scrollHeight;
      }
    }
  };

  const openChat = async (id: number, type: 'BLOCK' | 'CHAPTER' | 'NOVEL' = 'NOVEL') => {
    if (isOpen.value && activeTargetId.value === id && targetType.value === type) {
      closeChat();
      return;
    }
    if (activeTargetId.value) unsubscribeFromCurrent();

    activeTargetId.value = id;
    targetType.value = type;
    isOpen.value = true;


    currentPage.value = 0;
    isLastPage.value = false;
    isLoadingMore.value = false;
    comments.value = [];

    const topic = `/topic/${type.toLowerCase()}.${id}`;

    if (!activeSubscriptions.has(topic) || !chatCallback) {
      chatCallback = (wsEvent: any) => {
        const type = wsEvent.type;
        const payload = wsEvent.payload;
        if (type === 'COMMENT_DELETED') {
          comments.value = comments.value.filter(c => c.id !== payload.id);
        } else if (type === 'COMMENT_CREATED') {
          const comment = payload as CommentResponseDto;
          if (!comments.value.some(c => c.id === comment.id)) {
            const receiveTime = Date.now();
            const receiveTimeStr = formatPreciseTime(receiveTime);
            const sendTime = sentCommentsTimestamps.get(comment.content || '');
            if (sendTime !== undefined) {
              const totalLatency = receiveTime - sendTime;
              const sendTimeStr = formatPreciseTime(sendTime);
              console.log(`%c[WebSocket Latency] Мой комментарий: "${comment.content}" отправлен в ${sendTimeStr}, получен в ${receiveTimeStr}. Полная задержка (Отправка -> Получение): ${totalLatency}мс`, "color: #10b981; font-weight: bold;");
              sentCommentsTimestamps.delete(comment.content || '');
            } else {
              const serverTime = new Date(comment.timestamp).getTime();
              const latency = receiveTime - serverTime;
              console.log(`%c[WebSocket Latency] Комментарий от @${comment.username}: "${comment.content}" получен в ${receiveTimeStr} за ${latency}мс (Бэкенд -> Клиент)`, "color: #10b981; font-weight: bold;");
            }
            comments.value.push(comment);
            scrollToBottom();
          }
        }
      };
      subscribeToTopic<any>(topic, chatCallback);
    }

    try {
      const commentFilters: any = {};

      if (type === 'BLOCK') commentFilters.blockId = id;
      else if (type === 'CHAPTER') commentFilters.chapterId = id;
      else if (type === 'NOVEL') commentFilters.novelId = id;


      const history = await getComments(
        commentFilters,
        currentPage.value,
        size,
        'timestamp,desc'
      );


      comments.value = history.content ? [...history.content].reverse() : [];


      isLastPage.value = history.last;
      await nextTick();
      if (commentsListRef.value) {
        const saved = localStorage.getItem(`scroll_comment_${id}`);
        if (saved) {
          const scrollTopVal = parseInt(saved, 10);
          commentsListRef.value.scrollTop = scrollTopVal;
          setTimeout(() => {
            if (commentsListRef.value) commentsListRef.value.scrollTop = scrollTopVal;
          }, 100);
          setTimeout(() => {
            if (commentsListRef.value) commentsListRef.value.scrollTop = scrollTopVal;
          }, 300);
        } else {
          commentsListRef.value.scrollTop = commentsListRef.value.scrollHeight;
        }
      }
    } catch (e) {
      console.error("Ошибка загрузки чата:", e);
    }
  };
  const loadMoreComments = async () => {
    if (isLoadingMore.value || isLastPage.value || !activeTargetId.value) return;

    isLoadingMore.value = true;
    try {
      currentPage.value++;
      const commentFilters: any = {};

      if (targetType.value  === 'BLOCK') commentFilters.blockId = activeTargetId.value;
      else if (targetType.value === 'CHAPTER') commentFilters.chapterId = activeTargetId.value;
      else if (targetType.value  === 'NOVEL') commentFilters.novelId = activeTargetId.value;


      const history = await getComments(
        commentFilters,
        currentPage.value,
        size,
        'timestamp,desc'
      );

      if (history && history.content) {

        comments.value = [...history.content.reverse(), ...comments.value];

        isLastPage.value = history.last;
      }
    } catch (e) {
      console.error("Ошибка подгрузки истории:", e);
      currentPage.value--;
    } finally {
      isLoadingMore.value = false;
    }
  };

  const send = async (payload: {
    content: string,
    file?: File | null,
    quote?: { text: string, url: string }
  }) => {
    if (!activeTargetId.value || isSending.value) return;

    isSending.value = true;


    const dto: any = {
      content: payload.content,
      type: 'PLAIN'
    };


    if (targetType.value === 'BLOCK') dto.blockId = activeTargetId.value;
    else if (targetType.value === 'CHAPTER') dto.chapterId = activeTargetId.value;
    else if (targetType.value === 'NOVEL') dto.novelId = activeTargetId.value;

    const currentQuote = pendingQuote.value;
    if (currentQuote) {
      dto.type = 'QUOTE';
      dto.quoteText = currentQuote.text;
      dto.anchorUrl = currentQuote.url;
    }

    else if (payload.file) {
      dto.type = 'IMAGE';
    }

    try {


      await createComment(payload.file || null, dto);
      pendingQuote.value = null;
    } catch (e: any) {
      alert(e.message || "Ошибка при отправке");
    } finally {
      isSending.value = false;
    }
  };

  const removeComment = async (commentId: number) => {
    try {
      await deleteComment(commentId);
    } catch (e) {
      alert("Недостаточно прав для удаления");
    }
  };

  const unsubscribeFromCurrent = () => {
    if (activeTargetId.value && chatCallback) {
      const topic = `/topic/${targetType.value.toLowerCase()}.${activeTargetId.value}`;
      unsubscribeFromTopic(topic, chatCallback);
      chatCallback = null;
    }
    comments.value = [];
  };

  const closeChat = () => {
    unsubscribeFromCurrent();
    activeTargetId.value = null;
    isOpen.value = false;
  };

  const setContext = async (novelId: number, novelTitle: string, chapterId: number, chapterTitle: string) => {
    if (currentChapterContext.value && currentChapterContext.value.id !== chapterId) {
      clearContext();
    }
    currentNovelContext.value = { id: novelId, title: novelTitle };
    currentChapterContext.value = { id: chapterId, title: chapterTitle };

    const topic = `/topic/chapter.${chapterId}`;

    if (!contextCallback) {
      contextCallback = (wsEvent: any) => {
        const type = wsEvent.type;
        const payload = wsEvent.payload;
        if (type === 'COMMENT_DELETED') {
          chapterComments.value = chapterComments.value.filter(c => c.id !== payload.id);
        } else if (type === 'COMMENT_CREATED') {
          const comment = payload as CommentResponseDto;
          if (!chapterComments.value.some(c => c.id === comment.id)) {
            chapterComments.value.push(comment);
          }
        }
      };
      subscribeToTopic<any>(topic, contextCallback);
    }

    try {
      const history = await getComments({ chapterId }, 0, 50, 'timestamp,asc');
      chapterComments.value = history.content ? [...history.content] : [];
    } catch (e) {
      console.error("Ошибка загрузки комментариев главы", e);
    }
  };

  const clearContext = () => {
    if (currentChapterContext.value && contextCallback) {
      const topic = `/topic/chapter.${currentChapterContext.value.id}`;
      unsubscribeFromTopic(topic, contextCallback);
      contextCallback = null;
    }
    currentNovelContext.value = null;
    currentChapterContext.value = null;
    chapterComments.value = [];
  };

  return {
    isOpen, comments,scrollToBottom, activeTargetId, groupedComments, targetType, isSending,setQuoteMode ,pendingQuote,
    clearQuote, commentsListRef, openChat, closeChat, send, removeComment ,loadMoreComments, isLoadingMore, isLastPage,
    currentNovelContext, currentChapterContext, chapterComments, setContext, clearContext
  };
});
