# 变更日志 — 云服务器域名 Docker 部署配置调整

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
| --- | --- | --- | --- |
| 2026-05-15 | propose | 读取根级 `code_copilot` 规则、两个子项目规则与 `s-pay-mall-ddd/code_copilot` | 确认本次为跨项目配置部署变更 |
| 2026-05-15 | research | 检查 `docs/tag/v1`、两个 `application-prod.yml` 与配置使用点 | 发现 Docker compose profile 与 prod 配置存在运行态分歧 |
| 2026-05-15 | propose | 新建 proposal 文档 | 等待用户确认后再进入 `/apply` |
| 2026-05-15 | confirm | 用户确认域名与 profile 策略 | 域名为 `licodetech.top`，首期入口使用 `http://licodetech.top`，Docker 切到 `prod` |
| 2026-05-15 | confirm | 用户确认端口与内网访问策略 | Nginx 暴露 `80:80`；云服务器 frps 已监听 `8080` 和 `9001`，应用需避开；MySQL/Redis/RabbitMQ 管理端仅服务器内网访问 |
| 2026-05-15 | apply | 修改 `docs/tag/v1` 与两个 `application-prod.yml` | Nginx 改 `80:80`，商城 app 改为 Docker 网络内暴露，应用 profile 改 `prod`，中间件端口绑定 `127.0.0.1` |
| 2026-05-15 | verify | 执行 Maven 构建与配置检查 | 两个 app reactor 构建成功；Docker CLI 缺失导致 compose config 未能执行 |
| 2026-05-15 | fix | 修正微信/支付宝敏感配置默认值 | 去掉 `please-change` 假占位，改为运行时必填环境变量，并新增不含真实值的 `.env.example` |
| 2026-05-15 | fix | 同步 Docker 镜像版本 | `build.sh` 已构建 `1.2` 镜像，compose 应用镜像同步为 `1.2`，避免云服务器启动旧镜像 |

## 技术决策

| 决策 | 选择 | 放弃的方案 | 原因 |
| --- | --- | --- | --- |
| proposal 范围 | 只写 `code_copilot/changes/cloud-domain-docker-deploy/` | 直接改部署配置 | 用户使用 `propose`，且域名、端口、profile 策略仍需确认 |
| 入口模型 | Nginx 统一公网入口 | 后端端口直接对外 | 当前静态页使用同源相对 API，Nginx 已有路由配置 |
| Nginx 端口 | `80:80` | `9001:80` | 用户要求直接输入域名访问，不追加端口 |
| 访问协议 | 首期使用 `http://licodetech.top` | 立即配置 HTTPS | 先满足浏览器和手机直接输入域名访问；HTTPS 后续需要证书和 Nginx TLS 配置 |
| 配置策略 | `prod + env override` | 继续 `dev + env override` | 用户已确认 Docker 切到 `prod`，云服务器语义也更接近 prod |
| 应用端口 | 应用不占用宿主机 `8080` 和 `9001` | 保留 `8080:8080` | 云服务器已有 frps 监听这两个端口，应用通过 Docker 网络由 Nginx 代理即可 |
| 中间件访问 | 仅服务器内网访问 | 对公网开放管理端口 | 降低数据库、缓存、消息队列暴露面 |

## DDD 边界记录

| 变更点 | 项目 | 模块 | 是否越界 | 说明 |
| --- | --- | --- | --- | --- |
| Docker/Nginx 参数 | root docs | `docs/tag/v1` | 否 | 部署配置，不进入领域层 |
| 中间件连接参数 | group-buy-market | app | 否 | 只改 profile 配置 |
| 公网回调与支付 URL | s-pay-mall-ddd | app | 否 | 配置影响支付入口，但不改支付业务代码 |

## 风险记录

