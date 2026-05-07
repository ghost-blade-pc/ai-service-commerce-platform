# 变更日志 — 退单退款服务对接

> 记录决策、踩坑和知识发现。知识飞轮的输入。

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
|------|------|------|------|
| 2026-05-07 | Research | 读取 `code_copilot` 规则、项目上下文、安全规则、知识索引 | 按 SpecAI 流程启动 |
| 2026-05-07 | Research | 分析支付商城退单入口、领域服务、仓储、MQ、拼团网关 | 发现支付商城当前直接置 `REFUNDED`，缺少拼团退单和退款 MQ |
| 2026-05-07 | Research | 分析拼团营销退单接口、规则链、策略、本地消息表、MQ 消费 | 拼团侧 `refund_market_pay_order` 与 `topic.team_refund` 基本已存在 |
| 2026-05-07 | Propose | 生成 `spec.md` 和 `tasks.md` | 等待用户确认后才能 /apply |
| 2026-05-07 | Clarify | 用户确认本期模拟退款、使用 `out_trade_no=orderId`、`PAY_WAIT` 也走 MQ、允许 `MARKET/DEAL_DONE` 退单 | 已同步更新 spec/tasks |
| 2026-05-07 | Clarify | 用户确认执行顺序：先改支付商城，再把拼团侧两个小补强放进最后一个 task | 拼团项目已创建同名分支 `3-8-260507-lpc-refund-intf-mq` |
| 2026-05-07 | Apply | 完成支付商城退单退款主链路实现 | 出站拼团退单、`REFUNDING` 中间态、退款成功 MQ 监听、模拟退款端口 |
| 2026-05-07 | Apply | 完成拼团侧两个小补强 | `DataNodeFilter` 空数据保护、`topic_team_refund` 显式 Binding |
| 2026-05-07 | Verify | 支付商城 `OrderServiceRefundTest` 执行通过 | 7 tests, 0 failures, 0 errors |
| 2026-05-07 | Verify | 拼团项目 reactor 编译通过 | 测试按项目配置跳过，但源码/测试源码编译通过 |

## 技术决策

| 决策 | 选择 | 放弃的方案 | 原因 |
|------|------|------------|------|
| 支付商城调用拼团退单 | 继续使用 Retrofit2 `IGroupBuyMarketService` | 引入 Feign/WebClient | 当前项目已有 Retrofit2 锁单/结算模式 |
| 退款状态 | 引入/使用 `REFUNDING` 中间态 | 退单申请直接 `REFUNDED` | 需要等待拼团 MQ 和支付宝退款最终一致 |
| MQ 消费 | 支付商城新增独立 `RefundSuccessTopicListener` | 复用 `TeamSuccessTopicListener` | 消息语义不同，职责隔离 |
| 支付商城消费队列 | 独立队列绑定 `topic.team_refund` | 复用拼团队列 | 多服务消费同一事件需要各自队列 |
| 拼团侧修改范围 | 最后 task 做空值保护和 MQ 绑定显式化 | 主任务一开始就改拼团 | 支付商城是主链路缺口，拼团补强不阻塞主链路 |

## DDD 边界记录

| 变更点 | 所属模块 | 是否越界 | 说明 |
|--------|----------|----------|------|
| 用户退单入口 | trigger | 否 | Controller 只做参数校验和响应转换 |
| 退单申请/退款确认规则 | domain | 否 | `OrderService` 编排状态和端口 |
| 拼团退单 HTTP 调用 | infrastructure | 否 | Retrofit DTO 和外部 Response 不进入 domain |
| 退款实现 | infrastructure | 否 | 本期模拟退款成功，后续真实支付宝退款仍应通过端口封装 |
| MQ 消息解析 | trigger | 否 | Listener 解析 JSON 后调用 domain |
| 拼团侧补强 | group-buy-market domain/app | 否 | 仅补充退单前置数据保护和 MQ Binding 配置 |

## 风险记录

