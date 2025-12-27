# AuraTime Frontend

AuraTimeのフロントエンドアプリケーション（Next.js + TypeScript + Tailwind CSS）

## セットアップ

```bash
# 依存関係のインストール
pnpm install

# 開発サーバーの起動
pnpm dev
```

## 環境変数

`.env.local`ファイルを作成し、以下の環境変数を設定してください：

```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
```

## 主な機能

- 認証（ログイン、ログアウト）
- 認証状態管理
- PWA対応（Service Worker、オフライン対応）
- レスポンシブデザイン

## 技術スタック

- Next.js 16.0.10以上
- React 19.2.1以上
- TypeScript
- Tailwind CSS 4.x
- React Query
- React Hook Form + Zod




