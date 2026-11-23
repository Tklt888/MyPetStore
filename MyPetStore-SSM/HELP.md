# MyPetStore-SSM 工程说明

这是实验一用户端宠物商城的独立 Spring Boot 工程，目录结构与课程视频一致：

```text
MyPetStore-SSM/
├── pom.xml
├── src/main/java/
├── src/main/resources/
├── src/test/java/
└── database/
```

技术栈：Spring Boot、Spring MVC、Thymeleaf、Spring Security、MyBatis-Plus、MySQL 和 Maven。

## 数据库

在空数据库中选择执行：

- `database/01-schema.sql`：只创建表结构；
- `database/02-schema-with-demo.sql`：创建表结构并加入课程演示数据。

两个文件二选一，不要依次执行。用户端和管理端共同使用 `MyPetStore` 数据库。

## 启动

```bash
export DB_PASSWORD='你的 MySQL 密码'
./mvnw clean verify
java -jar target/mypetstore-user-0.0.1-SNAPSHOT.jar
```

用户端地址：<http://localhost:8080/main>。
