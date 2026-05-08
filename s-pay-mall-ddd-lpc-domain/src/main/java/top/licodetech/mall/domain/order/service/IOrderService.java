package top.licodetech.mall.domain.order.service;

import top.licodetech.mall.domain.order.model.entity.PayOrderEntity;
import top.licodetech.mall.domain.order.model.entity.ShopCartEntity;
import top.licodetech.mall.domain.order.model.entity.OrderEntity;

import java.util.Date;
import java.util.List;

public interface IOrderService {

    PayOrderEntity createOrder(ShopCartEntity shopCartEntity) throws Exception;


    void changeOrderPaySuccess(String orderId, Date payTime);

    void changeOrderMarketSettlement(List<String> outTradeNoList);

    List<String> queryNoPayNotifyOrder();

    List<String> queryTimeoutCloseOrderList();

    boolean changeOrderClose(String orderId);

    List<OrderEntity> queryUserOrderList(String userId, Long lastId, Integer pageSize);

    OrderEntity refundOrder(String userId, String orderId);

    OrderEntity changeOrderRefundSuccess(String orderId);

    boolean receiveRefundSuccessMessage(String orderId, String message);

    boolean processRefundTask(String orderId);

    List<String> queryPendingRefundTaskOrderList(Integer pageSize);
}
