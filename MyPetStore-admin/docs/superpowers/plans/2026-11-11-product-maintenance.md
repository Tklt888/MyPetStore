# 后台商品维护与补货 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成管理端商品新增、资料编辑、上下架和增量补货，并用乐观锁保护共享库存与资料。

**Architecture:** 保留现有五模块和服务端渲染结构；扩充商品模型与显式 Mapper SQL，Service 统一校验并解释乐观锁结果，MVC 使用独立表单命令避免写回只读字段。所有写操作以提交的旧版本为条件并由数据库原子递增版本。

**Tech Stack:** Java 21、Spring Boot 3.5.16、Spring MVC、Spring Security、Thymeleaf、MyBatis-Plus 3.5.18、MySQL 8、JUnit 5、Mockito

**Spec:** `docs/superpowers/specs/2026-11-11-product-maintenance-design.md`

## Global Constraints

- 不修改课程数据库结构和演示数据；不提交真实数据库密码。
- 新增商品固定 `DRAFT`、版本 0；商品不删除，SKU 创建后不可变。
- 编辑资料不写库存、SKU 或状态；库存仅新增初始化或独立增量补货。
- 编辑、上下架、补货都以旧 `version` 为条件，成功时 `version = version + 1`。
- 商品只允许归属存在的二级分类；所有 POST 继续受管理员认证和 CSRF 保护。
- 只使用站内相对封面路径，不实现上传、相册、富文本、多规格或销量。

## Review Focus

- Unicode 空白、超长文本与价格小数位必须在写库前稳定拒绝，不依赖 MySQL 截断。
- 伪造一级、缺失或并发删除的分类 ID 必须拒绝，不能创建或更新非法归属。
- 旧编辑页、旧上下架按钮和旧补货页必须产生版本冲突，不能覆盖最新数据。
- 补货必须基于数据库最新库存原子相加，并在 `INT` 溢出前无副作用地失败。
- 数据库唯一键、约束和 SQL 异常不得把约束名、SQL 或堆栈显示给管理员。

---

### Task 1: 商品模型、筛选与显式数据访问

**Files:**
- Modify: `store-model/src/main/java/org/csu/mypetstore/model/Product.java`
- Modify: `store-model/src/main/java/org/csu/mypetstore/model/dto/ProductListItem.java`
- Modify: `store-model/src/main/java/org/csu/mypetstore/model/query/ProductListQuery.java`
- Create: `store-model/src/main/java/org/csu/mypetstore/model/dto/ProductEditView.java`
- Create: `store-model/src/main/java/org/csu/mypetstore/model/dto/ProductCategoryOption.java`
- Modify: `store-dao/src/main/java/org/csu/mypetstore/dao/ProductMapper.java`
- Test: `store-admin-web/src/test/java/org/csu/mypetstore/admin/ProductMapperContractTest.java`
- Test: `store-service/src/test/java/org/csu/mypetstore/service/ProductListQueryTest.java`

**Interfaces:**
- Consumes: 现有 `category`/`product` 表与 `ProductQueryService.findProducts(ProductListQuery)`。
- Produces: `ProductMapper.selectProductPage(Page, keyword, status, topCategoryId, categoryId)`、`selectDetailById(long)`、`selectCategoryOptions()`、`selectBySku(String)`、`insert(Product)`、`updateDetails(...)`、`updateStatus(...)`、`incrementStock(...)`；`ProductListQuery` 提供规范化分类筛选；`ProductEditView` 与 `ProductCategoryOption` 供后续任务使用。

- [ ] **Step 1: 写失败的 Mapper 契约和查询规范化测试**

测试锁定：列表 SQL 联接父分类并支持一级/二级分类筛选；资料更新 SQL 包含 `id/version`、递增版本且不包含 `stock/status/sku` 的 `SET`；状态与补货 SQL 都检查版本并递增，补货使用 `stock + quantity` 和溢出条件；关键字去空白，非法状态/分类页码稳定规范化。

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw -pl store-admin-web -am test -Dtest=ProductMapperContractTest,ProductListQueryTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，原因是新增 DTO、筛选字段或 Mapper 方法尚不存在。

