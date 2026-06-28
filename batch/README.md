# Jingda Batch

This is a standalone Spring Boot batch project for cron execution.

## Build

```sh
source ../env.sh
mvn -f pom.xml package
```

The executable jar is created at:

```text
batch/target/jingda-batch-0.0.1-SNAPSHOT.jar
```

## Manual Run

Run with the default batch code and today's date in `Asia/Tokyo`:

```sh
java -jar target/jingda-batch-0.0.1-SNAPSHOT.jar
```

Run a specific batch date:

```sh
java -jar target/jingda-batch-0.0.1-SNAPSHOT.jar \
  --app.batch.code=credit-card-repayment \
  --app.batch.target-date=2026-06-27
```

Useful environment variables:

```sh
export DB_URL='jdbc:mysql://115.190.57.247:3306/jingda?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Tokyo'
export DB_USERNAME='app_user'
export DB_PASSWORD='app_password'
export BATCH_CODE='credit-card-repayment'
export BATCH_ZONE='Asia/Tokyo'
```

## Cron Example

Run at 00:05 every day in Japan time:

```cron
CRON_TZ=Asia/Tokyo
5 0 * * * root /opt/jingda-batch/run-credit-card-repayment.sh >> /var/log/jingda-batch/credit-card-repayment.log 2>&1
```
