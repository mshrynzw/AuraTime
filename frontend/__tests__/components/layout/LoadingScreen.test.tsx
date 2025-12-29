import LoadingScreen from '@/components/layout/LoadingScreen';
import { cleanup, render, screen } from '@testing-library/react';

describe('LoadingScreen', () => {
  afterEach(() => {
    cleanup();
  });

  it('正常系：デフォルトのメッセージが表示される', () => {
    render(<LoadingScreen />);

    expect(screen.getByText('読み込み中...')).toBeInTheDocument();
  });

  it('正常系：カスタムメッセージが表示される', () => {
    render(<LoadingScreen message="カスタムメッセージ" />);

    expect(screen.getByText('カスタムメッセージ')).toBeInTheDocument();
  });

  it('正常系：メッセージが空の場合は表示されない', () => {
    render(<LoadingScreen message="" />);

    expect(screen.queryByText('読み込み中...')).not.toBeInTheDocument();
  });

  it('正常系：全画面表示（デフォルト）', () => {
    const { container } = render(<LoadingScreen />);
    const loadingElement = container.firstChild as HTMLElement;

    expect(loadingElement.className).toContain('min-h-screen');
  });

  it('正常系：インライン表示（fullScreen=false）', () => {
    const { container } = render(<LoadingScreen fullScreen={false} />);
    const loadingElement = container.firstChild as HTMLElement;

    expect(loadingElement.className).toContain('p-8');
    expect(loadingElement.className).not.toContain('min-h-screen');
  });

  it('正常系：追加のクラス名が適用される', () => {
    const { container } = render(<LoadingScreen className="custom-class" />);
    const loadingElement = container.firstChild as HTMLElement;

    expect(loadingElement.className).toContain('custom-class');
  });

  it('正常系：Spinnerコンポーネントが表示される', () => {
    render(<LoadingScreen />);
    // Spinnerコンポーネントはrole="status"とaria-label="Loading"を持つ
    const spinnerElement = screen.getByRole('status', { name: 'Loading' });
    expect(spinnerElement).toBeInTheDocument();
  });
});

