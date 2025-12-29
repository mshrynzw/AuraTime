import { authApi } from "@/lib/api/auth";
import { apiClient } from "@/lib/api/client";

// jest.setup.jsのモックを無効化して、apiClientをモック
jest.unmock("@/lib/api/client");
jest.unmock("@/lib/api/auth");

// apiClientをモック
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    post: jest.fn(),
    get: jest.fn(),
  },
}));

const mockApiClient = apiClient as jest.Mocked<typeof apiClient>;

describe("authApi", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("register", () => {
    it("正常系：ユーザー登録APIを呼び出す", async () => {
      const mockResponse = {
        data: {
          success: true,
        },
      };

      mockApiClient.post.mockResolvedValue(mockResponse);

      const result = await authApi.register({
        invitationToken: "test-token",
        email: "test@example.com",
        password: "Password123!@#",
        familyName: "山田",
        firstName: "太郎",
      });

      expect(mockApiClient.post).toHaveBeenCalledWith("/v1/auth/register", {
        invitationToken: "test-token",
        email: "test@example.com",
        password: "Password123!@#",
        familyName: "山田",
        firstName: "太郎",
      });

      expect(result).toEqual(mockResponse.data);
    });
  });

  describe("login", () => {
    it("正常系：ログインAPIを呼び出す", async () => {
      const mockResponse = {
        data: {
          success: true,
          data: {
            token: "test-token",
            user: {
              id: "user-id",
              email: "test@example.com",
              familyName: "山田",
              firstName: "太郎",
              companyId: "company-id",
              role: "employee",
            },
          },
        },
      };

      mockApiClient.post.mockResolvedValue(mockResponse);

      const result = await authApi.login({
        email: "test@example.com",
        password: "Password123!@#",
      });

      expect(mockApiClient.post).toHaveBeenCalledWith("/v1/auth/login", {
        email: "test@example.com",
        password: "Password123!@#",
      });

      expect(result).toEqual(mockResponse.data);
    });
  });

  describe("getMe", () => {
    it("正常系：ユーザー情報取得APIを呼び出す", async () => {
      const mockResponse = {
        data: {
          success: true,
          data: {
            id: "user-id",
            email: "test@example.com",
            familyName: "山田",
            firstName: "太郎",
            companyId: "company-id",
            role: "employee",
          },
        },
      };

      mockApiClient.get.mockResolvedValue(mockResponse);

      const result = await authApi.getMe();

      expect(mockApiClient.get).toHaveBeenCalledWith("/v1/auth/me");
      expect(result).toEqual(mockResponse.data);
    });
  });

  describe("getInvitation", () => {
    it("正常系：招待情報取得APIを呼び出す", async () => {
      const mockResponse = {
        data: {
          success: true,
          data: {
            id: "invitation-id",
            companyId: "company-id",
            companyName: "テスト会社",
            email: "test@example.com",
            role: "employee",
            employeeNo: "EMP001",
            expiresAt: "2026-01-01T00:00:00Z",
          },
        },
      };

      mockApiClient.get.mockResolvedValue(mockResponse);

      const result = await authApi.getInvitation("test-token");

      expect(mockApiClient.get).toHaveBeenCalledWith(
        "/v1/auth/invitations/test-token"
      );
      expect(result).toEqual(mockResponse.data);
    });
  });

  describe("requestPasswordReset", () => {
    it("正常系：パスワードリセット要求APIを呼び出す", async () => {
      const mockResponse = {
        data: {
          success: true,
        },
      };

      mockApiClient.post.mockResolvedValue(mockResponse);

      const result = await authApi.requestPasswordReset({
        email: "test@example.com",
      });

      expect(mockApiClient.post).toHaveBeenCalledWith(
        "/v1/auth/password-reset/request",
        {
          email: "test@example.com",
        }
      );

      expect(result).toEqual(mockResponse.data);
    });
  });

  describe("confirmPasswordReset", () => {
    it("正常系：パスワードリセット確認APIを呼び出す", async () => {
      const mockResponse = {
        data: {
          success: true,
        },
      };

      mockApiClient.post.mockResolvedValue(mockResponse);

      const result = await authApi.confirmPasswordReset({
        token: "test-token",
        newPassword: "NewPassword123!@#",
      });

      expect(mockApiClient.post).toHaveBeenCalledWith(
        "/v1/auth/password-reset/confirm",
        {
          token: "test-token",
          newPassword: "NewPassword123!@#",
        }
      );

      expect(result).toEqual(mockResponse.data);
    });
  });
});
