# AuraTime

マルチテナント型勤怠・給与管理システム

## 概要

AuraTimeは、中規模〜大規模組織をターゲットとした、セキュアでスケーラブルなマルチテナント型「勤怠・給与管理システム」です。組織の複雑な構造（部署、チーム、プロジェクト、コストセンター）に柔軟に対応し、正確な労働時間の集計から給与計算、支払管理までを一気通貫でサポートします。

## 主な機能

- **勤怠管理**: PWAによるWeb/モバイル打刻、日次集計、シフト管理（ICカードは将来対応）
- **休暇管理**: 各種休暇（有給・特休等）の申請・承認、残数管理
- **給与管理**: 給与形態（月給・時給）に応じた計算、支給・控除項目の自由設定
- **支払管理**: 銀行振込データ（FBデータ）作成、支払ステータス管理
- **監査・ログ**: 業務データ変更履歴の保持、ジョブ実行履歴の可視化

## 技術スタック

### フロントエンド
- **Next.js 16.0.10以上** (TypeScript必須)
  - App Router
  - Server Components / Client Components
  - Server Actions
  - **重要**: セキュリティアップデート（CVE-2025-55184, CVE-2025-55183）が適用されたバージョンを使用
  - **TypeScript**: 必須。JavaScriptは使用しない。
- **React 19.2.1以上**
  - **重要**: CVE-2025-55182（RCE）が修正されたバージョンを使用
- **pnpm** (パッケージマネージャー)
  - 厳密な依存関係管理
  - ディスク容量の節約
  - 高速なインストール
- **Tailwind CSS 4.x** (スタイリング)
- **Shadcn UI** (UIコンポーネント)
- **React Hook Form** (フォーム管理)
- **Zod** (バリデーション)
- **React Query** (データフェッチング)
- **PWA**: Progressive Web App（通知機能、オフライン対応）

### バックエンド
- **Spring Boot 3.3.x以上** (Java)
  - Spring Security (認証・認可)
  - Spring Batch (バッチ処理)
  - Spring Data JPA (データベースアクセス)
  - Spring AOP (監査ログ)
- **Java 21 LTS** (またはJava 17 LTS)
- **Gradle** (ビルドツール)
  - Kotlin DSL推奨（またはGroovy DSL）
  - Spring Boot公式推奨

### データベース・インフラ
- **PostgreSQL 16.x** (AWS RDS推奨)
  - UUID v7生成関数 (`gen_uuid_v7()`) が必要
  - 拡張機能: `pgcrypto`
- **Redis 7.2.x以上** (ElastiCache推奨)
  - セッションストア
  - キャッシュ
  - キュー管理（オプション）
- **AWS S3** (ファイルストレージ)
  - 給与明細PDF
  - CSV/Excel出力
- **AWS CloudWatch** (ログ管理)
  - システムログ
  - アプリケーションログ

### デプロイメント
- **フロントエンド**: AWS Amplify
  - Git連携で自動デプロイ
  - CloudFrontで高速配信
- **バックエンド**: AWS EC2 / ECS
  - Dockerコンテナ化
  - ロードバランサー対応

## システム要件

### 開発環境
- **Node.js**: 20.x LTS以上（または22.x LTS）
  - 最新版（v24.x等）も開発環境では使用可能
  - 本番環境ではLTS版を推奨
- **pnpm**: 10.x以上（パッケージマネージャー）
  - 最新版: 10.26.1（2025年1月時点）
- **Java**: 21 LTS（または17 LTS）
- **PostgreSQL**: 16.x（Docker）
- **Redis**: 7.2.x以上（Docker）

