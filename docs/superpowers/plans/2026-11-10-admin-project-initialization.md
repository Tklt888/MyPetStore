# 管理端项目初始化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建可构建、可测试、可启动的 MyPetStore 管理端五模块 Maven 工程。

**Architecture:** 根 POM 聚合五个模块并统一依赖版本。Web 模块是唯一 Spring Boot 启动模块，沿 `web -> service -> dao -> model` 使用下层能力；common 存放跨模块基础类型。

**Tech Stack:** Java 21、Spring Boot 3.5.16、Spring MVC、Thymeleaf、MyBatis-Plus 3.5.18、MySQL、JUnit 5、Maven Wrapper

**Spec:** `docs/superpowers/specs/2026-11-10-admin-project-initialization-design.md`

## 全局约束

- 根工程目录必须为 `MyPetStore-admin`。
- 基础包名必须为 `org.csu.mypetstore`。
- 管理端默认端口必须为 8081。
- 本阶段不得要求真实数据库可用，也不得提交数据库密码。
- 不创建 REST API 或前端分离模块。

## 复核重点

- 没有全局 Maven 时，`./mvnw` 仍能执行完整构建。
- Spring 上下文测试不得因尚未配置 MySQL 而失败。
- Web 模块必须能够访问 service 层，但下层模块不能反向依赖 web。
- 首页必须返回 Thymeleaf 模板而不是 JSON。
- 构建产物必须是可执行 Spring Boot Jar，而不是仅有普通 Jar。

---

### Task 1: 建立 Maven 聚合结构与依赖边界

**Files:**
- Create: `MyPetStore-admin/pom.xml`
- Create: `MyPetStore-admin/store-common/pom.xml`
- Create: `MyPetStore-admin/store-model/pom.xml`
- Create: `MyPetStore-admin/store-dao/pom.xml`
- Create: `MyPetStore-admin/store-service/pom.xml`
- Create: `MyPetStore-admin/store-admin-web/pom.xml`
- Create: `MyPetStore-admin/.mvn/wrapper/maven-wrapper.properties`
- Create: `MyPetStore-admin/mvnw`
- Create: `MyPetStore-admin/mvnw.cmd`

**Interfaces:**
- Consumes: 无。
- Produces: 五模块 Maven Reactor；`store-admin-web` 对 `store-service` 的编译依赖。

- [ ] 创建父 POM，锁定 Java 21、Spring Boot 3.5.16 和 MyBatis-Plus 3.5.18，并声明五个模块。
- [ ] 创建各模块 POM，按设计建立单向依赖关系。
- [ ] 加入 Maven Wrapper 配置与脚本。
- [ ] 运行 `./mvnw -q help:evaluate -Dexpression=project.modules -DforceStdout`，确认列出五个模块。

### Task 2: 用测试驱动最小管理端应用

**Files:**
- Test: `MyPetStore-admin/store-admin-web/src/test/java/org/csu/mypetstore/admin/AdminApplicationTest.java`
- Create: `MyPetStore-admin/store-admin-web/src/main/java/org/csu/mypetstore/admin/AdminApplication.java`
- Create: `MyPetStore-admin/store-admin-web/src/main/java/org/csu/mypetstore/admin/controller/HomeController.java`
- Create: `MyPetStore-admin/store-admin-web/src/main/resources/templates/index.html`
- Create: `MyPetStore-admin/store-admin-web/src/main/resources/application.yml`

**Interfaces:**
- Consumes: 任务一提供的 Web 模块构建和测试依赖。
- Produces: `AdminApplication` 启动入口；GET `/` 返回视图名 `index`。

- [ ] 先编写 `AdminApplicationTest`，使用 `@WebMvcTest(HomeController.class)` 断言 GET `/` 返回 200、视图名 `index`，且模型属性 `applicationName` 为 `MyPetStore 管理端`。
- [ ] 运行 `./mvnw -pl store-admin-web -am test`，确认测试因缺少 `HomeController` 而失败。
- [ ] 创建最小启动类、Controller、首页模板和端口配置，使测试通过。
- [ ] 再运行 `./mvnw -pl store-admin-web -am test`，确认测试通过。

### Task 3: 验证完整构建与可执行产物

**Files:**
- Create: `MyPetStore-admin/README.md`

**Interfaces:**
- Consumes: 任务一和任务二的完整工程。
- Produces: 可复现的构建、测试和启动说明。

- [ ] 编写 README，记录 Java 21、`./mvnw clean verify`、启动命令、8081 地址和数据库接入留待后续阶段。
- [ ] 运行 `./mvnw clean verify`，确认 Reactor 全部模块成功且测试零失败。
- [ ] 检查 `store-admin-web/target/store-admin-web-*.jar` 存在且包含 Spring Boot loader。
- [ ] 提交并推送，提交信息为 `feat: initialize admin multi-module project`。

