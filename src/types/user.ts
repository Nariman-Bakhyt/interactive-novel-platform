export interface UserUpdateRequestDto {
  newUsername: string;
  newEmail: string;
}

export interface ChangePasswordRequestDto {
  oldPassword: string;
  newPassword: string;
}

