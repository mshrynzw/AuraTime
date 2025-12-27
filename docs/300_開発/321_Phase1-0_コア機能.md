# Phase 1: コア機能（MVP）

## 1. 目的

- 基本的な勤怠管理機能を実装し、MVP（Minimum Viable Product）を完成させる
- 従業員が打刻し、勤怠を確認できる機能を提供

## 2. 実装内容

### 2.1 マスタ管理（Admin）

#### バックエンド

- [ ] 会社設定管理
  - `GET /api/v1/companies/{id}`
  - `PATCH /api/v1/companies/{id}`
- [ ] グループ（組織）管理
  - `GET /api/v1/groups`
  - `POST /api/v1/groups`
  - `PATCH /api/v1/groups/{id}`
  - `DELETE /api/v1/groups/{id}`（ソフト削除）
- [ ] 従業員マスタ
  - `GET /api/v1/employees`
  - `POST /api/v1/employees`
  - `PATCH /api/v1/employees/{id}`
  - 従業員とユーザーの紐付け
- [ ] シフトテンプレート管理
  - `GET /api/v1/shift-templates`
  - `POST /api/v1/shift-templates`
  - `PATCH /api/v1/shift-templates/{id}`

#### フロントエンド

- [ ] 会社設定画面（System Admin）
- [ ] グループ管理画面（Admin）
- [ ] 従業員マスタ画面（Admin）
- [ ] シフトテンプレート管理画面（Admin）

### 2.2 打刻機能（Employee）

#### バックエンド

- [ ] 打刻API
  - `POST /api/v1/time-clock/events`
  - `type`: `in`, `out`, `break_in`, `break_out`
  - `source`: `web`, `mobile`, `ic_card`, `admin`（注: `ic_card`は将来対応、初期実装では`web`と`mobile`のみ）
  - `company_id`, `employee_id`は自動設定
- [ ] 打刻履歴取得API
  - `GET /api/v1/time-clock/events`
  - クエリパラメータ：`start_date`, `end_date`, `type`
  - ページング対応

#### フロントエンド

- [ ] 打刻画面
  - 現在時刻の表示
  - 出勤・退勤・休憩入り・休憩戻りボタン
  - 打刻履歴の表示
- [ ] 打刻履歴一覧画面

### 2.3 日次集計（自動計算）

#### バックエンド

- [ ] 日次集計ロジック
  - 打刻イベントから`t_time_records`を自動生成/更新
  - 労働時間の計算（法定内、残業、深夜、休憩）
  - シフトテンプレートとの照合
- [ ] 日次集計API
  - `GET /api/v1/time-records`
  - クエリパラメータ：`start_date`, `end_date`
  - ページング対応

#### フロントエンド

- [ ] 日次勤怠一覧画面
  - カレンダー表示
  - 日別の労働時間表示
  - 残業時間の表示

## 3. APIエンドポイント

| メソッド | エンドポイント | 権限 | 説明 |
| :--- | :--- | :--- | :--- |
| GET | `/api/v1/companies/{id}` | System Admin | 会社設定取得 |
| PATCH | `/api/v1/companies/{id}` | System Admin | 会社設定更新 |
| GET | `/api/v1/groups` | Admin | グループ一覧取得 |
| POST | `/api/v1/groups` | Admin | グループ作成 |
| PATCH | `/api/v1/groups/{id}` | Admin | グループ更新 |
| DELETE | `/api/v1/groups/{id}` | Admin | グループ削除 |
| GET | `/api/v1/employees` | Admin | 従業員一覧取得 |
| POST | `/api/v1/employees` | Admin | 従業員作成 |
| PATCH | `/api/v1/employees/{id}` | Admin | 従業員更新 |
| GET | `/api/v1/shift-templates` | Admin | シフトテンプレート一覧 |
| POST | `/api/v1/shift-templates` | Admin | シフトテンプレート作成 |
| POST | `/api/v1/time-clock/events` | Employee | 打刻イベント作成 |
| GET | `/api/v1/time-clock/events` | Employee | 打刻履歴取得 |
| GET | `/api/v1/time-records` | Employee | 日次勤怠一覧取得 |

## 4. テスト項目

- [ ] マスタ管理のテスト
  - グループの作成・更新・削除
  - 従業員の作成・更新
  - シフトテンプレートの作成
- [ ] 打刻機能のテスト
  - 出勤・退勤・休憩の打刻
  - 打刻履歴の取得
  - マルチテナント分離の確認
- [ ] 日次集計のテスト
  - 打刻イベントからの自動集計
  - 労働時間の計算（法定内、残業、深夜）
  - シフトテンプレートとの照合

## 5. 完了基準

- [ ] 管理者がマスタデータを管理できる
- [ ] 従業員が打刻できる
- [ ] 打刻履歴を確認できる
- [ ] 日次集計が自動的に実行される
- [ ] 日次勤怠を確認できる
- [ ] 全てのテストがパスする

## 6. 依存関係

- Phase 0が完了していること（認証・認可基盤）
- マスタデータが投入されていること（テスト用）

## 7. 参考ドキュメント

- [開発順序](./300_概要.md): 開発フェーズの概要
- [API仕様_勤怠](../200_詳細設計/231_API_勤怠.md): 勤怠管理APIの詳細仕様
- [DB設計方針](../200_詳細設計/221_DB設計.md): マルチテナント、ソフト削除、監査列
