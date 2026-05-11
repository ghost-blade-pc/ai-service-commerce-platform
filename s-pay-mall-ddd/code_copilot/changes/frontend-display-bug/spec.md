# 前端输出展示bug
> status: propose
> created: 2026-05-08
> complexity: 🟢简单

## 1. 背景与目标

修复前端商品详情页"单独购买"按钮点击后支付确认弹窗展示的价格与按钮标注价格不一致的 bug，并优化订单列表页退单按钮的布局位置。

### 1.1 业务边界

- 所属上下文：订单
- 调用方向：前端页面展示，不涉及后端接口变更
- 是否涉及资金或订单状态：否（仅前端展示修复，不改变支付/退款逻辑）

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

- HTTP: 前端静态页面，通过 Nginx 托管
- Domain Service: 无
- Repository/Port: 无

### 2.2 现有实现

涉及文件：

| 文件 | 说明 |
|------|------|
| `docs/dev-ops/nginx/html/index.html` | 拼团商品详情页，含商品信息、拼团列表、底部购买按钮、支付确认弹窗逻辑 |
| `docs/dev-ops/nginx/html/css/index.css` | 商品详情页样式 |
| `docs/dev-ops/nginx/html/js/index.js` | 公共 JS（Cookie 读取、轮播图、倒计时） |
| `docs/dev-ops/nginx/html/order-list.html` | 订单列表页，含退单按钮和退单逻辑 |

### 2.3 发现与风险

**Bug 1 — 单独购买支付确认弹窗价格错误**

文件：`docs/dev-ops/nginx/html/index.html`

- 第 182-183 行：底部"单独购买"按钮的文本和 dataset 正确使用了 `goods.originalPrice`：
  ```javascript
  buyAloneBtn.textContent = `单独购买(￥${goods.originalPrice.toFixed(0)})`;
  buyAloneBtn.dataset.price = goods.originalPrice;
  ```
- 第 234-264 行：单独购买点击事件中，第 237 行通过 `button.dataset.price` 正确获取了 `originalPrice`，赋值给局部变量 `price`，但第 257 行调用 `showPaymentConfirm` 时传入了 `goods.payPrice`（拼团优惠价）而非变量 `price`（原价）：
  ```javascript
  // 第 257 行 — BUG：应该传 price（原价），实际传了 goods.payPrice（拼团优惠价）
  showPaymentConfirm(goods.payPrice);
  ```
- 对比拼团购买的分支（第 188-230 行），第 223 行同样使用 `goods.payPrice`，这是正确的，因为拼团购买确实应该展示拼团优惠价。

**根因**：单独购买分支的 `showPaymentConfirm` 调用硬编码了 `goods.payPrice`，没有使用已提取的 `price` 变量。

**Bug 2 — 退单按钮位置优化**

文件：`docs/dev-ops/nginx/html/order-list.html`

- 第 52-61 行：订单卡片使用 `grid-template-columns: 1fr auto` 布局，左侧为订单信息 `.order-main`，右侧为操作区 `.actions`（第 261 行），退单按钮放置在右侧独立的操作列中。
- 第 119-124 行：`.actions` 样式为右对齐的 flex 容器，最小宽度 76px。
- 第 126-141 行：退单按钮为红色（`#ef4444`），宽 76px，高 34px。

当前布局在桌面端正常，但在移动端（第 169-181 行的 `@media (max-width: 640px)`）时，订单卡片变为单列布局，操作区左对齐——但退单按钮仍紧贴订单信息，视觉上不够理想。

优化方向：将退单按钮从独立的右侧列移动到订单信息卡片内部（如金额或状态行右侧），使其与订单信息更紧密关联，减少页面横向空间占用。

### 2.4 DDD 模块影响

| 模块 | 是否影响 | 文件/类 | 说明 |
|------|----------|---------|------|
| api | 否 | - | 无后端接口变更 |
| trigger | 否 | - | 无后端接口变更 |
| domain | 否 | - | 无后端接口变更 |
| infrastructure | 否 | - | 无后端接口变更 |
| types | 否 | - | 无后端接口变更 |
| app | 否 | - | 无后端接口变更 |

> 本次变更仅涉及 `docs/dev-ops/nginx/html/` 下的静态前端文件。

## 3. 功能点

- [ ] Bug 1：修复单独购买支付确认弹窗展示价格为拼团优惠价的问题——应展示原价
- [ ] Bug 2：优化订单列表页退单按钮布局，将其从独立右侧列移至订单信息区域内部

## 4. 业务规则

- 订单状态流转：不变更
- 支付/退款规则：不变更
- 拼团营销规则：不变更
- 幂等规则：不变更
- 异常与补偿：不变更

## 5. 数据变更

无数据库变更。

- 是否需要改 MyBatis mapper：否
- 是否需要改初始化 SQL：否

## 6. 接口变更

无后端接口变更。

### 6.1 REST API 契约

无变更。

### 6.2 MQ 契约

无变更。

## 7. 影响范围

- 前端页面：
  - `docs/dev-ops/nginx/html/index.html` — 修复单独购买支付确认弹窗价格
  - `docs/dev-ops/nginx/html/order-list.html` — 调整退单按钮布局
  - `docs/dev-ops/nginx/html/css/index.css` — 可能需要微调退单按钮相关样式
- 外部服务：无
- 定时任务：无
- MQ 消费者/生产者：无
- 运维配置：无

## 8. 风险与关注点

- ⚠️ 资金风险：无（仅前端展示修复，支付请求参数不变）
- ⚠️ 状态流转风险：无
- ⚠️ 外部服务失败风险：无
- ⚠️ 重复请求/重复消息风险：无

## 8.5 测试策略

- **测试范围**：前端页面手动验证
- **覆盖率目标**：不适用（纯前端展示变更）
- **独立 Test Spec**：否
- **优先验证命令**：
  1. 打开商品详情页，点击"单独购买"，确认支付弹窗展示的价格与按钮标注的原价一致
  2. 点击"开团购买"/"参与拼团"，确认支付弹窗展示的价格仍为拼团优惠价
  3. 打开订单列表页，确认退单按钮位置合理、功能正常

## 9. 待澄清

- [ ] 退单按钮的具体目标位置：是放在金额行右侧、状态标签旁边，还是订单卡片底部？（当前方案：将退单按钮放在订单信息 `.order-main` 内部，金额和状态行下方，与订单信息左对齐）

## 10. 技术决策

| 决策点 | 选择 | 备选 | 理由 |
|--------|------|------|------|
| Bug 1 修复方式 | 将 `goods.payPrice` 替换为已提取的 `price` 变量 | 直接写 `goods.originalPrice` | `price` 变量已从 `button.dataset.price` 正确读取原价，复用变量更清晰 |
| 退单按钮布局 | 将退单按钮移入 `.order-main` 内部，放在 meta 信息下方 | 保持在 `.actions` 独立列，仅调整样式 | 与订单信息整合更自然，移动端体验更好 |

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|-------------|------|
| Task 1: 修复单独购买支付确认弹窗价格 | ✅ done | `docs/dev-ops/nginx/html/index.html` | 第 257 行 `goods.payPrice` → `price` |
| Task 2: 优化退单按钮布局位置 | ✅ done | `docs/dev-ops/nginx/html/order-list.html` | 布局从 grid 改 flex，退单按钮移入 `.order-main` 内部 |

## 12. 审查结论

## 13. 确认记录（HARD-GATE）

- **确认时间**：
- **确认人**：
