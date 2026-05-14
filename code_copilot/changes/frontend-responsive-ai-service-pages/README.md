# frontend-responsive-ai-service-pages 归档说明

> status: done
> archived: 2026-05-14

## 变更目标

参考“智链云汇 AI 服务订阅 & 拼团采购平台”视觉方向，改造 `docs/dev-ops/nginx/html/` 下的 Nginx 静态前端页面，使当前已有业务能力适配桌面端和移动端。

## 最终实现

- 首页 `index.html`：
  - 桌面端改为“智链云汇 AI 服务购买工作台”。
  - 左侧展示服务价值，中间展示 AI API 套餐配置器，右侧展示订单结算。
  - 支持 `拼团购买` / `单独订阅` 模式切换，并联动结算金额和 CTA。
  - 移动端使用商品卡片路径，并新增底部结算条。
- 登录页 `login.html`：
  - 统一品牌视觉和响应式布局。
  - 保持微信扫码 ticket 获取与轮询逻辑不变。
- 订单页 `order-list.html`：
  - 优化订单管理视图和移动端卡片体验。
  - 保留订单查询、分页和退单能力。

## 边界

- 未修改后端接口、数据库、MQ、订单状态、支付规则、退款规则。
- 未新增企业控制台、用量统计、发票、消息中心、解决方案、帮助中心等无后端支撑页面。
- 商品图片继续复用 `docs/dev-ops/nginx/html/images/sku-13811216-*.png`。

## 接口契约

- 营销配置：`POST /api/v1/gbm/index/query_group_buy_market_config`
- 创建支付单：`POST /api/v1/alipay/create_pay_order`
- 订单查询：`POST /api/v1/alipay/query_user_order_list`
- 退单：`POST /api/v1/alipay/refund_order`
- 登录：`GET /api/v1/login/weixin_qrcode_ticket_scene`、`GET /api/v1/login/check_login_scene`

下单字段保持：

- `userId`
- `productId`
- `servicePackageId`
- `teamId`
- `activityId`
- `marketType`

## 验证摘要

- `node --check docs/dev-ops/nginx/html/js/index.js` 通过。
- `index.html`、`login.html`、`order-list.html` 内联脚本解析通过。
- Nginx `http://127.0.0.1:9001/` 返回 200。
- 营销配置接口经 Nginx 代理返回 `code=0000`。
- `git diff --check` 通过，仅有 LF/CRLF 换行提示。

未完成自动截图验证：本地 Node 环境未安装 Playwright 包。

## 后续拆分建议

- 自动截图验证：新建 `frontend-responsive-screenshot-verification`。
- 多套餐/智能体/企业控制台等能力：先补后端目录、订单、权益或统计接口，再新建独立 change。
- 商品图片替换：优先只替换现有 SKU 图片资源，不改变页面接口契约。

## 全局知识

本 change 已沉淀到：

- `code_copilot/knowledge/static-frontend-ai-service-pages.md`
- `code_copilot/knowledge/index.md`
