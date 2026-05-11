# 本地 Docker 栈网络隔离与 FRP 回调入口整理
> status: done
> created: 2026-05-11
> completed: 2026-05-11
> scope: cross-project
> complexity: 中等

## 1. 背景与目标

当前根工作区 `docs/dev-ops/` 下存在两类 Docker Compose 文件：

- `docs/dev-ops/docker-compose-app-v1.0.yml`：部署前端 Nginx、`s-pay-mall-ddd` 后端、`group-buy-market` 两个后端实例。
- `docs/dev-ops/docker-compose-environment.yml`：部署 MySQL、Redis、RabbitMQ 以及管理工具。

目标是让这两份本机测试部署文件属于同一个项目专用 Docker 栈，使用同一条项目专用 bridge 网络，并减少对宿主机和外部 Docker 网络的耦合。由于支付宝和微信回调依赖公网域名，本地还通过 FRP 暴露本机 `8080` 与 `9001` 端口，需要确保 Nginx 入口能承接公网回调并转发到容器内服务。用户已确认 FRP 只监听并转发本机 `8080`、`9001`，且后端端口仍需要暴露给宿主机调试。

## 2. 业务边界

- 影响项目：根工作区 `docs/dev-ops/`，同时服务 `group-buy-market` 与 `s-pay-mall-ddd` 的本地联调。
- 所属上下文：配置 / 本地部署 / 支付回调 / 微信回调。
- 调用方向：外部支付宝/微信 -> FRP -> 本机 `8080`/`9001` -> Nginx -> `s-pay-mall-ddd`；前端/商城 -> Nginx -> `group-buy-market`。
- 是否跨项目：是，部署层面跨两个独立 Java DDD 项目。
- 是否涉及资金、订单状态、拼团状态：不修改业务代码，但会影响支付宝支付回调和微信登录回调能否路由到正确入口。

## 3. 代码现状（Research Findings）

### 3.1 相关入口

