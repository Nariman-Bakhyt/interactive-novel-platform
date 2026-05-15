import apiClient from "./axios.ts";
import type {ProfileResponseDto, UserResponseDto} from "@/types/auth.ts";
import type {UserUpdateRequestDto, ChangePasswordRequestDto} from "@/types/user.ts";
import type {AxiosError, AxiosResponse} from "axios";
import type {PagedModel} from "@/types/PagedModel.ts";

export async function uploadAvatar(file: File|null):Promise<ProfileResponseDto> {

  const formData = new FormData();
  if (file) {
    formData.append("file", file);
  }
  try {
    const response: AxiosResponse<ProfileResponseDto> = await apiClient.post(
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


export async function updateProfileApi(dto: UserUpdateRequestDto): Promise<ProfileResponseDto> {
  const response: AxiosResponse<ProfileResponseDto> = await apiClient.post(
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
export async function searchUsers(username:string , page:number, size:number ):Promise<PagedModel<UserResponseDto>> {
  const response = await apiClient.get('/users/public/search', {
    params: {username, page, size}
  })
  return response.data;
}
