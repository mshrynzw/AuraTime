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

# 切り替え後、PowerShellを再起動するか、以下のコマンドでPATHを再読み込み
refreshenv
```

**トラブルシューティング**:

- **Java 21をインストールしたのに、`java -version`でJava 8（1.8.0_471等）やJava 17が表示される場合**
  - システムのPATHに古いJavaバージョン（Java 8等）が含まれていて、ScoopのJava 21より優先されている可能性があります
  - 以下の手順で対処してください：

    1. **Javaのインストール場所を確認**

       ```powershell
       # 現在使用されているJavaのパスを確認
       (Get-Command java).Path

       # すべてのJavaインストールを確認
       scoop list | Select-String java
       ```

    2. **ScoopのJava 21を有効化**

       ```powershell
       scoop reset openjdk21
       refreshenv  # またはPowerShellを再起動
       java -version  # Java 21が表示されることを確認
       ```

    3. **それでも古いバージョンが表示される場合**
       - システムの環境変数`PATH`にJava 8のパスが含まれている可能性があります
       - 以下のコマンドでPATHを確認し、Java 8のパスを削除または後ろに移動してください：

         ```powershell
         # 環境変数PATHを確認
         $env:PATH -split ';' | Select-String java

         # システムの環境変数を確認（管理者権限が必要な場合あり）
         [Environment]::GetEnvironmentVariable("PATH", "Machine") -split ';' | Select-String java
         ```

       - 環境変数の編集方法：
         1. Windowsの設定 → システム → 詳細情報 → システムの詳細設定
         2. 「環境変数」ボタンをクリック
         3. 「システム環境変数」の`PATH`を選択して「編集」
         4. Java 8のパス（例：`C:\Program Files\Java\jdk1.8.0_471\bin`）を削除または下に移動
         5. PowerShellを再起動して確認

- **`scoop reset openjdk21`実行時に「The following instances of "openjdk21" are still running」エラーが表示される場合**
  - 実行中のJavaプロセス（IDE、アプリケーションサーバー等）が原因です
  - 以下のいずれかの方法で対処してください：
    1. **実行中のJavaアプリケーションを終了する**（推奨）
       - IDE（IntelliJ IDEA、Eclipse等）を閉じる
       - Spring Bootアプリケーションを停止する
       - その他のJavaアプリケーションを終了する
    2. **強制終了する場合**（注意：データ損失の可能性あり）

       ```powershell
       # 実行中のJavaプロセスを確認
       Get-Process java

       # すべてのJavaプロセスを一度に終了（推奨）
       # 注意：すべてのJavaアプリケーションが終了します
       Get-Process java | Stop-Process -Force

       # または、特定のプロセスIDを終了（例：PID 26228）
       Stop-Process -Id 26228 -Force
       ```

       **注意**: コマンドのタイポに注意してください
       - `-Force`（正しい）: 大文字のF、小文字のorce
       - パラメーター間は半角スペースを使用してください（全角スペースはエラーになります）

    3. **プロセスを終了しても新しいプロセスが起動し続ける場合**
       - IDE（IntelliJ IDEA、Eclipse、VS Code等）が自動的にJavaプロセスを再起動している可能性があります
       - 以下の手順で対処してください：
         1. **IDEを完全に終了する**
            - IDEのウィンドウを閉じるだけでは不十分な場合があります
            - タスクマネージャーでIDEのプロセスも確認し、終了してください
         2. **すべてのJavaプロセスを終了してから、すぐに`scoop reset`を実行**

            ```powershell
            # すべてのJavaプロセスを終了
            Get-Process java | Stop-Process -Force

            # すぐにscoop resetを実行（IDEが再起動する前に）
            scoop reset openjdk21
            ```

         3. **IDEを起動する前に、Javaバージョンの切り替えを完了する**
            - `scoop reset openjdk21`を実行
            - `refreshenv`を実行（またはPowerShellを再起動）
            - `java -version`でJava 21が表示されることを確認
            - その後、IDEを起動

    4. **PowerShellを再起動してから再度実行**
       - 新しいPowerShellウィンドウを開いてから`scoop reset openjdk21`を実行

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

#### 手動インストールの場合（Node.js）

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

**前提条件**: Gradle Wrapperファイル（`gradlew`または`gradlew.bat`）が存在する必要があります。

**Windows (PowerShell):**

```powershell
cd backend
.\gradlew.bat bootRun
```

**macOS / Linux (Bash):**

```bash
cd backend
./gradlew bootRun
```

**注意**: PowerShellでは`./gradlew`ではなく`.\gradlew.bat`を使用してください。`./gradlew`はエラーになります。

**トラブルシューティング**:

- **`gradlew.bat`が見つからない場合**
  - Gradle Wrapperファイルが生成されていない可能性があります
  - 以下の手順でGradle Wrapperを生成してください：

    1. **Gradleをインストール**（Scoop推奨）

       ```powershell
       scoop install gradle
       ```

    2. **Gradle Wrapperを生成**

       ```powershell
       cd backend
       gradle wrapper --gradle-version 8.9
       ```

       **注意**: `gradle wrapper`コマンドが失敗する場合（Gradle 9.xでKotlin DSLの互換性問題が発生する場合）は、以下の手順で手動でGradle Wrapperを作成してください：

       1. **`gradle/wrapper`ディレクトリを作成**

          ```powershell
          New-Item -ItemType Directory -Force -Path gradle\wrapper
          ```

       2. **`gradle-wrapper.properties`を作成**

          ```powershell
          @"
          distributionBase=GRADLE_USER_HOME
          distributionPath=wrapper/dists
          distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
          networkTimeout=10000
          validateDistributionUrl=true
          zipStoreBase=GRADLE_USER_HOME
          zipStorePath=wrapper/dists
          "@ | Out-File -FilePath gradle\wrapper\gradle-wrapper.properties -Encoding UTF8
          ```

       3. **`gradle-wrapper.jar`をダウンロード**

          ```powershell
          Invoke-WebRequest -Uri "https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar" -OutFile "gradle\wrapper\gradle-wrapper.jar"
          ```

       4. **`gradlew.bat`を作成**（標準的なGradle Wrapperスクリプトをコピー）

          - Spring Initializrで生成されたプロジェクトからコピーするか、[Gradle公式リポジトリ](https://github.com/gradle/gradle/tree/v8.9.0/gradle/wrapper)から取得してください

    3. **生成されたファイルを確認**
       - `gradlew`（Unix用）
       - `gradlew.bat`（Windows用）
       - `gradle/wrapper/gradle-wrapper.properties`
       - `gradle/wrapper/gradle-wrapper.jar`

    4. **動作確認**

       ```powershell
       .\gradlew.bat --version
       ```

    5. **アプリケーションを起動**

       ```powershell
       .\gradlew.bat bootRun
       ```

  - または、Gradleがインストールされている場合は直接実行することもできます：

    ```powershell
    cd backend
    gradle bootRun
    ```

### 8. VS Codeの設定（推奨）

プロジェクトには`.vscode`フォルダが含まれており、推奨拡張機能とワークスペース設定が定義されています。

#### 推奨拡張機能のインストール

VS Codeを開くと、推奨拡張機能のインストールを促す通知が表示されます。以下の拡張機能が推奨されています：

**フロントエンド開発:**

- ESLint
- Prettier
- TypeScript
- Tailwind CSS IntelliSense
- React Snippets

**バックエンド開発:**

- Extension Pack for Java
- Spring Boot Tools
- Spring Boot Dashboard

**共通:**

- GitLens
- Markdown All in One
- PostgreSQL
- Docker

#### ワークスペース設定

`.vscode/settings.json`には以下の設定が含まれています：

- **自動フォーマット**: 保存時に自動でコードをフォーマット
- **ESLint自動修正**: 保存時にESLintエラーを自動修正
- **インポート整理**: 保存時に未使用のインポートを削除
- **タブサイズ**: TypeScript/JavaScriptは2スペース、Javaは4スペース
- **ファイルエンコーディング**: UTF-8、改行コードはLF

#### デバッグ設定

`.vscode/launch.json`には以下のデバッグ設定が含まれています：

- **Next.js: debug server-side**: サーバーサイドのデバッグ
- **Next.js: debug client-side**: クライアントサイドのデバッグ（Chrome）
- **Spring Boot: debug**: Spring Bootアプリケーションのデバッグ

#### タスク設定

`.vscode/tasks.json`には以下のタスクが定義されています：

- **Frontend: dev server**: フロントエンド開発サーバーの起動
- **Backend: bootRun**: バックエンドアプリケーションの起動
- **Database: start**: Docker Composeでデータベースを起動

**使用方法:**

1. `Ctrl+Shift+P` (Windows) / `Cmd+Shift+P` (Mac) でコマンドパレットを開く
2. `Tasks: Run Task` を選択
3. 実行したいタスクを選択

#### 個人設定

個人固有の設定を追加したい場合は、`.vscode/settings.local.json`を作成してください（このファイルは`.gitignore`に含まれています）。

## プロジェクト構造

```text
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
│   ├── __tests__/        # テストコード（Jest + React Testing Library）
│   │   ├── app/          # ページコンポーネントのテスト
│   │   └── lib/          # ユーティリティのテスト
│   ├── jest.config.js    # Jest設定
│   ├── jest.setup.js     # Jestセットアップ
│   ├── package.json
│   └── pnpm-lock.yaml     # pnpmロックファイル
├── backend/               # Spring Bootバックエンド
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/      # Javaソースコード
│   │   │   └── resources/ # 設定ファイル
│   │   │       └── db/
│   │   │           └── migration/  # Flywayマイグレーションファイル
│   │   └── test/
│   │       ├── java/      # テストコード（JUnit 5 + Mockito）
│   │       │   └── com/auratime/
│   │       │       ├── api/        # コントローラーのテスト
│   │       │       ├── service/    # サービスのテスト
│   │       │       ├── util/        # ユーティリティのテスト
│   │       │       ├── filter/      # フィルターのテスト
│   │       │       └── integration/ # 統合テスト（Testcontainers）
│   │       └── resources/ # テスト用設定ファイル
│   │           └── application-test.yml
│   └── build.gradle.kts   # Gradleビルド設定（Kotlin DSL）
├── database/
│   └── migrations/
│       └── 20260101_init.sql  # 初期マイグレーション
├── .vscode/              # VS Code設定（推奨拡張機能、ワークスペース設定）
│   ├── extensions.json   # 推奨拡張機能
│   ├── settings.json     # ワークスペース設定
│   ├── launch.json       # デバッグ設定
│   └── tasks.json        # タスク設定
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

