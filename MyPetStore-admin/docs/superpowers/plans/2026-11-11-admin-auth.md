# 管理员登录与权限 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 使用数据库账号与 Spring Security 为 MyPetStore 管理端增加管理员登录、受保护页面、会话状态复查和安全退出。

**Architecture:** `StoreUserMapper` 和 `AdminAccountService` 提供与 HTTP 无关的账号查询；`AdminUserDetailsService` 将启用管理员适配为 Spring Security 身份；`SecurityConfig` 统一配置表单登录、授权、CSRF 与退出，`AdminAccountStatusFilter` 在每次受保护请求时复查账号角色和状态。

**Tech Stack:** Java 21、Spring Boot 3.5.16、Spring Security 6、Spring MVC、Thymeleaf、MyBatis-Plus 3.5.18、MySQL、JUnit 5

**Spec:** `docs/superpowers/specs/2026-11-11-admin-auth-design.md`

## Global Constraints

- 仅允许 `role=ADMIN` 且 `status=ENABLED` 的数据库账号登录。
- Session Cookie 名称必须是 `PETSTORE_ADMIN_SESSION`。
- 除 `/login` 外，管理端页面默认需要认证。
- 登录失败统一显示“账号或密码错误，或账号不可用”。
- 密码按原样进行 BCrypt 校验，不去除空格、不回显、不记录日志。
- 退出必须使用带 CSRF 的 `POST /logout`；GET 不得退出。
- Service 不依赖 HTTP、Session 或页面对象。
- 保持模块依赖方向 `admin-web → service → dao → model`。

## Review Focus

- 用户名为 `null`、纯空白或超过 50 字符时不得访问数据库或抛出页面异常；Task 1 测试覆盖。
- 不存在、CUSTOMER、DISABLED 和错误密码不得产生可区分的浏览器提示；Task 2 与 Task 4 测试覆盖。
- 已认证用户的数据库记录被删除、禁用或角色改变时，下一请求必须清除会话；Task 3 测试覆盖。
- 登录失败暂存的用户名必须截断并在渲染后删除，密码绝不能进入 Session；Task 4 测试覆盖。
- `/login` 必须公开，而 `/`、`/admin/products` 和未知后台路径默认受保护；Task 3 配置测试覆盖。

---

### Task 1: 管理员账号数据访问与业务查询

**Files:**
- Create: `store-model/src/main/java/org/csu/mypetstore/model/StoreUser.java`
- Create: `store-model/src/main/java/org/csu/mypetstore/model/dto/AdminAccount.java`
- Create: `store-dao/src/main/java/org/csu/mypetstore/dao/StoreUserMapper.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/AdminAccountService.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/impl/AdminAccountServiceImpl.java`
- Create: `store-service/src/test/java/org/csu/mypetstore/service/AdminAccountServiceTest.java`

**Interfaces:**
- Consumes: `store_user` 表中的 `id`、`username`、`password_hash`、`display_name`、`role`、`status`。
- Produces: `Optional<AdminAccount> findLoginAccount(String username)` 与 `boolean isActiveAdmin(Long userId)`；`AdminAccount` 为包含上述六个字段的不可变记录。

- [ ] **Step 1: 编写账号查询失败测试**

用不依赖 Mockito 的 `StoreUserMapper` 动态代理，覆盖合法用户名去除首尾空白、`null`、纯空白、超过 50 字符不查询、启用管理员为 true、CUSTOMER/DISABLED/不存在为 false。

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw -pl store-service -am test -Dtest=AdminAccountServiceTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，因为账号模型、Mapper 和 Service 尚不存在。

- [ ] **Step 3: 实现账号模型、参数化 Mapper 查询和 Service**

`StoreUserMapper.selectByUsername(String username)` 和 `selectRoleStatusById(Long userId)` 均使用绑定参数；Service 在调用 Mapper 前完成用户名边界校验，`isActiveAdmin` 只接受同时满足 `ADMIN + ENABLED` 的记录。

- [ ] **Step 4: 运行 Task 1 测试与全量测试**

Run: `./mvnw -pl store-service -am test -Dtest=AdminAccountServiceTest -Dsurefire.failIfNoSpecifiedTests=false && ./mvnw clean verify`

Expected: 新测试及现有测试全部 PASS。

- [ ] **Step 5: 提交 Task 1**

```bash
git add MyPetStore-admin/store-model MyPetStore-admin/store-dao MyPetStore-admin/store-service
git commit -m "feat: add admin account lookup"
```

