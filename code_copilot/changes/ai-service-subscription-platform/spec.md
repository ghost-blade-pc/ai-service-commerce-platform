# AI 服务订阅与营销平台适配
> status: done
> created: 2026-05-11
> scope: cross-project
> complexity: 复杂

## 1. 背景与目标

当前系统的核心业务语义仍是传统商品商城拼团：商城侧创建商品订单、支付宝支付、支付成功后模拟发货；营销侧围绕商品 `goods_id` 做拼团活动、锁单、成团结算和退单回滚。

本 change 目标是基于现有交易链路，把业务场景重新适配为 **AI 服务订阅与营销平台**：

- 商城侧从“商品下单/模拟发货”演进为“AI 服务套餐订阅、支付、履约、退订退款”。
- 营销侧从“商品拼团优惠”演进为“大模型调用额度套餐的多人拼团采购营销”。
- 首期重点仍放在交易履约层：普通订阅、拼团采购、支付宝支付、成团结算、调用额度开通、退订退款、状态一致性、MQ 解耦和补偿可靠性。
- 后续可对接 AI Chat、Agent 工作流、MCP 工具网关，但本 change 不售卖 API Key、智能体权限、工作流模板权限或 MCP 工具访问权限，这些能力由后续 AI Agent 项目承接。

完成后应能用更贴近 AI 服务订阅场景的领域语言表达现有交易闭环，同时保留当前订单、拼团、支付宝交易三方一致性机制。

## 2. 业务边界

- 影响项目：`s-pay-mall-ddd`、`group-buy-market`
- 所属上下文：订单 / 支付 / 退款 / 拼团营销 / 权益履约 / 配置
- 调用方向：
  - 商城侧接收用户订阅下单请求。
  - 商城侧通过 `servicePackageId` 表达用户购买的大模型调用额度套餐，对内兼容营销侧 `goodsId/sku`。
  - 营销侧基于现有 `sku/sc_sku_activity/group_buy_activity/group_buy_discount` 等表维护可售卖套餐和拼团活动配置，是首期服务套餐目录来源。
  - 商城侧通过 Retrofit 调用营销侧锁单、结算、退单接口。
  - 营销侧通过 RabbitMQ 通知拼团成团与拼团退单。
  - 商城侧通过 RabbitMQ 消费支付成功与成团消息，触发权益履约。
  - 商城侧通过 Job 补偿支付回调、超时关单、退款任务。
- 是否跨项目：是。
- 是否涉及资金、订单状态、拼团状态：是，高风险。

## 3. 代码现状（Research Findings）

### 3.1 相关入口

- HTTP:
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/AliPayController.java`：当前创建支付单、退款、用户订单查询等入口仍使用 `productId`、`productName`、`marketType` 语义。
  - `group-buy-market/group-buy-market-lpc-trigger/src/main/java/top/licodetech/market/trigger/http/MarketIndexController.java`：`query_group_buy_market_config` 以 `goodsId/source/channel` 查询拼团营销配置与可参团队伍。
  - `group-buy-market/group-buy-market-lpc-trigger/src/main/java/top/licodetech/market/trigger/http/MarketTradeController.java`：提供 `lock_market_pay_order`、`settlement_market_pay_order`、`refund_market_pay_order` 三个核心交易接口。
- MQ:
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/listener/OrderPaySuccessListener.java`：消费 `topic.order_pay_success` 后调用 `IGoodsService#changeOrderDealDone`，当前语义是模拟发货。
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/listener/TeamSuccessTopicListener.java`：消费 `topic.team_success` 后解析 `NotifyRequestDTO.outTradeNoList`，推进营销结算。
  - `group-buy-market/group-buy-market-lpc-trigger/src/main/java/top/licodetech/market/trigger/listener/RefundSuccessTopicListener.java` 与 `s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/listener/RefundSuccessTopicListener.java`：处理 `topic.team_refund` 相关退单消息。
- Job:
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/job/NoPayNotifyOrderJob.java`：支付回调兜底查单。
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/job/TimeoutCloseOrderJob.java`：超时未支付关单。
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/job/RefundTaskJob.java`：退款任务补偿。
  - `group-buy-market/group-buy-market-lpc-trigger/src/main/java/top/licodetech/market/trigger/job/GroupBuyNotifyJob.java`、`TimeoutRefundJob.java`：营销通知与超时退单补偿。
