# 变更日志 — Nginx 静态前端响应式改版方案

> 记录决策、偏差、验证证据和可沉淀知识。

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
| --- | --- | --- | --- |
| 2026-05-14 | Propose | 读取根级 `code_copilot` 规则、知识索引、当前 Git 状态 | 发现工作区已有多处未提交改动，本轮只新增 proposal。 |
| 2026-05-14 | Research | 检查 `docs/dev-ops/nginx/html/` 静态页面和图片资源 | 当前已有 `index.html`、`login.html`、`order-list.html`、`css/index.css`、`js/index.js` 和 `sku-13811216-*.png`。 |
| 2026-05-14 | Research | 检查 Nginx 代理配置 | `/api/v1/gbm/`、`/api/v1/alipay/`、`/api/v1/login/` 已有同源代理。 |
| 2026-05-14 | Research | 检查后端接口与 DTO | 当前只确认登录、商品/拼团配置、创建支付单、订单列表、退单能力。 |
| 2026-05-14 | Propose | 新增 `frontend-responsive-ai-service-pages` proposal | 未修改前端页面。 |
| 2026-05-14 | Clarification | 用户确认品牌名、首页布局和顶部导航范围 | 品牌名使用“智链云汇”；允许首页混合响应式重构；桌面端不保留无支撑的“解决方案/帮助中心”等入口。 |
| 2026-05-14 | Apply | 用户确认执行 `frontend-responsive-ai-service-pages` 修改任务 | 开始修改 `docs/dev-ops/nginx/html/` 静态页面。 |
| 2026-05-14 | Apply | 完成首页、登录页、订单页响应式改版 | 未修改后端接口、数据库、MQ、订单状态或退款规则。 |

## 技术决策

| 决策 | 选择 | 放弃的方案 | 原因 |
| --- | --- | --- | --- |
| 方案落盘目录 | `code_copilot/changes/frontend-responsive-ai-service-pages` | `./copilot/changes` | 当前仓库不存在 `./copilot/changes`，根级规范目录为 `code_copilot/changes`。 |
| 品牌名 | 智链云汇 | AI 服务订阅平台 | 用户确认采用参考图品牌。 |
| 首页布局 | 桌面端产品购买页 + 移动端商品卡片页 | 保持商品详情页 | 用户确认允许重构 `index.html` 信息架构。 |
| 桌面端导航 | 不保留“解决方案/帮助中心”等无支撑入口 | 保留静态入口或置灰 | 当前项目没有对应页面，用户确认不保留。 |
| 改版范围 | 只规划现有静态页面改版 | 新增企业控制台、采购审批、发票、用量统计等页面 | 当前后端无对应接口与数据模型支撑。 |
| 商品展示 | 继续复用现有商品图 | 新增图片资产 | 用户明确要求商品图片暂用之前图片。 |
| 前端架构 | 原生 HTML/CSS/JS 响应式增强 | 引入前端构建工具 | 当前 Nginx 页面无构建链，保持最小技术栈。 |

## DDD 边界记录

| 变更点 | 项目 | 模块 | 是否越界 | 说明 |
| --- | --- | --- | --- | --- |
| 首页 UI 改版 | root docs | static frontend | 否 | 只消费现有接口，不改领域逻辑。 |
| 拼团展示 | group-buy-market consumer | trigger/api consumer | 否 | 只使用 `GoodsMarketResponseDTO` 已有字段。 |
| 支付入口 | s-pay-mall-ddd consumer | trigger/api consumer | 否 | 只使用 `CreatePayRequestDTO` 已有字段。 |
| 订单页 | s-pay-mall-ddd consumer | trigger/api consumer | 否 | 只使用 `UserOrderItemDTO` 和退单接口现有响应。 |
| 倒计时与轮播脚本 | root docs | static frontend | 否 | 仅增强空元素与异常倒计时值兼容。 |

## 风险记录

