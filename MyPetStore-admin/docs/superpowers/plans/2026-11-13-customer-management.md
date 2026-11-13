# 后台普通用户查询与启停实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现后台普通用户筛选分页及安全启用、禁用闭环，同时禁止密码、角色、管理员账号和其他资料被修改。

**Architecture:** 使用专用查询 DTO 和显式 MyBatis SQL 将列表永久限定为 `CUSTOMER`，关键词在 Service 中规范化并转义为字面量 LIKE。状态变更采用 ID、角色、旧状态三重条件更新；Web 层区分 400、403、404 与成功/重复操作的 PRG。

**Tech Stack:** Java 21、Spring Boot 3.5.16、Spring MVC、Spring Security、Thymeleaf、MyBatis-Plus 3.5.18、JUnit 5、Mockito

**Spec:** `docs/superpowers/specs/2026-11-13-customer-management-design.md`

## Global Constraints

- 列表和计数始终固定 `role = 'CUSTOMER'`，不能通过请求参数扩大范围。
- 用户名搜索去首尾空白，最长 50 字符；`%`、`_` 和转义字符必须按普通文字匹配。
- 状态只接受 `ENABLED`、`DISABLED`；空值或 `ALL` 表示全部。
- `page` 默认 1，小于 1 归一为 1，格式错误或溢出返回 400；`pageSize` 默认 10，仅允许 1—50。
- 状态更新只修改 `status`，条件必须包含 ID、`CUSTOMER` 角色和旧状态。
- 非法参数 400、非普通用户目标 403、缺失用户 404；仅成功或重复操作使用 PRG。
- 页面和 DTO 不查询、不返回、不渲染密码散列，也不提供角色或资料编辑能力。
- 本计划不实现用户端持久会话撤销机制，不声称完成双应用会话验收。

## Review Focus

- 使用 `%`、`_`、反斜杠搜索时只能匹配字面用户名，不能扩大结果集。
- 构造管理员 ID 发起状态 POST 必须返回 403，且不能泄露账号资料或修改数据库。
- 两个同向启停请求在没有反向中间变更时最多一次 UPDATE 成功，重复请求不刷新 `updated_at`。
- 超末页必须保留筛选与页大小并提供有效页链接，零结果必须提供清空筛选入口。
- 已禁用管理员的真实 `AdminPrincipal` 必须在 Controller 前被过滤，Service 不得被调用。

---

### Task 1: 普通用户查询模型、Mapper 与 Service

**Files:**
- Create: `store-model/src/main/java/org/csu/mypetstore/model/query/CustomerListQuery.java`
- Create: `store-model/src/main/java/org/csu/mypetstore/model/dto/CustomerListItem.java`
- Modify: `store-dao/src/main/java/org/csu/mypetstore/dao/StoreUserMapper.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/CustomerQueryService.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/impl/CustomerQueryServiceImpl.java`
- Create: `store-service/src/test/java/org/csu/mypetstore/service/CustomerListQueryTest.java`
- Create: `store-service/src/test/java/org/csu/mypetstore/service/CustomerQueryServiceTest.java`
- Create: `store-admin-web/src/test/java/org/csu/mypetstore/admin/CustomerMapperContractTest.java`

**Interfaces:**
- Consumes: 既有 `store_user` 表和 MyBatis-Plus 分页配置。
- Produces: `CustomerListQuery.normalize()` 及 `normalizedKeywordPattern/status/page/pageSize`；`IPage<CustomerListItem> findCustomers(CustomerListQuery query)`；`StoreUserMapper.selectCustomerPage(Page<CustomerListItem>, String keywordPattern, String status)`。

- [ ] **Step 1: 写失败的查询规范化测试**

  覆盖默认页与页大小、负页归一、用户名 trim、空搜索、50/51 字符边界、五个包含 `%`、`_`、`\` 的字面转义样例、`ALL`、两种合法状态、未知状态和 `pageSize=1/50/0/51`。

- [ ] **Step 2: 写失败的 Mapper 与 Service 测试**

  Mapper 契约断言显式投影不含 `password_hash`、固定 `role = 'CUSTOMER'`、LIKE 使用 `ESCAPE`、状态可组合且 `ORDER BY id ASC`；Service 测试断言规范值和页大小传入 Mapper。

- [ ] **Step 3: 运行测试确认失败**

  Run: `./mvnw -pl store-service,store-admin-web -am -Dtest=CustomerListQueryTest,CustomerQueryServiceTest,CustomerMapperContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
  Expected: FAIL，原因是查询模型、Mapper 方法和 Service 尚不存在。

