# Nginx 静态前端响应式改版方案
> status: done
> created: 2026-05-14
> scope: cross-project
> complexity: 中等

## 0. 归档摘要

本 change 已完成并归档。它围绕 `docs/dev-ops/nginx/html/` 的 Nginx 静态前端做了两轮改版：

- 第一轮：基于参考图统一首页、登录页、订单页的“智链云汇”AI 服务订阅与拼团采购视觉，保留现有登录、营销配置、支付下单、订单查询和退单接口。
- 第二轮：把首页桌面端进一步升级为“智链云汇 AI 服务购买工作台”，形成左侧价值说明、中间 AI API 套餐配置器、右侧订单结算栏；移动端新增底部结算条。

最终实现范围：

- 已修改静态页面：`docs/dev-ops/nginx/html/index.html`、`login.html`、`order-list.html`。
- 已修改样式/脚本：`docs/dev-ops/nginx/html/css/index.css`、`docs/dev-ops/nginx/html/js/index.js`。
- 未修改后端接口、数据库、MQ、订单状态、支付/退款规则。
- 商品图继续复用 `docs/dev-ops/nginx/html/images/sku-13811216-*.png`。

最终验证：

- JS 语法检查与 HTML 内联脚本解析通过。
- Nginx `http://127.0.0.1:9001/` 返回 200。
- 营销配置接口经 Nginx 代理返回 `code=0000`。
- 接口字段、无支撑入口和敏感文案均已 grep 核对。
- 未做自动截图验证：本地 Node 环境未安装 Playwright 包。

## 1. 背景与目标

参考用户提供的 4 张“AI 服务订阅 & 拼团采购平台”视觉稿，对 `docs/dev-ops/nginx/html/` 下现有静态前端页面做响应式改版规划，使页面同时适配移动端和桌面网页端。

本 change 先完成 proposal，经用户确认后进入 `/apply`；后续又追加并执行了桌面端购买工作台的二次设计迭代。

目标：

- 保留当前项目已经存在的业务能力：微信扫码登录、AI 服务套餐展示、拼团优惠、单独订阅、支付下单、我的订阅订单、退单。
- 使用参考图的视觉方向：浅色科技感背景、蓝紫主色、服务卡片、拼团进度/优惠信息、桌面端右侧结算摘要、移动端单列卡片流。
- 商品展示图片暂时继续使用现有资源 `docs/dev-ops/nginx/html/images/sku-13811216-*.png`，后续由用户替换。
- 不新增当前项目没有后端支撑的页面或功能。

## 2. 业务边界

- 影响项目：根工作区 `docs/dev-ops/nginx/` 静态页面，运行时依赖 `group-buy-market` 与 `s-pay-mall-ddd` 已有接口。
- 所属上下文：登录 / AI 服务订阅 / 拼团营销 / 订单查询 / 退单。
- 调用方向：浏览器静态页面通过 Nginx 同源代理调用两个后端服务。
- 是否跨项目：是，页面同时消费营销服务与商城支付服务接口。
- 是否涉及资金、订单状态、拼团状态：前端改版不改变资金、订单、拼团状态规则，但涉及支付与退单入口展示，必须保持接口请求参数不变。

## 3. 代码现状（Research Findings）

### 3.1 相关入口

- 静态资源：
  - `docs/dev-ops/nginx/html/index.html`：商品/拼团首页，查询营销配置、展示商品信息和拼团队伍，调用创建支付单接口。
  - `docs/dev-ops/nginx/html/css/index.css`：商品/拼团首页样式，当前以移动端商品详情页结构为主。
  - `docs/dev-ops/nginx/html/js/index.js`：Cookie 读取、轮播分页点、自动轮播、倒计时。
  - `docs/dev-ops/nginx/html/login.html`：微信扫码登录页。
  - `docs/dev-ops/nginx/html/order-list.html`：我的订阅订单页，查询订单并支持退单。
  - `docs/dev-ops/nginx/html/images/logo.png`、`sku-13811216-01.png` 到 `sku-13811216-04.png`：现有可复用图片。
- Nginx：
  - `docs/dev-ops/nginx/conf/conf.d/localhost.conf`：`/api/v1/gbm/` 代理到 `group-buy-market`，`/api/v1/alipay/` 和 `/api/v1/login/` 代理到 `s-pay-mall-ddd`，静态文件根目录为 `/usr/share/nginx/html`。
