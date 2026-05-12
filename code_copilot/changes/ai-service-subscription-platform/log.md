# 变更日志 — AI 服务订阅与营销平台适配

> 记录决策、偏差、验证证据和可沉淀知识。

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
| --- | --- | --- | --- |
| 2026-05-11 20:44:48 +0800 | propose | 读取根级 `code_copilot/` 规则、知识、模板和 Git 状态 | 当前工作区已有较多未提交改动，本 change 仅新增根级 proposal 文档。 |
| 2026-05-11 20:44:48 +0800 | research | 读取 `s-pay-mall-ddd` 子项目 `AGENTS.md`、`code_copilot/README.md`、规则与知识索引 | 子项目规则优先，涉及商城支付/订单/退款必须标记高风险。 |
| 2026-05-11 20:44:48 +0800 | research | 扫描商城订单、商品、支付成功、成团、退款相关代码 | 发现当前履约为 `GoodsService#changeOrderDealDone` 直接置 `DEAL_DONE`。 |
| 2026-05-11 20:44:48 +0800 | research | 扫描营销侧拼团配置、锁单、结算、退单、SKU、通知任务相关代码与 SQL | 发现营销侧可复用 `goods_id/sku/activity/team/outTradeNo` 链路承载服务包拼团。 |
| 2026-05-11 20:44:48 +0800 | propose | 创建 `ai-service-subscription-platform` proposal | 只新增 `spec.md`、`tasks.md`、`log.md`，不进入实现。 |
| 2026-05-12 14:22:14 +0800 | clarify | 用户确认首期边界并纠正套餐目录来源 | 套餐目录来源为营销侧数据库表及现有营销查询/锁单链路；商城侧不新增第二套套餐目录主数据。 |
| 2026-05-12 14:22:14 +0800 | propose-update | 修正 `spec.md` 与 `tasks.md` | 明确首期只售卖大模型调用额度；新增额度权益表、履约任务表和端口；履约失败重试后自动退款；退订按剩余额度比例退款。 |
| 2026-05-12 14:38:39 +0800 | clarify | 用户确认履约重试与退款计算口径 | 履约失败重试 3 次、间隔 1 秒、总计 5 秒后自动退款；退款金额保留 2 位、四舍五入、最低 0.01 元，已消耗额度从商城侧额度权益表读取。 |
| 2026-05-12 14:38:39 +0800 | apply | 进入实现阶段 | `spec.md` 状态更新为 `applying`，不再存在阻塞实现的业务规则待澄清项。 |
| 2026-05-12 14:55:04 +0800 | apply | 完成首期代码实现 | `servicePackageId` 下单、营销侧套餐额度字段、商城侧权益表/履约任务、支付成功履约、失败重试自动退款、按比例退款已落地。 |
| 2026-05-12 14:55:04 +0800 | verify | 执行两个子项目编译 | `s-pay-mall-ddd` 与 `group-buy-market` app 模块依赖编译均通过。 |
| 2026-05-12 15:14:00 +0800 | verify | 应用本地增量 SQL 并验证 Docker 依赖 | 新增非破坏性增量 SQL；Docker MySQL 已存在 `pay_order.service_package_id`、`pay_order.total_quota`、`subscription_entitlement`、`subscription_fulfillment_task`、`sku.total_quota`。 |
| 2026-05-12 15:30:00 +0800 | fix | 修正联调暴露的问题 | 营销应用启动需先安装 reactor 依赖；商城营销 DTO 增加忽略未知字段；订单创建未传营销类型时默认 `NO_MARKET`；测试商品统一为 `9890001` 并避开营销侧限流窗口。 |
| 2026-05-12 15:51:00 +0800 | verify | 商城侧 SpringBoot 测试报告通过 | `OrderServiceTest` 2 个、`OrderServiceRefundTest` 20 个、`RefundSuccessTopicListenerTest` 3 个均 0 failure / 0 error；Maven 进程因现有调度线程未退出，已手动停止。 |

## 技术决策

