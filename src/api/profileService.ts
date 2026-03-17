import apiClient from "./axios.ts";
import type {UserResponseDto} from "@/types/auth.ts";
import type {UserUpdateRequestDto, ChangePasswordRequestDto} from "@/types/user.ts";
import type {AxiosError, AxiosResponse} from "axios";

export async function uploadAvatar(file: File):Promise<UserResponseDto> {

  const formData = new FormData();
  formData.append("file", file);
  try {
    const response: AxiosResponse<UserResponseDto> = await apiClient.post(
      '/users/me/avatar',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        }
      }
    );
    return response.data;

  } catch (error) {
    const axiosError = error as AxiosError;

    if (axiosError.response) {
      const serverResponse = axiosError.response.data;

      if (axiosError.response.status === 400 && serverResponse && (serverResponse as any).message) {
        throw new Error((serverResponse as any).message);
      }

      throw new Error(`Ошибка загрузки аватара: ${axiosError.response.status}`);
    }

    throw new Error("Ошибка сети или таймаут. Попробуйте позже.");
  }
}


export async function updateProfileApi(dto: UserUpdateRequestDto): Promise<UserResponseDto> {
  const response: AxiosResponse<UserResponseDto> = await apiClient.post(
    '/users/me/update',
    dto
  );
  return response.data;
}

export async function changePasswordApi(dto: ChangePasswordRequestDto): Promise<boolean> {
  const response: AxiosResponse<void> = await apiClient.post(
    '/users/me/password',
    dto
  );
  return true;
}
