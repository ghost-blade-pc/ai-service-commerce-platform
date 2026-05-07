package top.licodetech.mall.domain.order.adapter.port;

import java.math.BigDecimal;

public interface IRefundPort {

    boolean refund(String orderId, BigDecimal refundAmount);

}
