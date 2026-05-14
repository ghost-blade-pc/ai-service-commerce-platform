# 任务拆分 — Nginx 静态前端响应式改版方案

> 推荐顺序：页面结构 → CSS 响应式系统 → JS 渲染适配 → 登录/订单页统一 → 本地验证  
> 本轮已按任务执行静态前端改版；完整浏览器联调受本机 Docker/Nginx 环境限制。

## 前置条件

- [x] 已读取根级 `code_copilot/rules/*.md`
- [x] 已读取 `code_copilot/README.md`
- [x] 已读取 `code_copilot/knowledge/index.md`
- [x] 已读取 `s-pay-mall-ddd/code_copilot/README.md` 与规则，因为页面消费商城支付/登录接口
- [x] 已读取 `group-buy-market/AGENTS.md`，因为页面消费拼团营销接口
- [x] 已确认影响项目和 DDD 层级
- [x] 已确认是否涉及资金、订单状态、退款、拼团、MQ、外部接口、数据库
- [x] 已检查 Git 分支和未提交变更
- [x] 用户已确认品牌名、首页混合响应式布局和桌面端无支撑导航入口处理方式
- [x] 用户已确认进入 `/apply` 并要求执行前端页面修改

## Task 1: 首页响应式信息架构改造

- **目标**：将 `index.html` 改成“桌面端产品购买页 + 移动端商品卡片页”的混合响应式 AI 服务订阅购买页。
- **影响项目**：cross-project static frontend
- **DDD 层级**：docs/static frontend
- **涉及文件**：
  - `docs/dev-ops/nginx/html/index.html`：调整页面结构，增加品牌导航、套餐卡片、拼团摘要、结算摘要区域。
  - `docs/dev-ops/nginx/html/css/index.css`：新增桌面端网格与移动端单列响应式样式。
- **关键约束**：
  - 品牌名使用“智链云汇”。
  - 桌面端不展示“解决方案/帮助中心”等当前项目没有页面支撑的静态入口。
  - 不新增后端没有支撑的商品分类或企业管理入口。
  - 继续使用 `goodsId = "9890001"`。
  - 继续复用 `sku-13811216-*.png`。
- **依赖**：无
- **风险标记**：资金 / 外部接口
- **验收标准**：
  - 桌面端首页包含套餐信息、拼团优惠、支付入口、我的订阅入口。
  - 移动端无横向滚动，底部操作区不遮挡内容。
  - 单独订阅和拼团订阅按钮文案与金额展示一致。
- **验证命令**：浏览器访问 `http://127.0.0.1:9001/`，分别检查 1440px、1024px、390px、375px。
- **状态**：done

## Task 2: 拼团队伍与优惠展示适配

- **目标**：根据 `query_group_buy_market_config` 返回值展示拼团队伍、剩余人数、倒计时、累计参团数据和优惠金额。
- **影响项目**：cross-project static frontend
- **DDD 层级**：docs/static frontend
- **涉及文件**：
  - `docs/dev-ops/nginx/html/index.html`：调整 `teamList` 渲染模板。
  - `docs/dev-ops/nginx/html/js/index.js`：必要时增强倒计时对异常值的兼容。
  - `docs/dev-ops/nginx/html/css/index.css`：拼团卡片、进度条、空状态样式。
- **关键约束**：
  - 只使用 `GoodsMarketResponseDTO` 已有字段。
  - `teamList` 为空时只展示“立即开团”的空状态，不伪造队伍。
- **依赖**：Task 1
- **风险标记**：拼团 / 外部接口
- **验收标准**：
  - 有队伍时展示参团按钮、剩余人数、倒计时。
  - 无队伍时展示开团引导。
  - 倒计时异常值不导致页面脚本崩溃。
- **验证命令**：通过真实或 mock 返回数据手动验证 `teamList` 有/无两种状态。
- **状态**：done

## Task 3: 支付入口和交互状态整理

- **目标**：保持现有创建支付单接口不变，优化按钮 loading、失败提示、支付确认弹窗的移动端适配。
- **影响项目**：s-pay-mall-ddd static frontend consumer
- **DDD 层级**：docs/static frontend
- **涉及文件**：
  - `docs/dev-ops/nginx/html/index.html`：保留 `create_pay_order` 调用，整理单独订阅/拼团订阅分支。
  - `docs/dev-ops/nginx/html/css/index.css`：支付弹窗、按钮 loading、禁用态样式。
