import type {PrivacyLevel} from "@/types/user.ts";
import type {NovelResponseDto} from "@/types/novel.ts";export enum LibraryStatus {
  READING = 'READING',
  PLANNING = 'PLANNING',
  COMPLETED = 'COMPLETED',
  DROPPED = 'DROPPED',
}


export interface UserLibraryRequestDto{
  novelId: number;
  status: LibraryStatus;
  privacyLevel: PrivacyLevel;

}

export interface UserLibraryResponseDto {
  novel: NovelResponseDto;
  status: LibraryStatus;
  createdAt: string;
  privacyLevel: PrivacyLevel;
}

export interface UserLibraryStatusDto{
  novelId: number;
  status: LibraryStatus;
}