- 营销服务：
  - `group-buy-market/group-buy-market-lpc-trigger/src/main/java/top/licodetech/market/trigger/http/MarketIndexController.java`：`POST /api/v1/gbm/index/query_group_buy_market_config` 返回商品、拼团队伍、拼团统计。
  - `group-buy-market/group-buy-market-lpc-api/src/main/java/top/licodetech/market/api/dto/GoodsMarketResponseDTO.java`：当前商品字段包括 `goodsId`、`goodsName`、`totalQuota`、`originalPrice`、`deductionPrice`、`payPrice`；拼团队伍字段包括 `teamId`、`targetCount`、`lockCount`、`validTimeCountdown`；统计字段包括 `allTeamCount`、`allTeamCompleteCount`、`allTeamUserCount`。
- 商城支付服务：
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/AliPayController.java`：提供 `create_pay_order`、`query_user_order_list`、`refund_order`。
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-api/src/main/java/top/licodetech/mall/api/dto/CreatePayRequestDTO.java`：下单请求字段为 `userId`、`productId`、`servicePackageId`、`teamId`、`activityId`、`marketType`。
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-api/src/main/java/top/licodetech/mall/api/dto/UserOrderItemDTO.java`：订单列表字段为 `orderId`、`productName`、`totalAmount`、`payAmount`、`status`、`statusDesc`、`createTime`、`canRefund`。
- 登录服务：
  - `s-pay-mall-ddd/s-pay-mall-ddd-lpc-trigger/src/main/java/top/licodetech/mall/trigger/http/LoginController.java`：当前登录页消费 `weixin_qrcode_ticket_scene` 与 `check_login_scene`。

### 3.2 现有实现

- `index.html` 当前以移动端详情页形态组织：顶部“我的订阅”入口、轮播图、商品信息、拼团列表、底部固定操作栏。
- `index.html` 当前固定查询 `goodsId = "9890001"`，页面只展示一个套餐，不具备多个套餐、采购包、智能体服务包列表的数据来源。
- `order-list.html` 当前为独立订单列表页，支持分页加载和退单。
- `login.html` 当前为扫码登录卡片页，可改造成与新首页一致的品牌视觉，但不应改变扫码登录轮询逻辑。
- 当前根工作区没有 `./copilot/changes` 目录，实际权威工作流目录为 `code_copilot/changes`。

### 3.3 参考图取舍

可落地参考：

- 图 1：首屏品牌区、套餐卡片、拼团优惠、结算摘要、合作方/保障条。
- 图 2：拼团专区、成团进度、邀请/参团 CTA、优惠对比。
- 图 3：产品市场和采购清单的双栏布局，可映射为“当前单套餐 + 右侧订阅/拼团摘要”，但不新增多商品采购车能力。
- 图 4：企业管理中心视觉可借鉴卡片密度和后台布局风格，但当前项目没有企业后台、成员协作、用量统计、发票、续费提醒等接口，本次不新增该页面。

明确排除：

- 不新增“企业服务管理中心”“企业控制台”“成员协作”“企业采购审批”“用量统计”“续费提醒”“发票管理”“费用中心”等页面。
- 不新增多个 AI API 套餐、智能体服务包、工作流模板、MCP 工具服务、企业采购包等商品分类页，除非后续后端提供套餐目录接口。
- 不新增消息中心、帮助中心、解决方案页面。
- 不修改后端接口、数据库、MQ、订单状态、退款规则。

## 4. DDD 模块影响

| 项目 | 模块 | 是否影响 | 文件/类 | 说明 |
| --- | --- | --- | --- | --- |
| group-buy-market | api | 否 | `GoodsMarketResponseDTO` | 仅消费现有字段，不改契约。 |
| group-buy-market | trigger | 否 | `MarketIndexController#queryGroupBuyMarketConfig` | 仅消费现有接口，不改 Controller。 |
| group-buy-market | domain | 否 | - | 不改拼团规则。 |
| group-buy-market | infrastructure | 否 | - | 不改适配器和持久化。 |
| s-pay-mall-ddd | api | 否 | `CreatePayRequestDTO`、`UserOrderItemDTO` | 仅消费现有字段，不改契约。 |
| s-pay-mall-ddd | trigger | 否 | `AliPayController`、`LoginController` | 仅消费现有接口，不改 Controller。 |
| s-pay-mall-ddd | domain | 否 | - | 不改订单、支付、退款规则。 |
| s-pay-mall-ddd | infrastructure | 否 | - | 不改支付宝、微信、营销网关。 |
| root docs | static frontend | 是 | `docs/dev-ops/nginx/html/*` | 后续 apply 只改静态 HTML/CSS/JS 和复用图片。 |

