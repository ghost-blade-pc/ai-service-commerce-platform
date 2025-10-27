package top.licodetech.mall.domain.order.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductEntity {

    private String productId;
    private String productName;
    private String productDesc;
    private BigDecimal price;

}
