# 管理端两级分类维护实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现管理端两级分类的分页树列表、新增、编辑和受限删除，完整满足 `FR-A-02` 的后台闭环。

**Architecture:** 在 `store-model` 定义分类实体、命令与页面 DTO，在 `store-dao` 用 MyBatis-Plus Mapper 完成一级分页、子分类和引用计数，在 `store-service` 集中实现规范化、层级、唯一性和删除保护，`store-admin-web` 只负责 MVC 绑定、错误映射与 Thymeleaf 页面。分类列表以一级分类为分页单位，每页完整装配其二级子树。

**Tech Stack:** Java 21、Spring Boot 3.5.16、Spring MVC、Spring Security、Thymeleaf、MyBatis-Plus 3.5.18、JUnit 5、Mockito、MySQL 8

**Spec:** `docs/superpowers/specs/2026-11-11-category-management-design.md`

## 全局约束

- 分类固定两级；商品只能归属二级分类，禁止第三级、循环和非法父级。
- `code` Unicode `strip()` 后长度 1—32、全局唯一、创建后不可修改。
- `name` Unicode `strip()` 后长度 1—50、同一父级唯一；不同一级下允许同名二级分类。
- `sortOrder` 为非负整数，展示按 `sort_order ASC, id ASC` 稳定排序。
- 分类列表按一级分类分页，每页固定 10 个一级分类并带出其完整直接子树，不整表加载。
- 编辑只允许修改名称与排序；父分类和编码没有写入接口。
- 有子分类或任意商品引用（含下架商品）时拒绝删除，不级联修改数据。
- 所有写操作为 POST 并使用 Spring Security CSRF；不存在分类返回 404，非法页码格式返回 400。
- 不新增分类类型字段，不用名称或编码后缀强制 `PETS/SUPPLIES`；后续用户端按 `categoryId` 使用新增分类。
- 不读取、记录或提交真实 `DB_PASSWORD`。

## 审查重点

- 输入仅含全角空格等 Unicode 空白时，应在访问数据库前拒绝。
- `parentId` 指向二级分类时必须拒绝创建第三级，数据库不发生写入。
- 并发创建重复编码或同级重名触发唯一索引时，页面只能看到安全业务提示，不能看到 SQL。
- 删除检查与实际删除之间出现新子分类或商品引用时，数据库外键兜底且事务不能误报成功。
- 最后一页删除后、页码超过末页或本页为空时，列表保持稳定、提供返回有效页入口且不加载全表。

---

### 任务 1：分类模型与分页数据访问

**文件：**
- 新建：`store-model/src/main/java/org/csu/mypetstore/model/Category.java`
- 新建：`store-model/src/main/java/org/csu/mypetstore/model/dto/CategoryRow.java`
- 新建：`store-model/src/main/java/org/csu/mypetstore/model/dto/CategoryTreeItem.java`
- 新建：`store-model/src/main/java/org/csu/mypetstore/model/query/CategoryListQuery.java`
- 新建：`store-dao/src/main/java/org/csu/mypetstore/dao/CategoryMapper.java`
- 新建：`store-admin-web/src/test/java/org/csu/mypetstore/admin/CategoryMapperContractTest.java`

**接口：**
- 消费：数据库表 `category(id, code, parent_id, parent_key, name, sort_order, created_at, updated_at)` 与 `product.category_id`。
- 产出：`CategoryMapper extends BaseMapper<Category>`；`IPage<CategoryRow> selectTopLevelPage(Page<CategoryRow> page)`；`List<CategoryRow> selectChildrenByParentIds(List<Long> parentIds)`；`long countChildren(Long categoryId)`；`long countProductReferences(Long categoryId)`。
- 产出：`CategoryListQuery#getPage()` 把缺省或小于 1 归一为 1，固定 `PAGE_SIZE = 10`；格式错误交给 MVC 返回 400。

- [ ] **步骤 1：编写失败的 Mapper 合约测试**

