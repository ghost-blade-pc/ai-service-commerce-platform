# 云服务器域名 Docker 部署配置调整
> status: done
> created: 2026-05-15
> scope: cross-project
> complexity: 中等

## 1. 背景与目标

计划将 `ddd-trade-marketing-platform` 工作区按既有 Docker Compose 部署策略迁移到有域名的云服务器，使 Nginx 静态页、商城支付服务、拼团营销服务、MySQL、Redis、RabbitMQ 在云服务器上可运行，并通过公网域名访问。

本次 `/apply` 重点修改 `docs/tag/v1/` 部署配置与两个子项目的 `application-prod.yml`，并避免覆盖当前工作区已有的无关改动。

## 2. 业务边界

- 影响项目：`group-buy-market`、`s-pay-mall-ddd`、根级 `docs/tag/v1`
- 所属上下文：配置 / 部署 / 登录 / 支付回调 / 拼团营销服务调用
- 调用方向：
  - 浏览器访问 Nginx 静态页与同源 API。
  - Nginx 代理 `/api/v1/gbm/` 到 `group-buy-market`，代理 `/api/v1/alipay/`、`/api/v1/login/`、`/api/v1/weixin/` 到 `s-pay-mall-ddd`。
  - `s-pay-mall-ddd` 通过 Retrofit 调用拼团营销服务。
  - 支付宝与微信服务器通过公网域名回调 `s-pay-mall-ddd`。
- 是否跨项目：是。
- 是否涉及资金、订单状态、拼团状态：涉及支付回调入口与拼团回调入口的公网地址配置，但不改变资金或状态流转代码。

## 3. 代码现状（Research Findings）

### 3.1 相关入口

- Nginx:
  - `docs/tag/v1/nginx/conf/conf.d/localhost.conf:1` 定义 `backend_servers_group_buy_market`，上游为 `group-buy-market-01:8091` 与 `group-buy-market-02:8091`。
  - `docs/tag/v1/nginx/conf/conf.d/localhost.conf:6` 定义 `backend_servers_s_pay_mall`，上游为 `s-pay-mall-app:8080`。
  - `docs/tag/v1/nginx/conf/conf.d/localhost.conf:13` `server_name` 为 `licodetech.top www.licodetech.top`。
  - `docs/tag/v1/nginx/conf/conf.d/localhost.conf:15`、`:23`、`:31`、`:39` 分别代理拼团、支付宝、登录、微信入口。
- Docker Compose:
  - `/apply` 前 `docs/tag/v1/docker-compose-app-v1.1.yml:12` Nginx 暴露 `9001:80`；本次已调整为 `80:80`。
  - `/apply` 前 `docs/tag/v1/docker-compose-app-v1.1.yml:30`、`:69`、`:105` 三个应用容器都显式设置 `SPRING_PROFILES_ACTIVE=dev`；本次已调整为 `prod`。
  - `/apply` 前 `docs/tag/v1/docker-compose-app-v1.1.yml:48` 支付宝异步通知为 `http://licodetech.top:8080/api/v1/alipay/alipay_notify_url`；本次已调整为 `http://licodetech.top/api/v1/alipay/alipay_notify_url`。
  - `/apply` 前 `docs/tag/v1/docker-compose-app-v1.1.yml:49` 支付宝浏览器回跳仍为 `http://localhost:9001/`；本次已调整为 `http://licodetech.top`。
  - `docs/tag/v1/docker-compose-app-v1.1.yml:32` 商城调用拼团服务的 `APP_CONFIG_GROUP_BUY_MARKET_API_URL` 为 `http://group-buy-market-front`，即走容器内 Nginx。
  - `docs/tag/v1/docker-compose-app-v1.1.yml:33` 拼团回调地址为 `http://s-pay-mall-app:8080/api/v1/alipay/group_buy_notify`，即容器内回调。
  - `docs/tag/v1/docker-compose-environment.yml:15`、`:52`、`:93`、`:94` 暴露 MySQL `13306`、Redis `16379`、RabbitMQ `5672/15672`。
