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


export async function getChatMessages(conversationId: number, page: number = 0, size: number = 50,sort: string = 'timestamp,desc'): Promise<SliceModel<MessageResponseDto>> {
  const response = await apiClient.get(`/chats/${conversationId}/messages`, { params: { page, size,sort } });
  return response.data;
}


export async function getOrCreatePrivateChat(targetUserId: number): Promise<ConversationResponseDto> {
  const response = await apiClient.post(`/chats/private/${targetUserId}`);
  return response.data;
}


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


export async function sendMessage(conversationId: number, dto: SendMessageRequestDto, files?: File[]): Promise<void> {
  const formData = new FormData();

  
  formData.append('dto', new Blob([JSON.stringify(dto)], { type: 'application/json' }));

  
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


export async function deleteMessage(messageId: number): Promise<void> {
  const response = await apiClient.delete(`/chats/messages/${messageId}`);
  return response.data;
}


export async function deleteChatForUser(conversationId: number): Promise<void> {
  const response = await apiClient.delete(`/chats/${conversationId}`);
  return response.data;
}


export async function toggleChatSettings(settings: ChatSettingsRequestDto): Promise<void> {
  const response = await apiClient.patch(`/chats/conversation/settings`, settings);
  return response.data;
}


export async function leaveGroup(conversationId: number): Promise<void> {
  const response = await apiClient.post(`/chats/${conversationId}/leave`);
  return response.data;
}


export async function addUserToGroup(conversationId: number, targetUserIds: Array<number>): Promise<ConversationResponseDto> {
  const response = await apiClient.post(`/chats/${conversationId}/members`, targetUserIds);
  return response.data;
}


export async function kickUser(conversationId: number, targetUserId: number): Promise<void> {
  const response = await apiClient.delete(`/chats/${conversationId}/members/${targetUserId}`);
  return response.data;
}


export async function sendTypingStatus(conversationId: number): Promise<void> {
  const response = await apiClient.post(`/chats/${conversationId}/messages/typing`);
  return response.data;
}

export async function markConversationAsRead(conversationId:number): Promise<void> {
  const response = await apiClient.post(`/chats/${conversationId}/read`);
  return response.data;
}
