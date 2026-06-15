# TypeScript React + Spring Boot REST + Oracle 学習プロジェクト

登録・ログインと社員情報の登録、取得、編集、削除を実装した Java フルスタック学習プロジェクトです。

## 技術スタック

- バックエンド：Java 21、Spring Boot 3.4、Spring Web REST API、Spring Data JPA、Bean Validation
- フロントエンド：TypeScript、React 19、Vite 6
- データベース：Oracle Database Free 23ai
- テスト：Spring Boot Test、MockMvc、H2 Oracle 互換モード

## プロジェクト構成

```text
.
├── backend/                 # Spring Boot REST API
├── frontend/                # TypeScript React フロントエンド
├── compose.yml              # ローカル Oracle Free
└── .env.example             # データベース環境変数の例
```

## 必要な環境

- JDK 21+
- Maven 3.9+
- Node.js 20+
- Docker（ローカル Oracle の起動に使用）

システムに Java、Maven、Node.js がない場合は、プロジェクト内の開発ツールを読み込めます。

```bash
source ./env.sh
```

この環境では Colima を Docker エンジンとして使用します。

```bash
source ./env.sh
colima start --vm-type vz
```

## プロジェクトの起動

### 1. Oracle を起動

```bash
cp .env.example .env
docker compose up -d
```

初回起動には数分かかる場合があります。デフォルト接続情報：

```text
URL:        jdbc:oracle:thin:@localhost:1521/FREEPDB1
ユーザー名: app_user
パスワード: app_password
```

### 2. バックエンドを起動

```bash
cd backend
export DB_URL=jdbc:oracle:thin:@localhost:1521/FREEPDB1
export DB_USERNAME=app_user
export DB_PASSWORD=app_password
mvn spring-boot:run
```

バックエンドは `http://localhost:8080` で起動します。初回起動時に Hibernate が必要なテーブルとシーケンスを作成します。

### 3. フロントエンドを起動

```bash
cd frontend
npm install
npm run dev
```

ブラウザで `http://localhost:5173` を開きます。開発サーバーは `/api` リクエストをバックエンドへプロキシします。

## REST API

| メソッド | パス | 説明 |
| --- | --- | --- |
| POST | `/api/auth/register` | アカウント登録 |
| POST | `/api/auth/login` | ログイン |
| GET | `/api/employees` | 社員一覧の取得 |
| GET | `/api/employees/{id}` | 社員情報の取得 |
| POST | `/api/employees` | 社員の登録 |
| PUT | `/api/employees/{id}` | 社員情報の更新 |
| DELETE | `/api/employees/{id}` | 社員の削除 |

リクエストボディの例：

```json
{
  "name": "山田太郎",
  "email": "yamada@example.com",
  "department": "開発部",
  "salary": 300000,
  "hireDate": "2026-06-10"
}
```

## テストとビルド

```bash
cd backend
mvn test
mvn clean package

cd ../frontend
npm run typecheck
npm run build
```

`backend` で `mvn clean package` を実行すると、TypeScript の型チェック、React のビルド、Spring Boot JAR への静的ファイル組み込みが自動で行われます。

```bash
source ./env.sh
docker compose up -d
java -jar backend/target/employee-api-0.0.1-SNAPSHOT.jar
```

ブラウザで `http://localhost:8080` を開きます。

バックエンドテストではインメモリデータベースを使用するため、ローカル Oracle は不要です。本番環境では `DDL_AUTO=validate` と Flyway または Liquibase の使用を推奨します。