## 5. 功能点

- [x] 首页桌面端改版：形成品牌导航、AI 服务套餐展示、拼团进度/优惠、右侧订阅结算摘要的响应式布局。
- [x] 首页移动端改版：移动端使用单列卡片流，底部购买操作栏不遮挡内容，拼团队伍和价格信息可读。
- [x] 商品图复用：继续使用 `sku-13811216-*.png` 作为套餐展示图，不新增图片资产依赖。
- [x] 拼团队伍展示：基于 `teamList` 展示队伍、剩余人数、倒计时；空队伍时展示开团引导。
- [x] 支付入口保持：单独订阅使用 `marketType = 0`，拼团订阅使用 `marketType = 1` 并携带 `teamId/activityId`。
- [x] 订单页响应式优化：桌面端更接近后台列表/卡片，移动端保持卡片流，保留分页和退单能力。
- [x] 登录页视觉统一：扫码登录页使用同一品牌、色彩、响应式间距，不改变二维码 ticket 获取与轮询逻辑。
- [x] 非支撑业务排除：不实现企业控制台、用量统计、采购审批、发票、消息中心等页面。

## 6. 业务规则

- 状态流转：不改变订单状态、支付状态、退款状态、拼团状态。
- 金额/优惠/退款：前端只展示 `originalPrice`、`deductionPrice`、`payPrice` 和订单金额，不做新的金额计算规则。
- 拼团营销：继续以 `query_group_buy_market_config` 返回的 `activityId`、`teamList`、`teamStatistic` 为准。
- 幂等：不改变后端幂等策略；前端后续可在点击支付/退单后禁用按钮，避免重复提交。
- 异常与补偿：不改变后端失败补偿；前端需保留接口失败提示。

## 7. 数据变更

| 操作 | 项目 | 表名 | 字段/索引 | 说明 |
| --- | --- | --- | --- | --- |
| 无 | - | - | - | 本次仅静态页面改版。 |

- 是否需要改 MyBatis mapper：否。
- 是否需要改初始化 SQL：否。

## 8. 接口与消息变更

### 8.1 REST API

| 方向 | 服务 | Path | Method | Request DTO | Response DTO | 鉴权/签名 |
| --- | --- | --- | --- | --- | --- | --- |
| 消费现有 | group-buy-market | `/api/v1/gbm/index/query_group_buy_market_config` | POST | `GoodsMarketRequestDTO` | `GoodsMarketResponseDTO` | 沿用现状。 |
| 消费现有 | s-pay-mall-ddd | `/api/v1/alipay/create_pay_order` | POST | `CreatePayRequestDTO` | `Response<String>` | 沿用现状。 |
| 消费现有 | s-pay-mall-ddd | `/api/v1/alipay/query_user_order_list` | POST | `QueryUserOrderListRequestDTO` | `QueryUserOrderListResponseDTO` | 沿用现状。 |
| 消费现有 | s-pay-mall-ddd | `/api/v1/alipay/refund_order` | POST | `RefundOrderRequestDTO` | `RefundOrderResponseDTO` | 沿用现状。 |
| 消费现有 | s-pay-mall-ddd | `/api/v1/login/weixin_qrcode_ticket_scene` | GET | query string | ticket | 沿用现状。 |
| 消费现有 | s-pay-mall-ddd | `/api/v1/login/check_login_scene` | GET | query string | login token | 沿用现状。 |

### 8.2 MQ 契约

| 方向 | Exchange | Routing Key | Queue | Message | 幂等键 |
| --- | --- | --- | --- | --- | --- |
| 无 | - | - | - | - | - |

## 9. 测试策略

