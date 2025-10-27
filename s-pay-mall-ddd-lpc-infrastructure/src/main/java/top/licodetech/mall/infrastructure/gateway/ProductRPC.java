package top.licodetech.mall.infrastructure.gateway;

import org.springframework.stereotype.Service;
import top.licodetech.mall.infrastructure.gateway.dto.ProductDTO;

import java.math.BigDecimal;

@Service
public class ProductRPC {

    public ProductDTO queryProductByProductId(String productId) {

        ProductDTO productVO = new ProductDTO();
        productVO.setProductId(productId);
        productVO.setProductName("测试商品");
        productVO.setProductDesc("这是一个测试商品");
        productVO.setPrice(new BigDecimal("1.68"));
        return productVO;

    }

}
