# 编码风格

## 基础约定

- Java 17，UTF-8，四空格缩进。
- 遵循现有包结构和命名，不引入与项目风格不一致的新抽象。
- 类名使用大驼峰；方法、字段使用小驼峰；常量使用 `UPPER_SNAKE_CASE`。
- 接口命名沿用项目已有风格，常见模式为 `I*Service`、`I*Repository`、`I*Dao`。
- DTO 使用 `*RequestDTO`、`*ResponseDTO`；领域对象使用 `*Entity`、`*VO`、`*Aggregate`；持久化对象放入 `po` 包。

## DDD 分层

- `api`：只放对外契约、DTO、Response。
- `app`：启动类、配置、资源、测试承载。
- `domain`：业务规则、领域服务、聚合、值对象、端口接口。
- `trigger`：HTTP、MQ、Job 等入口；只做协议转换和调用编排。
- `infrastructure`：DAO、Mapper、Repository 实现、Gateway、MQ、Redis、HTTP 客户端。
- `types`：跨模块通用常量、枚举、异常和工具。

## 实现要求

- 优先复用现有服务、Repository、Port、Gateway、Mapper 风格。
- 不把 Controller 写成业务服务，不把 DAO/HTTP/RabbitMQ 细节泄漏到 `domain`。
- 新增配置必须使用环境变量或 profile 配置，不硬编码生产地址、账号、密码。
- 修改公共 API、MQ 消息、数据库字段、支付/退款/订单/拼团状态前，必须在 Spec 中写清兼容性和风险。
- 日志要包含可排查上下文，但不得输出密钥、token、支付渠道敏感字段或隐私数据。

## 测试风格

- 测试类以 `Test` 结尾，优先放在对应子项目 `*-lpc-app/src/test/java/.../test/`。
- 领域规则优先单元测试或 SpringBootTest 中的领域服务测试。
- DAO、Mapper、配置装配、MQ/HTTP 适配按风险补充集成测试。
