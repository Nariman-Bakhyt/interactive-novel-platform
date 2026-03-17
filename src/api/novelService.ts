import apiClient from "./axios.ts";
import type {
  ChapterRequestDto, ChapterResponseDto,
  NovelAndChapterShortResponseDto, NovelRequestDto,
  NovelResponseDto, NovelSearchRequestDto,
  NovelUpdateRequestDto,
  TagOrGenreResponseDto
} from "@/types/novel";
import axios, { type AxiosResponse } from "axios";

export async function createNovel(data: NovelRequestDto): Promise<NovelResponseDto> {
  const response = await apiClient.post('/novels', data);
  return response.data
}
export async function updateNovel(data: NovelUpdateRequestDto, novelId:number): Promise<NovelResponseDto> {
  const response = await apiClient.put(`/novels/${novelId}`, data);
  return response.data
}

export async function uploadNovelCover(novelId: number, file: File): Promise<NovelResponseDto> {
  const formData = new FormData();
  formData.append("file", file);
  const response: AxiosResponse<NovelResponseDto> = await apiClient.post(
    `/novels/${novelId}/cover`,
    formData,
    {
      headers: {
        "Content-Type": "multipart/form-data",
      }
    }
  );
  return response.data;
}

export async function getNovelById(novelId: number):Promise<NovelAndChapterShortResponseDto> {
  const response: AxiosResponse<NovelAndChapterShortResponseDto> = await apiClient.get<NovelAndChapterShortResponseDto>(
    `/novels/public/${novelId}`);
  return response.data;
}
export async function getAllNovels(page: number=0, size: number = 20): Promise<any> {
  const response = await apiClient.get(`/novels/public`,{
    params: { page, size }
  });
  return response.data;
}
export async function getNewNovels(page: number=0, size: number = 20): Promise<any> {
  const response = await apiClient.get(`/novels/public/new`,{
    params: { page, size }
  });
  return response.data;
}
export async function getMyNovels(page: number=0, size: number = 20): Promise<any> {
  const response = await apiClient.get(`/novels/my`,{
    params: { page, size }
  });
  return response.data;
}
export async function getMyNovel(novelId: number): Promise<NovelAndChapterShortResponseDto> {
  const response: AxiosResponse<NovelAndChapterShortResponseDto> = await apiClient.get(`/novels/my/${novelId}`,{
  });
  return response.data;
}
export async function getAllGenres(): Promise<TagOrGenreResponseDto[]> {
  const response: AxiosResponse<TagOrGenreResponseDto[]> = await apiClient.get("/genres/public");
  return response.data;
}
export async function getAllTags(): Promise<TagOrGenreResponseDto[]> {
  const response: AxiosResponse<TagOrGenreResponseDto[]> = await apiClient.get("/tags/public");
  return response.data;
}
export async function createChapter(novelId: number, dto: ChapterRequestDto): Promise<ChapterResponseDto> {
  const response = await apiClient.post(`/novels/${novelId}/addchapter`, dto);
  return response.data;
}

export async function updateChapter(novelId: number, chapterId: number, dto: ChapterRequestDto): Promise<ChapterResponseDto> {
  const response = await apiClient.put(`/novels/${novelId}/updatechapter/${chapterId}`, dto);
  return response.data;
}

export async function getChapter(novelId: number,chapterId: number): Promise<ChapterResponseDto> {
  const response = await apiClient.get(`/novels/public/${novelId}/chapter/${chapterId}`);
  return response.data;
}
export const searchNovels = async (dto: NovelSearchRequestDto, page: number, size: number) => {
  const response = await apiClient.post(`/novels/public?page=${page}&size=${size}`, dto);
  return response.data;
};
