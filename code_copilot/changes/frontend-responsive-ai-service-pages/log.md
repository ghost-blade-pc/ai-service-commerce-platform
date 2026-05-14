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
| 2026-05-14 | Propose Update | 用户反馈桌面端产品购买页设计仍不够好，要求按新的设计思路补充 proposal | 本轮只更新 `code_copilot/changes/frontend-responsive-ai-service-pages` 文档，不修改前端源码。 |
| 2026-05-14 | Apply Update | 用户确认执行 Task 7-10 | 将 `index.html` 升级为桌面端购买工作台 + 移动端底部结算条。 |
| 2026-05-14 | Archive | 归纳整理 `frontend-responsive-ai-service-pages` 文档 | 标记 change 为 done，补充归档摘要、完成范围、验证边界和后续拆分建议。 |
| 2026-05-14 | Archive Knowledge | 用户要求沉淀全局知识 | 新增 `code_copilot/knowledge/static-frontend-ai-service-pages.md`，并接入 `knowledge/index.md`。 |

## 归档摘要

- **最终状态**：done。
- **最终实现**：`index.html` 采用桌面端购买工作台和移动端底部结算条；`login.html` 与 `order-list.html` 保持第一轮响应式品牌统一成果。
- **最终代码范围**：`docs/dev-ops/nginx/html/index.html`、`login.html`、`order-list.html`、`css/index.css`、`js/index.js`。
- **最终接口范围**：只消费现有 `/api/v1/gbm/index/query_group_buy_market_config`、`/api/v1/alipay/create_pay_order`、订单查询/退单、登录接口；未新增或修改后端契约。
- **风险处理**：金额、拼团、订单、退款相关规则不在前端重新定义；前端只展示后端返回字段并保持请求参数不变。
- **验证边界**：静态语法、接口字段、无支撑入口/敏感文案、Nginx 入口、Docker compose 状态、营销配置接口均已验证；自动截图未执行，原因是本地 Node 环境缺少 Playwright。

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
| 二次设计方向 | 桌面端升级为“智链云汇 AI 服务购买工作台” | 继续堆叠商品卡片或传统详情页 | 更符合 AI SaaS 订阅购买路径，强化套餐配置、拼团进度和订单结算确定性。 |
| 桌面端布局 | 左侧价值说明 + 中间套餐配置器 + 右侧 sticky 结算栏 | 当前商品详情式双栏/卡片组合 | 让用户首屏完成“买什么、怎么买、省多少、下一步做什么”的判断。 |
| 移动端布局 | 商品卡片 + 拼团状态 + 底部结算条 | 桌面三栏缩放到移动端 | 移动端购买路径更短，避免压缩后信息拥挤。 |
| 购买模式联动 | 用前端状态维护 `group` / `alone` 并联动 CTA 与结算金额 | 保留两个独立按钮各自散落提交 | 让桌面端结算栏和移动端底部条使用同一购买意图，降低文案不一致风险。 |

## DDD 边界记录

| 变更点 | 项目 | 模块 | 是否越界 | 说明 |
| --- | --- | --- | --- | --- |
| 首页 UI 改版 | root docs | static frontend | 否 | 只消费现有接口，不改领域逻辑。 |
| 拼团展示 | group-buy-market consumer | trigger/api consumer | 否 | 只使用 `GoodsMarketResponseDTO` 已有字段。 |
| 支付入口 | s-pay-mall-ddd consumer | trigger/api consumer | 否 | 只使用 `CreatePayRequestDTO` 已有字段。 |
| 订单页 | s-pay-mall-ddd consumer | trigger/api consumer | 否 | 只使用 `UserOrderItemDTO` 和退单接口现有响应。 |
| 倒计时与轮播脚本 | root docs | static frontend | 否 | 仅增强空元素与异常倒计时值兼容。 |
| 二次设计 proposal | root docs | static frontend docs | 否 | 只追加方案和待执行任务，未改静态源码。 |
| 二次设计 apply | root docs | static frontend | 否 | 只改 `index.html` 和 `css/index.css`，不改后端领域、接口、数据库、MQ。 |

## 风险记录