- Domain Service:
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/AbstractOrderService.java`：创建订单，必要时调用营销锁单，再创建支付宝预支付。
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/OrderService.java`：支付成功、营销结算、退款和退款完成的核心编排。
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/goods/service/GoodsService.java`：当前只把订单状态改为 `DEAL_DONE`。
  - `group-buy-market/group-buy-market-lpc-domain/src/main/java/top/licodetech/market/domain/trade/service/ITradeLockOrderService.java`、`ITradeSettlementOrderService.java`、`ITradeRefundOrderService.java`：营销锁单、结算、退单领域服务。
- Repository/Port/Gateway:
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/adapter/port/ProductPort.java`：查询商品、调用营销锁单/结算/退单。
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/gateway/ProductRPC.java`：当前商品查询是硬编码示例，返回 `MyBatisBook` 和 `100.00`。
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/gateway/IGroupBuyMarketService.java`：Retrofit 契约固定为 `api/v1/gbm/trade/*`。
  - `group-buy-market/group-buy-market-lpc-infrastructure/src/main/java/top/licodetech/market/infrastructure/adapter/repository/TradeRepository.java`：维护拼团订单、订单明细、通知任务、退单消息。
- Config:
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/resources/application-dev.yml`：配置 `topic.order_pay_success`、`topic.team_success`、`topic.team_refund` 和 `app.config.group-buy-market.*`。
  - `group-buy-market/group-buy-market-lpc-app/src/main/resources/application-dev.yml`：配置 `group_buy_market_exchange`、`topic.team_success`、`topic.team_refund`。

### 3.2 现有实现

- 商城侧创建支付单以 `CreatePayRequestDTO.productId` 作为订购对象标识；`AbstractOrderService#createOrder` 通过 `IProductPort#queryProductByProductId` 查询产品，再通过 `IProductPort#lockMarketPayOrder` 对接拼团营销。
- 商城侧 `OrderService#doPrepayOrder` 使用支付宝 `AlipayTradePagePayRequest`，`subject` 当前取 `productName`，支付金额取原价或营销优惠后的 `MarketPayDiscountEntity.payPrice`。
- 商城侧订单状态由 `OrderStatusVO` 统一表达，已有 `CREATE`、`PAY_WAIT`、`PAY_SUCCESS`、`MARKET`、`DEAL_DONE`、`CLOSE`、`REFUNDING`、`REFUNDED`。
- 商城侧普通支付成功订单直接从支付成功消息推进到 `DEAL_DONE`；拼团订单先由支付成功推进到 `PAY_SUCCESS` 并调用营销结算，成团后由 `topic.team_success` 推进到 `MARKET`，再发布后续履约消息。
- 商城侧 `pay_order_mapper.xml` 中订单主表仍是 `product_id/product_name/total_amount/market_type/market_deduction_amount/pay_amount` 模型，退款允许从 `PAY_WAIT/PAY_SUCCESS/MARKET/DEAL_DONE` 进入退单。
- 营销侧以 `goods_id`、`sku`、`sc_sku_activity`、`group_buy_activity` 组织商品与拼团活动关系；`group_buy_order_list.out_trade_no` 是外部商城订单号幂等锚点。
- 营销侧 `notify_task.parameter_json` 承载 `topic.team_success` 与 `topic.team_refund` 消息，`notify_task.uuid` 当前只有普通索引 `KEY uq_uuid`，不是数据库唯一约束。

### 3.3 发现与风险

