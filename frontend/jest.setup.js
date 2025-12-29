// Learn more: https://github.com/testing-library/jest-dom
import "@testing-library/jest-dom";

// crypto.randomUUIDのポリフィル（Jest環境で利用可能にする）
if (!global.crypto) {
  global.crypto = {};
}
if (!global.crypto.randomUUID) {
  global.crypto.randomUUID = () => {
    return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
      const r = (Math.random() * 16) | 0;
      const v = c === "x" ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });
  };
}

// Next.js 16 App Router のフックをモック
const mockPush = jest.fn();
const mockReplace = jest.fn();
const mockGet = jest.fn(() => null);
const mockRedirect = jest.fn((url) => {
  throw new Error("NEXT_REDIRECT"); // Next.jsのredirectはエラーを投げる
});

jest.mock("next/navigation", () => ({
  useRouter: jest.fn(() => ({
    push: mockPush,
    replace: mockReplace,
    refresh: jest.fn(),
    back: jest.fn(),
    forward: jest.fn(),
    prefetch: jest.fn(),
  })),
  useSearchParams: jest.fn(() => ({
    get: mockGet,
    has: jest.fn(),
    getAll: jest.fn(),
    keys: jest.fn(),
    values: jest.fn(),
    entries: jest.fn(),
    forEach: jest.fn(),
    size: 0,
    sort: jest.fn(),
    toString: jest.fn(),
  })),
  redirect: mockRedirect,
}));

// useAuth をモック
const mockLogin = jest.fn();
jest.mock("@/lib/auth/context", () => ({
  useAuth: jest.fn(() => ({
    login: mockLogin,
    user: null,
    loading: false,
    logout: jest.fn(),
    refreshUser: jest.fn(),
  })),
}));

// authApi をモック
const mockAuthApiLogin = jest.fn();
const mockAuthApiRegister = jest.fn();
const mockAuthApiGetMe = jest.fn();
const mockAuthApiGetInvitation = jest.fn();
const mockAuthApiRequestPasswordReset = jest.fn();
const mockAuthApiConfirmPasswordReset = jest.fn();

jest.mock("@/lib/api/auth", () => ({
  authApi: {
    login: mockAuthApiLogin,
    register: mockAuthApiRegister,
    getMe: mockAuthApiGetMe,
    getInvitation: mockAuthApiGetInvitation,
    requestPasswordReset: mockAuthApiRequestPasswordReset,
    confirmPasswordReset: mockAuthApiConfirmPasswordReset,
  },
}));

// apiClient もモック（念のため）
const mockApiClientPost = jest.fn();
const mockApiClientGet = jest.fn();

jest.mock("@/lib/api/client", () => ({
  apiClient: {
    post: mockApiClientPost,
    get: mockApiClientGet,
    interceptors: {
      request: { use: jest.fn() },
      response: { use: jest.fn() },
    },
  },
}));

// テストファイルから参照できるようにグローバルに公開
global.mockPush = mockPush;
global.mockReplace = mockReplace;
global.mockGet = mockGet;
global.mockRedirect = mockRedirect;
global.mockLogin = mockLogin;
global.mockAuthApiLogin = mockAuthApiLogin;
global.mockAuthApiRegister = mockAuthApiRegister;
global.mockAuthApiGetMe = mockAuthApiGetMe;
global.mockAuthApiGetInvitation = mockAuthApiGetInvitation;
global.mockAuthApiRequestPasswordReset = mockAuthApiRequestPasswordReset;
global.mockAuthApiConfirmPasswordReset = mockAuthApiConfirmPasswordReset;
global.mockApiClientPost = mockApiClientPost;
global.mockApiClientGet = mockApiClientGet;