| 风险类型 | 位置 | 风险描述 | 处理方式 |
|----------|------|----------|----------|
| 资金 | 支付商城退款确认 | MQ 重复投递可能导致重复退款动作 | 本期模拟退款，仍使用订单状态幂等，只有 `REFUNDING` 可执行，`REFUNDED` 忽略 |
| 状态 | `OrderService#refundOrder` | 当前直接置 `REFUNDED`，与异步退款链路冲突 | 改为 `REFUNDING`，最终由 MQ 推进 |
| 外部接口 | `IGroupBuyMarketService` | 拼团退单 HTTP 成功不代表 MQ 已被消费 | 支付商城只进入 `REFUNDING`，最终等 MQ |
| MQ | `topic.team_refund` | 支付商城未配置消费队列 | 新增 `s_pay_mall_queue_2_topic_team_refund` |
| 数据 | 支付宝退款 | 当前未保存 `trade_no` | 用户已确认本期只使用 `out_trade_no = orderId`，不新增字段 |

## 踩坑记录

| 问题 | 原因 | 解决方案 | 沉淀？ |
|------|------|----------|--------|
| 支付商城和拼团都有 `Response` 类 | 两个项目各自定义统一响应 | Retrofit 网关继续使用 `infrastructure.gateway.response.Response`，API 使用 `api.response.Response` | 是 |
| `topic.team_refund` 既要拼团消费又要支付消费 | RabbitMQ topic 事件广播语义需要多队列绑定 | 两个服务各自队列绑定同一 exchange/routing key | 是 |
| 退单 HTTP 响应和退款最终完成不是同一时刻 | 拼团本地消息表异步发送 MQ | 支付商城返回 `REFUNDING`，监听 MQ 后置 `REFUNDED` | 是 |
| app 模块固定 `skipTests=true` 导致目标单测无法执行 | `s-pay-mall-ddd-lpc-app/pom.xml` surefire 写死跳过测试 | 改为默认属性 `skipTests=true`，允许 `-DskipTests=false` 覆盖 | 是 |

## 知识发现
> 每个 task 后实时记录，/archive 时逐条确认沉淀到 knowledge/

- [ ] **拼团退单接口**: 拼团营销已提供 `POST /api/v1/gbm/trade/refund_market_pay_order`，由 `MarketTradeController#refundMarketPayOrder` 进入 `TradeRefundOrderService#refundOrder`。
- [ ] **拼团退单策略**: `RefundTypeEnumVO#getRefundStrategy` 按拼团状态和交易订单状态选择未支付未成团、已支付未成团、已支付已成团三种策略。
- [ ] **拼团退单 MQ**: `TradeRepository` 在退单事务中插入 `NotifyTask`，通过 `TradeTaskService` 和 `TradePort` 发送 `topic.team_refund`。
- [x] **支付商城退款缺口**: 支付商城原 `OrderService#refundOrder` 直接更新 `REFUNDED`，本期已改为拼团订单先进入 `REFUNDING`，消费拼团 `topic.team_refund` 后模拟退款并置 `REFUNDED`。

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
|--------|-----------|----------|----------|
| `application-test.yml` MQ 配置 | spec 建议同步 test/prod/dev | 当前 `application-test.yml` 为极简配置，未承载 RabbitMQ consumer 配置 | 仅同步 dev/prod，测试环境如需联调 RabbitMQ 再补 |
| Listener 单测 | tasks 建议可新增 `RefundSuccessTopicListenerTest` | 本期优先保障领域状态流转，未新增 Listener 单测 | 已通过 reactor 编译；后续联调 MQ 时补 P2 测试 |
| 真实退款补偿 | 风险提示可能需要本地补偿 | 用户确认本期模拟退款成功 | 不新增支付商城退款补偿表/Job |

## 代码质量备忘

- 后续实现时不能让 `RefundSuccessTopicListener` 直接承载退款业务；本期模拟退款也要经领域服务/端口封装。
- 后续实现时不要把拼团 `RefundMarketPayOrderResponseDTO` 泄漏到 `domain`。
- 后续实现时 `ProductPort#lockMarketPayOrder` 当前 catch 后返回 null 的风格不适合退款，退款失败必须明确阻断进入 `REFUNDING`。

## 验证证据

| 命令/方式 | 结果 | 备注 |
|-----------|------|------|
| `rg refund/Refund/...` | 已完成 | Research 证据已写入 `spec.md` |
| `sed` 读取关键类 | 已完成 | 支付商城和拼团营销关键类均已引用 |
| `mvn -pl s-pay-mall-ddd-lpc-app -am -DskipTests=false -DfailIfNoTests=false -Dtest=OrderServiceRefundTest test` | 通过 | `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0` |
| `mvn -pl group-buy-market-lpc-app -am -DskipTests -DfailIfNoTests=false test` | 通过 | reactor `BUILD SUCCESS`；项目配置跳过测试 |
