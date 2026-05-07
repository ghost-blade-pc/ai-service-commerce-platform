# 退单退款服务对接
> status: done
> created: 2026-05-07
> complexity: 🔴复杂

## 1. 背景与目标

当前支付商城已有用户退单入口，但实现是同步把支付订单直接更新为 `REFUNDED`。本需求要把退单退款改造成支付商城与拼团营销协作的完整链路：

1. 支付商城由 `AliPayController#refundOrder` 接收用户退单申请。
2. 支付商城调用拼团营销的 `MarketTradeController#refundMarketPayOrder`，由拼团系统选择退单策略。
3. 拼团系统按未支付未成团、已支付未成团、已支付已成团三类场景更新拼团订单/队伍数据，并通过本地消息表补偿发送 `topic.team_refund` MQ。
4. 拼团系统自身消费该 MQ 恢复锁单库存；支付商城也消费同一 MQ，执行支付宝退款和支付订单状态变更。
5. 支付商城对用户接口返回“退单中”，最终退款结果由 MQ 异步推进到“已退单”。

术语纠正：

- “退单申请”更准确说法是“发起退单退款申请”。
- “解除组队锁单量/完成量”更准确说法是“扣减拼团队伍锁单量、完成量，必要时恢复库存”。
- “RedundSuccessTopicListener”应为 `RefundSuccessTopicListener`。
- “d RefundSuccessTopicListener”在支付商城侧建议命名为 `RefundSuccessTopicListener`，与拼团侧同名但包名不同。

### 1.1 业务边界

- 所属上下文：支付商城的订单/支付/退款，拼团营销的交易退单/库存恢复。
- 调用方向：
  - 入站：支付商城 `POST /api/v1/alipay/refund_order`。
  - 出站：支付商城调用拼团营销 `POST /api/v1/gbm/trade/refund_market_pay_order`。
  - MQ：拼团营销发布 `topic.team_refund`，拼团营销和支付商城各自监听。
- 是否涉及资金或订单状态：是。⚠️ 涉及支付宝退款、支付订单状态、拼团订单状态、MQ 最终一致性，必须人工复核。

## 2. 代码现状（Research Findings）
> 每个结论必须有代码出处（文件路径 + 类名/方法名）

### 2.1 支付商城相关入口与链路

- HTTP：支付商城退单入口已存在，`AliPayController#refundOrder` 接收 `RefundOrderRequestDTO`，调用 `IOrderService#refundOrder` 并返回 `RefundOrderResponseDTO`。见 `s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/AliPayController.java` 的 `refundOrder`。
- API 契约：`IPayService#refundOrder` 已定义退单接口。见 `s-pay-mall-ddd-lpc-api/src/main/java/top/licodetech/mall/api/IPayService.java`。
- API DTO：`RefundOrderRequestDTO` 当前只有 `userId`、`orderId`；`RefundOrderResponseDTO` 当前返回 `orderId`、`status`、`statusDesc`。见 `s-pay-mall-ddd-lpc-api/src/main/java/top/licodetech/mall/api/dto/RefundOrderRequestDTO.java` 和 `RefundOrderResponseDTO.java`。
- Domain Service：`OrderService#refundOrder` 当前校验用户、订单归属、可退状态后直接调用 `repository.refundOrder`，并把领域对象状态设为 `REFUNDED`。见 `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/OrderService.java`。
- Repository Port：`IOrderRepository#refundOrder` 当前语义是直接退单完成。见 `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/adapter/repository/IOrderRepository.java`。
- Infrastructure SQL：`pay_order_mapper.xml#refundOrder` 直接把 `PAY_WAIT`、`PAY_SUCCESS` 更新为 `REFUNDED`。见 `s-pay-mall-ddd-lpc-app/src/main/resources/mybatis/mapper/pay_order_mapper.xml`。
- MQ：支付商城当前只监听 `topic.team_success` 和 `topic.order_pay_success`；未配置/监听拼团退单 `topic.team_refund`。见 `TeamSuccessTopicListener`、`OrderPaySuccessListener` 和 `application-dev.yml` 的 `spring.rabbitmq.config.consumer`。
- 外部拼团网关：`IGroupBuyMarketService` 当前只定义锁单和结算，没有退单接口。见 `s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/gateway/IGroupBuyMarketService.java`。
- 外部拼团端口：`IProductPort` 当前只定义 `lockMarketPayOrder` 和 `settlementMarketPayOrder`，没有 `refundMarketPayOrder`。见 `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/adapter/port/IProductPort.java`。
- 支付宝退款能力：当前支付商城代码中未发现 `AlipayTradeRefundRequest` 或等价退款调用，只有支付创建和支付回调。见 `OrderService#doPrepayOrder` 和 `AliPayController#payNotify`。

