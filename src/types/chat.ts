import type {Metadata} from "@/types/comment.ts";

export enum ConversationType {
  PRIVATE = 'PRIVATE',
  GROUP = 'GROUP',
}
export enum ConversationMembersRole {
  MEMBER = 'MEMBER',
  ADMIN = 'ADMIN'
}
export interface ChatMemberDto{
  userId: number;
  username: string;
  avatarUrl: string;
  role: ConversationMembersRole;
}


export interface ConversationResponseDto{
  id: number;
  type: ConversationType;
  title: string;
  avatarUrl: string;
  lastMessageAt: string;
  lastMessagePreview: string;
  members: ChatMemberDto[];
  blocked: boolean;
  lastReadAt: string;
  isMuted: boolean;
  isPinned: boolean;
}

export interface SendMessageRequestDto {
  conversationId: number;
  content?: string;
  type?: string;        // 'PLAIN', 'IMAGE', 'QUOTE'
  quoteText?: string;
  anchorUrl?: string;
}

export interface MessageResponseDto {
  id: number;
  conversationId: number;
  content: string;
  timestamp: string;
  metadata: Metadata;

  senderId: number;
  senderUsername: string;
  senderAvatarUrl: string;
}
export interface ChatSettingsRequestDto{
  conversationId: number;
  isPinned?: boolean;
  isMuted?: boolean;
}
export interface CreateGroupRequest {
  title: string;
  avatarUrl: File | null;
  memberIds: number[];
}

export enum WsEventType{
  NEW_MESSAGE = 'NEW_MESSAGE',
  MESSAGE_DELETED = 'MESSAGE_DELETED',
  CHAT_UPDATED = 'CHAT_UPDATED',
  READ_UPDATE = 'READ_UPDATE',
  USER_TYPING= 'USER_TYPING',
}
