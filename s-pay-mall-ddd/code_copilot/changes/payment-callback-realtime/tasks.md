# 任务拆分 — 支付回调实时化
> status: propose

## 前置条件

- [ ] 用户确认是否允许新增支付结果查询接口 `POST /api/v1/alipay/query_pay_result`。
- [x] 用户确认第一阶段不保存支付宝 `trade_no`，只通过支付宝查单结果推进状态。
- [x] Research 支付创建返回页、前端调用方式和当前订单查询接口。
- [ ] 用户确认是否允许将 `create_pay_order` 响应从 `Response<String>` 调整为 `Response<CreatePayResponseDTO>`。

## Task 1: Research 支付成功链路

- **目标**: 明确支付宝异步回调、return_url、定时任务和前端查询当前如何协作。
- **涉及文件**:
  - `s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/AliPayController.java`
  - `s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/job/NoPayNotifyOrderJob.java`
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/OrderService.java`
- **完成**: pending
- **最新证据**:
  - `AliPayController#payNotify` 只处理支付宝异步 POST 通知，收到并验签成功后才调用 `OrderService#changeOrderPaySuccess`。
  - `NoPayNotifyOrderJob#exec()` 每分钟运行，但 `queryNoPayNotifyOrder` 只查下单超过 1 分钟仍为 `PAY_WAIT` 的订单。
  - 本地 Docker 日志显示订单 `621247345364` 未见异步回调日志，最终在 `20:08:00` 由 Job 查单推进成功。
  - 当前 `return_url` 已回本机，但这只影响浏览器跳转，不等价于支付成功状态确认。
  - **完成**: completed

## Task 2: 设计支付主动确认端口

- **目标**: 将支付宝查单能力从 Job 中抽出为 infrastructure 端口。
- **建议变更**:
  - 在 `domain/order/adapter/port` 新增支付宝查单端口，例如 `IPayOrderQueryPort`。
  - 端口方法建议：`PayOrderCheckResultEntity queryPayResult(String orderId)`。
  - 在 `infrastructure/adapter/port` 用 `AlipayClient + AlipayTradeQueryRequest` 实现。
  - `NoPayNotifyOrderJob` 后续也复用该端口，避免 Job 和 Controller 各自散落 SDK 调用。
- **完成**: pending

## Task 3: 新增支付结果查询/确认入口

- **目标**: 用户支付完成后可通过接口主动触发查单并推进状态。
- **建议接口**: `POST /api/v1/alipay/query_pay_result`
- **建议行为**:
  - 入参包含 `userId`、`orderId`。
  - 后端调用支付宝查单。
  - 只有 `TRADE_SUCCESS` 时调用既有 `OrderService#changeOrderPaySuccess(orderId, payTime)`。
  - 返回订单当前状态，前端可短轮询 3-5 次。
- **涉及文件**:
  - `s-pay-mall-ddd-lpc-api/src/main/java/top/licodetech/mall/api/IPayService.java`
  - `s-pay-mall-ddd-lpc-api/src/main/java/top/licodetech/mall/api/dto/QueryPayResultRequestDTO.java`
  - `s-pay-mall-ddd-lpc-api/src/main/java/top/licodetech/mall/api/dto/QueryPayResultResponseDTO.java`
  - `s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/AliPayController.java`
- **完成**: pending

## Task 4: 调整创建支付单响应与前端保存订单号

- **目标**: 让前端在跳转支付宝前可靠拿到本次 `orderId`。
- **建议变更**:
  - 新增 `CreatePayResponseDTO(orderId, payUrl)`。
  - `IPayService#createPayOrder` 响应改为 `Response<CreatePayResponseDTO>`。
  - `AliPayController#createPayOrder` 从 `PayOrderEntity` 读取 `orderId` 和 `payUrl` 组装响应。
  - `docs/dev-ops/nginx/html/index.html` 读取 `json.data.orderId`、`json.data.payUrl`，并写入 `sessionStorage.pendingPayOrderId`。
- **完成**: pending

## Task 5: 前端回跳后短轮询支付结果

- **目标**: 支付宝 `return_url` 回到本地首页后主动确认支付状态，减少等待 Job 的体感延迟。
- **建议变更**:
  - 页面加载时读取 `sessionStorage.pendingPayOrderId`。
  - 调用 `/api/v1/alipay/query_pay_result`，间隔 1-2 秒，最多 3-5 次。
  - 成功后清理 pending orderId，并刷新订单/页面状态。
  - 失败或仍处理中时展示“支付处理中，可稍后在我的订单查看”。
- **完成**: pending

## Task 6: 补充幂等和测试

- **目标**: 覆盖异步回调、主动查单、Job 重复执行的状态幂等。
- **建议验证**:
  - Controller 测试：`query_pay_result` 入参缺失、订单不属于当前用户、支付宝返回未支付、支付宝返回成功。
  - Domain 测试：重复 `changeOrderPaySuccess` 不应重复破坏状态。
  - 配置/静态验证：`docker compose config --quiet`、前端关键 URL 检查。
- **完成**: pending
