export interface CommentRequestDto {
  content?: string;
  parentCommentId?: number;
  blockId?: number;
  chapterId?: number;
  novelId?: number;
  forumTopicId?: number;
  channelId?: number;

}
export interface Metadata{
  type?:string;
  images?:string[];
  quoteText?:string;
  anchorUrl?:string;
}

export interface CommentResponseDto extends CommentRequestDto{
  id: number;
  timestamp: string;
  userId: number;
  username: string;
  userAvatarUrl: string;
  metadata:Metadata;


}

