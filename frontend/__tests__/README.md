# フロントエンドテスト

## テスト構造

```text
__tests__/
├── lib/
│   ├── api/               # APIクライアントのテスト
│   ├── auth/              # 認証関連のテスト
│   └── validation/        # バリデーションスキーマのテスト
├── app/                   # ページコンポーネントのテスト
└── components/             # コンポーネントのテスト
```

## テスト実行

```bash
# すべてのテストを実行
pnpm test

# ウォッチモード
pnpm test:watch

# カバレッジを測定
pnpm test:coverage

# テスト結果をJSON形式で出力
pnpm test:output
# → test-results.json に結果が出力されます

# テスト結果をテキスト形式で出力
pnpm test:output:txt
# → test-results.txt に結果が出力されます

# テスト結果を両方の形式で出力
pnpm test:output:both
# → test-results.json と test-results.txt の両方に出力されます
```

## 出力ファイルの場所

- frontend/test-results.json - JSON形式のテスト結果
- frontend/test-results.txt - テキスト形式のテスト結果

これらのファイルは .gitignore に追加されているため、Gitにはコミットされません。
JSON形式の方がプログラムで解析しやすく、テキスト形式は人間が読みやすいです。用途に応じて選択してください。

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
import { renderHook, waitFor } from "@testing-library/react";
import { AuthProvider, useAuth } from "@/lib/auth/context";

describe("useAuth", () => {
  it("正常系：認証状態を取得", async () => {
    const { result } = renderHook(() => useAuth(), {
      wrapper: AuthProvider,
    });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.user).toBeNull();
  });
});
```

## テスト結果の出力

テスト結果は以下のファイルに出力されます：

- `test-results.json` - JSON形式のテスト結果（プログラムで解析可能）
- `test-results.txt` - テキスト形式のテスト結果（人間が読みやすい）

これらのファイルは `.gitignore` に含まれているため、Gitにはコミットされません。

## 注意事項

- テスト実行中にメモリ不足が発生する場合は、`jest.config.js` の `maxWorkers` を調整してください
- 非同期処理を含むテストでは、`waitFor` を使用して適切に待機してください
- React 19では `useEffect` が即座に実行される可能性があるため、テストでは状態の変化を待つ必要があります
