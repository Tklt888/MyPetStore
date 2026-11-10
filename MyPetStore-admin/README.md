# MyPetStore 管理端

实验一商城后台 CMS，采用 Spring Boot、Spring MVC、Thymeleaf 和 MyBatis-Plus 构建。

## 环境要求

- Java 21
- MySQL 8.x
- 无需预装 Maven，项目已包含 Maven Wrapper

## 模块结构

```text
store-common     通用工具、异常及基础类型
store-model      Entity、DTO 和 VO
store-dao        MyBatis-Plus Mapper 与数据库访问
store-service    业务逻辑与事务
store-admin-web  Spring Boot 启动、Controller 和 Thymeleaf 页面
```

模块依赖方向为 `store-admin-web -> store-service -> store-dao -> store-model`。`store-dao` 同时使用 `store-common`。

## 构建与测试

在 `MyPetStore-admin` 目录执行：

```bash
./mvnw clean verify
```

## 启动

先设置数据库密码，再完成构建并运行生成的可执行 Jar：

```bash
export DB_PASSWORD='你的 MySQL 密码'
./mvnw clean verify
java -jar store-admin-web/target/store-admin-web-0.0.1-SNAPSHOT.jar
```

启动后访问 <http://localhost:8081/>。

应用默认连接本机 `MyPetStore` 数据库，用户名默认为 `root`，也可通过 `DB_USERNAME` 修改。真实用户名和密码只通过环境变量提供，不得提交到 Git。