- 静态页:
  - `docs/tag/v1/nginx/html/index.html:449` 和 `:518` 使用相对路径请求支付宝下单与拼团配置。
  - `docs/tag/v1/nginx/html/login.html:223` 设置 `sPayMallUrl = ""`，登录接口同源访问。
  - `docs/tag/v1/nginx/html/order-list.html:346` 设置 `sPayMallUrl = ""`，订单查询与退单接口同源访问。
- `group-buy-market` 配置:
  - `group-buy-market/group-buy-market-lpc-app/src/main/resources/application.yml:5` 默认 profile 为 `prod`。
  - `group-buy-market/group-buy-market-lpc-app/src/main/resources/application-prod.yml:30` MySQL 仍指向 `127.0.0.1:3306`。
  - `group-buy-market/group-buy-market-lpc-app/src/main/resources/application-prod.yml:45` RabbitMQ 指向 `rabbitmq:5672`。
  - `group-buy-market/group-buy-market-lpc-app/src/main/resources/application-prod.yml:81`、`:98` Redis 与 xfg-wrench 注册中心仍指向 `127.0.0.1:16379`。
  - `group-buy-market/group-buy-market-lpc-app/src/main/resources/application-prod.yml:103` Logstash 指向 `127.0.0.1`。
- `s-pay-mall-ddd` 配置:
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/resources/application.yml:5` 默认 profile 为 `prod`。
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/resources/application-prod.yml:13` 拼团 API 地址为 `http://licodetech.top`。
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/resources/application-prod.yml:14` 拼团回调通知为 `http://licodetech.top/api/v1/alipay/group_buy_notify`。
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/resources/application-prod.yml:42` MySQL 仍指向 `127.0.0.1:3306`。
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/resources/application-prod.yml:57` RabbitMQ 指向 `rabbitmq:5672`。
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/resources/application-prod.yml:116` 支付宝异步通知为 `http://licodetech.top/api/v1/alipay/alipay_notify_url`。
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/resources/application-prod.yml:118` 支付宝浏览器回跳为 `http://licodetech.top`。
- 配置使用点:
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/java/top/licodetech/mall/config/Retrofit2Config.java:16` 使用 `app.config.group-buy-market.api-url` 作为 Retrofit baseUrl。
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-infrastructure/src/main/java/top/licodetech/mall/infrastructure/adapter/port/ProductPort.java:26` 使用 `app.config.group-buy-market.notify-url`。
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-domain/src/main/java/top/licodetech/mall/domain/order/service/OrderService.java:45`、`:47` 使用 `alipay.notify_url` 与 `alipay.return_url`。
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/AliPayController.java:177` 接收支付宝异步通知，`:219` 接收拼团回调。
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/WeixinPortalController.java:23` 微信回调入口为 `/api/v1/weixin/portal/`。

### 3.2 现有实现

`/apply` 前 `docs/tag/v1` 的部署文件更像“本机 Docker 联调 + 部分公网域名”的混合状态：

- Nginx 已按容器服务名代理后端，适合 Docker bridge 网络。
- 静态页已基本使用同源相对路径，适合域名部署。
- `/apply` 前应用容器仍设置 `SPRING_PROFILES_ACTIVE=dev`，因此 Docker 运行时优先使用 `application-dev.yml` 与 compose 环境变量覆盖；本次已切换为 `prod`。
- `/apply` 前 `application-prod.yml` 里仍存在 `127.0.0.1`、固定账号密码和支付/微信密钥；本次已将中间件地址改为容器服务名默认值，并将支付/微信敏感值改为运行时必填环境变量。

### 3.3 发现与风险

- `/apply` 前 `ALIPAY_RETURN_URL=http://localhost:9001/` 会把用户浏览器回跳到用户本机；本次已改为 `http://licodetech.top`。
- `/apply` 前 `ALIPAY_NOTIFY_URL=http://licodetech.top:8080/...` 与 Nginx 对外入口 `9001:80`/`80` 不一致；本次已统一到 `http://licodetech.top/api/v1/alipay/alipay_notify_url`。
- `/apply` 前 `SPRING_PROFILES_ACTIVE=dev` 与要检查的 `application-prod.yml` 存在策略冲突；本次已按用户确认切换为 `prod + env override`。
- `/apply` 前 MySQL、Redis、RabbitMQ 相关端口暴露策略与云服务器目标不一致；本次按用户确认改为 MySQL/Redis 本体仅绑定 `127.0.0.1`，phpMyAdmin、Redis Admin、RabbitMQ AMQP/管理端对外开放。
- `/apply` 前 `s-pay-mall-ddd/application-prod.yml` 内包含真实或类似真实的支付宝、微信密钥；本次改为运行时必填环境变量，不使用 `please-change` 这类假占位默认值，避免服务假启动后对接失败。
- 如果改为公网 HTTPS 域名，支付宝、微信、前端、Nginx `server_name`、回调 URL 必须统一 scheme、host、port。