| 风险类型 | 位置 | 风险描述 | 处理方式 |
| --- | --- | --- | --- |
| 资金 | `index.html` 支付入口 | 前端金额文案若与请求分支不一致，会误导用户 | 后续 apply 必须分别校验 `originalPrice` 与 `payPrice` 展示。 |
| 退款/状态 | `order-list.html` 退单入口 | UI 调整不能改变退单条件和状态更新 | 继续以 `canRefund` 控制按钮展示，保持请求体不变。 |
| 外部接口 | 静态页面 fetch | 改版可能破坏现有接口调用路径或请求字段 | 不改 API path，不改 DTO 字段。 |
| 安全 | 登录与支付弹窗 | 不得新增真实账号、token、密钥或生产敏感值 | 后续 apply 中复核硬编码文案。 |
| 响应式 | `index.html` 桌面/移动端 | 桌面购买工作台若直接缩放到移动端，会造成按钮、金额和倒计时溢出 | 方案明确桌面三栏与移动端商品卡片分离设计。 |

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
| --- | --- | --- | --- |
| 第一轮本地完整页面验证 | 预期通过 Nginx 9001 与后端接口联调 | 第一轮验证时 9001 未启动，且 WSL 内没有 `docker` 命令 | 已作为历史环境阻塞记录；二次设计 apply 时 Nginx/Docker 状态已重新验证。 |
| 二次设计自动截图验证 | 预期可做桌面/移动端自动截图 | 本地 Node 环境未安装 Playwright 包 | 已完成 Nginx/API 冒烟和静态检查，自动截图留作后续补充。 |

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
| `rg -n "购买工作台\|桌面端\|移动端\|智链云汇" code_copilot/changes/frontend-responsive-ai-service-pages` | 已执行 | 二次设计 proposal 已落到 `spec.md`、`tasks.md`、`log.md`。 |
| `node --check docs/dev-ops/nginx/html/js/index.js` | 通过 | 二次设计 apply 后通用脚本语法检查通过。 |
| `node -e "... new Function(index inline scripts) ..."` | 通过 | `index.html` 内联脚本可解析。 |
| `rg -n "api/v1\|marketType\|servicePackageId\|teamId\|activityId\|purchase-workbench\|mobile-settlement" docs/dev-ops/nginx/html/index.html` | 通过 | 接口字段、购买工作台和移动端底部结算条落点存在。 |
| `rg -n "解决方案\|帮助中心\|hovdlm\|111111\|企业控制台\|用量统计\|发票\|消息中心" docs/dev-ops/nginx/html/index.html docs/dev-ops/nginx/html/css/index.css` | 通过 | 无匹配，说明无支撑入口和旧敏感文案未回归。 |
| `curl --noproxy '*' -I http://127.0.0.1:9001/` | 通过 | Nginx 返回 200，首页静态资源入口可访问。 |
| `docker compose -f docs/dev-ops/docker-compose-app-v1.1.yml ps` | 通过 | Nginx、商城、营销、MySQL、Redis、RabbitMQ 容器处于运行状态。 |
| `POST /api/v1/gbm/index/query_group_buy_market_config` | 通过 | 经 Nginx 代理返回 `code=0000`，包含 `goods`、空 `teamList` 和 `teamStatistic`。 |
| `node -e "try { require('playwright') ... }"` | 未通过 | Playwright 包不可用，未做自动截图验证。 |
| `git diff --check -- ...` | 通过 | 未发现空白错误；仅输出 LF/CRLF 换行提示。 |

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
- [x] 二次设计迭代应把桌面端视为购买工作台，而不是传统电商详情页；移动端则继续保持卡片式购买路径。
- [x] Nginx 9001 当前可访问，营销配置接口经代理可返回真实套餐数据；自动截图验证仍受本地 Playwright 包缺失限制。

## 后续建议

- 若要补自动截图验证，建议新建 `frontend-responsive-screenshot-verification` change，并先确认是否允许安装或引入 Playwright。
- 若要新增多套餐、智能体服务包、企业控制台、用量统计、发票或消息中心，必须先确认后端目录/订单/权益/统计接口，不应继续塞入本已完成 change。
- 若后续替换商品图片，只替换 `docs/dev-ops/nginx/html/images/sku-13811216-*.png` 同类静态资源即可，不需要改变本次接口契约。

## 全局知识沉淀

- 已新增：`code_copilot/knowledge/static-frontend-ai-service-pages.md`。
- 已更新：`code_copilot/knowledge/index.md` 的全局知识地图和主题路由。
- 适用后续任务：Nginx 静态前端、首页购买工作台、扫码登录、订单页、拼团购买、支付下单字段核对、静态页面验证。
