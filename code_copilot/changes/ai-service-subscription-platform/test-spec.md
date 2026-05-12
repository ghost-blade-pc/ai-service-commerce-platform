# 测试计划 — AI 服务订阅与营销平台适配

> created: 2026-05-12
> status: applying

## 1. 验证范围

- 商城侧 `servicePackageId` 下单兼容：公开 API 接收 `servicePackageId`，内部映射为既有 `productId/goodsId`。
- 营销侧服务包目录：`sku` 返回服务套餐名称、原价、调用额度和拼团优惠。
- 额度履约：普通订阅支付成功、拼团成团后都通过既有支付成功消息触发本地额度权益开通。
- 履约补偿：本地履约任务失败后最多重试 3 次、间隔 1 秒、从首次失败起 5 秒后自动退款。
- 退订退款：未消耗额度全额退款，已消耗额度按 `剩余额度 / 总额度` 比例退款，金额保留 2 位、四舍五入，最低退款金额 0.01 元。

## 2. 自动化验证

| 层级 | 场景 | 断言 |
| --- | --- | --- |
| API/Trigger | `CreatePayRequestDTO.servicePackageId` | 不传 `productId` 时仍能构造下单领域对象；过渡期仍兼容 `productId`。 |
| Infrastructure | `ProductPort#queryProductByProductId(userId, productId)` | 通过营销侧 `query_group_buy_market_config` 获取服务包名称、价格和总额度，不再使用本地硬编码 `ProductRPC`。 |
| Domain | `SubscriptionService#fulfillOrder` | 重复履约不会重复插入权益；成功后订单进入 `DEAL_DONE`。 |
| Domain | `SubscriptionService#calculateRefundAmount` | 按商城侧权益表的剩余额度计算退款金额，保留 2 位、四舍五入。 |
| Job | `SubscriptionFulfillmentTaskJob` | 到期任务可重试，超过阈值后调用既有退单退款入口。 |

## 3. 手工联调路径

1. 初始化营销侧 AI 服务套餐数据和商城侧新增表。
2. 调用 `/api/v1/gbm/index/query_group_buy_market_config` 查询 `goodsId=9890001`，确认返回 `goodsName` 和 `totalQuota`。
3. 调用 `/api/v1/alipay/create_pay_order`，请求体使用 `servicePackageId=9890001`。
4. 模拟支付宝支付成功或执行支付回调兜底，确认支付成功消息触发 `subscription_entitlement` 开通。
5. 拼团场景等待 `topic.team_success` 后确认订单进入 `MARKET -> DEAL_DONE`。
6. 修改 `subscription_entitlement.used_quota/remaining_quota` 后发起退订，确认退款金额按比例计算且权益被撤销。

## 4. 已执行验证

| 命令 | 结果 | 备注 |
| --- | --- | --- |
| `cd s-pay-mall-ddd && mvn -pl s-pay-mall-ddd-lpc-app -am -DskipTests compile` | 通过 | 校验商城侧 API、domain、trigger、infrastructure 和 mapper 资源编译。 |
| `cd group-buy-market && mvn -pl group-buy-market-lpc-app -am -DskipTests compile` | 通过 | 校验营销侧服务包额度字段和响应 DTO 编译。 |
| `docker exec ... mysql < code_copilot/changes/ai-service-subscription-platform/sql/2026-05-12-ai-service-subscription-migration.sql` | 通过 | 已在本地 Docker MySQL 非破坏性新增商城权益/任务表、订单字段和营销 `sku.total_quota`。 |
| `curl --noproxy '*' -X POST http://127.0.0.1:8091/api/v1/gbm/index/query_group_buy_market_config ...` | 通过 | 营销服务返回 `code=0000`、`goodsId=9890001`、`totalQuota=1000000`。 |
| `cd group-buy-market && mvn -pl group-buy-market-lpc-app -am -DskipTests install` | 通过 | 安装当前 reactor 依赖，保证启动应用使用本次修改后的 API/domain/infrastructure jar。 |
| `cd s-pay-mall-ddd && mvn -pl s-pay-mall-ddd-lpc-app -am -DskipTests=false -DfailIfNoTests=false test` | 测试报告通过 | `OrderServiceTest` 2 个、`OrderServiceRefundTest` 20 个、`RefundSuccessTopicListenerTest` 3 个均 0 failure / 0 error；Maven 进程因现有调度线程未退出，读取报告后手动停止。 |

## 5. 未覆盖风险

- 未完成真实支付回调后的端到端履约闭环；当前只验证到创建支付单、营销查询/锁单、退款相关单测和本地表结构。
- 支付宝沙箱查询对测试订单返回 `ACQ.TRADE_NOT_EXIST`，因为这些订单未真实支付；后续需要用真实支付回调或模拟回调验证 `PAY_SUCCESS -> DEAL_DONE` 履约链路。
- 未单独新增 Mapper 层和 Job 层测试；后续如继续硬化，应补充权益表、履约任务表和自动退款任务的持久化断言。