### Task 2: Spring Security 管理员身份适配

**Files:**
- Modify: `store-admin-web/pom.xml`
- Create: `store-admin-web/src/main/java/org/csu/mypetstore/admin/security/AdminPrincipal.java`
- Create: `store-admin-web/src/main/java/org/csu/mypetstore/admin/security/AdminUserDetailsService.java`
- Create: `store-admin-web/src/test/java/org/csu/mypetstore/admin/security/AdminUserDetailsServiceTest.java`

**Interfaces:**
- Consumes: Task 1 的 `AdminAccountService.findLoginAccount(String)`。
- Produces: `AdminPrincipal implements UserDetails`，暴露 `userId`、`username`、`displayName`、BCrypt 散列和 `ROLE_ADMIN`；`AdminUserDetailsService.loadUserByUsername(String)`。

- [ ] **Step 1: 编写身份适配失败测试**

覆盖启用 ADMIN 转换为 `ROLE_ADMIN`、CUSTOMER/DISABLED/不存在统一抛 `UsernameNotFoundException`，并用真实 `BCryptPasswordEncoder` 验证演示密码 `PetStore123!` 与 SQL 中管理员散列匹配、错误密码不匹配。

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw -pl store-admin-web -am test -Dtest=AdminUserDetailsServiceTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，因为 Spring Security 依赖和身份类尚不存在。

- [ ] **Step 3: 添加 Security 依赖并实现身份适配**

加入 `spring-boot-starter-security` 与测试范围的 `spring-security-test`；`AdminUserDetailsService` 对所有不可用账号抛相同异常，`AdminPrincipal` 不在 `toString` 中输出散列。

- [ ] **Step 4: 运行 Task 2 测试与全量测试**

Run: `./mvnw -pl store-admin-web -am test -Dtest=AdminUserDetailsServiceTest -Dsurefire.failIfNoSpecifiedTests=false && ./mvnw clean verify`

Expected: 新测试及现有测试全部 PASS。

- [ ] **Step 5: 提交 Task 2**

```bash
git add MyPetStore-admin/store-admin-web/pom.xml MyPetStore-admin/store-admin-web/src/main/java/org/csu/mypetstore/admin/security MyPetStore-admin/store-admin-web/src/test/java/org/csu/mypetstore/admin/security
git commit -m "feat: add admin security identity"
```

### Task 3: 访问控制与会话状态复查

**Files:**
- Create: `store-admin-web/src/main/java/org/csu/mypetstore/admin/config/SecurityConfig.java`
- Create: `store-admin-web/src/main/java/org/csu/mypetstore/admin/security/AdminAccountStatusFilter.java`
- Create: `store-admin-web/src/test/java/org/csu/mypetstore/admin/security/AdminAccountStatusFilterTest.java`
- Create: `store-admin-web/src/test/java/org/csu/mypetstore/admin/config/SecurityConfigTest.java`
- Modify: `store-admin-web/src/main/resources/application.yml`

**Interfaces:**
- Consumes: Task 1 的 `AdminAccountService.isActiveAdmin(Long)` 与 Task 2 的 `AdminPrincipal`、`AdminUserDetailsService`。
- Produces: `SecurityFilterChain`；`BCryptPasswordEncoder`；失效账号重定向 `/login?expired` 的 `AdminAccountStatusFilter`。

- [ ] **Step 1: 编写状态过滤器失败测试**

使用 `MockHttpServletRequest/Response` 和真实 `SecurityContext`，覆盖启用管理员继续过滤链，禁用/删除/角色变化时清除认证、使 Session 失效并重定向 `/login?expired`，匿名与 `/login` 不查询账号。

- [ ] **Step 2: 编写安全配置失败测试**

构建最小 Spring Security 测试上下文，验证 `/login` 可公开访问，`/`、`/admin/products` 和未知路径未登录时重定向登录；`GET /logout` 不退出，缺少 CSRF 的 `POST /logout` 被拒绝。若当前 JVM 的 Mockito agent 限制影响 Spring 测试，使用 `mock-maker-subclass` 测试配置而不是跳过行为验证。

- [ ] **Step 3: 运行测试确认失败**

