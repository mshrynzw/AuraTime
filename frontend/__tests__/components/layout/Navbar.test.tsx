import Navbar from "@/components/layout/Navbar";
import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

// useAuthをモック
const mockUseAuth = jest.fn();
const mockLogout = jest.fn();

jest.mock("@/lib/auth/context", () => ({
  useAuth: () => mockUseAuth(),
}));

describe("Navbar", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockLogout.mockClear();
  });

  afterEach(() => {
    cleanup();
  });

  it("正常系：認証済みユーザーの場合、ナビゲーションバーが表示される", () => {
    mockUseAuth.mockReturnValue({
      user: {
        id: "user-id",
        email: "test@example.com",
        familyName: "山田",
        firstName: "太郎",
        companyId: "company-id",
        role: "employee",
      },
      logout: mockLogout,
    });

    render(<Navbar />);

    // h1タグ内のテキストを確認（テキストで検索）
    const heading = screen.getByText("AuraTime");
    expect(heading).toBeInTheDocument();
    expect(heading.tagName).toBe("H1");
  });

  it("正常系：未認証ユーザーの場合、何も表示されない", () => {
    mockUseAuth.mockReturnValue({
      user: null,
      logout: mockLogout,
    });

    const { container } = render(<Navbar />);
    expect(container.firstChild).toBeNull();
  });

  it("正常系：ユーザー名とメールアドレスが表示される", async () => {
    mockUseAuth.mockReturnValue({
      user: {
        id: "user-id",
        email: "test@example.com",
        familyName: "山田",
        firstName: "太郎",
        companyId: "company-id",
        role: "employee",
      },
      logout: mockLogout,
    });

    render(<Navbar />);

    // アバターボタンをクリックしてドロップダウンメニューを開く
    const user = userEvent.setup();
    // AvatarFallbackのテキスト（イニシャル）で確認
    const avatarFallback = await screen.findByText("山");
    expect(avatarFallback).toBeInTheDocument();

    // ドロップダウンメニューを開く
    const avatarButton = avatarFallback.closest("button");
    if (avatarButton) {
      await user.click(avatarButton);
    }

    // ユーザー名とメールアドレスが表示されることを確認
    expect(await screen.findByText("山田 太郎")).toBeInTheDocument();
    expect(await screen.findByText("test@example.com")).toBeInTheDocument();
  });

  it("正常系：ログアウトボタンをクリックするとlogoutが呼ばれる", async () => {
    mockUseAuth.mockReturnValue({
      user: {
        id: "user-id",
        email: "test@example.com",
        familyName: "山田",
        firstName: "太郎",
        companyId: "company-id",
        role: "employee",
      },
      logout: mockLogout,
    });

    render(<Navbar />);

    const user = userEvent.setup();
    // AvatarFallbackのテキスト（イニシャル）を含む要素を探す
    const avatarFallback = await screen.findByText("山");
    const avatarButton = avatarFallback.closest("button");
    if (avatarButton) {
      await user.click(avatarButton);
    }

    // ドロップダウンメニューが開くのを待つ
    const logoutButton = await screen.findByText("ログアウト");
    await user.click(logoutButton);

    expect(mockLogout).toHaveBeenCalled();
  });

  it("正常系：カスタムアプリ名が表示される", () => {
    mockUseAuth.mockReturnValue({
      user: {
        id: "user-id",
        email: "test@example.com",
        familyName: "山田",
        firstName: "太郎",
        companyId: "company-id",
        role: "employee",
      },
      logout: mockLogout,
    });

    render(<Navbar appName="CustomApp" />);

    // h1タグ内のテキストを確認（テキストで検索）
    const heading = screen.getByText("CustomApp");
    expect(heading).toBeInTheDocument();
    expect(heading.tagName).toBe("H1");
  });

  it("正常系：ユーザーのイニシャルが表示される", async () => {
    mockUseAuth.mockReturnValue({
      user: {
        id: "user-id",
        email: "test@example.com",
        familyName: "山田",
        firstName: "太郎",
        companyId: "company-id",
        role: "employee",
      },
      logout: mockLogout,
    });

    render(<Navbar />);

    // アバターのフォールバックにイニシャルが表示される
    const avatarFallback = await screen.findByText("山");
    expect(avatarFallback).toBeInTheDocument();
  });
});
