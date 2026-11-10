# 后台商品列表 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建 `/admin/products` 只读商品列表，支持关键字、状态筛选和每页 10 条的服务端分页。

**Architecture:** `ProductController` 接收 GET 参数并调用 `ProductQueryService`；Service 负责规范化查询参数，`ProductMapper` 使用 MyBatis-Plus 分页和分类联表查询；Thymeleaf 页面渲染筛选表单、表格及分页链接。

**Tech Stack:** Java 21、Spring Boot 3.5.16、Spring MVC、Thymeleaf、MyBatis-Plus 3.5.18、MySQL 9.7.1、JUnit 5

**Spec:** `docs/superpowers/specs/2026-11-10-product-list-design.md`

## Global Constraints

- 保持模块依赖方向 `admin-web → service → dao → model`。
- 数据库密码仅通过 `DB_PASSWORD` 环境变量提供。
- 使用 GET 整页刷新，不使用 AJAX。
- 每页固定 10 条，页码小于 1 时归一为 1。
- 本阶段不实现新增、编辑、上下架、补货或管理员登录。

## Review Focus

- `page` 未传或小于 1 时使用第 1 页；Task 2 单元测试覆盖。
- 关键字只有空白时按无关键字查询；Task 2 单元测试覆盖。
- 状态不是 `DRAFT` 或 `ON_SALE` 时按无状态筛选处理；Task 2 单元测试覆盖。
- 查询无结果时页面显示明确空状态而不是空表格；Task 3 Controller/视图测试覆盖。
- 分页链接必须保留关键字和状态筛选；Task 3 视图测试覆盖。

---

### Task 1: 商品分页数据访问

**Files:**
- Create: `store-model/src/main/java/org/csu/mypetstore/model/dto/ProductListItem.java`
- Create: `store-model/src/main/java/org/csu/mypetstore/model/query/ProductListQuery.java`
- Modify: `store-dao/src/main/java/org/csu/mypetstore/dao/ProductMapper.java`
- Create: `store-admin-web/src/main/java/org/csu/mypetstore/admin/config/MybatisPlusConfig.java`
- Modify: `store-admin-web/pom.xml`

**Interfaces:**
- Consumes: `product` 与 `category` 表。
- Produces: `IPage<ProductListItem> selectProductPage(Page<ProductListItem> page, String keyword, String status)`；`ProductListItem` 包含 `id`、`sku`、`name`、`categoryName`、`price`、`stock`、`status`。

- [ ] **Step 1: 编写 mapper 上下文加载失败测试**

新增一个仅检查 MyBatis-Plus 分页拦截器 Bean 和 Mapper 方法签名的测试；断言配置 Bean 存在、返回类型为分页结果。

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw -pl store-admin-web -am test -Dtest=ProductMapperContractTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，因为 DTO、查询对象和分页配置尚不存在。

- [ ] **Step 3: 实现分页 DTO、查询对象、联表 SQL 和分页拦截器**

`ProductMapper.selectProductPage(...)` 使用参数化动态 SQL：`keyword` 同时匹配 `product.name` 和 `product.sku`，`status` 精确匹配，按 `sort_order, id` 升序；配置 `PaginationInnerInterceptor(DbType.MYSQL)`，并加入 MyBatis-Plus JSQLParser 依赖。

- [ ] **Step 4: 运行 Task 1 测试**

Run: `./mvnw -pl store-admin-web -am test -Dtest=ProductMapperContractTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS。

- [ ] **Step 5: 提交 Task 1**

```bash
git add MyPetStore-admin/store-model MyPetStore-admin/store-dao MyPetStore-admin/store-admin-web/pom.xml MyPetStore-admin/store-admin-web/src/main/java/org/csu/mypetstore/admin/config
git commit -m "feat: add paged product query"
```

### Task 2: 商品查询业务层

**Files:**
- Create: `store-service/src/main/java/org/csu/mypetstore/service/ProductQueryService.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/impl/ProductQueryServiceImpl.java`
- Create: `store-service/src/test/java/org/csu/mypetstore/service/ProductListQueryTest.java`

**Interfaces:**
- Consumes: Task 1 的 `ProductListQuery` 与 `ProductMapper.selectProductPage(...)`。
- Produces: `IPage<ProductListItem> findProducts(ProductListQuery query)`；规范化后的查询对象提供 `normalizedPage()`、`normalizedKeyword()`、`normalizedStatus()`。

- [ ] **Step 1: 编写查询参数规范化失败测试**

覆盖默认页码、负页码、空白关键字、合法状态、非法状态五种输入，并断言页大小固定为 10。

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw -pl store-service -am test -Dtest=ProductListQueryTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，因为规范化方法尚未实现。

- [ ] **Step 3: 实现参数规范化与查询 Service**

`ProductQueryServiceImpl.findProducts(ProductListQuery query)` 创建 `Page<ProductListItem>(normalizedPage, 10)` 并调用 Task 1 Mapper；只接受 `DRAFT`、`ON_SALE` 两个状态。

- [ ] **Step 4: 运行 Task 2 测试**

Run: `./mvnw -pl store-service -am test -Dtest=ProductListQueryTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS。

- [ ] **Step 5: 提交 Task 2**

```bash
git add MyPetStore-admin/store-service MyPetStore-admin/store-model
git commit -m "feat: add product query service"
```

### Task 3: 商品列表 Controller 与页面

**Files:**
- Create: `store-admin-web/src/main/java/org/csu/mypetstore/admin/controller/ProductController.java`
- Create: `store-admin-web/src/main/resources/templates/product/list.html`
- Create: `store-admin-web/src/test/java/org/csu/mypetstore/admin/controller/ProductControllerTest.java`
- Modify: `store-admin-web/src/main/resources/templates/index.html`
- Modify: `README.md`

**Interfaces:**
- Consumes: Task 2 的 `ProductQueryService.findProducts(ProductListQuery)`。
- Produces: `GET /admin/products`，模型属性为 `page` 和 `query`，视图名为 `product/list`。

- [ ] **Step 1: 编写 Controller 失败测试**

使用普通 Java 测试替身，不使用 Mockito；断言 Controller 返回 `product/list`，模型含分页结果与原查询；分别覆盖有记录、无记录、筛选条件保留。

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw -pl store-admin-web -am test -Dtest=ProductControllerTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，因为 Controller 尚不存在。

- [ ] **Step 3: 实现 Controller 与 Thymeleaf 页面**

页面包含关键字输入框、状态下拉框、查询/清空入口、商品表格、空状态、总记录数、上一页/下一页；分页链接使用 Thymeleaf 参数表达式保留 `keyword` 和 `status`。

- [ ] **Step 4: 更新首页入口与 README**

首页添加“商品管理”链接；README 记录 `/admin/products` 地址、筛选参数和启动方式。

- [ ] **Step 5: 运行全量验证**

Run: `./mvnw clean verify`

Expected: 6 个模块均为 `SUCCESS`，所有测试 0 failures / 0 errors。

- [ ] **Step 6: 使用本机数据库进行冒烟测试**

Run: `DB_PASSWORD='<本机密码>' java -jar store-admin-web/target/store-admin-web-0.0.1-SNAPSHOT.jar`

Expected: `/admin/products` 显示演示商品；关键字、状态和翻页操作可用。密码不得进入命令历史、日志、提交或测试文件，实际执行时由用户在本地终端设置环境变量。

- [ ] **Step 7: 提交 Task 3**

```bash
git add MyPetStore-admin/store-admin-web MyPetStore-admin/README.md
git commit -m "feat: add admin product list"
```
