import type {
  SocialGraphResponseDto,
  UserRelationRequestDto,
  UserRelationResponseDto
} from "@/types/social.ts";
import apiClient from "@/api/axios.ts";
import type {PagedModel, SliceModel} from "@/types/PagedModel.ts";


export async function follow(dto: UserRelationRequestDto): Promise<UserRelationResponseDto> {
  const response = await apiClient.post<UserRelationResponseDto>("/social/follow", dto);
  return response.data;
}

export async function unfollow(dto: UserRelationRequestDto): Promise<void> {
  await apiClient.post('/social/unfollow', dto);
}

export async function getFollowers(page: number = 0,
                                   size: number = 20,
                                   sort: string = 'updatedAt,desc'): Promise<SliceModel<UserRelationResponseDto>>  {
  const response = await apiClient.get('/social/followers', {
    params: {page, size, sort},
  });
  return response.data;
}

export async function getFollowing(page: number = 0,
                                   size: number = 20,
                                   sort: string = 'updatedAt,desc'): Promise<SliceModel<UserRelationResponseDto>>  {
  const response = await apiClient.get('/social/following', {
    params: {page, size, sort},
  });
  return response.data;
}

export async function sendFriendRequest(dto: UserRelationRequestDto): Promise<UserRelationResponseDto> {
  const response = await apiClient.post('/social/friends/requests', dto);
  return response.data;
}

export async function acceptFriendRequest(dto: UserRelationRequestDto): Promise<UserRelationResponseDto> {
  const response = await apiClient.post('/social/friends/requests/accept', dto);
  return response.data;
}

export async function declineFriendRequest(dto: UserRelationRequestDto): Promise<UserRelationResponseDto> {
  const response = await apiClient.post('/social/friends/requests/decline', dto);
  return response.data;
}

export async function getIncomingRequest(page: number = 0,size: number = 20,sort: string = 'updatedAt,desc'): Promise<SliceModel<UserRelationResponseDto>>  {
  const response = await apiClient.get('/social/friends/requests/incoming', {
    params: {page, size, sort},
  });
  return response.data;
}

export async function getOutgoingRequests(page: number = 0, size: number = 20 ,sort: string = 'updatedAt,desc'): Promise<SliceModel<UserRelationResponseDto>> {
  const response = await apiClient.get('/social/friends/requests/outgoing', {
    params: { page, size ,sort },
  });
  return response.data;
}

export async function getFriends(page: number = 0,size: number = 20,sort: string = 'updatedAt,desc'):Promise<SliceModel<UserRelationResponseDto>> {
  const response = await apiClient.get('/social/friends', {
    params: {page, size, sort},
  })
  return response.data;
}

export async function addCloseFriend(dto: UserRelationRequestDto): Promise<UserRelationResponseDto> {
  const response = await apiClient.post('/social/friends/close', dto);
  return response.data;
}

export async function removeCloseFriend(dto: UserRelationRequestDto): Promise<void> {
  await apiClient.post('/social/friends/close/remove', dto)
}

export async function getCloseFriends(): Promise<Array<UserRelationResponseDto>> {
  const response = await apiClient.get<Array<UserRelationResponseDto>>('/social/friends/close', );
  return response.data;
}

export async function blockUser(dto: UserRelationRequestDto): Promise<UserRelationResponseDto> {
  const response = await apiClient.post('/social/blocks', dto);
  return response.data;
}

export async function unblockUser(dto: UserRelationRequestDto): Promise<void> {
  await apiClient.post('/social/blocks/unblock', dto);
}

export async function getMyBlackList(page: number = 0,size: number = 20,sort: string = 'updatedAt,desc'): Promise<SliceModel<UserRelationResponseDto>> {
  const response = await apiClient.get('/social/blocks', {
    params: {page, size, sort},
  });
  return response.data;
}

export async function getSocialGraph():Promise<SocialGraphResponseDto> {
  const response = await apiClient.get<SocialGraphResponseDto>('/social/graph', );
  return response.data;
}