- 测试范围：静态页面手动验证 + 浏览器响应式验证 + 接口联调冒烟。
- 优先测试类型：无需 Java 单元测试；建议使用本地 Nginx 页面和浏览器 DevTools/Playwright 做桌面与移动端截图核对。
- 需要独立 `test-spec.md`：否，当前为静态前端改版，可在 `tasks.md` 和 `log.md` 记录验证。
- 推荐验证命令：
  - `docker compose -f docs/dev-ops/docker-compose-environment.yml up -d`：按实际环境启动依赖。
  - `docker compose -f docs/dev-ops/docker-compose-app-v1.1.yml up -d`：按当前仓库实际 compose 文件验证 Nginx 与应用。
  - 浏览器访问 `http://127.0.0.1:9001/`、`/login.html`、`/order-list.html`。
  - 检查桌面宽度 1440px、1024px，以及移动端 390px、375px 下无横向滚动、无按钮遮挡、文本不溢出。

## 10. 待澄清

当前无阻塞待澄清项。

已确认事项：

- [x] 品牌名使用参考图中的“智链云汇”。
- [x] 允许把 `index.html` 从“商品详情页”改成“桌面端产品购买页 + 移动端商品卡片页”的混合响应式布局。
- [x] 桌面端不保留顶部导航中的“解决方案/帮助中心”等当前项目没有页面支撑的静态入口。

## 11. 技术决策

| 决策点 | 选择 | 备选 | 理由 |
| --- | --- | --- | --- |
| 变更目录 | `code_copilot/changes/frontend-responsive-ai-service-pages` | `./copilot/changes` | 当前仓库不存在 `./copilot/changes`，根级权威目录是 `code_copilot/changes`。 |
| 品牌名 | 使用“智链云汇” | 继续使用“AI 服务订阅平台” | 用户已确认采用参考图品牌名。 |
| 首页信息架构 | 桌面端产品购买页 + 移动端商品卡片页 | 保持商品详情页 | 用户已确认允许重构为混合响应式布局。 |
| 桌面端顶部导航 | 只保留当前页面有支撑的入口，例如首页、AI API 套餐、拼团专区、我的订阅/登录 | 保留“解决方案/帮助中心”等静态入口 | 当前项目没有对应页面，用户已确认不保留无支撑入口。 |
| 前端技术 | 原生 HTML/CSS/JS | 引入 Vue/React/Vite | 当前页面是 Nginx 静态页，引入构建链会扩大改动范围。 |
| 商品数据 | 只展示 `goodsId=9890001` 对应现有套餐 | 硬编码多个套餐卡片 | 后端当前只提供单商品营销配置，硬编码多套餐会误导用户。 |
| 图片资源 | 复用 `sku-13811216-*.png` | 新增图片 | 用户要求商品图先用之前图片。 |
| 后台管理页 | 不新增 | 参考图 4 新建企业控制台 | 当前缺少企业管理、成员、用量、发票等接口。 |

## 12. 风险与人工确认

- 资金风险：中。虽然不改金额规则，但页面展示支付金额与按钮文案必须严格对应 `originalPrice` / `payPrice`，避免误导。
- 状态流转风险：低。不改状态流转。
- MQ/外部接口风险：低。不改 MQ 和外部接口。
- 数据风险：无。不改数据库。
- 安全风险：中。登录 token 仍来自 Cookie；页面不可新增硬编码真实账号、密钥、token 或生产敏感信息。旧沙箱账号密码文案已在实现中移除。

## 13. 确认记录

- 确认时间：2026-05-14。
- 确认人：用户。
- 确认范围：确认品牌名、首页混合响应式布局、桌面端不保留无支撑静态导航入口；2026-05-14 已确认进入 `/apply` 执行前端页面修改。
- 二次确认：2026-05-14 用户确认执行 Task 7-10，将桌面端升级为购买工作台并补充移动端底部结算条。

## 14. 二次设计迭代方案：桌面端购买工作台

### 14.1 触发原因

用户反馈上一轮桌面端产品购买页“还算不错，但设计不够好”。本节记录二次设计 proposal；该 proposal 后续已在 Task 7-10 中执行完成。

当前实现已经具备品牌导航、套餐展示、拼团区和右侧结算面板，但桌面端仍偏“商品详情 + 卡片摘要”结构，购买决策链路不够集中。下一轮应在原有蓝紫 AI 服务风格上，把桌面端升级为“智链云汇 AI 服务购买工作台”。

