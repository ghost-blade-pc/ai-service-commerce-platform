package top.licodetech.mall.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOrderItemDTO {

    // 游标ID
    private Long id;
    // 订单ID
    private String orderId;
    // 商品名称
    private String productName;
    // 订单金额
    private BigDecimal totalAmount;
    // 支付金额
    private BigDecimal payAmount;
    // 订单状态
    private String status;
    // 订单状态描述
    private String statusDesc;
    // 下单时间
    private String createTime;
    // 是否允许退单
    private Boolean canRefund;

}
