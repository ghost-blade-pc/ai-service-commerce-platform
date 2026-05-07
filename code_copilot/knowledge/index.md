# 知识索引
> 领域知识的轻量索引。每条用一句话说清核心逻辑。
> 格式：- **触发关键词**: 一句话核心逻辑 → `包名.类名.方法名`（可选）

## 业务知识

- **创建支付单**: 用户下单先查未支付/未创建支付单订单，必要时复用订单或补建支付单；拼团订单会先调用营销锁单再创建支付宝支付单 → `top.licodetech.mall.domain.order.service.AbstractOrderService#createOrder`
- **支付宝支付回调**: 只有 `TRADE_SUCCESS` 且 RSA 验签通过才更新订单支付成功 → `top.licodetech.mall.trigger.http.AliPayController#payNotify`
- **拼团营销锁单**: 支付商城通过 Retrofit 调用拼团营销 `api/v1/gbm/trade/lock_market_pay_order` 获取营销抵扣结果 → `top.licodetech.mall.infrastructure.gateway.IGroupBuyMarketService#lockMarketPayOrder`
- **拼团营销结算**: 拼团服务回调支付商城 `api/v1/alipay/group_buy_notify` 后，支付商城批量更新订单营销结算状态并发布支付成功事件 → `top.licodetech.mall.trigger.http.AliPayController#groupBuyNotify`
- **用户订单列表**: Controller 限制 pageSize 最大 50，并通过 `lastId` 游标分页查询用户订单 → `top.licodetech.mall.trigger.http.AliPayController#queryUserOrderList`
- **退款**: 退款入口返回订单最新状态和描述，领域层需要负责订单归属和可退款状态判断 → `top.licodetech.mall.trigger.http.AliPayController#refundOrder`

## 技术约定

- **DDD 模块边界**: `trigger` 接协议，`api` 定契约，`domain` 放业务规则，`infrastructure` 适配数据库/MQ/外部 HTTP，`types` 放通用类型。
- **外部 REST 调用**: 当前项目已使用 Retrofit2，接口定义放 `infrastructure/gateway`，请求响应 DTO 放 `infrastructure/gateway/dto`。
- **统一响应**: 对外 API 使用 `top.licodetech.mall.api.response.Response`，外部网关响应使用 `top.licodetech.mall.infrastructure.gateway.response.Response`，不要混淆导包。
- **订单状态**: 订单状态由 `OrderStatusVO` 统一表达，新增业务不能散落状态字符串。
- **配置入口**: 拼团营销地址和回调配置位于 `app.config.group-buy-market.*`。

## 踩坑记录

- **Response 类重名**: `api/response/Response` 与 `infrastructure/gateway/response/Response` 类名相同，跨模块编辑时必须检查 import。
- **渠道字段拼写**: 配置里已有 `chanel` 字段拼写，改名会影响既有配置绑定，除非有完整迁移。
- **敏感配置**: `application-dev.yml` 当前含沙箱密钥和数据库密码，新增文档示例不要复制真实值。
