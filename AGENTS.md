# AuraTime エージェントタスク

本ドキュメントは、Cursorエージェントが自動実行するタスクを定義します。
各フェーズの実装タスクを段階的に実行することで、AuraTimeシステムを構築します。

## 使用方法

1. エージェントモードで本ドキュメントを参照
2. 実行したいフェーズのタスクを選択
3. エージェントが自動的に実装を進める

## Phase 0: 基盤構築（必須・最優先）

### タスク1: データベースセットアップ

```markdown
1. マイグレーションファイルの確認
   - `database/migrations/20260101_init.sql`が存在することを確認
   - UUID v7生成関数が定義されていることを確認

2. データベース接続設定
   - Spring Boot: `application.yml`にデータベース接続情報を設定
   - 環境変数から読み込む形式で実装

3. 初期データ投入
   - `system-bot`ユーザーの作成
   - テスト用会社データの投入
```

### タスク2: 認証・認可基盤の実装（最優先）

```markdown
**バックエンド（Spring Boot）**

1. ユーザー登録APIの実装
   - エンドポイント: `POST /api/v1/auth/register`
   - パスワードハッシュ化（bcrypt）
   - バリデーション（メールアドレス、パスワード強度）
   - レスポンス: 統一されたエラーレスポンス形式

2. ログインAPIの実装
   - エンドポイント: `POST /api/v1/auth/login`
   - JWT発行（user_id, company_id, roleを含む）
   - トークン有効期限の設定（24時間）
   - レスポンス: JWTトークンとユーザー情報

3. JWT検証フィルターの実装
   - Spring Securityのフィルターとして実装
   - トークンの検証
   - `company_id`の取得と設定
   - ロールの取得と設定

4. 認可チェックの実装
   - `@PreAuthorize`アノテーションの設定
   - ロールベースアクセス制御（RBAC）
   - 4つのロール: system_admin, admin, manager, employee

5. マルチテナント分離の実装
   - グローバルフィルターで`company_id`を自動設定
   - リポジトリ層で`company_id`を自動フィルタリング
   - 他社データへのアクセスを防止（403 Forbidden）

**フロントエンド（Next.js）**

1. ログイン画面の実装
   - メールアドレス・パスワード入力フォーム
   - バリデーション（Zod）
   - エラーハンドリング
   - ローディング状態の表示

2. 認証状態管理の実装
   - JWTの保存（httpOnly cookie推奨）
   - 認証状態の確認（Context API）
   - ログアウト機能

3. 認証ガードの実装
   - ミドルウェアで未認証ユーザーをリダイレクト
   - 認証済みユーザーのみアクセス可能なページ保護

4. APIクライアントの実装
   - 認証トークンを自動的に付与
   - エラーハンドリング
   - リトライロジック
```

### タスク3: 監査ログ基盤の実装

```markdown
1. 監査ログの自動記録
   - Spring AOPを使用してデータ変更を自動記録
   - `created_by`, `updated_by`の自動設定
   - `audit_logs`テーブルへの記録

2. 監査ログAPIの実装（参照用）
   - エンドポイント: `GET /api/v1/audit-logs`
   - フィルタリング（target_type, action, actor_user_id）
   - ページング対応
```

### テスト実装

```markdown
1. ユーザー登録のテスト
   - 正常系：新規ユーザー登録
   - 異常系：重複メールアドレス、バリデーションエラー

2. ログインのテスト
   - 正常系：正しい認証情報でログイン
   - 異常系：間違った認証情報、存在しないユーザー

3. JWT検証のテスト
   - 正常系：有効なトークンでアクセス
   - 異常系：無効なトークン、期限切れトークン

4. マルチテナント分離のテスト
   - 正常系：自社データへのアクセス
   - 異常系：他社データへのアクセス拒否（403 Forbidden）

5. 認可のテスト
   - 正常系：適切なロールでアクセス
   - 異常系：不適切なロールでアクセス拒否（403 Forbidden）
```

## Phase 1: コア機能（MVP）

### タスク1: マスタ管理の実装

```markdown
**バックエンド**

1. 会社設定管理API
   - `GET /api/v1/companies/{id}`: 会社設定取得
   - `PATCH /api/v1/companies/{id}`: 会社設定更新
   - 権限: System Admin

2. グループ管理API
   - `GET /api/v1/groups`: グループ一覧取得
   - `POST /api/v1/groups`: グループ作成
   - `PATCH /api/v1/groups/{id}`: グループ更新
   - `DELETE /api/v1/groups/{id}`: グループ削除（ソフト削除）
   - 権限: Admin

3. 従業員マスタAPI
   - `GET /api/v1/employees`: 従業員一覧取得
   - `POST /api/v1/employees`: 従業員作成
   - `PATCH /api/v1/employees/{id}`: 従業員更新
   - 従業員とユーザーの紐付け

4. シフトテンプレート管理API
   - `GET /api/v1/shift-templates`: シフトテンプレート一覧
   - `POST /api/v1/shift-templates`: シフトテンプレート作成
   - `PATCH /api/v1/shift-templates/{id}`: シフトテンプレート更新

**フロントエンド**

1. 会社設定画面（System Admin）
2. グループ管理画面（Admin）
3. 従業員マスタ画面（Admin）
4. シフトテンプレート管理画面（Admin）
```

