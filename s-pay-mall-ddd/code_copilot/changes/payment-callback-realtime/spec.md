# 支付回调实时化
> status: propose
> created: 2026-05-07
> complexity: 🟡中等

## 1. 背景与目标

当前支付宝异步通知到达存在延迟，支付商城主要依赖 `NoPayNotifyOrderJob` 每分钟查询兜底，用户支付完成后订单和拼团结算可能不能及时推进。

目标是让支付成功后的订单状态尽快变成 `PAY_SUCCESS/MARKET`，不能总是等待定时任务。可选方向包括：支付返回页主动查询、前端轮询查单、后端提供支付结果查询接口、或在支付成功页面触发支付宝主动查单。

### 1.1 业务边界

- 所属上下文：支付 / 订单 / 拼团营销结算
- 调用方向：支付宝异步回调 / 支付宝主动查单 / 本服务对外提供查询接口 / Job 补偿
- 是否涉及资金或订单状态：是

## 2. 代码现状（Research Findings）

- HTTP 支付回调入口：`s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/AliPayController.java` 的 `payNotify(HttpServletRequest)` 只处理支付宝异步 POST 通知，验签通过后调用 `orderService.changeOrderPaySuccess(tradeNo, payTime)`。
- 支付成功领域编排：`s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/OrderService.java` 的 `changeOrderPaySuccess(String orderId, Date payTime)` 对拼团订单先 `changeMarketOrderPaySuccess`，再调用 `port.settlementMarketPayOrder`。
- 兜底任务：`s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/job/NoPayNotifyOrderJob.java` 的 `exec()` 每分钟查询 `orderService.queryNoPayNotifyOrder()`，再调用支付宝 `AlipayTradeQueryRequest`，成功时调用 `changeOrderPaySuccess`。
- 当前风险：如果支付宝异步通知慢，订单需要等 Job；如果 `changeOrderPaySuccess` 里的拼团结算失败，需要依赖下一次异步通知或 Job 重试。
- 2026-05-11 本地 Docker 运行态证据：订单 `621247345364` 在 `20:06:59` 创建，日志中没有出现 `AliPayController#payNotify` 的 `支付回调，消息接收 TRADE_SUCCESS`；`20:08:00` 由 `NoPayNotifyOrderJob#exec()` 查单得到支付宝 `TRADE_SUCCESS` 并触发 `OrderPaySuccessListener`。这说明至少该次不是应用内处理慢，而是支付宝异步通知没有及时到达本机回调入口。
- 兜底任务查询条件位于 `s-pay-mall-ddd-lpc-app/src/main/resources/mybatis/mapper/pay_order_mapper.xml#queryNoPayNotifyOrder`，只查询 `status = 'PAY_WAIT' AND NOW() >= order_time + INTERVAL 1 MINUTE`，因此只要异步通知未到，当前设计天然存在 1 分钟以上的状态推进延迟。
- 本地 FRP 会增加通知链路的不确定性，但不是唯一原因：支付宝沙箱异步通知本身也不承诺实时到达。当前前端 `return_url` 已优化为本机 `localhost:9001`，但 `return_url` 只影响用户浏览器跳转，不会触发后端支付成功状态推进。

## 3. 功能点

- [ ] 功能 1：新增或复用支付结果主动查询能力，用户支付完成后可主动推进订单状态。
- [ ] 功能 2：保持 `NoPayNotifyOrderJob` 作为兜底补偿，而不是主路径。
- [ ] 功能 3：拼团订单支付成功后仍必须触发 `settlementMarketPayOrder`，不能只改支付状态。
- [ ] 功能 4：支付成功处理必须幂等，重复回调、重复查单不能重复破坏状态。
- [ ] 功能 5：前端支付完成后应基于当前订单号主动查询/确认支付结果，不再只依赖支付宝异步通知或 1 分钟 Job。
- [ ] 功能 6：创建支付单接口返回结构化数据 `orderId + payUrl`，避免前端从支付宝表单 HTML 中反解析订单号。

## 4. 业务规则

- 只有支付宝查询/回调明确 `TRADE_SUCCESS` 才允许推进支付成功。
- 支付成功处理统一收敛到 `OrderService#changeOrderPaySuccess`。
- 拼团订单不能绕过营销结算。
- 定时任务保留为补偿手段。
- 前端主动查询只允许把支付宝已确认为 `TRADE_SUCCESS` 的订单推进为成功；查询失败、交易不存在或仍未支付时，只返回等待状态，不能关闭订单或误判成功。
- 前端轮询只改善用户体验，不作为支付成功可信来源；可信来源必须是后端验签回调或后端调用支付宝查单。
- 主动查单接口必须校验订单归属，`userId` 与 `orderId` 不匹配时不得返回或推进订单状态。
- 重复调用主动查单接口必须幂等：订单已经是 `PAY_SUCCESS`、`MARKET`、`DEAL_DONE` 等非待支付状态时，直接返回当前状态，不重复发送支付成功消息、不重复拼团结算。

