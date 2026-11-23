# MyPetStore 用户端

独立运行的服务器端渲染商城，使用 Spring Boot、Spring MVC、Thymeleaf、Spring Security、MyBatis-Plus 和 MySQL。与 `MyPetStore-admin` 共享 `MyPetStore` 数据库，但使用独立端口和会话 Cookie。

## 启动

```bash
export DB_PASSWORD='你的 MySQL 密码'
./mvnw clean verify
java -jar target/mypetstore-user-0.0.1-SNAPSHOT.jar
```

访问 <http://localhost:8080/main>。管理端继续使用 <http://localhost:8081>。

若尚未初始化数据库，请只在空数据库中执行 `database/02-schema-with-demo.sql`。用户端不会在启动时重建或清空数据。

## 已有数据库升级

从旧版本升级时，在共享 MyPetStore 数据库执行一次
`database/04-add-archive-columns.sql`，然后重启用户端和管理端。
新建数据库使用的两份建表脚本已包含归档字段，无需再执行迁移。
