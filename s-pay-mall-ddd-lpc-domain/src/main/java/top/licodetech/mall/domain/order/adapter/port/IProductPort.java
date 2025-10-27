package top.licodetech.mall.domain.order.adapter.port;

import top.licodetech.mall.domain.order.model.entity.ProductEntity;

public interface IProductPort {
    ProductEntity queryProductByProductId(String productId);

}