- 当前“商品/发货”命名已经不能准确表达 AI 服务订阅，但底层交易链路可复用。直接全量重命名 `product/goods` 会造成跨项目 API、SQL、前端、测试大范围破坏，建议采用兼容性适配策略。
- 当前调用额度履约没有独立模型，`GoodsService#changeOrderDealDone` 只更新订单状态；API Key、智能体权限、工作流模板权限和 MCP 工具访问权限属于后续 AI Agent 项目范围，不进入首期实现。
- 当前 `ProductRPC` 是早期硬编码示例，不应作为 AI 服务套餐目录来源。首期服务套餐目录仍以营销侧数据库表及其现有查询/锁单链路为准，商城侧不新增第二套套餐主数据，只保存订单快照和权益履约所需数据。
- 支付、退款、拼团成团、MQ 和本地补偿均为高风险路径，不能把“权益开通失败”混入支付成功事务内导致支付链路不稳定，应通过本地履约任务或幂等履约记录解耦。
- 首期权益类型仅包含大模型调用额度；不得在本 change 中售卖或开通 API Key、智能体权限、工作流模板权限、MCP 工具访问权限。
- 权益开通需要明确幂等键，推荐以 `orderId + servicePackageId` 作为核心幂等维度，避免重复发放调用额度。

## 4. DDD 模块影响

| 项目 | 模块 | 是否影响 | 文件/类 | 说明 |
| --- | --- | --- | --- | --- |
| s-pay-mall-ddd | api | 是 | `CreatePayRequestDTO`、订单列表 DTO、可能新增订阅 DTO | 新增 `servicePackageId` 作为公开订购字段，过渡期可兼容 `productId`。 |
| s-pay-mall-ddd | trigger | 是 | `AliPayController`、`OrderPaySuccessListener`、`TeamSuccessTopicListener`、退款 Listener/Job | 下单、成团、履约、退订退款入口需要使用订阅语义和幂等策略。 |
| s-pay-mall-ddd | domain | 是 | `order`、当前 `goods` 履约服务，可能新增 `entitlement/subscription` 上下文 | 订单状态可复用，但履约规则应从模拟发货升级为权益开通。 |
| s-pay-mall-ddd | infrastructure | 是 | `ProductPort`、`ProductRPC`、`OrderRepository`、Mapper、可能新增权益 Repository/Port | 停止依赖 `ProductRPC` 的硬编码套餐；通过营销侧目录/活动查询链路获取套餐信息，并新增权益开通、履约任务、退款补偿适配。 |
| s-pay-mall-ddd | app | 是 | `application-*.yml`、MyBatis mapper、SQL、测试 | 新配置、SQL、MQ、任务调度和测试装配。 |
| s-pay-mall-ddd | types | 可能 | 状态/事件/常量 | 若新增权益类型、履约状态或事件常量，应放公共类型。 |
| group-buy-market | api | 是 | `GoodsMarket*DTO`、`LockMarketPayOrder*DTO` 等 | 可先兼容 `goodsId` 字段并在文档/DTO 注释中定义为服务包标识，必要时新增别名字段。 |
| group-buy-market | trigger | 是 | `MarketIndexController`、`MarketTradeController` | 拼团配置、锁单、结算、退单入口需适配 AI 服务订阅术语。 |
| group-buy-market | domain | 是 | activity/trade 领域服务 | 活动、SKU、试算、锁单、结算规则可复用，但需明确套餐购买限制和可退规则。 |
| group-buy-market | infrastructure | 是 | `TradeRepository`、SKU/活动 mapper、通知任务 | 服务包活动关联、通知任务幂等和消息体可能调整。 |
| group-buy-market | app | 是 | SQL、配置、测试 | 初始化数据从图书商品改为 AI 服务套餐；MQ 配置原则上复用。 |
| group-buy-market | types | 可能 | 枚举/常量 | 若新增营销品类或退单行为类型，需要补充。 |
| 前端（nginx） | — | 是 | `index.html`、`order-list.html`、`login.html`、`css/index.css`、`js/index.js` | 页面标题、商品名、按钮文案、支付弹窗、空状态提示改为 AI 服务订阅语义；下单请求增加 `servicePackageId`。 |

