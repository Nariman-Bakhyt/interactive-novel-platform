import apiClient from "@/api/axios.ts";
import type {NotificationResponseDto} from "@/types/notification";

export const getNotifications = async (page: number = 0, size: number = 20) => {
    const response = await apiClient.get(`/notifications?page=${page}&size=${size}`);
    return response.data;
};

export const markNotificationAsRead = async (id: number) => {
    await apiClient.post(`/notifications/${id}/read`);
};

export const markAllNotificationsAsRead = async () => {
    await apiClient.post(`/notifications/read-all`);
};

