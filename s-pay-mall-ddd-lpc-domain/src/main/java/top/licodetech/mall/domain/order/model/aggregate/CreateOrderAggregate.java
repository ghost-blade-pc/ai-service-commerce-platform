package top.licodetech.mall.domain.order.model.aggregate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import top.licodetech.mall.domain.order.model.entity.OrderEntity;
import top.licodetech.mall.domain.order.model.entity.ProductEntity;
import top.licodetech.mall.domain.order.model.valobj.OrderStatusVO;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderAggregate {

    private String userId;

    private ProductEntity productEntity;

    private OrderEntity orderEntity;

    public static OrderEntity buildOrderEntity(String productId, String productName) {
        return OrderEntity.builder()
                .productId(productId)
                .productName(productName)
                .orderId(RandomStringUtils.randomNumeric(16))
                .orderTime(new Date())
                .orderStatusVO(OrderStatusVO.CREATE)
                .build();
    }


}
