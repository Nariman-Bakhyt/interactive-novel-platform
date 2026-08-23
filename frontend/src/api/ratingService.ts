import apiClient from "./axios.ts";
import type {AllRatingsResponseDto, RatingRequestDto, RatingResponseDto} from "@/types/rating.ts";


export async function setRating(novelId:number,data:RatingRequestDto ): Promise<RatingResponseDto> {
  const response = await apiClient.post<RatingResponseDto>(`/rating/${novelId}`, data);
  return response.data
}
export async function deleteRating(novelId:number,ratingId: number): Promise<void> {
  await apiClient.delete<void>(`/rating/${novelId}/${ratingId}`);
}
export async function getRatings(novelId: number, page: number = 0,
                                 size: number = 20,
                                 sort: string = 'timestamp,asc'
): Promise<AllRatingsResponseDto> {
  const response= await apiClient.get<AllRatingsResponseDto>(`/rating/public/${novelId}`, {
    params: { page, size, sort }
  });
  return response.data;
}