## 5. 功能点

- [x] 功能 1：服务套餐目录适配。把商城侧订购对象定义为 AI 大模型调用额度套餐，公开 API 新增 `servicePackageId`，对内映射营销侧 `goodsId/sku`；套餐名称、价格、额度、活动优惠来源于营销侧数据库及现有营销查询链路，不在商城侧新增第二套目录主数据。
- [x] 功能 2：营销活动适配。把营销侧 `goods_id/sku/sc_sku_activity` 定义为可参与拼团的大模型调用额度套餐标识，初始化数据改为不同额度档位的 AI 服务套餐。
- [x] 功能 3：订阅下单与支付链路适配。保留当前支付宝预支付、订单金额、营销抵扣、支付成功回调与补偿机制，接口响应和日志改为订阅语义。
- [x] 功能 4：成团后额度权益开通。把 `OrderPaySuccessListener` 当前模拟发货替换为额度履约入口；普通订阅支付成功后开通额度，拼团订阅成团结算后开通额度。
- [x] 功能 5：额度履约幂等与补偿。新增本地权益记录和履约任务，记录总额度、已用额度、剩余额度、履约状态和重试状态；MQ 重复消费不能重复增加额度。
- [x] 功能 6：退订退款。保留当前退款与拼团退单机制，退订时撤销未使用额度；已消耗额度按比例退款；履约失败重试一段时间后自动退款，让用户侧尽量无感。
- [x] 功能 7：展示和文档语义同步。本次覆盖日志、SQL 示例数据、测试语义和 change 文档。
- [x] 功能 8：前端静态页适配 AI 服务订阅场景。把 `docs/dev-ops/nginx/html/` 中商品拼团商城的页面（index.html、order-list.html、login.html、css/index.css、js/index.js）调整为 AI 大模型调用额度套餐订阅平台的视觉和文案。

## 6. 业务规则

- 状态流转：
  - 普通订阅建议保持 `CREATE -> PAY_WAIT -> PAY_SUCCESS -> DEAL_DONE`，其中 `DEAL_DONE` 重新定义为“权益开通完成”。
  - 拼团订阅建议保持 `CREATE -> PAY_WAIT -> PAY_SUCCESS -> MARKET -> DEAL_DONE`，其中 `MARKET` 表示拼团成团并完成营销结算，`DEAL_DONE` 表示权益开通完成。
  - `REFUNDING/REFUNDED/CLOSE` 继续作为退订退款和超时关闭状态，但已开通权益的退订需要补充权益撤销策略。
- 金额/优惠/退款：
  - 原价、营销抵扣、实付金额继续使用 `BigDecimal`，分别对应 `totalAmount`、`marketDeductionAmount`、`payAmount`。
  - 退款金额以 `payAmount` 为基准。未消耗额度时可全额退款；已消耗额度时按 `剩余额度 / 总额度` 计算可退金额，金额保留 2 位小数，四舍五入，最低退款金额为 0.01 元；已消耗额度从商城侧额度权益表读取。
  - 服务订阅的周期、额度、权益类型不能与金额字段混用，应独立建模。
- 服务套餐目录：
  - 营销侧继续作为可售卖套餐与拼团活动配置来源，商城侧不新增套餐目录主数据。
  - 商城侧订单需保存套餐快照，包括 `servicePackageId`、套餐名称、购买额度、原价、优惠后实付金额等履约和退款所需字段。
  - `ProductRPC` 的硬编码商品示例应被移除或旁路，不再返回 `MyBatisBook` 作为订购对象。
- 拼团营销：
  - 营销侧 `activityId/teamId/outTradeNo` 继续作为拼团活动、队伍、商城订单关联键。
  - 首期不改变 `lock_market_pay_order`、`settlement_market_pay_order`、`refund_market_pay_order` 路径，降低跨项目联调风险。
  - 若新增 `servicePackageId`，必须兼容现有 `goodsId/productId`，避免前端和现有测试同时失效。