Run: `./mvnw -pl store-admin-web -am test -Dtest=AdminAccountStatusFilterTest,SecurityConfigTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，因为过滤器和安全配置尚不存在。

- [ ] **Step 4: 实现过滤器、安全配置和 Cookie 名称**

`SecurityConfig` 使用数据库 `UserDetailsService`、BCrypt、表单登录、默认成功页 `/`、临时统一失败地址 `/login?error`、`POST /logout`、CSRF 默认开启，并在 `AuthorizationFilter` 前加入状态过滤器；`application.yml` 设置 `server.servlet.session.cookie.name: PETSTORE_ADMIN_SESSION`。Task 4 再用正式失败处理器替换临时失败地址。

- [ ] **Step 5: 运行 Task 3 测试与全量测试**

Run: `./mvnw -pl store-admin-web -am test -Dtest=AdminAccountStatusFilterTest,SecurityConfigTest -Dsurefire.failIfNoSpecifiedTests=false && ./mvnw clean verify`

Expected: 新测试及现有测试全部 PASS。

- [ ] **Step 6: 提交 Task 3**

```bash
git add MyPetStore-admin/store-admin-web
git commit -m "feat: protect admin routes"
```

### Task 4: 登录、失败回显和退出页面

**Files:**
- Create: `store-admin-web/src/main/java/org/csu/mypetstore/admin/controller/LoginController.java`
- Create: `store-admin-web/src/main/java/org/csu/mypetstore/admin/security/AdminAuthenticationFailureHandler.java`
- Create: `store-admin-web/src/main/resources/templates/login.html`
- Modify: `store-admin-web/src/main/java/org/csu/mypetstore/admin/config/SecurityConfig.java`
- Modify: `store-admin-web/src/main/java/org/csu/mypetstore/admin/controller/HomeController.java`
- Modify: `store-admin-web/src/main/resources/templates/index.html`
- Modify: `store-admin-web/src/test/java/org/csu/mypetstore/admin/AdminApplicationTest.java`
- Create: `store-admin-web/src/test/java/org/csu/mypetstore/admin/controller/LoginControllerTest.java`
- Create: `store-admin-web/src/test/java/org/csu/mypetstore/admin/security/AdminAuthenticationFailureHandlerTest.java`
- Modify: `README.md`

**Interfaces:**
- Consumes: Task 2 的 `AdminPrincipal` 和 Task 3 的安全配置扩展点。
- Produces: `GET /login` 视图；失败重定向 `/login?error`；首页管理员名称和带 CSRF 的 `POST /logout` 表单。

- [ ] **Step 1: 编写登录页与失败处理器失败测试**

不用 Mockito：验证失败处理器只把去首尾、最多 50 字符的用户名写入一次性 Session 属性并重定向 `/login?error`，不保存 `password`；验证 `LoginController` 渲染后删除该属性，并对 `error/logout/expired` 显示互斥提示。

- [ ] **Step 2: 编写首页身份展示失败测试**

更新现有普通 Java Controller 测试，传入 `AdminPrincipal` 并断言模型包含显示名称；独立渲染首页模板，断言退出是 POST 表单且包含 CSRF 字段，不存在 GET 退出链接。

- [ ] **Step 3: 运行测试确认失败**

Run: `./mvnw -pl store-admin-web -am test -Dtest=LoginControllerTest,AdminAuthenticationFailureHandlerTest,AdminApplicationTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，因为登录 Controller、失败处理器和页面尚不存在。

- [ ] **Step 4: 实现登录页、失败回显、首页身份和退出表单**

登录模板使用 `POST /login`，字段名为 `username/password` 并包含 `_csrf`；失败处理器不读取或保存密码；SecurityConfig 注入该处理器。首页只显示 `AdminPrincipal.displayName`，退出表单提交 `POST /logout`。

- [ ] **Step 5: 更新 README**

记录登录地址、演示账号 `admin / PetStore123!`、Cookie 名称及安全退出方式；明确演示凭据只用于本地教学数据。

- [ ] **Step 6: 运行全量验证**

Run: `./mvnw clean verify`

Expected: 6 个模块全部 SUCCESS，所有测试 0 failures / 0 errors。

- [ ] **Step 7: 本地数据库冒烟测试**

由用户在本地设置 `DB_PASSWORD` 后启动 Jar，验证未登录重定向、正确管理员登录、错误密码统一提示、商品列表访问、POST 退出和 Cookie 名称。密码不得进入命令历史、日志、测试或提交。

- [ ] **Step 8: 提交 Task 4**

```bash
git add MyPetStore-admin
git commit -m "feat: add admin login and logout"
```