## 5. 数据变更

- 本方案第一阶段不新增数据库字段。
- 不保存支付宝 `trade_no`、最后查单时间、支付确认来源；这些可作为后续观测性增强单独评估。
- 依赖现有 `pay_order.order_id` 作为支付宝 `out_trade_no` 与本地订单的幂等锚点。

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 修改 | `/api/v1/alipay/create_pay_order` | POST | 响应由 `Response<String>` 调整为 `Response<CreatePayResponseDTO>`，`data` 包含 `orderId`、`payUrl` |
| 新增 | `/api/v1/alipay/query_pay_result` | POST | 入参 `QueryPayResultRequestDTO(userId, orderId)`；后端主动查支付宝，确认 `TRADE_SUCCESS` 后调用既有 `OrderService#changeOrderPaySuccess`；返回 `QueryPayResultResponseDTO(orderId,status,statusDesc,paid)` |

### 6.1 DTO 设计

```text
CreatePayResponseDTO
- orderId: String
- payUrl: String

QueryPayResultRequestDTO
- userId: String
- orderId: String

QueryPayResultResponseDTO
- orderId: String
- status: String
- statusDesc: String
- paid: Boolean
```

### 6.2 前端交互设计

1. 前端调用 `create_pay_order` 后读取 `json.data.orderId` 和 `json.data.payUrl`。
2. 前端将 `orderId` 保存到 `sessionStorage.pendingPayOrderId`。
3. 前端插入 `payUrl` 表单并提交到支付宝。
4. 支付宝 `return_url` 回到 `http://localhost:9001/` 后，前端读取 `pendingPayOrderId`。
5. 前端短轮询 `query_pay_result`，建议 3-5 次、间隔 1-2 秒。
6. 若返回 `paid=true`，刷新订单/商品状态并清理 `pendingPayOrderId`。
7. 若仍未成功，提示“支付处理中，可稍后在我的订单查看”，保留 `NoPayNotifyOrderJob` 兜底。

## 7. 风险与关注点

- ⚠️ 资金风险：不能伪造支付成功，必须依赖支付宝签名回调或主动查单结果。
- ⚠️ 状态风险：重复回调、Job、主动查询可能并发推进同一订单。
- ⚠️ 外部服务风险：支付宝查单失败不能误判为支付失败。
- ⚠️ 本地网络风险：FRP 和支付宝沙箱都可能造成异步通知延迟；系统不能把异步通知作为唯一实时入口。
- ⚠️ 前端契约风险：当前创建支付单只返回支付宝表单 HTML，没有显式返回 `orderId`；若要前端主动确认，需要让前端能可靠拿到本次订单号，或调整创建支付单响应结构。
- ⚠️ API 兼容风险：`create_pay_order` 响应 data 类型从字符串改为对象，会影响当前静态页。当前项目内只有 `docs/dev-ops/nginx/html/index.html` 直接消费该接口；若存在外部调用方，需要同时兼容旧结构或另开接口。
- ⚠️ 并发风险：异步回调、主动查单、Job 可能同时确认同一订单；落库更新需要依赖状态条件或领域幂等判断，避免重复消息和重复结算。

## 8. 测试策略

- `domain`：单元测试 + Mock，覆盖 `changeOrderPaySuccess` 幂等和拼团结算失败重试语义。
- `trigger`：MockMvc 或 Controller 单测，覆盖主动查询接口入参和响应。
- `infrastructure`：支付宝查单适配建议封装成端口后 Mock。

## 9. 待澄清

- [ ] 是否确认采用“调整 `create_pay_order` 响应为 `orderId + payUrl`”方案，而不是前端解析支付宝表单 HTML？
- [ ] 是否确认新增入站接口 `POST /api/v1/alipay/query_pay_result`？
- [x] 第一阶段不保存支付宝 `trade_no`，只用支付宝查单结果推进状态；如需审计字段后续单独设计。
- [ ] 拼团结算失败时是否需要新增独立补偿 Job，而不是复用未支付通知查询 Job？

## 10. 技术决策

| 决策点 | 建议 | 理由 |
|--------|------|------|
| 支付成功入口 | 统一调用 `OrderService#changeOrderPaySuccess` | 避免回调、查单、Job 三套状态逻辑 |
| 定时任务定位 | 保留兜底 | 支付宝异步通知和主动查单都可能失败 |
| 支付宝查单 | 通过端口封装 | 避免 Controller/Job 直接散落 SDK 调用 |
| 订单号传递 | 修改创建支付单响应，显式返回 `orderId + payUrl` | 领域层 `PayOrderEntity` 已有 `orderId`，比解析 HTML 更稳定 |
| 前端轮询 | `sessionStorage` 保存 pending orderId，短轮询 3-5 次 | 支付回跳后页面可恢复上下文，避免无限轮询 |
| 数据库字段 | 第一阶段不新增 | 减少迁移风险，先解决用户体验主路径 |
