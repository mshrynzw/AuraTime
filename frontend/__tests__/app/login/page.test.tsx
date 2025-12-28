import LoginPage from '@/app/(public)/login/page';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

// モックは jest.setup.js で設定済み

describe('LoginPage', () => {
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

  it('正常系：ログインフォームが表示される', () => {
    render(<LoginPage />);

    expect(screen.getByPlaceholderText('メールアドレス')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('パスワード')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'ログイン' })).toBeInTheDocument();
  });

  it('正常系：ログイン成功', async () => {
    mockAuthApiLogin.mockResolvedValue({
      success: true,
      data: {
        token: 'test-token',
        user: {
          id: 'user-id',
          email: 'test@example.com',
          familyName: '山田',
          firstName: '太郎',
          companyId: 'company-id',
          role: 'employee',
        },
      },
    });

    render(<LoginPage />);

    const user = userEvent.setup();
    await user.type(screen.getByPlaceholderText('メールアドレス'), 'test@example.com');
    await user.type(screen.getByPlaceholderText('パスワード'), 'password123');
    await user.click(screen.getByRole('button', { name: 'ログイン' }));

    await waitFor(() => {
      expect(mockAuthApiLogin).toHaveBeenCalledWith({
        email: 'test@example.com',
        password: 'password123',
      });
    }, { timeout: 3000 });

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('test-token');
      expect(mockPush).toHaveBeenCalledWith('/dashboard');
    }, { timeout: 3000 });
  });

  it('異常系：バリデーションエラー（メールアドレス未入力）', async () => {
    render(<LoginPage />);

    const user = userEvent.setup();
    await user.type(screen.getByPlaceholderText('パスワード'), 'password123');
    await user.click(screen.getByRole('button', { name: 'ログイン' }));

    await waitFor(() => {
      expect(screen.getByText(/メールアドレスは必須です/i)).toBeInTheDocument();
    });
  });

  it('異常系：ログイン失敗', async () => {
    mockAuthApiLogin.mockRejectedValue({
      response: {
        data: {
          error: {
            message: 'メールアドレスまたはパスワードが正しくありません',
          },
        },
      },
    });

    render(<LoginPage />);

    const user = userEvent.setup();
    await user.type(screen.getByPlaceholderText('メールアドレス'), 'test@example.com');
    await user.type(screen.getByPlaceholderText('パスワード'), 'wrongpassword');
    await user.click(screen.getByRole('button', { name: 'ログイン' }));

    await waitFor(() => {
      // エラーメッセージが表示されることを確認（APIから返されたメッセージまたはデフォルトメッセージ）
      expect(screen.getByText(/メールアドレスまたはパスワードが正しくありません/i)).toBeInTheDocument();
    });
  });
});