## 4. DDD 模块影响

| 项目 | 模块 | 是否影响 | 文件/类 | 说明 |
| --- | --- | --- | --- | --- |
| group-buy-market | api | 否 | - | 不改公开契约 |
| group-buy-market | trigger | 否 | - | 不改 Controller / Listener |
| group-buy-market | domain | 否 | - | 不改领域规则 |
| group-buy-market | infrastructure | 否 | - | 不改 DAO、MQ 发布或 Redis 代码 |
| group-buy-market | app | 是 | `application-prod.yml`、Docker app compose | 调整 profile、数据源、Redis、RabbitMQ、Logstash 参数 |
| group-buy-market | types | 否 | - | 不改公共类型 |
| s-pay-mall-ddd | api | 否 | - | 不改公开契约 |
| s-pay-mall-ddd | trigger | 否 | `AliPayController`、`WeixinPortalController` 只作为配置入口证据 | 不改入口代码 |
| s-pay-mall-ddd | domain | 否 | `OrderService` 只作为配置入口证据 | 不改支付状态规则 |
| s-pay-mall-ddd | infrastructure | 否 | `Retrofit2Config`、`ProductPort` 只作为配置入口证据 | 不改网关代码 |
| s-pay-mall-ddd | app | 是 | `application-prod.yml`、Docker app compose | 调整公网域名、支付/微信回调、数据源、RabbitMQ 参数 |
| s-pay-mall-ddd | types | 否 | - | 不改公共类型 |
| root docs | docs/tag/v1 | 是 | Docker Compose、Nginx、静态页配置 | 云服务器部署入口 |

## 5. 功能点

- [x] 将 `docs/tag/v1` Docker app compose 从本机访问参数调整为云服务器域名可访问参数。
- [x] 统一 Nginx `server_name`、对外端口、支付宝 `notify_url`、`return_url`、微信回调域名。
- [x] 明确 Docker 运行 profile：云服务器使用 `prod + env override`，并让 compose 与 `application-prod.yml` 一致。
- [x] 将生产敏感配置改为运行时必填环境变量，不新增真实密钥。
- [x] 保持容器内服务发现继续走 Docker service name，例如 `mysql`、`redis`、`rabbitmq`、`s-pay-mall-app`、`group-buy-market-front`。
- [x] 按用户确认收敛端口策略：Nginx 对外 `80`，MySQL/Redis 本体仅本机，指定管理端口对外暴露。

## 6. 业务规则

- 状态流转：不改变订单、支付、退款、拼团状态流转。
- 金额/优惠/退款：不改变金额计算、优惠计算和退款逻辑。
- 拼团营销：只调整商城调用拼团营销服务与拼团回调地址的运行配置。
- 幂等：不改变现有支付回调、拼团回调、MQ 消费幂等行为。
- 异常与补偿：不改变现有 Job、MQ 或退款补偿策略。

