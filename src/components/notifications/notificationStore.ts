import {defineStore} from 'pinia';
import {computed, ref} from 'vue';
import {getNotifications, markNotificationAsRead, markAllNotificationsAsRead} from "@/api/notificationService";
import type {NotificationResponseDto} from "@/types/notification";
import type {WsEventDto} from "@/types/ws";
import {useToastStore} from "@/components/toast/toastStore";

export const useNotificationStore = defineStore('notification', () => {
    const notifications = ref<NotificationResponseDto[]>([]);
    const unreadCount = computed(() => notifications.value.filter(n => !n.isRead).length);
    const isLoading = ref(false);
    const currentPage = ref(0);
    const isLastPage = ref(false);
    const toastStore = useToastStore();

    const normalizeNotification = (n: any): NotificationResponseDto => {
        return {
            ...n,
            isRead: n.isRead !== undefined ? n.isRead : !!n.read
        };
    };

    const fetchNotifications = async (page: number = 0) => {
        if (isLoading.value || (isLastPage.value && page > 0)) return;
        isLoading.value = true;
        try {
            const data = await getNotifications(page, 20);
            const content = (data.content || []).map(normalizeNotification);
            if (page === 0) {
                notifications.value = content;
            } else {
                notifications.value = [...notifications.value, ...content];
            }
            currentPage.value = page;
            isLastPage.value = data.last;
        } catch (e) {
            console.error("Failed to fetch notifications", e);
        } finally {
            isLoading.value = false;
        }
    };

    const handleNotificationEvent = (event: WsEventDto<any>) => {
        if (event.type === 'NOTIFICATION_RECEIVED') {
            const newNotif = normalizeNotification(event.payload);
            notifications.value.unshift(newNotif);
            toastStore.info(`Новое уведомление от ${newNotif.senderName || 'системы'}`);
        } else if (event.type === 'NOTIFICATION_REVOKED') {
            const payload = event.payload as { type: string; novelId?: number; chapterId?: number };
            if (payload.type === 'NEW_CHAPTER' && payload.chapterId) {
                const index = notifications.value.findIndex(n => n.type === 'NEW_CHAPTER' && Number(n.metadata?.chapterId) === payload.chapterId);
                if (index !== -1) notifications.value.splice(index, 1);
            } else if (payload.type === 'NEW_NOVEL' && payload.novelId) {
                const index = notifications.value.findIndex(n => n.type === 'NEW_NOVEL' && Number(n.metadata?.novelId) === payload.novelId);
                if (index !== -1) notifications.value.splice(index, 1);
            } else if (payload.type === 'ALL' && payload.novelId) {
                notifications.value = notifications.value.filter(n => Number(n.metadata?.novelId) !== payload.novelId);
            }
        }
    };

    const markAsRead = async (id: number) => {
        if (!id) return;
        const notif = notifications.value.find(n => n.id === id);
        if (notif && !notif.isRead) {
            notif.isRead = true;
            try {
                await markNotificationAsRead(id);
            } catch (e) {
                notif.isRead = false; // rollback on failure
                console.error("Failed to mark notification as read", e);
            }
        }
    };

    const markAllAsRead = async () => {
        const hasUnread = notifications.value.some(n => !n.isRead);
        if (!hasUnread) return;
        
        notifications.value.forEach(n => n.isRead = true);
        try {
            await markAllNotificationsAsRead();
        } catch (e) {
            console.error("Failed to mark all as read", e);
            // hard to rollback all smoothly without deep copy, refetch instead
            await fetchNotifications(0);
        }
    };

    return {
        notifications,
        unreadCount,
        isLoading,
        currentPage,
        isLastPage,
        fetchNotifications,
        handleNotificationEvent,
        markAsRead,
        markAllAsRead
    };
});