在 `CategoryMapperContractTest` 断言：实体映射表名为 `category`，字段不包含生成列 `parent_key`；Mapper 方法签名存在；一级分页条件为 `parent_id IS NULL`；子分类只按本页 `parent_id IN (...)` 查询；两者都按 `sort_order, id` 升序；引用计数覆盖 `category.parent_id` 和 `product.category_id`。

- [ ] **步骤 2：运行测试确认失败**

运行：`./mvnw -pl store-admin-web -am test -Dtest=CategoryMapperContractTest -Dsurefire.failIfNoSpecifiedTests=false`

预期：因 `Category`、DTO、Query 或 `CategoryMapper` 不存在而编译失败。

- [ ] **步骤 3：实现分类实体、DTO、分页 Query 与 Mapper**

`Category` 仅映射可写列，时间字段只读使用；`CategoryRow` 提供 `id/code/parentId/name/sortOrder`；`CategoryTreeItem` 提供父节点字段与不可变 `List<CategoryRow> children`；Mapper 使用带显式 SQL 的 `@Select`，一级总数由 MyBatis-Plus 分页插件计算，空 `parentIds` 时由 Service 跳过子查询。

- [ ] **步骤 4：运行测试确认通过**

运行：`./mvnw -pl store-admin-web -am test -Dtest=CategoryMapperContractTest -Dsurefire.failIfNoSpecifiedTests=false`

预期：测试通过，且现有 `ProductMapperContractTest` 继续通过。

- [ ] **步骤 5：提交**

```bash
git add store-model store-dao store-admin-web/src/test/java/org/csu/mypetstore/admin/CategoryMapperContractTest.java
git commit -m "feat: add category data access"
```

### 任务 2：分类业务规则与事务写入

**文件：**
- 新建：`store-model/src/main/java/org/csu/mypetstore/model/command/CategoryCreateCommand.java`
- 新建：`store-model/src/main/java/org/csu/mypetstore/model/command/CategoryUpdateCommand.java`
- 新建：`store-model/src/main/java/org/csu/mypetstore/model/dto/CategoryEditView.java`
- 新建：`store-service/src/main/java/org/csu/mypetstore/service/CategoryService.java`
- 新建：`store-service/src/main/java/org/csu/mypetstore/service/CategoryNotFoundException.java`
- 新建：`store-service/src/main/java/org/csu/mypetstore/service/CategoryValidationException.java`
- 新建：`store-service/src/main/java/org/csu/mypetstore/service/impl/CategoryServiceImpl.java`
- 新建：`store-service/src/test/java/org/csu/mypetstore/service/CategoryServiceTest.java`

**接口：**
- 消费：任务 1 的 `CategoryMapper`、`CategoryRow`、`CategoryTreeItem`、`CategoryListQuery`。
- 产出：`IPage<CategoryTreeItem> listTree(CategoryListQuery query)`；`List<CategoryRow> listTopLevelOptions()`；`CategoryEditView getForEdit(long id)`；`long create(CategoryCreateCommand command)`；`void update(long id, CategoryUpdateCommand command)`；`void delete(long id)`。
- 产出：`CategoryValidationException#getField()` 返回可选字段名（`code`、`name`、`sortOrder`、`parentId` 或空），`getMessage()` 返回安全中文消息；`CategoryNotFoundException` 保存目标 ID。

- [ ] **步骤 1：编写失败的规范化与创建测试**

在 `CategoryServiceTest` 覆盖：合法一级和二级分类会以 `strip()` 后值插入；null、Unicode 纯空白、code 33 字符、name 51 字符、负排序在任何写查询前拒绝；二级父级不存在或父级自身已有 `parentId` 时拒绝且不 insert；重复 code 和同父级 name 返回对应字段错误。

- [ ] **步骤 2：运行测试确认失败**

运行：`./mvnw -pl store-service -am test -Dtest=CategoryServiceTest -Dsurefire.failIfNoSpecifiedTests=false`

预期：因命令、异常和 Service 不存在而编译失败。

