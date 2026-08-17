export interface NotificationResponseDto {
    id: number;
    senderId?: number;
    senderName?: string;
    senderAvatar?: string;
    type: 'NEW_NOVEL' | 'NEW_CHAPTER';
    metadata: Record<string, any>;
    relatedEntityId?: number;
    isRead: boolean;
    createdAt: string;
}

export type NotificationEventType = 'NOTIFICATION_RECEIVED' | 'NOTIFICATION_REVOKED';