| 决策 | 选择 | 放弃的方案 | 原因 |
| --- | --- | --- | --- |
| 首期 proposal 策略 | 用“兼容字段 + 语义适配 + 履约边界扩展”描述迁移路径 | 直接要求全量重命名 `product/goods` | 当前 API、SQL、Mapper、前端和测试均依赖 `productId/goodsId`，全量重命名风险过高。 |
| 套餐目录来源 | 营销侧数据库表及现有营销查询/锁单链路 | 商城侧新增 AI 服务套餐目录表 | 既有可售卖商品和拼团活动由营销侧维护，商城侧新增目录会带来价格、活动、额度语义双写不一致。 |
| 履约边界 | 支付/成团后异步触发权益开通，并用本地幂等记录或任务补偿 | 支付回调事务内同步调用 AI 权益系统 | 外部权益开通失败不能影响支付成功主路径。 |
| 权益范围 | 首期仅售卖大模型调用额度 | API Key、智能体权限、工作流模板权限、MCP 工具访问权限一并售卖 | 这些能力属于后续 AI Agent 项目，不进入本 change。 |
| 履约失败处理 | 重试一段时间后自动退款 | 人工处理或长期显示开通中 | 用户侧期望购买成功即成功，不成功自动退钱。 |
| 退订退款 | 撤销未使用额度，已消耗额度按比例退款 | 一律全额退款或只停止续期 | 符合大模型调用额度的可量化消耗模型。 |
| 履约重试参数 | 失败后最多重试 3 次，间隔 1 秒，总计 5 秒后自动退款 | 长时间挂起或人工处理 | 已由用户确认，便于用短周期任务补偿和测试断言验证。 |
| 退款计算参数 | 保留 2 位、四舍五入、最低 0.01 元，消耗额度读商城侧权益表 | 依赖外部 AI Agent 项目或人工估算 | 首期额度权益在商城侧建模，退款口径必须来自同一张权益表。 |
| 变更目录 | `code_copilot/changes/ai-service-subscription-platform` | 放入某个子项目 change | 需求横跨商城与营销两个项目，根级工作区 change 更合适。 |

## DDD 边界记录

| 变更点 | 项目 | 模块 | 是否越界 | 说明 |
| --- | --- | --- | --- | --- |
| 订阅下单契约 | s-pay-mall-ddd | api/trigger/domain | 否 | API 接收 DTO，trigger 做协议转换，domain 处理订单规则。 |
| 服务套餐查询 | cross-project | domain/infrastructure | 否 | 营销侧维护套餐目录和活动配置，商城侧通过端口获取套餐快照并落订单/权益。 |
| 拼团锁单/结算/退单 | cross-project | infrastructure/trigger/domain | 否 | 商城通过 gateway/port 调用营销 HTTP，营销 trigger 调用领域服务。 |
| 权益开通 | s-pay-mall-ddd | trigger/domain/infrastructure | 否 | 首期新增额度权益表、履约任务表和端口，不能把 DAO 或后续 AI Agent 项目调用泄漏进订单领域。 |
| 营销活动数据 | group-buy-market | app/infrastructure/domain | 否 | SQL 初始化和 mapper 承载活动与服务包关联，domain 保持活动规则。 |

## 风险记录

| 风险类型 | 位置 | 风险描述 | 处理方式 |
| --- | --- | --- | --- |
| 资金 | `OrderService#doPrepayOrder`、`refundOrder`、`changeOrderRefundSuccess` | 服务订阅涉及额度消耗后按比例退款，不能沿用普通商品全额退款假设。 | 已确认保留 2 位、四舍五入、最低 0.01 元，已消耗额度从商城侧额度权益表读取。 |
| 状态 | `OrderStatusVO`、`pay_order_mapper.xml` | `DEAL_DONE` 当前表示商品发货完成，适配后要表达权益开通完成。 | 首期建议复用状态但改语义；如新增状态需单独确认。 |
| MQ | `OrderPaySuccessListener`、`TeamSuccessTopicListener`、退款 Listener | 重复消息可能导致重复开通权益、重复退款或重复撤销。 | 权益履约必须增加幂等记录或本地任务表。 |
| 外部接口 | `ProductPort`、`IGroupBuyMarketService`、未来权益网关 | 套餐目录来源已确认在营销侧；后续 AI Agent 权限/API Key/MCP 不进入首期。 | 首期不新增外部权益系统调用，仅做本地额度权益履约；安全加固后续单独建 change。 |
| 数据库 | `pay_order`、`pay_refund_task`、`sku`、`sc_sku_activity`、可能新增权益表 | 字段重命名或新增表会影响 Mapper、DAO、PO、SQL 和测试。 | 首期不直接重命名既有字段；新增表需单独设计和测试。 |
| 安全 | API Key、token、MCP 访问凭证 | 权益凭证不能进入日志、MQ、测试数据或文档示例。 | `security.md` 约束已写入 proposal 风险与验收。 |

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
| --- | --- | --- | --- |
| AI 服务套餐目录 | 来源为营销侧数据库表及现有营销查询/锁单链路 | 当前 `ProductRPC#queryProductByProductId` 硬编码 `MyBatisBook` 和 `100.00` | Task 1 修正为移除或旁路 `ProductRPC` 硬编码，不在商城新增第二套套餐目录。 |
| 权益履约 | 支付或成团后开通大模型调用额度 | 当前 `GoodsService#changeOrderDealDone` 只改订单状态 | Task 4 修正为新增额度权益表、履约任务表和端口。 |
| 营销商品语义 | AI 服务包可参加拼团 | 当前 SQL 初始化数据为图书商品 | 标记为 Task 2，按服务包示例数据适配。 |
| 退款金额 | 已消耗额度后按剩余额度比例退款 | 当前退款默认按 `payAmount/totalAmount` 全额退款 | `OrderService#changeOrderRefundSuccess` 改为委托 `SubscriptionService#calculateRefundAmount` 读取商城侧权益表计算。 |

