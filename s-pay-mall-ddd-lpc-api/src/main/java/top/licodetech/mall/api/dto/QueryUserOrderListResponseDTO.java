package top.licodetech.mall.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryUserOrderListResponseDTO {

    // 订单列表
    private List<UserOrderItemDTO> orderList;
    // 是否还有下一页
    private Boolean hasMore;
    // 当前页最后一条订单ID
    private Long lastId;

}
