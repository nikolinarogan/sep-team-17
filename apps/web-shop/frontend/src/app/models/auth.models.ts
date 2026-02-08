export interface RegisterRequest {
  name: string;
  surname: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  message?: string;
  token?: string | null;
  mustChangePassword?: boolean;
  status?: string;
  email?: string;
}