# 海棠公共库（Crabapple Commons）

[![CI](https://img.shields.io/github/actions/workflow/status/xuechong1989/crabapple-commons/ci.yml?branch=main&style=flat-square&label=CI&logo=githubactions&logoColor=white)](https://github.com/xuechong1989/crabapple-commons/actions/workflows/ci.yml)
![JDK](https://img.shields.io/badge/JDK-21-C9184A?style=flat-square&logo=openjdk&logoColor=white)
[![License](https://img.shields.io/badge/License-Apache--2.0-C9184A?style=flat-square&logo=apache&logoColor=white)](LICENSE)
[![Version](https://img.shields.io/badge/Version-21--4.2.00-C9184A?style=flat-square)](https://github.com/xuechong1989/crabapple-commons/releases)

**海棠公共库（Crabapple Commons）** 是面向服务端开发的 Java 21 通用工具库，沉淀文件、网络、加解密、OCR、短信、数据库/中间件集成等常用能力。

## 项目定位与适用范围

这是一个 **Java 类库（jar）**，不是可独立启动的服务：

- 适合作为 Java 21 / Spring Boot 服务端项目的 Maven 依赖；
- 适合复用文件、HTTP、加解密、分页、定时器、日志、短信、OCR 等基础能力；
- `crabapple-standardms`、`crabapple-sensitive` 都依赖本库；
- 不提供 Web 端口、进程入口或管理界面，不能通过 `java -jar` 启动；
- 部分工具依赖 Windows 路径、本地 Chrome/ChromeDriver、Tesseract、LibreOffice、MinIO、Redis 等外部资源，使用前请阅读对应类的注释。

## 环境要求

| 依赖 | 要求 |
|---|---|
| JDK | 21（`java.version=21`） |
| Maven | 3.9+ |
| 操作系统 | Linux / Windows；部分工具类有平台限制 |

## 构建与安装

### 只构建、跑单元测试

```bash
git clone https://github.com/xuechong1989/crabapple-commons.git
cd crabapple-commons
mvn clean verify
```

当前包含 45 个单元测试；CI 使用 JDK 21 + `mvn -B -ntp clean verify`。

### 安装到本机 Maven 仓库

`crabapple-standardms` / `crabapple-sensitive` 通过 Maven 坐标依赖本库。首次构建这两个服务前，需要先安装：

```bash
mvn clean install
```

如果只想快速安装、跳过测试：

```bash
mvn clean install -DskipTests
```

安装后坐标：

```xml
<dependency>
    <groupId>xc.crabapple</groupId>
    <artifactId>crabapple-commons</artifactId>
    <version>21-4.2.00</version>
</dependency>
```

> 本仓库没有配置 Maven Central 的 `distributionManagement`；默认使用本地 `~/.m2` 或你自己的私有 Nexus/Artifactory。

### 可选：PDF/A（Spire.PDF）

`src/main/spire/java` 下的 PDF/A 相关类默认不参与编译，避免默认构建依赖商业仓库。确认 Spire.PDF 授权条款后，可显式启用：

```bash
mvn clean verify -Ppdf-spire
```

未购买/未配置 Spire 授权时，不要在生产环境启用该 profile。

## 作为依赖使用

工具类以静态方法为主：

```java
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.collection.StringUtil;

public class Demo {
    public static void main(String[] args) {
        LogUtil.init("logback", "debug");
        LogUtil.print("demo_1", "hello crabapple");

        boolean blank = StringUtil.isBlank("");
        System.out.println(blank);
    }
}
```

日志实现默认只依赖 slf4j API；需要 logback 输出时，请在业务项目中显式加入 `logback-classic`，并放置 logback 配置。

## 功能范围

- 文件：文本、Office、PDF、图片、压缩包、音频、CAD、配置文件、水印、大文件
- 网络：HTTP/HTTPS、WebSocket、邮件、高可用与负载均衡
- 数据：JSON、XML、序列化
- 日志：slf4j/logback 封装与配置模板
- 加解密：AES、RSA、DES、SM2、SM3、SM4、MD5、SHA、CRC、BCrypt、PBKDF2、Base64
- OCR：百度 OCR、合合 OCR、Tesseract
- 短信：腾讯云、阿里云、网关短信
- 数据库/中间件：MySQL、Oracle、达梦、Redis、MongoDB、Elasticsearch、RocketMQ、LDAP 等
- 浏览器自动化：Selenium、Playwright
- 其他：分页、排序、正则、随机数、数学、时间、定时器、数据流、二维码、线程池、Shell、对象克隆、雪花算法

## MongoDB 分块文件存储

`xc.crabapple.ssm.fs.mongodb.MongoUtil` 是从 `cdk-cloudkd-08` 迁移的本地文件存储引擎，保留：

- 2MB 分块；
- `info` / `file` / `chunk0..14` 三组集合；
- MD5 秒传、断点续传；
- AES-256 分块加密；
- 8 线程并行下载；
- 瞬时 Mongo 异常重试；
- 索引自动创建；
- 本地文件 `writeByUrl` 上传。

只支持 **MongoDB Java Driver 5.x**，不再包含 mongo3 双驱动。

### 初始化

```java
import xc.crabapple.ssm.fs.mongodb.MongoConfig;
import xc.crabapple.ssm.fs.mongodb.MongoUtil;

MongoConfig config = new MongoConfig(
        "mongodb://user:password@127.0.0.1:27017/?authSource=admin",
        "cloudkd",
        "change-me-deployment-key");
MongoUtil.init(config);
```

也可以使用系统属性：

```bash
-Dcrabapple.mongo.uri=mongodb://user:password@127.0.0.1:27017/?authSource=admin
-Dcrabapple.mongo.database=cloudkd
-Dcrabapple.mongo.encrypt.key=change-me-deployment-key
```

### 上传/下载

```java
MongoUtil.write(inputStream, md5, fileSize, pos); // pos=-1 探测，0 从头，>0 续传
MongoUtil.exist(md5);
MongoUtil.readByHash(md5, outputStream);
MongoUtil.findByHash(md5);
MongoUtil.getSize();
MongoUtil.destroy();
```

`write` 的核心参数是 `InputStream`，不依赖 Spring `MultipartFile`；HTTP 服务层负责把 `MultipartFile` 转为流。

### 兼容性

- 旧 cloudkd 数据使用旧 AES/ECB 格式；读取旧数据时 `encryptionKey` 必须与旧部署密钥一致，否则无法解密。
- 新部署不要使用示例密钥，必须通过环境变量或配置中心注入。
- 旧 MongoDB 3.6 服务端需要先升级或迁移；Driver 5.x 不保证兼容 3.6。
- `cdk-cloudkd-08` 本身保持原样，不因本库迁移而修改。
- 当前沿用静态客户端和进程内 `FILE_LOCKS` 并发控制；**同一 Mongo 库建议只由一个应用实例写入**，多实例部署需要额外的分布式锁或事务方案。

## 可选依赖

本库采用“核心常驻、集成可选”的策略。以下能力需要业务项目自行引入依赖：

| 功能 | 需要自行引入 |
|---|---|
| 日志输出 | `logback-classic`、`logback-core` |
| Spring / MyBatis 集成 | `spring-boot-starter-web`、`spring-boot-starter-jdbc`、`mybatis`、`mybatis-spring` 等 |
| 浏览器自动化 | `selenium-java` + Guava、Playwright |
| 视频处理 | `javacv-platform`（含多平台原生库，体积大） |
| Word | `docx4j-core` 等 |
| PDF | `itextpdf`、`html2pdf`、`kernel`、`spire.pdf`（按类引入） |
| 文档转换 | `jodconverter-core` + 本地 LibreOffice |
| OCR | `tess4j` + 本地 Tesseract 语言模型 |
| 短信 | 腾讯云/阿里云/短信 SDK |
| 对象存储 | `minio` |
| MongoDB 分块文件存储 | `mongodb-driver-sync` 5.x（POM 中已标记 optional） |
| 数据库 | `mysql-connector-j`、`DmJdbcDriver18` 等 |
| 连接池/缓存 | `druid`、`redisson` |
| 内嵌容器 | `tomcat-embed-core` 等 |

## 配置与安全

- 第三方服务（短信、OCR、邮件等）的账号密钥在源码中均为占位符（`your-api-key`、`your-password`），必须由部署方替换。
- Redis 加密默认关闭；启用 `sm4` / `aes128` 时必须显式设置 `cipherKey`，代码中不提供默认密钥。
- `MinioUtil` 的文件加密需要设置系统属性：

```bash
-Dcrabapple.minio.sm4Key=<16位密钥>
-Dcrabapple.minio.sm4Iv=<16位IV>
```

未配置时加密上传/下载会直接报错，避免静默使用固定默认密钥。

- HTTPS 默认使用 JVM 信任库与标准主机名校验；`HttpsUtil.getTrustAllHttpClient()` 仅用于内网自签名/测试环境，使用前请评估中间人攻击风险。
- `Aes2Util` / `Aes3Util` 的 ECB 模式仅用于兼容历史数据；新系统建议使用 AES/GCM。
- DES 强度不足，仅建议对接遗留系统。
- 日志不会输出密钥、密码；部署侧密钥材料不要提交到 Git。

## 目录结构

```text
crabapple-commons
├── src/main/java/xc/crabapple
│   ├── core          通用工具（网络/文件/加解密/OCR/短信/邮件/定时/爬虫等）
│   └── ssm           数据库/缓存/中间件等集成支持
│       └── fs/mongodb  MongoDB 分块文件存储
├── src/main/spire/java   PDF/A 可选源码（-Ppdf-spire 时参与编译）
├── src/test/java     单元测试
└── pom.xml           单模块 POM
```

## 部署与升级

- 库本身没有“启动”步骤；部署方式是 `mvn install` 到本地仓库，或发布到私有 Maven 仓库后由业务项目依赖。
- 升级版本时修改业务项目中的 `<version>`，重新构建并跑业务回归；本库的单元测试不能替代业务集成测试。
- 如果使用 `crabapple-standardms` / `crabapple-sensitive`，先安装本库，再构建对应服务。

## 测试与 CI

```bash
mvn clean verify
```

CI 工作流见 [.github/workflows/ci.yml](.github/workflows/ci.yml)，使用 Temurin JDK 21、Maven 缓存和 `mvn -B -ntp clean verify`。

## 适用与不适用

**适合：**

- Java 21 服务端工具复用；
- 作为 `crabapple-standardms` / `crabapple-sensitive` 的基础库；
- 需要统一日志、HTTP、加解密、文件处理的服务。

**不适合：**

- 直接作为独立服务部署；
- 无 JDK/Maven 的环境；
- 需要开箱即用的云原生可观测性/配置中心/服务治理的平台（本库只提供基础工具）。

## 参与贡献与许可证

欢迎 Issue / Pull Request，提交前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)；安全问题请按 [SECURITY.md](SECURITY.md) 私下报告。

本项目采用 [Apache License 2.0](LICENSE)。第三方组件许可与署名见 [NOTICE](NOTICE)；其中 itextpdf 为 AGPL、Spire.PDF 为商业组件，使用对应可选功能前请自行确认授权。