| 风险类型 | 位置 | 风险描述 | 处理方式 |
| --- | --- | --- | --- |
| 资金 | `index.html` 支付入口 | 前端金额文案若与请求分支不一致，会误导用户 | 后续 apply 必须分别校验 `originalPrice` 与 `payPrice` 展示。 |
| 退款/状态 | `order-list.html` 退单入口 | UI 调整不能改变退单条件和状态更新 | 继续以 `canRefund` 控制按钮展示，保持请求体不变。 |
| 外部接口 | 静态页面 fetch | 改版可能破坏现有接口调用路径或请求字段 | 不改 API path，不改 DTO 字段。 |
| 安全 | 登录与支付弹窗 | 不得新增真实账号、token、密钥或生产敏感值 | 后续 apply 中复核硬编码文案。 |

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
| --- | --- | --- | --- |
| 本地完整页面验证 | 预期通过 Nginx 9001 与后端接口联调 | 当前 9001 未启动，且 WSL 内没有 `docker` 命令 | 记录为环境阻塞；已完成静态语法与契约 grep 检查。 |

## 验证证据

| 命令/方式 | 结果 | 备注 |
| --- | --- | --- |
| `git status --short --branch` | 已执行 | 发现已有未提交改动，包括 `docs/dev-ops/nginx/html/*`。 |
| `rg --files docs/dev-ops/nginx` | 已执行 | 确认静态页面、图片、CSS、JS 和 Nginx 配置文件。 |
| `rg -n "create_pay_order|query_user_order_list|refund_order|query_group_buy_market_config|weixin_qrcode_ticket_scene|check_login_scene" .` | 已执行 | 确认页面消费接口与后端 Controller/DTO 位置。 |
| `node --check docs/dev-ops/nginx/html/js/index.js` | 通过 | 通用脚本语法检查通过。 |
| `node -e "... new Function(inline scripts) ..."` | 通过 | `index.html`、`login.html`、`order-list.html` 内联脚本均可解析。 |
| `rg -n "api/v1|marketType|servicePackageId|teamId|activityId|解决方案|帮助中心|智链云汇|hovdlm|111111" docs/dev-ops/nginx/html` | 通过 | 确认接口路径/请求字段仍在，品牌名已替换；无 `解决方案/帮助中心` 导航和旧沙箱账号密码文案。 |
| `git diff --check -- docs/dev-ops/nginx/html ... code_copilot/changes/frontend-responsive-ai-service-pages` | 通过 | 未发现空白错误；输出仅包含 LF/CRLF 换行提示。 |
| `curl --noproxy '*' -I http://127.0.0.1:9001/` | 失败 | 连接被拒绝，说明本机 Nginx 入口未启动。 |
| `docker compose -f docs/dev-ops/docker-compose-app-v1.1.yml ps` | 失败 | 当前 WSL 2 distro 未安装或未接入 Docker，无法启动完整 compose 栈验证。 |

## 踩坑记录

| 问题 | 原因 | 解决方案 | 是否沉淀 |
| --- | --- | --- | --- |
| 用户要求写入 `./copilot/changes`，但仓库实际是 `code_copilot/changes` | 当前工作区规范目录名称与用户口述路径不一致 | 按仓库权威目录 `code_copilot/changes` 落盘，并在 spec 中说明 | 否 |
| 参考图包含大量后台/企业采购功能 | 当前项目没有对应接口和数据模型 | 在 spec 中明确排除，不做无后端支撑页面 | 否 |

## 知识发现

- [x] 当前静态前端是 Nginx 托管的原生 HTML/CSS/JS 页面，不是独立前端工程。
- [x] 当前首页只有单个 `goodsId=9890001` 商品配置来源，不应硬编码多个服务套餐。
- [x] 当前根工作区已有未提交前端改动，后续 apply 前必须再次检查 diff，避免覆盖用户修改。
- [x] 该改版保持所有后端接口不变，只调整静态页面结构、样式、交互状态与兼容性。