- 幂等：
  - 订单幂等继续依赖商城侧 `orderId` 和营销侧 `outTradeNo`。
  - 权益开通必须新增明确幂等记录，建议键：`orderId + servicePackageId`。
  - MQ 消费必须允许重复消息，重复消费不能重复发放额度。
- 权益履约：
  - 首期权益类型仅为大模型调用额度，记录总额度、已用额度、剩余额度、有效状态和来源订单。
  - 支付或成团后的额度开通失败不回滚支付成功状态，应进入履约任务重试。
  - 履约任务最多重试 3 次，重试间隔 1 秒；从首次失败起总计 5 秒内仍未履约成功则自动发起退款；退款任务需与既有订单退款状态机保持一致。
  - 退订退款需要先冻结或撤销未使用额度，避免退款后继续消耗。
- 异常与补偿：
  - 支付成功后权益开通失败不应回滚支付成功状态，应进入本地履约任务重试；超过重试上限后自动退款。
  - 成团消息先于本地状态提交、退款消息先于 `REFUNDING` 提交等既有时序风险仍需保留短重试或任务补偿。
  - 首期不调用外部 AI Agent、工作流或 MCP 工具网关；后续对接时必须记录可重试错误，不输出 token/API Key 明文。

## 7. 数据变更

| 操作 | 项目 | 表名 | 字段/索引 | 说明 |
| --- | --- | --- | --- | --- |
| 修改初始化数据 | group-buy-market | `sku` | `goods_name/original_price` | 把示例商品从图书改为 AI 服务套餐。 |
| 修改初始化数据 | group-buy-market | `sc_sku_activity`、`group_buy_activity`、`group_buy_discount` | `goods_id/activity_name/discount_name` | 让拼团活动与 AI 服务套餐关联。 |
| 新增 | s-pay-mall-ddd | `subscription_entitlement` 或等价表 | `order_id`、`user_id`、`service_package_id`、`total_quota`、`used_quota`、`remaining_quota`、`status`、唯一索引 | 记录大模型调用额度权益、幂等开通、撤销和按比例退款依据。 |
| 新增 | s-pay-mall-ddd | `subscription_fulfillment_task` 或等价表 | `order_id`、`service_package_id`、`status`、`retry_count`、`next_retry_time`、`fail_reason`、唯一索引 | 记录额度履约补偿任务；超过重试上限后触发自动退款。可参考 `pay_refund_task`。 |
| 可能修改注释/文档 | s-pay-mall-ddd | `pay_order` | `product_id/product_name` 注释 | 可先保持字段名不变，把业务语义定义为服务套餐标识和名称。 |

- 是否需要改 MyBatis mapper：需要，新增权益表和履约任务表对应 DAO/PO/Mapper。
- 是否需要改初始化 SQL：需要，至少要把示例数据调整为 AI 服务订阅场景。

## 8. 接口与消息变更

### 8.1 REST API

| 方向 | 服务 | Path | Method | Request DTO | Response DTO | 鉴权/签名 |
| --- | --- | --- | --- | --- | --- | --- |
| 入站 | s-pay-mall-ddd | `/api/v1/alipay/create_pay_order` | POST | `CreatePayRequestDTO` 新增 `servicePackageId` | 当前支付响应 | 当前未见强鉴权，首期沿用现有用户来源策略，后续安全加固单独建 change。 |
| 入站 | s-pay-mall-ddd | `/api/v1/alipay/refund_order` | POST | `RefundOrderRequestDTO` | `RefundOrderResponseDTO` | 必须校验用户与订单归属，当前由 `OrderService#refundOrder` 校验。 |
| 出站 | s-pay-mall-ddd -> group-buy-market | `api/v1/gbm/trade/lock_market_pay_order` | POST | `LockMarketPayOrderRequestDTO` | `LockMarketPayOrderResponseDTO` | 内部调用，当前无签名；是否增加内部鉴权待确认。 |
| 出站 | s-pay-mall-ddd -> group-buy-market | `api/v1/gbm/trade/settlement_market_pay_order` | POST | `SettlementMarketPayOrderRequestDTO` | `SettlementMarketPayOrderResponseDTO` | 内部调用，当前无签名；失败依赖异常与补偿。 |
| 出站 | s-pay-mall-ddd -> group-buy-market | `api/v1/gbm/trade/refund_market_pay_order` | POST | `RefundMarketPayOrderRequestDTO` | `RefundMarketPayOrderResponseDTO` | 内部调用，`success/repeat` 视为成功语义。 |

