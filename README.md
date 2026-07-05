# Jingda Private Hub

Jingda 個人利用向けの Spring Boot + React アプリです。家計簿、初期資産・負債、クレジットカード返済 batch、日本語学習、株式取引記録、ユーザー管理、権限管理をまとめて扱います。

## 技術スタック

- バックエンド：Java 21、Spring Boot 3.4、Spring Web REST API、Spring JDBC
- フロントエンド：TypeScript、React 19、Vite 6
- データベース：MySQL 8.4 LTS
- batch：Spring Boot CLI アプリケーション、cron 実行想定、Spring JDBC
- テスト：Spring Boot Test、MockMvc、H2

## 構成

```text
.
├── backend/      # REST API と React 静的ファイル配信
├── frontend/     # TypeScript React 画面
├── batch/        # cron から起動する独立 batch
├── compose.yml   # ローカル MySQL
└── env.sh        # プロジェクト内ツールチェーン読み込み
```

## ローカル起動

Java、Maven、Node.js をプロジェクト内ツールチェーンから使う場合：

```bash
source ./env.sh
```

MySQL を Docker Compose で起動：

```bash
docker compose up -d
```

デフォルト接続情報：

```text
Host:     localhost
Port:     3306
Database: jingda
User:     app_user
Password: app_password
```

バックエンドを起動：

```bash
source ./env.sh
cd backend
mvn spring-boot:run
```

フロントエンド開発サーバーを起動：

```bash
source ./env.sh
cd frontend
npm install
npm run dev
```

開発時は `http://localhost:5173`、JAR 実行時は `http://localhost:8080` を開きます。

## ビルド

```bash
source ./env.sh
cd backend
mvn clean package
java -jar target/jingda-api-0.0.1-SNAPSHOT.jar
```

`backend` の package 時に `frontend` の TypeScript チェックと Vite build が実行され、生成された静的ファイルが Spring Boot JAR に組み込まれます。そのため本番実行時にフロントエンドを別プロセスで起動する必要はありません。

## batch

クレジットカード返済 batch は `batch` 工程から起動します。

```bash
source ./env.sh
cd batch
mvn clean package
java -jar target/jingda-batch-0.0.1-SNAPSHOT.jar
```

実行する batch と対象日は環境変数で指定できます。

```bash
BATCH_CODE=credit-card-repayment TARGET_DATE=2026-07-05 \
java -jar target/jingda-batch-0.0.1-SNAPSHOT.jar
```

サーバーでは cron からこの JAR を起動します。接続先 DB は `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` で上書きしてください。

## テスト

```bash
source ./env.sh
mvn -f backend/pom.xml test
mvn -f batch/pom.xml test
cd frontend && npm run typecheck && npm run build
```

## 設定

主な環境変数：

```text
SERVER_PORT
DB_URL
DB_USERNAME
DB_PASSWORD
CORS_ALLOWED_ORIGIN
BATCH_CODE
TARGET_DATE
BATCH_ZONE
```

ローカルのデフォルト DB は `localhost:3306/jingda` です。サーバーに配置する場合は systemd や cron の環境変数で接続先を明示してください。
