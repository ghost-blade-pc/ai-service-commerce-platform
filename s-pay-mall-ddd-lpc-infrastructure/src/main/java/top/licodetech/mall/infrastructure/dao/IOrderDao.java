package top.licodetech.mall.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
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

    void changeOrderMarketSettlement(List<String> outTradeNoList);

    PayOrder queryOrderByOrderId(String orderId);

}
