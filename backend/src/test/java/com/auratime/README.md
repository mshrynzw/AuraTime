# バックエンドテスト

## テスト構造

```text
src/test/java/com/auratime/
├── api/
│   └── v1/
│       ├── controller/     # コントローラーのテスト
│       └── exception/      # 例外ハンドラーのテスト
├── service/                # サービスのテスト
├── util/                   # ユーティリティのテスト
├── filter/                 # フィルターのテスト
└── integration/            # 統合テスト
```

## テスト実行

### PowerShellでの実行（Windows）

PowerShellでテストを実行する場合、文字化けを防ぐためにUTF-8エンコーディングを設定してください：

```powershell
# PowerShellのエンコーディングをUTF-8に設定
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"

# すべてのテストを実行
./gradlew test

# 特定のテストクラスを実行
./gradlew test --tests AuthServiceTest

# 特定のパッケージのテストを実行
./gradlew test --tests "com.auratime.integration.*"

# テストレポートを生成（JaCoCo設定後）
./gradlew test jacocoTestReport
```

**永続的な設定（推奨）**: PowerShellプロファイルに以下を追加すると、毎回設定する必要がなくなります：

```powershell
# PowerShellプロファイルを開く
notepad $PROFILE

# 以下の行を追加
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"
```

### コマンドプロンプト（CMD）での実行

```cmd
# すべてのテストを実行
gradlew.bat test

# 特定のテストクラスを実行
gradlew.bat test --tests AuthServiceTest
```

### テスト実行時の注意事項

- テストは自動的にH2インメモリデータベースを使用します
- 各テストは`@Transactional`により、実行後に自動的にロールバックされます
- テスト実行前にDockerや外部サービスを起動する必要はありません
- PowerShellで文字化けが発生する場合は、上記のエンコーディング設定を確認してください

## テスト環境

- **データベース**: H2（インメモリ）
- **プロファイル**: `test`（`application-test.yml`を使用）
- **トランザクション**: 各テスト後にロールバック（`@Transactional`）

### 外部サービスの起動について

**Docker Composeの起動は不要です。**

現在のテスト設定では、以下の理由により外部サービス（PostgreSQL、Redis等）の起動は不要です：

- **データベース**: H2インメモリデータベース（`jdbc:h2:mem:testdb`）を使用
  - テスト実行ごとに新しいデータベースが作成され、終了時に自動的に破棄されます
  - 外部のPostgreSQLサーバーは不要です

- **Redis**: テストコードでは使用されていません
  - `application-test.yml`に設定はありますが、実際のテストでは使用されていません

### 将来的な拡張

将来的にTestcontainersを使用してPostgreSQLやRedisのコンテナテストを行う場合は、Dockerが必要になります。その場合は、以下のようにテストを拡張できます：

```java
@SpringBootTest
@Testcontainers
class SomeIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    // ...
}
```

その場合、テスト実行前にDockerが起動している必要があります。

## テストカバレッジ

現在、JaCoCoは設定されていません。カバレッジを測定する場合は、`build.gradle.kts`にJaCoCoプラグインを追加する必要があります。

### JaCoCoの設定例

```kotlin
plugins {
    // ... 既存のプラグイン ...
    jacoco
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
```

設定後、以下のコマンドでカバレッジレポートを生成できます：

```bash
./gradlew test jacocoTestReport
```

レポートは `build/reports/jacoco/test/html/index.html` に生成されます。