- [ ] **Step 4: 实现只读模型和字面量 LIKE 规范化**

  `CustomerListQuery` 将关键字中的 `\`、`%`、`_` 依次转义并交由 SQL `LIKE CONCAT('%', #{keywordPattern}, '%') ESCAPE '\\'` 使用；DTO 只包含 ID、用户名、显示名、邮箱、手机号、状态和创建时间。

- [ ] **Step 5: 实现 Mapper 和查询 Service**

  `selectCustomerPage` 使用显式字段和固定角色条件；`CustomerQueryServiceImpl.findCustomers` 只用规范值创建 `Page<CustomerListItem>`。

- [ ] **Step 6: 运行任务测试**

  Run: `./mvnw -pl store-service,store-admin-web -am -Dtest=CustomerListQueryTest,CustomerQueryServiceTest,CustomerMapperContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
  Expected: PASS。

- [ ] **Step 7: 提交**

  ```bash
  git add store-model store-dao store-service store-admin-web/src/test/java/org/csu/mypetstore/admin/CustomerMapperContractTest.java
  git commit -m "feat: add customer account queries"
  ```

### Task 2: 原子启停与错误语义

**Files:**
- Create: `store-model/src/main/java/org/csu/mypetstore/model/command/CustomerStatusCommand.java`
- Modify: `store-dao/src/main/java/org/csu/mypetstore/dao/StoreUserMapper.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/CustomerStatusService.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/CustomerStatusResult.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/CustomerNotFoundException.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/CustomerForbiddenException.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/CustomerStateException.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/impl/CustomerStatusServiceImpl.java`
- Create: `store-service/src/test/java/org/csu/mypetstore/service/CustomerStatusServiceTest.java`
- Modify: `store-admin-web/src/test/java/org/csu/mypetstore/admin/CustomerMapperContractTest.java`

**Interfaces:**
- Consumes: 既有 `StoreUser` 的 ID、角色、状态字段。
- Produces: `CustomerStatusResult changeStatus(long id, CustomerStatusCommand command)`，枚举值为 `CHANGED`、`UNCHANGED`；`updateCustomerStatus(long id, String oldStatus, String targetStatus)`；`selectRoleStatusForUpdate(long id)` 使用当前读诊断失败状态。

- [ ] **Step 1: 写失败的状态 Service 测试**

  覆盖 `ENABLED -> DISABLED`、`DISABLED -> ENABLED`、未知目标 400 语义、缺失用户 404、`ADMIN` 目标 403、重复目标返回 `UNCHANGED` 且不再次写、影响 0 行后的并发状态变化提示刷新。

- [ ] **Step 2: 扩充 Mapper 契约测试**

  断言 UPDATE 的 SET 子句只有 `status`，WHERE 同时包含 ID、`role = 'CUSTOMER'`、旧状态；不出现密码、角色、资料、购物车或订单字段。

- [ ] **Step 3: 运行测试确认失败**

  Run: `./mvnw -pl store-service,store-admin-web -am -Dtest=CustomerStatusServiceTest,CustomerMapperContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
  Expected: FAIL，原因是命令、状态服务和更新 SQL 尚不存在。

- [ ] **Step 4: 实现条件更新与事务服务**

  Service 根据目标状态推导唯一旧状态并执行一次 UPDATE；失败后通过 `SELECT ... FOR UPDATE` 当前读获取角色状态，映射为 404、403、`UNCHANGED` 或并发变化，不执行补偿写入。

- [ ] **Step 5: 运行任务测试**

  Run: `./mvnw -pl store-service,store-admin-web -am -Dtest=CustomerStatusServiceTest,CustomerMapperContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
  Expected: PASS。

- [ ] **Step 6: 提交**

  ```bash
  git add store-model store-dao store-service store-admin-web/src/test/java/org/csu/mypetstore/admin/CustomerMapperContractTest.java
  git commit -m "feat: add atomic customer status changes"
  ```

### Task 3: 用户管理页面、HTTP 状态和安全过滤

**Files:**
- Create: `store-admin-web/src/main/java/org/csu/mypetstore/admin/controller/CustomerController.java`
- Create: `store-admin-web/src/main/java/org/csu/mypetstore/admin/controller/CustomerExceptionHandler.java`
- Create: `store-admin-web/src/main/resources/templates/customer/list.html`
- Modify: `store-admin-web/src/main/resources/templates/index.html`
- Modify: `store-admin-web/src/main/resources/templates/category/list.html`
- Modify: `store-admin-web/src/main/resources/templates/product/list.html`
- Modify: `store-admin-web/src/main/resources/templates/order/list.html`
- Create: `store-admin-web/src/test/java/org/csu/mypetstore/admin/controller/CustomerControllerTest.java`
- Modify: `store-admin-web/src/test/java/org/csu/mypetstore/admin/config/SecurityConfigTest.java`
- Modify: `store-admin-web/src/test/java/org/csu/mypetstore/admin/security/AdminAccountStatusFilterTest.java`

**Interfaces:**
- Consumes: `CustomerQueryService.findCustomers` 与 `CustomerStatusService.changeStatus`。
- Produces: `GET /admin/customers`、`POST /admin/customers/{id}/status`，以及 400/403/404 错误映射。

- [ ] **Step 1: 写失败的 MVC 与模板测试**

  覆盖列表模型、筛选保留、空结果、超末页有效链接、零结果清空、每页 1/50、非法参数 400、成功和重复操作 PRG、管理员目标 403、缺失目标 404；真实渲染模板断言确认框、CSRF、只有反向状态按钮且不存在密码/角色/删除/资料编辑控件。

- [ ] **Step 2: 写失败的安全测试**

  覆盖匿名 POST 携有效 CSRF 跳转登录、已认证缺 CSRF 返回 403、有效 `AdminPrincipal` 携 CSRF 能进入 Controller、已禁用管理员被 `AdminAccountStatusFilter` 重定向且状态 Service 未调用。

- [ ] **Step 3: 运行测试确认失败**

  Run: `./mvnw -pl store-admin-web -am -Dtest=CustomerControllerTest,SecurityConfigTest,AdminAccountStatusFilterTest -Dsurefire.failIfNoSpecifiedTests=false test`
  Expected: FAIL，原因是用户管理路由、模板和安全用例尚不存在。

- [ ] **Step 4: 实现 Controller 和异常映射**

  Controller 用字符串显式解析分页数值；成功和 `UNCHANGED` 使用 flash message + PRG，校验异常 400、`CustomerForbiddenException` 403、`CustomerNotFoundException` 404 由专用 Advice 保持真实状态。

- [ ] **Step 5: 实现列表模板与导航**

  页面保留规范化筛选和页大小，提供有效页链接与清空入口；表单只提交目标状态和 CSRF，并在提交前确认。

- [ ] **Step 6: 运行任务测试**

  Run: `./mvnw -pl store-admin-web -am -Dtest=CustomerControllerTest,SecurityConfigTest,AdminAccountStatusFilterTest -Dsurefire.failIfNoSpecifiedTests=false test`
  Expected: PASS。

- [ ] **Step 7: 提交**

  ```bash
  git add store-admin-web
  git commit -m "feat: add admin customer management pages"
  ```

### Task 4: 全量回归与验收资料

**Files:**
- Create: `docs/verification/customer-management-checklist.md`
- Modify: `README.md`

**Interfaces:**
- Consumes: Tasks 1—3 的完整管理端用户功能。
- Produces: 自动化证据、真实 MySQL 验收步骤和明确未完成的双应用会话验收。

- [ ] **Step 1: 运行全量测试**

  Run: `./mvnw clean verify`
  Expected: BUILD SUCCESS，0 failures，0 errors。

- [ ] **Step 2: 编写验收清单**

  包含查询、字面量通配符、分页、普通用户启停、重复操作 `updated_at` 不变、管理员 403、同向双连接并发；用户端两条会话撤销路径保持未勾选并说明需要持久撤销机制。

- [ ] **Step 3: 更新 README**

  记录 `/admin/customers` 能力与安全边界，不写入密码散列、数据库密码或虚构验收结果。

- [ ] **Step 4: 运行最终检查**

  Run: `git diff --check && ./mvnw clean verify && git status --short`
  Expected: 无 whitespace 错误、全量测试通过、状态只包含计划内文档。

- [ ] **Step 5: 提交**

  ```bash
  git add README.md docs/verification/customer-management-checklist.md
  git commit -m "docs: add customer management verification"
  ```

- [ ] **Step 6: 最终审查与推送**

  对照设计重点审查角色边界、字面量 LIKE、HTTP 状态、并发条件更新和真实模板；修复 Critical/Important 后再次运行 `./mvnw clean verify`，再推送已授权的 `main`。
