# フロントエンドテスト

## テスト構造

```
__tests__/
├── lib/validation/        # バリデーションスキーマのテスト
└── app/                   # ページコンポーネントのテスト
```

## テスト実行

```bash
# すべてのテストを実行
pnpm test

# ウォッチモード
pnpm test:watch

# カバレッジを測定
pnpm test:coverage
```

## テスト環境

- **テストフレームワーク**: Jest
- **テストライブラリ**: React Testing Library
- **環境**: jsdom（ブラウザ環境のシミュレーション）

## テストの書き方

### コンポーネントテストの例

```typescript
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MyComponent from '@/components/MyComponent';

describe('MyComponent', () => {
  it('正常系：コンポーネントが表示される', () => {
    render(<MyComponent />);
    expect(screen.getByText('Hello')).toBeInTheDocument();
  });
});
```

### カスタムフックのテスト

```typescript
import { renderHook } from '@testing-library/react';
import { useAuth } from '@/lib/auth/context';

describe('useAuth', () => {
  it('正常系：認証状態を取得', () => {
    const { result } = renderHook(() => useAuth());
    expect(result.current.user).toBeNull();
  });
});
```