| 风险类型 | 位置 | 风险描述 | 处理方式 |
| --- | --- | --- | --- |
| 外部接口 | `ALIPAY_NOTIFY_URL` / `alipay.notify_url` | 回调 URL 与 Nginx 对外端口不一致会导致支付宝通知不可达 | 后续统一为 `http://licodetech.top/api/v1/alipay/alipay_notify_url` |
| 外部接口 | `ALIPAY_RETURN_URL` | 仍指向 `localhost:9001`，云服务器用户回跳错误 | 改为公网域名根路径 |
| 配置 | `SPRING_PROFILES_ACTIVE=dev` | 修改 `application-prod.yml` 可能不影响 Docker 运行态 | 用户已确认切换到 `prod` |
| 安全 | `application-prod.yml` 与 compose 环境变量 | 存在固定账号密码和支付/微信密钥 | 支付/微信密钥使用运行时必填环境变量，不提供假默认值 |
| 安全 | `docker-compose-environment.yml` | MySQL、Redis、RabbitMQ 端口暴露到宿主机 | 仅允许服务器内网访问，不对公网开放 |
| 端口冲突 | `docs/tag/v1/docker-compose-app-v1.1.yml` | `s-pay-mall-app` 当前占用宿主机 8080，Nginx 当前占用宿主机 9001；云服务器 frps 已监听这两个端口 | Nginx 改为 `80:80`，应用后端改为仅 Docker 网络访问或避开已占用端口 |

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
| --- | --- | --- | --- |
| 无 | - | - | - |
| Docker 运行态验证 | 预期执行 `docker compose config` 与 Nginx 检查 | 当前 WSL 环境无 `docker` 命令 | 记录为验证阻塞，需在云服务器或 Docker 可用环境补跑 |

## 验证证据

| 命令/方式 | 结果 | 备注 |
| --- | --- | --- |
| `git status --short --branch` | 当前分支 `tag-v1`，已有 4 个未提交改动 | 未触碰这些业务配置文件 |
| `rg --files docs/tag/v1 ...` | 找到 `docker-compose-app-v1.1.yml`、`docker-compose-environment.yml`、Nginx 与静态页 | 用于确定部署配置范围 |
| `rg -n "localhost|127.0.0.1|9001|8080|8091|alipay|domain|server_name|proxy_pass|mysql|redis|rabbit" ...` | 找到云部署相关参数 | 作为 proposal research 输入 |
| `nl -ba ...application-prod.yml` | 已定位两个 prod 配置中的中间件、回调和密钥配置 | 未执行构建或启动 |
| `docker compose -f docs/tag/v1/docker-compose-environment.yml config` | 未执行成功 | 当前 WSL 环境提示 `docker` 命令不可用 |
| `docker compose -f docs/tag/v1/docker-compose-app-v1.1.yml config` | 未执行成功 | 当前 WSL 环境提示 `docker` 命令不可用 |
| `mvn -pl group-buy-market-lpc-app -am -DskipTests package` | 通过 | 测试按命令跳过；构建成功 |
| `mvn -pl s-pay-mall-ddd-lpc-app -am -DskipTests package` | 通过 | 测试按命令跳过；构建成功 |
| `git diff --check -- ...` | 通过 | 仅有 Git 行尾 CRLF 提示，无 whitespace error |
| `rg -n "please-change|..." ...` | 通过 | 已清理 `please-change` 假默认值 |

## 踩坑记录

| 问题 | 原因 | 解决方案 | 是否沉淀 |
| --- | --- | --- | --- |
| Docker compose 设置 `dev`，但用户点名检查 `prod` | 当前部署策略混合了本机联调和生产配置 | `/apply` 前先确认 profile 策略 | 否 |
| 当前环境无法运行 Docker 验证 | WSL 中没有 Docker CLI 或 Docker Desktop WSL integration 未启用 | 在云服务器或 Docker 可用环境补跑 `docker compose config`、`nginx -t` 与 HTTP 冒烟 | 否 |
| 微信/支付宝配置不能使用假默认值 | 使用 `please-change` 会让服务启动后在真实对接时失败 | 改为 `${VAR:?message}` 和 Spring 必填占位符，部署时通过 `docs/tag/v1/.env` 提供真实值 | 否 |

## 知识发现

- [ ] TODO: 云服务器部署完成后，可将最终域名入口、profile 策略和端口暴露策略沉淀到 `code_copilot/knowledge/` 的 Docker/配置主题。
