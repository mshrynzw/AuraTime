import RegisterForm from "@/components/form/RegisterForm";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

describe("RegisterForm", () => {
  const mockPush = (global as any).mockPush as jest.Mock;
  const mockGet = (global as any).mockGet as jest.Mock;
  const mockAuthApiRegister = (global as any).mockAuthApiRegister as jest.Mock;
  const mockAuthApiGetInvitation = (global as any)
    .mockAuthApiGetInvitation as jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();
    mockPush.mockClear();
    mockGet.mockReturnValue(null);
    mockAuthApiRegister.mockClear();
    mockAuthApiGetInvitation.mockClear();
  });

  afterEach(() => {
    cleanup();
  });

  describe("基本機能", () => {
    describe("招待トークンなし（手動入力）", () => {
      it("正常系：登録フォームが表示される", () => {
        render(<RegisterForm />);

        expect(
          screen.getByPlaceholderText("招待トークンを入力してください")
        ).toBeInTheDocument();
        expect(
          screen.getByPlaceholderText("example@example.com")
        ).toBeInTheDocument();
        expect(
          screen.getByRole("button", { name: "登録" })
        ).toBeInTheDocument();
      });

      it("正常系：新規ユーザー登録成功", async () => {
        mockAuthApiGetInvitation.mockResolvedValue({
          success: true,
          data: {
            id: "invitation-id",
            companyId: "company-id",
            companyName: "テスト会社",
            email: "newuser@example.com",
            role: "employee",
            employeeNo: "EMP001",
            employmentType: "full_time",
            hireDate: "2025-01-01",
            expiresAt: "2026-01-01T00:00:00Z",
          },
        });

        mockAuthApiRegister.mockResolvedValue({
          success: true,
        });

        render(<RegisterForm />);

        const user = userEvent.setup();
        await user.type(
          screen.getByPlaceholderText("招待トークンを入力してください"),
          "test-token"
        );

        await waitFor(() => {
          expect(mockAuthApiGetInvitation).toHaveBeenCalledWith("test-token");
        });

        await waitFor(() => {
          expect(screen.getByPlaceholderText("パスワード")).toBeInTheDocument();
        });

        await user.type(
          screen.getByPlaceholderText("パスワード"),
          "NewPassword123!@#"
        );
        await user.type(screen.getByPlaceholderText("山田"), "山田");
        await user.type(screen.getByPlaceholderText("太郎"), "太郎");
        await user.click(screen.getByRole("button", { name: "登録" }));

        await waitFor(() => {
          expect(mockAuthApiRegister).toHaveBeenCalledWith({
            invitationToken: "test-token",
            email: "newuser@example.com",
            password: "NewPassword123!@#",
            familyName: "山田",
            firstName: "太郎",
            familyNameKana: "",
            firstNameKana: "",
          });
        });

        // 登録成功後のリダイレクトを確認（82行目をカバー）
        await waitFor(
          () => {
            expect(mockPush).toHaveBeenCalledWith("/login?registered=true");
          },
          { timeout: 1000 }
        );
      });

      it("正常系：既存ユーザー登録成功（パスワード不要）", async () => {
        mockAuthApiGetInvitation.mockResolvedValue({
          success: true,
          data: {
            id: "invitation-id",
            companyId: "company-id",
            companyName: "テスト会社",
            email: "existing@example.com",
            role: "employee",
            employeeNo: "EMP001",
            employmentType: "full_time",
            hireDate: "2025-01-01",
            expiresAt: "2026-01-01T00:00:00Z",
          },
        });

        mockAuthApiRegister.mockResolvedValue({
          success: true,
        });

        render(<RegisterForm />);

        const user = userEvent.setup();
        await user.type(
          screen.getByPlaceholderText("招待トークンを入力してください"),
          "test-token"
        );

        await waitFor(() => {
          expect(mockAuthApiGetInvitation).toHaveBeenCalledWith("test-token");
        });

        // 招待情報が取得されたことを確認
        await waitFor(() => {
          expect(
            screen.getByText(/招待情報を取得しました: テスト会社 \(employee\)/i)
          ).toBeInTheDocument();
        });

        // パスワードフィールドが表示されていることを確認（常に表示される）
        await waitFor(() => {
          expect(screen.getByPlaceholderText("パスワード")).toBeInTheDocument();
        });

        await user.click(screen.getByRole("button", { name: "登録" }));

        await waitFor(() => {
          expect(mockAuthApiRegister).toHaveBeenCalledWith({
            invitationToken: "test-token",
            email: "existing@example.com",
            password: "",
            familyName: "",
            firstName: "",
            familyNameKana: "",
            firstNameKana: "",
          });
        });

        // 登録成功後のリダイレクトを確認（82行目をカバー）
        await waitFor(
          () => {
            expect(mockPush).toHaveBeenCalledWith("/login?registered=true");
          },
          { timeout: 1000 }
        );
      });
    });

    describe("URLパラメータから招待トークンを取得", () => {
      it("正常系：URLパラメータから招待トークンを取得して招待情報を読み込む", async () => {
        mockGet.mockReturnValue("url-token");

        mockAuthApiGetInvitation.mockResolvedValue({
          success: true,
          data: {
            id: "invitation-id",
            companyId: "company-id",
            companyName: "テスト会社",
            email: "newuser@example.com",
            role: "employee",
            employeeNo: "EMP001",
            employmentType: "full_time",
            hireDate: "2025-01-01",
            expiresAt: "2026-01-01T00:00:00Z",
          },
        });

        render(<RegisterForm />);

        await waitFor(() => {
          expect(mockAuthApiGetInvitation).toHaveBeenCalledWith("url-token");
        });

        await waitFor(() => {
          expect(
            screen.getByText(/招待情報を取得しました: テスト会社 \(employee\)/i)
          ).toBeInTheDocument();
        });

        // メールアドレスが自動入力されていることを確認
        const emailInput = screen.getByPlaceholderText(
          "example@example.com"
        ) as HTMLInputElement;
        expect(emailInput.value).toBe("newuser@example.com");
        expect(emailInput.disabled).toBe(true);
      });

      it("正常系：URLパラメータから招待トークンを取得（setValueの確認）", async () => {
        mockGet.mockReturnValue("url-token-setvalue");

        mockAuthApiGetInvitation.mockResolvedValue({
          success: true,
          data: {
            id: "invitation-id",
            companyId: "company-id",
            companyName: "テスト会社",
            email: "newuser@example.com",
            role: "employee",
            employeeNo: "EMP001",
            employmentType: "full_time",
            hireDate: "2025-01-01",
            expiresAt: "2026-01-01T00:00:00Z",
          },
        });

        render(<RegisterForm />);

        // setValueが呼ばれていることを確認するため、フォームの値を確認（39-40行目をカバー）
        await waitFor(() => {
          const tokenInput = screen.getByPlaceholderText(
            "招待トークンを入力してください"
          ) as HTMLInputElement;
          expect(tokenInput.value).toBe("url-token-setvalue");
        });

        await waitFor(() => {
          expect(mockAuthApiGetInvitation).toHaveBeenCalledWith(
            "url-token-setvalue"
          );
        });
      });

      it("異常系：招待トークンが無効", async () => {
        mockGet.mockReturnValue("invalid-token");

        mockAuthApiGetInvitation.mockResolvedValue({
          success: false,
        });

        render(<RegisterForm />);

        await waitFor(() => {
          expect(
            screen.getByText(/招待トークンが無効です/i)
          ).toBeInTheDocument();
        });
      });

      it("異常系：招待情報の取得に失敗", async () => {
        mockGet.mockReturnValue("error-token");

        mockAuthApiGetInvitation.mockRejectedValue({
          response: {
            data: {
              error: {
                message: "招待情報の取得に失敗しました",
              },
            },
          },
        });

        render(<RegisterForm />);

        await waitFor(() => {
          expect(
            screen.getByText(/招待情報の取得に失敗しました/i)
          ).toBeInTheDocument();
        });
      });

      it("正常系：URLパラメータがない場合（手動入力モード）", () => {
        mockGet.mockReturnValue(null);

        render(<RegisterForm />);

        // 招待トークン入力フィールドが表示されていることを確認
        expect(
          screen.getByPlaceholderText("招待トークンを入力してください")
        ).toBeInTheDocument();
        expect(
          screen.getByPlaceholderText("example@example.com")
        ).toBeInTheDocument();

        // メールアドレスフィールドが有効であることを確認
        const emailInput = screen.getByPlaceholderText(
          "example@example.com"
        ) as HTMLInputElement;
        expect(emailInput.disabled).toBe(false);
      });
    });
  });

  describe("バリデーション", () => {
    beforeEach(() => {
      // デフォルトでは新規ユーザーとして扱う（パスワードフィールドが表示される）
      mockAuthApiGetInvitation.mockResolvedValue({
        success: true,
        data: {
          id: "invitation-id",
          companyId: "company-id",
          companyName: "テスト会社",
          email: "newuser@example.com",
          role: "employee",
          employeeNo: "EMP001",
          employmentType: "full_time",
          hireDate: "2025-01-01",
          expiresAt: "2026-01-01T00:00:00Z",
        },
      });
    });

    it("異常系：招待トークン未入力", async () => {
      render(<RegisterForm />);

      const user = userEvent.setup();
      await user.click(screen.getByRole("button", { name: "登録" }));

      await waitFor(() => {
        expect(screen.getByText(/招待トークンは必須です/i)).toBeInTheDocument();
      });
    });

    it("異常系：メールアドレス未入力（招待情報取得失敗）", async () => {
      // 招待情報が取得されない場合をテスト
      mockAuthApiGetInvitation.mockResolvedValue({
        success: false,
        error: { message: "招待トークンが無効です" },
      });

      render(<RegisterForm />);

      const user = userEvent.setup();
      await user.type(
        screen.getByPlaceholderText("招待トークンを入力してください"),
        "invalid-token"
      );

      await waitFor(() => {
        expect(screen.getByText(/招待トークンが無効です/i)).toBeInTheDocument();
      });

      // メールアドレスフィールドが有効になっていることを確認
      const emailInput = screen.getByPlaceholderText("example@example.com");
      expect(emailInput).not.toBeDisabled();

      // メールアドレスを入力せずに、パスワードと姓・名を入力して登録ボタンをクリック
      await user.type(
        screen.getByPlaceholderText("パスワード"),
        "NewPassword123!@#"
      );
      await user.type(screen.getByPlaceholderText("山田"), "山田");
      await user.type(screen.getByPlaceholderText("太郎"), "太郎");
      await user.click(screen.getByRole("button", { name: "登録" }));

      await waitFor(() => {
        expect(
          screen.getByText(/メールアドレスは必須です/i)
        ).toBeInTheDocument();
      });
    });

    it("異常系：パスワード未入力で姓・名を入力（新規ユーザーの可能性）", async () => {
      render(<RegisterForm />);

      const user = userEvent.setup();
      await user.type(
        screen.getByPlaceholderText("招待トークンを入力してください"),
        "test-token"
      );

      await waitFor(() => {
        expect(screen.getByPlaceholderText("パスワード")).toBeInTheDocument();
      });

      // パスワードを入力せずに、姓と名を入力して登録ボタンをクリック
      // 新規ユーザーとして扱う場合、パスワードは必須だが、フロントエンドでは既存ユーザーの可能性もあるため
      // バリデーションエラーは出ない（バックエンドで判定）
      await user.type(screen.getByPlaceholderText("山田"), "山田");
      await user.type(screen.getByPlaceholderText("太郎"), "太郎");
      await user.click(screen.getByRole("button", { name: "登録" }));

      // パスワードが未入力の場合、フロントエンドではバリデーションエラーを出さない
      // バックエンドで既存ユーザーかどうかを判定する
      // このテストは、パスワードが未入力の場合にバリデーションエラーが出ないことを確認する
      await waitFor(() => {
        expect(
          screen.queryByText(/パスワードは必須です/i)
        ).not.toBeInTheDocument();
      });

      // バックエンドAPIが呼ばれることを確認
      await waitFor(() => {
        expect(mockAuthApiRegister).toHaveBeenCalled();
      });
    });

    it("異常系：パスワードが12文字未満（新規ユーザー）", async () => {
      render(<RegisterForm />);

      const user = userEvent.setup();
      await user.type(
        screen.getByPlaceholderText("招待トークンを入力してください"),
        "test-token"
      );

      await waitFor(() => {
        expect(screen.getByPlaceholderText("パスワード")).toBeInTheDocument();
      });

      await user.type(screen.getByPlaceholderText("パスワード"), "Short1!");
      await user.click(screen.getByRole("button", { name: "登録" }));

      await waitFor(() => {
        expect(screen.getByText(/12文字以上/i)).toBeInTheDocument();
      });
    });

    it("異常系：パスワードに数字・大文字・小文字・記号が含まれていない（新規ユーザー）", async () => {
      render(<RegisterForm />);

      const user = userEvent.setup();
      await user.type(
        screen.getByPlaceholderText("招待トークンを入力してください"),
        "test-token"
      );

      await waitFor(() => {
        expect(screen.getByPlaceholderText("パスワード")).toBeInTheDocument();
      });

      await user.type(
        screen.getByPlaceholderText("パスワード"),
        "passwordonly"
      );
      await user.click(screen.getByRole("button", { name: "登録" }));

      await waitFor(() => {
        expect(
          screen.getByText(/数字・大文字・小文字・記号を含む/i)
        ).toBeInTheDocument();
      });
    });

    it("異常系：姓未入力（新規ユーザー）", async () => {
      render(<RegisterForm />);

      const user = userEvent.setup();
      await user.type(
        screen.getByPlaceholderText("招待トークンを入力してください"),
        "test-token"
      );

      await waitFor(() => {
        expect(screen.getByPlaceholderText("パスワード")).toBeInTheDocument();
      });

      await user.type(
        screen.getByPlaceholderText("パスワード"),
        "NewPassword123!@#"
      );
      await user.type(screen.getByPlaceholderText("太郎"), "太郎");
      await user.click(screen.getByRole("button", { name: "登録" }));

      await waitFor(() => {
        expect(screen.getByText(/姓は必須です/i)).toBeInTheDocument();
      });
    });

    it("異常系：名未入力（新規ユーザー）", async () => {
      render(<RegisterForm />);

      const user = userEvent.setup();
      await user.type(
        screen.getByPlaceholderText("招待トークンを入力してください"),
        "test-token"
      );

      await waitFor(() => {
        expect(screen.getByPlaceholderText("パスワード")).toBeInTheDocument();
      });

      await user.type(
        screen.getByPlaceholderText("パスワード"),
        "NewPassword123!@#"
      );
      await user.type(screen.getByPlaceholderText("山田"), "山田");
      await user.click(screen.getByRole("button", { name: "登録" }));

      await waitFor(() => {
        expect(screen.getByText(/名は必須です/i)).toBeInTheDocument();
      });
    });
  });

  describe("エラーハンドリング", () => {
    beforeEach(() => {
      mockAuthApiGetInvitation.mockResolvedValue({
        success: true,
        data: {
          id: "invitation-id",
          companyId: "company-id",
          companyName: "テスト会社",
          email: "newuser@example.com",
          role: "employee",
          employeeNo: "EMP001",
          employmentType: "full_time",
          hireDate: "2025-01-01",
          expiresAt: "2026-01-01T00:00:00Z",
        },
      });
    });

    it("異常系：登録失敗（APIエラー）", async () => {
      mockAuthApiRegister.mockRejectedValue({
        response: {
          data: {
            error: {
              message: "ライセンス数の上限に達しています",
            },
          },
        },
      });

      render(<RegisterForm />);

      const user = userEvent.setup();
      await user.type(
        screen.getByPlaceholderText("招待トークンを入力してください"),
        "test-token"
      );

      await waitFor(() => {
        expect(screen.getByPlaceholderText("パスワード")).toBeInTheDocument();
      });

      await user.type(
        screen.getByPlaceholderText("パスワード"),
        "NewPassword123!@#"
      );
      await user.type(screen.getByPlaceholderText("山田"), "山田");
      await user.type(screen.getByPlaceholderText("太郎"), "太郎");
      await user.click(screen.getByRole("button", { name: "登録" }));

      await waitFor(() => {
        expect(
          screen.getByText(/ライセンス数の上限に達しています/i)
        ).toBeInTheDocument();
      });
    });

    it("異常系：登録失敗（レスポンスがsuccess: false）", async () => {
      mockAuthApiRegister.mockResolvedValue({
        success: false,
      });

      render(<RegisterForm />);

      const user = userEvent.setup();
      await user.type(
        screen.getByPlaceholderText("招待トークンを入力してください"),
        "test-token"
      );

      await waitFor(() => {
        expect(screen.getByPlaceholderText("パスワード")).toBeInTheDocument();
      });

      await user.type(
        screen.getByPlaceholderText("パスワード"),
        "NewPassword123!@#"
      );
      await user.type(screen.getByPlaceholderText("山田"), "山田");
      await user.type(screen.getByPlaceholderText("太郎"), "太郎");
      await user.click(screen.getByRole("button", { name: "登録" }));

      await waitFor(() => {
        expect(
          screen.getByText(/ユーザー登録に失敗しました/i)
        ).toBeInTheDocument();
      });
    });

    it("異常系：招待情報取得時のネットワークエラー（responseなし）", async () => {
      mockGet.mockReturnValue("network-error-token");

      // responseプロパティがないエラーをシミュレート（60-62行目のフォールバック部分をカバー）
      mockAuthApiGetInvitation.mockRejectedValue(new Error("Network error"));

      render(<RegisterForm />);

      await waitFor(() => {
        expect(
          screen.getByText(/招待情報の取得に失敗しました/i)
        ).toBeInTheDocument();
      });
    });

    it("異常系：招待情報取得時のエラー（response.data.error.messageなし）", async () => {
      mockGet.mockReturnValue("error-token");

      // response.data.error.messageがないエラーをシミュレート
      mockAuthApiGetInvitation.mockRejectedValue({
        response: {
          data: {
            error: {},
          },
        },
      });

      render(<RegisterForm />);

      await waitFor(() => {
        expect(
          screen.getByText(/招待情報の取得に失敗しました/i)
        ).toBeInTheDocument();
      });
    });
  });

  describe("その他", () => {
    it("正常系：ログイン画面に戻るボタンクリック", async () => {
      render(<RegisterForm />);

      const user = userEvent.setup();
      const loginButton = screen.getByRole("button", {
        name: "既にアカウントをお持ちの方はこちら",
      });

      await user.click(loginButton);

      // 301行目をカバー：ログイン画面へのリダイレクト
      expect(mockPush).toHaveBeenCalledTimes(1);
      expect(mockPush).toHaveBeenCalledWith("/login");
    });

    it("正常系：送信中の状態が表示される", async () => {
      mockAuthApiGetInvitation.mockResolvedValue({
        success: true,
        data: {
          id: "invitation-id",
          companyId: "company-id",
          companyName: "テスト会社",
          email: "newuser@example.com",
          role: "employee",
          employeeNo: "EMP001",
          employmentType: "full_time",
          hireDate: "2025-01-01",
          expiresAt: "2026-01-01T00:00:00Z",
        },
      });

      // 少し遅延を入れて、isSubmittingの状態を確認できるようにする
      mockAuthApiRegister.mockImplementation(
        () =>
          new Promise((resolve) => {
            setTimeout(() => {
              resolve({ success: true });
            }, 100);
          })
      );

      render(<RegisterForm />);

      const user = userEvent.setup();
      await user.type(
        screen.getByPlaceholderText("招待トークンを入力してください"),
        "test-token"
      );

      await waitFor(() => {
        expect(screen.getByPlaceholderText("パスワード")).toBeInTheDocument();
      });

      await user.type(
        screen.getByPlaceholderText("パスワード"),
        "NewPassword123!@#"
      );
      await user.type(screen.getByPlaceholderText("山田"), "山田");
      await user.type(screen.getByPlaceholderText("太郎"), "太郎");
      await user.click(screen.getByRole("button", { name: "登録" }));

      // waitForを使って「登録中...」の表示を待つ
      await waitFor(() => {
        expect(
          screen.getByRole("button", { name: "登録中..." })
        ).toBeInTheDocument();
      });

      expect(screen.getByRole("button", { name: "登録中..." })).toBeDisabled();
    });

    it("正常系：招待情報読み込み中の表示", async () => {
      mockGet.mockReturnValue("loading-token");

      mockAuthApiGetInvitation.mockResolvedValue({
        success: true,
        data: {
          id: "invitation-id",
          companyId: "company-id",
          companyName: "テスト会社",
          email: "newuser@example.com",
          role: "employee",
          employeeNo: "EMP001",
          employmentType: "full_time",
          hireDate: "2025-01-01",
          expiresAt: "2026-01-01T00:00:00Z",
        },
      });

      render(<RegisterForm />);

      // 非同期処理の完了を待ってから、読み込み中の表示を確認
      await waitFor(() => {
        expect(
          screen.getByText(/招待情報を読み込み中.../i)
        ).toBeInTheDocument();
      });

      // loadInvitationInfoが呼ばれることを確認
      await waitFor(() => {
        expect(mockAuthApiGetInvitation).toHaveBeenCalledWith("loading-token");
      });
    });
  });
});


