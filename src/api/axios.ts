import axios from "axios";
import {useAuthStore} from "@/api/auth.ts";
import {getCachedVisitorId} from "@/api/fingerprint.ts";

const apiClient = axios.create({
  baseURL: "/api",
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  }
});

// Добавляем X-Visitor-Id и Bearer JWT при наличии
apiClient.interceptors.request.use(
  (config) => {
    const vId = getCachedVisitorId();

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
    const authStore = useAuthStore();
    const originalRequest = error.config;

    if (error.response?.status === 401 && originalRequest?.url?.includes('/auth/public/refresh')) {
      authStore.logout(true);
      return Promise.reject(error);
    }

    // Если 401 вызван отсутствием guest_id (требуется PoW), либо пользователь не авторизован (нет токена),
    // НЕ пытаемся обновлять токен, а просто отклоняем промис
    const isPoWChallengeRequired = error.response?.data?.requires_challenge === true;
    const hasToken = !!localStorage.getItem('jwt_token');

    if (error.response?.status === 401 && !originalRequest?._retry && !isPoWChallengeRequired && hasToken) {
      originalRequest._retry = true;

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

      isRefreshing = true;

      try {
        const newToken = await authStore.refreshToken();
        processQueue(null, newToken);
        originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        authStore.logout(true);
        processQueue(refreshError, null);
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
