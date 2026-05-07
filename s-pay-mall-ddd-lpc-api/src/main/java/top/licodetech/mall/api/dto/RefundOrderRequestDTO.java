package top.licodetech.mall.api.dto;

import lombok.Data;

@Data
public class RefundOrderRequestDTO {

    // 用户ID
    private String userId;
    // 订单ID
    private String orderId;

}
