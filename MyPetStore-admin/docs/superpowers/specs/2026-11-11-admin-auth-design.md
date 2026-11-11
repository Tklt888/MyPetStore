# 管理员登录与权限设计

## 目的

为 MyPetStore 管理端增加真实数据库管理员登录，使首页、商品列表及后续后台功能只能由启用状态的 `ADMIN` 访问，并满足 PRD 对独立 Session、统一失败提示、POST 退出和 CSRF 防护的要求。

## 目标

- 从 `store_user` 表读取管理员账号，不使用内存或写死账号。
- 仅允许 `role=ADMIN` 且 `status=ENABLED` 的账号登录。
- 使用 BCrypt 校验 `password_hash`，密码不回显、不写日志。
- 提供 `/login` 登录页；登录成功跳转 `/`。
- 除 `/login` 外，管理端页面默认都需要认证。
- Session Cookie 名称为 `PETSTORE_ADMIN_SESSION`，与未来用户端隔离。
- 退出使用带 CSRF 令牌的 `POST /logout`，成功后返回登录页。
- 已登录管理员若后来被禁用或改为非管理员，下一次受保护请求立即清除会话并拒绝访问。

## 非目标

- 不实现注册、找回密码、记住登录、验证码或第三方登录。
- 不实现用户端 `CUSTOMER` 登录。
- 不实现管理员账号的新增、编辑或改密页面。
- 不提供 JSON 登录 API。

## 约束

- 保持模块依赖方向 `admin-web → service → dao → model`。
- 使用现有 `store_user` 表及演示账号；教学管理员为 `admin`，密码由已有 BCrypt 散列验证。
- 用户名去除首尾空白并限制为 50 字符；密码按原样校验，不去除任何空格。
- 不向页面、日志或异常消息输出密码散列。
- 登录失败统一显示“账号或密码错误，或账号不可用”，不区分账号不存在、密码错误、角色不符或禁用。

## 方案比较

### 采用：Spring Security

Spring Security 负责认证上下文、Session 固定攻击防护、请求授权、CSRF 和 POST 退出。自定义 `UserDetailsService` 从数据库读取账号，自定义请求过滤器在每次受保护请求时重新确认角色和状态。优点是安全规则集中且符合 PRD；代价是需要增加 Security 配置和少量适配代码。

### 未采用：自定义 MVC 拦截器

自行在 Controller 中处理登录并用拦截器检查 Session，概念直接，但必须重复实现 CSRF、Session 更新、退出和统一授权规则，遗漏风险更高。

## 数据与模块设计

### `store-model`

新增 `StoreUser`，映射 `store_user` 的 `id`、`username`、`passwordHash`、`displayName`、`role`、`status`。认证流程只使用这些字段。

### `store-dao`

新增 `StoreUserMapper`，按用户名查询单个账号。查询使用绑定参数；用户名唯一约束保证最多一条记录。

### `store-service`

新增只读认证查询接口：

- `AdminAccount findLoginAccount(String username)`：返回认证所需账号信息；不存在时返回空。
- `boolean isActiveAdmin(Long userId)`：每次受保护请求确认账号仍为 `ADMIN + ENABLED`。

Service 不依赖 HTTP、Session 或页面对象。

### `store-admin-web`

- `AdminUserDetailsService` 将数据库账号转换为 Spring Security `UserDetails`；非管理员或禁用账号按不可用处理。
- `SecurityConfig` 公开 `/login`，保护其余路径，配置表单登录、统一失败处理、POST 退出和 BCrypt 编码器。
- `AdminAccountStatusFilter` 对已认证请求按用户 ID 重新检查数据库；失效时清除认证、使 Session 失效并跳转 `/login?expired`。
- `LoginController` 渲染登录页；失败处理只暂存安全的用户名用于回显，密码永不保存。
- `login.html` 包含用户名、密码、CSRF 隐藏字段、统一错误提示。
- 首页显示当前管理员名称及 POST 退出表单。

## 请求流程

### 登录

1. 未登录访问受保护页面时跳转 `/login`。
2. 用户提交 `POST /login`。
3. Spring Security 调用数据库 `AdminUserDetailsService`。
4. BCrypt 校验成功且账号为启用管理员时更新 Session 标识并跳转 `/`。
5. 任一验证失败时返回 `/login?error`，显示统一消息；可回显用户名，不回显密码。

### 后续请求

1. Spring Security 从 `PETSTORE_ADMIN_SESSION` 恢复认证。
2. `AdminAccountStatusFilter` 查询当前账号状态与角色。
3. 仍为启用管理员时继续访问；否则清除并失效当前管理端 Session，跳转登录页。

### 退出

1. 首页提交带 CSRF 令牌的 `POST /logout`。
2. Spring Security 清除认证、失效 Session 并删除管理端 Cookie。
3. 跳转 `/login?logout`。

## 错误与安全处理

- 登录失败不说明账号是否存在，避免账号枚举。
- 认证查询异常不向浏览器暴露 SQL、堆栈或散列。
- CSRF 保持开启；GET 不执行退出或其他状态变更。
- 登录页不展示默认密码提示，README 可单独说明教学演示账号。
- 登录后 Session ID 由 Spring Security 默认迁移。

## 测试与验收

- 正确的 `admin / PetStore123!` 可以登录。
- 错误密码、`CUSTOMER`、`DISABLED`、不存在账号得到相同失败结果。
- 未登录访问 `/` 或 `/admin/products` 被引导到 `/login`。
- 登录后可访问首页和商品列表。
- `GET /logout` 不执行退出；带 CSRF 的 `POST /logout` 成功。
- 已登录管理员在数据库被禁用后，下一次请求被拒绝。
- Session Cookie 名称为 `PETSTORE_ADMIN_SESSION`。
- 全量 Maven 构建和测试通过；最终由用户使用本地数据库完成真实登录冒烟测试。

## 留给实现者的选择

- 登录页的具体视觉样式和文案层级可以在不改变上述行为的前提下自行确定。
- 认证失败用户名暂存可以使用一次性 Session 属性，但必须在渲染后删除。