- Docker 应用编排：`docs/dev-ops/docker-compose-app-v1.0.yml`。
- Docker 基础设施编排：`docs/dev-ops/docker-compose-environment.yml`。
- Nginx 主配置：`docs/dev-ops/nginx/conf/nginx.conf`。
- Nginx 路由配置：`docs/dev-ops/nginx/conf/conf.d/localhost.conf`。
- 支付宝回调入口：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/AliPayController.java#payNotify`，路径 `/api/v1/alipay/alipay_notify_url`。
- 拼团结算回调入口：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/AliPayController.java#groupBuyNotify`，路径 `/api/v1/alipay/group_buy_notify`。
- 微信公众号入口：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/WeixinPortalController.java`，路径 `/api/v1/weixin/portal/receive`。
- 拼团 HTTP 入口：`group-buy-market/group-buy-market-lpc-trigger/src/main/java/top/licodetech/market/trigger/http/MarketTradeController.java`，路径前缀 `/api/v1/gbm/trade/`。
- 生产 profile 端口与服务名：`s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/resources/application-prod.yml` 使用 `server.port=9091`、RabbitMQ 地址 `rabbitmq`；`group-buy-market/group-buy-market-lpc-app/src/main/resources/application-prod.yml` 使用 `server.port=8091`、RabbitMQ 地址 `rabbitmq`。

### 3.2 现有实现

- `docs/dev-ops/docker-compose-app-v1.0.yml` 中 Nginx 暴露宿主机 `80:80`，`s-pay-mall-app` 暴露 `9091:8080`，两个拼团实例暴露 `8091:8091`、`8092:8091`。
- `docs/dev-ops/docker-compose-app-v1.0.yml` 通过 `networks.default.name=software_my-network` 且 `external: true` 依赖外部网络。
- `docs/dev-ops/docker-compose-environment.yml` 创建内部网络 `my-network`，服务连接 `mysql`、`redis`、`rabbitmq`。
- `docs/dev-ops/nginx/conf/conf.d/localhost.conf` upstream 使用 `172.17.0.1:8091`、`172.17.0.1:8092`、`172.17.0.1:9091`，通过宿主机端口回打后端。
- `s-pay-mall-app` 的 compose 环境变量将 `SERVER_PORT` 设为 `8080`，因此容器内应用端口与 `application-prod.yml` 默认 `9091` 不一致，Nginx 应以实际容器内端口为准。
- 当前工作区已有未提交改动：`docs/dev-ops/docker-compose-app-v1.0.yml` 新增且又有工作区修改，`docs/dev-ops/docker-compose-app.yml` 被删除，`docs/dev-ops/docker-compose-environment-aliyun.yml` 被删除，`docs/dev-ops/docker-compose-environment.yml` 被修改，`docs/dev-ops/nginx/conf/conf.d/localhost.conf` 和 `docs/dev-ops/nginx/conf/nginx.conf` 新增。

### 3.3 发现与风险

- 网络不一致：应用 compose 依赖 `software_my-network`，环境 compose 创建 `my-network`，两份文件分开启动时不能保证服务名 `mysql`、`redis`、`rabbitmq` 可解析。
- 隔离性不足：应用容器和 Nginx 通过宿主机端口通信，绕过项目专用 Docker 网络。
- 命名冲突风险：`container_name: mysql`、`redis`、`rabbitmq`、`nginx` 等名称过于通用，容易与本机其他项目冲突。
- 回调风险：支付宝和微信回调依赖公网域名与 FRP 端口，Nginx 端口映射变更会直接影响 `/api/v1/alipay/` 与 `/api/v1/weixin/` 的可达性。
- 敏感配置风险：compose 和应用配置中存在微信、支付宝、数据库等本地测试凭据或密钥类配置。此次变更不应新增真实密钥；如有机会，应至少避免继续扩散。

## 4. DDD 模块影响

| 项目 | 模块 | 是否影响 | 文件/类 | 说明 |
| --- | --- | --- | --- | --- |
| group-buy-market | api | 否 | - | 不修改 API 契约 |
| group-buy-market | trigger | 间接 | `MarketTradeController` | 仅路由到既有 `/api/v1/gbm/` 入口 |
| group-buy-market | domain | 否 | - | 不修改业务规则 |
| group-buy-market | infrastructure | 间接 | `application-prod.yml` | Compose 需满足 MySQL/Redis/RabbitMQ 服务名连接 |
| group-buy-market | app | 间接 | `application-prod.yml` | 容器端口与环境变量需一致 |
| group-buy-market | types | 否 | - | 不修改 |
| s-pay-mall-ddd | api | 否 | - | 不修改 API 契约 |
| s-pay-mall-ddd | trigger | 间接 | `AliPayController`、`WeixinPortalController` | 仅调整 Nginx 路由到既有回调入口 |
| s-pay-mall-ddd | domain | 否 | - | 不修改订单、支付、登录领域逻辑 |
| s-pay-mall-ddd | infrastructure | 间接 | `application-prod.yml` | Compose 需满足 MySQL/RabbitMQ 服务名连接 |
| s-pay-mall-ddd | app | 间接 | `application-prod.yml` | 容器端口与环境变量需一致 |
| s-pay-mall-ddd | types | 否 | - | 不修改 |

## 5. 功能点

- [x] 两份 compose 使用同一个项目专用 Docker 网络：`ai-service-commerce-platform-network`。
- [x] 基础设施 compose 负责创建项目专用网络，应用 compose 复用该网络，启动顺序明确为先环境后应用。
- [x] Nginx upstream 改为容器服务名与容器内端口，例如 `group-buy-market-01:8091`、`group-buy-market-02:8091`、`s-pay-mall-app:8080`。
- [x] 后端服务保留宿主机端口暴露用于本机调试，同时所有服务加入项目专用网络，Nginx 与应用间通过服务名访问。
- [x] `8080` 按用户要求保留给 `s-pay-mall-app` 的 `8080:8080`；Nginx 入口映射 `9001:80`，用于前端与 `/api/v1/gbm/` 代理。
- [x] 容器名称使用项目名前缀，降低与本机其他 Docker 项目的冲突概率。
- [x] 统一更新 compose 文件顶部启动命令注释，避免继续引用已删除或旧文件名。
- [x] 前端静态页在本地 Docker/Nginx 运行时不再硬编码远程部署域名，接口请求使用当前 Nginx 同源入口 `/api/v1/...`。
- [x] 本地 Nginx 对静态 HTML 关闭缓存，避免浏览器继续使用旧版 HTML 中的远程接口基址。
- [x] 本地 Docker 支付场景拆分支付宝 `notify_url` 与 `return_url`：异步通知继续走 FRP 公网入口，同步页面跳转回本机 Nginx，减少支付完成后的页面卡顿。

## 6. 业务规则

- 状态流转：不修改订单、支付、退款、拼团状态。
- 金额/优惠/退款：不修改金额计算和退款逻辑。
- 拼团营销：不修改锁单、结算、退款业务规则。
- 幂等：不修改支付宝、微信、MQ 或拼团回调幂等策略。
- 异常与补偿：不修改业务补偿逻辑；仅保证本地部署链路能路由到既有入口。

## 7. 数据变更

| 操作 | 项目 | 表名 | 字段/索引 | 说明 |
| --- | --- | --- | --- | --- |
| 无 | - | - | - | 不涉及数据库结构 |

- 是否需要改 MyBatis mapper：否。
- 是否需要改初始化 SQL：否。

## 8. 接口与消息变更

### 8.1 REST API

| 方向 | 服务 | Path | Method | Request DTO | Response DTO | 鉴权/签名 |
| --- | --- | --- | --- | --- | --- | --- |
| 外部 -> s-pay-mall-ddd | 支付宝回调 | `/api/v1/alipay/alipay_notify_url` | POST | 支付宝表单参数 | `success` / `false` | `AliPayController#payNotify` 内验签 |
| 外部 -> s-pay-mall-ddd | 微信公众号 | `/api/v1/weixin/portal/receive` | GET/POST | 微信 query + XML | `echostr` / XML | `WeixinPortalController` 内签名校验 |
| s-pay-mall-ddd -> s-pay-mall-ddd | 拼团结算通知回调入口 | `/api/v1/alipay/group_buy_notify` | POST | `NotifyRequestDTO` | `success` / `error` | 当前入口未额外签名 |
| 前端/商城 -> group-buy-market | 拼团接口 | `/api/v1/gbm/**` | GET/POST | 既有 DTO | 既有 Response | 按现有实现 |