## 7. 数据变更

| 操作 | 项目 | 表名 | 字段/索引 | 说明 |
| --- | --- | --- | --- | --- |
| 无 | - | - | - | 本 change 不修改数据库结构 |

- 是否需要改 MyBatis mapper：否。
- 是否需要改初始化 SQL：暂不需要；若发现初始化数据里写死 `127.0.0.1` 回调地址，需要单独确认是否清理示例数据。

## 8. 接口与消息变更

### 8.1 REST API

| 方向 | 服务 | Path | Method | Request DTO | Response DTO | 鉴权/签名 |
| --- | --- | --- | --- | --- | --- | --- |
| 外部回调 | s-pay-mall-ddd | `/api/v1/alipay/alipay_notify_url` | POST | 支付宝表单参数 | `success` / `false` | 支付宝 RSA2 验签，现有逻辑不变 |
| 外部回调 | s-pay-mall-ddd | `/api/v1/weixin/portal/receive` | GET/POST | 微信请求参数/XML | 文本/XML | 微信签名校验，现有逻辑不变 |
| 内部/公网代理 | group-buy-market | `/api/v1/gbm/**` | 多个 | 现有 DTO | 现有 Response | 不新增 |
| 内部/公网代理 | s-pay-mall-ddd | `/api/v1/alipay/**`、`/api/v1/login/**` | 多个 | 现有 DTO | 现有 Response | 不新增 |

### 8.2 MQ 契约

| 方向 | Exchange | Routing Key | Queue | Message | 幂等键 |
| --- | --- | --- | --- | --- | --- |
| 不变 | `s_pay_mall_exchange` | `topic.order_pay_success` | `s_pay_mall_queue_2_order_pay_success` | 现有消息 | 现有逻辑 |
| 不变 | `group_buy_market_exchange` | `topic.team_success` | `s_pay_mall_queue_2_topic_team_success` | 现有消息 | 现有逻辑 |
| 不变 | `group_buy_market_exchange` | `topic.team_refund` | `s_pay_mall_queue_2_topic_team_refund` | 现有消息 | 现有逻辑 |

## 9. 测试策略

- 测试范围：配置渲染、Nginx 配置语法、容器网络连通、静态页同源请求、支付宝/微信回调 URL 可达性。
- 优先测试类型：Docker Compose 配置校验与手工 HTTP 验证。
- 需要独立 `test-spec.md`：否，当前为配置部署变更，任务验收中记录命令即可。
- 推荐验证命令：
  - `docker compose -f docs/tag/v1/docker-compose-environment.yml config`
  - `docker compose -f docs/tag/v1/docker-compose-app-v1.1.yml config`
  - `docker exec ai-service-commerce-platform-nginx nginx -t`
  - `curl --noproxy '*' -I http://<domain>/`
  - `curl --noproxy '*' -i http://<domain>/api/v1/gbm/index/query_group_buy_market_config`
  - `curl --noproxy '*' -i http://<domain>/api/v1/login/weixin_qrcode_ticket_scene?sceneStr=test`

## 10. 待澄清

- [x] 云服务器最终使用域名：`licodetech.top`。
- [x] 首期公网入口使用 `http://licodetech.top`，目标体验是浏览器或手机直接输入域名即可访问服务器上运行的程序。
- [x] Nginx 对外端口映射使用 `80:80`，避免用户访问时需要额外输入端口。
- [x] Docker 运行态切换为 `SPRING_PROFILES_ACTIVE=prod`，同时继续用 compose 环境变量覆盖敏感值。
- [x] 云服务器已有 `frps` 监听 `8080` 和 `9001`，应用容器不能继续占用这两个宿主机端口；需要修改 `s-pay-mall-app` 当前 `8080:8080` 暴露策略。其他端口不做业务语义调整。
- [x] MySQL `13306`、Redis `16379` 仅限服务器本机访问；phpMyAdmin `8899`、Redis Admin `8081`、RabbitMQ `5672/15672` 按用户确认对外暴露，供远程管理访问。
- [x] 支付宝与微信平台侧是否已经配置对应公网回调域名无法从本仓库确认，作为上线前外部人工核对项处理。建议支付宝异步通知配置为 `http://licodetech.top/api/v1/alipay/alipay_notify_url`，微信服务器地址配置为 `http://licodetech.top/api/v1/weixin/portal/receive`。

