# CI/CD設定手順

本ドキュメントでは、AuraTimeシステムのCI/CDパイプラインを設定する手順を説明します。GitHub Actionsを使用した自動デプロイパイプラインを構築します。

## 前提条件

- GitHubリポジトリが作成済みであること
- [`912_アプリケーションデプロイ.md`](./912_アプリケーションデプロイ.md)の手順が完了していること
- AWS CLIがインストール・設定済みであること

## 1. GitHub Actionsの設定

### 1.1 ワークフローディレクトリの作成

```bash
# プロジェクトルートで実行
mkdir -p .github/workflows
```

### 1.2 フロントエンド用ワークフロー（.github/workflows/frontend-deploy.yml）

```yaml
name: Frontend Deploy

on:
  push:
    branches:
      - main
      - production
    paths:
      - 'frontend/**'
      - '.github/workflows/frontend-deploy.yml'
  workflow_dispatch:

env:
  AWS_REGION: ap-northeast-1
  NODE_VERSION: '20'

jobs:
  deploy:
    name: Deploy to AWS Amplify
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}

      - name: Setup pnpm
        uses: pnpm/action-setup@v2
        with:
          version: 10

      - name: Install dependencies
        working-directory: ./frontend
        run: pnpm install --frozen-lockfile

      - name: Run linter
        working-directory: ./frontend
        run: pnpm lint

      - name: Run tests
        working-directory: ./frontend
        run: pnpm test

      - name: Build
        working-directory: ./frontend
        run: pnpm build
        env:
          NEXT_PUBLIC_API_URL: ${{ secrets.NEXT_PUBLIC_API_URL }}

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Trigger Amplify deployment
        run: |
          AMPLIFY_APP_ID=${{ secrets.AMPLIFY_APP_ID }}
          BRANCH_NAME=${GITHUB_REF#refs/heads/}

          aws amplify start-job \
            --app-id $AMPLIFY_APP_ID \
            --branch-name $BRANCH_NAME \
            --job-type RELEASE
```

### 1.3 バックエンド用ワークフロー（.github/workflows/backend-deploy.yml）

```yaml
name: Backend Deploy

on:
  push:
    branches:
      - main
      - production
    paths:
      - 'backend/**'
      - '.github/workflows/backend-deploy.yml'
  workflow_dispatch:

env:
  AWS_REGION: ap-northeast-1
  ECR_REPOSITORY: auratime-backend
  ECS_SERVICE: auratime-backend
  ECS_CLUSTER: auratime-cluster
  ECS_TASK_DEFINITION: auratime-backend
  JAVA_VERSION: '21'

jobs:
  build-and-deploy:
    name: Build and Deploy to ECS
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Build, tag, and push image to Amazon ECR
        id: build-image
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          IMAGE_TAG: ${{ github.sha }}
        run: |
          cd backend

          # Dockerイメージのビルド
          docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
          docker tag $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG $ECR_REGISTRY/$ECR_REPOSITORY:latest

          # ECRにプッシュ
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:latest

          echo "image=$ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG" >> $GITHUB_OUTPUT

      - name: Download task definition
        run: |
          aws ecs describe-task-definition \
            --task-definition $ECS_TASK_DEFINITION \
            --query taskDefinition > task-definition.json

      - name: Fill in the new image ID in the Amazon ECS task definition
        id: task-def
        uses: aws-actions/amazon-ecs-render-task-definition@v1
        with:
          task-definition: task-definition.json
          container-name: auratime-backend
          image: ${{ steps.build-image.outputs.image }}

      - name: Deploy Amazon ECS task definition
        uses: aws-actions/amazon-ecs-deploy-task-definition@v1
        with:
          task-definition: ${{ steps.task-def.outputs.task-definition }}
          service: $ECS_SERVICE
          cluster: $ECS_CLUSTER
          wait-for-service-stability: true

      - name: Run tests
        working-directory: ./backend
        run: |
          ./gradlew test

      - name: Run integration tests
        working-directory: ./backend
        run: |
          ./gradlew integrationTest
        env:
          TEST_DATABASE_URL: ${{ secrets.TEST_DATABASE_URL }}
          TEST_REDIS_URL: ${{ secrets.TEST_REDIS_URL }}
```

### 1.4 統合テスト用ワークフロー（.github/workflows/integration-tests.yml）

