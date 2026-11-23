# MyPetStore 用户端

独立运行的服务器端渲染商城，使用 Spring Boot、Spring MVC、Thymeleaf、Spring Security、MyBatis-Plus 和 MySQL。与 `MyPetStore-admin` 共享 `MyPetStore` 数据库，但使用独立端口和会话 Cookie。

## 启动

```bash
export DB_PASSWORD='你的 MySQL 密码'
./mvnw clean verify
java -jar target/mypetstore-user-0.0.1-SNAPSHOT.jar
```

访问 <http://localhost:8080/main>。管理端继续使用 <http://localhost:8081>。

若尚未初始化数据库，请只在空数据库中执行上级目录的 `02-schema-with-demo.sql`。用户端不会在启动时重建或清空数据。