- [ ] **Step 3: 扩充模型、查询对象和 Mapper**

`Product` 映射完整字段；`ProductListItem` 增加父分类、排序和版本；分类选项携带 `id/parentId/parentName/name`。显式 SQL 使用稳定排序 `p.sort_order, p.id`，固定每页 10 条；`incrementStock` 以 `stock <= 2147483647 - quantity` 防溢出。

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw -pl store-admin-web -am test -Dtest=ProductMapperContractTest,ProductListQueryTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，0 failures/errors。

- [ ] **Step 5: 提交**

```bash
git add store-model store-dao store-admin-web/src/test/java/org/csu/mypetstore/admin/ProductMapperContractTest.java store-service/src/test/java/org/csu/mypetstore/service/ProductListQueryTest.java
git commit -m "feat: add product maintenance data access"
```

### Task 2: 商品校验、乐观锁与库存业务

**Files:**
- Create: `store-model/src/main/java/org/csu/mypetstore/model/command/ProductCreateCommand.java`
- Create: `store-model/src/main/java/org/csu/mypetstore/model/command/ProductUpdateCommand.java`
- Create: `store-model/src/main/java/org/csu/mypetstore/model/command/ProductStatusCommand.java`
- Create: `store-model/src/main/java/org/csu/mypetstore/model/command/ProductRestockCommand.java`
- Modify: `store-service/src/main/java/org/csu/mypetstore/service/ProductService.java`
- Modify: `store-service/src/main/java/org/csu/mypetstore/service/impl/ProductServiceImpl.java`
- Modify: `store-service/src/main/java/org/csu/mypetstore/service/impl/ProductQueryServiceImpl.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/ProductNotFoundException.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/ProductValidationException.java`
- Create: `store-service/src/main/java/org/csu/mypetstore/service/ProductConflictException.java`
- Create: `store-service/src/test/java/org/csu/mypetstore/service/ProductServiceTest.java`

**Interfaces:**
- Consumes: Task 1 的 Mapper、DTO 与筛选接口。
- Produces: `listCategoryOptions()`、`getForEdit(long)`、`create(ProductCreateCommand)`、`update(long, ProductUpdateCommand)`、`changeStatus(long, ProductStatusCommand)`、`restock(long, ProductRestockCommand)`；业务异常分别表达字段校验、404 和版本冲突。

- [ ] **Step 1: 写失败的 Service 测试**

覆盖正常新增默认 `DRAFT/0`；Unicode 空白与长度；价格 `0`、三位小数和超过 `9999999999.99`；负库存/排序；绝对 URL、绝对路径、`..` 封面；一级/不存在分类；重复 SKU 与数据库竞争；编辑保留 SKU/库存/状态；零库存上架；旧版本三类写入均冲突；补货正整数、按最新值更新和溢出；完整性异常消息不含 `SQL`。

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw -pl store-service -am test -Dtest=ProductServiceTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，原因是命令、异常和业务方法尚不存在。

- [ ] **Step 3: 实现 Service 最小闭环**

写方法均加 `@Transactional`。字符串使用 `strip()`；价格用 `scale() <= 2` 与上限比较；分类必须查询到且 `parentId != null`；影响行数为 0 时重读商品区分 404 与冲突。`DataIntegrityViolationException` 转为安全业务消息；不捕获后继续成功。

- [ ] **Step 4: 运行 Service 测试确认通过**

