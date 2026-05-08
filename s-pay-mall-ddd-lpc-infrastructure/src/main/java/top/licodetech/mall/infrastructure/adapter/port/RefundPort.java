package top.licodetech.mall.infrastructure.adapter.port;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.licodetech.mall.domain.order.adapter.port.IRefundPort;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Slf4j
@Component
public class RefundPort implements IRefundPort {

    @Resource
    private AlipayClient alipayClient;

    @Override
    public boolean refund(String orderId, BigDecimal refundAmount) {
        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            AlipayTradeRefundModel refundModel = new AlipayTradeRefundModel();
            refundModel.setOutTradeNo(orderId);
            refundModel.setRefundAmount(refundAmount.toString());
            refundModel.setRefundReason("交易退单");
            request.setBizModel(refundModel);

            AlipayTradeRefundResponse response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                log.warn("支付宝退款失败 orderId:{} refundAmount:{} code:{} msg:{} subMsg:{}",
                        orderId, refundAmount, response.getCode(), response.getMsg(), response.getSubMsg());
                return false;
            }
            log.info("支付宝退款成功 orderId:{} refundAmount:{}", orderId, refundAmount);
            return true;
        } catch (AlipayApiException e) {
            log.error("支付宝退款异常 orderId:{} refundAmount:{}", orderId, refundAmount, e);
            return false;
        }
    }

}
