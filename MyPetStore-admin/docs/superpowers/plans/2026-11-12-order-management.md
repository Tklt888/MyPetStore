# 后台订单查询、详情与发货实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现后台全站订单筛选分页、订单快照详情及仅限已支付订单的一次性发货闭环。

**Architecture:** 使用专用查询 DTO 与显式 MyBatis SQL 读取订单、用户和成交快照；Service 统一规范化参数并负责事务与状态判定；发货采用 `id + PAID` 条件更新保证并发请求只成功一次。Spring MVC/Thymeleaf 只暴露列表、详情和确认发货入口，不提供任意订单编辑能力。

**Tech Stack:** Java 21、Spring Boot 3.5.16、Spring MVC、Spring Security、Thymeleaf、MyBatis-Plus 3.5.18、JUnit 5、Mockito

**Spec:** `docs/superpowers/specs/2026-11-12-order-management-design.md`

## 全局约束

- 订单号去首尾空白，非空最长 40 字符，使用完整值精确匹配。
- 状态仅允许 `PENDING_PAYMENT`、`PAID`、`SHIPPED`、`COMPLETED`、`CANCELLED`；空值或 `ALL` 表示全部。
- `page` 默认 1，小于 1 归一为 1，格式错误或溢出返回 400；`pageSize` 默认 10，仅允许 1—50。
- 订单详情必须读取 `sales_order_item` 快照，不能以当前商品资料覆盖历史。
- 发货只能执行 `PAID -> SHIPPED`，且只能写 `status` 与首次 `shipped_at`。
- 写请求必须经过管理员认证、账号状态过滤和 CSRF 防护。
- 不修改数据库结构，不实现物流信息、订单创建、支付、取消或完成。

## 审查重点

- 两个并发发货请求只能一个更新成功，第二个不能覆盖首次 `shipped_at`。
- `page` 的负数、非数字和溢出值必须分别归一或返回 400，不能落成 500。
- `pageSize` 的 1、50、0、51 必须分别按合法边界或 400 处理。
- 未支付和已取消订单不能显示伪造支付渠道或凭证。
- 发货失败后金额、库存、支付字段和原 `shipped_at` 必须保持不变。

---

### Task 1: 订单领域模型与查询参数

**Files:**
- Create: `store-model/src/main/java/org/csu/mypetstore/model/SalesOrder.java`
- Create: `store-model/src/main/java/org/csu/mypetstore/model/query/OrderListQuery.java`
- Create: `store-model/src/main/java/org/csu/mypetstore/model/dto/OrderListItem.java`
- Create: `store-model/src/main/java/org/csu/mypetstore/model/dto/OrderDetail.java`
- Create: `store-model/src/main/java/org/csu/mypetstore/model/dto/OrderItemSnapshot.java`
- Create: `store-service/src/test/java/org/csu/mypetstore/service/OrderListQueryTest.java`

**Interfaces:**
- Consumes: `sales_order`、`sales_order_item` 和 `store_user` 的既有字段。
- Produces: `OrderListQuery.normalize()`；`OrderListQuery.normalizedOrderNo()`、`normalizedStatus()`、`normalizedPage()`、`normalizedPageSize()`；订单列表、详情和明细只读投影。

- [ ] **Step 1: 写失败的查询参数测试**

  在 `OrderListQueryTest` 中锁定：空值默认 `page=1/pageSize=10`；订单号 trim；`ALL -> null`；`page=0/-1 -> 1`；五种状态合法；未知状态、41 字符订单号、`pageSize=0/51` 抛 `IllegalArgumentException`。Web 层格式错误和溢出在任务 4 测试。

- [ ] **Step 2: 运行测试确认失败**

  Run: `./mvnw -pl store-service -am -Dtest=OrderListQueryTest -Dsurefire.failIfNoSpecifiedTests=false test`
  Expected: FAIL，原因是 `OrderListQuery` 尚不存在。

- [ ] **Step 3: 实现模型和只读投影**

  `SalesOrder` 映射订单状态与时间字段；`OrderListItem` 包含列表展示所需订单、用户、金额、状态和创建时间；`OrderDetail` 包含全部订单、用户、收货、支付和状态时间字段；`OrderItemSnapshot` 只包含成交快照字段。

- [ ] **Step 4: 实现查询规范化接口**

  `OrderListQuery.normalize()` 原地生成规范值或抛 `IllegalArgumentException`；所有下游代码只读取 `normalized*` 方法，避免模板原始输入直接进入 SQL。

- [ ] **Step 5: 运行任务测试**

  Run: `./mvnw -pl store-service -am -Dtest=OrderListQueryTest -Dsurefire.failIfNoSpecifiedTests=false test`
  Expected: PASS。

- [ ] **Step 6: 提交**

  ```bash
  git add store-model store-service/src/test/java/org/csu/mypetstore/service/OrderListQueryTest.java
  git commit -m "feat: add order query model"
  ```

### Task 2: 订单查询数据访问与服务

