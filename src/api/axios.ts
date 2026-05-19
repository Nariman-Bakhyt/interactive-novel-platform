import axios from "axios";
import {useAuthStore} from "@/api/auth.ts";
import {getCachedVisitorId} from "@/api/fingerprint.ts";

const apiClient = axios.create({
  baseURL: import.meta.env.PROD ? '/api' : `http://${import.meta.env.VITE_API_IP}:8080/api`,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  }
});

// Инжектируем фингерпринт X-Visitor-Id для неавторизованных (гостевых) пользователей и Bearer JWT для авторизованных.
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

// failedQueue и isRefreshing реализуют слияние (concurrency pooling) параллельных запросов обновления токена. 
// Если одновременно падает несколько параллельных запросов с 401, выполняется ровно один запрос /auth/refresh, 
// а остальные ждут его завершения в очереди. Это защищает бэкенд от спама рефрешами и предотвращает race conditions.
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

    
    if (error.response?.status === 401 && originalRequest.url === '/auth/refresh') {
      authStore.logout();
      return Promise.reject(error);
    }

    if (error.response?.status === 401 && !originalRequest._retry) {
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
        
        processQueue(refreshError, null);
        authStore.logout();
        return Promise.reject(refreshError);
      } finally {
        
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
