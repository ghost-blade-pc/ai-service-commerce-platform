# 任务拆分 — 未支付退单退款误触发与 MQ 重试风暴修复
> status: done

## 前置条件

- [x] 已分析支付商城退单完成链路。
- [x] 已确认拼团侧 `topic.team_refund` 消息包含 `type`。
- [x] 已确认普通未支付直购订单也存在误触发退款风险。
- [x] 已分析运行日志中的 MyBatis 参数绑定异常。
- [x] 已确认修复方向采用幂等状态更新、有限重试和 DLQ。

## Task 1: 建模退单类型并接入 MQ 消息

- **目标**: 支付商城识别拼团退单类型，未支付解锁不走资金退款。
- **涉及文件**:
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/model/valobj/RefundTypeVO.java`
  - `s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/listener/RefundSuccessTopicListener.java`
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/IOrderService.java`
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/OrderService.java`
- **验收**:
  - [x] `unpaid_unlock` 映射为不需要资金退款。
  - [x] `paid_unformed/paid_formed` 映射为需要资金退款。
  - [x] listener 对缺失/非法 `type` 记录永久异常并确认消息。

## Task 2: 扩展本地退款任务

- **目标**: 本地任务保存并恢复退单类型，补偿任务不丢失退款语义。
- **涉及文件**:
  - `docs/dev-ops/mysql/sql/s-pay-mall.sql`
  - `s-pay-mall-ddd-lpc-app/src/main/resources/mybatis/mapper/refund_task_mapper.xml`
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/model/entity/RefundTaskEntity.java`
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/adapter/repository/IRefundTaskRepository.java`
  - `s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/adapter/repository/RefundTaskRepository.java`
  - `s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/dao/IRefundTaskDao.java`
  - `s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/dao/po/RefundTask.java`
  - `s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/job/RefundTaskJob.java`
- **验收**:
  - [x] `pay_refund_task.refund_type` 已加入建表 SQL。
  - [x] 新任务保存 `orderId + refundType + message`。
  - [x] 待处理任务查询返回 `RefundTaskEntity`。
  - [x] 历史任务可从 `message.type` 尝试解析，无法解析则失败落库。

## Task 3: 修复普通未支付直购退单误退款

- **目标**: 不选择营销拼团、直接购买但未支付的订单退单时不调用退款端口。
- **涉及文件**:
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/OrderService.java`
  - `s-pay-mall-ddd-lpc-app/src/test/java/top/licodetech/mall/test/domain/OrderServiceRefundTest.java`
- **验收**:
  - [x] `PAY_WAIT` 普通订单退单进入 `REFUNDED`。
  - [x] `IRefundPort#refund` 从未被调用。
  - [x] 已支付订单仍保持退款端口调用。

## Task 4: 修复 team_success MyBatis 参数错误和结算幂等

- **目标**: 解决拼团达成后支付侧 listener 因 MyBatis 参数绑定失败无限重试的问题，并保证重复消息不重复发货。
- **涉及文件**:
  - `s-pay-mall-ddd-lpc-app/src/main/resources/mybatis/mapper/pay_order_mapper.xml`
  - `s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/dao/IOrderDao.java`
  - `s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/adapter/repository/OrderRepository.java`
- **验收**:
  - [x] `changeOrderMarketSettlement` 改为单订单、单参数更新。
  - [x] SQL 只允许 `PAY_SUCCESS -> MARKET`。
  - [x] 只有实际更新成功的订单才发布 `topic.order_pay_success`。

## Task 5: 增加 MQ 有限重试和 DLQ

- **目标**: 消费失败不再无限 requeue，超过重试次数后进入死信队列。
- **涉及文件**:
  - `s-pay-mall-ddd-lpc-app/pom.xml`
  - `s-pay-mall-ddd-lpc-app/src/main/java/top/licodetech/mall/config/RabbitMQConfig.java`
  - `s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/listener/TeamSuccessTopicListener.java`
  - `s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/listener/RefundSuccessTopicListener.java`
  - `s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/listener/OrderPaySuccessListener.java`
- **验收**:
  - [x] 引入 `spring-retry`。
  - [x] listener container factory 配置最多 3 次尝试和指数退避。
  - [x] exhausted 后 reject 且不 requeue。
  - [x] 三个主队列均声明 DLX/DLQ。
  - [x] listener 改为引用配置好的队列，队列声明集中到 `RabbitMQConfig`。

## Task 6: 补充测试和提交

- **目标**: 覆盖关键业务分支并保存提交。
- **涉及文件**:
  - `s-pay-mall-ddd-lpc-app/src/test/java/top/licodetech/mall/test/domain/OrderServiceRefundTest.java`
  - `s-pay-mall-ddd-lpc-app/src/test/java/top/licodetech/mall/test/trigger/RefundSuccessTopicListenerTest.java`
  - `s-pay-mall-ddd-lpc-app/src/test/java/top/licodetech/mall/test/config/RabbitMQConfigTest.java`
  - `s-pay-mall-ddd-lpc-app/src/test/java/top/licodetech/mall/test/infrastructure/OrderRepositoryMarketSettlementTest.java`
- **验收**:
  - [x] `bbbb6b8 fix: avoid refunding unpaid orders`
  - [x] `7f91a12 fix: add mq retry dlq and settlement idempotency`
  - [x] 目标测试通过。