## 验证证据

| 命令/方式 | 结果 | 备注 |
| --- | --- | --- |
| `git status --short --branch` | 成功 | 工作区已有未提交改动；本 proposal 避免触碰既有改动文件。 |
| 读取根级 `code_copilot/README.md`、`rules/*.md`、`knowledge/index.md`、`platform-knowledge.md` | 成功 | 确认跨项目规则和高风险边界。 |
| 读取 `s-pay-mall-ddd/code_copilot/README.md`、`rules/*.md`、`knowledge/index.md` | 成功 | 确认商城侧支付/订单/退款规则。 |
| `rg`/`sed` 扫描商城和营销核心代码 | 成功 | 形成 Research Findings。 |
| Maven 测试 | 未执行 | `/propose` 阶段仅新增文档，不改业务代码。 |
| `cd s-pay-mall-ddd && mvn -pl s-pay-mall-ddd-lpc-app -am -DskipTests compile` | 成功 | 商城侧 API、domain、trigger、infrastructure 与 mapper 资源编译通过。 |
| `cd group-buy-market && mvn -pl group-buy-market-lpc-app -am -DskipTests compile` | 成功 | 营销侧服务包额度字段、DTO、领域和基础设施编译通过。 |
| `docker exec ... mysql < code_copilot/changes/ai-service-subscription-platform/sql/2026-05-12-ai-service-subscription-migration.sql` | 成功 | 非破坏性应用本地增量 SQL，仅新增字段/表并修正示例套餐数据。 |
| `curl --noproxy '*' -X POST http://127.0.0.1:8091/api/v1/gbm/index/query_group_buy_market_config ...` | 成功 | 返回 `code=0000`，`goodsId=9890001`，`totalQuota=1000000`。 |
| `cd group-buy-market && mvn -pl group-buy-market-lpc-app -am -DskipTests install` | 成功 | 安装当前 reactor 依赖，避免 `spring-boot:run` 只运行 app 模块时加载本地仓库旧 jar。 |
| `cd s-pay-mall-ddd && mvn -pl s-pay-mall-ddd-lpc-app -am -DskipTests=false -DfailIfNoTests=false test` | 测试报告通过 | `OrderServiceTest`、`OrderServiceRefundTest`、`RefundSuccessTopicListenerTest` 均通过；进程因现有定时任务线程未退出，读取 surefire 报告后手动停止。 |

## 踩坑记录

| 问题 | 原因 | 解决方案 | 是否沉淀 |
| --- | --- | --- | --- |
| `s-pay-mall-ddd/docs/dev-ops/mysql/sql/s-pay-mall.sql` 未找到 | 当前工作区实际可见的商城 SQL 位于 `group-buy-market/docs/dev-ops/mysql/sql/s-pay-mall.sql` | proposal 中引用 Mapper 与实际扫描到的 SQL 事实，不依赖不存在路径。 | 否 |
| `spring-boot:run -pl group-buy-market-lpc-app` 首次联调出现 `Sku.totalQuota` 无 setter | app 模块运行时从本地 Maven 仓库加载旧的 infrastructure jar，而不是当前 reactor 编译产物 | 先执行 `mvn -pl group-buy-market-lpc-app -am -DskipTests install`，再启动 app 模块。 | 否 |
| 商城测试进程断言完成后不退出 | SpringBoot 测试启动了定时任务线程，继续执行支付宝未支付订单查询 | 以 surefire 报告为准确认测试通过，随后手动停止残留 Maven/Surefire 进程。 | 否 |

## 知识发现

- [ ] 当前工作区里商城 SQL 文件路径与历史知识记录存在差异，进入 `/apply` 前需要再次确认 SQL 权威位置。
- [x] `ProductRPC` 仍是硬编码商品模拟实现，但不应成为套餐目录源；首期套餐目录应来自营销侧数据库表及现有营销查询/锁单链路。
- [x] 权益开通不能只复用 `DEAL_DONE` 状态，需要额外表达“总额度、已用额度、剩余额度、是否可重试、是否已撤销”。
- [x] 履约任务重试次数、重试间隔、自动退款触发阈值，以及按比例退款舍入规则已确认。
- [x] `ProductRPC` 已不再被 `ProductPort` 注入和调用；服务套餐快照改为通过营销侧 `query_group_buy_market_config` 获取。
- [x] 首期自动退款复用既有 `OrderService#refundOrder`，避免新增一套资金退款状态机。
- [x] 本地已补充非破坏性增量 SQL，避免为了验证新增字段/表而执行带 `DROP TABLE` 的初始化脚本。
