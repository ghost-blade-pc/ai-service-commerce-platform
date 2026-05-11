# 变更日志 — 本地 Docker 栈网络隔离与 FRP 回调入口整理

> 记录决策、偏差、验证证据和可沉淀知识。

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
| --- | --- | --- | --- |
| 2026-05-11 17:36:00 +0800 | propose | 读取根级 `code_copilot` 规则、Docker Compose、Nginx、应用配置与回调入口，创建 change 提案 | 未修改 compose 实现 |
| 2026-05-11 17:46:04 +0800 | propose | 用户确认 FRP 只监听并转发 `8080`、`9001`，`s-pay-mall-app` 使用 `8080:8080`，后端端口需要暴露给宿主机调试 | 已同步更新 spec/tasks |
| 2026-05-11 17:46:04 +0800 | propose | 用户确认 `container_name` 使用项目名前缀，并询问网络名和栈名称 | 起初推荐 `ddd-trade-marketing-platform` / `ddd-trade-marketing-platform-network` |
| 2026-05-11 17:46:04 +0800 | propose | 用户指定使用 `ai-service-commerce-platform` 作为命名基础 | 已同步 spec/tasks |
| 2026-05-11 17:46:04 +0800 | apply | 实现前发现 `8080:80` 与 `s-pay-mall-app` 的 `8080:8080` 会冲突 | 按用户硬性要求将 `8080` 留给商城应用，Nginx 使用 `9001:80` |
| 2026-05-11 17:57:10 +0800 | apply | 完成 compose、Nginx upstream 与 change 文档同步 | `docker compose config` 静态校验通过 |
| 2026-05-11 17:57:10 +0800 | fix | 发现 app compose 未覆盖 RabbitMQ 地址；在三个后端服务补充 `SPRING_RABBITMQ_*` 和 `SPRING_PROFILES_ACTIVE=dev` | 避免容器在 dev profile 下连接 `127.0.0.1` |
| 2026-05-11 20:10:00 +0800 | fix | 运行后发现静态页仍硬编码 `http://licodetech.top` 作为业务 API 基址；已改为当前 Nginx 同源入口 | 修复本机 `localhost:9001` 页面绕到远程部署环境的问题 |
| 2026-05-11 19:55:51 +0800 | fix | 用户反馈扫码登录可用但拼团信息仍不可访问；实测拼团接口经 `9001` 返回 `code=0000`，Nginx 日志未见浏览器发出 `/api/v1/gbm/...` | 增加本地 Nginx no-cache 响应头并 reload，避免旧 HTML 缓存继续生效 |
| 2026-05-11 20:04:30 +0800 | fix | 用户反馈支付宝支付回跳后页面卡顿；确认 Docker 运行态 `return_url` 走 `licodetech.top:9001`，会让浏览器页面回跳绕 FRP | 改为 `ALIPAY_RETURN_URL=http://localhost:9001/`，保留 `notify_url` 走 `licodetech.top:8080` |
| 2026-05-11 20:30:00 +0800 | archive | 归档本地 Docker/FRP/Nginx 联调变更 | `payment-callback-realtime` 属于后续业务能力，保持 propose，不并入本 change 完成范围 |

## 技术决策

