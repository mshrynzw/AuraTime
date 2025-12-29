import DashboardContent from '@/app/(protected)/dashboard/DashboardContent';
import { cleanup, render, screen } from '@testing-library/react';

// useAuthをモック
const mockUseAuth = jest.fn();
jest.mock('@/lib/auth/context', () => ({
  useAuth: () => mockUseAuth(),
}));

describe('DashboardContent', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('正常系：認証済みユーザーの情報が表示される', () => {
    mockUseAuth.mockReturnValue({
      user: {
        id: 'user-id',
        email: 'test@example.com',
        familyName: '山田',
        firstName: '太郎',
        companyId: 'company-id',
        role: 'employee',
      },
    });

    render(<DashboardContent />);

    expect(screen.getByText(/ようこそ、山田 太郎さん/i)).toBeInTheDocument();
    expect(screen.getByText(/ロール: employee/i)).toBeInTheDocument();
  });

  it('正常系：未認証ユーザーの場合は何も表示されない', () => {
    mockUseAuth.mockReturnValue({
      user: null,
    });

    const { container } = render(<DashboardContent />);
    expect(container.firstChild).toBeNull();
  });

  it('正常系：異なるロールが表示される', () => {
    mockUseAuth.mockReturnValue({
      user: {
        id: 'user-id',
        email: 'admin@example.com',
        familyName: '佐藤',
        firstName: '花子',
        companyId: 'company-id',
        role: 'admin',
      },
    });

    render(<DashboardContent />);

    expect(screen.getByText(/ようこそ、佐藤 花子さん/i)).toBeInTheDocument();
    expect(screen.getByText(/ロール: admin/i)).toBeInTheDocument();
  });
});