#### フロントエンドテスト

- **単体テスト**: Jest + React Testing Library
  - フォルダー: `frontend/__tests__/`
  - ページコンポーネント: `frontend/__tests__/app/`
  - ユーティリティ・バリデーション: `frontend/__tests__/lib/`

#### バックエンドテスト

- **単体テスト**: JUnit 5 + Mockito
  - コントローラー: `backend/src/test/java/com/auratime/api/`
  - サービス: `backend/src/test/java/com/auratime/service/`
  - ユーティリティ: `backend/src/test/java/com/auratime/util/`
  - フィルター: `backend/src/test/java/com/auratime/filter/`
  - 例外ハンドラー: `backend/src/test/java/com/auratime/api/v1/exception/`

- **統合テスト**: Testcontainers（PostgreSQL/Redisのコンテナテスト）
  - フォルダー: `backend/src/test/java/com/auratime/integration/`
  - 認証フロー: `AuthIntegrationTest.java`
  - 認可テスト: `AuthorizationTest.java`
  - マルチテナント分離: `MultiTenantIsolationTest.java`

- **APIテスト**: REST Assured（RESTful APIのテスト）
  - 統合テスト内で実施（`backend/src/test/java/com/auratime/integration/`）

#### E2Eテスト

- **E2Eテスト**: Playwright（主要フローのE2Eテスト）
  - フォルダー: `e2e/`（将来実装予定）

#### カバレッジ

- **カバレッジ**: JaCoCo（Java）、istanbul/nyc（JavaScript）
  - レポート出力先: `backend/build/reports/jacoco/`（JaCoCo）
  - レポート出力先: `frontend/coverage/`（istanbul/nyc）

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
