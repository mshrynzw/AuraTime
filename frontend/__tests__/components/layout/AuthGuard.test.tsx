import AuthGuard from "@/components/layout/AuthGuard";
import { cleanup, render, screen, waitFor } from "@testing-library/react";

// useAuthをモック
const mockUseAuth = jest.fn();
jest.mock("@/lib/auth/context", () => ({
  useAuth: () => mockUseAuth(),
}));

// useRouterをモック
const mockPush = jest.fn();
jest.mock("next/navigation", () => ({
  useRouter: () => ({
    push: mockPush,
  }),
}));

describe("AuthGuard", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockPush.mockClear();
  });

  afterEach(() => {
    cleanup();
  });

  it("正常系：認証済みユーザーは子コンポーネントを表示", async () => {
    mockUseAuth.mockReturnValue({
      user: {
        id: "user-id",
        email: "test@example.com",
        familyName: "山田",
        firstName: "太郎",
        companyId: "company-id",
        role: "employee",
      },
      loading: false,
    });

    render(
      <AuthGuard>
        <div>Protected Content</div>
      </AuthGuard>
    );

    await waitFor(() => {
      expect(screen.getByText("Protected Content")).toBeInTheDocument();
    });
    expect(mockPush).not.toHaveBeenCalled();
  });

  it("正常系：ローディング中はローディング画面を表示", async () => {
    mockUseAuth.mockReturnValue({
      user: null,
      loading: true,
    });

    render(
      <AuthGuard>
        <div>Protected Content</div>
      </AuthGuard>
    );

    await waitFor(() => {
      expect(screen.getByText(/読み込み中.../i)).toBeInTheDocument();
    });
    expect(screen.queryByText("Protected Content")).not.toBeInTheDocument();
    expect(mockPush).not.toHaveBeenCalled();
  });

  it("正常系：未認証ユーザーはログインページにリダイレクト", async () => {
    mockUseAuth.mockReturnValue({
      user: null,
      loading: false,
    });

    render(
      <AuthGuard>
        <div>Protected Content</div>
      </AuthGuard>
    );

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith("/login");
    });

    expect(screen.queryByText("Protected Content")).not.toBeInTheDocument();
  });

  it("正常系：認証状態が変更されたときにリダイレクト", async () => {
    // 最初はローディング中
    mockUseAuth.mockReturnValue({
      user: null,
      loading: true,
    });

    const { rerender } = render(
      <AuthGuard>
        <div>Protected Content</div>
      </AuthGuard>
    );

    await waitFor(() => {
      expect(screen.getByText(/読み込み中.../i)).toBeInTheDocument();
    });

    // ローディングが完了し、未認証
    mockUseAuth.mockReturnValue({
      user: null,
      loading: false,
    });
    rerender(
      <AuthGuard>
        <div>Protected Content</div>
      </AuthGuard>
    );

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith("/login");
    });
  });
});
