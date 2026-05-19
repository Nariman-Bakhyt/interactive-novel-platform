export interface NovelResponseDto {
  id:number;
  title: string ;
  status: string;
  description: string;
  publicationDate:string;
  chapterCount: number;
  totalScore: number ;
  ratingCount: number;
  viewCount:bigint;
  authorName: string;
  coverUrl:string;
  tags:Array<TagOrGenreResponseDto>;
  genres:Array<TagOrGenreResponseDto>;
}
export interface NovelRequestDto {
  title: string;
  status: string;
  description: string;
  coverImage:File|null
  tags:Array<number>;
  genres:Array<number>;
}
export type NovelUpdateRequestDto = Partial<NovelRequestDto>;

export interface TagOrGenreResponseDto{
  id: number;
  name: string;
}
export enum novelStatus {
  RETRACTED = 'RETRACTED',
  ARCHIVED = 'ARCHIVED',
  DRAFT = 'DRAFT',
  COMPLETED = 'COMPLETED',
  IN_PROGRESS = 'IN_PROGRESS',
  HIATUS = 'HIATUS',
}

export interface NovelSearchRequestDto{
  title: string | null;           
  authorId?: number | null;
  includedGenreIds: number[];      
  excludedGenreIds: number[];
  includedTagIds: number[];
  excludedTagIds: number[];
  minRating: number | null;        
  maxRating?: number | null;
  status: novelStatus | null;
}

export enum ChapterStatus {
  DRAFT = 'DRAFT',
  SCHEDULED = 'SCHEDULED',
  PUBLISHED = 'PUBLISHED',
}

export interface ChapterShortResponseDto {
  id: number;
  title: string;
  chapterNumber: number;
  status: ChapterStatus;
  publishedAt: string | null;
}

export interface NovelAndChapterShortResponseDto{
  novel:NovelResponseDto;
  chapters:Array<ChapterShortResponseDto>;
}

export interface ChapterBlockRequestDto{
  id: number|null;
  sequenceOrder: number;
  type: string;
  content: string;
}

export interface ChapterBlockResponseDto{
  id: number;
  sequenceOrder: number;
  type: string;
  content: string;
}

export interface ChapterRequestDto{
  title: string;
  blocks: ChapterBlockRequestDto[];
}

export interface ChapterResponseDto{
  id: number;
  chapterNumber: number;
  title: string;
  status: ChapterStatus;
  publishedAt: string | null;
  blocks: ChapterBlockResponseDto[];
}
