package top.licodetech.mall.domain.order.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.licodetech.mall.domain.order.adapter.port.IProductPort;
import top.licodetech.mall.domain.order.adapter.port.IRefundPort;
import top.licodetech.mall.domain.order.adapter.repository.IOrderRepository;
import top.licodetech.mall.domain.order.adapter.repository.IRefundTaskRepository;
import top.licodetech.mall.domain.order.model.aggregate.CreateOrderAggregate;
import top.licodetech.mall.domain.order.model.entity.MarketPayDiscountEntity;
import top.licodetech.mall.domain.order.model.entity.OrderEntity;
import top.licodetech.mall.domain.order.model.entity.PayOrderEntity;
import top.licodetech.mall.domain.order.model.entity.RefundTaskEntity;
import top.licodetech.mall.domain.order.model.valobj.MarketTypeVO;
import top.licodetech.mall.domain.order.model.valobj.OrderStatusVO;
import top.licodetech.mall.domain.order.model.valobj.RefundTypeVO;
import top.licodetech.mall.domain.subscription.service.ISubscriptionService;
import top.licodetech.mall.types.common.Constants;
import top.licodetech.mall.types.exception.AppException;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class OrderService extends AbstractOrderService{

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_REFUND_TASK_PAGE_SIZE = 20;
    private static final int MAX_REFUND_TASK_PAGE_SIZE = 100;
    private static final int REFUND_STATUS_QUERY_RETRY_TIMES = 3;
    private static final long REFUND_STATUS_QUERY_RETRY_INTERVAL_MILLIS = 100L;

    @Value("${alipay.notify_url}")
    private String notifyUrl;
    @Value("${alipay.return_url}")
    private String returnUrl;

    @Resource
    private AlipayClient alipayClient;

    @Resource
    private IRefundPort refundPort;

    @Resource
    private IRefundTaskRepository refundTaskRepository;

    @Resource
    private ISubscriptionService subscriptionService;

    public OrderService(IOrderRepository repository, IProductPort port) {
        super(repository, port);
    }

    @Override
    protected void doSaveOrder(CreateOrderAggregate orderAggregate) {
        repository.doSaveOrder(orderAggregate);
    }

    @Override
    protected MarketPayDiscountEntity lockMarketPayOrder(String userId, String teamId, Long activityId, String productId, String orderId) {
        return port.lockMarketPayOrder(userId, teamId, activityId, productId, orderId);
    }

    @Override
    protected PayOrderEntity doPrepayOrder(String userId, String productId, String productName, String orderId, BigDecimal totalAmount) throws AlipayApiException {
        return doPrepayOrder(userId, productId, productName, orderId, totalAmount, null);
    }

    @Override
    protected PayOrderEntity doPrepayOrder(String userId, String productId, String productName, String orderId, BigDecimal totalAmount, MarketPayDiscountEntity marketPayDiscountEntity) throws AlipayApiException {
        // 支付金额
        BigDecimal payAmount = null == marketPayDiscountEntity ? totalAmount : marketPayDiscountEntity.getPayPrice();

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(notifyUrl);
        request.setReturnUrl(returnUrl);

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderId);
        bizContent.put("total_amount", payAmount);
        bizContent.put("subject", productName);
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        request.setBizContent(bizContent.toString());

        String form = alipayClient.pageExecute(request).getBody();

        PayOrderEntity payOrderEntity = new PayOrderEntity();
        payOrderEntity.setOrderId(orderId);
        payOrderEntity.setPayUrl(form);
        payOrderEntity.setOrderStatus(OrderStatusVO.PAY_WAIT);

        // 营销信息
        payOrderEntity.setMarketType(null == marketPayDiscountEntity ? MarketTypeVO.NO_MARKET.getCode() : MarketTypeVO.GROUP_BUY_MARKET.getCode());
        payOrderEntity.setMarketDeductionAmount(null == marketPayDiscountEntity ? BigDecimal.ZERO : marketPayDiscountEntity.getDeductionPrice());
        payOrderEntity.setPayAmount(payAmount);

        repository.updataOrderPayInfo(payOrderEntity);

        return payOrderEntity;
    }

    @Override
    public void changeOrderPaySuccess(String orderId, Date payTime) {
        OrderEntity orderEntity = repository.queryOrderByOrderId(orderId);
        if (null == orderEntity) {
            return;
        }

        if (MarketTypeVO.GROUP_BUY_MARKET.getCode().equals(orderEntity.getMarketType())) {
            repository.changeMarketOrderPaySuccess(orderId);
            // 发起营销结算。这个过程可以是http/rpc直接调用，也可以发一个商城交易支付完成的消息，之后拼团系统自己接收做结算。
            port.settlementMarketPayOrder(orderEntity.getUserId(), orderId, payTime);
            // 注意；在公司中，发起结算的http/rpc调用可能会失败，这个时候还会有增加job任务补偿。条件为，检查一笔走了拼团的订单，超过n分钟后，仍然没有做拼团结算状态变更。
            // 我们这里失败了，会抛异常，借助支付宝回调/job来重试。你可以单独实现一个独立的job来处理。
        } else {
            repository.changeOrderPaySuccess(orderId, payTime);
        }
    }

    @Override
    public void changeOrderMarketSettlement(List<String> outTradeNoList) {
        repository.changeOrderMarketSettlement(outTradeNoList);
    }

    @Override
    public List<String> queryNoPayNotifyOrder() {
        return repository.queryNoPayNotifyOrder();
    }

    @Override
    public List<String> queryTimeoutCloseOrderList() {
        return repository.queryTimeoutCloseOrderList();
    }

    @Override
    public boolean changeOrderClose(String orderId) {
        return repository.changeOrderClose(orderId);
    }

    @Override
    public List<OrderEntity> queryUserOrderList(String userId, Long lastId, Integer pageSize) {
        if (StringUtils.isBlank(userId)) {
            throw new AppException(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode(), "userId不能为空");
        }

        int limit = null == pageSize ? DEFAULT_PAGE_SIZE : pageSize;
        if (limit <= 0) {
            throw new AppException(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode(), "pageSize必须大于0");
        }
        limit = Math.min(limit, MAX_PAGE_SIZE);

        return repository.queryUserOrderList(userId, lastId, limit + 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderEntity refundOrder(String userId, String orderId) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(orderId)) {
            throw new AppException(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode(), "userId和orderId不能为空");
        }

        OrderEntity orderEntity = repository.queryOrderByOrderId(orderId);
        if (null == orderEntity) {
            throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "订单不存在");
        }

        if (!userId.equals(orderEntity.getUserId())) {
            throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "订单不属于当前用户");
        }

        OrderStatusVO orderStatusVO = orderEntity.getOrderStatusVO();
        if (null != orderStatusVO && OrderStatusVO.isRefunded(orderStatusVO.getCode())) {
            return orderEntity;
        }
        if (null != orderStatusVO && OrderStatusVO.isRefunding(orderStatusVO.getCode())) {
            return orderEntity;
        }
        if (null == orderStatusVO || !OrderStatusVO.canRefund(orderStatusVO.getCode())) {
            throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "当前订单状态不允许退单");
        }

        if (MarketTypeVO.GROUP_BUY_MARKET.getCode().equals(orderEntity.getMarketType())) {
            port.refundMarketPayOrder(userId, orderId);
            int updateCount = repository.changeOrderRefunding(userId, orderId);
            if (1 != updateCount) {
                return queryRefundingOrRefundedOrder(orderId, "退单申请失败，请稍后重试");
            }

            orderEntity.setOrderStatusVO(OrderStatusVO.REFUNDING);
            return orderEntity;
        }

        int updateCount = repository.changeOrderRefunding(userId, orderId);
        if (1 != updateCount) {
            OrderEntity latestOrderEntity = queryRefundingOrRefundedOrder(orderId, "退单申请失败，请稍后重试");
            if (OrderStatusVO.isRefunded(latestOrderEntity.getOrderStatusVO().getCode())) {
                return latestOrderEntity;
            }
        }

        return changeOrderRefundSuccess(orderId, needPayRefund(orderStatusVO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderEntity changeOrderRefundSuccess(String orderId) {
        return changeOrderRefundSuccess(orderId, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderEntity changeOrderRefundSuccess(String orderId, RefundTypeVO refundType) {
        if (null == refundType) {
            throw new AppException(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode(), "refundType不能为空");
        }
        return changeOrderRefundSuccess(orderId, refundType.isNeedPayRefund());
    }

    private OrderEntity changeOrderRefundSuccess(String orderId, boolean needPayRefund) {
        if (StringUtils.isBlank(orderId)) {
            throw new AppException(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode(), "orderId不能为空");
        }

        OrderEntity orderEntity = repository.queryOrderByOrderId(orderId);
        if (null == orderEntity) {
            throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "订单不存在");
        }

        OrderStatusVO orderStatusVO = orderEntity.getOrderStatusVO();
        if (null != orderStatusVO && OrderStatusVO.isRefunded(orderStatusVO.getCode())) {
            return orderEntity;
        }

        if (null != orderStatusVO && OrderStatusVO.canRefund(orderStatusVO.getCode())) {
            int refundingCount = repository.changeOrderRefunding(orderEntity.getUserId(), orderId);
            if (1 != refundingCount) {
                orderEntity = queryRefundingOrRefundedOrder(orderId, "更新退单中状态失败");
                orderStatusVO = orderEntity.getOrderStatusVO();
                if (OrderStatusVO.isRefunded(orderStatusVO.getCode())) {
                    return orderEntity;
                }
            } else {
                orderEntity.setOrderStatusVO(OrderStatusVO.REFUNDING);
                orderStatusVO = OrderStatusVO.REFUNDING;
            }
        }

        if (null == orderStatusVO || !OrderStatusVO.isRefunding(orderStatusVO.getCode())) {
            throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "当前订单状态不允许完成退款");
        }

        if (needPayRefund) {
            BigDecimal refundAmount = null == subscriptionService
                    ? (null != orderEntity.getPayAmount() ? orderEntity.getPayAmount() : orderEntity.getTotalAmount())
                    : subscriptionService.calculateRefundAmount(orderEntity);
            if (null == refundAmount) {
                throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "退款金额不能为空");
            }

            if (BigDecimal.ZERO.compareTo(refundAmount) == 0) {
                log.info("服务套餐额度已全部消耗，退款金额为0，跳过外部退款 orderId:{}", orderId);
            } else if (null != refundPort && !refundPort.refund(orderId, refundAmount)) {
                throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "模拟退款失败");
            } else if (null == refundPort) {
                log.info("模拟退款端口未注入，按测试场景跳过外部退款 orderId:{} refundAmount:{}", orderId, refundAmount);
            }
        }

        int updateCount = repository.changeOrderRefunded(orderId);
        if (1 != updateCount) {
            OrderEntity latestOrderEntity = repository.queryOrderByOrderId(orderId);
            if (null != latestOrderEntity
                    && null != latestOrderEntity.getOrderStatusVO()
                    && OrderStatusVO.isRefunded(latestOrderEntity.getOrderStatusVO().getCode())) {
                return latestOrderEntity;
            }
            throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "更新退款完成状态失败");
        }

        if (null != subscriptionService) {
            subscriptionService.revokeEntitlement(orderId);
        }
        orderEntity.setOrderStatusVO(OrderStatusVO.REFUNDED);
        return orderEntity;
    }

    @Override
    public boolean receiveRefundSuccessMessage(String orderId, String message) {
        return receiveRefundSuccessMessage(orderId, RefundTypeVO.PAID_FORMED, message);
    }

    @Override
    public boolean receiveRefundSuccessMessage(String orderId, RefundTypeVO refundType, String message) {
        if (StringUtils.isBlank(orderId)) {
            throw new AppException(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode(), "orderId不能为空");
        }
        if (null == refundType) {
            throw new AppException(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode(), "refundType不能为空");
        }
        if (null == refundTaskRepository) {
            changeOrderRefundSuccess(orderId, refundType);
            return true;
        }

        refundTaskRepository.saveRefundTask(orderId, refundType, message);
        return processRefundTask(RefundTaskEntity.builder()
                .orderId(orderId)
                .refundType(refundType)
                .message(message)
                .build());
    }

    @Override
    public boolean processRefundTask(String orderId) {
        return processRefundTask(RefundTaskEntity.builder()
                .orderId(orderId)
                .refundType(RefundTypeVO.PAID_FORMED)
                .build());
    }

    @Override
    public boolean processRefundTask(RefundTaskEntity refundTaskEntity) {
        if (null == refundTaskEntity) {
            throw new AppException(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode(), "refundTaskEntity不能为空");
        }
        String orderId = refundTaskEntity.getOrderId();
        if (StringUtils.isBlank(orderId)) {
            throw new AppException(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode(), "orderId不能为空");
        }
        RefundTypeVO refundType = resolveRefundType(refundTaskEntity);
        if (null == refundTaskRepository) {
            changeOrderRefundSuccess(orderId, refundType);
            return true;
        }

        int lockCount = refundTaskRepository.lockRefundTask(orderId);
        if (1 != lockCount) {
            return false;
        }

        try {
            if (null == refundType) {
                throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "退单类型缺失或非法");
            }
            changeOrderRefundSuccess(orderId, refundType);
            refundTaskRepository.markRefundTaskSuccess(orderId);
            return true;
        } catch (AppException e) {
            if (isPermanentRefundTaskException(e)) {
                refundTaskRepository.markRefundTaskFailed(orderId, e.getInfo());
                log.warn("拼团退单成功消息无法在支付商城落地，标记退款任务永久失败 orderId:{} code:{} info:{}", orderId, e.getCode(), e.getInfo());
            } else {
                refundTaskRepository.markRefundTaskRetry(orderId, e.getInfo());
                log.warn("拼团退单成功消息暂未完成处理，标记退款任务重试 orderId:{} code:{} info:{}", orderId, e.getCode(), e.getInfo());
            }
            return false;
        } catch (Exception e) {
            refundTaskRepository.markRefundTaskRetry(orderId, e.getMessage());
            log.warn("拼团退单成功消息处理异常，标记退款任务重试 orderId:{}", orderId, e);
            return false;
        }
    }

    @Override
    public List<RefundTaskEntity> queryPendingRefundTaskList(Integer pageSize) {
        int limit = null == pageSize ? DEFAULT_REFUND_TASK_PAGE_SIZE : pageSize;
        if (limit <= 0) {
            throw new AppException(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode(), "pageSize必须大于0");
        }
        limit = Math.min(limit, MAX_REFUND_TASK_PAGE_SIZE);
        return refundTaskRepository.queryPendingRefundTaskList(limit);
    }

    private RefundTypeVO resolveRefundType(RefundTaskEntity refundTaskEntity) {
        if (null != refundTaskEntity.getRefundType()) {
            return refundTaskEntity.getRefundType();
        }
        if (StringUtils.isBlank(refundTaskEntity.getMessage())) {
            return null;
        }
        try {
            com.alibaba.fastjson2.JSONObject messageJson = JSON.parseObject(refundTaskEntity.getMessage());
            return RefundTypeVO.of(messageJson.getString("type"));
        } catch (Exception e) {
            log.warn("解析退款任务退单类型失败 orderId:{} message:{}", refundTaskEntity.getOrderId(), refundTaskEntity.getMessage(), e);
            return null;
        }
    }

    private boolean needPayRefund(OrderStatusVO orderStatusVO) {
        return null != orderStatusVO && !OrderStatusVO.PAY_WAIT.equals(orderStatusVO);
    }

    private OrderEntity queryRefundingOrRefundedOrder(String orderId, String errorInfo) {
        for (int retry = 0; retry <= REFUND_STATUS_QUERY_RETRY_TIMES; retry++) {
            OrderEntity latestOrderEntity = repository.queryOrderByOrderId(orderId);
            if (null == latestOrderEntity) {
                throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "订单不存在");
            }

            OrderStatusVO latestStatus = latestOrderEntity.getOrderStatusVO();
            if (null != latestStatus
                    && (OrderStatusVO.isRefunding(latestStatus.getCode()) || OrderStatusVO.isRefunded(latestStatus.getCode()))) {
                return latestOrderEntity;
            }

            if (retry < REFUND_STATUS_QUERY_RETRY_TIMES) {
                sleepBeforeRetry(orderId);
            }
        }

        throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), errorInfo);
    }

    private void sleepBeforeRetry(String orderId) {
        try {
            Thread.sleep(REFUND_STATUS_QUERY_RETRY_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "等待退款状态提交被中断 orderId:" + orderId);
        }
    }

    private boolean isPermanentRefundTaskException(AppException e) {
        String info = e.getInfo();
        return "订单不存在".equals(info)
                || "当前订单状态不允许完成退款".equals(info)
                || "退单类型缺失或非法".equals(info);
    }
}