Run: `./mvnw -pl store-service -am test -Dtest=ProductServiceTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，0 failures/errors。

- [ ] **Step 5: 运行服务模块回归测试**

Run: `./mvnw -pl store-service -am test`

Expected: 现有分类、登录和商品查询测试继续通过。

- [ ] **Step 6: 提交**

```bash
git add store-model/src/main/java/org/csu/mypetstore/model/command store-service
git commit -m "feat: add product maintenance rules"
```

### Task 3: 管理端商品表单与操作页面

**Files:**
- Modify: `store-admin-web/src/main/java/org/csu/mypetstore/admin/controller/ProductController.java`
- Create: `store-admin-web/src/main/java/org/csu/mypetstore/admin/controller/ProductExceptionHandler.java`
- Create: `store-admin-web/src/main/java/org/csu/mypetstore/admin/form/ProductCreateForm.java`
- Create: `store-admin-web/src/main/java/org/csu/mypetstore/admin/form/ProductUpdateForm.java`
- Create: `store-admin-web/src/main/java/org/csu/mypetstore/admin/form/ProductRestockForm.java`
- Modify: `store-admin-web/src/main/resources/templates/product/list.html`
- Create: `store-admin-web/src/main/resources/templates/product/form.html`
- Create: `store-admin-web/src/main/resources/templates/product/restock.html`
- Test: `store-admin-web/src/test/java/org/csu/mypetstore/admin/controller/ProductControllerTest.java`

**Interfaces:**
- Consumes: Task 2 的 ProductService 方法与业务异常。
- Produces: `/admin/products` 完整列表，新增/编辑/状态/补货 GET/POST 路由以及三张 Thymeleaf 页面。

- [ ] **Step 1: 写失败的 MVC 与模板测试**

覆盖新增页分类分组和默认封面/排序/库存；创建校验失败回显；编辑页 SKU/库存只读且隐藏版本；资料 POST 不接收库存、SKU、状态；状态 POST 携带目标状态和版本；补货只接受增加数量与版本；列表无删除按钮并含筛选、编辑、上下架、补货入口；越界空页有直接有效页入口；不存在 404；业务校验 400；未登录重定向且 POST 无 CSRF 为 403。

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw -pl store-admin-web -am test -Dtest=ProductControllerTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL，原因是表单、路由和模板入口尚不存在。

- [ ] **Step 3: 实现表单、Controller、异常映射与模板**

成功写操作使用 PRG 和一次性中文消息；失败时保留安全输入并重新装载分类选项或当前商品。`ProductConflictException` 在编辑/补货页显示 409 语义提示；状态冲突重定向列表并显示刷新提示。所有模板使用 Thymeleaf 默认转义。

- [ ] **Step 4: 运行 MVC 测试确认通过**

Run: `./mvnw -pl store-admin-web -am test -Dtest=ProductControllerTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS，0 failures/errors。

- [ ] **Step 5: 运行全量构建**

Run: `./mvnw clean verify`

Expected: Reactor 全部 SUCCESS，所有既有及新增测试 0 failures/errors。

- [ ] **Step 6: 提交**

```bash
git add store-admin-web
git commit -m "feat: add product maintenance pages"
```

### Task 4: 真实数据库验证说明与完成核对

**Files:**
- Modify: `README.md`
- Create: `docs/verification/product-maintenance-checklist.md`

**Interfaces:**
- Consumes: Task 3 的页面和路由。
- Produces: 不含密码的启动、操作和数据库前后核对步骤；明确延期的跨应用证据。

- [ ] **Step 1: 写验证清单与 README 说明**

清单覆盖：新增默认下架；重复 SKU、一级分类、非法价格/路径拒绝；编辑不改 SKU/库存；零库存可上架；上下架；补货正整数与库存/版本递增；旧编辑/状态/补货版本冲突；退出后不可访问。记录 `AC-25` 用户端可见与 `AC-28` 双应用并发延后，不能标为已通过。

- [ ] **Step 2: 核对文档中的命令与敏感信息**

Run: `rg -n "DB_PASSWORD=|password:|jdbc:mysql" README.md docs/verification/product-maintenance-checklist.md`

Expected: 不出现真实密码；只允许已有安全占位或环境变量名称。

- [ ] **Step 3: 最终全量验证**

Run: `./mvnw clean verify`

Expected: `BUILD SUCCESS`，所有模块 0 failures/errors。

- [ ] **Step 4: 提交**

```bash
git add README.md docs/verification/product-maintenance-checklist.md
git commit -m "docs: add product maintenance verification"
```

