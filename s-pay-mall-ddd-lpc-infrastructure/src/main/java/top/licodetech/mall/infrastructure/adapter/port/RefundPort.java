package top.licodetech.mall.infrastructure.adapter.port;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.licodetech.mall.domain.order.adapter.port.IRefundPort;

import java.math.BigDecimal;

@Slf4j
@Component
public class RefundPort implements IRefundPort {

    @Override
    public boolean refund(String orderId, BigDecimal refundAmount) {
        log.info("模拟支付宝退款成功 orderId:{} refundAmount:{}", orderId, refundAmount);
        return true;
    }

}
