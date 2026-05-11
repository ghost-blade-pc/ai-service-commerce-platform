# 项目上下文

> 模式：Existing Project / Workspace  
> 范围：`ddd-trade-marketing-platform` 根工作区  
> 最后更新：2026-05-09

## 工作区结构

当前根目录没有 Maven 聚合 `pom.xml`，而是通过符号链接承载两个独立 Java DDD 项目：

- `group-buy-market -> ../group-buy-market/`
- `s-pay-mall-ddd -> ../s-pay-mall-ddd/`

根目录已有 `AGENTS.md`，用于描述两个子项目的通用贡献规范。

## 子项目：group-buy-market

证据：

- Maven 父工程：`group-buy-market/pom.xml`
- groupId/artifactId：`top.licodetech.market` / `group-buy-market-lpc`
- Spring Boot parent：`org.springframework.boot:spring-boot-starter-parent:2.7.12`
- Java：`java.version=17`
- 启动类：`group-buy-market/group-buy-market-lpc-app/src/main/java/top/licodetech/market/Application.java`
- 子项目规则：`group-buy-market/AGENTS.md`

模块：

- `group-buy-market-lpc-api`
- `group-buy-market-lpc-app`
- `group-buy-market-lpc-domain`
- `group-buy-market-lpc-trigger`
- `group-buy-market-lpc-infrastructure`
- `group-buy-market-lpc-types`

已观察到的入口与适配：

- HTTP Controller：`group-buy-market/group-buy-market-lpc-trigger/src/main/java/top/licodetech/market/trigger/http/MarketTradeController.java`
- HTTP Controller：`group-buy-market/group-buy-market-lpc-trigger/src/main/java/top/licodetech/market/trigger/http/MarketIndexController.java`
- RabbitMQ Listener：`group-buy-market/group-buy-market-lpc-trigger/src/main/java/top/licodetech/market/trigger/listener/TeamSuccessTopicListener.java`
- MyBatis DAO：`group-buy-market/group-buy-market-lpc-infrastructure/src/main/java/top/licodetech/market/infrastructure/dao/IGroupBuyOrderDao.java`
- 领域服务接口：`group-buy-market/group-buy-market-lpc-domain/src/main/java/top/licodetech/market/domain/trade/service/ITradeLockOrderService.java`
- 测试目录：`group-buy-market/group-buy-market-lpc-app/src/test/java/top/licodetech/market/test/`

## 子项目：s-pay-mall-ddd

证据：

- Maven 父工程：`s-pay-mall-ddd/pom.xml`
- groupId/artifactId：`top.licodetech.mall` / `s-pay-mall-ddd-lpc`
- Spring Boot parent：`org.springframework.boot:spring-boot-starter-parent:2.7.12`
- Java：`java.version=17`
- 启动类：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/java/top/licodetech/mall/Application.java`
- 子项目 Spec 工作台：`s-pay-mall-ddd/code_copilot/README.md`

模块：

- `s-pay-mall-ddd-lpc-api`
- `s-pay-mall-ddd-lpc-app`
- `s-pay-mall-ddd-lpc-domain`
- `s-pay-mall-ddd-lpc-trigger`
- `s-pay-mall-ddd-lpc-infrastructure`
- `s-pay-mall-ddd-lpc-types`

已观察到的入口与适配：

- HTTP Controller：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/AliPayController.java`
- HTTP Controller：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/LoginController.java`
- RabbitMQ Listener：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/listener/OrderPaySuccessListener.java`
- RabbitMQ Listener：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/listener/RefundSuccessTopicListener.java`
- MyBatis DAO：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/dao/IOrderDao.java`
- 外部网关：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/gateway/IGroupBuyMarketService.java`
- 测试目录：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/test/java/top/licodetech/mall/test/`

## 运行与基础设施

两个子项目均包含 `docs/dev-ops/`，可见 Docker Compose 本地环境文件：

- `group-buy-market/docs/dev-ops/docker-compose-environment.yml`
- `s-pay-mall-ddd/docs/dev-ops/docker-compose-environment.yml`

POM 与配置显示当前工作区涉及 MyBatis/MySQL、RabbitMQ、Redis/Redisson、日志采集和 HTTP 客户端。具体端口、账号、队列、topic 以各子项目 `application-*.yml` 和实际配置类为准。

## 规则优先级

1. 用户本轮明确指令。
2. 目标子项目内的 `AGENTS.md`、`code_copilot/`。
3. 根级 `code_copilot/`。
4. 根级 `AGENTS.md`。

若规则冲突，停止并说明冲突，不直接猜测。
