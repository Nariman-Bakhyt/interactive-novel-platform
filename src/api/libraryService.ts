import apiClient from './axios';
import type {
  UserLibraryRequestDto,
  UserLibraryResponseDto,
  UserLibraryStatusDto
} from "@/types/library.ts";

export async function addToLibraryApi(data: UserLibraryRequestDto): Promise<UserLibraryResponseDto> {
  const response = await apiClient.put('/userlibrary', data);
  return response.data;
}

export async function removeFromLibraryApi(novelId: number): Promise<void> {
  await apiClient.delete(`/userlibrary/${novelId}`);
}

export async function getUserLibrary(userId:number ,
                                     page = 0,
                                     size = 20,
                                     sort: string = 'createdAt,desc'): Promise<any> {
  const response = await apiClient.get(`/userlibrary/${userId}`,{
    params: {page,size,sort }
  });
  return response.data;

}

export async function getLibraryStatusesApi(): Promise<UserLibraryStatusDto[]> {
  const response = await apiClient.get('/userlibrary/statuses');
  return response.data;
}
