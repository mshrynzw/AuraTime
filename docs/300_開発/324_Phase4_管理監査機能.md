# Phase 4: 管理・監査機能

## 1. 目的

- 監査ログ参照機能を実装し、データ変更履歴を確認できるようにする
- ジョブ管理機能を実装し、バッチ処理の状況を確認できるようにする
- システム管理機能を実装し、System Adminがシステム全体を管理できるようにする

## 2. 実装内容

### 2.1 監査ログ参照

#### バックエンド

- [ ] 監査ログ参照API
  - `GET /api/v1/audit-logs`
  - フィルタリング（`target_type`, `action`, `actor_user_id`）
  - 検索機能
  - ページング対応

#### フロントエンド

- [ ] 監査ログ一覧画面（Admin）
- [ ] 監査ログ詳細画面
- [ ] フィルタリング・検索機能

### 2.2 ジョブ管理

#### バックエンド

- [ ] ジョブ実行状況取得API
  - `GET /api/v1/job-runs`
  - `GET /api/v1/job-runs/{id}`
  - ジョブ実行状況の取得
- [ ] ジョブ再実行API
  - `POST /api/v1/job-runs/{id}/retry`
  - 失敗したジョブの再実行

#### フロントエンド

- [ ] ジョブ実行状況一覧画面（Admin）
- [ ] ジョブ詳細画面
- [ ] ジョブ再実行画面

### 2.3 システム管理（System Admin）

#### バックエンド

- [ ] 会社設定管理API
  - `GET /api/v1/companies`
  - `POST /api/v1/companies`
  - `PATCH /api/v1/companies/{id}`
- [ ] 契約・プラン管理API
  - `GET /api/v1/contracts`
  - `POST /api/v1/contracts`
  - `PATCH /api/v1/contracts/{id}`
- [ ] 全ユーザー・権限管理API
  - `GET /api/v1/users`
  - `PATCH /api/v1/users/{id}`
  - `PATCH /api/v1/company-memberships/{id}`

#### フロントエンド

- [ ] 会社設定管理画面（System Admin）
- [ ] 契約・プラン管理画面（System Admin）
- [ ] 全ユーザー・権限管理画面（System Admin）

## 3. APIエンドポイント

| メソッド | エンドポイント | 権限 | 説明 |
| :--- | :--- | :--- | :--- |
| GET | `/api/v1/audit-logs` | Admin | 監査ログ一覧取得 |
| GET | `/api/v1/job-runs` | Admin | ジョブ実行状況一覧取得 |
| GET | `/api/v1/job-runs/{id}` | Admin | ジョブ詳細取得 |
| POST | `/api/v1/job-runs/{id}/retry` | Admin | ジョブ再実行 |
| GET | `/api/v1/companies` | System Admin | 全会社一覧取得 |
| POST | `/api/v1/companies` | System Admin | 会社作成 |
| GET | `/api/v1/contracts` | System Admin | 契約一覧取得 |
| POST | `/api/v1/contracts` | System Admin | 契約作成 |
| GET | `/api/v1/users` | System Admin | 全ユーザー一覧取得 |
| PATCH | `/api/v1/users/{id}` | System Admin | ユーザー更新 |
| PATCH | `/api/v1/company-memberships/{id}` | System Admin | 権限更新 |

## 4. テスト項目

- [ ] 監査ログ参照のテスト
  - 監査ログ一覧の取得
  - フィルタリング・検索機能
  - ページング機能
- [ ] ジョブ管理のテスト
  - ジョブ実行状況の取得
  - ジョブ再実行の処理
- [ ] システム管理のテスト
  - 会社設定の管理
  - 契約・プランの管理
  - 全ユーザー・権限の管理

## 5. 完了基準

- [ ] 監査ログを参照できる
- [ ] ジョブ実行状況を確認できる
- [ ] ジョブを再実行できる
- [ ] システム管理機能が正常に動作する
- [ ] 全てのテストがパスする

## 6. 依存関係

- Phase 0が完了していること（監査ログ基盤）
- Phase 3が完了していること（ジョブ管理）
- システム管理機能は独立して実装可能

## 7. 参考ドキュメント

- [開発順序](./300_概要.md): 開発フェーズの概要
- [ログ設計](../200_詳細設計/270_ログ.md): 監査ログ、ジョブログの設計
- [バッチ設計](../200_詳細設計/240_バッチ.md): バッチ処理の設計
