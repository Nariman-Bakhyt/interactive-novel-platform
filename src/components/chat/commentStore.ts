import { defineStore } from 'pinia';
import { ref, computed, nextTick } from 'vue';
import { getComments, deleteComment, createComment } from "@/api/commentService.ts"; // Импортируем createComment
import {
  subscribeToTopic,
  unsubscribeFromTopic,
  activeSubscriptions
} from "@/api/stompService.ts";
import type { CommentResponseDto, CommentRequestDto } from "@/types/comment.ts";

export const useCommentStore = defineStore('chat', () => {
  const isOpen = ref(false);
  const comments = ref<CommentResponseDto[]>([]);
  const activeTargetId = ref<number | null>(null);
  const targetType = ref<'BLOCK' | 'CHAPTER' | 'NOVEL'>('NOVEL');
  const commentsListRef = ref<HTMLElement | null>(null);
  const isSending = ref(false); // Индикатор загрузки
  const pendingQuote = ref<{ text: string, url: string } | null>(null);
  const savedQuote = localStorage.getItem('pending_quote');

  const currentPage = ref(0);
  const isLastPage = ref(false);
  const isLoadingMore = ref(false); // Индикатор загрузки старых сообщений
  const size = 3;
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

  const scrollToBottom = async () => {
    await nextTick();
    if (commentsListRef.value) {
      commentsListRef.value.scrollTop = commentsListRef.value.scrollHeight;
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

    // Сбрасываем пагинацию при открытии нового чата (ТЕПЕРЬ ПО-НАСТОЯЩЕМУ!)
    currentPage.value = 0;
    isLastPage.value = false;
    isLoadingMore.value = false;
    comments.value = []; // На всякий случай жестко чистим массив перед загрузкой

    const topic = `/topic/${type.toLowerCase()}.${id}`;

    if (!activeSubscriptions.has(topic)) {
      subscribeToTopic<any>(topic, (newMsg) => {
        if ('deleted' in newMsg && newMsg.deleted) {
          comments.value = comments.value.filter(c => c.id !== newMsg.id);
        } else {
          const comment = newMsg as CommentResponseDto;
          if (!comments.value.some(c => c.id === comment.id)) {
            comments.value.push(comment);
            scrollToBottom();
          }
        }
      });
    }

    try {
      const commentFilters: any = {};

      if (type === 'BLOCK') commentFilters.blockId = id;
      else if (type === 'CHAPTER') commentFilters.chapterId = id;
      else if (type === 'NOVEL') commentFilters.novelId = id;

// 2. Вызываем функцию, передавая аргументы на свои места
      const history = await getComments(
        commentFilters,    // 1-й аргумент (params)
        currentPage.value,                 // 2-й аргумент (page)
        size,                // 3-й аргумент (size)
        'timestamp,desc'   // 4-й аргумент (sort) - ОБЯЗАТЕЛЬНО desc для чата
      );

      // РАЗВОРАЧИВАЕМ МАССИВ, чтобы новые сообщения были внизу
      comments.value = history.content ? [...history.content].reverse() : [];

      // Проверяем, последняя ли это страница
      if (history.page) {
        isLastPage.value = (history.page.number + 1) >= history.page.totalPages || history.content.length === 0;
      } else {
        isLastPage.value = history.last ?? true;
      }
      scrollToBottom();
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

// 2. Вызываем функцию, передавая аргументы на свои места
      const history = await getComments(
        commentFilters,    // 1-й аргумент (params)
        currentPage.value,                 // 2-й аргумент (page)
        size,                // 3-й аргумент (size)
        'timestamp,desc'   // 4-й аргумент (sort) - ОБЯЗАТЕЛЬНО desc для чата
      );

      if (history && history.content) {
        // Разворачиваем старые сообщения и ставим их В НАЧАЛО массива
        comments.value = [...history.content.reverse(), ...comments.value];

        if (history.page) {
          isLastPage.value = (history.page.number + 1) >= history.page.totalPages || history.content.length === 0;
        } else {
          isLastPage.value = history.last ?? true;
        }
      }
    } catch (e) {
      console.error("Ошибка подгрузки истории:", e);
      currentPage.value--; // Откат страницы при ошибке
    } finally {
      isLoadingMore.value = false;
    }
  };
  /**
   * Универсальный метод отправки (Текст, Фото, Цитата)
   */
  const send = async (payload: {
    content: string,
    file?: File | null,
    quote?: { text: string, url: string }
  }) => {
    if (!activeTargetId.value || isSending.value) return;

    isSending.value = true;

    // 1. Собираем DTO для бэкенда
    const dto: any = {
      content: payload.content,
      type: 'PLAIN'
    };

    // Определяем цель (Target)
    if (targetType.value === 'BLOCK') dto.blockId = activeTargetId.value;
    else if (targetType.value === 'CHAPTER') dto.chapterId = activeTargetId.value;
    else if (targetType.value === 'NOVEL') dto.novelId = activeTargetId.value;

    const currentQuote = pendingQuote.value; // Кешируем значение
    if (currentQuote) {
      dto.type = 'QUOTE';
      dto.quoteText = currentQuote.text;
      dto.anchorUrl = currentQuote.url;
    }
    // 2. Если цитаты нет, проверяем файл
    else if (payload.file) {
      dto.type = 'IMAGE';
    }

    try {
      // 2. Вызываем API сервис (Multipart POST)
      // Мы не пушим результат в массив вручную, так как он прилетит через WebSocket
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
    if (activeTargetId.value) {
      const topic = `/topic/${targetType.value.toLowerCase()}.${activeTargetId.value}`;
      unsubscribeFromTopic(topic);
    }
    comments.value = [];
  };

  const closeChat = () => {
    unsubscribeFromCurrent();
    activeTargetId.value = null;
    isOpen.value = false;
  };

  return {
    isOpen, comments,scrollToBottom, activeTargetId, groupedComments, targetType, isSending,setQuoteMode ,pendingQuote,
    clearQuote, commentsListRef, openChat, closeChat, send, removeComment ,loadMoreComments, isLoadingMore, isLastPage
  };
});
