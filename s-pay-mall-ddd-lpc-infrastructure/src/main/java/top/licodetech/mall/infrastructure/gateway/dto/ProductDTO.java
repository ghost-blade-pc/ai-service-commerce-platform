package top.licodetech.mall.infrastructure.gateway.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDTO {

    private String productId;
    private String productName;
    private String productDesc;
    private BigDecimal price;

}
