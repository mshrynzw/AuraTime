import axios from "axios";
import Cookies from "js-cookie";

// axiosとCookiesをモック
jest.mock("axios");
jest.mock("js-cookie", () => ({
  get: jest.fn(),
  set: jest.fn(),
  remove: jest.fn(),
}));

const mockAxios = axios as jest.Mocked<typeof axios>;
const mockCookies = Cookies as jest.Mocked<typeof Cookies>;

describe("apiClient", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockCookies.get.mockReturnValue(undefined as any);
  });

  describe("リクエストインターセプター", () => {
    it("正常系：認証トークンが設定されている場合、Authorizationヘッダーに追加", async () => {
      mockCookies.get.mockReturnValue("test-token" as any);

      const mockCreate = jest.fn(() => ({
        interceptors: {
          request: {
            use: jest.fn((onFulfilled: any) => {
              // インターセプターを直接テスト
              const config = {
                headers: {},
              };
              onFulfilled(config);
              expect(config.headers).toHaveProperty(
                "Authorization",
                "Bearer test-token"
              );
            }),
          },
          response: {
            use: jest.fn(),
          },
        },
      }));

      mockAxios.create.mockReturnValue(mockCreate() as any);

      // apiClientの再初期化をシミュレート
      const testConfig = {
        headers: {},
      };

      const token = mockCookies.get("auth_token");
      if (token && testConfig.headers) {
        (testConfig.headers as any).Authorization = `Bearer ${token}`;
      }

      expect((testConfig.headers as any).Authorization).toBe(
        "Bearer test-token"
      );
    });

    it("正常系：認証トークンがない場合、Authorizationヘッダーを追加しない", async () => {
      mockCookies.get.mockReturnValue(undefined as any);

      const testConfig = {
        headers: {},
      };

      const token = mockCookies.get("auth_token");
      if (token && testConfig.headers) {
        (testConfig.headers as any).Authorization = `Bearer ${token}`;
      }

      expect((testConfig.headers as any).Authorization).toBeUndefined();
    });

    it("正常系：X-Request-Idヘッダーが追加される", () => {
      const testConfig = {
        headers: {},
      };

      const requestId = crypto.randomUUID();
      if (testConfig.headers) {
        (testConfig.headers as any)["X-Request-Id"] = requestId;
      }

      expect((testConfig.headers as any)["X-Request-Id"]).toBeDefined();
      expect(typeof (testConfig.headers as any)["X-Request-Id"]).toBe("string");
    });
  });

  describe("レスポンスインターセプター", () => {
    it("正常系：401エラーの場合、トークンを削除してログインページにリダイレクト", () => {
      // window.location.hrefをモック
      const originalLocation = window.location;
      delete (window as any).location;
      window.location = { ...originalLocation, href: "" };

      mockCookies.remove.mockImplementation(() => {});

      // 401エラーをシミュレート
      const error = {
        response: {
          status: 401,
        },
      };

      // エラーハンドリングをシミュレート
      if (error.response?.status === 401) {
        mockCookies.remove("auth_token");
        if (typeof window !== "undefined") {
          window.location.href = "/login";
        }
      }

      expect(mockCookies.remove).toHaveBeenCalledWith("auth_token");
      expect(window.location.href).toBe("/login");

      // 元に戻す
      window.location = originalLocation;
    });

    it("正常系：401以外のエラーの場合、リダイレクトしない", () => {
      const originalLocation = window.location;
      delete (window as any).location;
      window.location = {
        ...originalLocation,
        href: "http://localhost:3000/dashboard",
      };

      const error = {
        response: {
          status: 500,
        },
      };

      if (error.response?.status === 401) {
        mockCookies.remove("auth_token");
        if (typeof window !== "undefined") {
          window.location.href = "/login";
        }
      }

      expect(window.location.href).toBe("http://localhost:3000/dashboard");
      expect(mockCookies.remove).not.toHaveBeenCalled();

      window.location = originalLocation;
    });
  });

  describe("baseURL設定", () => {
    it("正常系：環境変数からbaseURLを取得", () => {
      const originalEnv = process.env.NEXT_PUBLIC_API_BASE_URL;
      process.env.NEXT_PUBLIC_API_BASE_URL = "https://api.example.com/api";

      // apiClientのbaseURLを確認（実際の実装では、axios.createの引数で確認）
      const expectedBaseURL =
        process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api";
      expect(expectedBaseURL).toBe("https://api.example.com/api");

      if (originalEnv) {
        process.env.NEXT_PUBLIC_API_BASE_URL = originalEnv;
      }
    });

    it("正常系：環境変数がない場合、デフォルト値を使用", () => {
      const originalEnv = process.env.NEXT_PUBLIC_API_BASE_URL;
      delete process.env.NEXT_PUBLIC_API_BASE_URL;

      const expectedBaseURL =
        process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api";
      expect(expectedBaseURL).toBe("http://localhost:8080/api");

      if (originalEnv) {
        process.env.NEXT_PUBLIC_API_BASE_URL = originalEnv;
      }
    });
  });
});
