import apiClient from "./axios.ts";
import type {CommentRequestDto, CommentResponseDto} from "@/types/comment.ts";

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  last: boolean;
}

export async function getComments(params: Partial<CommentRequestDto>, page = 0,
                                  size = 20,
                                  sort: string = 'timestamp,asc'
): Promise<PageResponse<CommentResponseDto>> {
  const response = await apiClient.get<PageResponse<CommentResponseDto>>(`/comments/public`, {
    params: { ...params, page, size,sort }
  });
  return response.data;
}
export async function deleteComment(commentId: number): Promise<void> {
  await apiClient.delete(`/comments/${commentId}`)

}


