# ddd-trade-marketing-platform 全局知识地图

> 范围：根工作区、`group-buy-market/`、`s-pay-mall-ddd/`  
> 性质：已 Research 的可复用项目知识  
> 最后更新：2026-05-09

## 1. 工作区定位

`ddd-trade-marketing-platform` 根目录不是 Maven reactor，而是聚合两个独立 Java DDD 项目的工作区：

- `group-buy-market/` 指向拼团营销服务，父工程见 `group-buy-market/pom.xml`。
- `s-pay-mall-ddd/` 指向支付商城服务，父工程见 `s-pay-mall-ddd/pom.xml`。
- 根目录 `AGENTS.md` 与根级 `code_copilot/` 提供跨项目协作规范。
- `s-pay-mall-ddd/code_copilot/` 已存在子项目级 SpecAI 工作台，处理商城内部变更时优先级高于根级规则。

两个子项目均使用 Java 17、Spring Boot 2.7.12、Maven 多模块 DDD 分层。常见模块是 `api`、`app`、`domain`、`trigger`、`infrastructure`、`types`。

## 2. 服务职责

### group-buy-market

拼团营销上下文，负责活动展示、优惠试算、拼团锁单、支付后结算、超时退单、拼团退款回滚和回调通知。

关键证据：

- 启动类：`group-buy-market/group-buy-market-lpc-app/src/main/java/top/licodetech/market/Application.java`
- 交易接口：`group-buy-market/group-buy-market-lpc-trigger/src/main/java/top/licodetech/market/trigger/http/MarketTradeController.java`
- 锁单服务：`group-buy-market/group-buy-market-lpc-domain/src/main/java/top/licodetech/market/domain/trade/service/lock/TradeLockOrderService.java`
- 结算服务：`group-buy-market/group-buy-market-lpc-domain/src/main/java/top/licodetech/market/domain/trade/service/settlement/TradeSettlementOrderService.java`
- 退单服务：`group-buy-market/group-buy-market-lpc-domain/src/main/java/top/licodetech/market/domain/trade/service/refund/TradeRefundOrderService.java`
- 回调任务服务：`group-buy-market/group-buy-market-lpc-domain/src/main/java/top/licodetech/market/domain/trade/service/task/TradeTaskService.java`

### s-pay-mall-ddd

支付商城上下文，负责商品查询、订单创建、支付宝支付、支付回调、订单状态、退款、微信登录和与拼团营销服务交互。

关键证据：

- 启动类：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/java/top/licodetech/mall/Application.java`
- 支付入口：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/AliPayController.java`
- 登录入口：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/LoginController.java`
- 订单服务接口：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/IOrderService.java`
- 订单服务实现：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/OrderService.java`
- 拼团营销网关：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/gateway/IGroupBuyMarketService.java`

## 3. DDD 分层约定

两个子项目遵循相同的模块边界：

- `api`：对外接口、DTO、统一 Response。
- `app`：启动类、Spring 配置、`application-*.yml`、MyBatis mapper XML、测试承载。
- `domain`：领域服务、聚合、实体、值对象、Repository/Port 接口。
- `trigger`：HTTP Controller、RabbitMQ Listener、Scheduled Job。
- `infrastructure`：DAO/PO、Repository 实现、Gateway、EventPublisher、Redis、外部系统适配。
- `types`：枚举、异常、常量、通用工具。

跨项目变更时，必须先判断改动落在哪个子项目和哪个层级。领域层只能依赖端口接口，不应直接依赖 Controller、Mapper、RabbitMQ 注解、Retrofit、OkHttp 或配置路径。

## 4. 跨项目同步调用链

支付商城通过 Retrofit 调用拼团营销服务，配置入口是：

- `s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/java/top/licodetech/mall/config/Retrofit2Config.java`
- `s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/resources/application-dev.yml`

商城侧网关接口：

- `IGroupBuyMarketService.lockMarketPayOrder` 调用 `POST api/v1/gbm/trade/lock_market_pay_order`
- `IGroupBuyMarketService.settlementMarketPayOrder` 调用 `POST api/v1/gbm/trade/settlement_market_pay_order`
- `IGroupBuyMarketService.refundMarketPayOrder` 调用 `POST api/v1/gbm/trade/refund_market_pay_order`

商城侧端口适配在 `ProductPort`：

