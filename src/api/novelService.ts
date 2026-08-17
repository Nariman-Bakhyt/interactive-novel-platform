import apiClient from "./axios.ts";
import type {
  ChapterRequestDto,
  ChapterResponseDto,
  NovelAndChapterShortResponseDto,
  NovelRequestDto,
  NovelResponseDto,
  NovelSearchRequestDto,
  NovelUpdateRequestDto,
  TagOrGenreResponseDto
} from "@/types/novel";
import {type AxiosResponse} from "axios";
import type {PagedModel} from "@/types/PagedModel.ts";

export const createNovel = async (payload: NovelRequestDto) => {
  const formData = new FormData();

  formData.append('title', payload.title);
  formData.append('description', payload.description);
  formData.append('status', payload.status);

  if (payload.coverImage) {
    formData.append('coverImage', payload.coverImage); 
  }

  payload.genres.forEach(id => formData.append('genres', id.toString()));
  payload.tags.forEach(id => formData.append('tags', id.toString()));

  const response = await apiClient.post('/novels', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return response.data;
};

export async function updateNovel(data: NovelUpdateRequestDto, novelId:number): Promise<NovelResponseDto> {
  const response = await apiClient.put(`/novels/${novelId}`, data);
  return response.data
}

export async function uploadNovelCover(novelId: number, file: File|null): Promise<NovelResponseDto> {
  const formData = new FormData();
  if (file) {
    formData.append("file", file);
  }
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
  const cached = localStorage.getItem("genres");
  if (cached) {
    return JSON.parse(cached);
  }
  const response: AxiosResponse<TagOrGenreResponseDto[]> = await apiClient.get("/genres/public");
  localStorage.setItem("genres", JSON.stringify(response.data));
  return response.data;
}
export async function getAllTags(): Promise<TagOrGenreResponseDto[]> {
  const cached = localStorage.getItem("tags");
  if (cached) {
    return JSON.parse(cached);
  }
  const response: AxiosResponse<TagOrGenreResponseDto[]> = await apiClient.get("/tags/public");
  localStorage.setItem("tags", JSON.stringify(response.data));
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
export const findAllNovels = async (dto: NovelSearchRequestDto, page: number, size: number, sort: string = 'lastChapterAddedAt,desc') => {
  const response = await apiClient.post(`/novels/public?page=${page}&size=${size}&sort=${sort}`, dto);
  return response.data;
};
export async function searchNovels(title:string , page:number, size:number ):Promise<PagedModel<NovelResponseDto>> {
  const response = await apiClient.get('/novels/public/search', {
    params: {title, page, size}
  })
  return response.data;
}

export async function deleteNovel(novelId: number): Promise<void> {
  await apiClient.delete(`/novels/${novelId}`);
}

export async function deleteChapter(novelId: number, chapterId: number): Promise<void> {
  await apiClient.delete(`/novels/${novelId}/chapter/${chapterId}`);
}

export async function updateChapterPublishTime(novelId: number, chapterId: number, publishTime: string | null): Promise<ChapterResponseDto> {
  const params: Record<string, string> = {};
  if (publishTime) {
    params.publishTime = publishTime;
  }
  const response = await apiClient.put(`/novels/${novelId}/chapter/${chapterId}/publish`, null, { params });
  return response.data;
}

export async function uploadChapterImage(novelId: number, file: File): Promise<{ url: string }> {
  const formData = new FormData();
  formData.append("file", file);
  const response = await apiClient.post(`/novels/${novelId}/chapter-images`, formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    }
  });
  return response.data;
}

export async function deleteChapterImage(novelId: number, url: string): Promise<void> {
  await apiClient.delete(`/novels/${novelId}/chapter-images`, {
    params: { url }
  });
}
