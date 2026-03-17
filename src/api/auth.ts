import {defineStore} from "pinia";
import {ref,computed} from "vue";
import apiClient from "./axios.ts";
import type {LoginRequest, AuthResponse, RegistrationRequestDto} from "@/types/auth.ts";
import type {UserResponseDto} from "@/types/auth.ts";
import router from "@/router";
import {type AxiosError, isAxiosError} from "axios";

export const useAuthStore = defineStore("auth", ()=> {
  const token = ref<String|null>(localStorage.getItem('jwt_token'));
  const user = ref<string|null>(localStorage.getItem('username'));
  const isAuthenticated = computed(()=> !!token.value);
  const userDetails = ref<UserResponseDto | null>(null);
  const avatarTimestamp = ref<number>(Date.now());


  if (token.value) {
    // Убедитесь, что токен приводится к строке, если используется String|null
    apiClient.defaults.headers.common["Authorization"] = `Bearer ${token.value}`;
  }
  async function login(credentials:LoginRequest){
    try {
      const response = await apiClient.post<AuthResponse>('/auth/login',credentials);
      const {accessToken, username } = response.data ;
      token.value = accessToken;
      user.value = username;
      localStorage.setItem('jwt_token', accessToken);
      localStorage.setItem('username', username);
      if(token.value){
        apiClient.defaults.headers.common["Authorization"] = `Bearer ${accessToken}`;
      }
      fetchUserDetails()
      return true;
    }
    catch(error){
      console.log('Ошибка входа:',error);
      throw error;
    }
  }

  function logout(){
    const response =  apiClient.post<void>('/auth/logout');
    token.value = null;
    user.value = null;
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('username');
    delete apiClient.defaults.headers.common['Authorization'];
  }


  async function register(credentials:RegistrationRequestDto){
    try {
      const response = await apiClient.post<void>('/auth/register',credentials);
      return true;
    }
    catch(error){
      console.log('Ошибка регистрации:',error);
      throw error;
    }
  }

  async function fetchUserDetails() {
    const token = localStorage.getItem('jwt_token');
    if (!token || token === 'null') {
      console.log('Запрос профиля отменен: пользователь не авторизован');
      userDetails.value = null; // Очищаем данные, если были
      return null;
    }
    try {
      const response = await apiClient.get<UserResponseDto>('/users/me');

      userDetails.value = response.data; // ⬅️ Сохраняем полные детали

      return response.data;
    }catch (e) {
      if (isAxiosError(e)) {
        const error = e as AxiosError;
        console.error('Ошибка профиля, статус:', error.response?.status);

        // Обработка 401 (Если токен истек)
        if (error.response && error.response.status === 401) {
          logout();
          router.push('/');
          return; // Прекращаем выполнение функции после разлогина
        }
      } else {
        // Ошибка, не связанная с Axios (например, сетевая ошибка или CORS)
        console.error('Неизвестная ошибка профиля:', e);
      }

      // Выбросим ошибку, чтобы компонент ProfileView смог показать сообщение "Не удалось загрузить данные"
      throw e;
    }
  }
  function setDetails(details: UserResponseDto) {
    avatarTimestamp.value = Date.now();
    userDetails.value = details;
    user.value = details.username;
    localStorage.setItem('username', details.username);
  }

  return {token, user, isAuthenticated,userDetails,avatarTimestamp,login,logout,register,fetchUserDetails,setDetails};
});

