package top.licodetech.mall.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.licodetech.mall.infrastructure.dao.po.PayOrder;

import java.util.List;

@Mapper
public interface IOrderDao {

    void insert(PayOrder payOrder);

    PayOrder queryUnPayOrder(PayOrder payOrderReq);

    void updateOrderPayInfo(PayOrder payOrder);

    void changeOrderPaySuccess(PayOrder payOrderReq);

    boolean changeOrderClose(String orderId);

    void changeOrderDealDone(String orderId);

    List<String> queryNoPayNotifyOrder();

    List<String> queryTimeoutCloseOrderList();

    int changeOrderMarketSettlement(@Param("orderId") String orderId);

    PayOrder queryOrderByOrderId(String orderId);

    List<PayOrder> queryUserOrderList(@Param("userId") String userId, @Param("lastId") Long lastId, @Param("pageSize") Integer pageSize);

    int refundOrder(@Param("userId") String userId, @Param("orderId") String orderId);

    int changeOrderRefunding(@Param("userId") String userId, @Param("orderId") String orderId);

    int changeOrderRefunded(@Param("orderId") String orderId);

}
