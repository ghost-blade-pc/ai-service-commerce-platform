# 任务拆分 — 本地 Docker 栈网络隔离与 FRP 回调入口整理

> 推荐顺序：配置现状确认 -> Compose 网络与端口 -> Nginx upstream -> 验证与文档同步

## 前置条件

- [x] 已读取根级 `code_copilot/rules/*.md`
- [x] 已读取目标子项目 `AGENTS.md`
- [x] 涉及 `s-pay-mall-ddd/` 时已读取其子项目级相关入口与配置证据
- [x] 已确认影响项目和 DDD 层级
- [x] 已确认是否涉及资金、订单状态、退款、拼团、MQ、外部接口、数据库
- [x] 已检查 Git 分支和未提交变更

## Task 1: 统一 Docker Compose 项目网络

- **目标**：让基础设施和应用部署文件加入同一个项目专用 Docker bridge 网络。
- **影响项目**：cross-project
- **DDD 层级**：配置 / docs-dev-ops
- **涉及文件**：
  - `docs/dev-ops/docker-compose-environment.yml`：定义项目专用网络，必要时设置稳定网络名。
  - `docs/dev-ops/docker-compose-app-v1.0.yml`：复用同一网络，不再依赖 `software_my-network`。
- **关键签名**：不涉及 Java 方法签名。
- **依赖**：用户确认 `spec.md` 待澄清事项。
- **风险标记**：外部接口 / 安全
- **验收标准**：两个 compose 文件的服务可通过 `mysql`、`redis`、`rabbitmq`、`group-buy-market-01`、`group-buy-market-02`、`s-pay-mall-app` 等服务名互通。
- **命名约定**：Compose project name 使用 `ai-service-commerce-platform`，共享网络名称使用 `ai-service-commerce-platform-network`。
- **验证命令**：
  ```bash
  docker compose -f docs/dev-ops/docker-compose-environment.yml config
  docker compose -f docs/dev-ops/docker-compose-app-v1.1.yml config
  ```
- **状态**：completed

## Task 2: 保留调试端口并对齐应用端口

- **目标**：后端服务继续暴露宿主机端口用于本机调试，同时保持容器间通过项目专用网络通信。
- **影响项目**：cross-project
- **DDD 层级**：配置 / docs-dev-ops
- **涉及文件**：
  - `docs/dev-ops/docker-compose-app-v1.0.yml`：保留拼团 `8091:8091`、`8092:8091`；将 `s-pay-mall-app` 调整为 `8080:8080` 且 `SERVER_PORT=8080`。
  - `docs/dev-ops/docker-compose-environment.yml`：基础设施管理端口暂按现状保留，便于本机测试排查。
- **关键签名**：不涉及 Java 方法签名。
- **依赖**：Task 1
- **风险标记**：外部接口 / 安全
- **验收标准**：FRP 所需入口可访问，后端调试端口可直接从宿主机访问，容器间仍使用服务名通信。
- **验证命令**：
  ```bash
  docker compose -f docs/dev-ops/docker-compose-app-v1.1.yml config
  ```
- **状态**：completed

## Task 3: 调整 Nginx upstream 与 FRP 入口

- **目标**：Nginx 使用 Docker 服务名访问后端，并将宿主机 `9001` 映射到 Nginx 容器入口；宿主机 `8080` 保留给 `s-pay-mall-app`。
- **影响项目**：cross-project
- **DDD 层级**：配置 / docs-dev-ops
- **涉及文件**：
  - `docs/dev-ops/nginx/conf/conf.d/localhost.conf`：将 upstream 从 `172.17.0.1:*` 改为服务名和容器内端口。
  - `docs/dev-ops/docker-compose-app-v1.0.yml`：Nginx 端口映射对齐 FRP 监听端口，使用 `9001:80`。
- **关键签名**：不涉及 Java 方法签名。
- **依赖**：Task 1、Task 2
- **风险标记**：支付回调 / 微信回调 / 外部接口
- **验收标准**：`127.0.0.1:8080` 直连 `s-pay-mall-app`；`127.0.0.1:9001` 进入 Nginx；Nginx 中 `/api/v1/alipay/`、`/api/v1/weixin/` 转发到 `s-pay-mall-app:8080`，`/api/v1/gbm/` 负载到两个 `group-buy-market` 实例。
- **验证命令**：
  ```bash
  docker exec <nginx-container> nginx -t
  curl -I http://127.0.0.1:8080/
  curl -I http://127.0.0.1:9001/
  ```
- **状态**：completed

## Task 4: 更新注释、校验并记录结果

- **目标**：更新启动命令注释，记录实际验证结果和剩余风险。
- **影响项目**：cross-project
- **DDD 层级**：配置 / docs-dev-ops / code_copilot
- **涉及文件**：
  - `docs/dev-ops/docker-compose-app-v1.0.yml`
  - `docs/dev-ops/docker-compose-environment.yml`
  - `code_copilot/changes/project-local-docker-stack/log.md`
- **关键签名**：不涉及 Java 方法签名。
- **依赖**：Task 1、Task 2、Task 3
- **风险标记**：安全
- **验收标准**：compose config 校验通过，Nginx 配置校验通过，change log 有真实命令和结果。
- **验证命令**：
  ```bash
  git diff -- docs/dev-ops/docker-compose-app-v1.1.yml docs/dev-ops/docker-compose-environment.yml docs/dev-ops/nginx/conf/conf.d/localhost.conf
  ```
- **状态**：completed

## Task 5: 修复静态页远程接口基址残留

