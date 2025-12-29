import PasswordResetForm from '@/components/form/PasswordResetForm';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

describe('PasswordResetForm', () => {
  const mockPush = (global as any).mockPush as jest.Mock;
  const mockGet = (global as any).mockGet as jest.Mock;
  const mockAuthApiRequestPasswordReset = (global as any)
    .mockAuthApiRequestPasswordReset as jest.Mock;
  const mockAuthApiConfirmPasswordReset = (global as any)
    .mockAuthApiConfirmPasswordReset as jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();
    mockPush.mockClear();
    mockGet.mockReturnValue(null);
    mockAuthApiRequestPasswordReset.mockClear();
    mockAuthApiConfirmPasswordReset.mockClear();
  });

  afterEach(() => {
    cleanup();
  });

  describe('パスワードリセット要求（request step）', () => {
    it('正常系：パスワードリセット要求フォームが表示される', () => {
      render(<PasswordResetForm />);

      expect(screen.getByPlaceholderText('メールアドレス')).toBeInTheDocument();
      expect(
        screen.getByRole('button', { name: 'リセットメールを送信' })
      ).toBeInTheDocument();
      expect(
        screen.getByRole('button', { name: 'ログイン画面に戻る' })
      ).toBeInTheDocument();
    });

    it('正常系：パスワードリセット要求成功', async () => {
      mockAuthApiRequestPasswordReset.mockResolvedValue({
        success: true,
      });

      render(<PasswordResetForm />);

      const user = userEvent.setup();
      await user.type(screen.getByPlaceholderText('メールアドレス'), 'test@example.com');
      await user.click(screen.getByRole('button', { name: 'リセットメールを送信' }));

      await waitFor(() => {
        expect(mockAuthApiRequestPasswordReset).toHaveBeenCalledWith({
          email: 'test@example.com',
        });
      });

      await waitFor(() => {
        expect(
          screen.getByText(
            /パスワードリセットメールを送信しました。メールをご確認ください。/i
          )
        ).toBeInTheDocument();
      });
    });

    it('異常系：バリデーションエラー（メールアドレス未入力）', async () => {
      render(<PasswordResetForm />);

      const user = userEvent.setup();
      await user.click(screen.getByRole('button', { name: 'リセットメールを送信' }));

      await waitFor(() => {
        expect(screen.getByText(/メールアドレスは必須です/i)).toBeInTheDocument();
      });
    });

    it('異常系：バリデーションエラー（無効なメールアドレス形式）', async () => {
      render(<PasswordResetForm />);

      const user = userEvent.setup();
      await user.type(screen.getByPlaceholderText('メールアドレス'), 'invalid-email');
      await user.click(screen.getByRole('button', { name: 'リセットメールを送信' }));

      await waitFor(() => {
        expect(screen.getByText(/有効なメールアドレス/i)).toBeInTheDocument();
      });
    });

    it('異常系：パスワードリセット要求失敗（APIエラー）', async () => {
      mockAuthApiRequestPasswordReset.mockRejectedValue({
        response: {
          data: {
            error: {
              message: 'ユーザーが見つかりません',
            },
          },
        },
      });

      render(<PasswordResetForm />);

      const user = userEvent.setup();
      await user.type(screen.getByPlaceholderText('メールアドレス'), 'test@example.com');
      await user.click(screen.getByRole('button', { name: 'リセットメールを送信' }));

      await waitFor(() => {
        expect(screen.getByText(/ユーザーが見つかりません/i)).toBeInTheDocument();
      });
    });

    it('異常系：パスワードリセット要求失敗（レスポンスがsuccess: false）', async () => {
      mockAuthApiRequestPasswordReset.mockResolvedValue({
        success: false,
      });

      render(<PasswordResetForm />);

      const user = userEvent.setup();
      await user.type(screen.getByPlaceholderText('メールアドレス'), 'test@example.com');
      await user.click(screen.getByRole('button', { name: 'リセットメールを送信' }));

      await waitFor(() => {
        expect(
          screen.getByText(/パスワードリセット要求に失敗しました/i)
        ).toBeInTheDocument();
      });
    });

    it('正常系：ログイン画面に戻るボタンクリック', async () => {
      render(<PasswordResetForm />);

      const user = userEvent.setup();
      await user.click(screen.getByRole('button', { name: 'ログイン画面に戻る' }));

      expect(mockPush).toHaveBeenCalledWith('/login');
    });

    it('正常系：送信中の状態が表示される', async () => {
      mockAuthApiRequestPasswordReset.mockImplementation(
        () =>
          new Promise((resolve) => {
            setTimeout(() => {
              resolve({
                success: true,
              });
            }, 100);
          })
      );

      render(<PasswordResetForm />);

      const user = userEvent.setup();
      await user.type(screen.getByPlaceholderText('メールアドレス'), 'test@example.com');
      await user.click(screen.getByRole('button', { name: 'リセットメールを送信' }));

      expect(screen.getByRole('button', { name: '送信中...' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '送信中...' })).toBeDisabled();
    });
  });

  describe('パスワードリセット実行（confirm step）', () => {
    beforeEach(() => {
      mockGet.mockReturnValue('test-token');
    });

    it('正常系：パスワードリセット実行フォームが表示される（URLにトークンがある場合）', () => {
      render(<PasswordResetForm />);

      expect(screen.getByPlaceholderText('リセットトークン')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('新しいパスワード（8文字以上）')).toBeInTheDocument();
      expect(
        screen.getByRole('button', { name: 'パスワードをリセット' })
      ).toBeInTheDocument();
    });

    it('正常系：パスワードリセット実行成功', async () => {
      mockAuthApiConfirmPasswordReset.mockResolvedValue({
        success: true,
      });

      render(<PasswordResetForm />);

      const user = userEvent.setup();
      // defaultValuesで既にトークンが設定されているため、入力フィールドをクリアしてから入力
      const tokenInput = screen.getByPlaceholderText('リセットトークン') as HTMLInputElement;
      await user.clear(tokenInput);
      await user.type(tokenInput, 'test-token');

      await user.type(
        screen.getByPlaceholderText('新しいパスワード（8文字以上）'),
        'NewPassword123!@#'
      );

      // フォーカスを外してバリデーションを実行
      await user.tab();

      await user.click(screen.getByRole('button', { name: 'パスワードをリセット' }));

      await waitFor(() => {
        expect(mockAuthApiConfirmPasswordReset).toHaveBeenCalledWith({
          token: 'test-token',
          newPassword: 'NewPassword123!@#',
        });
      });

      await waitFor(() => {
        expect(
          screen.getByText(/パスワードをリセットしました。ログイン画面に移動します。/i)
        ).toBeInTheDocument();
      });

      await waitFor(
        () => {
          expect(mockPush).toHaveBeenCalledWith('/login');
        },
        { timeout: 3000 }
      );
    });

    it('正常系：パスワード表示/非表示の切り替え', async () => {
      render(<PasswordResetForm />);

      const passwordInput = screen.getByPlaceholderText(
        '新しいパスワード（8文字以上）'
      ) as HTMLInputElement;
      const toggleButton = screen.getByLabelText('パスワードを表示');

      expect(passwordInput.type).toBe('password');

      const user = userEvent.setup();
      await user.click(toggleButton);

      await waitFor(() => {
        expect(passwordInput.type).toBe('text');
        expect(screen.getByLabelText('パスワードを非表示')).toBeInTheDocument();
      });
    });

    it('異常系：バリデーションエラー（トークン未入力）', async () => {
      // トークンが設定されている状態でconfirmステップを表示
      mockGet.mockReturnValue('test-token');

      render(<PasswordResetForm />);

      const user = userEvent.setup();
      // トークン入力フィールドをクリア
      const tokenInput = screen.getByPlaceholderText('リセットトークン') as HTMLInputElement;
      await user.clear(tokenInput);

      await user.type(
        screen.getByPlaceholderText('新しいパスワード（8文字以上）'),
        'NewPassword123!@#'
      );

      // フォーカスを外してバリデーションを実行
      await user.tab();

      await user.click(screen.getByRole('button', { name: 'パスワードをリセット' }));

      await waitFor(() => {
        expect(screen.getByText(/トークンは必須です/i)).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('異常系：バリデーションエラー（パスワード未入力）', async () => {
      render(<PasswordResetForm />);

      const user = userEvent.setup();
      await user.type(screen.getByPlaceholderText('リセットトークン'), 'test-token');
      await user.click(screen.getByRole('button', { name: 'パスワードをリセット' }));

      await waitFor(() => {
        expect(screen.getByText(/パスワードは必須です/i)).toBeInTheDocument();
      });
    });

    it('異常系：バリデーションエラー（パスワードが12文字未満）', async () => {
      render(<PasswordResetForm />);

      const user = userEvent.setup();
      await user.type(screen.getByPlaceholderText('リセットトークン'), 'test-token');
      await user.type(
        screen.getByPlaceholderText('新しいパスワード（8文字以上）'),
        'Short1!'
      );
      await user.click(screen.getByRole('button', { name: 'パスワードをリセット' }));

      await waitFor(() => {
        expect(screen.getByText(/12文字以上/i)).toBeInTheDocument();
      });
    });

    it('異常系：バリデーションエラー（パスワードに数字・大文字・小文字・記号が含まれていない）', async () => {
      render(<PasswordResetForm />);

      const user = userEvent.setup();
      await user.type(screen.getByPlaceholderText('リセットトークン'), 'test-token');
      await user.type(
        screen.getByPlaceholderText('新しいパスワード（8文字以上）'),
        'passwordonly'
      );
      await user.click(screen.getByRole('button', { name: 'パスワードをリセット' }));

      await waitFor(() => {
        expect(
          screen.getByText(/数字・大文字・小文字・記号を含む/i)
        ).toBeInTheDocument();
      });
    });

    it('異常系：パスワードリセット実行失敗（APIエラー）', async () => {
      mockAuthApiConfirmPasswordReset.mockRejectedValue({
        response: {
          data: {
            error: {
              message: 'トークンが無効または期限切れです',
            },
          },
        },
      });

      render(<PasswordResetForm />);

      const user = userEvent.setup();
      await user.type(screen.getByPlaceholderText('リセットトークン'), 'invalid-token');
      await user.type(
        screen.getByPlaceholderText('新しいパスワード（8文字以上）'),
        'NewPassword123!@#'
      );
      await user.click(screen.getByRole('button', { name: 'パスワードをリセット' }));

      await waitFor(() => {
        expect(screen.getByText(/トークンが無効または期限切れです/i)).toBeInTheDocument();
      });
    });

    it('異常系：パスワードリセット実行失敗（レスポンスがsuccess: false）', async () => {
      mockAuthApiConfirmPasswordReset.mockResolvedValue({
        success: false,
      });

      render(<PasswordResetForm />);

      const user = userEvent.setup();
      await user.type(screen.getByPlaceholderText('リセットトークン'), 'test-token');
      await user.type(
        screen.getByPlaceholderText('新しいパスワード（8文字以上）'),
        'NewPassword123!@#'
      );
      await user.click(screen.getByRole('button', { name: 'パスワードをリセット' }));

      await waitFor(() => {
        expect(screen.getByText(/パスワードリセットに失敗しました/i)).toBeInTheDocument();
      });
    });

    it('正常系：ログイン画面に戻るボタンクリック', async () => {
      render(<PasswordResetForm />);

      const user = userEvent.setup();
      await user.click(screen.getByRole('button', { name: 'ログイン画面に戻る' }));

      expect(mockPush).toHaveBeenCalledWith('/login');
    });

    it('正常系：送信中の状態が表示される', async () => {
      mockAuthApiConfirmPasswordReset.mockImplementation(
        () =>
          new Promise((resolve) => {
            setTimeout(() => {
              resolve({
                success: true,
              });
            }, 100);
          })
      );

      render(<PasswordResetForm />);

      const user = userEvent.setup();
      await user.type(screen.getByPlaceholderText('リセットトークン'), 'test-token');
      await user.type(
        screen.getByPlaceholderText('新しいパスワード（8文字以上）'),
        'NewPassword123!@#'
      );
      await user.click(screen.getByRole('button', { name: 'パスワードをリセット' }));

      expect(screen.getByRole('button', { name: 'リセット中...' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'リセット中...' })).toBeDisabled();
    });
  });
});