### 2.2 拼团营销相关入口与链路

- HTTP：拼团营销已提供 `POST /api/v1/gbm/trade/refund_market_pay_order`，由 `MarketTradeController#refundMarketPayOrder` 校验参数并调用 `ITradeRefundOrderService#refundOrder`。见 `/home/lpc/project/group-buy-market/group-buy-market-lpc-trigger/src/main/java/top/licodetech/market/trigger/http/MarketTradeController.java`。
- API 契约：`IMarketTradeService#refundMarketPayOrder` 已定义。见 `/home/lpc/project/group-buy-market/group-buy-market-lpc-api/src/main/java/top/licodetech/market/api/IMarketTradeService.java`。
- API DTO：拼团退单请求字段为 `userId`、`outTradeNo`、`source`、`channel`；响应字段为 `userId`、`orderId`、`teamId`、`code`、`info`。见 `/home/lpc/project/group-buy-market/group-buy-market-lpc-api/src/main/java/top/licodetech/market/api/dto/RefundMarketPayOrderRequestDTO.java` 和 `RefundMarketPayOrderResponseDTO.java`。
- Domain Service：`TradeRefundOrderService#refundOrder` 通过 `tradeRefundRuleFiler` 执行退单规则链；`restoreTeamLockStock` 根据 MQ 中的退款类型选择策略恢复库存。见 `/home/lpc/project/group-buy-market/group-buy-market-lpc-domain/src/main/java/top/licodetech/market/domain/trade/service/refund/TradeRefundOrderService.java`。
- 规则链：`TradeRefundRuleFilterFactory#tradeRefundRuleFiler` 组装 `DataNodeFilter`、`UniqueRefundNodeFilter`、`RefundOrderNodeFilter`。见 `/home/lpc/project/group-buy-market/group-buy-market-lpc-domain/src/main/java/top/licodetech/market/domain/trade/service/refund/factory/TradeRefundRuleFilterFactory.java`。
- 策略选择：`RefundTypeEnumVO#getRefundStrategy` 根据拼团状态和交易订单状态选择 `unpaid2RefundStrategy`、`paid2RefundStrategy`、`paidTeam2RefundStrategy`。见 `/home/lpc/project/group-buy-market/group-buy-market-lpc-domain/src/main/java/top/licodetech/market/domain/trade/model/valobj/RefundTypeEnumVO.java`。
- 三类策略：`Unpaid2RefundStrategy`、`Paid2RefundStrategy`、`PaidTeam2RefundStrategy` 均会调用仓储并异步发送退单 MQ 通知。见 `/home/lpc/project/group-buy-market/group-buy-market-lpc-domain/src/main/java/top/licodetech/market/domain/trade/service/refund/business/impl/*RefundStrategy.java`。
- 本地消息表：`TradeRepository#unpaid2Refund`、`paid2Refund`、`paidTeam2Refund` 更新拼团订单/队伍状态，并插入 `NotifyTask`，`notifyMQ` 为 `topic.team_refund`，消息体包含 `type`、`userId`、`teamId`、`orderId`、`outTradeNo`、`activityId`。见 `/home/lpc/project/group-buy-market/group-buy-market-lpc-infrastructure/src/main/java/top/licodetech/market/infrastructure/adapter/repository/TradeRepository.java`。
- MQ 发送：`TradeTaskService#execNotifyJob` 调用 `ITradePort#groupBuyNotify`，`TradePort#groupBuyNotify` 对 MQ 类型调用 `EventPublisher#publish`。见 `/home/lpc/project/group-buy-market/group-buy-market-lpc-domain/src/main/java/top/licodetech/market/domain/trade/service/task/TradeTaskService.java` 和 `/home/lpc/project/group-buy-market/group-buy-market-lpc-infrastructure/src/main/java/top/licodetech/market/infrastructure/adapter/port/TradePort.java`。
- 拼团侧 MQ 消费：`RefundSuccessTopicListener#listener` 监听 `topic.team_refund`，调用 `ITradeRefundOrderService#restoreTeamLockStock`，失败抛异常触发 MQ 重试。见 `/home/lpc/project/group-buy-market/group-buy-market-lpc-trigger/src/main/java/top/licodetech/market/trigger/listener/RefundSuccessTopicListener.java`。
- MQ 配置：拼团侧 `application-dev.yml` 已配置 `group_buy_market_exchange`、`topic.team_success`、`topic.team_refund`。见 `/home/lpc/project/group-buy-market/group-buy-market-lpc-app/src/main/resources/application-dev.yml`。