**Files:**
- Create: `store-dao/src/main/java/org/csu/mypetstore/dao/SalesOrderMapper.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/OrderQueryService.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/OrderNotFoundException.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/impl/OrderQueryServiceImpl.java`
- Create: `store-service/src/test/java/org/csu/mypetstore/service/OrderQueryServiceTest.java`
- Create: `store-admin-web/src/test/java/org/csu/mypetstore/admin/OrderMapperContractTest.java`

**Interfaces:**
- Consumes: 任务 1 的 `OrderListQuery`、`OrderListItem`、`OrderDetail`、`OrderItemSnapshot`。
- Produces: `IPage<OrderListItem> findOrders(OrderListQuery query)`；`OrderDetail getOrder(long id)`；`List<OrderItemSnapshot> findItems(long orderId)` 由 Service 合并到详情对象。

- [ ] **Step 1: 写失败的 Mapper 契约测试**

  断言分页 SQL 使用订单号 `=`、状态可组合、排序为 `created_at DESC, id DESC`；详情联接 `store_user`；明细仅从 `sales_order_item` 读取快照并按 `id ASC`，不联接 `product`。

- [ ] **Step 2: 写失败的查询 Service 测试**

  覆盖规范化参数传给 Mapper、默认/边界页大小、详情合并多条明细、订单不存在抛 `OrderNotFoundException`、未支付详情保留空支付信息。

- [ ] **Step 3: 运行测试确认失败**

  Run: `./mvnw -pl store-service,store-admin-web -am -Dtest=OrderQueryServiceTest,OrderMapperContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
  Expected: FAIL，原因是 Mapper 与 Service 尚不存在。

- [ ] **Step 4: 实现 `SalesOrderMapper` 显式查询**

  定义 `selectOrderPage(Page<OrderListItem>, String orderNo, String status)`、`selectOrderDetail(long id)`、`selectOrderItems(long orderId)`；列名全部显式映射到 DTO 属性。

- [ ] **Step 5: 实现只读查询服务**

  `OrderQueryServiceImpl` 在只读事务中规范化查询；详情先查主单，不存在即 404 语义，再读取明细并返回完整详情。

- [ ] **Step 6: 运行任务测试**

  Run: `./mvnw -pl store-service,store-admin-web -am -Dtest=OrderQueryServiceTest,OrderMapperContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
  Expected: PASS。

- [ ] **Step 7: 提交**

  ```bash
  git add store-dao store-service store-admin-web/src/test/java/org/csu/mypetstore/admin/OrderMapperContractTest.java
  git commit -m "feat: add order query services"
  ```

### Task 3: 原子发货服务

**Files:**
- Modify: `store-dao/src/main/java/org/csu/mypetstore/dao/SalesOrderMapper.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/OrderFulfillmentService.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/OrderStateException.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/impl/OrderFulfillmentServiceImpl.java`
- Create: `store-service/src/test/java/org/csu/mypetstore/service/OrderFulfillmentServiceTest.java`
- Modify: `store-admin-web/src/test/java/org/csu/mypetstore/admin/OrderMapperContractTest.java`

**Interfaces:**
- Consumes: `SalesOrderMapper` 与 `SalesOrder` 当前状态。
- Produces: `void ship(long orderId)`；`int markShippedIfPaid(long orderId)`；失败时抛 `OrderNotFoundException` 或 `OrderStateException`。

- [ ] **Step 1: 写失败的发货测试**

  覆盖条件更新成功；影响 0 行后订单不存在；`SHIPPED/COMPLETED` 返回“已处理”；`PENDING_PAYMENT/CANCELLED` 返回“当前状态不能发货”；重复请求不再次更新。

- [ ] **Step 2: 扩充 Mapper 契约测试**

  断言 SQL 仅设置 `status='SHIPPED'` 和 `shipped_at=CURRENT_TIMESTAMP`，条件含 `id` 与 `status='PAID'`，不出现金额、库存、支付字段或订单明细更新。

- [ ] **Step 3: 运行测试确认失败**

  Run: `./mvnw -pl store-service,store-admin-web -am -Dtest=OrderFulfillmentServiceTest,OrderMapperContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
  Expected: FAIL，原因是发货接口尚不存在。

- [ ] **Step 4: 实现条件发货 SQL**

  `markShippedIfPaid(long orderId)` 使用单条条件更新，数据库当前时间只在成功迁移时写入。

- [ ] **Step 5: 实现事务服务**

  `ship(long orderId)` 先尝试条件更新；失败后重新读取当前订单并映射为不存在、已处理或非法状态，不进行第二次写入。

- [ ] **Step 6: 运行任务测试**

  Run: `./mvnw -pl store-service,store-admin-web -am -Dtest=OrderFulfillmentServiceTest,OrderMapperContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
  Expected: PASS。

- [ ] **Step 7: 提交**

  ```bash
  git add store-dao store-service store-admin-web/src/test/java/org/csu/mypetstore/admin/OrderMapperContractTest.java
  git commit -m "feat: add atomic order shipping"
  ```

