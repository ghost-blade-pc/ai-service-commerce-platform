# 云服务器域名 Docker 部署知识

> 适用范围：`ddd-trade-marketing-platform` 根工作区的 `docs/tag/v1` 云服务器部署。
> 当前确认入口：`http://licodetech.top`。

## 1. 部署入口

- 用户访问入口由 `docs/tag/v1/docker-compose-app-v1.1.yml` 中的 Nginx 提供。
- Nginx 宿主机端口映射为 `80:80`，用户浏览器或手机直接访问：

```text
http://licodetech.top
```

- Nginx 配置位于 `docs/tag/v1/nginx/conf/conf.d/localhost.conf`：
  - `/api/v1/gbm/` 代理到 `group-buy-market-01:8091`、`group-buy-market-02:8091`。
  - `/api/v1/alipay/`、`/api/v1/login/`、`/api/v1/weixin/` 代理到 `s-pay-mall-app:8080`。
  - 静态页使用同源相对路径请求接口。

## 2. Docker Profile 与镜像

- 云服务器运行态统一使用 `SPRING_PROFILES_ACTIVE=prod`。
- `group-buy-market/group-buy-market-lpc-app/build.sh` 构建镜像：

```text
lipeicheng/group-buy-market-lpc-app:1.2
```

- `s-pay-mall-ddd/s-pay-mall-ddd-lpc-app/build.sh` 构建镜像：

```text
lipeicheng/s-pay-mall-ddd-lpc-app:1.2
```

- `docs/tag/v1/docker-compose-app-v1.1.yml` 已同步引用 `1.2` 镜像，避免构建镜像与启动镜像不一致。

## 3. 端口策略

### 3.1 对外访问端口

| 服务 | 宿主机端口 | 容器端口 | 说明 |
| --- | --- | --- | --- |
| Nginx | `80` | `80` | 普通用户访问入口 |
| group-buy-market-01 | `8091` | `8091` | 保留宿主机调试入口 |
| group-buy-market-02 | `8092` | `8091` | 保留宿主机调试入口 |
| phpMyAdmin | `8899` | `80` | 用户确认对外开放 |
| Redis Admin | `8081` | `8081` | 用户确认对外开放 |
| RabbitMQ AMQP | `5672` | `5672` | 用户确认对外开放 |
| RabbitMQ Management | `15672` | `15672` | 用户确认对外开放 |

### 3.2 仅服务器本机访问端口

| 服务 | 宿主机端口 | 容器端口 | 说明 |
| --- | --- | --- | --- |
| MySQL | `127.0.0.1:13306` | `3306` | 不直接公网暴露 |
| Redis | `127.0.0.1:16379` | `6379` | 不直接公网暴露 |

云服务器已有 `frps` 监听 `8080` 和 `9001`，因此：

- Nginx 不再使用 `9001:80`。
- `s-pay-mall-app` 不再使用 `8080:8080`，只通过 Docker 网络 `expose: 8080` 给 Nginx 访问。

## 4. 敏感配置

- `docs/tag/v1/.env` 存放真实微信、支付宝、数据库、RabbitMQ 等运行参数。
- `docs/tag/v1/.env` 已在根级 `.gitignore` 中忽略，不能提交。
- `docs/tag/v1/.env.example` 只保留变量名和非敏感默认值，不写真实密钥。
- `docs/tag/v1/docker-compose-app-v1.1.yml` 对微信与支付宝关键变量使用 `${VAR:?message}`，缺少真实值时 `docker compose` 应直接失败，避免服务假启动。

必填变量包括：

```text
WEIXIN_CONFIG_ORIGINALID
WEIXIN_CONFIG_TOKEN
WEIXIN_CONFIG_APP_ID
WEIXIN_CONFIG_APP_SECRET
WEIXIN_CONFIG_TEMPLATE_ID
ALIPAY_APP_ID
ALIPAY_MERCHANT_PRIVATE_KEY
ALIPAY_ALIPAY_PUBLIC_KEY
```

## 5. 回调地址

支付宝异步通知建议配置为：

```text
http://licodetech.top/api/v1/alipay/alipay_notify_url
```

支付宝浏览器回跳为：

```text
http://licodetech.top
```

微信服务器地址建议配置为：

```text
http://licodetech.top/api/v1/weixin/portal/receive
```

这些平台侧配置无法从仓库内验证，上线前必须到支付宝/微信平台后台核对。

## 6. 启动与验证

云服务器 clone 后，先准备真实 `.env`：

```bash
cd docs/tag/v1
cp .env.example .env
```

编辑 `.env` 填入真实值后启动：

```bash
docker compose -f docker-compose-environment.yml up -d
docker compose -f docker-compose-app-v1.1.yml up -d
```

推荐验证：

```bash
docker compose -f docker-compose-environment.yml config
docker compose -f docker-compose-app-v1.1.yml config
docker exec ai-service-commerce-platform-nginx nginx -t
curl --noproxy '*' -I http://licodetech.top/
curl --noproxy '*' -i http://licodetech.top/api/v1/login/weixin_qrcode_ticket_scene?sceneStr=test
```

当前本地 WSL 环境缺少 Docker CLI，因此 Docker Compose 与 Nginx 运行态验证需要在云服务器或 Docker 可用环境补跑。

## 7. 风险提示

- phpMyAdmin、Redis Admin、RabbitMQ 已按用户确认对外暴露，建议在云服务器安全组中限制可信来源 IP，并修改默认密码。
- 使用 HTTP 可以满足首期直接访问，但登录、支付和管理端口长期运行建议升级 HTTPS。
- 支付状态推进依赖支付宝异步通知或主动查单/Job 兜底；回调 URL 错误会导致支付成功状态延迟推进。
