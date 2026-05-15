import type {PrivacyLevel} from "@/types/user.ts";

export enum LibraryStatus {
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
  novelId: number;
  title: string;
  coverUrl: string;
  status: LibraryStatus;
  createdAt: string;
  privacyLevel: PrivacyLevel;
}

export interface UserLibraryStatusDto{
  novelId: number;
  status: LibraryStatus;
}