| 决策 | 选择 | 放弃的方案 | 原因 |
| --- | --- | --- | --- |
| 工作流 | 先 `/propose`，确认后再 `/apply` | 直接修改 compose | 涉及支付宝/微信回调入口、端口暴露和敏感配置，需先确认风险点 |
| Nginx 路由方向 | 倾向服务名 + 容器内端口 | `172.17.0.1` + 宿主机端口 | 满足同一项目网络内隔离，降低对宿主机端口和 Docker 默认网桥的依赖 |
| FRP 入口 | `8080` 映射到 `s-pay-mall-app:8080`，`9001` 映射到 Nginx `80` | 两个端口都映射到 Nginx `80` | `8080` 不能同时绑定给 Nginx 和商城应用，用户明确要求商城应用使用 `8080:8080` |
| 后端端口 | 保留宿主机暴露用于调试 | 仅栈内 `expose` | 用户明确需要宿主机直接调试后端 |
| 商城应用端口 | `s-pay-mall-app` 使用 `8080:8080` | `9091:8080` | 用户明确要求 |
| 栈与网络命名 | Compose project name 使用 `ai-service-commerce-platform`，网络名使用 `ai-service-commerce-platform-network` | 默认目录名、`my-network`、`software_my-network` | 用户指定命名基础，归属明确，避免误接入其他项目网络 |
| 静态页 API 基址 | `sPayMallUrl` / `groupBuyMarketUrl` 使用空字符串同源基址 | 继续写死 `http://licodetech.top` 或改成固定 `http://localhost:9001` | 同一份 HTML 同时适配本机和 FRP 域名，浏览器请求统一进入当前 Nginx |
| `application-dev.yml` | 暂不修改 | 改成 Docker 服务名或 FRP 公网地址 | Docker 容器运行态已有 compose 环境变量覆盖；直接修改 dev profile 会影响 IDE/宿主机本地启动 |
| 本地静态缓存 | Nginx `location /` 添加 no-cache 响应头 | 依赖用户手动清缓存或强制刷新 | 运行态排查发现 Nginx 未收到浏览器的拼团接口请求，需降低旧 HTML 缓存干扰 |
| 支付宝跳转地址 | `notify_url` 保持 FRP 公网地址，`return_url` 改为本机 Nginx | 两者都走 FRP 公网地址 | 支付宝服务器通知需要公网可达；用户浏览器同步跳转可直接回本机，减少链路和延迟 |

## DDD 边界记录

| 变更点 | 项目 | 模块 | 是否越界 | 说明 |
| --- | --- | --- | --- | --- |
| Compose 网络与端口 | cross-project | docs-dev-ops | 否 | 部署配置层变更，不进入领域层 |
| Nginx upstream | cross-project | docs-dev-ops | 否 | 只改变本地路由目标，不改变 REST 契约 |
| 支付宝/微信回调可达性 | s-pay-mall-ddd | trigger | 否 | 只引用既有入口，不修改 Controller |
| 静态页 API 调用基址 | cross-project | docs-dev-ops/html | 否 | 只调整浏览器请求入口，不修改 REST 契约 |

## 风险记录

| 风险类型 | 位置 | 风险描述 | 处理方式 |
| --- | --- | --- | --- |
| 外部接口 | `docs/dev-ops/nginx/conf/conf.d/localhost.conf` | FRP 8080/9001 与服务端口映射错误会导致支付宝/微信回调不可达 | `8080` 指向商城应用，`9001` 指向 Nginx |
| 安全 | `docs/dev-ops/docker-compose-app-v1.0.yml`、应用配置 | 存在微信、支付宝、数据库等测试凭据或密钥类配置 | 不新增真实密钥；后续可单独改为环境变量占位 |
| 网络 | 两份 compose 文件 | 应用文件依赖 `software_my-network`，环境文件创建 `my-network`，服务名解析可能不一致 | 已统一为项目专用命名网络 |
| 前端请求绕远程 | `docs/dev-ops/nginx/html/*.html` | 访问 `localhost:9001` 时，页面脚本仍将业务 API 请求发往 `http://licodetech.top`，导致微信二维码和拼团接口不走本机 Docker 栈 | 改为同源相对入口，由当前 Nginx 代理 `/api/v1/...` |
| 静态页缓存 | 浏览器 / Nginx 静态入口 | HTML 修改后浏览器可能继续使用旧页面，导致新版同源请求逻辑没有执行 | 为本地静态入口添加 `Cache-Control: no-store` 并 reload Nginx |
| 支付回跳卡顿 | `ALIPAY_RETURN_URL=http://licodetech.top:9001` | 用户支付完成后浏览器先回公网域名，再经 FRP 回到本机 Nginx，页面静态资源和接口也可能绕远端链路 | 本地 Docker compose 将 `return_url` 改为 `http://localhost:9001/` |

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
| --- | --- | --- | --- |
| FRP 端口分配 | 初始建议 `8080`、`9001` 都进入同一个 Nginx | `s-pay-mall-app` 必须使用 `8080:8080`，会与 Nginx `8080:80` 冲突 | 按用户确认优先级调整为 `8080` 指向商城应用、`9001` 指向 Nginx |
| 静态页访问入口 | Nginx 已代理 `/api/v1/...` | HTML 脚本仍使用远程绝对 URL，浏览器没有进入当前 Nginx 代理 | 改为 `"" + "/api/v1/..."` 的同源请求 |
| 拼团不可访问二次反馈 | 用户认为拼团后端仍不可访问 | `curl --noproxy '*'` 直连 `8091` 和经 `9001` 都返回 `code=0000`；Nginx 访问日志未见浏览器发出 `/api/v1/gbm/...` | 判断后端链路可用，补充 no-cache 配置解决旧页面缓存风险 |
| 支付宝同步/异步回调混用 | 本地 compose 初始让 `notify_url` 和 `return_url` 都走公网域名 | `return_url` 是用户浏览器跳转，不需要支付宝服务器访问；本机浏览器访问公网域名会绕 FRP | 拆分为 `notify_url=licodetech.top:8080`、`return_url=localhost:9001` |

