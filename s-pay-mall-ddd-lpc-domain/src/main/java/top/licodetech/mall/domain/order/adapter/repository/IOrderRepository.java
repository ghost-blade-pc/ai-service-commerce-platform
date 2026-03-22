package top.licodetech.mall.domain.order.adapter.repository;

import top.licodetech.mall.domain.order.model.aggregate.CreateOrderAggregate;
import top.licodetech.mall.domain.order.model.entity.OrderEntity;
import top.licodetech.mall.domain.order.model.entity.PayOrderEntity;
import top.licodetech.mall.domain.order.model.entity.ShopCartEntity;

import java.util.Date;
import java.util.List;

public interface IOrderRepository {
    OrderEntity queryUnPayOrder(ShopCartEntity shopCartEntity);

    void doSaveOrder(CreateOrderAggregate orderAggregate);

    void updataOrderPayInfo(PayOrderEntity payOrderEntity);

    void changeOrderPaySuccess(String orderId, Date payTime);

    void changeMarketOrderPaySuccess(String orderId);

    List<String> queryNoPayNotifyOrder();

    List<String> queryTimeoutCloseOrderList();

    boolean changeOrderClose(String orderId);

    void changeOrderMarketSettlement(List<String> outTradeNoList);

    OrderEntity queryOrderByOrderId(String orderId);


}
