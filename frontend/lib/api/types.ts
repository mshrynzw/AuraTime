export interface ApiResponse<T> {
  success: boolean;
  data: T;
  requestId?: string;
}

export interface ErrorResponse {
  success: false;
  error: {
    code: string;
    message: string;
    details?: string[];
  };
  requestId?: string;
}




