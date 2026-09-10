# coolkid-rss-webflux

基于 Spring WebFlux 的 RSS 订阅、内容过滤和自动下载服务。项目负责抓取 RSS/Atom Feed、保存订阅记录、按规则筛选种子链接，并将任务提交到 qBittorrent 或 Transmission。

当前仓库只包含后端服务，不包含 Web 前端，也没有内置用户认证层。接口默认挂载在 `/coolkid-rss` 下。

## 镜像与相关链接

- 容器镜像及部署配置：[coolkid-rss-container](https://github.com/Coolkids/coolkid-rss-container)

## 功能

- RSS/Atom 订阅源管理、手动刷新和排序
- Feed 内容定时更新，未读/收藏状态管理
- 普通关键词、排除关键词、`|` 或条件和正则表达式过滤
- 下载器配置、单条记录下载和自动下载日志
- 支持 qBittorrent 与 Transmission
- MongoDB Reactive 持久化
- Redis 缓存、分布式锁和 RSS 更新队列
- 定时清理历史 RSS 记录和下载日志
- 使用 Snowflake 算法生成 Feed、规则、下载器和日志 ID

## 技术栈

| 组件 | 版本/说明 |
| --- | --- |
| Java | 21 |
| Spring Boot | 3.5.16 |
| Spring WebFlux | 响应式 HTTP API |
| MongoDB | Spring Data MongoDB Reactive |
| Redis | Reactive Redis、缓存和分布式锁 |
| RSS 解析 | Rome 2.1.0 |
| HTTP 客户端 | OkHttp 5.5.0 |
| 构建工具 | Maven |

## 运行环境

- JDK 21+
- Maven 3.9+
- MongoDB
- Redis
- 可选：qBittorrent Web API 或 Transmission RPC

## 配置

默认配置位于 `src/main/resources/application.properties`。敏感配置通过环境变量传入：

| 环境变量 | 对应配置 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `CRW_MONGODB_URL` | `spring.data.mongodb.uri` | 无 | MongoDB 连接 URI，必填 |
| `CRW_REDIS_HOST` | `spring.data.redis.host` | 无 | Redis 主机，必填 |
| `CRW_REDIS_PORT` | `spring.data.redis.port` | `6379` | Redis 端口 |
| `CRW_REDIS_PW` | `spring.data.redis.password` | 无 | Redis 密码；无密码时应按部署环境显式处理 |
| `CRW_REDIS_DB` | `spring.data.redis.database` | `1` | Redis 数据库编号 |
| `CRW_SERVER_LOG_LEVEL` | `logging.level.root` | `info` | 日志级别 |
| `CRW_SERVER_LOG_NAME` | `logging.file.name` | `coolkidrss.log` | 日志文件名 |
| `CRW_SNID_DB` | `coolkidrss.databaseId` | `1` | Spring 配置中的 Snowflake 数据中心/数据库标识，范围 0–31 |
| `CRW_SNID_ND` | `coolkidrss.nodeId` | `1` | Spring 配置中的 Snowflake 节点标识，范围 0–31 |
| `CRW_SETTING_CLEAN_DATA` | `coolkidrss.clean.data` | `false` | 是否启用历史数据清理 |
| `CRW_SETTING_CLEAN_DATA_MONTH` | `coolkidrss.keep.data.month` | `12` | 保留最近多少个月的数据 |

示例：

```bash
export CRW_MONGODB_URL='mongodb://127.0.0.1:27017/coolkid_rss'
export CRW_REDIS_HOST='127.0.0.1'
export CRW_REDIS_PORT='6379'
export CRW_REDIS_PW='your-redis-password'
export CRW_REDIS_DB='1'

mvn spring-boot:run
```

服务启动后地址为：

```text
http://localhost:8081/coolkid-rss
```

开发模式可将 `coolkidrss.devmode` 设为 `true`。该模式会跳过 RSS 更新、自动下载和历史数据清理等后台任务，但不会关闭 HTTP 接口。

## 构建与启动

```bash
# 编译并打包
mvn clean package

# 启动打包后的服务
java -jar target/coolkid-rss.jar
```

项目的 Maven 打包配置默认跳过测试执行；如需执行测试，可单独运行：

```bash
mvn test -Dsurefire.skip=false
```

## 后台任务

后台任务由 Spring Scheduling 启用，并通过 Redisson 分布式锁避免多实例重复执行。

| 任务 | 周期 | 作用 |
| --- | --- | --- |
| `RssUpdateQueueJob` | 每秒检查 | 领取到期订阅并更新 Feed；启动时恢复调度队列 |
| `RssDownloadTask` | 每 5 分钟 | 执行启用的下载规则 |
| `CleanOldDataTask` | 每 2 小时 | 按保留月数清理旧记录和下载日志 |

## API 速查

所有接口都需要拼接基础路径 `/coolkid-rss`。成功响应统一为：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### Feed 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/feed/getFeedList?full=false` | 获取订阅源列表；`full=true` 包含停用项 |
| POST | `/api/feed/getFeedRecord` | 分页查询 Feed 记录 |
| GET | `/api/feed/readRecord?recordId={id}` | 标记单条记录为已读 |
| GET | `/api/feed/favRecord?recordId={id}&fav=0\|1` | 设置收藏状态 |
| GET | `/api/feed/allRead?feedId={id}` | 将订阅源下的记录全部标记为已读 |
| GET | `/api/feed/delete?feedId={id}` | 删除订阅源 |
| POST | `/api/feed/sorton` | 批量更新订阅源排序 |
| POST | `/api/feed/save` | 新增或更新订阅源 |
| POST | `/api/feed/flush` | 立即刷新指定订阅源 |

新增订阅源示例：

```json
{
  "feedName": "示例 RSS",
  "feedUrl": "https://example.com/feed.xml",
  "feedCrontab": 30,
  "status": 1,
  "sortOn": 1
}
```

Feed 记录查询示例：

```json
{
  "feedId": "订阅源 ID",
  "keywords": "linux -beta ubuntu|debian",
  "unread": true,
  "fav": false,
  "page": 1,
  "pageSize": 15,
  "startDate": "2026-01-01",
  "endDate": "2026-12-31"
}
```

### 规则接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/rule/list` | 按标题关键字查询规则列表 |
| GET | `/api/rule/item?ruleId={id}` | 获取规则关联的订阅源 |
| POST | `/api/rule/testRule` | 使用指定订阅源测试规则 |
| POST | `/api/rule/save` | 新增或更新下载规则 |
| GET | `/api/rule/delete?ruleId={id}` | 删除规则 |
| POST | `/api/rule/dlold` | 手动执行一次启用规则的自动下载 |

规则保存示例：

```json
{
  "ruleTitle": "Linux ISO",
  "ruleParam": "linux iso -beta ubuntu|debian",
  "ruleType": 0,
  "ruleSavePath": "/downloads/linux",
  "ruleSaveParam": 1,
  "dlId": "下载器 ID",
  "status": 1,
  "feedIds": ["订阅源 ID"]
}
```

规则字段说明：

- `ruleType=0`：普通规则。空格分隔的条件同时满足；以 `-` 开头表示排除；同一条件中使用 `|` 表示满足任意一个候选词。
- `ruleType=1`：正则规则，`ruleParam` 按 Java/MongoDB 标题正则匹配。
- `ruleSaveParam=0`：使用下载器默认目录布局。
- `ruleSaveParam=1`：不新建子文件夹。
- 自动下载只处理 `status=1` 且尚未下载的记录。

普通规则示例：

```text
linux ubuntu -beta
```

表示标题必须包含 `linux` 和 `ubuntu`，且不能包含 `beta`。

### 下载器接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/dl/list` | 获取下载器配置 |
| POST | `/api/dl/save` | 新增或更新下载器 |
| GET | `/api/dl/delete?dlId={id}` | 删除下载器 |
| POST | `/api/dl/log` | 分页查询下载日志 |
| POST | `/api/dl/download` | 下载单条 Feed 记录附件 |

下载器保存示例：

```json
{
  "dlName": "qBittorrent",
  "dlUrl": "http://127.0.0.1:8080",
  "dlType": 1,
  "dlUser": "admin",
  "dlPasswd": "password",
  "status": 1
}
```

下载器类型：

- `dlType=1`：qBittorrent Web API。
- `dlType=2`：Transmission RPC，服务会访问 `/transmission/rpc`。

单条下载示例：

```json
{
  "downUrl": "https://example.com/file.torrent",
  "ruleSavePath": "/downloads/linux",
  "ruleSaveParam": 1,
  "dlId": "下载器 ID",
  "recordId": "记录 ID",
  "recordTitle": "记录标题"
}
```

## ID 生成与多实例部署

`RssIdUtil` 是项目统一的 ID 入口，首次调用时通过 Hutool `Props` 直接读取 classpath 下的 `application.properties` 中的 `coolkidrss.databaseId` 和 `coolkidrss.nodeId`，并按需初始化 `SnowflakeIdGenerator`。Feed、规则、下载器和下载日志等业务对象都会通过 `RssIdUtil.nextIdStr()` 生成字符串 ID。

由于 `RssIdUtil` 不是通过 Spring `Environment` 读取配置，部署时应确认打包进应用的 `application.properties` 中这两个值是实际数字。当前文件里的 `${CRW_SNID_DB:1}`、`${CRW_SNID_ND:1}` 是 Spring 的占位符，不能假定它们会被 Hutool `Props` 解析为环境变量；如果需要通过环境变量动态设置节点值，应先调整 `RssIdUtil` 的配置读取方式。

Snowflake 的节点和数据库标识均为 5 bit，合法范围是 `0–31`。单实例运行可使用默认值；多实例部署时必须为不同实例配置不重复的 `CRW_SNID_ND`/`CRW_SNID_DB` 组合，否则可能产生重复 ID。系统检测到时钟回拨时会拒绝生成 ID，需要检查宿主机时间同步。

## 数据存储

MongoDB 中主要使用以下集合：

| 集合 | 内容 |
| --- | --- |
| `rss_feed_info` | RSS 订阅源 |
| `rss_feed_record` | RSS 条目及阅读/收藏/下载状态 |
| `rss_rule_info` | 过滤和下载规则 |
| `rss_dl_info` | 下载器配置 |
| `rss_dl_log` | 下载日志 |

请先准备好 MongoDB 和 Redis，再启动应用。项目会根据实体注解创建/使用必要索引；生产环境建议提前规划备份、权限和容量策略。

## 项目结构

```text
src/main/java/com/coolkid/coolkidrss
├── controller/    HTTP 接口
├── service/       业务服务和下载逻辑
├── task/          RSS 更新、自动下载、数据清理任务
├── rssfilter/     普通关键词和正则规则处理
├── download/      qBittorrent、Transmission 客户端
├── dao/           Reactive MongoDB Repository
├── entity/        MongoDB 文档实体
├── model/         请求、响应和任务模型
├── config/        Redis、Jackson、运行时提示等配置
└── util/          Feed、HTTP、加密、ID 等工具类
```

## 注意事项

- 当前配置未提供认证和授权，请通过网关、反向代理或网络隔离保护管理接口。
- `dlUser` 和 `dlPasswd` 的实体注释标记为 AES 加密字段，但当前下载流程会直接将字段值传给下载器客户端，未自动调用 `AESUtil` 解密；录入和部署时应结合实际业务流程保护凭据，不要把真实凭据提交到代码仓库。
- RSS 更新和自动下载任务在非开发模式下会自动运行；首次上线建议先设置 `coolkidrss.devmode=true` 验证数据库、Redis 和下载器连通性。
- 项目使用响应式 WebFlux，但下载器调用底层 HTTP API 时包含同步 OkHttp 调用，部署时应关注线程池、网络超时和下载器响应时间。
- 未发现仓库内置的集成测试或 API 文档生成配置，接口联调可使用上面的请求示例。

## License

当前仓库未声明开源许可证。如需对外发布，请补充许可证和版权信息。