### 8.2 MQ 契约

| 方向 | Exchange | Routing Key | Queue | Message | 幂等键 |
| --- | --- | --- | --- | --- | --- |
| 商城生产/商城消费 | `s_pay_mall_exchange` 或当前配置值 | `topic.order_pay_success` | `s_pay_mall_queue_2_order_pay_success` | `PaySuccessMessageEvent.PaySuccessMessage` | `tradeNo/orderId` |
| 营销生产/商城消费 | `group_buy_market_exchange` 或当前配置值 | `topic.team_success` | `s_pay_mall_queue_2_topic_team_success` | `NotifyRequestDTO{teamId,outTradeNoList}` | `teamId + outTradeNoList` |
| 营销生产/双方消费 | `group_buy_market_exchange` 或当前配置值 | `topic.team_refund` | 双方队列 | 当前退款消息 JSON，包含 `type/orderId/teamId/activityId/userId` | `orderId + type` |
| 可能新增 | 待确认 | 待确认 | 待确认 | 额度权益开通/撤销事件 | `orderId + servicePackageId` |

首期建议优先复用 `topic.order_pay_success` 与 `topic.team_success` 作为额度履约触发，不新增 MQ topic；只有当后续 AI Agent 项目需要独立消费权益事件时，再新增权益事件。

## 9. 测试策略

- 测试范围：
  - 商城侧订阅下单、普通支付成功后额度开通、拼团成团后额度开通、退订退款、权益重复开通幂等。
  - 营销侧服务包拼团配置查询、锁单、结算、退单消息。
  - 跨项目消息体和状态推进兼容性。
- 优先测试类型：
  - 领域服务测试：订单状态、退款可退规则、额度开通幂等、按比例退款计算。
  - Repository/Mapper 测试：新增权益表或任务表的 insertIgnore/lock/retry/update。
  - Listener 测试：重复 `topic.order_pay_success`、重复 `topic.team_success`、重复 `topic.team_refund`。
  - 端到端手工验证：普通订阅、开团、参团成团、退订退款。
- 需要独立 `test-spec.md`：是，进入 `/apply` 前创建。
- 推荐验证命令：
  - `cd s-pay-mall-ddd && mvn -pl s-pay-mall-ddd-lpc-app -am -DskipTests=false -DfailIfNoTests=false test`
  - `cd group-buy-market && mvn -pl group-buy-market-lpc-app -am -DskipTests=false -DfailIfNoTests=false test`

## 10. 待澄清

- [x] 首期新增额度权益表、履约任务表和领域端口；不只做文案语义适配。
- [x] AI 服务套餐目录来源为营销侧数据库表及现有营销查询/锁单链路，商城侧不新增第二套套餐目录主数据。
- [x] 首期权益类型仅为大模型调用额度；API Key、智能体权限、工作流模板权限、MCP 工具访问权限不进入首期售卖内容。
- [x] 权益开通失败后进入履约任务重试；超过重试上限后自动退款。
- [x] 退订退款需要撤销未使用权益；已消耗额度按比例退款。
- [x] 公开 API 新增 `servicePackageId`，对内兼容既有 `productId/goodsId` 映射。
- [x] 首期不新增内部接口签名、白名单或完整登录态改造，沿用现有调用边界；接口安全加固后续单独建 change。
- [x] 履约任务重试次数为 3 次，重试间隔 1 秒，从首次失败起总计 5 秒内仍未履约成功则自动退款。
- [x] 按比例退款金额保留 2 位小数，四舍五入，最低退款金额 0.01 元，已消耗额度从商城侧额度权益表读取。

