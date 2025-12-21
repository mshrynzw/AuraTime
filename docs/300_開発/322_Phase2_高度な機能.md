# Phase 2: 高度な機能

## 1. 目的

- 勤怠修正申請、休暇管理、シフト管理などの高度な機能を実装
- 承認フローを実装し、業務プロセスを完成させる

## 2. 実装内容

### 2.1 勤怠管理（修正申請・承認）

#### バックエンド

- [ ] 勤怠修正申請API
  - `PATCH /api/v1/time-records/{id}`
  - ステータス管理（`draft`, `submitted`, `approved`, `rejected`）
  - 承認済み・締め済みの場合は409 Conflict
- [ ] 承認API
  - `POST /api/v1/approval-requests/{id}/approve`
  - `POST /api/v1/approval-requests/{id}/reject`
  - 承認ルートの管理

#### フロントエンド

- [ ] 勤怠修正申請画面
- [ ] 承認待ち一覧画面（Manager）
- [ ] 承認画面

### 2.2 休暇管理

#### バックエンド

- [ ] 休暇申請API
  - `POST /api/v1/leave-requests`
  - `GET /api/v1/leave-requests`
  - `PATCH /api/v1/leave-requests/{id}`
  - 残数管理
- [ ] 休暇タイプ管理
  - `GET /api/v1/leave-types`
  - `POST /api/v1/leave-types`

#### フロントエンド

- [ ] 休暇申請画面
- [ ] 休暇履歴画面
- [ ] 残数確認画面

### 2.3 シフト管理

#### バックエンド

- [ ] シフト割当API
  - `GET /api/v1/shift-assignments`
  - `PUT /api/v1/shift-assignments/bulk`
  - 個人別・日別シフト割当
- [ ] シフト一覧API
  - `GET /api/v1/shifts`
  - カレンダー表示用

#### フロントエンド

- [ ] シフト作成・割当画面（Manager）
- [ ] シフト一覧画面（カレンダー表示）

### 2.4 承認フロー（汎用化）

#### バックエンド

- [ ] 承認ルート設定
  - `GET /api/v1/approval-routes`
  - `POST /api/v1/approval-routes`
  - 多段承認の管理
- [ ] 承認ステップ管理
  - `GET /api/v1/approval-steps`
  - 承認ステップの進行状況管理

#### フロントエンド

- [ ] 承認ルート設定画面（Admin）
- [ ] 承認ステップ表示

## 3. APIエンドポイント

| メソッド | エンドポイント | 権限 | 説明 |
| :--- | :--- | :--- | :--- |
| PATCH | `/api/v1/time-records/{id}` | Employee | 勤怠修正申請 |
| GET | `/api/v1/approval-requests` | Manager | 承認待ち一覧取得 |
| POST | `/api/v1/approval-requests/{id}/approve` | Manager | 承認 |
| POST | `/api/v1/approval-requests/{id}/reject` | Manager | 却下 |
| POST | `/api/v1/leave-requests` | Employee | 休暇申請 |
| GET | `/api/v1/leave-requests` | Employee | 休暇履歴取得 |
| GET | `/api/v1/leave-types` | Employee | 休暇タイプ一覧 |
| GET | `/api/v1/shift-assignments` | Manager | シフト割当取得 |
| PUT | `/api/v1/shift-assignments/bulk` | Manager | シフト一括割当 |
| GET | `/api/v1/shifts` | Employee | シフト一覧取得 |
| GET | `/api/v1/approval-routes` | Admin | 承認ルート一覧 |
| POST | `/api/v1/approval-routes` | Admin | 承認ルート作成 |

## 4. テスト項目

- [ ] 勤怠修正申請のテスト
  - 修正申請の作成
  - 承認・却下の処理
  - 承認済み・締め済みの場合は409 Conflict
- [ ] 休暇管理のテスト
  - 休暇申請の作成
  - 残数の減算
  - 承認フローの動作
- [ ] シフト管理のテスト
  - シフト割当の作成
  - 一括割当の処理
  - シフト一覧の取得
- [ ] 承認フローのテスト
  - 多段承認の動作
  - 承認ステップの進行
  - 承認ルートの設定

## 5. 完了基準

- [ ] 勤怠修正申請が正常に動作する
- [ ] 承認フローが正常に動作する
- [ ] 休暇申請が正常に動作する
- [ ] シフト管理が正常に動作する
- [ ] 全てのテストがパスする

## 6. 依存関係

- Phase 1が完了していること（打刻機能、マスタ管理）
- 承認フローは休暇管理と勤怠管理の両方で使用される

## 7. 参考ドキュメント

- [開発順序](./300_概要.md): 開発フェーズの概要
- [API仕様_勤怠](../200_詳細設計/231_API_勤怠.md): 勤怠管理APIの詳細仕様
- [API仕様_休暇](../200_詳細設計/232_API_休暇.md): 休暇管理APIの詳細仕様
- [API仕様_承認](../200_詳細設計/233_API_承認.md): 承認フローAPIの詳細仕様
