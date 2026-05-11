# 任务拆分 — 退单退款服务对接
> DDD 推荐顺序：契约/API → 领域模型与端口 → 基础设施适配 → 领域服务编排 → 触发入口 → 配置与验证
> 每个任务 = 可独立提交的原子变更（3-5 个文件）
> 每个任务必须精确到文件路径和函数签名

## 前置条件

- [x] 已读取 `code_copilot/rules/*.md`
- [x] 已确认是否涉及资金、退款、订单状态、拼团营销交互
- [x] 已确认本地分支不是禁止变更分支
- [x] 用户确认本期先模拟退款成功，不真实调用支付宝退款。
- [x] 用户确认退款标识只使用 `out_trade_no = orderId`，不新增 `trade_no` 落库。
- [x] 用户确认 `PAY_WAIT` 未支付订单也等待拼团 MQ 后再变 `REFUNDED`。
- [x] 用户确认 `MARKET`、`DEAL_DONE` 状态允许用户退单，队伍所有人退单时拼团失败。
- [x] 用户确认本期不新增支付商城本地退款补偿表/Job。
- [x] 用户确认本期主任务先改支付商城，最后一个 task 再修改 `/home/lpc/project/group-buy-market` 的两个小补强。

## Task 1: 支付商城补齐拼团退单出站契约

- **目标**: 让支付商城可以通过 Retrofit2 调用拼团营销退单接口。
- **DDD 层级**: infrastructure / domain
- **涉及文件**:
  - `s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/gateway/IGroupBuyMarketService.java` — 新增 `refundMarketPayOrder` Retrofit 方法。
  - `s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/gateway/dto/RefundMarketPayOrderRequestDTO.java` — 新增请求 DTO。
  - `s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/gateway/dto/RefundMarketPayOrderResponseDTO.java` — 新增响应 DTO。
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/adapter/port/IProductPort.java` — 新增领域端口方法。
  - `s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/adapter/port/ProductPort.java` — 实现拼团退单调用和响应转换。
- **关键签名**:
  ```java
  @POST("api/v1/gbm/trade/refund_market_pay_order")
  Call<Response<RefundMarketPayOrderResponseDTO>> refundMarketPayOrder(@Body RefundMarketPayOrderRequestDTO requestDTO);

  boolean refundMarketPayOrder(String userId, String orderId);
  ```
- **依赖**: 拼团系统已有 `MarketTradeController#refundMarketPayOrder`。
- **风险标记**: 外部接口 / 状态
- **验收标准**: 拼团返回 `0000 + success/repeat` 时领域端口返回成功；非成功或异常时抛出/返回可识别失败，不吞掉关键错误。
- **验证命令（可选）**:
  - `mvn -pl s-pay-mall-ddd-lpc-app -DskipTests=false -Dtest=OrderServiceRefundTest test`
- **完成**: done

## Task 2: 支付商城调整退单申请领域流转

- **目标**: 将用户退单申请从“直接已退单”改为“申请成功后退单中”，并按普通/拼团订单分支处理；`PAY_WAIT` 也等待拼团 MQ 完成最终状态。
- **DDD 层级**: domain / infrastructure
- **涉及文件**:
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/model/valobj/OrderStatusVO.java` — 确认/调整 `canRefund`，增加 `canRefundApply` 或幂等判断。
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/IOrderService.java` — 调整/新增退单申请方法签名，新增退款成功确认方法。
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/OrderService.java` — 实现 `refundOrder` 调拼团退单、更新 `REFUNDING`、幂等处理。
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/adapter/repository/IOrderRepository.java` — 新增 `changeOrderRefunding`、`changeOrderRefunded` 等语义明确的方法。
  - `s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/adapter/repository/OrderRepository.java` — 实现新仓储方法。
- **关键签名**:
  ```java
  OrderEntity refundOrder(String userId, String orderId);

  OrderEntity changeOrderRefundSuccess(String orderId);

  int changeOrderRefunding(String userId, String orderId);

  int changeOrderRefunded(String orderId);
  ```
- **依赖**: Task 1 的拼团退单端口。
- **风险标记**: 资金 / 状态 / 外部接口
- **验收标准**: 拼团订单退单申请成功后支付订单状态为 `REFUNDING`；`PAY_WAIT/PAY_SUCCESS/MARKET/DEAL_DONE` 按确认规则允许退单；拼团退单失败时支付订单不进入 `REFUNDING`；重复申请对 `REFUNDING/REFUNDED` 返回幂等结果。
- **验证命令（可选）**:
  - `mvn -pl s-pay-mall-ddd-lpc-app -DskipTests=false -Dtest=OrderServiceRefundTest test`
- **完成**: done

## Task 3: 支付商城补齐订单状态 SQL 和配置