### 2.3 发现与风险

- ⚠️ 支付商城当前退单状态一步到 `REFUNDED`，与目标“拼团处理完成后发送 MQ，支付商城监听后执行退款”冲突。
- ⚠️ 支付商城没有保存支付宝 `trade_no`，目前退款只能用商户订单号 `out_trade_no` 即本项目 `orderId` 发起支付宝退款；需确认支付宝沙箱是否允许仅用 `out_trade_no` 退款。
- ⚠️ 支付商城没有支付宝退款请求实现，需要新增退款端口或领域服务方法，不能把支付宝 SDK 调用放进 Listener 或 Controller。
- ⚠️ 拼团系统 `DataNodeFilter#apply` 对 `repository.queryMarketPayOrderEntityByOutTradeNo` 返回空没有显式保护，支付商城调用不存在的拼团单号时可能 NPE；本期如果只改支付商城，需要在支付商城侧只对 `marketType=GROUP_BUY_MARKET` 调拼团退单，并处理非成功响应。
- ⚠️ 拼团侧 `sendRefundNotifyMessage` 异步发送 MQ；HTTP 退单响应成功不等于 MQ 已被支付商城消费成功，需要支付商城把状态置为 `REFUNDING` 并依赖 MQ 最终一致性。
- ⚠️ `topic.team_refund` 会被拼团自身消费恢复库存，也会被支付商城消费退款；两个系统需要绑定到同一 exchange/routing key，但使用各自队列，避免互相抢消息。
- ⚠️ MQ 重复投递时，支付商城退款必须幂等：`REFUNDED` 直接忽略，`REFUNDING` 才允许发起支付宝退款或确认最终状态。

### 2.4 DDD 模块影响

| 模块 | 是否影响 | 文件/类 | 说明 |
|------|----------|---------|------|
| api | 否 | `RefundOrderResponseDTO` | 现有响应字段足够表达 `REFUNDING/REFUNDED`，未修改 API DTO |
| trigger | 是 | `AliPayController#refundOrder`、新增 `RefundSuccessTopicListener` | HTTP 入口只转换响应；MQ 入口解析消息并调用领域服务 |
| domain | 是 | `IOrderService`、`OrderService`、`OrderStatusVO`、`IProductPort`、`IOrderRepository` | 退款申请、退款成功确认、状态流转、拼团退单端口 |
| infrastructure | 是 | `IGroupBuyMarketService`、新增退单 DTO、`ProductPort`、`OrderRepository`、`IOrderDao`、mapper XML | 调拼团退单、支付宝退款适配、订单状态 SQL |
| types | 可能 | 常量/错误码 | 如需新增响应码或消息 DTO 放通用层 |
| app | 是 | `application-dev.yml`、`application-prod.yml`、`pom.xml` | 新增 `topic_team_refund` 消费配置；调整 app 测试开关使单测可执行 |

## 3. 功能点

- [x] 功能 1：用户发起退单申请，支付商城校验订单归属与可退状态；普通订单直接进入支付退款流程，拼团订单先调用拼团营销退单接口。
- [x] 功能 2：支付商城调用拼团营销 `refund_market_pay_order`，成功或重复成功后把支付订单状态更新为 `REFUNDING` 并返回用户“退单中”。
- [x] 功能 3：拼团营销根据状态组合完成退单策略并发送 `topic.team_refund` MQ；支付商城新增监听器消费该 MQ。
- [x] 功能 4：支付商城监听 `topic.team_refund` 后，按 `outTradeNo/orderId` 查询订单，执行模拟退款，成功后把订单状态更新为 `REFUNDED`。
- [x] 功能 5：MQ 重复投递时，支付商城侧保证退款幂等；已 `REFUNDED` 不重复退款，非 `REFUNDING` 状态按规则拒绝或忽略。
- [x] 功能 6：补充单元测试和编译校验，覆盖订单状态流转、退款成功确认和重复退款幂等。

