
export interface LoginRequest {
  username: string;
  password: string;
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
  email:string;
  avatarUrl: string | null;
}