## 验证证据

| 命令/方式 | 结果 | 备注 |
| --- | --- | --- |
| `git status --short --branch` | 发现目标 compose 与 Nginx 文件已有未提交改动 | 已在 spec 中记录，避免覆盖用户改动 |
| 静态读取 Docker/Nginx/配置文件 | 发现网络名、upstream、端口暴露与目标存在冲突 | 作为 `/propose` Research 证据 |
| 用户确认 | 明确 FRP 端口、后端调试端口、`s-pay-mall-app` 端口策略 | 作为后续 `/apply` 实现依据 |
| `docker compose -f docs/dev-ops/docker-compose-environment.yml config` | 通过 | 基础设施 compose 静态配置有效 |
| `docker compose -f docs/dev-ops/docker-compose-app-v1.0.yml config` | 通过 | 应用 compose 静态配置有效 |
| `docker compose -f docs/dev-ops/docker-compose-app-v1.0.yml config --quiet` | 通过 | 补充 RabbitMQ 环境变量后重新校验 |
| `docker compose -f docs/dev-ops/docker-compose-app-v1.0.yml config \| rg -n "SPRING_PROFILES_ACTIVE\|SPRING_RABBITMQ"` | 通过 | 三个后端服务均包含 `SPRING_PROFILES_ACTIVE=dev` 与 `SPRING_RABBITMQ_*` |
| `rg` 检查旧网络和旧 upstream | 未发现 `software_my-network`、`my-network`、`172.17.0.1` 残留 | `8080:8080` 为预期保留的商城后端调试端口 |
| `rg -n "licodetech\\.top\|sPayMallUrl\|groupBuyMarketUrl\|fetch\\(" docs/dev-ops/nginx/html/index.html docs/dev-ops/nginx/html/login.html docs/dev-ops/nginx/html/order-list.html` | 通过 | 三个业务 HTML 中的业务 API 基址已变为同源空字符串；`licodetech.top` 不再作为前端请求基址残留 |
| `docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'` | 通过 | Nginx、商城、两个拼团后端、MySQL、Redis、RabbitMQ 均在运行，端口映射存在 |
| `curl --noproxy '*' -i -X POST http://127.0.0.1:8091/api/v1/gbm/index/query_group_buy_market_config ...` | 通过 | 直连拼团后端返回 `{"code":"0000","info":"成功",...}` |
| `curl --noproxy '*' -i -X POST http://127.0.0.1:9001/api/v1/gbm/index/query_group_buy_market_config ...` | 通过 | 经 Nginx 代理返回 `{"code":"0000","info":"成功",...}` |
| `docker exec ai-service-commerce-platform-nginx nginx -t` | 通过 | Nginx 配置语法有效 |
| `docker exec ai-service-commerce-platform-nginx nginx -s reload` | 通过 | 已将 no-cache 配置加载到运行中容器 |
| `curl --noproxy '*' -I http://127.0.0.1:9001/` | 通过 | 首页响应包含 `Cache-Control: no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0` |
| `docker compose -f docs/dev-ops/docker-compose-app-v1.0.yml config --quiet` | 通过 | 支付宝 return_url 调整后 compose 仍有效 |
| `docker compose -f docs/dev-ops/docker-compose-app-v1.0.yml config \| rg -n "ALIPAY_(NOTIFY\|RETURN)_URL"` | 通过 | 输出确认 `ALIPAY_NOTIFY_URL=http://licodetech.top:8080/...`、`ALIPAY_RETURN_URL=http://localhost:9001/` |
| `docker compose -f docs/dev-ops/docker-compose-app-v1.0.yml up -d s-pay-mall-app` | 通过 | 已重建商城后端容器，使新的 `ALIPAY_RETURN_URL` 生效 |
| `docker exec ai-service-commerce-platform-s-pay-mall-app printenv ALIPAY_NOTIFY_URL ALIPAY_RETURN_URL` | 通过 | 运行态确认 notify 走 FRP、return 回本机 |
| `curl --noproxy '*' -i -X POST http://127.0.0.1:8080/api/v1/alipay/create_pay_order ...` | 通过 | 新支付表单中 `return_url=http%3A%2F%2Flocalhost%3A9001%2F`，`notify_url=http%3A%2F%2Flicodetech.top%3A8080%2Fapi%2Fv1%2Falipay%2Falipay_notify_url` |