## 4. 业务规则

- 订单状态流转：
  - 普通订单：`PAY_WAIT/PAY_SUCCESS` → `REFUNDING` → `REFUNDED`。
  - 拼团订单：`PAY_WAIT/PAY_SUCCESS/MARKET/DEAL_DONE` → 支付商城请求拼团退单成功 → `REFUNDING` → 消费 `topic.team_refund` → 模拟退款成功 → `REFUNDED`。
  - 已 `REFUNDED`：退单申请或 MQ 重复消费返回/处理为幂等成功。
  - `PAY_WAIT` 未支付订单也等待拼团 `topic.team_refund` 后再变 `REFUNDED`，保证流程一致性。
  - `MARKET`、`DEAL_DONE` 允许该用户退单；如果队伍中所有人都退单，则由拼团侧策略把拼团置为失败。
  - `CLOSE/CREATE` 是否允许退单仍按当前规则谨慎处理，默认不开放。
- 支付/退款规则：
  - 本期不真实调用支付宝退款接口，先模拟退款成功完成链路。
  - 支付宝退款标识使用 `out_trade_no = orderId`，不新增落库 `trade_no`。
  - 未支付订单不需要真实退款，但也必须等待拼团 MQ 后再完成订单状态变更。
  - 退款金额使用 `OrderEntity#payAmount`，无值时需兜底 `totalAmount` 或拒绝退款，必须避免金额为 null。
- 拼团营销规则：
  - 支付商城只对 `MarketTypeVO.GROUP_BUY_MARKET` 调拼团退单接口。
  - 拼团退单响应 `Response.code=0000` 且 data.code 为 `success` 或 `repeat` 才视作可进入 `REFUNDING`。
  - 拼团退单失败时，支付商城订单保持原状态，不进入 `REFUNDING`。
- 幂等规则：
  - 支付商城退单申请：`REFUNDING/REFUNDED` 需要给出幂等响应。
  - 支付商城退款 MQ：同一 `outTradeNo` 重复 MQ 不得重复发起支付宝退款。
  - 拼团侧已有 `UniqueRefundNodeFilter` 对 `TradeOrderStatusEnumVO.CLOSE` 返回重复退单。
- 异常与补偿：
  - 拼团 MQ 已由本地消息表和 `GroupBuyNotifyJob` 补偿。
  - 本期模拟退款成功，支付商城暂不新增本地退款补偿表/Job；后续接入真实支付宝退款时再评估退款补偿表。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 修改 SQL | `pay_order` | `status` | 新增/使用 `REFUNDING` 中间态，`REFUNDED` 作为最终态 |
| 暂不新增字段 | `pay_order` | `refund_time`、`refund_amount`、`refund_no`、`alipay_trade_no` | 本期模拟退款成功，只使用 `out_trade_no = orderId` |
| 暂不新增表 | `pay_refund_task` 或本地消息表 | 无 | 本期不接真实支付宝退款，暂不做支付商城本地退款补偿 |

- 是否需要改 MyBatis mapper：是。
- 是否需要改初始化 SQL：如新增字段/表则是；仅改状态 SQL 则不一定。

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 修改语义 | `/api/v1/alipay/refund_order` | POST | 从同步已退单改为发起退单申请并返回 `REFUNDING` |
| 新增出站 | `/api/v1/gbm/trade/refund_market_pay_order` | POST | 支付商城 Retrofit 调用拼团退单 |
| 新增 MQ 消费 | `topic.team_refund` | RabbitMQ Topic | 支付商城监听拼团退单成功消息并执行退款 |

### 6.1 REST API 契约

| 方向 | 服务 | Path | Method | Request DTO | Response DTO | 鉴权/签名 |
|------|------|------|--------|-------------|--------------|-----------|
| 入站 | 支付商城 | `/api/v1/alipay/refund_order` | POST | `RefundOrderRequestDTO(userId, orderId)` | `RefundOrderResponseDTO(orderId,status,statusDesc)` | 当前无，沿用现状 |
| 出站 | 拼团营销 | `/api/v1/gbm/trade/refund_market_pay_order` | POST | `RefundMarketPayOrderRequestDTO(userId,outTradeNo,source,channel)` | `RefundMarketPayOrderResponseDTO(userId,orderId,teamId,code,info)` | 当前无，内网配置地址 |