- **目标**：修复本地 Docker/Nginx 访问静态页时，浏览器仍请求旧远程部署域名导致微信二维码、拼团接口不可达的问题。
- **影响项目**：cross-project
- **DDD 层级**：前端静态资源 / docs-dev-ops
- **涉及文件**：
  - `docs/dev-ops/nginx/html/login.html`：微信二维码 ticket 与登录轮询接口使用同源 `/api/v1/login/...`。
  - `docs/dev-ops/nginx/html/index.html`：拼团配置查询与创建支付单接口使用同源 `/api/v1/gbm/...`、`/api/v1/alipay/...`。
  - `docs/dev-ops/nginx/html/order-list.html`：订单查询与退单接口使用同源 `/api/v1/alipay/...`。
- **关键签名**：不涉及 Java 方法签名。
- **依赖**：Task 3 已保证 Nginx 能代理 `/api/v1/login/`、`/api/v1/gbm/`、`/api/v1/alipay/`。
- **风险标记**：微信登录 / 支付下单 / 拼团接口 / 外部接口
- **验收标准**：业务 HTML 中不再将业务 API 基址硬编码为 `http://licodetech.top`；访问 `http://localhost:9001/` 时浏览器请求走当前 Nginx 同源入口。
- **验证命令**：
  ```bash
  rg -n "licodetech\\.top|sPayMallUrl|groupBuyMarketUrl|fetch\\(" docs/dev-ops/nginx/html/index.html docs/dev-ops/nginx/html/login.html docs/dev-ops/nginx/html/order-list.html
  ```
- **状态**：completed

## Task 6: 禁用本地 Nginx 静态页缓存并验证拼团链路

- **目标**：避免浏览器继续命中旧版 HTML，导致页面没有执行新版同源 `/api/v1/gbm/...` 请求。
- **影响项目**：cross-project
- **DDD 层级**：Nginx 本地部署配置 / docs-dev-ops
- **涉及文件**：
  - `docs/dev-ops/nginx/conf/conf.d/localhost.conf`：在静态资源入口添加 no-cache 响应头。
- **关键签名**：不涉及 Java 方法签名。
- **依赖**：Task 5 已将 HTML 业务 API 基址改为同源入口。
- **风险标记**：本地联调缓存 / 前端请求链路
- **验收标准**：`http://127.0.0.1:9001/` 响应带 `Cache-Control: no-store`，`POST /api/v1/gbm/index/query_group_buy_market_config` 经 Nginx 返回 `code=0000`。
- **验证命令**：
  ```bash
  docker exec ai-service-commerce-platform-nginx nginx -t
  docker exec ai-service-commerce-platform-nginx nginx -s reload
  curl --noproxy '*' -I http://127.0.0.1:9001/
  curl --noproxy '*' -i -X POST http://127.0.0.1:9001/api/v1/gbm/index/query_group_buy_market_config -H 'Content-Type: application/json' -d '{"userId":"test","source":"s01","channel":"c01","goodsId":"9890001"}'
  ```
- **状态**：completed

## Task 7: 优化支付宝支付完成后的本地页面跳转

- **目标**：降低支付宝支付完成后页面通过 FRP 回跳导致的加载卡顿。
- **影响项目**：s-pay-mall-ddd / docs-dev-ops
- **DDD 层级**：支付配置 / 本地部署配置
- **涉及文件**：
  - `docs/dev-ops/docker-compose-app-v1.0.yml`：仅调整本地 Docker 运行态 `ALIPAY_RETURN_URL`。
- **关键签名**：不涉及 Java 方法签名。
- **依赖**：FRP 仍转发 `8080` 给商城后端，保障支付宝异步通知 `notify_url` 可达。
- **风险标记**：支付宝回调 / 支付状态 / 外部接口
- **验收标准**：新创建的支付宝支付表单中 `notify_url` 仍为 `http://licodetech.top:8080/api/v1/alipay/alipay_notify_url`，`return_url` 为 `http://localhost:9001/`。
- **验证命令**：
  ```bash
  docker compose -f docs/dev-ops/docker-compose-app-v1.1.yml config --quiet
  docker compose -f docs/dev-ops/docker-compose-app-v1.1.yml up -d s-pay-mall-app
  docker exec ai-service-commerce-platform-s-pay-mall-app printenv ALIPAY_NOTIFY_URL ALIPAY_RETURN_URL
  curl --noproxy '*' -i -X POST http://127.0.0.1:8080/api/v1/alipay/create_pay_order -H 'Content-Type: application/json' -d '{"userId":"return-url-test","productId":"9890001","marketType":0}'
  ```
- **状态**：completed

## 变更摘要

- **总文件数**：修改 3 个部署文件、3 个静态页文件，更新 3 个 change 文档。
- **Spec-Plan 偏差记录**：实现前发现 `8080` 不能同时映射给 Nginx 与 `s-pay-mall-app`，最终按用户要求保留 `s-pay-mall-app` 的 `8080:8080`，Nginx 使用 `9001:80`。
- **验证结果**：`docker compose -f docs/dev-ops/docker-compose-environment.yml config` 与 `docker compose -f docs/dev-ops/docker-compose-app-v1.0.yml config --quiet` 均通过；RabbitMQ 环境变量覆盖已在 app compose config 输出中确认；业务 HTML 的接口基址已改为同源入口；Nginx 首页响应已带 no-cache 头，拼团接口经 `9001` 返回 `code=0000`；支付宝支付表单已验证 `return_url` 回本机、`notify_url` 走 FRP。
- **运行态处理**：已在本机运行态执行 Nginx 配置校验与 reload，并重建 `s-pay-mall-app` 使新的 `ALIPAY_RETURN_URL` 生效。
- **归档状态**：completed。
- **遗留问题**：支付成功后的主动查单第二层能力不在本 change 内实现，已保留在 `s-pay-mall-ddd/code_copilot/changes/payment-callback-realtime/` 等待后续确认。