### Task 4: 订单 Web 页面与安全行为

**Files:**
- Create: `store-admin-web/src/main/java/org/csu/mypetstore/admin/controller/OrderController.java`
- Create: `store-admin-web/src/main/java/org/csu/mypetstore/admin/controller/OrderExceptionHandler.java`
- Create: `store-admin-web/src/main/resources/templates/order/list.html`
- Create: `store-admin-web/src/main/resources/templates/order/detail.html`
- Modify: `store-admin-web/src/main/resources/templates/index.html`
- Modify: `store-admin-web/src/main/resources/templates/product/list.html`
- Modify: `store-admin-web/src/main/resources/templates/category/list.html`
- Create: `store-admin-web/src/test/java/org/csu/mypetstore/admin/controller/OrderControllerTest.java`
- Modify: `store-admin-web/src/test/java/org/csu/mypetstore/admin/config/SecurityConfigTest.java`

**Interfaces:**
- Consumes: `OrderQueryService.findOrders/getOrder` 与 `OrderFulfillmentService.ship`。
- Produces: `GET /admin/orders`、`GET /admin/orders/{id}`、`POST /admin/orders/{id}/ship`。

- [ ] **Step 1: 写失败的 MVC 测试**

  覆盖列表模型与筛选保留、空状态、详情和快照、多明细、仅 `PAID` 显示发货、未支付不显示支付渠道、成功/重复/非法状态发货消息、缺失订单 404。

- [ ] **Step 2: 写参数绑定与安全测试**

  覆盖 `page=0/-1` 归一；非数字/溢出页码为 400；`pageSize=1/50` 合法、`0/51` 为 400；未知状态和超长订单号为 400；发货无 CSRF 被拒绝、未认证跳转登录、禁用管理员被过滤。

- [ ] **Step 3: 运行测试确认失败**

  Run: `./mvnw -pl store-admin-web -am -Dtest=OrderControllerTest,SecurityConfigTest -Dsurefire.failIfNoSpecifiedTests=false test`
  Expected: FAIL，原因是路由和模板尚不存在。

- [ ] **Step 4: 实现 Controller 与异常映射**

  列表用字符串接收 `page/pageSize` 后显式解析，区分负整数归一与格式/溢出 400；详情与发货采用 PRG，一次性消息通过 flash attributes 返回详情页。

- [ ] **Step 5: 实现订单列表与详情模板**

  列表保留筛选及页大小参数并提供首页、末页和相邻翻页；详情按状态决定支付区块与发货表单，发货前使用浏览器确认，不渲染任何编辑金额、明细或状态的控件。

- [ ] **Step 6: 增加后台导航并验证安全配置**

  在现有后台页面加入订单入口；确认 `/admin/orders/**` 继续由 `/admin/**` 认证规则、账号状态过滤和 CSRF 覆盖，无需新增放行规则。

- [ ] **Step 7: 运行任务测试**

  Run: `./mvnw -pl store-admin-web -am -Dtest=OrderControllerTest,SecurityConfigTest -Dsurefire.failIfNoSpecifiedTests=false test`
  Expected: PASS。

- [ ] **Step 8: 提交**

  ```bash
  git add store-admin-web
  git commit -m "feat: add admin order pages"
  ```

### Task 5: 全量回归与验收资料

**Files:**
- Create: `docs/verification/order-management-checklist.md`
- Modify: `README.md`

**Interfaces:**
- Consumes: 任务 1—4 的完整订单管理能力。
- Produces: 可重复执行的自动化与真实 MySQL/双端验收说明。

- [ ] **Step 1: 运行全量测试**

  Run: `./mvnw clean verify`
  Expected: 所有模块 BUILD SUCCESS，0 failures，0 errors。

- [ ] **Step 2: 修复回归并重复全量测试**

  若失败，仅修复本轮引入的问题；重复 `./mvnw clean verify` 直到通过，并记录最终测试数。

- [ ] **Step 3: 编写验收清单**

  清单包含：启动前提、订单筛选、分页边界、多明细快照、商品资料变化后快照不变、未支付展示、PAID 发货一次、重复发货保留首次时间、非 PAID 拒绝、两端状态一致性；未实际执行的真实 MySQL/双端项目项保持未勾选。

- [ ] **Step 4: 更新 README**

  记录订单管理已实现的路由和启动后验证入口，不写入数据库密码或虚构验收结果。

- [ ] **Step 5: 运行最终检查**

  Run: `git diff --check && ./mvnw clean verify && git status --short`
  Expected: 无 whitespace 错误；测试全绿；状态只包含计划内文档变化。

- [ ] **Step 6: 提交**

  ```bash
  git add README.md docs/verification/order-management-checklist.md
  git commit -m "docs: add order management verification"
  ```

- [ ] **Step 7: 最终审查与推送**

  对照设计逐项复核 SQL、状态迁移、页面权限和测试证据；修复审查发现的问题并再次 `./mvnw clean verify`。确认分支领先远端且工作区干净后推送 `main`。