### 6.2 MQ 契约

| 方向 | Exchange | Routing Key | Queue | Message | 幂等键 |
|------|----------|-------------|-------|---------|--------|
| 拼团发布，拼团消费 | `group_buy_market_exchange` | `topic.team_refund` | `group_buy_market_queue_2_topic_team_refund` | `TeamRefundSuccess(type,userId,teamId,activityId,orderId,outTradeNo)` | `orderId/outTradeNo` |
| 拼团发布，支付消费 | `group_buy_market_exchange` | `topic.team_refund` | 建议 `s_pay_mall_queue_2_topic_team_refund` | 同上，支付商城新增 DTO 或复用字段解析 | `outTradeNo` 即支付商城 `orderId` |

## 7. 影响范围

- 前端页面：退单按钮后状态可能显示“退单中”，最终状态异步变成“已退单”。
- 外部服务：依赖 `/home/lpc/project/group-buy-market` 的退单接口和 `topic.team_refund` MQ。
- 定时任务：拼团侧已有 `GroupBuyNotifyJob` 补偿本地消息；支付商城本期模拟退款，暂不新增本地退款补偿任务。
- MQ 消费者/生产者：支付商城新增 `topic.team_refund` 消费者；拼团侧已有生产者和自身消费者。
- 运维配置：支付商城 `application-dev/test/prod.yml` 需要新增 `spring.rabbitmq.config.consumer.topic_team_refund`。

## 8. 风险与关注点
> ⚠️ 涉及资金/状态流转/权限变更必须标注

- ⚠️ 资金风险：支付宝退款失败、重复退款、退款金额错误。
- ⚠️ 状态流转风险：支付商城当前 `REFUNDED` 一步到位，需改为 `REFUNDING` 中间态，否则 MQ 异步链路无落点。
- ⚠️ 外部服务失败风险：拼团退单 HTTP 成功但 MQ 未发送、HTTP 失败但拼团状态已变化、网络超时返回不确定。
- ⚠️ 重复请求/重复消息风险：用户重复点击退单、拼团重复发送 `topic.team_refund`、RabbitMQ 重试。
- ⚠️ 数据一致性风险：本期模拟退款成功，真实支付宝退款失败补偿暂不处理，后续接入真实退款时必须补齐。

## 8.5 测试策略

- **测试范围**：
  - `domain`：优先单元测试 + Mock，覆盖 `OrderService#refundOrder`、退款成功确认、状态幂等。
  - `infrastructure`：Mock Retrofit，模拟退款端口成功，验证拼团退单响应转换。
  - `trigger`：SpringBootTest 或 MockMvc 验证 Controller 响应；RabbitMQ Listener 可单元测试消息解析。
  - SQL：必要时用 SpringBoot 集成测试验证 mapper 更新状态。
- **覆盖率目标**：覆盖退单申请成功/失败、拼团失败、重复退单、MQ 重复消费、模拟退款成功。
- **独立 Test Spec**：是，建议基于 `code_copilot/changes/templates/test-spec.md` 创建 `test-spec.md`。
- **优先验证命令**：
  - `mvn -pl s-pay-mall-ddd-lpc-app -DskipTests=false -Dtest=OrderServiceRefundTest test`
  - `mvn -pl s-pay-mall-ddd-lpc-app -DskipTests=false test`

## 9. 待澄清

- [x] 支付商城最终是否要真实调用支付宝 `AlipayTradeRefundRequest`，还是本期先模拟退款成功只做状态变更？结论：本期先模拟退款成功，只完成链路。
- [x] 支付宝退款是否只使用 `out_trade_no=orderId`，还是需要在支付回调时落库保存 `trade_no`？结论：只使用 `out_trade_no = orderId`，不落库 `trade_no`。
- [x] `PAY_WAIT` 未支付订单是否需要等待拼团 MQ 后再变 `REFUNDED`，还是可在退单申请时直接关闭/退单完成？结论：也等待拼团 MQ，保证流程一致性。
- [x] `MARKET`、`DEAL_DONE` 状态是否允许用户退单？当前 `OrderStatusVO#canRefund` 不允许。结论：允许用户退单；如果队伍中所有人都退单，则拼团失败。
- [x] 支付商城是否新增本地退款任务表/补偿 Job，还是依赖 MQ 重试和人工处理？结论：本期模拟退款，暂不新增支付商城本地退款补偿表/Job。
- [x] 是否需要同步修改 `/home/lpc/project/group-buy-market`？结论：本期主任务先改支付商城，最后一个 task 再做拼团侧两个小补强。

