import apiClient from "@/api/axios.ts";
import type {
  ChatSettingsRequestDto,
  ConversationResponseDto,
  CreateGroupRequest,
  MessageResponseDto,
  SendMessageRequestDto
} from "@/types/chat.ts";
import type {PagedModel, SliceModel} from "@/types/PagedModel.ts";


export async function getMyChats(page: number = 0, size: number = 20 ): Promise<PagedModel<ConversationResponseDto>> {
  const response = await apiClient.get('/chats', { params: { page, size} });
  return response.data;
}

// 2. Получить историю сообщений конкретного чата
export async function getChatMessages(conversationId: number, page: number = 0, size: number = 50,sort: string = 'timestamp,desc'): Promise<SliceModel<MessageResponseDto>> {
  const response = await apiClient.get(`/chats/${conversationId}/messages`, { params: { page, size,sort } });
  return response.data;
}

// 3. Начать приватный чат (или получить существующий)
export async function getOrCreatePrivateChat(targetUserId: number): Promise<ConversationResponseDto> {
  const response = await apiClient.post(`/chats/private/${targetUserId}`);
  return response.data;
}

// 4. Создать групповый чат
export const createGroupChat = async (payload: CreateGroupRequest): Promise<ConversationResponseDto> => {
  const formData = new FormData();

  formData.append('title', payload.title);

  payload.memberIds.forEach(id => {
    formData.append('memberIds', id.toString());
  });

  if (payload.avatarUrl) {
    formData.append('avatarUrl', payload.avatarUrl);
  }
  const response = await apiClient.post<ConversationResponseDto>('/chats/group', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
  return response.data;
}

// 5. Отправить сообщение (с файлами)
export async function sendMessage(conversationId: number, dto: SendMessageRequestDto, files?: File[]): Promise<void> {
  const formData = new FormData();

  // Упаковываем DTO как JSON Blob (важно для Spring @RequestPart)
  formData.append('dto', new Blob([JSON.stringify(dto)], { type: 'application/json' }));

  // Добавляем файлы, если они есть
  if (files && files.length > 0) {
    files.forEach(file => formData.append('files', file));
  }

  const response = await apiClient.post(`/chats/${conversationId}/messages`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
}

// 6. Удалить сообщение
export async function deleteMessage(messageId: number): Promise<void> {
  const response = await apiClient.delete(`/chats/messages/${messageId}`);
  return response.data;
}

// 7. Скрыть/Удалить чат для себя
export async function deleteChatForUser(conversationId: number): Promise<void> {
  const response = await apiClient.delete(`/chats/${conversationId}`);
  return response.data;
}

// 8. Настройки чата (Закрепить / Mute)
export async function toggleChatSettings(settings: ChatSettingsRequestDto): Promise<void> {
  const response = await apiClient.patch(`/chats/conversation/settings`, settings);
  return response.data;
}

// 9. Выйти из группы
export async function leaveGroup(conversationId: number): Promise<void> {
  const response = await apiClient.post(`/chats/${conversationId}/leave`);
  return response.data;
}

// 10. Добавить пользователя в группу
export async function addUserToGroup(conversationId: number, targetUserIds: Array<number>): Promise<ConversationResponseDto> {
  const response = await apiClient.post(`/chats/${conversationId}/members`, targetUserIds);
  return response.data;
}

// 11. Исключить пользователя из группы
export async function kickUser(conversationId: number, targetUserId: number): Promise<void> {
  const response = await apiClient.delete(`/chats/${conversationId}/members/${targetUserId}`);
  return response.data;
}

// 12. Отправить статус "Печатает..."
export async function sendTypingStatus(conversationId: number): Promise<void> {
  const response = await apiClient.post(`/chats/${conversationId}/messages/typing`);
  return response.data;
}

export async function markConversationAsRead(conversationId:number): Promise<void> {
  const response = await apiClient.post(`/chats/${conversationId}/read`);
  return response.data;
}
