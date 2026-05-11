# 任务拆分 — AI 服务订阅与营销平台适配

> 推荐顺序：契约/API → 领域模型与端口 → 基础设施适配 → 领域服务编排 → 触发入口 → 配置与验证  
> 当前阶段为 `/propose`，所有实现任务均为 pending，待用户确认后才能进入 `/apply`。

## 前置条件

- [x] 已读取根级 `code_copilot/rules/*.md`
- [x] 已读取目标子项目 `AGENTS.md`
- [x] 涉及 `s-pay-mall-ddd/` 时已读取其子项目级 `code_copilot/`
- [x] 已确认影响项目和 DDD 层级
- [x] 已确认是否涉及资金、订单状态、退款、拼团、MQ、外部接口、数据库
- [x] 已检查 Git 分支和未提交变更
- [ ] 已确认首期实现边界：仅语义适配，还是新增权益履约模型
- [ ] 已确认权益目录来源、权益类型和退订退款规则

## Task 1: 服务套餐目录与公开契约兼容适配

- **目标**：把商城侧订购对象从传统商品语义适配为 AI 服务套餐，同时保持现有 `productId` API 字段兼容。
- **影响项目**：s-pay-mall-ddd
- **DDD 层级**：api / domain / infrastructure / trigger
- **涉及文件**：
  - `s-pay-mall-ddd-lpc-api/src/main/java/top/licodetech/mall/api/dto/CreatePayRequestDTO.java`：补充订阅服务套餐语义注释或兼容字段。
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/model/entity/ProductEntity.java`：确认是否保留 Product 命名或新增订阅服务包模型。
  - `s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/gateway/ProductRPC.java`：移除硬编码图书示例，改为 AI 服务套餐数据源。
  - `s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/AliPayController.java`：日志和接口说明调整为订阅下单语义。
- **关键签名**：
  ```java
  ProductEntity queryProductByProductId(String productId);
  PayOrderEntity createOrder(ShopCartEntity shopCartEntity) throws Exception;
  ```
- **依赖**：用户确认服务套餐目录来源
- **风险标记**：外部接口 / 数据库 / 安全
- **验收标准**：
  - 普通订阅下单仍兼容当前请求结构。
  - 支付宝 `subject` 使用服务套餐名称。
  - 不再出现 `MyBatisBook` 这类传统商品硬编码。
- **验证命令**：
  - `cd s-pay-mall-ddd && mvn -pl s-pay-mall-ddd-lpc-app -am -DskipTests=false -DfailIfNoTests=false test`
- **状态**：pending

## Task 2: 营销侧服务包拼团活动数据适配

- **目标**：把营销侧 `goods_id/sku/sc_sku_activity` 的业务语义适配为 AI 服务包参与拼团活动。
- **影响项目**：group-buy-market
- **DDD 层级**：api / domain / infrastructure / app
- **涉及文件**：
  - `group-buy-market-lpc-api/src/main/java/top/licodetech/market/api/dto/GoodsMarketRequestDTO.java`：确认是否保持 `goodsId` 兼容。
  - `group-buy-market-lpc-api/src/main/java/top/licodetech/market/api/dto/GoodsMarketResponseDTO.java`：确认展示字段是否需要服务包名称、权益摘要。
  - `group-buy-market-lpc-app/src/main/resources/mybatis/mapper/sku_mapper.xml`：保持查询逻辑，数据语义改为服务包。
  - `group-buy-market/docs/dev-ops/mysql/sql/2-28-group_buy_market.sql`：初始化数据改为 AI 服务包与活动。
- **关键签名**：
  ```java
  Response<GoodsMarketResponseDTO> queryGroupBuyMarketConfig(GoodsMarketRequestDTO requestDTO);
  ```
- **依赖**：Task 1 的服务套餐标识与价格策略
- **风险标记**：数据库 / 外部接口
- **验收标准**：
  - 拼团配置查询能返回 AI 服务包的价格、优惠和可参团队伍。
  - `goodsId` 与商城侧服务套餐标识保持一致。
- **验证命令**：
  - `cd group-buy-market && mvn -pl group-buy-market-lpc-app -am -DskipTests=false -DfailIfNoTests=false test`
- **状态**：pending

## Task 3: 订阅订单支付与拼团结算语义适配

- **目标**：在不破坏当前支付和拼团状态机的前提下，把订单链路表述为订阅采购与成团结算。
- **影响项目**：cross-project
- **DDD 层级**：domain / infrastructure / trigger
- **涉及文件**：
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/AbstractOrderService.java`：确认订阅下单、复用未支付订单、营销锁单逻辑。
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/OrderService.java`：确认普通订阅与拼团订阅支付成功状态流。
  - `s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/adapter/port/ProductPort.java`：确认锁单、结算、退单调用保持兼容。
  - `group-buy-market-lpc-trigger/src/main/java/top/licodetech/market/trigger/http/MarketTradeController.java`：日志、注释、参数校验语义调整。
- **关键签名**：
  ```java
  void changeOrderPaySuccess(String orderId, Date payTime);
  void settlementMarketPayOrder(String userId, String orderId, Date payTime);
  ```
- **依赖**：Task 1、Task 2
- **风险标记**：资金 / 状态 / 外部接口
- **验收标准**：
  - 普通订阅支付成功后可触发履约。
  - 拼团订阅支付成功后先结算营销，成团后再触发履约。
  - 现有支付宝回调与 `NoPayNotifyOrderJob` 兜底语义不被破坏。
- **验证命令**：
  - 两个子项目 app 模块测试命令均需执行。
- **状态**：pending

## Task 4: 权益履约模型与幂等补偿

- **目标**：把当前模拟发货改为 AI 服务权益开通，并提供幂等和失败补偿能力。
- **影响项目**：s-pay-mall-ddd
- **DDD 层级**：api / domain / infrastructure / trigger / app / types
- **涉及文件**：
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/goods/service/GoodsService.java`：从模拟发货转为权益履约编排，或新增独立 `subscription/entitlement` 领域服务。
  - `s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/listener/OrderPaySuccessListener.java`：消费支付成功消息后触发权益开通。
  - `s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/adapter/repository/GoodsRepository.java`：现有只改订单 `DEAL_DONE`，需要扩展或替换为权益记录。
  - `s-pay-mall-ddd-lpc-app/src/main/resources/mybatis/mapper/*`：若新增权益表/任务表，需要新增 mapper。
