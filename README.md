# crabapple（海棠果）

crabapple（中文名“海棠果”）是一套面向服务端开发的通用 Java 类库，自 2016 年起持续迭代（前身先后为 yczq4j、snow4j、snowj）。开发初衷：减少冗余、行化使用、统一规范、方案积累。本仓库为其基础核心模块 `crabapple-core` 的开源版本。

`crabapple-core` 沉淀了服务端日常开发中高频使用的通用能力：

- 文件处理：HTML / 文本 / Office / PDF / 图片 / 压缩包 / 音频 / CAD / 配置文件 / 水印处理 / 超大文件
- 网络：HTTP / HTTPS / WebSocket / 邮件 / 内置高可用与负载均衡算法
- 数据传输：JSON / XML / 序列化
- 日志：基于 logback 的统一日志与跟踪（另附 log4j2 标准配置模板）
- 加密与摘要：AES / RSA / DES / SM2 / SM3 / SM4 / MD5 / SHA / CRC / BCrypt / PBKDF2 / Base64
- OCR：百度 OCR / 合合 OCR / tesseract
- 短信：腾讯云 / 阿里云 / 网关短信
- 数据库与中间件：MySQL / Oracle / 达梦 / Redis / MongoDB / ElasticSearch / RocketMQ / LDAP 等
- 浏览器自动化：Selenium / Playwright
- 其它：分页 / 排序 / 正则表达式 / 随机数 / 数学 / 时间管理 / 定时器 / 数据流 / 二维码 / 多线程与线程池 / Shell 调用 / 对象克隆 / 雪花算法 等

## 设计理念

- 尽量仅依赖 JDK 底层，保持稳定的底层依赖；
- 脚本化调用，追求行化（一行完成常见任务），兼顾功能与易用；
- 方便且统一的底层 API 调用模式；
- 统一的规范与标准（统一的数据对象模型与标准接口数据格式）；
- 性能优化及防止误写（如各类空值安全处理）。

## 版本说明

自 v11.0.0 起，大版本号与所基于的 JDK 版本保持一致，并按 JDK 版本维护不同版本线（`crabapple-08` / `crabapple-21`）。本仓库为 JDK 21 版本线（`crabapple-21`），当前版本 `21-4.2.00`。

## 历史沿革

| 时间 | 里程碑 |
| --- | --- |
| 2016-11 | 项目立项（原名 yczq4j） |
| 2018-12 | 更名为 snow4j，开始拆分并各自独立发布 |
| 2019-12 | 依赖由 JDK 8 升级至 JDK 11，开始集成 Spring Boot |
| 2020-08 | 更名为 snowj |
| 2021-06 | 统一更名为 crabapple（海棠果），整库项目拆分并各自维护 |
| 2022-01 | v8.0.0：模块化，Maven 统一集中管理 |
| 2022-05 | v11.0.0：大版本号与 JDK 版本保持一致 |
| 2024-03 | 按 JDK 版本分别发布 crabapple-08 / crabapple-21 版本线 |

## 模块说明

crabapple 为多模块工程（spring / web / file-* / redis / ocr 等模块），本仓库开源的是其基础核心模块：

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

## 可选依赖说明

`crabapple-core` 采用“核心常驻、集成可选”的依赖策略：JSON / HTTP / 文件 / 加解密等基础能力保留为默认依赖；涉及第三方服务或框架集成的依赖均标记 `optional`，不向下传递，使用对应功能时需自行引入：

| 功能 | 需自行引入的依赖 | 说明 |
| --- | --- | --- |
| 日志输出 | logback-classic、logback-core | 默认只含 slf4j API，不强制日志实现；配置模板见下文 |
| Spring / MyBatis 集成 | spring-boot-starter-web、spring-boot-starter-jdbc、mybatis、mybatis-spring 等 | `xc.crabapple.ssm` 下的集成类使用 |
| 浏览器自动化 | selenium-java + guava、playwright | Selenium 需本地 driver；Playwright 需 Chromium（驱动首次运行自动下载） |
| 视频处理 | javacv-platform | 仅 `VideoUtil` 使用，含多平台原生库，体积较大 |
| Word 文档 | docx4j-core 等 | docx 的读写与转换 |
| PDF 处理 | itextpdf、html2pdf、kernel、spire.pdf | 按工具类需要引入；spire.pdf 为商业组件，遵循其官方授权 |
| 文档转换 | jodconverter-core | 需本地安装 LibreOffice |
| OCR | tess4j | 需本地安装 tesseract 及语言模型 |
| 短信 | tencentcloud-sdk-java-sms、dysmsapi20170525、qcloudsms、okhttp | 按短信通道引入 |
| 对象存储 | minio | |
| 数据库 | mysql-connector-j、DmJdbcDriver18 | 按数据库类型引入驱动 |
| 连接池 / 缓存 | druid、redisson | |
| 内嵌容器 | tomcat-embed-core 等 | 内嵌 Web 场景 |

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
- 部分工具类依赖本地软件或资源文件（如 tesseract 语言模型、chromedriver、7z 等），详见对应类的注释说明；Playwright 的驱动首次运行会自动下载，离线环境可参考官方文档预置。
- `crabapple-core` 的依赖中包含第三方收费组件（如 spire.pdf），其许可与使用限制请遵循各自官方说明。
- 日志配置模板参见 `crabapple-core/src/main/java/xc/crabapple/core/log/` 目录下 `标准配置文件-logback.xml` 与 `标准配置文件-log4j2.xml`。

## 安全说明

- HTTPS 请求默认使用 JVM 信任库与标准主机名校验；仅当明确需要（内网自签名证书、测试环境）时才使用 `HttpsUtil.getTrustAllHttpClient()` / `getTrustAllSSLSocketFactory()` 跳过证书校验，并自行评估中间人攻击风险。
- 加密工具类中 ECB 模式（`Aes2Util.encrypt/decrypt`、`Aes3Util`）仅为兼容历史数据保留，新系统建议使用 `Aes2Util.encryptGcm/decryptGcm`（AES/GCM 随机 IV + 完整性校验）。
- DES 密钥仅 56 位，强度不足，仅建议用于对接遗留系统，新系统请使用 AES。
- 工具类不会将密钥、密码输出到日志。

## 许可证

本项目基于 [Apache License 2.0](LICENSE) 开源。
