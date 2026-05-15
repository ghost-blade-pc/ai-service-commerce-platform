# 任务拆分 — 云服务器域名 Docker 部署配置调整

> 推荐顺序：部署入口 → 应用 profile → 外部回调 URL → 中间件连接 → 安全收口 → 验证

## 前置条件

- [x] 已读取根级 `code_copilot/rules/*.md`
- [x] 已读取目标子项目 `AGENTS.md`
- [x] 涉及 `s-pay-mall-ddd/` 时已读取其子项目级 `code_copilot/`
- [x] 已确认影响项目和 DDD 层级
- [x] 已确认是否涉及资金、订单状态、退款、拼团、MQ、外部接口、数据库
- [x] 已检查 Git 分支和未提交变更

## Task 1: 确认云服务器入口参数

- **目标**：确认域名、协议、对外端口与安全组策略，避免回调 URL 与 Nginx 入口不一致。
- **影响项目**：cross-project
- **DDD 层级**：docs / app config
- **涉及文件**：
  - `code_copilot/changes/cloud-domain-docker-deploy/spec.md`：记录确认结果
- **关键签名**：无
- **依赖**：无
- **风险标记**：外部接口 / 安全
- **验收标准**：明确最终公网入口为 `http://licodetech.top`，Docker 运行态切换为 `prod`。
- **验证命令**：无
- **状态**：completed

## Task 2: 调整 docs/tag/v1 Nginx 与 app compose

- **目标**：将 Docker app 部署参数从本机联调状态调整为云服务器域名访问状态。
- **影响项目**：cross-project
- **DDD 层级**：docs / deployment
- **涉及文件**：
  - `docs/tag/v1/docker-compose-app-v1.1.yml`：调整 profile、对外端口、支付宝回调/回跳、容器环境变量
  - `docs/tag/v1/nginx/conf/conf.d/localhost.conf`：调整 `server_name` 与必要的代理入口
- **关键签名**：无
- **依赖**：Task 1
- **风险标记**：外部接口 / 安全
- **验收标准**：`docker compose config` 通过；Nginx 使用 `80:80`；应用容器不占用宿主机 `8080` 和 `9001`；Nginx 对外入口与支付宝/微信配置一致。
- **验证命令**：
  ```bash
  docker compose -f docs/tag/v1/docker-compose-app-v1.1.yml config
  ```
- **状态**：completed

## Task 3: 调整 group-buy-market production 配置

- **目标**：让 `group-buy-market` 的 `prod` profile 能在 Docker 云服务器环境中连接 MySQL、Redis、RabbitMQ 与可选 Logstash。
- **影响项目**：group-buy-market
- **DDD 层级**：app
- **涉及文件**：
  - `group-buy-market/group-buy-market-lpc-app/src/main/resources/application-prod.yml`：调整 MySQL、Redis、xfg-wrench register、Logstash 等配置为环境变量占位或容器服务名默认值
- **关键签名**：无
- **依赖**：Task 1
- **风险标记**：数据库 / MQ / 安全
- **验收标准**：配置不再默认指向 `127.0.0.1` 中间件，且不新增真实密码。
- **验证命令**：
  ```bash
  cd group-buy-market && mvn -pl group-buy-market-lpc-app -am -DskipTests package
  ```
- **状态**：completed

## Task 4: 调整 s-pay-mall-ddd production 配置

- **目标**：让 `s-pay-mall-ddd` 的 `prod` profile 与云服务器域名、支付/微信回调、拼团营销服务调用保持一致。
- **影响项目**：s-pay-mall-ddd
- **DDD 层级**：app
- **涉及文件**：
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/src/main/resources/application-prod.yml`：调整公网 URL、中间件地址、支付宝/微信敏感配置为环境变量占位
- **关键签名**：无
- **依赖**：Task 1、Task 2
- **风险标记**：资金 / 外部接口 / 数据库 / MQ / 安全
- **验收标准**：`alipay.notify_url`、`alipay.return_url`、`app.config.group-buy-market.*` 与云服务器入口一致，配置不新增真实密钥。
- **验证命令**：
  ```bash
  cd s-pay-mall-ddd && mvn -pl s-pay-mall-ddd-lpc-app -am -DskipTests package
  ```
- **状态**：completed

## Task 5: 收口中间件公网暴露与运维入口

- **目标**：按云服务器安全策略收口 MySQL、Redis、RabbitMQ、管理后台端口暴露，确保仅服务器内网可访问。
- **影响项目**：cross-project
- **DDD 层级**：docs / deployment
- **涉及文件**：
  - `docs/tag/v1/docker-compose-environment.yml`：根据确认结果保留、绑定本机或移除中间件宿主机端口映射
- **关键签名**：无
- **依赖**：Task 1
- **风险标记**：数据库 / MQ / 安全
- **验收标准**：公网只开放必要用户访问入口；MySQL、Redis、RabbitMQ 管理端仅服务器内网访问。
- **验证命令**：
  ```bash
  docker compose -f docs/tag/v1/docker-compose-environment.yml config
  ```
- **状态**：completed

## Task 6: 配置验证与运行态冒烟

- **目标**：验证 Docker 配置、Nginx 配置和关键 HTTP 链路。
- **影响项目**：cross-project
- **DDD 层级**：deployment / app
- **涉及文件**：
  - `code_copilot/changes/cloud-domain-docker-deploy/log.md`：记录验证结果
- **关键签名**：无
- **依赖**：Task 2、Task 3、Task 4、Task 5
- **风险标记**：外部接口 / 安全
- **验收标准**：静态首页可访问，拼团配置接口、登录二维码接口、支付宝下单入口至少完成一次冒烟验证。
- **验证命令**：
  ```bash
  docker exec ai-service-commerce-platform-nginx nginx -t
  curl --noproxy '*' -I http://<domain>/
  ```
- **状态**：blocked：当前 WSL 环境缺少 Docker CLI，无法执行 Docker/Nginx 运行态冒烟；Maven 构建已通过。

## 变更摘要

- **总文件数**：7 个文件，包括 4 个部署/配置文件与 3 个 change 文档
- **Spec-Plan 偏差记录**：Docker Compose / Nginx 运行态验证因当前环境缺少 Docker CLI 被阻塞
- **验证结果**：两个 Maven app reactor `-DskipTests package` 均通过；`docker compose config` 未执行成功，原因是 `docker` 命令不可用；敏感配置改为运行时必填环境变量并新增 `.env.example`
- **遗留问题**：上线前需要在 `docs/tag/v1/.env` 填入真实微信/支付宝值，在支付宝/微信平台侧核对公网回调 URL，并在云服务器真实 Docker 环境执行 compose 与 Nginx 验证