- **关键签名**：
  ```java
  void changeOrderDealDone(String orderId);
  ```
- **依赖**：用户确认权益类型和是否新增本地权益表/任务表
- **风险标记**：状态 / MQ / 外部接口 / 数据库 / 安全
- **验收标准**：
  - 重复支付成功消息不会重复开通权益。
  - 权益开通失败可以重试或进入可观测的补偿状态。
  - 日志不输出 API Key、token 或敏感权限凭证。
- **验证命令**：
  - `cd s-pay-mall-ddd && mvn -pl s-pay-mall-ddd-lpc-app -am -DskipTests=false -DfailIfNoTests=false -Dtest=*ListenerTest,*ServiceTest test`
- **状态**：pending

## Task 5: 退订退款与权益撤销策略

- **目标**：在当前退款链路上补充 AI 服务订阅退订语义，包括未开通、已开通、已消耗额度等场景。
- **影响项目**：cross-project
- **DDD 层级**：domain / infrastructure / trigger
- **涉及文件**：
  - `s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/OrderService.java`：退订退款状态和金额规则。
  - `s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/listener/RefundSuccessTopicListener.java`：拼团退单成功后权益撤销或状态同步。
  - `group-buy-market-lpc-domain/src/main/java/top/licodetech/market/domain/trade/service/ITradeRefundOrderService.java` 及实现：营销退单规则保持一致。
- **关键签名**：
  ```java
  OrderEntity refundOrder(String userId, String orderId);
  boolean receiveRefundSuccessMessage(String orderId, RefundTypeVO refundType, String message);
  ```
- **依赖**：Task 4 的权益状态模型；用户确认退款规则
- **风险标记**：资金 / 状态 / MQ / 外部接口
- **验收标准**：
  - 未支付订阅退订不触发真实资金退款。
  - 已支付订阅退款金额和权益撤销规则明确。
  - 拼团退单消息重复消费不重复退款、不重复撤销。
- **验证命令**：
  - `cd s-pay-mall-ddd && mvn -pl s-pay-mall-ddd-lpc-app -am -DskipTests=false -DfailIfNoTests=false -Dtest=*Refund*Test test`
- **状态**：pending

## Task 6: 测试计划与跨项目验证

- **目标**：为 AI 服务订阅适配补充独立 `test-spec.md`，覆盖普通订阅、拼团订阅、权益履约、退订退款和异常补偿。
- **影响项目**：cross-project
- **DDD 层级**：app / test
- **涉及文件**：
  - `code_copilot/changes/ai-service-subscription-platform/test-spec.md`：新增测试计划。
  - 两个子项目 `*-lpc-app/src/test/java/.../test/`：按实现范围新增或调整测试。
- **关键签名**：
  ```java
  // 根据确认后的实现范围补充具体测试类
  ```
- **依赖**：Task 1-5
- **风险标记**：资金 / 状态 / MQ / 外部接口 / 数据库
- **验收标准**：
  - 测试计划列出每条高风险链路的输入、前置状态、预期状态和幂等断言。
  - 关键测试命令可执行并记录结果。
- **验证命令**：
  - `cd s-pay-mall-ddd && mvn -pl s-pay-mall-ddd-lpc-app -am -DskipTests=false -DfailIfNoTests=false test`
  - `cd group-buy-market && mvn -pl group-buy-market-lpc-app -am -DskipTests=false -DfailIfNoTests=false test`
- **状态**：pending

## 变更摘要

- **总文件数**：proposal 阶段新增 3 个文档文件；实现阶段待确认。
- **Spec-Plan 偏差记录**：暂无。
- **验证结果**：proposal 阶段仅完成只读 Research，未运行 Maven 测试。
- **遗留问题**：首期范围、权益模型、套餐目录来源、退款规则、API 字段兼容策略待用户确认。
