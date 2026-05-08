package top.licodetech.mall.infrastructure.adapter.repository;

import com.alibaba.fastjson.JSON;
import com.google.common.eventbus.EventBus;
import org.springframework.stereotype.Repository;
import top.licodetech.mall.domain.order.adapter.event.PaySuccessMessageEvent;
import top.licodetech.mall.domain.order.adapter.repository.IOrderRepository;
import top.licodetech.mall.domain.order.model.aggregate.CreateOrderAggregate;
import top.licodetech.mall.domain.order.model.entity.OrderEntity;
import top.licodetech.mall.domain.order.model.entity.PayOrderEntity;
import top.licodetech.mall.domain.order.model.entity.ProductEntity;
import top.licodetech.mall.domain.order.model.entity.ShopCartEntity;
import top.licodetech.mall.domain.order.model.valobj.MarketTypeVO;
import top.licodetech.mall.domain.order.model.valobj.OrderStatusVO;
import top.licodetech.mall.infrastructure.dao.IOrderDao;
import top.licodetech.mall.infrastructure.dao.po.PayOrder;
import top.licodetech.mall.infrastructure.event.EventPublisher;
import top.licodetech.mall.types.common.Constants;
import top.licodetech.mall.types.event.BaseEvent;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class OrderRepository implements IOrderRepository {

    @Resource
    private IOrderDao orderDao;

    @Resource
    private PaySuccessMessageEvent paySuccessMessageEvent;

    @Resource
    private EventBus eventBus;

    @Resource
    private EventPublisher eventPublisher;

    @Override
    public OrderEntity queryUnPayOrder(ShopCartEntity shopCartEntity) {

        // 1. 封装参数
        PayOrder orderReq = new PayOrder();
        orderReq.setUserId(shopCartEntity.getUserId());
        orderReq.setProductId(shopCartEntity.getProductId());

        // 2. 查询到订单
        PayOrder order = orderDao.queryUnPayOrder(orderReq);
        if (null == order) {
            return null;
        }

        // 3. 返回结果
        return OrderEntity.builder()
                .id(order.getId())
                .productId(order.getProductId())
                .productName(order.getProductName())
                .orderId(order.getOrderId())
                .orderStatusVO(OrderStatusVO.valueOf(order.getStatus()))
                .orderTime(order.getOrderTime())
                .totalAmount(order.getTotalAmount())
                .payUrl(order.getPayUrl())
                .marketType(order.getMarketType())
                .marketDeductionAmount(order.getMarketDeductionAmount())
                .payAmount(order.getPayAmount())
                .build();

    }

    @Override
    public void doSaveOrder(CreateOrderAggregate orderAggregate) {
//        String userId = orderAggregate.getUserId();
//        ProductEntity productEntity = orderAggregate.getProductEntity();
//        OrderEntity orderEntity = orderAggregate.getOrderEntity();
//
//        PayOrder order = PayOrder.builder()
//                .userId(userId)
//                .productId(productEntity.getProductId())
//                .productName(productEntity.getProductName())
//                .orderId(orderEntity.getOrderId())
//                .totalAmount(productEntity.getPrice())
//                .orderTime(orderEntity.getOrderTime())
//                .status(orderEntity.getOrderStatusVO().getCode())
//                .build();
//
//        orderDao.insert(order);

        String userId = orderAggregate.getUserId();
        ProductEntity productEntity = orderAggregate.getProductEntity();
        OrderEntity orderEntity = orderAggregate.getOrderEntity();

        PayOrder order = new PayOrder();
        order.setUserId(userId);
        order.setProductId(productEntity.getProductId());
        order.setProductName(productEntity.getProductName());
        order.setOrderId(orderEntity.getOrderId());
        order.setOrderTime(orderEntity.getOrderTime());
        order.setTotalAmount(productEntity.getPrice());
        order.setStatus(orderEntity.getOrderStatusVO().getCode());
        order.setMarketType(MarketTypeVO.NO_MARKET.getCode());
        order.setMarketDeductionAmount(BigDecimal.ZERO);
        order.setPayAmount(productEntity.getPrice());
        order.setMarketType(orderEntity.getMarketType());

        orderDao.insert(order);

    }

    @Override
    public void updataOrderPayInfo(PayOrderEntity payOrderEntity) {
        PayOrder payOrderReq = PayOrder.builder()
                .userId(payOrderEntity.getUserId())
                .orderId(payOrderEntity.getOrderId())
                .status(payOrderEntity.getOrderStatus().getCode())
                .payUrl(payOrderEntity.getPayUrl())
                .marketType(payOrderEntity.getMarketType())
                .marketDeductionAmount(payOrderEntity.getMarketDeductionAmount())
                .payAmount(payOrderEntity.getPayAmount())
                .build();
        orderDao.updateOrderPayInfo(payOrderReq);

    }

    @Override
    public void changeOrderPaySuccess(String orderId, Date payTime) {
        PayOrder payOrderReq = new PayOrder();
        payOrderReq.setOrderId(orderId);
        payOrderReq.setStatus(OrderStatusVO.PAY_SUCCESS.getCode());
        payOrderReq.setPayTime(payTime);
        orderDao.changeOrderPaySuccess(payOrderReq);

        /// 不走拼团营销的直接结算发货
        BaseEvent.EventMessage<PaySuccessMessageEvent.PaySuccessMessage> paySuccessMessageEventMessage = paySuccessMessageEvent.buildEventMessage(
                PaySuccessMessageEvent.PaySuccessMessage.builder()
                        .tradeNo(orderId)
                        .build());
        PaySuccessMessageEvent.PaySuccessMessage paySuccessMessage = paySuccessMessageEventMessage.getData();

        // 旧版发送消息方式
        // eventBus.post(JSON.toJSONString(paySuccessMessage));
        eventPublisher.publish(paySuccessMessageEvent.topic(), JSON.toJSONString(paySuccessMessage));
    }

    @Override
    public void changeMarketOrderPaySuccess(String orderId) {
        PayOrder payOrderReq = new PayOrder();
        payOrderReq.setOrderId(orderId);
        payOrderReq.setStatus(OrderStatusVO.PAY_SUCCESS.getCode());
        orderDao.changeOrderPaySuccess(payOrderReq);
    }

    @Override
    public List<String> queryNoPayNotifyOrder() {
        return orderDao.queryNoPayNotifyOrder();
    }

    @Override
    public List<String> queryTimeoutCloseOrderList() {
        return orderDao.queryTimeoutCloseOrderList();
    }

    @Override
    public boolean changeOrderClose(String orderId) {
        return orderDao.changeOrderClose(orderId);
    }

    @Override
    public void changeOrderMarketSettlement(List<String> outTradeNoList) {
        if (null == outTradeNoList || outTradeNoList.isEmpty()) {
            return;
        }

        outTradeNoList.forEach(outTradeNo -> {
            int updateCount = orderDao.changeOrderMarketSettlement(outTradeNo);
            if (1 != updateCount) {
                return;
            }

            // 只对本次从 PAY_SUCCESS 推进到 MARKET 的订单发送发货消息，重复消费不重复发货。
            BaseEvent.EventMessage<PaySuccessMessageEvent.PaySuccessMessage> paySuccessMessageEventMessage = paySuccessMessageEvent.buildEventMessage(
                    PaySuccessMessageEvent.PaySuccessMessage.builder()
                            .tradeNo(outTradeNo)
                            .build());
            PaySuccessMessageEvent.PaySuccessMessage paySuccessMessage = paySuccessMessageEventMessage.getData();
            // 旧版发送消息方式
            // eventBus.post(JSON.toJSONString(paySuccessMessage));
            eventPublisher.publish(paySuccessMessageEvent.topic(), JSON.toJSONString(paySuccessMessage));
        });
    }

    @Override
    public OrderEntity queryOrderByOrderId(String orderId) {
        PayOrder payOrder = orderDao.queryOrderByOrderId(orderId);
        if (null == payOrder) {
            return null;
        }

        return buildOrderEntity(payOrder);
    }

    @Override
    public List<OrderEntity> queryUserOrderList(String userId, Long lastId, Integer pageSize) {
        List<PayOrder> payOrderList = orderDao.queryUserOrderList(userId, lastId, pageSize);
        if (null == payOrderList || payOrderList.isEmpty()) {
            return Collections.emptyList();
        }

        return payOrderList.stream()
                .map(this::buildOrderEntity)
                .collect(Collectors.toList());
    }

    @Override
    public int refundOrder(String userId, String orderId) {
        return orderDao.refundOrder(userId, orderId);
    }

    @Override
    public int changeOrderRefunding(String userId, String orderId) {
        return orderDao.changeOrderRefunding(userId, orderId);
    }

    @Override
    public int changeOrderRefunded(String orderId) {
        return orderDao.changeOrderRefunded(orderId);
    }

    private OrderEntity buildOrderEntity(PayOrder payOrder) {
        return OrderEntity.builder()
                .id(payOrder.getId())
                .userId(payOrder.getUserId())
                .productId(payOrder.getProductId())
                .productName(payOrder.getProductName())
                .orderId(payOrder.getOrderId())
                .orderTime(payOrder.getOrderTime())
                .totalAmount(payOrder.getTotalAmount())
                .orderStatusVO(OrderStatusVO.valueOf(payOrder.getStatus()))
                .payUrl(payOrder.getPayUrl())
                .marketType(payOrder.getMarketType())
                .marketDeductionAmount(payOrder.getMarketDeductionAmount())
                .payAmount(payOrder.getPayAmount())
                .build();
    }
}
