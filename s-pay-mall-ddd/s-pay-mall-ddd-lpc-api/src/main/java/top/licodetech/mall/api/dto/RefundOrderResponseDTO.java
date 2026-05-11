package top.licodetech.mall.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundOrderResponseDTO {

    // 订单ID
    private String orderId;
    // 最新订单状态
    private String status;
    // 最新订单状态描述
    private String statusDesc;

}
