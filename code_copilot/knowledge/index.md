# 知识索引

本目录沉淀跨 change 可复用的项目知识。只有经过验证、可复用、不会误导后续开发的内容才应写入。

## 读取策略

- 本文件是知识路由表，应优先读取。
- 不默认全文展开大型知识文件；先根据任务关键词定位主题，再读取相关章节或片段。
- 读取大型知识文件前，先用 `rg` 搜索关键词、类名、表名、topic、change id 或业务术语。
- 只打开命中的小范围片段；如果证据不足，再扩大到相邻章节。
- 需要写入 `spec.md` 的结论必须能回溯到真实源码、配置、SQL、Mapper、Listener、Job 或已验证知识。

## 已确认知识

### 全局知识地图

- [platform-knowledge.md](platform-knowledge.md)：大型综合知识图谱，覆盖根工作区、`group-buy-market/`、`s-pay-mall-ddd/` 的项目结构、DDD 分层、跨项目调用、MQ 链路、补偿任务、配置、风险点和 Research 入口。
- [subscription-entitlement-flow.md](subscription-entitlement-flow.md)：订阅权益履约幂等模式、履约任务重试补偿与自动退款、按比例退款计算，覆盖 `subscription_entitlement` / `subscription_fulfillment_task` 表结构与关键 SQL。
- [static-frontend-ai-service-pages.md](static-frontend-ai-service-pages.md)：Nginx 静态前端的 AI 服务订阅、拼团购买、扫码登录、订单查询/退单页面知识，覆盖 `docs/dev-ops/nginx/html/` 入口、接口契约、响应式设计边界和验证命令。

使用方式：

- 不作为每次 workflow 的默认全文读取对象。
- 当任务涉及跨项目联动、MQ、退款、订单状态、Mapper/SQL 或本地环境时，先按下面的主题关键词检索，再读取命中片段。

### 主题路由

| 任务关键词 | 优先检索关键词 | 推荐读取范围 |
| --- | --- | --- |
| 工作区结构、技术栈、模块边界 | `工作区定位`、`服务职责`、`DDD 分层约定`、`规则优先级` | `platform-knowledge.md` 的工作区与分层相关章节 |
| 商城下单、支付宝、订单状态 | `创建支付单`、`支付宝支付回调`、`OrderService`、`pay_order`、`OrderStatusVO` | 商城订单、支付、状态、Mapper 相关章节和源码 |
| 拼团锁单、结算、成团 | `lock_market_pay_order`、`settlement_market_pay_order`、`topic.team_success`、`group_buy_order` | 跨项目同步调用、拼团消息、营销表结构相关章节和源码 |
| 退单退款、退款补偿 | `refund_market_pay_order`、`topic.team_refund`、`pay_refund_task`、`RefundTaskJob`、`RefundTypeVO` | 退款链路、补偿任务、退款类型相关章节和源码 |
| MQ 契约、消息幂等 | `topic.order_pay_success`、`topic.team_success`、`topic.team_refund`、`notify_task`、`EventPublisher` | 异步消息链路、消息体、幂等键相关章节和 listener/publisher 源码 |
| Mapper、表结构、索引 | `pay_order`、`group_buy_order_list`、`notify_task`、`uq_`、`idx_` | 数据与持久化、核心表、SQL 和 Mapper 片段 |
| Docker、配置、本地环境 | `application-dev.yml`、`docker-compose`、`RabbitMQ`、`Redis`、`MySQL` | 配置与本地环境章节及目标配置文件 |
| Nginx 静态前端、首页、登录页、订单页 | `docs/dev-ops/nginx/html`、`index.html`、`login.html`、`order-list.html`、`create_pay_order`、`query_group_buy_market_config`、`frontend-responsive-ai-service-pages` | `static-frontend-ai-service-pages.md` 的入口、接口契约、设计边界和验证方式 |

### 工作区结构

- 根目录是工作区，不是 Maven reactor；`group-buy-market/` 与 `s-pay-mall-ddd/` 是符号链接指向的独立 Maven 多模块项目。
- 两个子项目均采用 `api/app/domain/trigger/infrastructure/types` 的 DDD 分层。
- `s-pay-mall-ddd/` 已有子项目级 `code_copilot/`，处理该项目内部变更时应优先读取。

### 常见验证命令

```bash
cd group-buy-market && mvn clean package
cd s-pay-mall-ddd && mvn clean package
cd group-buy-market && mvn -pl group-buy-market-lpc-app -DskipTests=false test
cd s-pay-mall-ddd && mvn -pl s-pay-mall-ddd-lpc-app -DskipTests=false test
```

部分 app 模块可能在 Maven Surefire 配置中默认跳过测试，执行测试时需按实际 POM 显式覆盖。

## 待沉淀

- TODO: 后续将 [platform-knowledge.md](platform-knowledge.md) 拆分为主题文件，例如 `workspace-map.md`、`order-payment-flow.md`、`group-buy-flow.md`、`refund-flow.md`、`mq-contracts.md`、`schema-map.md`、`dev-ops-map.md`。
- TODO: 涉及具体 Mapper 或 Repository 改动时，按目标表重新校验字段映射。
- TODO: 涉及 MQ 可靠投递改造时，补充确认模式、重试、死信和消费幂等设计。
