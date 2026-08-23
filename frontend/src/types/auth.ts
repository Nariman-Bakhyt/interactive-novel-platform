
export interface LoginRequest {
  username: string;
  password: string;
}
export interface EmailRequest{
  email: string;
}
export interface VerifyLoginCodeRequest{
  email: string;
  code: string;
}
export enum VerificationTokenType{
  REGISTRATION_CONFIRMATION = "REGISTRATION_CONFIRMATION",
  PASSWORD_RESET = "PASSWORD_RESET",
  EMAIL_CHANGE= "EMAIL_CHANGE",
  LOGIN_BY_CODE = "LOGIN_BY_CODE",
}
export interface VerificationRequest{
  userId: number | null;
  type: VerificationTokenType;
  code: string;
}
export interface ResetPasswordRequest{
  userId: number;
  newPassword: string;
}
export interface EmailRequest{
  email: string;
}

export interface AuthResponse {
  accessToken: string;
  username: string;
}
export interface RegistrationRequestDto {
  username:string;
  password:string;
  email:string;
}
export interface UserResponseDto {
  id: number;
  username:string;
  avatarUrl: string | null;
}
export interface ProfileResponseDto extends UserResponseDto {
  email: string | null;
  registrationDate: string;
  isActive: boolean;
  novelsCount:number;
  followersCount:number;
  followingCount:number;
  friendsCount:number;
  bestFriendsCount:number;
  isMyProfile: boolean;
  isFollowed: boolean;
  isFriend: boolean;
  isBestFriend: boolean;
  isBlockedByMe: boolean;
  isBlockedByTarget: boolean;

}