```yaml
name: Integration Tests

on:
  pull_request:
    branches:
      - main
      - production
  push:
    branches:
      - main
      - production

env:
  JAVA_VERSION: '21'
  NODE_VERSION: '20'

jobs:
  backend-integration-tests:
    name: Backend Integration Tests
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: auratime_test
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

      redis:
        image: redis:7.2-alpine
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 6379:6379

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'

      - name: Run integration tests
        working-directory: ./backend
        run: |
          ./gradlew integrationTest
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/auratime_test
          SPRING_DATASOURCE_USERNAME: postgres
          SPRING_DATASOURCE_PASSWORD: postgres
          SPRING_DATA_REDIS_HOST: localhost
          SPRING_DATA_REDIS_PORT: 6379

  frontend-tests:
    name: Frontend Tests
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}

      - name: Setup pnpm
        uses: pnpm/action-setup@v2
        with:
          version: 10

      - name: Install dependencies
        working-directory: ./frontend
        run: pnpm install --frozen-lockfile

      - name: Run linter
        working-directory: ./frontend
        run: pnpm lint

      - name: Run tests
        working-directory: ./frontend
        run: pnpm test

      - name: Build
        working-directory: ./frontend
        run: pnpm build
        env:
          NEXT_PUBLIC_API_URL: https://api.auratime.example.com
```

## 2. GitHub Secretsの設定

GitHubリポジトリの「Settings」→「Secrets and variables」→「Actions」で以下のシークレットを設定します：

### 2.1 AWS認証情報

- `AWS_ACCESS_KEY_ID`: AWSアクセスキーID
- `AWS_SECRET_ACCESS_KEY`: AWSシークレットアクセスキー

### 2.2 アプリケーション固有の設定

- `AMPLIFY_APP_ID`: AmplifyアプリID
- `NEXT_PUBLIC_API_URL`: フロントエンドからアクセスするAPIのURL
- `TEST_DATABASE_URL`: 統合テスト用データベースURL（オプション）
- `TEST_REDIS_URL`: 統合テスト用Redis URL（オプション）

## 3. AWS IAMユーザーの作成（CI/CD用）

### 3.1 IAMユーザーの作成

```bash
# CI/CD用IAMユーザーの作成
aws iam create-user \
  --user-name auratime-cicd \
  --tags Key=Project,Value=AuraTime Key=Purpose,Value=CI/CD

# アクセスキーの作成
aws iam create-access-key --user-name auratime-cicd
```

**重要**: アクセスキーIDとシークレットアクセスキーを安全に保管し、GitHub Secretsに設定してください。

### 3.2 IAMポリシーの作成とアタッチ

```bash
# CI/CD用ポリシーの作成
cat > cicd-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecs:DescribeServices",
        "ecs:DescribeTaskDefinition",
        "ecs:DescribeTasks",
        "ecs:ListTasks",
        "ecs:RegisterTaskDefinition",
        "ecs:UpdateService"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "iam:PassRole"
      ],
      "Resource": [
        "arn:aws:iam::ACCOUNT_ID:role/auratime-ecs-task-execution-role",
        "arn:aws:iam::ACCOUNT_ID:role/auratime-ecs-task-role"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "amplify:GetApp",
        "amplify:GetBranch",
        "amplify:ListJobs",
        "amplify:StartJob"
      ],
      "Resource": "*"
    }
  ]
}
EOF

# ポリシーの作成
aws iam create-policy \
  --policy-name auratime-cicd-policy \
  --policy-document file://cicd-policy.json \
  --tags Key=Project,Value=AuraTime

# ポリシーをユーザーにアタッチ
aws iam attach-user-policy \
  --user-name auratime-cicd \
  --policy-arn arn:aws:iam::ACCOUNT_ID:policy/auratime-cicd-policy
```

**注意**: `ACCOUNT_ID`を実際のAWSアカウントIDに置き換えてください。

## 4. デプロイ戦略の設定

### 4.1 ブルー・グリーンデプロイメント（ECS）

ECSでは、デフォルトでローリングアップデートが使用されます。ブルー・グリーンデプロイメントを有効にするには、CodeDeployを使用します。