### タスク2: 打刻機能の実装

```markdown
**バックエンド**

1. 打刻APIの実装
   - エンドポイント: `POST /api/v1/time-clock/events`
   - パラメータ: `type` (in|out|break_in|break_out), `source` (web|mobile|ic_card|admin), `happened_at`, `note`
   - `company_id`, `employee_id`は自動設定
   - バリデーション: 未来時刻不可

2. 打刻履歴取得APIの実装
   - エンドポイント: `GET /api/v1/time-clock/events`
   - クエリパラメータ: `start_date`, `end_date`, `type`
   - ページング対応

**フロントエンド**

1. 打刻画面の実装
   - 現在時刻の表示
   - 出勤・退勤・休憩入り・休憩戻りボタン
   - 打刻履歴の表示（直近10件）

2. 打刻履歴一覧画面の実装
   - カレンダー表示
   - フィルタリング機能
```

### タスク3: 日次集計の実装

```markdown
**バックエンド**

1. 日次集計ロジックの実装
   - 打刻イベントから`TIME_RECORDS`を自動生成/更新
   - 労働時間の計算（法定内、残業、深夜、休憩）
   - シフトテンプレートとの照合
   - バッチ処理またはイベント駆動で実装

2. 日次集計APIの実装
   - エンドポイント: `GET /api/v1/time-records`
   - クエリパラメータ: `start_date`, `end_date`
   - ページング対応

**フロントエンド**

1. 日次勤怠一覧画面の実装
   - カレンダー表示
   - 日別の労働時間表示
   - 残業時間の表示
```

## Phase 2: 高度な機能

### タスク1: 勤怠管理（修正申請・承認）の実装

```markdown
**バックエンド**

1. 勤怠修正申請APIの実装
   - エンドポイント: `PATCH /api/v1/time-records/{id}`
   - ステータス管理（draft, submitted, approved, rejected）
   - 承認済み・締め済みの場合は409 Conflict

2. 承認APIの実装
   - エンドポイント: `POST /api/v1/approval-requests/{id}/approve`
   - エンドポイント: `POST /api/v1/approval-requests/{id}/reject`
   - 承認ルートの管理

**フロントエンド**

1. 勤怠修正申請画面の実装
2. 承認待ち一覧画面の実装（Manager）
3. 承認画面の実装
```

### タスク2: 休暇管理の実装

```markdown
**バックエンド**

1. 休暇申請APIの実装
   - エンドポイント: `POST /api/v1/leave-requests`
   - エンドポイント: `GET /api/v1/leave-requests`
   - エンドポイント: `PATCH /api/v1/leave-requests/{id}`
   - 残数管理

2. 休暇タイプ管理APIの実装
   - エンドポイント: `GET /api/v1/leave-types`
   - エンドポイント: `POST /api/v1/leave-types`

**フロントエンド**

1. 休暇申請画面の実装
2. 休暇履歴画面の実装
3. 残数確認画面の実装
```

### タスク3: シフト管理の実装

```markdown
**バックエンド**

1. シフト割当APIの実装
   - エンドポイント: `GET /api/v1/shift-assignments`
   - エンドポイント: `PUT /api/v1/shift-assignments/bulk`
   - 個人別・日別シフト割当

2. シフト一覧APIの実装
   - エンドポイント: `GET /api/v1/shifts`
   - カレンダー表示用

**フロントエンド**

1. シフト作成・割当画面の実装（Manager）
2. シフト一覧画面の実装（カレンダー表示）
```

### タスク4: 承認フロー（汎用化）の実装

```markdown
**バックエンド**

1. 承認ルート設定APIの実装
   - エンドポイント: `GET /api/v1/approval-routes`
   - エンドポイント: `POST /api/v1/approval-routes`
   - 多段承認の管理

2. 承認ステップ管理APIの実装
   - エンドポイント: `GET /api/v1/approval-steps`
   - 承認ステップの進行状況管理

**フロントエンド**

1. 承認ルート設定画面の実装（Admin）
2. 承認ステップ表示の実装
```

## Phase 3: 給与機能

### タスク1: 給与計算基盤の実装

```markdown
**バックエンド**

1. 給与期間管理APIの実装
   - エンドポイント: `GET /api/v1/payroll-periods`
   - エンドポイント: `POST /api/v1/payroll-periods`
   - エンドポイント: `PATCH /api/v1/payroll-periods/{id}`
   - ステータス管理（open, closed, calculated, finalized）

2. 単価設定APIの実装
   - エンドポイント: `GET /api/v1/compensation-plans`
   - エンドポイント: `POST /api/v1/compensation-plans`
   - エンドポイント: `PATCH /api/v1/compensation-plans/{id}`

3. 給与計算ロジックの実装
   - 勤怠集計結果と単価設定に基づく計算
   - 支給項目・控除項目の計算
   - 社会保険料の計算（簡易版）
```