### 9.1 是否需要修改 group-buy-market 的原因说明

本需求的主实现可以只改支付商城，因为拼团侧退单接口、退单策略、本地消息表和 `topic.team_refund` 已经存在。

建议考虑修改 `/home/lpc/project/group-buy-market` 的原因只有两个：

1. **防御性修复**：`DataNodeFilter#apply` 对 `repository.queryMarketPayOrderEntityByOutTradeNo` 返回空没有显式判断，如果支付商城传入不存在的 `outTradeNo`，拼团侧可能出现空指针而不是稳定业务响应。修复它可以让跨服务接口更可靠。
2. **MQ 绑定显式化**：拼团侧 `RefundSuccessTopicListener` 通过 `@RabbitListener` 能声明队列绑定，但 `RabbitMQConfig` 当前只显式声明了 `topic_team_success` 的 Binding，没有显式声明 `topic_team_refund` 的 Binding。补上可以让配置更清晰，降低环境差异风险。

这两个修改不是支付商城联调的绝对前置条件。本期执行顺序确定为：先完成支付商城主链路，再在最后一个 task 中修改拼团项目的两个小补强。

## 10. 技术决策

| 决策点 | 选择 | 备选 | 理由 |
|--------|------|------|------|
| 支付商城调用拼团方式 | Retrofit2 `IGroupBuyMarketService` 新增退单方法 | Feign/WebClient | 当前项目已有 Retrofit2 配置和锁单/结算调用 |
| 退单中间状态 | 使用 `OrderStatusVO.REFUNDING` | 直接 `REFUNDED` | 需要等待拼团 MQ 和支付宝退款最终结果 |
| MQ 消费位置 | `trigger/listener/RefundSuccessTopicListener` | Controller/Job | 符合 trigger 接入协议、domain 处理业务 |
| 退款实现 | 本期模拟退款成功，保留端口抽象 | 直接真实调用支付宝退款 | 当前沙箱退款回调/能力不确定，先打通链路 |
| 支付商城消费队列 | 新增独立队列 `s_pay_mall_queue_2_topic_team_refund` | 复用拼团队列 | 多消费者系统必须各自队列，避免抢消息 |

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Research | done | 本 spec | 已分析支付商城和拼团营销现有代码 |
| Task 1 | done | `IGroupBuyMarketService`、`RefundMarketPayOrder*DTO`、`IProductPort`、`ProductPort` | 支付商城补齐拼团退单出站契约 |
| Task 2 | done | `OrderStatusVO`、`IOrderService`、`OrderService`、`IOrderRepository`、`OrderRepository` | 退单申请改为 `REFUNDING` 中间态和 MQ 后确认 |
| Task 3 | done | `IOrderDao`、`pay_order_mapper.xml`、`application-dev.yml`、`application-prod.yml` | 补齐状态 SQL 和 MQ 消费配置 |
| Task 4 | done | `RefundSuccessTopicListener`、`IRefundPort`、`RefundPort` | 支付商城消费拼团退款 MQ 并模拟退款成功 |
| Task 5 | done | `OrderServiceRefundTest`、`test-spec.md`、`s-pay-mall-ddd-lpc-app/pom.xml` | 单测扩展并修正测试开关 |
| Task 6 | done | `/home/lpc/project/group-buy-market/.../DataNodeFilter.java`、`/home/lpc/project/group-buy-market/.../RabbitMQConfig.java` | 拼团侧空值保护和 MQ Binding 补强 |

## 12. 审查结论

已按用户确认进入 apply 并完成实现。需要真实支付宝退款时，应新开 Spec 补充真实退款端口、失败补偿和集成测试。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-05-07
- **确认人**：用户在对话中确认“根据 code_copilot 开始执行”
