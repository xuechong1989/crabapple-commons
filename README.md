# crabapple

crabapple 是一套面向服务端开发的 Java 基础类库，沉淀了日常开发中高频使用的通用能力，包括：HTTP 客户端与代理、文件与目录处理、PDF/Word/Excel/图片等文档处理、加解密与摘要（AES/RSA/SM2/SM3/SM4/MD5/SHA/BCrypt/PBKDF2 等）、OCR（百度/合合/tesseract）、短信（腾讯云/阿里云/网关）、邮件、定时任务、数据库（MySQL/Oracle/达梦等）、Redis 缓存、Selenium/Playwright 浏览器自动化等。

## 模块说明

| 模块 | 说明 |
| --- | --- |
| crabapple-core | 核心类库（本仓库唯一模块） |

## 环境要求

- JDK 21
- Maven 3.9+

## 构建

```shell
mvn clean install
```

## 引入依赖

```xml
<dependency>
    <groupId>xc.crabapple</groupId>
    <artifactId>crabapple-core</artifactId>
    <version>21-4.2.00</version>
</dependency>
```

## 目录结构

```
crabapple-21-open
├── crabapple-core        核心模块
│   └── src/main/java/xc/crabapple
│       ├── core          通用工具（网络/文件/加解密/OCR/短信/邮件/定时/爬虫等）
│       └── ssm           数据库/缓存/中间件等集成支持
└── pom.xml               聚合与父 POM
```

## 使用说明

- 涉及第三方服务（短信、OCR、邮件等）的账号密钥在源码中均为占位符，需自行申请后配置。
- 部分工具类依赖本地软件或资源文件（如 tesseract 语言模型、chromedriver、7z、Playwright 驱动等），详见对应类的注释说明。
- `crabapple-core` 的依赖中包含第三方收费组件（如 spire.pdf），其许可与使用限制请遵循各自官方说明。
- 日志配置模板参见 `crabapple-core/src/main/java/xc/crabapple/core/log/` 目录下 `标准配置文件-logback.xml` 与 `标准配置文件-log4j2.xml`。

## 许可证

本项目基于 [Apache License 2.0](LICENSE) 开源。