### 8.2 MQ 契约

| 方向 | Exchange | Routing Key | Queue | Message | 幂等键 |
| --- | --- | --- | --- | --- | --- |
| 不变 | `group_buy_market_exchange` | `topic.team_success` / `topic.team_refund` | 既有队列 | 既有消息 | 既有实现 |
| 不变 | `s_pay_mall_exchange` | `topic.order_pay_success` | 既有队列 | 既有消息 | 既有实现 |

## 9. 测试策略

- 测试范围：Docker Compose 配置语法、网络连通性、Nginx 配置语法、核心 HTTP 路由可达性。
- 优先测试类型：配置静态校验 + 本地容器启动验证。
- 需要独立 `test-spec.md`：否，本次为部署配置调整，可在 `tasks.md` 和 `log.md` 记录验证命令。
- 推荐验证命令：
  - `docker compose -f docs/dev-ops/docker-compose-environment.yml config`
  - `docker compose -f docs/dev-ops/docker-compose-app-v1.0.yml config`
  - `docker compose -f docs/dev-ops/docker-compose-environment.yml up -d`
  - `docker compose -f docs/dev-ops/docker-compose-app-v1.0.yml up -d`
  - `docker exec <nginx-container> nginx -t`
  - `curl -I http://127.0.0.1:8080/`
  - `curl -I http://127.0.0.1:9001/`
  - `curl http://127.0.0.1:8080/api/v1/login/check_login`

## 10. 待澄清

- [x] FRP 的 `8080` 和 `9001` 只负责监听并转发本机端口；最终分配为 `8080` 转发到 `s-pay-mall-app:8080`，`9001` 转发到 Nginx `80`，由 Nginx 分发前端静态资源和 `/api/v1/gbm/` 等代理路径。
- [x] 后端端口需要暴露给宿主机调试，应用 compose 应保留 `group-buy-market-01` 的 `8091:8091`、`group-buy-market-02` 的 `8092:8091`、`s-pay-mall-app` 的宿主机映射。
- [x] `s-pay-mall-app` 容器内端口固定为 `8080`，宿主机映射调整为 `8080:8080`。
- [x] `container_name` 改为项目名前缀形式；Compose 栈名称使用 `ai-service-commerce-platform`，共享网络名称使用 `ai-service-commerce-platform-network`。

## 11. 技术决策

