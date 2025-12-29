import LoginForm from "@/components/form/LoginForm";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

describe("LoginForm", () => {
  const mockPush = (global as any).mockPush as jest.Mock;
  const mockReplace = (global as any).mockReplace as jest.Mock;
  const mockGet = (global as any).mockGet as jest.Mock;
  const mockLogin = (global as any).mockLogin as jest.Mock;
  const mockAuthApiLogin = (global as any).mockAuthApiLogin as jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();
    mockPush.mockClear();
    mockReplace.mockClear();
    mockGet.mockReturnValue(null);
    mockLogin.mockClear();
    mockAuthApiLogin.mockClear();
  });

  afterEach(() => {
    cleanup();
  });

  it("正常系：ログインフォームが表示される", () => {
    render(<LoginForm />);

    expect(screen.getByPlaceholderText("メールアドレス")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("パスワード")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "ログイン" })
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "パスワードを忘れた場合" })
    ).toBeInTheDocument();
  });

  it("正常系：ログイン成功", async () => {
    mockAuthApiLogin.mockResolvedValue({
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
    });

    render(<LoginForm />);

    const user = userEvent.setup();
    await user.type(
      screen.getByPlaceholderText("メールアドレス"),
      "test@example.com"
    );
    await user.type(
      screen.getByPlaceholderText("パスワード"),
      "Password123!@#"
    );
    await user.click(screen.getByRole("button", { name: "ログイン" }));

    await waitFor(() => {
      expect(mockAuthApiLogin).toHaveBeenCalledWith({
        email: "test@example.com",
        password: "Password123!@#",
      });
    });

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith("test-token");
      expect(mockPush).toHaveBeenCalledWith("/dashboard");
    });
  });

  it("正常系：ユーザー登録完了後のメッセージが表示される", () => {
    mockGet.mockReturnValue("true");

    render(<LoginForm />);

    expect(
      screen.getByText("ユーザー登録が完了しました。ログインしてください。")
    ).toBeInTheDocument();
    expect(mockReplace).toHaveBeenCalledWith("/login", { scroll: false });
  });

  it("正常系：パスワード表示/非表示の切り替え", async () => {
    render(<LoginForm />);

    const passwordInput = screen.getByPlaceholderText(
      "パスワード"
    ) as HTMLInputElement;
    const toggleButton = screen.getByLabelText("パスワードを表示");

    expect(passwordInput.type).toBe("password");

    const user = userEvent.setup();
    await user.click(toggleButton);

    await waitFor(() => {
      expect(passwordInput.type).toBe("text");
      expect(screen.getByLabelText("パスワードを非表示")).toBeInTheDocument();
    });
  });

  it("正常系：パスワードを忘れた場合のリンククリック", async () => {
    render(<LoginForm />);

    const user = userEvent.setup();
    await user.click(
      screen.getByRole("button", { name: "パスワードを忘れた場合" })
    );

    expect(mockPush).toHaveBeenCalledWith("/password-reset");
  });

  it("異常系：バリデーションエラー（メールアドレス未入力）", async () => {
    render(<LoginForm />);

    const user = userEvent.setup();
    await user.type(
      screen.getByPlaceholderText("パスワード"),
      "Password123!@#"
    );
    await user.click(screen.getByRole("button", { name: "ログイン" }));

    await waitFor(() => {
      expect(screen.getByText(/メールアドレスは必須です/i)).toBeInTheDocument();
    });
  });

  it("異常系：バリデーションエラー（パスワード未入力）", async () => {
    render(<LoginForm />);

    const user = userEvent.setup();
    await user.type(
      screen.getByPlaceholderText("メールアドレス"),
      "test@example.com"
    );
    await user.click(screen.getByRole("button", { name: "ログイン" }));

    await waitFor(() => {
      expect(screen.getByText(/パスワードは必須です/i)).toBeInTheDocument();
    });
  });

  it("異常系：バリデーションエラー（無効なメールアドレス形式）", async () => {
    render(<LoginForm />);

    const user = userEvent.setup();
    await user.type(
      screen.getByPlaceholderText("メールアドレス"),
      "invalid-email"
    );
    await user.type(
      screen.getByPlaceholderText("パスワード"),
      "Password123!@#"
    );
    await user.click(screen.getByRole("button", { name: "ログイン" }));

    await waitFor(() => {
      expect(screen.getByText(/有効なメールアドレス/i)).toBeInTheDocument();
    });
  });

  it("異常系：バリデーションエラー（パスワードが12文字未満）", async () => {
    render(<LoginForm />);

    const user = userEvent.setup();
    await user.type(
      screen.getByPlaceholderText("メールアドレス"),
      "test@example.com"
    );
    await user.type(screen.getByPlaceholderText("パスワード"), "Short1!");
    await user.click(screen.getByRole("button", { name: "ログイン" }));

    await waitFor(() => {
      expect(screen.getByText(/12文字以上/i)).toBeInTheDocument();
    });
  });

  it("異常系：バリデーションエラー（パスワードに数字・大文字・小文字・記号が含まれていない）", async () => {
    render(<LoginForm />);

    const user = userEvent.setup();
    await user.type(
      screen.getByPlaceholderText("メールアドレス"),
      "test@example.com"
    );
    await user.type(screen.getByPlaceholderText("パスワード"), "passwordonly");
    await user.click(screen.getByRole("button", { name: "ログイン" }));

    await waitFor(() => {
      expect(
        screen.getByText(/数字・大文字・小文字・記号を含む/i)
      ).toBeInTheDocument();
    });
  });

  it("異常系：ログイン失敗（APIエラー）", async () => {
    mockAuthApiLogin.mockRejectedValue({
      response: {
        data: {
          error: {
            message: "メールアドレスまたはパスワードが正しくありません",
          },
        },
      },
    });

    render(<LoginForm />);

    const user = userEvent.setup();
    await user.type(
      screen.getByPlaceholderText("メールアドレス"),
      "test@example.com"
    );
    await user.type(
      screen.getByPlaceholderText("パスワード"),
      "Password123!@#"
    );
    await user.click(screen.getByRole("button", { name: "ログイン" }));

    await waitFor(() => {
      expect(
        screen.getByText(/メールアドレスまたはパスワードが正しくありません/i)
      ).toBeInTheDocument();
    });
  });

  it("異常系：ログイン失敗（レスポンスがsuccess: false）", async () => {
    mockAuthApiLogin.mockResolvedValue({
      success: false,
    });

    render(<LoginForm />);

    const user = userEvent.setup();
    await user.type(
      screen.getByPlaceholderText("メールアドレス"),
      "test@example.com"
    );
    await user.type(
      screen.getByPlaceholderText("パスワード"),
      "Password123!@#"
    );
    await user.click(screen.getByRole("button", { name: "ログイン" }));

    await waitFor(() => {
      expect(screen.getByText(/ログインに失敗しました/i)).toBeInTheDocument();
    });
  });

  it("異常系：ログイン失敗（レスポンスにdataがない）", async () => {
    mockAuthApiLogin.mockResolvedValue({
      success: true,
      data: null,
    });

    render(<LoginForm />);

    const user = userEvent.setup();
    await user.type(
      screen.getByPlaceholderText("メールアドレス"),
      "test@example.com"
    );
    await user.type(
      screen.getByPlaceholderText("パスワード"),
      "Password123!@#"
    );
    await user.click(screen.getByRole("button", { name: "ログイン" }));

    await waitFor(() => {
      expect(screen.getByText(/ログインに失敗しました/i)).toBeInTheDocument();
    });
  });

  it("正常系：送信中の状態が表示される", async () => {
    mockAuthApiLogin.mockImplementation(
      () =>
        new Promise((resolve) => {
          setTimeout(() => {
            resolve({
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
            });
          }, 100);
        })
    );

    render(<LoginForm />);

    const user = userEvent.setup();
    await user.type(
      screen.getByPlaceholderText("メールアドレス"),
      "test@example.com"
    );
    await user.type(
      screen.getByPlaceholderText("パスワード"),
      "Password123!@#"
    );
    await user.click(screen.getByRole("button", { name: "ログイン" }));

    expect(
      screen.getByRole("button", { name: "ログイン中..." })
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "ログイン中..." })
    ).toBeDisabled();
  });
});