- `lockMarketPayOrder`：下单时请求营销锁单并返回优惠金额。
- `settlementMarketPayOrder`：支付成功后请求营销结算。
- `refundMarketPayOrder`：退单时请求营销退单，接受 `success` 和 `repeat` 作为成功语义。

营销侧对应入口在 `MarketTradeController`：

- `lock_market_pay_order`：参数校验、查重、拼团进度校验、优惠试算、锁单。
- `settlement_market_pay_order`：支付后营销结算，可能触发组队完成通知。
- `refund_market_pay_order`：拼团营销退单，返回退单行为结果。

### 跨项目服务套餐目录策略

> 来源：`ai-service-subscription-platform` change，2026-05-12

- **营销侧数据库表是服务套餐目录的唯一数据源**（`sku`、`sc_sku_activity`、`group_buy_activity`、`group_buy_discount`），负责维护可售卖套餐、价格、额度、优惠和拼团活动配置。
- **商城侧不新增第二套套餐目录主数据**，通过 Retrofit 调用营销侧 `query_group_buy_market_config`（`IGroupBuyMarketService`）获取套餐快照，落订单时保存 `servicePackageId`、`productName`、`totalQuota`、`price` 等履约所需字段。
- 跨项目关联键：商城侧 `pay_order.product_id` / `pay_order.service_package_id` = 营销侧 `sku.goods_id`。
- `ProductRPC` 早期硬编码示例（`MyBatisBook`、`100.00`）已被废弃并替换为 `UnsupportedOperationException`，不再作为套餐目录来源。

## 5. 异步消息链路

### 商城支付成功消息

`s-pay-mall-ddd` 生产支付成功消息：

- 发布器：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/event/EventPublisher.java`
- exchange 配置：`spring.rabbitmq.config.producer.topic_order_pay_success.exchange`
- 配置文件：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/resources/application-dev.yml`

`s-pay-mall-ddd` 自身消费支付成功消息：

- Listener：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/listener/OrderPaySuccessListener.java`
- 行为：解析 `PaySuccessMessageEvent.PaySuccessMessage`，调用 `goodsService.changeOrderDealDone`。

### 拼团成团消息

`group-buy-market` 使用统一 exchange `group_buy_market_exchange` 发布营销消息，配置见 `group-buy-market/group-buy-market-lpc-app/src/main/resources/application-dev.yml`。

`s-pay-mall-ddd` 消费 `topic.team_success`：

- Listener：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/listener/TeamSuccessTopicListener.java`
- 行为：解析 `NotifyRequestDTO`，调用 `orderService.changeOrderMarketSettlement(requestDTO.getOutTradeNoList())`。

`group-buy-market` 也存在 `TeamSuccessTopicListener`，当前行为主要是记录消息，见 `group-buy-market/group-buy-market-lpc-trigger/src/main/java/top/licodetech/market/trigger/listener/TeamSuccessTopicListener.java`。

### 拼团退单成功消息

`group-buy-market` 与 `s-pay-mall-ddd` 均消费 `topic.team_refund` 相关消息：

- 营销侧：`group-buy-market/group-buy-market-lpc-trigger/src/main/java/top/licodetech/market/trigger/listener/RefundSuccessTopicListener.java`
- 商城侧：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/listener/RefundSuccessTopicListener.java`

营销侧收到退单成功消息后调用 `tradeRefundOrderService.restoreTeamLockStock` 恢复拼团队伍锁单量。商城侧收到消息后解析 `type`、`outTradeNo/orderId`，调用 `orderService.receiveRefundSuccessMessage`，失败时依赖本地退款任务补偿。

## 6. 定时任务与补偿

### group-buy-market

- `GroupBuyNotifyJob`：每天执行回调通知任务，使用 Redisson 分布式锁 `group_buy_market_notify_job_exec`。
- `TimeoutRefundJob`：按 cron 扫描超时未支付订单，使用 Redisson 分布式锁 `group_buy_market_timeout_refund_job_exec`，调用 `tradeRefundOrderService.refundOrder`。
- `TradeTaskService`：执行 notify task，成功、重试、失败状态由 Repository 更新。

### s-pay-mall-ddd

- `NoPayNotifyOrderJob`：每分钟查询未处理支付回调的订单，通过支付宝查询接口补偿支付成功状态。
- `TimeoutCloseOrderJob`：周期性关闭超时未支付订单。
- `RefundTaskJob`：每分钟处理待补偿退款任务，调用 `orderService.processRefundTask`。

补偿任务都涉及状态一致性，修改时必须同时检查幂等条件、状态前置条件和失败重试路径。

## 7. 数据与持久化

两个子项目都使用 MyBatis：

- 营销 mapper：`group-buy-market/group-buy-market-lpc-app/src/main/resources/mybatis/mapper/`
- 商城 mapper：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/resources/mybatis/mapper/`

