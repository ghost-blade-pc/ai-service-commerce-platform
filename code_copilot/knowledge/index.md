# 知识索引
> 领域知识的轻量索引。每条用一句话说清核心逻辑。
> 格式：- **触发关键词**: 一句话核心逻辑 → `包名.类名.方法名`（可选）

## 业务知识

- **创建支付单**: 用户下单先查未支付/未创建支付单订单，必要时复用订单或补建支付单；拼团订单会先调用营销锁单再创建支付宝支付单 → `top.licodetech.mall.domain.order.service.AbstractOrderService#createOrder`
- **支付宝支付回调**: 只有 `TRADE_SUCCESS` 且 RSA 验签通过才更新订单支付成功 → `top.licodetech.mall.trigger.http.AliPayController#payNotify`
- **支付回调兜底任务**: 支付宝异步通知延迟时，当前每分钟由 `NoPayNotifyOrderJob#exec` 主动查单并调用 `OrderService#changeOrderPaySuccess` 兜底，但后续应避免把它作为主路径 → `top.licodetech.mall.trigger.job.NoPayNotifyOrderJob#exec`
- **拼团营销锁单**: 支付商城通过 Retrofit 调用拼团营销 `api/v1/gbm/trade/lock_market_pay_order` 获取营销抵扣结果 → `top.licodetech.mall.infrastructure.gateway.IGroupBuyMarketService#lockMarketPayOrder`
- **拼团营销结算**: 拼团服务回调支付商城 `api/v1/alipay/group_buy_notify` 后，支付商城批量更新订单营销结算状态并发布支付成功事件 → `top.licodetech.mall.trigger.http.AliPayController#groupBuyNotify`
- **拼团支付金额**: 创建支付宝支付单时，如有 `MarketPayDiscountEntity`，`OrderService#doPrepayOrder` 使用 `payPrice` 作为 `total_amount`，开团/参团都应走该优惠金额 → `top.licodetech.mall.domain.order.service.OrderService#doPrepayOrder`
- **用户订单列表**: Controller 限制 pageSize 最大 50，并通过 `lastId` 游标分页查询用户订单 → `top.licodetech.mall.trigger.http.AliPayController#queryUserOrderList`
- **退款**: 退款入口返回订单最新状态和描述，领域层需要负责订单归属和可退款状态判断；拼团退款先进入 `REFUNDING`，消费 `topic.team_refund` 后模拟退款并置 `REFUNDED` → `top.licodetech.mall.domain.order.service.OrderService#refundOrder`
- **退款 MQ 时序**: `topic.team_refund` 可能快于本地 `REFUNDING` 事务提交，退款确认需要短暂重查 `REFUNDING/REFUNDED` 状态 → `top.licodetech.mall.domain.order.service.OrderService#queryRefundingOrRefundedOrder`
- **退单类型**: 支付商城必须解析拼团退款消息 `type`，`unpaid_unlock` 只完成退单状态不调用退款端口，`paid_unformed/paid_formed` 才调用退款端口 → `top.licodetech.mall.domain.order.model.valobj.RefundTypeVO`
- **普通未支付退单**: 未选择营销拼团的 `PAY_WAIT` 普通订单退单不存在资金退款，只推进到 `REFUNDED` → `top.licodetech.mall.domain.order.service.OrderService#refundOrder`
- **拼团结算幂等**: 消费 `topic.team_success` 时只允许 `PAY_SUCCESS -> MARKET`，只有实际更新成功的订单才发布 `topic.order_pay_success` → `top.licodetech.mall.infrastructure.adapter.repository.OrderRepository#changeOrderMarketSettlement`

## 技术约定

- **DDD 模块边界**: `trigger` 接协议，`api` 定契约，`domain` 放业务规则，`infrastructure` 适配数据库/MQ/外部 HTTP，`types` 放通用类型。
- **外部 REST 调用**: 当前项目已使用 Retrofit2，接口定义放 `infrastructure/gateway`，请求响应 DTO 放 `infrastructure/gateway/dto`。
- **统一响应**: 对外 API 使用 `top.licodetech.mall.api.response.Response`，外部网关响应使用 `top.licodetech.mall.infrastructure.gateway.response.Response`，不要混淆导包。
- **订单状态**: 订单状态由 `OrderStatusVO` 统一表达，新增业务不能散落状态字符串。
- **配置入口**: 拼团营销地址和回调配置位于 `app.config.group-buy-market.*`。
- **后续 bug 拆分**: 支付回调实时化使用 `code_copilot/changes/payment-callback-realtime`；参团优惠未生效使用 `code_copilot/changes/group-buy-join-discount`。

## 踩坑记录

- **Response 类重名**: `api/response/Response` 与 `infrastructure/gateway/response/Response` 类名相同，跨模块编辑时必须检查 import。
- **渠道字段拼写**: 配置里已有 `chanel` 字段拼写，改名会影响既有配置绑定，除非有完整迁移。
- **敏感配置**: `application-dev.yml` 当前含沙箱密钥和数据库密码，新增文档示例不要复制真实值。
- **拼团 DTO 字段兼容**: 外部拼团响应新增字段时，支付商城 `infrastructure/gateway/dto` 必须同步字段或配置容错，否则 Retrofit/Jackson 反序列化失败会导致错误降级。
- **MQ 先于事务提交**: 跨服务 HTTP 返回后立刻投递 MQ 时，消费者可能先于本服务事务提交查库，不能只按第一次查询结果判断永久失败。
- **MQ 有限重试/DLQ**: 支付商城关键 RabbitMQ listener 使用 Spring Retry 最多 3 次尝试，超过后 reject 且不 requeue，由 DLQ 承接人工处理 → `top.licodetech.mall.config.RabbitMQConfig`
- **RabbitMQ 队列参数不可变**: 给已有队列增加 DLX 参数会触发 `PRECONDITION_FAILED`，发布前必须删除/重建旧队列或手工迁移。
