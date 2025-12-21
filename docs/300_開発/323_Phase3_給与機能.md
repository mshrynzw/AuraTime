# Phase 3: 給与機能

## 1. 目的

- 給与計算機能を実装し、給与明細の作成・確定・公開を行う
- 支払管理機能を実装し、振込データの出力を行う

## 2. 実装内容

### 2.1 給与計算基盤

#### バックエンド

- [ ] 給与期間管理
  - `GET /api/v1/payroll-periods`
  - `POST /api/v1/payroll-periods`
  - `PATCH /api/v1/payroll-periods/{id}`
  - ステータス管理（`open`, `closed`, `calculated`, `finalized`）
- [ ] 単価設定（`h_compensation_plans`）
  - `GET /api/v1/compensation-plans`
  - `POST /api/v1/compensation-plans`
  - `PATCH /api/v1/compensation-plans/{id}`
- [ ] 給与計算ロジック
  - 勤怠集計結果と単価設定に基づく計算
  - 支給項目・控除項目の計算
  - 社会保険料の計算（簡易版）

### 2.2 給与計算・明細

#### バックエンド

- [ ] 給与計算ジョブ
  - `POST /api/v1/payroll-periods/{id}/calculate`
  - 非同期バッチ処理（Spring Batch）
  - ジョブ実行状況の管理（`t_job_runs`）
- [ ] 明細管理API
  - `GET /api/v1/payslips`
  - `PATCH /api/v1/payslips/{id}`
  - ステータス管理（`draft`, `confirmed`, `paid`）
- [ ] 明細確定・公開
  - `POST /api/v1/payslips/{id}/finalize`
  - 従業員への公開

#### フロントエンド

- [ ] 給与期間管理画面（Admin）
- [ ] 給与計算実行画面（Admin）
- [ ] 明細一覧画面（Admin）
- [ ] 明細確定画面（Admin）
- [ ] 給与明細確認画面（Employee）

### 2.3 支払管理

#### バックエンド

- [ ] 支払バッチ作成API
  - `POST /api/v1/payments`
  - `GET /api/v1/payments`
  - FBデータ（振込データ）の生成
- [ ] 支払ステータス管理
  - `PATCH /api/v1/payments/{id}`
  - ステータス管理（`scheduled`, `processing`, `done`, `failed`）

#### フロントエンド

- [ ] 支払バッチ作成画面（Admin）
- [ ] 支払状況確認画面（Admin）
- [ ] FBデータ出力画面（Admin）

## 3. APIエンドポイント

| メソッド | エンドポイント | 権限 | 説明 |
| :--- | :--- | :--- | :--- |
| GET | `/api/v1/payroll-periods` | Admin | 給与期間一覧取得 |
| POST | `/api/v1/payroll-periods` | Admin | 給与期間作成 |
| PATCH | `/api/v1/payroll-periods/{id}` | Admin | 給与期間更新 |
| POST | `/api/v1/payroll-periods/{id}/calculate` | Admin | 給与計算実行 |
| GET | `/api/v1/compensation-plans` | Admin | 単価設定一覧 |
| POST | `/api/v1/compensation-plans` | Admin | 単価設定作成 |
| GET | `/api/v1/payslips` | Employee/Admin | 明細一覧取得 |
| PATCH | `/api/v1/payslips/{id}` | Admin | 明細更新 |
| POST | `/api/v1/payslips/{id}/finalize` | Admin | 明細確定 |
| POST | `/api/v1/payments` | Admin | 支払バッチ作成 |
| GET | `/api/v1/payments` | Admin | 支払一覧取得 |
| PATCH | `/api/v1/payments/{id}` | Admin | 支払ステータス更新 |

## 4. テスト項目

- [ ] 給与期間管理のテスト
  - 給与期間の作成・更新
  - 締め処理（`closed`）
  - 締め後のデータロック確認
- [ ] 給与計算のテスト
  - 給与計算ジョブの実行
  - 明細の生成
  - 計算結果の正確性
- [ ] 明細管理のテスト
  - 明細の作成・更新
  - 明細の確定・公開
  - 従業員への公開確認
- [ ] 支払管理のテスト
  - 支払バッチの作成
  - FBデータの生成
  - 支払ステータスの更新

## 5. 完了基準

- [ ] 給与期間を管理できる
- [ ] 給与計算が正常に実行される
- [ ] 明細が正しく生成される
- [ ] 明細を確定・公開できる
- [ ] 支払バッチを作成できる
- [ ] FBデータを出力できる
- [ ] 全てのテストがパスする

## 6. 依存関係

- Phase 1が完了していること（勤怠集計）
- Phase 2が完了していること（承認フロー）
- バッチ処理基盤（Spring Batch）が動作していること

## 7. 参考ドキュメント

- [開発順序](./300_概要.md): 開発フェーズの概要
- [API仕様_給与](../200_詳細設計/234_API_給与.md): 給与管理APIの詳細仕様
- [バッチ設計](../200_詳細設計/240_バッチ.md): バッチ処理の設計