### 14.2 设计目标

- 桌面端首屏从“商品详情页”升级为“购买工作台”：左侧价值说明，中间套餐配置，右侧固定结算。
- 拼团优惠不再只是区块说明，而是贯穿套餐选择、价格对比、进度和主 CTA。
- 商品图片继续复用现有 `sku-13811216-*.png`，但从大图轮播降级为产品资产/服务包视觉封面，避免页面像传统电商详情页。
- 移动端继续保持商品卡片页思路，但改为“纵向商品卡 + 拼团状态 + 底部结算条”，不强行缩放桌面三栏。
- 不新增后端未支撑的多套餐目录、企业控制台、用量统计、消息中心、解决方案或帮助中心页面。

### 14.3 桌面端信息架构

建议将 `index.html` 首屏组织为 12 栅格购买工作台：

| 区域 | 栅格建议 | 内容 | 说明 |
| --- | --- | --- | --- |
| 左侧价值栏 | 3/12 | 品牌主张、AI API 额度订阅、拼团省钱、支付后自动开通、订单可退订 | 保留参考图的品牌感，但控制文案密度。 |
| 中间配置区 | 6/12 | 当前套餐、Token 额度、订阅周期、权益清单、商品图封面、单独购买/拼团购买切换 | 使用现有单商品数据，不硬编码多个真实套餐。 |
| 右侧结算栏 | 3/12 | 商品摘要、原价、拼团优惠、合计、成团进度、主支付按钮、我的订阅入口 | 右侧在桌面端可 sticky，强化结算确定性。 |

推荐视觉层级：

- 第一优先级：套餐名称、Token 额度、拼团价、主 CTA。
- 第二优先级：拼团进度、优惠金额、剩余人数/倒计时。
- 第三优先级：服务保障、订单查询、退订说明。

### 14.4 中间配置区设计

中间区不再铺多张套餐卡，而是做“当前 AI API 套餐配置器”：

- 套餐标题：使用接口返回的 `goods.goodsName`，默认文案为 `AI API 套餐`。
- 额度展示：使用 `goods.totalQuota` 格式化为 Tokens/月。
- 价格对比：展示 `originalPrice`、`deductionPrice`、`payPrice`，不新增前端金额规则。
- 购买模式：提供 `拼团购买` 与 `单独购买` 两个切换按钮，切换时更新右侧结算摘要和主按钮文案。
- 商品图：使用现有 `sku-13811216-*.png`，作为服务包封面/资产预览，尺寸固定，避免轮播撑开布局。
- 权益清单：只写当前项目已支撑能力，例如支付后自动开通、订单可查询、支持退订退款、额度订阅。

### 14.5 右侧结算栏设计

右侧结算栏应成为桌面端最高确定性的区域：

- 顶部：`订单结算` + 当前购买模式。
- 商品：套餐名、Token 额度、服务周期文案。
- 价格：原价、优惠、应付金额。
- 拼团状态：当前队伍 `lockCount / targetCount`、还差人数、倒计时。
- 主 CTA：
  - 选中拼团模式且有队伍：`立即参团`
  - 选中拼团模式且无队伍：`立即开团`
  - 选中单独购买：`单独订阅`
- 次级入口：`查看我的订阅订单`。
- 安全说明：`安全支付 · 自动履约 · 支持退订`。

约束：

- CTA 请求体仍保持 `marketType=0/1`、`teamId`、`activityId`、`servicePackageId` 规则不变。
- 右侧金额只展示接口返回字段，不在前端创造新优惠算法。
- 移动端不要使用 sticky 右栏，改为底部结算条。

### 14.6 拼团展示设计

桌面端拼团信息建议分两层：

- 首屏右侧结算栏展示“当前推荐队伍”的关键状态，直接服务购买决策。
- 首屏下方或中间区下半部分展示队伍列表，作为更多选择。

队伍列表规则：

- 有 `teamList`：展示队伍发起人、`lockCount/targetCount`、还差人数、倒计时和参团按钮。
- 无 `teamList`：展示开团引导，不伪造头像、人数或虚假队伍。
- 统计区使用 `teamStatistic.allTeamCount`、`allTeamCompleteCount`、`allTeamUserCount`，只作为辅助信任信息。