| 决策点 | 选择 | 备选 | 理由 |
| --- | --- | --- | --- |
| 跨文件共享网络 | 环境 compose 创建命名网络，应用 compose 以 external 复用 | 两份 compose 都隐式创建 default 网络 | 两份文件分开启动时，命名网络更稳定 |
| Nginx upstream | 使用服务名 + 容器内端口 | 使用 `172.17.0.1` + 宿主机端口 | 服务名路由保留栈内隔离，不依赖 Docker 默认网桥地址 |
| 对外入口 | `s-pay-mall-app` 使用 `8080:8080`，Nginx 使用 `9001:80` | Nginx 同时映射 `8080:80` 与 `9001:80` | 同一宿主机端口不能同时被 Nginx 与 `s-pay-mall-app` 绑定；用户明确要求商城应用使用 `8080:8080` |
| 后端调试端口 | 保留后端宿主机端口映射 | 后端仅 `expose` | 用户需要本机直接调试后端 |
| `s-pay-mall-app` 端口 | `8080:8080` 且 `SERVER_PORT=8080` | `9091:8080` 或恢复 `9091` | 用户明确要求容器内端口为 `8080:8080` |
| Compose 栈名称 | `ai-service-commerce-platform` | 默认目录名或短缩写 | 用户指定该名称作为本地联调栈标识 |
| Docker 网络名称 | `ai-service-commerce-platform-network` | `my-network` / `software_my-network` | 明确归属当前工作区，避免复用外部通用网络 |
| 容器名前缀 | `ai-service-commerce-platform-` | 保持 `mysql`、`redis`、`nginx` 等通用名 | 降低与本机其他项目容器名称冲突的概率 |
| 业务代码 | 不修改 | 修改回调 Controller 或配置类 | 本次目标是本机部署编排，不能扩展业务行为 |
| 前端接口基址 | 静态页使用空字符串作为同源基址 | 写死 `http://licodetech.top` 或 `http://localhost:9001` | 同一份 HTML 可同时适配本机 `localhost:9001` 与 FRP 域名 `licodetech.top:9001`，避免浏览器请求绕到远程部署环境 |
| `application-dev.yml` | 暂不修改 | 直接改为 Docker 服务名或公网 FRP 地址 | Docker 运行态已由 compose 环境变量覆盖；`application-dev.yml` 的 `127.0.0.1` 更适合 IDE/宿主机直连调试，直接改成 Docker 服务名会破坏本地 IDE 启动 |
| 静态页缓存 | Nginx 本地静态资源入口返回 `Cache-Control: no-store` | 依赖浏览器手动强刷 | 本地联调期间 HTML 经常修改，旧缓存会让浏览器继续执行旧接口基址 |
| 支付宝同步跳转 | `ALIPAY_RETURN_URL=http://localhost:9001/` | `ALIPAY_RETURN_URL=http://licodetech.top:9001` | `return_url` 是用户浏览器跳转，本地联调不需要绕 FRP；`notify_url` 才需要支付宝服务器可访问 |

## 12. 风险与人工确认

- 资金风险：不改支付逻辑，但 Nginx/FRP 端口映射会影响支付宝回调可达性；已确认 FRP 只监听并转发本机 `8080` 与 `9001`，其中 `8080` 指向 `s-pay-mall-app`，`9001` 指向 Nginx。
- 状态流转风险：不改订单状态逻辑；如果回调不可达，会影响本地支付成功状态推进。
- MQ/外部接口风险：RabbitMQ 服务名需要在同一 Docker 网络内可解析；支付宝/微信公网回调需要 FRP 与 Nginx 端口一致。
- 数据风险：不改库表；MySQL 初始化脚本仍挂载 `./mysql/sql`。
- 安全风险：现有配置含本地测试凭据和第三方密钥类字段；本次不新增真实密钥，后续如允许可改为环境变量占位。

## 13. 确认记录

- 确认时间：2026-05-11 17:46:04 +0800
- 确认人：用户
- 确认范围：FRP 只监听并转发本机 `8080`、`9001`；`s-pay-mall-app` 使用 `8080:8080`；Nginx 使用 `9001:80`；后端端口需要暴露给宿主机调试；`container_name` 使用项目名前缀；Compose 栈名称使用 `ai-service-commerce-platform`；共享网络名称使用 `ai-service-commerce-platform-network`。

## 14. 归档说明

- 归档时间：2026-05-11
- 完成范围：本地 Docker Compose 栈、共享网络、容器命名、Nginx upstream、静态页同源 API、Nginx no-cache、本机支付宝 `return_url` 优化。
- 明确不包含：支付状态主动查单第二层能力；该问题已拆入 `s-pay-mall-ddd/code_copilot/changes/payment-callback-realtime/`，仍处于 propose，等待用户确认后再实现。
