import axios from "axios";
import {useAuthStore} from "@/api/auth.ts";
import {getCachedVisitorId} from "@/api/fingerprint.ts";

const apiClient = axios.create({
  baseURL: `http://${import.meta.env.VITE_API_IP}:8080/api`,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  }
});

apiClient.interceptors.request.use(
  (config) => {
    const vId = getCachedVisitorId();

    // Если ID еще не готов (мало ли), не вешаем запрос, а шлем что есть
    if (vId) {
      config.headers['X-Visitor-Id'] = vId;
    }

    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

let isRefreshing = false;
let failedQueue: any[] = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    // ВАЖНО: Вызываем хук здесь, чтобы избежать циклических зависимостей при старте приложения
    const authStore = useAuthStore();
    const originalRequest = error.config;

    // КРИТИЧНО: Не пытаемся рефрешить, если сам запрос на рефреш упал
    if (error.response?.status === 401 && originalRequest.url === '/auth/refresh') {
      authStore.logout();
      return Promise.reject(error);
    }

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      // Если рефреш УЖЕ идет, ставим этот запрос в очередь ожидания
      if (isRefreshing) {
        return new Promise(function(resolve, reject) {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          originalRequest.headers['Authorization'] = `Bearer ${token}`;
          return apiClient(originalRequest);
        }).catch(err => {
          return Promise.reject(err);
        });
      }

      // Если мы первые получили 401, блокируем очередь и делаем рефреш
      isRefreshing = true;

      try {
        const newToken = await authStore.refreshToken();

        // Разблокируем очередь: говорим всем ждущим запросам "Токен обновлен, летите!"
        processQueue(null, newToken);

        // Повторяем наш оригинальный запрос
        originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Рефреш не удался (прошло 30 дней или сессия убита)
        processQueue(refreshError, null);
        authStore.logout();
        return Promise.reject(refreshError);
      } finally {
        // В любом случае снимаем блокировку
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
