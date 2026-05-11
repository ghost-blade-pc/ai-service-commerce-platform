package top.licodetech.mall.domain.order.service;

import top.licodetech.mall.domain.order.model.entity.PayOrderEntity;
import top.licodetech.mall.domain.order.model.entity.RefundTaskEntity;
import top.licodetech.mall.domain.order.model.entity.ShopCartEntity;
import top.licodetech.mall.domain.order.model.entity.OrderEntity;
import top.licodetech.mall.domain.order.model.valobj.RefundTypeVO;

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

    boolean receiveRefundSuccessMessage(String orderId, RefundTypeVO refundType, String message);

    boolean processRefundTask(String orderId);

    boolean processRefundTask(RefundTaskEntity refundTaskEntity);

    List<RefundTaskEntity> queryPendingRefundTaskList(Integer pageSize);
}