```bash
# CodeDeployアプリケーションの作成
aws deploy create-application \
  --application-name auratime-backend \
  --compute-platform ECS \
  --tags Key=Project,Value=AuraTime

# CodeDeployデプロイメントグループの作成
aws deploy create-deployment-group \
  --application-name auratime-backend \
  --deployment-group-name auratime-backend-dg \
  --service-role-arn arn:aws:iam::ACCOUNT_ID:role/auratime-codedeploy-role \
  --ecs-services clusterName=auratime-cluster,serviceName=auratime-backend \
  --load-balancer-info "targetGroupInfoList=[{name=auratime-backend-tg}]" \
  --blue-green-deployment-configuration "terminateBlueInstancesOnDeploymentSuccess={action=TERMINATE,terminationWaitTimeInMinutes=5},deploymentReadyOption={actionOnTimeout=CONTINUE_DEPLOYMENT},greenFleetProvisioningOption={action=COPY_AUTO_SCALING_GROUP}" \
  --tags Key=Project,Value=AuraTime
```

### 4.2 カナリアデプロイメント（Amplify）

Amplifyでは、カナリアデプロイメントを設定できます。

```bash
# カナリアデプロイメントの設定
aws amplify update-app \
  --app-id $AMPLIFY_APP_ID \
  --custom-rules '[
    {
      "source": "/<*>",
      "target": "/index.html",
      "status": "200",
      "condition": null
    }
  ]'
```

## 5. デプロイ通知の設定

### 5.1 Slack通知の設定（オプション）

```yaml
# .github/workflows/notify-slack.yml の例
- name: Notify Slack
  if: always()
  uses: 8398a7/action-slack@v3
  with:
    status: ${{ job.status }}
    text: 'Deployment to ${{ github.ref }} completed'
    webhook_url: ${{ secrets.SLACK_WEBHOOK_URL }}
```

### 5.2 メール通知の設定（オプション）

GitHub Actionsの通知設定で、デプロイの成功/失敗をメールで通知できます。

## 6. デプロイの確認

### 6.1 ワークフローの実行確認

1. GitHubリポジトリの「Actions」タブでワークフローの実行状況を確認
2. 各ステップのログを確認してエラーがないかチェック

### 6.2 デプロイ後の動作確認

```bash
# フロントエンドの確認
curl https://auratime.example.com

# バックエンドの確認
curl https://api.auratime.example.com/actuator/health
```

## 7. ロールバック手順

### 7.1 ECSのロールバック

```bash
# 前のタスク定義に戻す
PREVIOUS_TASK_DEF=$(aws ecs describe-task-definition \
  --task-definition auratime-backend \
  --query 'taskDefinition.revision' \
  --output text)

aws ecs update-service \
  --cluster auratime-cluster \
  --service auratime-backend \
  --task-definition auratime-backend:$((PREVIOUS_TASK_DEF - 1)) \
  --force-new-deployment
```

### 7.2 Amplifyのロールバック

```bash
# 前のデプロイに戻す
aws amplify get-app --app-id $AMPLIFY_APP_ID
aws amplify list-jobs --app-id $AMPLIFY_APP_ID --branch-name production

# 特定のジョブに戻す
aws amplify start-job \
  --app-id $AMPLIFY_APP_ID \
  --branch-name production \
  --job-type RELEASE
```

## 次のステップ

- [`914_監視・ログ設定.md`](./914_監視・ログ設定.md): 監視とログ設定
- [`915_セキュリティ設定.md`](./915_セキュリティ設定.md): SSL証明書とセキュリティ設定

## トラブルシューティング

### GitHub Actionsが失敗する

- **エラー**: `AWS credentials not configured`
  - **解決策**: GitHub SecretsにAWS認証情報が正しく設定されているか確認

### ECRへのプッシュが失敗する

- **エラー**: `AccessDenied`
  - **解決策**: IAMユーザーにECRへの適切な権限があるか確認

### ECSデプロイが失敗する

- **エラー**: `Service did not stabilize`
  - **解決策**: CloudWatch Logsでアプリケーションのログを確認し、起動エラーがないか確認

## 参考資料

- [GitHub Actions ドキュメント](https://docs.github.com/ja/actions)
- [AWS CodeDeploy ユーザーガイド](https://docs.aws.amazon.com/ja_jp/codedeploy/)
- [AWS ECS デプロイメント ベストプラクティス](https://docs.aws.amazon.com/ja_jp/AmazonECS/latest/developerguide/deployment-best-practices.html)
