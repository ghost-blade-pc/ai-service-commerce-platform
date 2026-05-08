package top.licodetech.mall.infrastructure.adapter.port;

import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.licodetech.mall.domain.order.adapter.port.IRefundPort;
import top.licodetech.mall.domain.order.model.entity.OrderEntity;

import java.math.BigDecimal;

@Slf4j
@Component
public class RefundPort implements IRefundPort {

    @Override
    public boolean refund(String orderId, BigDecimal refundAmount) {
//        // 1. 查询订单信息，验证订单是否存在且属于该用户
//        OrderEntity orderEntity = repository.queryOrderByUserIdAndOrderId(userId, orderId);
//        if (null == orderEntity) {
//            log.warn("退款失败，订单不存在或不属于该用户 userId:{} orderId:{}", userId, orderId);
//            return false;
//        }
//        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
//        AlipayTradeRefundModel refundModel = new AlipayTradeRefundModel();
//        refundModel.setOutTradeNo(orderEntity.getOrderId());
//        refundModel.setRefundAmount(orderEntity.getPayAmount().toString());
//        refundModel.setRefundReason("交易退单");
//        request.setBizModel(refundModel);
//        // 交易退款
//        AlipayTradeRefundResponse execute = alipayClient.execute(request);
//        if (!execute.isSuccess()) return false;
//        // 状态变更
//        repository.refundOrder(userId, orderId);
//        return true;
        log.info("模拟支付宝退款成功 orderId:{} refundAmount:{}", orderId, refundAmount);
        return true;
    }

}
