
export enum RelationStatus{
  PENDING = 'PENDING',
  FRIEND='FRIEND',
}

export interface UserRelationResponseDto{
  id: number;
  userId: number;
  username: string;
  avatarUrl: string;
  status: RelationStatus|null;
  updatedAt: string;
}

export interface UserRelationRequestDto {
  relationId: number|null;
  receiverId: number|null;
}

export interface SocialGraphResponseDto{
  followerIds: Record<number, number>;
  followingIds: Record<number, number>;
  friendIds: Record<number, number>;
  closeFriendIds: Record<number, number>;
  blockIds: Record<number, number>;
  incomingRequestIds: Record<number, number>;
  outgoingRequestIds: Record<number, number>;
}

export enum SocialEventType {
  
  FOLLOW_SUCCESS = 'FOLLOW_SUCCESS',       
  UNFOLLOW_SUCCESS = 'UNFOLLOW_SUCCESS',   
  
  FRIEND_REQUEST_SENT = 'FRIEND_REQUEST_SENT',         
  FRIEND_REQUEST_RECEIVED = 'FRIEND_REQUEST_RECEIVED', 
  FRIEND_REQUEST_ACCEPTED = 'FRIEND_REQUEST_ACCEPTED', 
  FRIEND_REQUEST_DECLINED = 'FRIEND_REQUEST_DECLINED', 
  
  CLOSE_FRIEND_ADDED = 'CLOSE_FRIEND_ADDED',     
  CLOSE_FRIEND_REMOVED = 'CLOSE_FRIEND_REMOVED', 
  
  USER_BLOCKED = 'USER_BLOCKED',     
  USER_UNBLOCKED = 'USER_UNBLOCKED'  
}
