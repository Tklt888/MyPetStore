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

首次访问会跳转到登录页 <http://localhost:8081/login>。实验演示管理员账号为 `admin`，
初始密码为 `PetStore123!`。登录会话使用独立 Cookie `PETSTORE_ADMIN_SESSION`；退出必须通过页面中的 POST 表单完成，以保留 CSRF 防护。

商品管理列表地址为 <http://localhost:8081/admin/products>，支持以下 GET 参数：

- `keyword`：按商品名称或 SKU 模糊查询
- `status`：按 `ON_SALE` 或 `DRAFT` 筛选
- `page`：页码，每页固定 10 条

分类管理地址为 <http://localhost:8081/admin/categories>，提供以下能力：

- 按一级分类分页展示完整的两级分类树，每页 10 个一级分类
- 新增一级分类，或在现有一级分类下新增二级分类
- 编辑分类名称与排序；编码和父分类创建后不可修改
- 删除无子分类且未被任何商品引用的分类
- 拒绝重复编码、同级重名、第三级分类、负数排序及受引用删除

启动后可按照 [分类管理验收清单](docs/verification/category-management-checklist.md) 完成浏览器验证。清单只要求使用你自己在终端设置的 `DB_PASSWORD`，不得把真实密码写入项目文件、截图或 Git 提交。

应用默认连接本机 `MyPetStore` 数据库，用户名默认为 `root`，也可通过 `DB_USERNAME` 修改。真实用户名和密码只通过环境变量提供，不得提交到 Git。
