package top.licodetech.mall.infrastructure.adapter.port;

import org.springframework.stereotype.Component;
import top.licodetech.mall.domain.order.adapter.port.IProductPort;
import top.licodetech.mall.domain.order.model.entity.ProductEntity;
import top.licodetech.mall.infrastructure.gateway.ProductRPC;
import top.licodetech.mall.infrastructure.gateway.dto.ProductDTO;

@Component
public class ProductPort implements IProductPort {

    private final ProductRPC productRPC;

    public ProductPort(ProductRPC productRPC) {
        this.productRPC = productRPC;
    }

    @Override
    public ProductEntity queryProductByProductId(String productId) {
        ProductDTO productDTO = productRPC.queryProductByProductId(productId);

        return ProductEntity.builder()
                .productId(productDTO.getProductId())
                .productName(productDTO.getProductName())
                .productDesc(productDTO.getProductDesc())
                .price(productDTO.getPrice())
                .build();

    }
}