### タスク2: 給与計算・明細の実装

```markdown
**バックエンド**

1. 給与計算ジョブの実装
   - エンドポイント: `POST /api/v1/payroll-periods/{id}/calculate`
   - 非同期バッチ処理（Spring Batch）
   - ジョブ実行状況の管理（job_runs）

2. 明細管理APIの実装
   - エンドポイント: `GET /api/v1/payslips`
   - エンドポイント: `PATCH /api/v1/payslips/{id}`
   - ステータス管理（draft, confirmed, paid）

3. 明細確定・公開APIの実装
   - エンドポイント: `POST /api/v1/payslips/{id}/finalize`
   - 従業員への公開

**フロントエンド**

1. 給与期間管理画面の実装（Admin）
2. 給与計算実行画面の実装（Admin）
3. 明細一覧画面の実装（Admin）
4. 明細確定画面の実装（Admin）
5. 給与明細確認画面の実装（Employee）
```

### タスク3: 支払管理の実装

```markdown
**バックエンド**

1. 支払バッチ作成APIの実装
   - エンドポイント: `POST /api/v1/payments`
   - エンドポイント: `GET /api/v1/payments`
   - FBデータ（振込データ）の生成

2. 支払ステータス管理APIの実装
   - エンドポイント: `PATCH /api/v1/payments/{id}`
   - ステータス管理（scheduled, processing, done, failed）

**フロントエンド**

1. 支払バッチ作成画面の実装（Admin）
2. 支払状況確認画面の実装（Admin）
3. FBデータ出力画面の実装（Admin）
```

## Phase 4: 管理・監査機能

### タスク1: 監査ログ参照の実装

```markdown
**バックエンド**

1. 監査ログ参照APIの実装
   - エンドポイント: `GET /api/v1/audit-logs`
   - フィルタリング（target_type, action, actor_user_id）
   - 検索機能
   - ページング対応

**フロントエンド**

1. 監査ログ一覧画面の実装（Admin）
2. 監査ログ詳細画面の実装
3. フィルタリング・検索機能の実装
```

### タスク2: ジョブ管理の実装

```markdown
**バックエンド**

1. ジョブ実行状況取得APIの実装
   - エンドポイント: `GET /api/v1/job-runs`
   - エンドポイント: `GET /api/v1/job-runs/{id}`
   - ジョブ実行状況の取得

2. ジョブ再実行APIの実装
   - エンドポイント: `POST /api/v1/job-runs/{id}/retry`
   - 失敗したジョブの再実行

**フロントエンド**

1. ジョブ実行状況一覧画面の実装（Admin）
2. ジョブ詳細画面の実装
3. ジョブ再実行画面の実装
```

### タスク3: システム管理（System Admin）の実装

```markdown
**バックエンド**

1. 会社設定管理APIの実装
   - エンドポイント: `GET /api/v1/companies`
   - エンドポイント: `POST /api/v1/companies`
   - エンドポイント: `PATCH /api/v1/companies/{id}`

2. 契約・プラン管理APIの実装
   - エンドポイント: `GET /api/v1/contracts`
   - エンドポイント: `POST /api/v1/contracts`
   - エンドポイント: `PATCH /api/v1/contracts/{id}`

3. 全ユーザー・権限管理APIの実装
   - エンドポイント: `GET /api/v1/users`
   - エンドポイント: `PATCH /api/v1/users/{id}`
   - エンドポイント: `PATCH /api/v1/company-memberships/{id}`

**フロントエンド**

1. 会社設定管理画面の実装（System Admin）
2. 契約・プラン管理画面の実装（System Admin）
3. 全ユーザー・権限管理画面の実装（System Admin）
```

## 共通タスク

### テスト実装

各フェーズで以下のテストを実装：

1. **単体テスト**
   - Java: JUnit 5 + Mockito
   - TypeScript: Jest + React Testing Library

2. **統合テスト**
   - APIエンドポイントの統合テスト
   - データベース統合テスト

3. **E2Eテスト**
   - 主要フローのE2Eテスト

### ドキュメント更新

各フェーズで以下のドキュメントを更新：

1. API仕様書の更新
2. コードコメントの追加
3. READMEの更新（必要に応じて）

## 実行時の注意事項

### マルチテナント分離の徹底

- 全てのAPIエンドポイントで`company_id`をトークンから取得
- リクエストパラメータの`company_id`は無視する
- 他社データへのアクセスは403 Forbiddenを返却

### セキュリティ

- 全てのAPIエンドポイントで認証・認可を実装
- 全ての入力値をバリデーション
- SQLインジェクション対策（プリペアドステートメント）

### テスト

- 各機能で単体テスト・統合テストを実装
- マルチテナント分離のテストを重点的に実施

## 参考ドキュメント

- [README.md](./README.md): プロジェクト概要、技術スタック
- [docs/910_開発順序.md](./docs/910_開発順序.md): 詳細な開発順序と実装ガイドライン
- [docs/500_API基本方針.md](./docs/500_API基本方針.md): API設計方針
- [.cursor/rules](./.cursor/rules): コーディングルールとアーキテクチャ原則

