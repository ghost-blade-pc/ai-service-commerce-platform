package top.licodetech.mall.infrastructure.gateway.dto;

import lombok.Data;

@Data
public class RefundMarketPayOrderResponseDTO {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 组队ID
     */
    private String teamId;

    /**
     * 退单行为状态码
     */
    private String code;

    /**
     * 退单行为状态信息
     */
    private String info;

}
