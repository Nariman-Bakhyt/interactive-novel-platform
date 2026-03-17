export interface CommentRequestDto {
  content?: string;
  parentCommentId?: number;
  blockId?: number;
  chapterId?: number;
  novelId?: number;
  forumTopicId?: number;
  channelId?: number;

}

export interface CommentResponseDto extends CommentRequestDto{
  id: number;
  timestamp: string;
  userId: number;
  username: string;
  userAvatarUrl: string;

}
