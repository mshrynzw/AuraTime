import NavigationLoader from '@/components/layout/NavigationLoader';
import { cleanup, render, screen } from '@testing-library/react';
import { usePathname } from 'next/navigation';

// usePathnameをモック
const mockUsePathname = jest.fn();
jest.mock('next/navigation', () => ({
  ...jest.requireActual('next/navigation'),
  usePathname: jest.fn(),
}));

// useNavigationをモック
const mockUseNavigation = jest.fn();
jest.mock('@/hooks/use-navigation', () => ({
  useNavigation: () => mockUseNavigation(),
}));

const mockUsePathnameFn = usePathname as jest.Mock;

describe('NavigationLoader', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUsePathnameFn.mockReturnValue('/');
  });

  afterEach(() => {
    cleanup();
  });

  it('正常系：遷移中でない場合は何も表示されない', () => {
    mockUseNavigation.mockReturnValue(false);

    const { container } = render(<NavigationLoader />);
    expect(container.firstChild).toBeNull();
  });

  it('正常系：遷移中はローディング画面が表示される', () => {
    mockUseNavigation.mockReturnValue(true);

    render(<NavigationLoader />);

    expect(screen.getByText(/読み込み中.../i)).toBeInTheDocument();
  });

  it('正常系：遷移状態が変更されると表示が切り替わる', () => {
    const { rerender } = render(<NavigationLoader />);

    // 最初は遷移中でない
    mockUseNavigation.mockReturnValue(false);
    rerender(<NavigationLoader />);
    expect(screen.queryByText(/読み込み中.../i)).not.toBeInTheDocument();

    // 遷移中になる
    mockUseNavigation.mockReturnValue(true);
    rerender(<NavigationLoader />);
    expect(screen.getByText(/読み込み中.../i)).toBeInTheDocument();
  });
});

