package top.licodetech.mall.infrastructure.gateway;

import org.springframework.stereotype.Service;
import top.licodetech.mall.infrastructure.gateway.dto.ProductDTO;

import java.math.BigDecimal;

@Service
public class ProductRPC {

    public ProductDTO queryProductByProductId(String productId) {

        ProductDTO productVO = new ProductDTO();
        productVO.setProductId(productId);
        productVO.setProductName("MyBatisBook");
        productVO.setProductDesc("MyBatisBook");
        productVO.setPrice(new BigDecimal("100.00"));
        return productVO;

    }

}
