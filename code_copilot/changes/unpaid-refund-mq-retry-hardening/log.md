# 变更日志 — 未支付退单退款误触发与 MQ 重试风暴修复

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
|------|------|------|------|
| 2026-05-08 | Analysis | 分析拼团未支付退单误触发退款 | 根因是支付商城只按 `orderId` 完成退款，不识别 `type=unpaid_unlock` |
| 2026-05-08 | Fix | 新增 `RefundTypeVO` 并贯穿 listener、领域服务、本地任务 | `unpaid_unlock` 不调用退款端口 |
| 2026-05-08 | Analysis | 分析普通直购未支付退单误触发退款 | 根因是 `changeOrderRefundSuccess(String)` 默认需要资金退款 |
| 2026-05-08 | Fix | 普通 `PAY_WAIT` 订单退单设置 `needPayRefund=false` | 未支付订单只做状态收敛 |
| 2026-05-08 | Fix | `pay_refund_task` 增加 `refund_type` | 补偿任务不丢失退单类型 |
| 2026-05-08 | Verify | 执行退款相关目标测试 | `OrderServiceRefundTest`、`RefundSuccessTopicListenerTest` 通过 |
| 2026-05-08 | Commit | 创建提交 `bbbb6b8` | `fix: avoid refunding unpaid orders` |
| 2026-05-08 | Analysis | 检查 xfg07 付款完成后支付侧日志疯狂滚动 | 根因是 `team_success` 消费 MyBatis 参数绑定失败且 MQ 无限 requeue |
| 2026-05-08 | Fix | `changeOrderMarketSettlement` 改为单订单幂等更新 | 只有 `PAY_SUCCESS -> MARKET` 成功才发布后续事件 |
| 2026-05-08 | Fix | 新增 RabbitMQ 有限重试和 DLQ | 3 次尝试后 reject，不再 requeue |
| 2026-05-08 | Verify | 执行 MQ/DLQ 与退款目标测试 | 27 tests, 0 failures, 0 errors |
| 2026-05-08 | Commit | 创建提交 `7f91a12` | `fix: add mq retry dlq and settlement idempotency` |

## 关键决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 退单类型 | 在支付商城建 `RefundTypeVO` | 支付侧必须自主决定是否发生资金退款，不能只依赖订单号 |
| 未支付订单最终状态 | 沿用 `REFUNDED` | 用户侧语义是退单完成，不新增 `CLOSE` 降低影响面 |
| 历史任务兼容 | `refund_type` 为空时从 `message.type` 回补解析 | 避免升级前已落库任务丢失语义 |
| MQ 失败处理 | 有限重试后 DLQ | 业务异常不能无限占用消费者并刷日志 |
| 拼团结算更新 | 单订单幂等更新 | 批量更新难以判断哪些订单真实发生状态变化 |

## 踩坑记录

| 问题 | 原因 | 处理方式 |
|------|------|----------|
| `unpaid_unlock` 仍调用退款端口 | listener 未解析 `type`，领域默认需要退款 | 解析并持久化 `RefundTypeVO`，按类型决定是否调用退款端口 |
| 普通未支付直购退单仍调用退款端口 | `changeOrderRefundSuccess(String)` 默认需要退款 | 退单入口按订单状态传入 `needPayRefund`，`PAY_WAIT` 跳过退款 |
| 历史退款任务缺少类型 | 老数据没有 `refund_type` 字段 | 从 `message` JSON 中回补解析，无法解析则失败并停止误退款 |
| `team_success` 消费疯狂刷屏 | MyBatis `<foreach collection="outTradeNoList">` 无对应 `@Param`，异常后 RabbitMQ requeue | 改为单订单更新，并加入有限重试/DLQ |
| 重复 `team_success` 可能重复发布发货事件 | 原逻辑无法区分哪些订单是本次更新成功 | 只有 update count 为 1 的订单发布 `topic.order_pay_success` |
| 已存在 RabbitMQ 队列无法新增 DLX 参数 | RabbitMQ 队列参数不可变 | 发布前删除/重建旧队列，或手工迁移到带 DLX 的队列 |

## 变更文件摘要

### Commit `bbbb6b8`

- `RefundTypeVO`：新增退单类型枚举。
- `RefundSuccessTopicListener`：解析 `type`，非法类型按永久异常确认。
- `OrderService`：按 `RefundTypeVO` 或订单支付状态决定是否调用退款端口。
- `RefundTaskEntity/Repository/Mapper/Job`：本地任务保存、查询、处理 `refund_type`。
- `s-pay-mall.sql`：新增 `pay_refund_task.refund_type`。
- `OrderServiceRefundTest`、`RefundSuccessTopicListenerTest`：覆盖未支付不退款、已支付退款、非法类型等场景。

### Commit `7f91a12`

- `RabbitMQConfig`：集中声明主队列、DLX、DLQ、listener retry interceptor。
- `pom.xml`：新增 `spring-retry`。
- `TeamSuccessTopicListener`、`RefundSuccessTopicListener`、`OrderPaySuccessListener`：改为监听已配置队列。
- `IOrderDao/pay_order_mapper.xml/OrderRepository`：结算状态改为单订单幂等更新。
- `RabbitMQConfigTest`、`OrderRepositoryMarketSettlementTest`：覆盖 DLQ 参数和结算发布幂等。

## 验证证据

| 命令/方式 | 结果 | 备注 |
|-----------|------|------|
| `mvn -pl s-pay-mall-ddd-lpc-app -am -Dtest=OrderRepositoryMarketSettlementTest,RabbitMQConfigTest,OrderServiceRefundTest,RefundSuccessTopicListenerTest -DfailIfNoTests=false -DskipTests=false test` | 通过 | `Tests run: 27, Failures: 0, Errors: 0, Skipped: 0` |
| `git log -1 --oneline` | `7f91a12 fix: add mq retry dlq and settlement idempotency` | 当前最后提交 |
| `git status --short` | 提交后为空 | 代码提交前工作区干净 |

## 发布检查

- [ ] 数据库执行 `ALTER TABLE pay_refund_task ADD COLUMN refund_type varchar(32) DEFAULT NULL COMMENT '退单类型' AFTER order_id;`
- [ ] 删除或重建旧 RabbitMQ 队列：
  - `s_pay_mall_queue_2_topic_team_success`
  - `s_pay_mall_queue_2_topic_team_refund`
  - `s_pay_mall_queue_2_order_pay_success`
- [ ] 发布后检查对应 DLQ 是否为空。
- [ ] 若 DLQ 有消息，优先查看异常栈和 message payload，再人工决定重放或作废。