## 11. 技术决策

| 决策点 | 选择 | 备选 | 理由 |
| --- | --- | --- | --- |
| 云服务器入口 | Nginx 统一入口 | 后端端口直接公网暴露 | 静态页已同源请求，Nginx 能统一 API 路由与回调入口 |
| Nginx 端口 | `80:80` | `9001:80` | 用户期望浏览器或手机直接输入域名访问，不需要追加端口 |
| 首期访问协议 | `http://licodetech.top` | `https://licodetech.top` | 当前先保证域名直连可访问；HTTPS 需要证书、443 端口和 Nginx TLS 配置，可作为后续加固 |
| 应用宿主机端口 | 避开 `8080` 和 `9001` | 继续暴露 `8080:8080` | 云服务器已有 `frps` 监听 `8080` 和 `9001`，应用应通过 Docker 网络由 Nginx 访问 |
| 中间件暴露 | MySQL/Redis 本体仅本机，phpMyAdmin/Redis Admin/RabbitMQ 按指定端口对外 | 全部端口仅内网或全部端口公网 | 用户明确要求远程访问 `8899`、`8081`、`5672`、`15672`，但 MySQL/Redis 本体仍不直接公网开放 |
| 容器内服务互访 | Docker service name | 公网域名绕回 Nginx | 容器内调用 service name 更稳定，减少公网回环依赖 |
| profile | `prod + env override` | 沿用 `dev + env override` | 用户已确认 Docker 切到 `prod`，云服务器语义也更清晰 |
| 敏感配置 | 通过 `docs/tag/v1/.env` 在运行时提供真实值，仓库只提交 `.env.example` | 继续硬编码真实值 | 符合安全规则，避免密钥扩散，同时部署时仍能使用真实微信/支付宝配置 |
| 支付回跳 | `http://licodetech.top` | `localhost:9001` | 云服务器用户浏览器不能回跳到用户本机 |

## 12. 风险与人工确认

- 资金风险：支付宝 `notify_url` 错误会导致支付状态不能实时推进，只能依赖主动查单或 Job 兜底。
- 状态流转风险：本 change 不改状态逻辑，但回调入口不可达会影响订单支付成功状态更新。
- MQ/外部接口风险：RabbitMQ 地址、交换机和队列不变；外部风险集中在支付宝、微信公网回调地址，以及 `8080` / `9001` 被 frps 占用导致的端口冲突。
- 数据风险：不改表结构；初始化 SQL 中的历史 `127.0.0.1` 示例回调地址不应被当作生产事实。
- 安全风险：仓库不提交 `.env` 和真实微信/支付宝密钥；部署前需要在服务器 `docs/tag/v1/.env` 中填写真实值。phpMyAdmin、Redis Admin、RabbitMQ 对外暴露后，应在云安全组中限制可信来源 IP，并及时修改默认密码。

## 13. 确认记录

- 确认时间：2026-05-16
- 确认人：用户
- 确认范围：已确认域名 `licodetech.top`、首期入口 `http://licodetech.top`、Nginx `80:80`、Docker 切换到 `prod`、避开 frps 已占用的 `8080` 和 `9001`；已确认 MySQL/Redis 本体仅本机访问，phpMyAdmin `8899`、Redis Admin `8081`、RabbitMQ `5672/15672` 对外暴露；仍需用户在支付宝/微信平台侧核对公网回调配置，并在云服务器 `docs/tag/v1/.env` 中填写真实敏感值