### 推奨ツール
- **nvm** (Node Version Manager): Node.jsのバージョン管理
  - Windows: [nvm-windows](https://github.com/coreybutler/nvm-windows)
  - macOS/Linux: [nvm](https://github.com/nvm-sh/nvm)
  - 複数のNode.jsバージョンを切り替えて使用可能
- **Scoop**: Windows用パッケージマネージャー（Javaバージョン管理推奨）
  - Windows: [Scoop](https://scoop.sh/)
  - Java 21（または17）のインストールと切り替えが容易
  - 他の開発ツール（Node.js、PostgreSQL等）も管理可能
  - インストール: `iwr -useb get.scoop.sh | iex`
  - Javaバージョン切り替え: `scoop reset openjdk17` または `scoop reset openjdk21`
- **Docker Desktop**: PostgreSQL/Redisのコンテナ実行環境
  - Windows/Mac: [Docker Desktop](https://www.docker.com/products/docker-desktop/)
  - データベースとRedisをDocker Composeで起動するために必要
  - インストール後、起動してから `docker-compose up -d` を実行

### 本番環境（AWS）

#### インフラ構成
- **AWS RDS (PostgreSQL 16.x)**: メインデータベース
  - Multi-AZ配置（高可用性）
  - 自動バックアップ（Point-in-Time Recovery）
  - 暗号化（AES-256）
- **AWS ElastiCache (Redis 7.2.x)**: セッション管理・キャッシュ
  - クラスターモード（スケーラビリティ）
- **AWS S3**: ファイルストレージ
  - 給与明細PDF、アップロードファイルの保存
  - ライフサイクルポリシーでアーカイブ
- **AWS CloudWatch**: ログ管理・監視
  - アプリケーションログの集約
  - メトリクス監視（CPU、メモリ、DB接続数）
  - アラート設定（PWA通知、PagerDuty連携）
- **AWS Amplify**: フロントエンドホスティング
  - Next.jsの自動デプロイ
  - CDN配信
- **AWS ECS (Fargate)**: バックエンドホスティング（推奨）
  - コンテナベースのデプロイ
  - オートスケーリング
  - または **AWS EC2**: 従来型サーバー（必要に応じて）

#### セキュリティ
- **VPC**: プライベートサブネットにRDS、ElastiCacheを配置
- **ALB (Application Load Balancer)**: HTTPS終端、SSL証明書（ACM）
- **IAM**: 最小権限の原則でアクセス制御
- **Secrets Manager**: データベース認証情報、APIキーの管理

## セキュリティに関する重要な注意事項

### Next.js / React
- **必須**: Next.js 16.0.10以上、React 19.2.1以上を使用してください
- これらのバージョンには、以下の脆弱性の修正が含まれています：
  - CVE-2025-55184: Denial of Service（DoS）
  - CVE-2025-55183: Source Code Exposure
  - CVE-2025-55182: Remote Code Execution (RCE)
- 詳細: [Next.js Security Update: December 11, 2025](https://nextjs.org/blog/security-update-2025-12-11)

### 定期的なセキュリティチェック
- 各技術スタックの公式セキュリティアドバイザリを定期的に確認してください
- セキュリティパッチがリリースされたら、速やかにアップデートしてください

## ドキュメント

詳細な設計ドキュメントは [`docs/`](./docs/) ディレクトリを参照してください。

### クイックスタート
1. [`docs/010_概要.md`](./docs/010_概要.md) - システム全体像を把握
2. [`docs/000_要件定義/020_用語集.md`](./docs/000_要件定義/020_用語集.md) - 用語の定義を確認
3. [`docs/100_基本設計/150_業務フロー.md`](./docs/100_基本設計/150_業務フロー.md) - 業務の流れを理解
4. [`docs/000_目次.md`](./docs/000_目次.md) - 全ドキュメント一覧

### 主要ドキュメント
- **設計**: [`docs/100_基本設計/110_基本設計.md`](./docs/100_基本設計/110_基本設計.md), [`docs/200_詳細設計/221_DB設計.md`](./docs/200_詳細設計/221_DB設計.md)
- **API仕様**: [`docs/200_詳細設計/240_API.md`](./docs/200_詳細設計/240_API.md)
- **ER図**: [`docs/200_詳細設計/210_ER図.mmd`](./docs/200_詳細設計/210_ER図.mmd)
- **セキュリティ**: [`docs/200_詳細設計/260_セキュリティー.md`](./docs/200_詳細設計/260_セキュリティー.md)

## セットアップ

### 1. リポジトリのクローン

```bash
git clone <repository-url>
cd AuraTime
```

### 2. データベースのセットアップ（Docker推奨）

#### Docker Composeを使用する場合（推奨）

**前提条件**: Docker Desktopが起動している必要があります。

```bash
# 1. Docker Desktopの起動確認
# Windows: タスクバーのDockerアイコンを確認
# Mac: メニューバーのDockerアイコンを確認
# 起動していない場合は、Docker Desktopアプリケーションを起動してください

# 2. PostgreSQLとRedisを起動
docker-compose up -d

# 3. コンテナの状態確認
docker-compose ps

# ログの確認
docker-compose logs -f

# 停止
docker-compose down

# データも含めて完全に削除（注意：データが消えます）
docker-compose down -v
```

**トラブルシューティング**:
- `unable to get image` または `The system cannot find the file specified` エラーが出る場合
  - Docker Desktopが起動していない可能性があります
  - Docker Desktopを起動してから、再度 `docker-compose up -d` を実行してください
- Docker Desktopの起動確認方法
  ```bash
  # Dockerのバージョン確認（起動していれば表示される）
  docker --version
  
  # Docker Composeのバージョン確認
  docker-compose --version
  ```

**docker-compose.ymlの内容**:
- **PostgreSQL 16**: ポート5432、データベース名`auratime`
- **Redis 7.2**: ポート6379
- データは永続化されます（Dockerボリューム）

#### ローカルPostgreSQLの場合

```bash
# データベース作成
createdb auratime

### 3. 初期マイグレーションの実行

#### 開発環境（初回セットアップ時のみ）

**注意**: 以下の手動実行は開発環境の初回セットアップ時のみ許可されます。本番環境ではFlywayによる自動実行を使用してください。

```bash
# マイグレーションファイルを実行
psql -d auratime -f database/migrations/20260101_init.sql
```

**注意**: 初期マイグレーションには以下が含まれます：
- UUID v7生成関数 (`gen_uuid_v7()`)
- 全テーブルの作成
- インデックス、制約の設定

#### 本番環境（推奨）

Spring Bootアプリケーション起動時に、Flywayが自動的にマイグレーションを実行します。

1. マイグレーションファイルを `src/main/resources/db/migration/` に配置
2. アプリケーションをデプロイ
3. 起動時に自動実行

詳細は [`docs/200_詳細設計/212_DDL運用.md`](./docs/200_詳細設計/212_DDL運用.md) を参照してください。

### 4. 初期データの投入

システム管理用ユーザー（`system-bot`）の作成が必要です。詳細は [`docs/200_詳細設計/212_DDL運用.md`](./docs/200_詳細設計/212_DDL運用.md) を参照してください。

### 5. Javaのセットアップ（Scoop推奨）

#### Scoopを使用する場合（推奨）

```powershell
# Scoopが未インストールの場合
iwr -useb get.scoop.sh | iex

# Javaバケットの追加
scoop bucket add java

# Java 21 LTSをインストール（推奨）
scoop install openjdk21

# または、Java 17 LTSをインストール
scoop install openjdk17

# バージョン確認
java -version

# バージョン切り替え（必要に応じて）
scoop reset openjdk21  # Java 21に切り替え
scoop reset openjdk17  # Java 17に切り替え
```

#### 手動インストールの場合

1. [Eclipse Temurin (Adoptium)](https://adoptium.net/)からJava 21 LTSまたはJava 17 LTSをダウンロード
2. インストーラーを実行
3. 環境変数`JAVA_HOME`と`PATH`を設定

### 6. フロントエンドのセットアップ

#### nvmを使用する場合（推奨）

```bash
# Node.jsのインストール（推奨バージョン: 20.x LTS）
nvm install 20
nvm use 20

# バージョン確認
node --version

# pnpmのインストール（未インストールの場合）
npm install -g pnpm

# フロントエンドのセットアップ
cd frontend
pnpm install
pnpm dev
```

#### 手動インストールの場合

```bash
# Node.jsを公式サイトからインストール
# https://nodejs.org/ からLTS版をダウンロード・インストール

# pnpmのインストール（未インストールの場合）
npm install -g pnpm

# フロントエンドのセットアップ
cd frontend
pnpm install
pnpm dev
```

### 7. バックエンドのセットアップ

```bash
cd backend
./gradlew bootRun
```

## プロジェクト構造

```
AuraTime/
├── docs/                    # 設計ドキュメント
│   ├── 000_目次.md         # ドキュメント目次
│   ├── 010_概要.md         # システム概要
│   ├── 100_基本設計/       # 基本設計
│   ├── 200_詳細設計/       # 詳細設計（ER図、DB設計、API仕様等）
│   ├── 300_開発/           # 開発順序
│   └── ...                 # その他の設計ドキュメント
├── frontend/               # Next.jsフロントエンド
│   ├── app/               # App Router
│   ├── components/        # Reactコンポーネント
│   ├── lib/              # ユーティリティ
│   ├── package.json
│   └── pnpm-lock.yaml     # pnpmロックファイル
├── backend/               # Spring Bootバックエンド
│   ├── src/
│   │   └── main/
│   │       └── resources/
│   │           └── db/
│   │               └── migration/  # Flywayマイグレーションファイル
│   │       ├── java/      # Javaソースコード
│   │       └── resources/ # 設定ファイル
│   └── build.gradle.kts   # Gradleビルド設定（Kotlin DSL）
├── database/
│   └── migrations/
│       └── 20260101_init.sql  # 初期マイグレーション
├── docker-compose.yml    # PostgreSQL/Redis用Docker Compose設定
└── README.md             # このファイル
```

## 開発

### 開発環境の構築

1. データベースのセットアップ（上記「セットアップ」を参照）
2. フロントエンドのセットアップ（Next.js）
3. バックエンドのセットアップ（Spring Boot）
4. 環境変数の設定（`.env` ファイル等）
5. 開発サーバーの起動

### コーディング規約

- **フロントエンド**: ESLint + Prettier（Next.js標準）
- **バックエンド**: Google Java Style Guide または Checkstyle
- **コミットメッセージ**: Conventional Commits

### テスト

- 詳細は [`docs/400_テスト/400_テスト設計.md`](./docs/400_テスト/400_テスト設計.md) を参照
- マルチテナント境界のテストを重点的に実施
- **フロントエンド**: Jest + React Testing Library
- **バックエンド**: JUnit 5 + Mockito
- **E2Eテスト**: Playwright（主要フローのE2Eテスト）
- **統合テスト**: Testcontainers（PostgreSQL/Redisのコンテナテスト）
- **APIテスト**: REST Assured（RESTful APIのテスト）
- **カバレッジ**: JaCoCo（Java）、istanbul/nyc（JavaScript）

## アーキテクチャの特徴

### マルチテナント設計
- 1つのデータベース内で `company_id` による厳密なデータ分離
- 共有スキーマ方式を採用

### ID管理モデル
- **ユーザー（`m_users`）**: ログイン用の認証主体
- **従業員（`m_employees`）**: 会社ごとの雇用契約情報
- 1つのユーザーが複数の会社に所属可能（兼務対応）

### データ保護
- 全テーブルにソフト削除機能
- 監査列（`created_by`, `updated_by`）による変更履歴の追跡
- UUID v7による推測困難なID生成

## 未決定事項

以下の技術選定は未決定です。詳細は [`docs/000_要件定義/010_決定事項_未決定事項.md`](./docs/000_要件定義/010_決定事項_未決定事項.md) を参照してください。

- 行レベルセキュリティ（RLS）の実装有無
- 給与計算エンジンのDSL導入の是非
- データアーカイブ方針（7年以上保管が義務付けられる法定帳票データの長期保存方法）

**注**:
- **認証方式**: 初期実装では内製JWTを使用し、将来的なOAuth2/OIDC対応の可能性を残しています。
- **RLS（Row Level Security）**: 初期実装ではアプリケーション層でのマルチテナント分離を実装しますが、セキュリティ要件や監査要件に応じて、将来的にDB層でのRLS導入を検討します。

## ライセンス

[`LICENSE`](./LICENSE)を参照してください。

## 貢献

コントリビューションガイドラインは実装時に追加予定です。
