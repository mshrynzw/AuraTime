import { apiClient } from "./client";
import { ApiResponse } from "./types";

export interface RegisterRequest {
  invitationToken: string;
  email: string;
  password?: string; // 新規ユーザーの場合必須
  familyName?: string; // 新規ユーザーの場合必須
  firstName?: string; // 新規ユーザーの場合必須
  familyNameKana?: string;
  firstNameKana?: string;
}

export interface InvitationResponse {
  id: string;
  companyId: string;
  companyName: string;
  email: string;
  role: string;
  employeeNo: string;
  employmentType?: string;
  hireDate?: string;
  expiresAt: string;
}

export interface PasswordResetRequestRequest {
  email: string;
}

export interface PasswordResetConfirmRequest {
  token: string;
  newPassword: string;
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
    const response = await apiClient.post<ApiResponse<void>>(
      "/v1/auth/register",
      data
    );
    return response.data;
  },

  login: async (data: LoginRequest): Promise<ApiResponse<LoginResponse>> => {
    const response = await apiClient.post<ApiResponse<LoginResponse>>(
      "/v1/auth/login",
      data
    );
    return response.data;
  },

  getMe: async (): Promise<ApiResponse<MeResponse>> => {
    const response =
      await apiClient.get<ApiResponse<MeResponse>>("/v1/auth/me");
    return response.data;
  },

  getInvitation: async (
    token: string
  ): Promise<ApiResponse<InvitationResponse>> => {
    const response = await apiClient.get<ApiResponse<InvitationResponse>>(
      `/v1/auth/invitations/${token}`
    );
    return response.data;
  },

  requestPasswordReset: async (
    data: PasswordResetRequestRequest
  ): Promise<ApiResponse<void>> => {
    const response = await apiClient.post<ApiResponse<void>>(
      "/v1/auth/password-reset/request",
      data
    );
    return response.data;
  },

  confirmPasswordReset: async (
    data: PasswordResetConfirmRequest
  ): Promise<ApiResponse<void>> => {
    const response = await apiClient.post<ApiResponse<void>>(
      "/v1/auth/password-reset/confirm",
      data
    );
    return response.data;
  },
};
