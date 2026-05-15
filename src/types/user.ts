export interface UserUpdateRequestDto {
  newUsername: string;
  newEmail: string;
}

export interface ChangePasswordRequestDto {
  oldPassword: string;
  newPassword: string;
}
export enum PrivacyLevel {
  EVERYONE = 'EVERYONE',
  FOLLOWERS = 'FOLLOWERS',
  FRIENDS = 'FRIENDS',
  BEST_FRIENDS= 'BEST_FRIENDS',
  NOBODY = 'NOBODY'
}

export interface UserSettingsResponseDto {
  canSendMessage: PrivacyLevel;
  libraryPrivacy: PrivacyLevel;
}
export interface UserSettingsRequestDto{
  canSendMessage?: PrivacyLevel;
  libraryPrivacy?: PrivacyLevel;
}