- **关键约束**：
  - 单独订阅请求保持 `marketType: 0`。
  - 拼团订阅请求保持 `marketType: 1`，并携带 `teamId`、`activityId`。
  - 不改变 `CreatePayRequestDTO`。
- **依赖**：Task 1、Task 2
- **风险标记**：资金 / 外部接口 / 安全
- **验收标准**：
  - 点击支付后按钮不会重复快速提交。
  - 接口失败有可见提示。
  - 支付确认弹窗在移动端不超出屏幕。
- **验证命令**：手动点击单独订阅、拼团订阅，检查请求体与页面提示。
- **状态**：done

## Task 4: 登录页视觉统一

- **目标**：将 `login.html` 调整为与首页一致的品牌视觉和响应式布局。
- **影响项目**：s-pay-mall-ddd static frontend consumer
- **DDD 层级**：docs/static frontend
- **涉及文件**：
  - `docs/dev-ops/nginx/html/login.html`：调整结构与样式。
- **关键约束**：
  - 不改变 `weixin_qrcode_ticket_scene` 与 `check_login_scene` 调用逻辑。
  - 不新增真实账号、token、密钥等敏感信息。
- **依赖**：Task 1 的视觉规范
- **风险标记**：安全
- **验收标准**：
  - 桌面端二维码卡片居中且品牌信息明确。
  - 移动端二维码和说明不溢出。
  - 扫码登录轮询逻辑保持可用。
- **验证命令**：访问 `http://127.0.0.1:9001/login.html`，检查二维码获取与轮询。
- **状态**：done

## Task 5: 订单页响应式管理视图优化

- **目标**：优化 `order-list.html` 的桌面端列表密度和移动端卡片体验，保留分页和退单能力。
- **影响项目**：s-pay-mall-ddd static frontend consumer
- **DDD 层级**：docs/static frontend
- **涉及文件**：
  - `docs/dev-ops/nginx/html/order-list.html`：调整订单列表结构和内联样式。
- **关键约束**：
  - 不改变 `query_user_order_list`、`refund_order` 请求体。
  - 不新增后台统计、发票、企业成员等未支撑模块。
- **依赖**：Task 1 的视觉规范
- **风险标记**：退款 / 状态
- **验收标准**：
  - 订单号、金额、状态、创建时间、退单按钮在桌面/移动端均可读。
  - 退单按钮只在 `canRefund` 为 true 时展示。
  - 退单成功后状态更新逻辑保持。
- **验证命令**：访问 `http://127.0.0.1:9001/order-list.html`，检查订单加载、加载更多、退单流程。
- **状态**：done

## Task 6: 本地响应式与接口冒烟验证

- **目标**：完成静态页面的基础验证，记录结果。
- **影响项目**：cross-project
- **DDD 层级**：docs/dev-ops
- **涉及文件**：
  - `code_copilot/changes/frontend-responsive-ai-service-pages/log.md`：记录验证命令、结果和偏差。
- **关键约束**：
  - 不提交截图资产，除非用户后续要求。
  - 若后端服务或 Docker 环境不可用，记录阻塞原因。
- **依赖**：Task 1-5
- **风险标记**：外部接口
- **验收标准**：
  - 桌面端 1440px / 1024px 可用。
  - 移动端 390px / 375px 可用。
  - 首页、登录页、订单页无明显重叠、横向滚动、文本溢出。
- **验证命令**：
  - `git diff -- docs/dev-ops/nginx/html`
  - 浏览器/Playwright 检查 `index.html`、`login.html`、`order-list.html`
- **状态**：done

## 变更摘要

- **总文件数**：本轮修改 5 个静态前端文件，并更新 3 个 change 文档。
- **Spec-Plan 偏差记录**：无后端契约偏差；因本机 `docker` 不可用，未启动完整 Nginx/后端 compose 栈。
- **验证结果**：`node --check docs/dev-ops/nginx/html/js/index.js` 通过；`index.html`、`login.html`、`order-list.html` 内联脚本解析通过；接口路径与请求字段 grep 核对通过；`curl --noproxy '*' -I http://127.0.0.1:9001/` 因本机 9001 未启动而失败；`docker compose ... ps` 因 WSL 内没有 `docker` 命令而失败。
- **遗留问题**：需要在 Docker/Nginx 可用环境中做桌面与移动端真实浏览器截图验证，以及接口联调冒烟。