营销侧主要 DAO/PO 包在：

- `group-buy-market/group-buy-market-lpc-infrastructure/src/main/java/top/licodetech/market/infrastructure/dao/`
- `group-buy-market/group-buy-market-lpc-infrastructure/src/main/java/top/licodetech/market/infrastructure/dao/po/`

商城侧主要 DAO/PO 包在：

- `s-pay-mall-ddd/s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/dao/`
- `s-pay-mall-ddd/s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/dao/po/`

任何数据库变更都必须成组检查：DAO、PO、Mapper XML、初始化 SQL、Repository 映射和测试数据。

## 8. 配置与本地环境

本地主要配置文件：

- `group-buy-market/group-buy-market-lpc-app/src/main/resources/application-dev.yml`
- `s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/resources/application-dev.yml`
- `group-buy-market/docs/dev-ops/docker-compose-environment.yml`
- `s-pay-mall-ddd/docs/dev-ops/docker-compose-environment.yml`

已观察到的本地端口：

- `group-buy-market` 服务端口：`8091`
- `s-pay-mall-ddd` 服务端口：`8080`
- MySQL：`127.0.0.1:13306`
- Redis：`127.0.0.1:16379`
- RabbitMQ：`127.0.0.1:5672`

安全注意：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/resources/application-dev.yml` 中存在微信与支付宝相关配置字段，处理文档、日志和提交时不得复制真实密钥值。

## 9. 高风险变更清单

以下主题默认需要独立 Spec、任务拆分、测试计划和人工确认：

- 商城订单状态：`PAY_WAIT`、支付成功、营销结算、退款中、退款完成、关闭。
- 支付宝回调与 `NoPayNotifyOrderJob` 补偿。
- 商城调用营销锁单、结算、退单的 Retrofit 契约。
- 拼团锁单、成团结算、超时未支付退单、锁单量恢复。
- `topic.order_pay_success`、`topic.team_success`、`topic.team_refund` 消息契约。
- 本地任务表、退款任务表、通知任务表相关状态迁移。
- MyBatis Mapper、DAO、PO、数据库字段和索引。
- 配置项改动，尤其是外部地址、RabbitMQ exchange/routing key/queue、支付与微信配置。

## 10. 推荐 Research 入口

做跨项目需求时，优先按以下顺序阅读：

1. 根级 `code_copilot/rules/*.md`。
2. `s-pay-mall-ddd/code_copilot/README.md`，如果涉及商城。
3. 目标接口：
   - 商城支付入口：`AliPayController`
   - 营销交易入口：`MarketTradeController`
4. 领域服务：
   - 商城：`IOrderService`、`OrderService`
   - 营销：`ITradeLockOrderService`、`ITradeSettlementOrderService`、`ITradeRefundOrderService`
5. 端口与基础设施：
   - 商城：`ProductPort`、`RefundPort`、`IGroupBuyMarketService`
   - 营销：`TradeRepository`、`TradePort`、`EventPublisher`
6. Listener 与 Job：
   - 支付成功、成团、退单成功消息
   - 支付补偿、退款补偿、超时关单、超时退单
7. 配置：
   - 两个子项目的 `application-dev.yml`
   - 两个子项目的 `docs/dev-ops/`

## 11. 常用验证命令

```bash
cd group-buy-market && mvn clean package
cd s-pay-mall-ddd && mvn clean package
cd group-buy-market && mvn -pl group-buy-market-lpc-app -DskipTests=false test
cd s-pay-mall-ddd && mvn -pl s-pay-mall-ddd-lpc-app -DskipTests=false test
```

若 Maven 依赖下载受限，应至少完成结构检查、POM 检查、目标类编译影响分析，并在 `changes/<change-id>/log.md` 中记录未执行原因。

## 12. 数据表、幂等键与关键索引

### 12.1 s-pay-mall-ddd

商城侧核心表来自 `s-pay-mall-ddd/docs/dev-ops/mysql/sql/s-pay-mall.sql`。

`pay_order` 是商城订单主表：

- 主键：`id`
- 订单幂等键：唯一索引 `uq_order_id(order_id)`
- 用户商品查询索引：`idx_user_id_product_id(user_id, product_id)`
- 用户订单列表索引：`idx_user_id_id(user_id, id)`
- 状态字段：`status`
- 金额字段：`total_amount`、`market_deduction_amount`、`pay_amount`
- 营销字段：`market_type`

`pay_order.status` 当前语义：

- `CREATE`：订单创建完成，尚未创建支付单。
- `PAY_WAIT`：等待支付。
- `PAY_SUCCESS`：支付成功。
- `MARKET`：拼团营销结算完成。
- `DEAL_DONE`：发货或业务交付完成。
- `CLOSE`：超时关单。
- `REFUNDING`：退单中。
- `REFUNDED`：已退单。

状态枚举证据：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/model/valobj/OrderStatusVO.java`。

`pay_refund_task` 是商城侧拼团退款补偿任务表：

- 主键：`id`
- 任务幂等键：唯一索引 `uq_order_id(order_id)`
- 调度索引：`idx_status_next_retry_time(status, next_retry_time)`
- 状态字段：`status`
- 重试字段：`retry_count`、`next_retry_time`、`error_info`
- 原始消息：`message`

补偿 Mapper 证据：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/resources/mybatis/mapper/refund_task_mapper.xml`。

重要约束：

- `insertIgnore` 依赖 `uq_order_id(order_id)` 保证同一订单的退款任务只落一条。
- `lockRefundTask` 将 `PENDING/RETRY` 或超时 `PROCESSING` 任务置为 `PROCESSING`，用于任务抢占。
- `markRefundTaskRetry` 使用递增退避：`next_retry_time = date_add(now(), interval least(retry_count + 1, 10) minute)`。
- `markRefundTaskFailed` 表示永久失败，不再走普通重试。

### 12.2 group-buy-market

营销侧核心表来自 `group-buy-market/docs/dev-ops/mysql/sql/2-28-group_buy_market.sql`。

`group_buy_order` 是拼团队伍表：

- 主键：`id`
- 队伍幂等键：唯一索引 `uq_team_id(team_id)`
- 状态字段：`status`
- 计数字段：`target_count`、`complete_count`、`lock_count`
- 回调字段：`notify_type`、`notify_url`

`group_buy_order.status` 当前语义：

- `0`：拼单中。
- `1`：完成。
- `2`：失败。
- `3`：完成但含退单。

`group_buy_order_list` 是拼团订单明细表：

- 主键：`id`
- 营销订单幂等键：唯一索引 `uq_order_id(order_id)`
- 查询索引：`idx_user_id_activity_id(user_id, activity_id)`
- 外部订单号：`out_trade_no`，注释标明用于外部调用唯一幂等。
- 业务唯一值：`biz_id = activityId_userId_takeCount`
- 状态字段：`status`

`group_buy_order_list.status` 当前语义：

- `0`：初始锁定。
- `1`：消费完成。
- `2`：用户退单。

`notify_task` 是营销侧本地消息/回调任务表：

- 主键：`id`
- 任务标识：`uuid`
- 任务查询字段：`team_id`、`notify_status`
- 回调方式：`notify_type`，支持 `HTTP` 与 `MQ`
- MQ 路由键字段：`notify_mq`
- 回调载荷：`parameter_json`
- 重试字段：`notify_count`

注意：SQL 中 `notify_task` 的 `uq_uuid` 是普通 `KEY`，不是 `UNIQUE KEY`。虽然代码用 `uuid = teamId_notifyCategory_orderId` 构造幂等标识，但数据库层当前没有唯一约束兜底。变更通知任务写入逻辑时必须显式评估是否需要补充唯一索引。

任务 Mapper 证据：`group-buy-market/group-buy-market-lpc-app/src/main/resources/mybatis/mapper/nofify_task_mapper.xml`。

## 13. 消息链路、消息体与幂等键

### 13.1 商城支付成功消息

生产者：

- `s-pay-mall-ddd/s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/adapter/repository/OrderRepository.java`
- `changeOrderPaySuccess` 和 `changeOrderMarketSettlement` 均会发布支付成功消息。

消息定义：

- `s-pay-mall-ddd/s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/adapter/event/PaySuccessMessageEvent.java`
- 消息数据字段：`useId`、`tradeNo`
- 当前实际构造主要设置 `tradeNo = orderId/outTradeNo`

RabbitMQ 配置：

- exchange：`s_pay_mall_exchange`
- routing key：`topic.order_pay_success`
- queue：`s_pay_mall_queue_2_order_pay_success`
- 配置证据：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/resources/application-dev.yml`

消费者：

- `s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/listener/OrderPaySuccessListener.java`
- 消费后调用 `goodsService.changeOrderDealDone(paySuccessMessage.getTradeNo())`

幂等线索：

- 业务幂等依赖 `pay_order.uq_order_id(order_id)` 和订单状态更新逻辑。
- 消息体没有独立业务幂等键字段，`tradeNo` 是事实上的业务键。

### 13.2 拼团成团消息

生产者：

- `group-buy-market/group-buy-market-lpc-domain/src/main/java/top/licodetech/market/domain/trade/service/settlement/TradeSettlementOrderService.java`
- `group-buy-market/group-buy-market-lpc-infrastructure/src/main/java/top/licodetech/market/infrastructure/adapter/repository/TradeRepository.java`
- 拼团达成目标时写入 `notify_task`，再由 `TradeTaskService.execNotifyJob` 发送。

消息体：

- DTO：`group-buy-market/group-buy-market-lpc-api/src/main/java/top/licodetech/market/api/dto/NotifyRequestDTO.java`
- 商城侧同名 DTO：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-api/src/main/java/top/licodetech/mall/api/dto/NotifyRequestDTO.java`
- 字段：`teamId`、`outTradeNoList`
- `parameter_json` 示例结构：`{"teamId":"...","outTradeNoList":["..."]}`

RabbitMQ 配置：

- exchange：`group_buy_market_exchange`
- routing key：`topic.team_success`
- 营销本地 queue：`group_buy_market_queue_2_topic_team_success`
- 商城消费 queue：`s_pay_mall_queue_2_topic_team_success`

消费者：

- 商城：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/listener/TeamSuccessTopicListener.java`
- 行为：调用 `orderService.changeOrderMarketSettlement(outTradeNoList)`
- HTTP 兼容入口：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/AliPayController.java` 的 `group_buy_notify`

幂等线索：

- 营销侧 `notify_task.uuid = teamId + "_" + TRADE_SETTLEMENT + "_" + outTradeNo`
- 商城侧批量更新 `pay_order`：`changeOrderMarketSettlement(outTradeNoList)`，以 `order_id in (...)` 更新到 `MARKET`。
- 消息重复时需重点验证 `MARKET -> DEAL_DONE` 及重复发货消息的影响。

### 13.3 拼团退单成功消息

生产者：

- `group-buy-market/group-buy-market-lpc-infrastructure/src/main/java/top/licodetech/market/infrastructure/adapter/repository/TradeRepository.java`
- `unpaid2Refund`、`paid2Refund`、`paidTeam2Refund` 都会写入 `notify_task`。

消息体：

- 值对象：`group-buy-market/group-buy-market-lpc-domain/src/main/java/top/licodetech/market/domain/trade/model/valobj/TeamRefundSuccess.java`
- 字段：`type`、`userId`、`teamId`、`activityId`、`orderId`、`outTradeNo`
- `type` 取值来自 `RefundTypeEnumVO`：`unpaid_unlock`、`paid_unformed`、`paid_formed`

RabbitMQ 配置：

- exchange：`group_buy_market_exchange`
- routing key：`topic.team_refund`
- 营销本地 queue：`group_buy_market_queue_2_topic_team_refund`
- 商城消费 queue：`s_pay_mall_queue_2_topic_team_refund`

消费者：

- 营销侧：`group-buy-market/group-buy-market-lpc-trigger/src/main/java/top/licodetech/market/trigger/listener/RefundSuccessTopicListener.java`
- 商城侧：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/listener/RefundSuccessTopicListener.java`

幂等线索：

- 营销侧库存恢复使用 Redis 锁：`refund_lock_<orderId>`，见 `TradeRepository.refund2AddRecovery`。
- 商城侧退款补偿任务依赖 `pay_refund_task.uq_order_id(order_id)` 和 `insertIgnore`。
- 商城侧 `RefundSuccessTopicListener` 优先使用 `outTradeNo` 作为 `orderId`；为空时回退到消息里的 `orderId`。

## 14. 正向时序：商城下单到成团结算

```text
用户/前端
  -> s-pay-mall-ddd AliPayController.create_pay_order
  -> IOrderService.createOrder
  -> AbstractOrderService.createOrder
     -> IProductPort.queryProductByProductId
     -> 如 marketType=GROUP_BUY_MARKET:
        -> ProductPort.lockMarketPayOrder
        -> Retrofit IGroupBuyMarketService.lockMarketPayOrder
        -> group-buy-market MarketTradeController.lock_market_pay_order
        -> IIndexGroupBuyMarketService.indexMarketTrial
        -> ITradeLockOrderService.lockMarketPayOrder
        -> TradeRepository.lockMarketPayOrder
           -> group_buy_order / group_buy_order_list
     -> OrderService.doPrepayOrder
     -> AlipayTradePagePayRequest
     -> pay_order 更新 PAY_WAIT、pay_url、金额、营销信息

支付宝回调 / NoPayNotifyOrderJob 补偿
  -> OrderService.changeOrderPaySuccess(orderId, payTime)
     -> 如果非拼团:
        -> pay_order 更新 PAY_SUCCESS
        -> 发布 topic.order_pay_success
        -> OrderPaySuccessListener
        -> goodsService.changeOrderDealDone
     -> 如果拼团:
        -> pay_order 更新 PAY_SUCCESS
        -> ProductPort.settlementMarketPayOrder
        -> Retrofit IGroupBuyMarketService.settlementMarketPayOrder
        -> group-buy-market MarketTradeController.settlement_market_pay_order
        -> ITradeSettlementOrderService.settlementMarketPayOrder
        -> TradeRepository.settlementMarketPayOrder
           -> group_buy_order_list.status = 1
           -> group_buy_order.complete_count + 1
           -> 若达到 target_count:
              -> group_buy_order.status = 1
              -> notify_task 写入 topic.team_success + outTradeNoList
        -> TradeTaskService.execNotifyJob
        -> EventPublisher 发布 topic.team_success
        -> s-pay-mall-ddd TeamSuccessTopicListener
        -> OrderService.changeOrderMarketSettlement(outTradeNoList)
        -> pay_order 更新 MARKET
        -> 发布 topic.order_pay_success
        -> OrderPaySuccessListener
        -> goodsService.changeOrderDealDone
```

正向链路的关键一致性点：

- 商城 `pay_order.order_id` 与营销 `group_buy_order_list.out_trade_no` 是跨项目主关联键。
- 营销侧 `group_buy_order_list.order_id` 是营销内部预购订单 ID，不等同于商城订单号。
- 拼团成团通知可能通过 MQ，也保留 HTTP 回调入口；Spec 必须明确本次变更使用哪种通知方式。
- 支付成功后营销结算失败会依赖支付宝回调重试或 `NoPayNotifyOrderJob` 间接补偿；若要增强可靠性，应考虑独立结算补偿任务。

## 15. 逆向时序：商城退款到营销回滚

```text
用户/前端
  -> s-pay-mall-ddd AliPayController.refund_order
  -> IOrderService.refundOrder(userId, orderId)
  -> OrderService.refundOrder
     -> 校验订单存在、归属、状态可退
     -> 如果非拼团:
        -> pay_order 更新 REFUNDING
        -> RefundPort.refund 调支付宝退款
        -> pay_order 更新 REFUNDED
     -> 如果拼团:
        -> ProductPort.refundMarketPayOrder
        -> Retrofit IGroupBuyMarketService.refundMarketPayOrder
        -> group-buy-market MarketTradeController.refund_market_pay_order
        -> ITradeRefundOrderService.refundOrder
        -> RefundTypeEnumVO 根据 group_buy_order.status + group_buy_order_list.status 选择策略
        -> TradeRepository.unpaid2Refund / paid2Refund / paidTeam2Refund
           -> group_buy_order_list.status = 2
           -> group_buy_order lock_count/complete_count/status 调整
           -> notify_task 写入 topic.team_refund
        -> 商城 pay_order 更新 REFUNDING

营销退单通知
  -> TradeTaskService.execNotifyJob
  -> EventPublisher 发布 topic.team_refund
  -> group-buy-market RefundSuccessTopicListener
     -> tradeRefundOrderService.restoreTeamLockStock
     -> refund2AddRecovery 使用 refund_lock_<orderId> 防重复恢复库存
  -> s-pay-mall-ddd RefundSuccessTopicListener
     -> 解析 type、outTradeNo/orderId
     -> IOrderService.receiveRefundSuccessMessage
     -> pay_refund_task insertIgnore
     -> processRefundTask
     -> 根据 RefundTypeVO 判断是否需要支付渠道退款
     -> RefundPort.refund 调支付宝退款
     -> pay_order 更新 REFUNDED

商城退款任务补偿
  -> RefundTaskJob 每分钟扫描 PENDING/RETRY 或超时 PROCESSING
  -> IOrderService.processRefundTask
  -> 成功 markRefundTaskSuccess
  -> 可重试失败 markRefundTaskRetry
  -> 永久失败 markRefundTaskFailed
```

逆向链路的关键一致性点：

- 退单类型语义必须在两个项目保持一致：`unpaid_unlock`、`paid_unformed`、`paid_formed`。
- `unpaid_unlock` 不需要调支付渠道退款；`paid_unformed` 和 `paid_formed` 需要支付渠道退款。
- 商城侧退单主状态从可退状态进入 `REFUNDING`，最终由营销退单消息和退款任务补偿推进到 `REFUNDED`。
- 营销侧 `notify_task` 的 `uuid` 当前不是数据库唯一约束，重复写入风险需要在涉及重构或补偿增强时单独评估。

## 16. 本地环境依赖

### 16.1 基础组件

两个子项目的 `docker-compose-environment.yml` 都定义了 MySQL、phpMyAdmin、Redis、Redis Admin：

- MySQL：`mysql:8.0.32`，宿主机端口 `13306`
- phpMyAdmin：宿主机端口 `8899`
- Redis：`redis:6.2`，宿主机端口 `16379`
- Redis Admin：宿主机端口 `8081`

SQL 初始化目录均挂载到：

```text
./mysql/sql:/docker-entrypoint-initdb.d
```

注意：两个子项目的基础组件 compose 使用相同容器名和端口，不适合在同一 Docker 环境中同时从两个目录各启动一份。通常应选择一个环境栈启动，或调整容器名和端口。

### 16.2 RabbitMQ

RabbitMQ 未出现在普通 `docker-compose-environment.yml` 中，出现在 `group-buy-market/docs/dev-ops/docker-compose-environment-aliyun.yml`：

- image：`rabbitmq:3.12.9` 的阿里云镜像版本
- 宿主机端口：`5672`、`15672`
- 管理插件配置：`./rabbitmq/enabled_plugins:/etc/rabbitmq/enabled_plugins`

应用配置中两个项目均默认连接：

```text
spring.rabbitmq.addresses=127.0.0.1
spring.rabbitmq.port=5672
spring.rabbitmq.username=admin
spring.rabbitmq.password=admin
```

启动依赖时，如果只启动普通 environment compose，RabbitMQ 不会自动起来，涉及 MQ 的本地验证会失败。

### 16.3 观测组件

`group-buy-market/docs/dev-ops/` 额外包含：

- `docker-compose-elk.yml`：Elasticsearch、Logstash、Kibana。
- `docker-compose-grafana.yml`：Prometheus、Grafana、grafana-mcp。

这部分主要用于日志和监控，不是业务链路最小启动依赖。修改日志、Actuator、Prometheus 或 Logstash 配置时再纳入验证范围。

## 17. 后续仍需补充的知识

- 数据库表字段已梳理，但未逐项校验所有 Repository 映射是否覆盖字段；涉及 Mapper 改动时仍需针对目标表重新 Research。
- 正向/逆向时序已梳理为文本图；如果后续需要架构图，可基于本节生成 Mermaid 图。
- RabbitMQ exchange/routing key/queue 已梳理，但消息重试、死信、确认模式未完整建模；涉及可靠投递改造时需另建 change。
