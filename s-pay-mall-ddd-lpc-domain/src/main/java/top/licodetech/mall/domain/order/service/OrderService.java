package top.licodetech.mall.domain.order.service;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.licodetech.mall.domain.order.adapter.port.IProductPort;
import top.licodetech.mall.domain.order.adapter.repository.IOrderRepository;
import top.licodetech.mall.domain.order.model.aggregate.CreateOrderAggregate;
import top.licodetech.mall.domain.order.model.entity.PayOrderEntity;
import top.licodetech.mall.domain.order.model.valobj.OrderStatusVO;
import top.licodetech.mall.types.common.Constants;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Slf4j
@Service
public class OrderService extends AbstractOrderService{

    @Value("${alipay.notify_url}")
    private String notifyUrl;
    @Value("${alipay.return_url}")
    private String returnUrl;

    @Resource
    private AlipayClient alipayClient;

    public OrderService(IOrderRepository repository, IProductPort port) {
        super(repository, port);
    }

    @Override
    protected void doSaveOrder(CreateOrderAggregate orderAggregate) {
        repository.doSaveOrder(orderAggregate);
    }

    @Override
    protected PayOrderEntity doPrepayOrder(String userId, String productId, String productName, String orderId, BigDecimal totalAmount) throws AlipayApiException {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(notifyUrl);
        request.setReturnUrl(returnUrl);

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trand_no", orderId);
        bizContent.put("total_amount", totalAmount.toString());
        bizContent.put("subject", productName);
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        request.setBizContent(bizContent.toString());

        String form = alipayClient.pageExecute(request).getBody();

        PayOrderEntity payOrderEntity = new PayOrderEntity();
        payOrderEntity.setOrderId(orderId);
        payOrderEntity.setPayUrl(form);
        payOrderEntity.setOrderStatus(OrderStatusVO.PAY_WAIT);

        repository.updataOrderPayInfo(payOrderEntity);

        return payOrderEntity;
    }
}