- [ ] **步骤 3：实现创建、字段校验与唯一性接口**

为 Mapper 补充 `selectByCode(String code)` 和 `selectByParentAndName(Long parentId, String name)`；Service 的规范化统一使用 `strip()`，一级用 null 父级，二级必须读取并确认父节点是一级；`create` 使用 `@Transactional` 并把 `DuplicateKeyException`/`DataIntegrityViolationException` 映射为不含 SQL 的业务消息。

- [ ] **步骤 4：运行创建测试确认通过**

运行：同步骤 2。

预期：规范化与创建用例通过。

- [ ] **步骤 5：编写失败的列表、编辑与删除测试**

覆盖：`listTree` 只对当前 10 个一级 ID 查询子节点并保持稳定顺序；空页不查子分类；`update` 只写 name/sortOrder 且保留 code/parentId；同级重名拒绝但自身原名可保存；不存在返回 `CategoryNotFoundException`；有子分类或商品引用时删除拒绝；合法删除调用一次 `deleteById`；数据库完整性竞争不会被当作成功。

- [ ] **步骤 6：运行新增测试确认失败**

运行：同步骤 2。

预期：缺少列表组装、更新或删除逻辑导致失败。

- [ ] **步骤 7：实现列表、编辑与受限删除**

`listTree` 使用任务 1 的分页结果和单次批量子查询组装新分页对象；`update` 与 `delete` 使用 `@Transactional` 并在写入前重读；更新实体只设置 `id/name/sortOrder` 并使用显式 Mapper 更新 SQL，确保 code/parentId 不进入 `SET`；删除先检查子分类再检查商品引用，最后依赖外键兜底。

- [ ] **步骤 8：运行 Service 与模块测试确认通过**

运行：`./mvnw -pl store-service -am test -Dtest=CategoryServiceTest -Dsurefire.failIfNoSpecifiedTests=false`

预期：所有分类 Service 测试通过，`store-service` 现有测试无回归。

- [ ] **步骤 9：提交**

```bash
git add store-model store-dao store-service
git commit -m "feat: add category business rules"
```

### 任务 3：分类 MVC、表单与错误页面行为

**文件：**
- 新建：`store-admin-web/src/main/java/org/csu/mypetstore/admin/controller/CategoryController.java`
- 新建：`store-admin-web/src/main/java/org/csu/mypetstore/admin/controller/CategoryExceptionHandler.java`
- 新建：`store-admin-web/src/main/java/org/csu/mypetstore/admin/form/CategoryCreateForm.java`
- 新建：`store-admin-web/src/main/java/org/csu/mypetstore/admin/form/CategoryUpdateForm.java`
- 新建：`store-admin-web/src/main/resources/templates/category/list.html`
- 新建：`store-admin-web/src/main/resources/templates/category/form.html`
- 新建：`store-admin-web/src/main/resources/templates/error/400.html`
- 新建：`store-admin-web/src/main/resources/templates/error/404.html`
- 新建：`store-admin-web/src/test/java/org/csu/mypetstore/admin/controller/CategoryControllerTest.java`
- 修改：`store-admin-web/src/main/resources/templates/index.html`

**接口：**
- 消费：任务 2 的 `CategoryService` 六个方法、两个命令、`CategoryValidationException` 与 `CategoryNotFoundException`。
- 产出：`GET /admin/categories`、`GET /admin/categories/new`、`POST /admin/categories`、`GET /admin/categories/{id}/edit`、`POST /admin/categories/{id}`、`POST /admin/categories/{id}/delete`。
- 产出：创建成功消息“分类已创建”，更新成功消息“分类已更新”，删除成功消息“分类已删除”；受限删除的安全原因通过 redirect flash 显示。

- [ ] **步骤 1：编写失败的 Controller 路由测试**

