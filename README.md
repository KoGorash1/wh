# WH — интернет-магазин

Spring Boot интернет-магазин с каталогом, корзиной, заказами и административной панелью.

## Запуск локально

Требуются Java 17+, Maven и PostgreSQL. Создайте базу данных `wh` и задайте пароль пользователя PostgreSQL в PowerShell:

```powershell
$env:DB_PASSWORD = "пароль_пользователя_postgres"
mvn spring-boot:run
```

Приложение откроется на <http://localhost:8080>. Пароль базы не хранится в репозитории.

## Сборка

```powershell
mvn package
```

