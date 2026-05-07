---
alwaysApply: true
---
# 编码规范

## 1. 命名

- 类名：大驼峰，见名知意
- 方法名：小驼峰，动词开头
- 常量：全大写下划线分隔
- 抽象类以 Abstract 或 Base 开头
- 测试类以被测类名开头，Test 结尾
- 禁止拼音、中英混拼命名

项目补充：

- API 接口以 `I` 开头，例如 `IPayService`、`IAuthService`。
- 领域服务接口以 `I` 开头，实现类使用业务名，例如 `IOrderService`、`OrderService`、`AbstractOrderService`。
- 仓储端口命名为 `IxxxRepository`，实现类放在 `infrastructure/adapter/repository`，命名为 `XxxRepository`。
- 外部能力端口命名为 `IxxxPort`，实现类放在 `infrastructure/adapter/port`，命名为 `XxxPort`。
- 外部 HTTP 网关接口命名为 `IxxxService` 或按已有风格延续，例如 `IGroupBuyMarketService`、`IWeixinApiService`。
- API 层 DTO 使用 `*RequestDTO`、`*ResponseDTO`；领域对象使用 `*Entity`、`*VO`、`*Aggregate`；数据库对象使用 `PO` 包下对象。

## 2. 异常处理

- 业务异常使用当前项目的 `AppException`，携带 `code` 和 `info`。
- 系统异常向上抛出，由统一异常处理器兜底
- 禁止吞掉异常（空 catch）
- catch 中必须记录日志
- Controller 中捕获业务异常后，返回 `api/response/Response` 或当前接口既有返回结构。
- Infrastructure 调外部服务失败时，不要把 Retrofit、HTTP、JSON 细节直接抛给 Controller；应转换为领域可理解的失败。

## 3. 日志

- Controller 入口打 INFO，含请求关键参数
- 异常打 ERROR，含完整堆栈
- 禁止在日志中打印用户敏感信息
- 资金、订单状态、支付回调、退款、拼团结算日志必须包含 `userId`、`orderId/outTradeNo`、关键状态，便于排查。
- 支付宝私钥、微信密钥、JWT/token、数据库密码不能进入日志。

## 4. 其他

- 写接口必须考虑幂等
- 涉及并发场景必须说明同步策略
- 魔法值必须定义为常量
- Controller 不写复杂业务规则；规则下沉到 `domain`。
- MyBatis SQL 变更必须同步检查 `dao/po`、`IOrderDao`、mapper XML、SQL 初始化脚本。
- 新增 REST 调用优先沿用 Retrofit2 和 `Retrofit2Config`，不要临时引入 Feign/WebClient，除非 Spec 明确说明原因。
- 新增配置放入 `application-*.yml` 并配套 `@ConfigurationProperties` 或已有配置类，不硬编码。
- 同一需求尽量控制在 3-5 个核心文件；跨模块变更必须在 `tasks.md` 中明确执行顺序。
