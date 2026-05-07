package top.licodetech.mall.domain.order.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatusVO {

    CREATE("CREATE", "创建完成 - 如果调单了，也会从创建记录重新发起创建支付单"),
    PAY_WAIT("PAY_WAIT", "等待支付 - 订单创建完成后，创建支付单"),
    PAY_SUCCESS("PAY_SUCCESS", "支付成功 - 接收到支付回调消息"),
    DEAL_DONE("DEAL_DONE", "交易完成 - 商品发货完成"),
    CLOSE("CLOSE", "超时关单 - 超市未支付"),
    MARKET("MARKET", "营销结算 - 拼团组队完成"),
    REFUNDING("REFUNDING", "退单中"),
    REFUNDED("REFUNDED", "已退单"),
            ;

    private final String code;
    private final String desc;

    public static boolean canRefund(String code) {
        return PAY_WAIT.code.equals(code) || PAY_SUCCESS.code.equals(code);
    }

}
