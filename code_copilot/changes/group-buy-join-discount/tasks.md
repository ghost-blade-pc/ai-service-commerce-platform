# 任务拆分 — 参团优惠未生效
> status: propose

## 前置条件

- [ ] 用户提供或允许复现一组开团、参团请求。
- [ ] 确认参团订单的 `orderId`，用于查询支付商城和拼团营销两边数据。

## Task 1: Research 参团创建订单链路

- **目标**: 对比开团和参团入参、锁单响应、支付金额。
- **涉及文件**:
  - `s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/AliPayController.java`
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/AbstractOrderService.java`
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/OrderService.java`
  - `s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/adapter/port/ProductPort.java`
- **完成**: pending

## Task 2: 检查拼团营销锁单返回

- **目标**: 确认参团时拼团营销返回的 `payPrice/deductionPrice/teamId` 正确。
- **涉及文件**:
  - `/home/lpc/project/group-buy-market/group-buy-market-lpc-trigger/src/main/java/top/licodetech/market/trigger/http/MarketTradeController.java`
  - `/home/lpc/project/group-buy-market/group-buy-market-lpc-domain/src/main/java/top/licodetech/market/domain/activity/service/trial/node/MarketNode.java`
  - `/home/lpc/project/group-buy-market/group-buy-market-lpc-domain/src/main/java/top/licodetech/market/domain/activity/service/trial/node/EndNode.java`
- **完成**: pending

## Task 3: 修复金额选择逻辑

- **目标**: 保证开团和参团都以拼团返回 `payPrice` 创建支付宝支付单。
- **完成**: pending

## Task 4: 补充测试

- **目标**: 增加开团/参团金额流转单测，必要时补 mapper 集成测试。
- **完成**: pending