- **目标**: 为 `REFUNDING -> REFUNDED` 提供明确 SQL，并新增支付商城消费 `topic.team_refund` 的 RabbitMQ 配置。
- **DDD 层级**: infrastructure / app
- **涉及文件**:
  - `s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/dao/IOrderDao.java` — 新增 `changeOrderRefunding`、`changeOrderRefunded` mapper 方法。
  - `s-pay-mall-ddd-lpc-app/src/main/resources/mybatis/mapper/pay_order_mapper.xml` — 新增/调整状态更新 SQL，避免 `refundOrder` 直接置 `REFUNDED`。
  - `s-pay-mall-ddd-lpc-app/src/main/resources/application-dev.yml` — 新增 `spring.rabbitmq.config.consumer.topic_team_refund`。
  - `s-pay-mall-ddd-lpc-app/src/main/resources/application-test.yml` — 未修改；当前文件为极简测试配置，未承载 RabbitMQ consumer 配置。
  - `s-pay-mall-ddd-lpc-app/src/main/resources/application-prod.yml` — 同步生产环境配置。
- **关键签名**:
  ```java
  int changeOrderRefunding(@Param("userId") String userId, @Param("orderId") String orderId);

  int changeOrderRefunded(@Param("orderId") String orderId);
  ```
- **依赖**: Task 2 的仓储接口。
- **风险标记**: 状态 / MQ / 配置
- **验收标准**: SQL 只允许合法前置状态更新，支付商城新增独立队列 `s_pay_mall_queue_2_topic_team_refund` 绑定 `group_buy_market_exchange/topic.team_refund`。
- **验证命令（可选）**:
  - `mvn -pl s-pay-mall-ddd-lpc-app -DskipTests=false test`
- **完成**: done

## Task 4: 支付商城新增退款成功 MQ 监听和模拟退款适配

- **目标**: 支付商城监听拼团 `topic.team_refund`，执行模拟退款成功逻辑，并把订单状态从 `REFUNDING` 改为 `REFUNDED`。
- **DDD 层级**: trigger / domain / infrastructure
- **涉及文件**:
  - `s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/listener/RefundSuccessTopicListener.java` — 新增 MQ 监听器，只解析消息并调用领域服务。
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/model/entity/RefundOrderEntity.java` — 如需要，新增退款领域对象。
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/adapter/port/IRefundPort.java` 或复用现有端口 — 定义退款能力，当前实现为模拟成功。
  - `s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/adapter/port/RefundPort.java` — 本期不调用 `AlipayTradeRefundRequest`，只按 `out_trade_no = orderId` 模拟退款成功。
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/OrderService.java` — 新增 MQ 后退款确认编排。
- **关键签名**:
  ```java
  public void listener(String message);

  boolean refund(String orderId, BigDecimal refundAmount);

  OrderEntity changeOrderRefundSuccess(String orderId);
  ```
- **依赖**: Task 2/3 的状态与 SQL；本期已确认只模拟退款成功。
- **风险标记**: 资金 / MQ / 状态
- **验收标准**: `REFUNDING` 订单消费 MQ 后执行一次模拟退款并更新为 `REFUNDED`；重复 MQ 对 `REFUNDED` 幂等忽略；模拟退款异常时抛异常触发 MQ 重试。
- **验证命令（可选）**:
  - `mvn -pl s-pay-mall-ddd-lpc-app -DskipTests=false -Dtest=OrderServiceRefundTest test`
- **完成**: done

## Task 5: 补充测试

- **目标**: 按 `test-spec.md` 策略覆盖本次高风险退款链路。
- **DDD 层级**: app/test
- **涉及文件**:
  - `s-pay-mall-ddd-lpc-app/src/test/java/top/licodetech/mall/test/domain/OrderServiceRefundTest.java` — 扩展单元测试 + Mockito。
  - `s-pay-mall-ddd-lpc-app/src/test/java/top/licodetech/mall/test/trigger/RefundSuccessTopicListenerTest.java` — 本期未新增；MQ listener 以编译校验为主，领域状态流转由 `OrderServiceRefundTest` 覆盖。
  - `code_copilot/changes/refund-market-mq-integration/test-spec.md` — 新增测试 Spec。
- **关键签名**:
  ```java
  @Test
  public void test_refundOrder_groupBuy_paySuccess_refunding() { }

  @Test
  public void test_changeOrderRefundSuccess_refunded_repeat() { }
  ```
- **依赖**: Task 1-4。
- **风险标记**: 资金 / 状态 / MQ
- **验收标准**: 覆盖成功、失败、重复、非法状态、MQ 重试场景；测试输出记录到 log.md。
- **验证命令（可选）**:
  - `mvn -pl s-pay-mall-ddd-lpc-app -DskipTests=false -Dtest=OrderServiceRefundTest test`
  - `mvn -pl s-pay-mall-ddd-lpc-app -DskipTests=false test`
- **完成**: done

## Task 6: 补强拼团侧边界和配置

- **目标**: 在支付商城主链路完成后执行，修正拼团退单空数据保护和 `topic_team_refund` 绑定显式配置。该任务不是支付商城主链路的硬性前置，但能提高跨服务接口稳定性。
- **DDD 层级**: group-buy-market trigger/domain/app/infrastructure
- **涉及文件**:
  - `/home/lpc/project/group-buy-market/group-buy-market-lpc-domain/src/main/java/top/licodetech/market/domain/trade/service/refund/filter/DataNodeFilter.java` — 增加外部单号不存在时的业务异常。
  - `/home/lpc/project/group-buy-market/group-buy-market-lpc-app/src/main/java/top/licodetech/market/config/RabbitMQConfig.java` — 如需要，新增 `topicTeamRefundBinding`。
  - `/home/lpc/project/group-buy-market/group-buy-market-lpc-app/src/test/java/top/licodetech/market/test/...` — 本期未补充；拼团侧仅做两个小补强并通过编译校验。
- **关键签名**:
  ```java
  public Binding topicTeamRefundBinding(String routingKey, String queue);
  ```
- **依赖**: Task 1-5 完成；拼团项目已创建同名分支 `3-8-260507-lpc-refund-intf-mq`。
- **风险标记**: 外部接口 / MQ
- **验收标准**: 不存在外部单号返回明确业务错误；`topic.team_refund` 交换机、路由、队列绑定清晰。
- **验证命令（可选）**:
  - `mvn -f /home/lpc/project/group-buy-market/pom.xml -pl group-buy-market-lpc-app -DskipTests=false test`
- **完成**: done

## Task 7: 收尾运行期 bug 修复与文档归档

- **目标**: 根据联调日志修复已暴露的退款链路时序问题，并把后续非本期问题沉淀为独立 change。
- **DDD 层级**: domain / app-test / code_copilot
- **涉及文件**:
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/OrderService.java` — `queryRefundingOrRefundedOrder` 增加短暂重查，覆盖 MQ 快于本地事务提交的场景。
  - `s-pay-mall-ddd-lpc-app/src/test/java/top/licodetech/mall/test/domain/OrderServiceRefundTest.java` — 新增 `test_changeOrderRefundSuccess_payWait_inFlightRefundingRetry_success`。
  - `code_copilot/changes/refund-market-mq-integration/log.md` — 补齐运行日志分析、踩坑记录、验证证据。
  - `code_copilot/changes/payment-callback-realtime/*` — 后续支付回调实时化 bug 的 propose 文档。
  - `code_copilot/changes/group-buy-join-discount/*` — 后续参团优惠 bug 的 propose 文档。