## 11. 技术决策

| 决策点 | 选择 | 备选 | 理由 |
| --- | --- | --- | --- |
| 业务适配策略 | 公开 API 新增 `servicePackageId`，对内兼容既有 `productId/goodsId` 映射 | 全量重命名所有 API、SQL、Mapper 字段 | 全量重命名会同时影响 API、SQL、Mapper、前端、营销侧 DTO 和测试，风险高；先把外部语义升级为服务套餐 ID。 |
| 套餐目录来源 | 营销侧数据库表及现有营销查询/锁单链路 | 商城侧新增套餐目录表 | 现有可售卖商品和拼团活动已经由营销侧维护，商城侧新增目录会造成价格、活动和库存语义双写不一致。 |
| 履约入口 | 以现有支付成功/成团消息触发权益履约 | 支付回调事务内同步开通权益 | 支付链路应保持短事务，权益外部调用失败不能污染支付成功主路径。 |
| 拼团接口 | 暂不改 `api/v1/gbm/trade/*` 路径 | 新增订阅营销路径 | 当前路径已承载锁单/结算/退单契约，保持路径可降低联调成本。 |
| 权益幂等 | 新增本地额度权益记录和履约任务表，按 `orderId + servicePackageId` 幂等 | 只依赖 MQ 重试和订单状态 | 订单 `DEAL_DONE` 只能表达最终状态，不能表达额度总量、已用额度、剩余额度和履约重试状态。 |
| 权益范围 | 首期只售卖大模型调用额度 | 同期售卖 API Key、智能体、工作流模板、MCP 工具权限 | 后者属于 AI Agent 项目能力，放入本 change 会扩大边界并引入额外鉴权和资源开通契约。 |

## 12. 风险与人工确认

- 资金风险：涉及支付宝支付金额、营销抵扣、退款金额和退订退款规则。已确认已消耗额度按比例退款，金额保留 2 位小数，四舍五入，最低退款金额 0.01 元，额度消耗口径为商城侧额度权益表。
- 状态流转风险：`DEAL_DONE` 将从“商品发货完成”变为“权益开通完成”，需要确认是否复用状态名，还是新增更清晰的履约状态。
- MQ/外部接口风险：支付成功、成团、退款消息都可能重复或乱序；权益系统调用还可能失败、超时或部分成功，必须有幂等和补偿。
- 数据风险：新增权益表/任务表会影响 SQL、Mapper、Repository 和测试；修改 `product_id/goods_id` 字段名会造成大范围兼容性风险，首期不建议直接改名。
- 安全风险：首期不售卖 API Key、MCP 工具访问 token、智能体权限，但新增 `servicePackageId` 公开入参仍必须校验用户与订单归属；内部接口签名、白名单或完整登录态改造后续单独建 change。

## 13. 确认记录

- 确认时间：2026-05-12 14:22:14 +0800
- 确认人：用户
- 确认范围：
  - 服务套餐目录来源为营销侧数据库表及现有营销查询/锁单链路，商城侧不新增第二套套餐目录主数据。
  - 首期售卖内容仅为大模型调用额度。
  - 商城侧新增额度权益表、履约任务表和领域端口，用于开通幂等、重试补偿、撤销权益和按比例退款。
  - 权益开通失败后重试一段时间，超过阈值自动退款。
  - 退订退款需要撤销未使用权益，已消耗额度按比例退款。
  - 公开 API 新增 `servicePackageId`，内部兼容既有 `productId/goodsId` 映射。
  - 首期不新增内部接口签名、白名单或完整登录态改造；安全加固后续单独建 change。

- 确认时间：2026-05-12 14:38:39 +0800
- 确认人：用户
- 确认范围：
  - 履约失败重试 3 次，重试间隔 1 秒，从首次失败起总计 5 秒后自动退款。
  - 按比例退款金额保留 2 位小数，四舍五入，最低退款金额 0.01 元。
  - 已消耗额度从商城侧额度权益表读取。
