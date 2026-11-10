# MyPetStore 管理端

实验一商城后台 CMS，采用 Spring Boot、Spring MVC、Thymeleaf 和 MyBatis-Plus 构建。

## 环境要求

- Java 21
- MySQL 8.x（数据库接入将在下一阶段配置）
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

先完成构建，再运行生成的可执行 Jar：

```bash
./mvnw clean verify
java -jar store-admin-web/target/store-admin-web-0.0.1-SNAPSHOT.jar
```

启动后访问 <http://localhost:8081/>。

当前初始化版本暂时关闭数据源自动配置，因此不依赖本机 MySQL 即可验证工程骨架。接入课程数据库时，将增加本地配置示例并启用数据源；真实用户名和密码不得提交到 Git。