### 14.7 移动端设计

移动端不复制桌面三栏，使用更直接的购买路径：

- 顶部：品牌、登录/我的订阅入口、简短标题。
- 主体：单张商品卡，内含商品图、套餐名、额度、拼团价、原价、权益。
- 拼团：在商品卡下展示当前推荐队伍和队伍列表，按钮尺寸固定。
- 底部：固定结算条，展示当前购买模式、应付金额、主 CTA。
- 导航：隐藏桌面导航，不展示无页面支撑入口。

移动端验收重点：

- 375px / 390px 下无横向滚动。
- 底部结算条不遮挡最后一个拼团卡片。
- 长商品名、长金额、倒计时不会撑破按钮或卡片。

### 14.8 视觉风格

保留上一轮和参考图一致的方向，但降低装饰密度：

- 主色：蓝 / 紫作为 CTA 和关键价格。
- 辅色：青色用于 API/技术标签，绿色用于保障/成功，橙色用于优惠/倒计时。
- 面板：浅色玻璃面板、1px 边框、轻阴影，卡片圆角控制在 8px。
- 背景：浅色科技感渐变即可，避免大面积单色蓝紫或装饰性圆球。
- 字体：桌面端首屏标题可以大，但配置区、结算栏、卡片内标题要克制，避免压缩信息密度。

### 14.9 本次不做

- 不引入 Vue/React/Vite 或新的构建链。
- 不新增图片资产；商品图继续使用现有 SKU 图片。
- 不新增后端接口、数据库、MQ、订单状态或退款规则。
- 不把参考图 4 的企业控制台、成员协作、用量图表、发票和续费提醒做进页面。
- 不在桌面导航恢复“解决方案/帮助中心”等无页面支撑入口。

### 14.10 验收标准

- 桌面端 1440px 首屏形成清晰三段：价值说明、套餐配置、订单结算。
- 桌面端 1024px 不出现卡片挤压、按钮文字溢出或右侧结算栏遮挡。
- 移动端 375px / 390px 使用商品卡片与底部结算条，无横向滚动。
- `create_pay_order` 请求字段与上一轮一致。
- `query_group_buy_market_config`、登录、订单列表、退单接口调用路径不变。
- 页面不出现当前项目无支撑的入口和业务能力。

## 15. 二次设计 Apply 结果

2026-05-14 用户确认执行 Task 7-10，本轮已将 `index.html` 桌面端改为“智链云汇 AI 服务购买工作台”，并同步移动端商品卡片路径。

已完成：

- 桌面端首屏改为三栏：左侧价值说明、中间 AI API 套餐配置器、右侧订单结算栏。
- 中间区域提供 `拼团购买` / `单独订阅` 模式切换，联动右侧结算摘要与主 CTA 文案。
- 右侧结算栏展示套餐、额度、原价、拼团优惠、应付金额、推荐拼团状态和订单入口。
- 移动端新增底部结算条，复用同一套购买模式与金额状态，不把桌面三栏压缩到移动端。
- 支付请求仍保持 `marketType=0/1`、`servicePackageId`、`teamId`、`activityId` 等字段规则。
- 页面未恢复“解决方案/帮助中心”等无支撑入口，也未新增企业后台、用量统计、发票、消息中心等能力。

验证结果：

- `node --check docs/dev-ops/nginx/html/js/index.js` 通过。
- `index.html` 内联脚本 `new Function` 解析通过。
- `rg` 核对接口路径、`marketType`、`servicePackageId`、`teamId`、`activityId` 均保留。
- `rg` 核对无 `解决方案`、`帮助中心`、旧沙箱账号密码和无支撑业务入口文案。
- `curl --noproxy '*' -I http://127.0.0.1:9001/` 返回 200。
- `docker compose -f docs/dev-ops/docker-compose-app-v1.1.yml ps` 显示 Nginx、商城、营销和基础设施容器运行中。
- `POST /api/v1/gbm/index/query_group_buy_market_config` 经 Nginx 代理返回 `code=0000` 的套餐数据。

未完成自动截图验证：本地 Node 环境未安装 Playwright 包，本轮未做自动桌面/移动端截图。
