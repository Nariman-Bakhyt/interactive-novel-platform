import {defineStore} from "pinia";
import {computed, ref} from "vue";
import apiClient from "./axios.ts";
import {
  type AuthResponse,
  type EmailRequest,
  type LoginRequest,
  type ProfileResponseDto,
  type RegistrationRequestDto,
  type ResetPasswordRequest,
  type VerificationRequest,
  VerificationTokenType,
  type VerifyLoginCodeRequest
} from "@/types/auth.ts";
import type {UserSettingsRequestDto, UserSettingsResponseDto} from "@/types/user.ts";


export const useAuthStore = defineStore("auth", ()=> {
  const token = ref<String|null>(localStorage.getItem('jwt_token'));
  const user = ref<string|null>(localStorage.getItem('username'));
  const isAuthenticated = computed(()=> !!token.value);
  const userDetails = ref<ProfileResponseDto | null>(null);
  const avatarTimestamp = ref<number>(Date.now());
  const userSettings = ref<UserSettingsResponseDto | null>(null);
  const showAuthModal = ref(false);
  const isInitialized = ref(false);
  apiClient.defaults.withCredentials = true;

  async function refreshToken() {
    try {
      const response = await apiClient.post<AuthResponse>('/auth/public/refresh');
      const { accessToken } = response.data;
      token.value = accessToken;
      localStorage.setItem('jwt_token', accessToken);
      return accessToken;
    } catch (error) {
      await logout();
      throw error;
    }
  }
  async function login(credentials:LoginRequest){
    try {
      const response = await apiClient.post<AuthResponse>('/auth/public/login',credentials);
      await setAccessToken(response.data.accessToken,response.data.username);
      await fetchUserDetails();
      await fetchUserSettings();
      return true;
    }
    catch(error){
      console.log('Ошибка входа:',error);
      throw error;
    }
  }

  async function requestLoginByEmail(data: EmailRequest): Promise<string> {
    const response = await apiClient.post<string>('/auth/public/login/email', data);
    return response.data;
  }

  async function setAccessToken(accessToken: string, username: string): Promise<void> {
    token.value = accessToken;
    user.value = username;
    localStorage.setItem('jwt_token', accessToken);
    localStorage.setItem('username', username);
  }

  async function verifyLoginCode(data: VerifyLoginCodeRequest): Promise<boolean> {
    const response = await apiClient.post<AuthResponse>('/auth/public/login/verify', data);
    await setAccessToken(response.data.accessToken,response.data.username);
    await fetchUserDetails();
    await fetchUserSettings();
    return true;
  }

  async function verifyCode(data: VerificationRequest): Promise<string> {
    const endpoint = data.type === VerificationTokenType.REGISTRATION_CONFIRMATION
      ? '/auth/public/register/verify-code'
      : '/auth/public/verify-code';

    const response = await apiClient.post<string>(endpoint, data);
    return response.data;
  }

  async function requestPasswordReset(data: ResetPasswordRequest): Promise<string> {
    const response = await apiClient.post<string>('/auth/password/reset-request', data);
    return response.data;
  }


    async function requestEmailUpdate(data: EmailRequest): Promise<string> {
    const response = await apiClient.post<string>('/auth/email/update-request', data);
    return response.data;
  }


  async function logout() {
    try {
      await apiClient.post('/auth/logout');
    } catch (e) {
      console.error("Ошибка при логауте на сервере", e);
    } finally {
      token.value = null;
      user.value = null;
      userDetails.value = null;
      userSettings.value = null;

      localStorage.removeItem('jwt_token');
      localStorage.removeItem('username');
      isInitialized.value = false;
      showAuthModal.value = false;
    }
  }


  async function register(credentials:RegistrationRequestDto):Promise<number> {
    try {
      const response = await apiClient.post<number>('/auth/public/register',credentials);
      return response.data;
    }
    catch(error){
      console.log('Ошибка регистрации:',error);
      throw error;
    }
  }

  async function fetchUserDetails() {
    const currentToken = localStorage.getItem('jwt_token');
    if (!currentToken || currentToken === 'null') {
      console.log('Запрос профиля отменен: пользователь не авторизован');
      userDetails.value = null;
      return null;
    }
    try {
      const response = await apiClient.get<ProfileResponseDto>('/users/me');
      userDetails.value = response.data;
      return response.data;
    } catch (e) {
      console.error('Не удалось загрузить профиль:', e);
      throw e;
    }
    finally {
      isInitialized.value = true;
    }
  }

  async function fetchUserDetailsById(id:number) {
    const currentToken = localStorage.getItem('jwt_token');
    if (!currentToken || currentToken === 'null') {
      console.log('Запрос профиля отменен: пользователь не авторизован');
      return null;
    }
    try {
      const response = await apiClient.get<ProfileResponseDto>(`/users/profile/${id}`);
      return response.data;
    } catch (e) {
      console.error('Не удалось загрузить профиль:', e);
      throw e;
    }
  }

  async function fetchUserSettings() {
    if (!isAuthenticated.value) return null;
    try {

      const response = await apiClient.get<UserSettingsResponseDto>('/users/setting');
      userSettings.value = response.data;
      return response.data;
    } catch (e) {
      console.error('Ошибка при получении настроек:', e);
      throw e;
    }
  }


  async function updateUserSettings(dto: UserSettingsRequestDto) {
    try {
      const response = await apiClient.patch<UserSettingsResponseDto>('/users/setting', dto);

      userSettings.value = response.data;
      return response.data;
    } catch (e) {
      console.error('Ошибка при обновлении настроек:', e);
      throw e;
    }
  }

  function setDetails(details: ProfileResponseDto) {
    avatarTimestamp.value = Date.now();
    userDetails.value = details ;

    user.value = details.username;
    localStorage.setItem('username', details.username);
  }

  return {
    token, user, isAuthenticated, userDetails, avatarTimestamp, userSettings, showAuthModal,isInitialized
    ,requestLoginByEmail,verifyLoginCode,verifyCode,requestPasswordReset,requestEmailUpdate,
    login, logout, register, fetchUserDetails, fetchUserSettings, updateUserSettings,
    setDetails, refreshToken,fetchUserDetailsById
  };});

