# 管理端项目初始化设计

## 意图

为实验一建立可持续开发的商城后台 CMS 工程骨架，使后续分类、商品、库存和订单功能能够按清晰分层逐步实现，并留下可复现的 Maven 构建与 Git 记录。

## 目标

- 在仓库内创建根工程 `MyPetStore-admin`。
- 使用 Maven 聚合 `store-common`、`store-model`、`store-dao`、`store-service` 和 `store-admin-web` 五个模块。
- 建立单向依赖：`admin-web -> service -> dao -> model`，通用模块由需要它的下游模块引用。
- 提供运行在 8081 端口的最小 Spring Boot 管理端应用和健康首页。
- 使用 Maven Wrapper 保证没有全局 Maven 的机器也能构建。

## 非目标

- 本阶段不实现登录、分类、商品、库存或订单业务。
- 不创建 REST API、Vue、React 或前后端分离工程。
- 不在仓库中保存真实数据库密码。

## 约束

- Java 21。
- Spring Boot 3.5.16。
- MyBatis-Plus 3.5.18 的 Spring Boot 3 starter。
- Spring MVC、Thymeleaf、MySQL，服务端渲染。
- 基础包名 `org.csu.mypetstore`。
- 管理端默认端口 8081。

## 方案比较

1. 单模块工程：初始化最简单，但与老师展示的五层模块结构不一致，后续边界容易混乱。
2. 五模块 Maven 聚合工程：与课程示例一致，依赖关系清楚，适合逐步实现后台业务。本项目采用该方案。
3. 用户端和管理端一起做成一个更大的聚合工程：共享方便，但与 PRD 的两个独立工程要求不符，本阶段不采用。

## 验证标准

- Maven Reactor 能识别并构建全部五个模块。
- 自动化测试能加载管理端 Spring 上下文并验证首页路由。
- 打包后生成可运行的 `store-admin-web` Jar。
- 应用在测试配置下不依赖本机 MySQL 即可完成骨架验证；真实数据库连接留到数据库接入阶段。