- **关键签名**:
  ```java
  private OrderEntity queryRefundingOrRefundedOrder(String orderId, String errorInfo);
  ```
- **依赖**: Task 1-6 完成；联调日志确认退款最终状态无误。
- **风险标记**: 状态 / MQ / 资金
- **验收标准**: 退款 MQ 在本地 `REFUNDING` 事务提交稍慢时不再首次报错；目标单测通过；后续两个 bug 不混入本期已完成退款任务。
- **验证命令（可选）**:
  - `mvn -pl s-pay-mall-ddd-lpc-app -am -DskipTests=false -DfailIfNoTests=false -Dtest=OrderServiceRefundTest test`
- **完成**: done

## 变更摘要
> /apply 全部完成后填写

- **总文件数**: 支付商城 20+ 个文件变更/新增；拼团营销 2 个文件变更；`code_copilot` 文档补齐本期归档和后续 bug propose。
- **Spec-Plan 偏差记录**:
  - `application-test.yml` 未同步 MQ 配置，因为当前测试配置未承载 RabbitMQ consumer 配置。
  - 未新增 `RefundOrderEntity`，现有 `OrderEntity` 足够表达本期状态流转。
  - 未新增 Listener 单测，优先用领域单测覆盖核心规则，并用 Maven 编译校验 Listener。
  - 新增 `s-pay-mall-ddd-lpc-app/pom.xml` 测试开关，使默认跳过测试不变，但允许 `-DskipTests=false` 执行单测。
  - 运行期追加 `queryRefundingOrRefundedOrder` 短暂重查，处理 MQ 快于本地事务提交的真实联调时序。
- **遗留问题**:
  - 本期为模拟退款成功；真实支付宝退款接入时需补充支付宝退款端口实现、失败补偿和集成测试。
  - 支付回调不能总等定时任务，已拆到 `payment-callback-realtime`。
  - 参团未享受拼团优惠，已拆到 `group-buy-join-discount`。