## 踩坑记录

| 问题 | 原因 | 解决方案 | 是否沉淀 |
| --- | --- | --- | --- |
| 两份 compose 文件网络不一致 | 应用 compose 使用外部 `software_my-network`，环境 compose 创建 `my-network` | 统一项目专用网络，并明确启动顺序 | 否 |
| Nginx 通过宿主机端口访问后端 | upstream 写死 `172.17.0.1` | 改为服务名 + 容器内端口 | 否 |
| 宿主机 `8080` 端口冲突 | Nginx `8080:80` 与商城应用 `8080:8080` 不能同时存在 | `8080` 保留给商城应用，Nginx 使用 `9001:80` | 否 |
| 本机页面请求远程域名 | HTML 中 `sPayMallUrl`、`groupBuyMarketUrl` 写死 `http://licodetech.top` | 改为同源相对地址，统一走当前页面所在 Nginx | 否 |
| 宿主机 curl 返回 502 | 首次 curl 未加 `--noproxy '*'`，命中了本机代理环境 | 使用 `curl --noproxy '*'` 后直连 `8091` 和 `9001` 均正常 | 否 |
| 支付完成后页面卡顿 | 同步跳转 `return_url` 走 FRP，页面返回路径过长且受公网链路影响 | 本地联调将 `return_url` 指向 `localhost:9001` | 否 |

## 知识发现

- [x] 根工作区 `docs/dev-ops/` 已聚合双项目本地部署所需的前端、后端、MySQL、Redis、RabbitMQ 与 Nginx 配置。
- [x] `s-pay-mall-ddd` 的支付宝回调路径是 `/api/v1/alipay/alipay_notify_url`，微信入口路径是 `/api/v1/weixin/portal/receive`。
- [x] 当前 compose 中 `s-pay-mall-app` 通过 `SERVER_PORT=8080` 覆盖容器内服务端口，不能只按 `application-prod.yml` 的 `9091` 推断 Nginx upstream。
- [x] 两份 compose 使用 `name: ai-service-commerce-platform`，共享网络名为 `ai-service-commerce-platform-network`；应用 compose 将该网络声明为 external，需要先启动环境 compose 创建网络。
- [x] 当后端镜像使用 `dev` profile 时，compose 必须覆盖 RabbitMQ 地址为 `rabbitmq`，否则容器内 `127.0.0.1` 会指向应用容器自身。
- [x] 前端静态页如果由本地 Nginx 提供，应优先使用同源 `/api/v1/...` 请求，让 Nginx 统一代理到商城和拼团后端；不要在 HTML 中写死远程部署域名。
- [x] 本地联调排查 HTTP 时，如宿主机配置了代理，`curl localhost` 也可能得到代理层 502；验证 Docker 端口建议使用 `curl --noproxy '*'`。
- [x] 支付宝本地联调时，`notify_url` 和 `return_url` 不必相同：`notify_url` 面向支付宝服务器，必须公网可达；`return_url` 面向用户浏览器，本机测试可直接指向 `localhost:9001`。

## 归档结论

- 本 change 已完成本地部署与联调链路整理。
- 已验证运行态核心路径：前端、微信登录、拼团配置接口、支付宝支付表单回跳配置。
- 未将知识发现写入 `code_copilot/knowledge/`；如需沉淀为长期知识，应在用户确认后单独移动。
