# 参团优惠未生效
> status: propose
> created: 2026-05-07
> complexity: 🟡中等

## 1. 背景与目标

用户反馈：自己开团可以享受拼团优惠，但加入别人已有拼团时，好像没有享受到拼团优惠。该问题疑似出现在支付商城创建订单复用逻辑、拼团锁单参数传递、或拼团营销返回优惠金额的逻辑中。

目标是确认参团链路应该与开团一样使用拼团优惠价，并修复导致参团支付金额仍为原价或错误金额的逻辑。

### 1.1 业务边界

- 所属上下文：订单 / 拼团营销 / 支付
- 调用方向：支付商城调用拼团营销锁单接口
- 是否涉及资金或订单状态：是

## 2. 代码现状（Research Findings）

- 支付商城创建支付单入口：`s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/AliPayController.java` 的创建支付请求会把 `teamId`、`activityId` 放入 `ShopCartEntity`。
- 支付商城创建订单领域逻辑：`s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/AbstractOrderService.java` 的 `createOrder` 在 `MarketTypeVO.GROUP_BUY_MARKET` 时调用 `lockMarketPayOrder(userId, teamId, activityId, productId, orderId)`。
- 支付金额生成：`s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/OrderService.java` 的 `doPrepayOrder(..., MarketPayDiscountEntity)` 使用 `marketPayDiscountEntity.getPayPrice()` 作为支付宝 `total_amount`。
- 拼团锁单适配：`s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/adapter/port/ProductPort.java` 的 `lockMarketPayOrder` 将拼团响应转换为 `MarketPayDiscountEntity`，包含 `deductionPrice` 和 `payPrice`。
- 拼团营销锁单入口：`/home/lpc/project/group-buy-market/group-buy-market-lpc-trigger/src/main/java/top/licodetech/market/trigger/http/MarketTradeController.java` 的 `lockMarketPayOrder` 返回 `payPrice`、`deductionPrice`、`teamId`。
- 拼团营销优惠计算：`/home/lpc/project/group-buy-market/group-buy-market-lpc-domain/src/main/java/top/licodetech/market/domain/activity/service/trial/node/MarketNode.java` 计算 `payPrice`，`EndNode` 组装试算结果。
- 重点怀疑点：`AbstractOrderService#createOrder` 中复用 `CREATE` 状态订单时，如果已有 `marketDeductionAmount`，会使用 `unpaidOrderEntity.getPayAmount()` 创建支付单；需确认参团场景的 `payAmount` 是否已正确落库，以及参团传入的 `teamId` 是否完整。

## 3. 功能点

- [ ] 功能 1：复现开团和参团两种下单入参，确认 `teamId/activityId/marketType`。
- [ ] 功能 2：确认支付商城锁单响应是否包含正确 `payPrice/deductionPrice/teamId`。
- [ ] 功能 3：确认 `pay_order` 中 `market_type`、`market_deduction_amount`、`pay_amount` 是否按参团优惠价落库。
- [ ] 功能 4：修复参团复用订单或锁单响应转换导致的金额错误。

## 4. 业务规则

- 只要是合法拼团活动且参团锁单成功，支付金额应使用拼团营销返回的 `payPrice`。
- 开团和参团都必须保存拼团订单标识，不能降级为普通订单。
- 支付宝创建支付单的 `total_amount` 必须等于最终应付金额。

## 5. 数据变更

- 暂不预期新增字段。
- 可能需要修正已有 mapper 对 `pay_amount`、`market_deduction_amount` 的写入或查询映射。

## 6. 接口变更

当前不预期新增接口，重点检查现有创建支付单接口和拼团锁单接口。

## 7. 风险与关注点

- ⚠️ 资金风险：支付金额错误会直接影响用户实付。
- ⚠️ 状态风险：锁单成功但支付商城降级普通订单会导致后续支付结算、退款链路异常。
- ⚠️ 外部服务风险：拼团营销返回字段变化会影响支付商城 DTO 反序列化。

## 8. 测试策略

- `domain`：单元测试 + Mock，覆盖开团/参团都使用 `MarketPayDiscountEntity#payPrice`。
- `infrastructure`：Mock Retrofit，验证 `LockMarketPayOrderResponseDTO` 字段转换。
- `SQL`：必要时用 SpringBoot 集成测试验证 `pay_amount` 和 `market_deduction_amount` 落库。

## 9. 待澄清

- [ ] 参团请求中的 `teamId` 是前端传入，还是从拼团详情页接口返回？
- [ ] 参团时数据库中 `pay_order.pay_amount` 实际值是多少？
- [ ] 拼团营销返回的 `payPrice` 在开团和参团是否一致？

## 10. 技术决策

| 决策点 | 建议 | 理由 |
|--------|------|------|
| 修复优先级 | 先查支付商城创建订单/复用订单逻辑 | 用户看到的是支付金额，支付商城是金额进入支付宝的最后一层 |
| 拼团侧修改 | 只有确认返回金额错误时再改 | 避免在两个项目同时扩大变更面 |