用直接 Controller 测试和轻量 MockMvc 覆盖：列表把分页树和 query 放入模型；新增页按可选 `parentId` 区分一级/二级并提供父级选项；创建和更新验证失败返回 `category/form` 并保留输入；成功使用 PRG 重定向；不存在编辑返回 404；删除业务拒绝重定向并显示原因。

- [ ] **步骤 2：运行测试确认失败**

运行：`./mvnw -pl store-admin-web -am test -Dtest=CategoryControllerTest -Dsurefire.failIfNoSpecifiedTests=false`

预期：因 Controller、表单或异常处理器不存在而编译失败。

- [ ] **步骤 3：实现 MVC 绑定与异常映射**

表单使用 Jakarta Validation 声明长度和非负约束，但 Service 仍重复执行全部业务校验；Controller 不接收编辑 code/parentId；`CategoryExceptionHandler` 将格式错误映射为 400、`CategoryNotFoundException` 映射为 404，技术异常不输出内部信息。

- [ ] **步骤 4：运行 Controller 测试确认通过**

运行：同步骤 2。

预期：路由、状态码、PRG 和模型断言通过。

- [ ] **步骤 5：编写失败的模板与安全测试**

在 `CategoryControllerTest` 渲染模板并断言：列表显示父子缩进、编码、名称、排序、分页、空状态、新增/编辑入口；新增和删除表单均为 POST 且包含 `_csrf`；编辑页面只读显示 code 和父分类，不存在可提交的 code/parentId 字段；删除按钮带二次确认；输入和消息由 Thymeleaf 转义。扩展 `SecurityConfigTest` 断言未登录访问分类页会重定向登录、无 CSRF 的删除 POST 返回 403。

- [ ] **步骤 6：运行模板与安全测试确认失败**

运行：`./mvnw -pl store-admin-web -am test -Dtest=CategoryControllerTest,SecurityConfigTest -Dsurefire.failIfNoSpecifiedTests=false`

预期：模板不存在或断言失败。

- [ ] **步骤 7：实现分类列表、共用表单和首页入口**

页面沿用现有管理端简洁桌面布局；一级行与二级行视觉区分；一级行提供“新增子分类”；分页以一级总数计算；空页提供返回第一页；所有写表单输出 Thymeleaf CSRF hidden input；首页增加“分类管理”导航。

- [ ] **步骤 8：运行 Web 测试与完整构建**

运行：`./mvnw clean verify`

预期：所有模块 `BUILD SUCCESS`，分类新增测试与现有登录、商品列表测试全部通过。

- [ ] **步骤 9：提交**

```bash
git add store-admin-web
git commit -m "feat: add category management pages"
```

### 任务 4：运行说明与真实数据库冒烟准备

**文件：**
- 修改：`README.md`
- 新建：`docs/verification/category-management-checklist.md`

**接口：**
- 消费：任务 3 的全部路由和页面。
- 产出：不含数据库密码的本机验证步骤，记录 `FR-A-02` 当前可验证项和延后跨端项。

- [ ] **步骤 1：编写验收清单**

清单覆盖：登录管理员；分页查看两级树；创建一级和二级；重复 code、同级重名、负排序、三级父级被拒绝；编辑只改名称/排序；有子分类和有商品引用删除被拒绝；创建临时无引用分类后可删除；退出后分类页不可访问。明确 `AC-25` 用户端可选与商品上架联动延后。

- [ ] **步骤 2：更新 README**

补充分类管理地址、能力范围、构建命令，以及使用用户自己设置的 `DB_PASSWORD` 启动后执行清单；不得写入真实密码。

- [ ] **步骤 3：运行最终验证**

运行：`./mvnw clean verify`

预期：`BUILD SUCCESS` 且 0 failures / 0 errors。

- [ ] **步骤 4：检查提交内容**

运行：`git diff --check && git status --short`

预期：无空白错误；仅包含计划内 README、验收清单及实现文件，不包含密码、本机配置或构建产物。

- [ ] **步骤 5：提交**

```bash
git add README.md docs/verification/category-management-checklist.md
git commit -m "docs: add category verification guide"
```
