import { apiClient } from "./client";
import { ApiResponse } from "./types";

export interface RegisterRequest {
  email: string;
  password: string;
  familyName: string;
  firstName: string;
  familyNameKana?: string;
  firstNameKana?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: {
    id: string;
    email: string;
    familyName: string;
    firstName: string;
    companyId: string;
    role: string;
  };
}

export interface MeResponse {
  id: string;
  email: string;
  familyName: string;
  firstName: string;
  familyNameKana?: string;
  firstNameKana?: string;
  status: string;
  companyId: string;
  role: string;
}

export const authApi = {
  register: async (data: RegisterRequest): Promise<ApiResponse<void>> => {
    const response = await apiClient.post<ApiResponse<void>>("/v1/auth/register", data);
    return response.data;
  },

  login: async (data: LoginRequest): Promise<ApiResponse<LoginResponse>> => {
    const response = await apiClient.post<ApiResponse<LoginResponse>>("/v1/auth/login", data);
    return response.data;
  },

  getMe: async (): Promise<ApiResponse<MeResponse>> => {
    const response = await apiClient.get<ApiResponse<MeResponse>>("/v1/auth/me");
    return response.data;
  },
};




