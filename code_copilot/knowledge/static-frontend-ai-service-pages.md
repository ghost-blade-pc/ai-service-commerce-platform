# 静态前端：AI 服务订阅与拼团购买页

> 来源 change：`code_copilot/changes/frontend-responsive-ai-service-pages`
> 最后验证：2026-05-14
> 适用范围：`docs/dev-ops/nginx/html/` 下由 Nginx 托管的原生 HTML/CSS/JS 页面

## 适用场景

当任务涉及以下内容时，优先读取本文：

- 修改 `docs/dev-ops/nginx/html/index.html`、`login.html`、`order-list.html`。
- 调整 AI 服务订阅、拼团购买、订单查询、退单、扫码登录等静态前端页面。
- 校验 Nginx 静态前端与 `group-buy-market`、`s-pay-mall-ddd` 的接口契约。
- 继续优化“智链云汇”桌面端购买工作台、移动端商品卡片页或底部结算条。

## 前端入口

| 文件 | 作用 | 关键约束 |
| --- | --- | --- |
| `docs/dev-ops/nginx/html/index.html` | AI 服务套餐购买页，消费营销配置并创建支付单 | 当前只展示 `goodsId=9890001` 的单商品套餐；不要硬编码多个真实套餐。 |
| `docs/dev-ops/nginx/html/css/index.css` | 首页、拼团、结算、响应式样式 | 桌面端购买工作台；移动端商品卡片和底部结算条。 |
| `docs/dev-ops/nginx/html/js/index.js` | Cookie、轮播兼容、倒计时工具 | 保持无构建链原生 JS；倒计时需兼容异常时间串。 |
| `docs/dev-ops/nginx/html/login.html` | 微信扫码登录页 | 保持 ticket 获取与轮询逻辑不变。 |
| `docs/dev-ops/nginx/html/order-list.html` | 我的订阅订单页 | 保持订单查询、分页、退单接口和 `canRefund` 控制。 |

## Nginx 与接口契约

静态页面通过 Nginx 同源代理访问后端接口。当前页面只消费现有接口，不拥有后端业务规则。

| 能力 | Path | Method | 所属服务 | 前端约束 |
| --- | --- | --- | --- | --- |
| 营销配置 | `/api/v1/gbm/index/query_group_buy_market_config` | POST | `group-buy-market` | 请求固定使用 `goodsId=9890001`；响应中的 `goods`、`teamList`、`teamStatistic` 是页面展示来源。 |
| 创建支付单 | `/api/v1/alipay/create_pay_order` | POST | `s-pay-mall-ddd` | 请求字段保持 `userId`、`productId`、`servicePackageId`、`teamId`、`activityId`、`marketType`。 |
| 订单查询 | `/api/v1/alipay/query_user_order_list` | POST | `s-pay-mall-ddd` | 订单页展示订单号、金额、状态、创建时间和退单按钮。 |
| 退单 | `/api/v1/alipay/refund_order` | POST | `s-pay-mall-ddd` | 退单按钮只在 `canRefund` 为 true 时展示。 |
| 登录二维码 | `/api/v1/login/weixin_qrcode_ticket_scene` | GET | `s-pay-mall-ddd` | 保持扫码登录 ticket 获取逻辑。 |
| 登录轮询 | `/api/v1/login/check_login_scene` | GET | `s-pay-mall-ddd` | 登录成功后写入 `loginToken` 并跳转首页。 |

下单语义：

- 单独订阅：`marketType = 0`，不传拼团队伍语义。
- 拼团订阅：`marketType = 1`，保留 `activityId`，有可参团队伍时传 `teamId`，无队伍时允许发起开团。
- `productId` 与 `servicePackageId` 当前都使用营销返回的 `goods.goodsId`，用于兼容既有商品与服务套餐语义。

## 当前页面设计基线

首页采用“桌面端购买工作台 + 移动端商品卡片页”的响应式基线。

桌面端：

- 左侧价值栏：品牌、AI API 额度订阅、拼团优惠、自动履约等信任信息。
- 中间配置区：当前 AI API 套餐、额度、订阅周期、原价、拼团优惠、购买模式切换。
- 右侧结算栏：订单摘要、应付金额、推荐拼团状态、主 CTA 和我的订阅入口。

移动端：

- 隐藏桌面导航和右侧 sticky 结算栏。
- 商品卡片、拼团状态和队伍列表纵向展示。
- 底部固定结算条同步当前购买模式、应付金额和主 CTA。

设计约束：

- 品牌名使用“智链云汇”。
- 不恢复“解决方案”“帮助中心”等无页面支撑入口。
- 不新增企业控制台、成员协作、用量统计、发票、消息中心等后端未支撑功能。
- 商品图片继续复用 `docs/dev-ops/nginx/html/images/sku-13811216-*.png`，后续替换图片时优先只替换资源，不改接口契约。
- 页面只展示后端返回的金额字段：`originalPrice`、`deductionPrice`、`payPrice`；不要在前端创造新优惠算法。

## DDD 边界

该前端目录属于根工作区 `docs/dev-ops` 静态资源，不属于任一后端 DDD 模块。修改静态页面时：

- 不修改 `group-buy-market` 的营销领域规则。
- 不修改 `s-pay-mall-ddd` 的订单、支付、退款领域规则。
- 不修改数据库、MQ、Mapper、Controller、DTO。
- 不把前端展示逻辑写成新的资金、退款、拼团状态规则。

如果需求需要新增商品分类、多套餐目录、企业管理、用量统计、发票或消息中心，必须先确认后端契约，再新建独立 change。

## 验证方式

推荐按风险从低到高验证：

```bash
node --check docs/dev-ops/nginx/html/js/index.js
```

```bash
node -e "const fs=require('fs'); for (const f of ['docs/dev-ops/nginx/html/index.html','docs/dev-ops/nginx/html/login.html','docs/dev-ops/nginx/html/order-list.html']) { const html=fs.readFileSync(f,'utf8'); const scripts=[...html.matchAll(/<script>([\\s\\S]*?)<\\/script>/g)].map(m=>m[1]); for (const code of scripts) new Function(code); console.log(f+': inline scripts parse ok ('+scripts.length+')'); }"
```

```bash
rg -n "api/v1|marketType|servicePackageId|teamId|activityId" docs/dev-ops/nginx/html
```

```bash
rg -n "解决方案|帮助中心|hovdlm|111111|企业控制台|用量统计|发票|消息中心" docs/dev-ops/nginx/html
```

```bash
curl --noproxy '*' -I http://127.0.0.1:9001/
```

```bash
docker compose -f docs/dev-ops/docker-compose-app-v1.1.yml ps
```

可选接口冒烟：

```bash
curl --noproxy '*' -sS -X POST http://127.0.0.1:9001/api/v1/gbm/index/query_group_buy_market_config \
  -H 'Content-Type: application/json' \
  -d '{"userId":"codex-check","source":"s01","channel":"c01","goodsId":"9890001"}'
```

自动截图验证只有在本地安装 Playwright 或同类浏览器测试工具后再执行；不要在未安装工具时伪造截图结论。

## 已知限制

- 当前首页仍是单商品购买页，商品来源为 `goodsId=9890001`。
- 前端没有独立构建链，不应在小改动中引入 Vue、React、Vite 等构建体系。
- 自动桌面/移动端截图验证在上一轮归档时未执行，原因是本地 Node 环境缺少 Playwright 包。
- `loginToken` 来源仍沿用现有 Cookie 逻辑；本知识文件不改变鉴权与安全策略。
