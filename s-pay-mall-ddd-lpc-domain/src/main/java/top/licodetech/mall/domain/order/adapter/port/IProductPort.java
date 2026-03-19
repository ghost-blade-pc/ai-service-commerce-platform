package top.licodetech.mall.domain.order.adapter.port;

import top.licodetech.mall.domain.order.model.entity.MarketPayDiscountEntity;
import top.licodetech.mall.domain.order.model.entity.ProductEntity;

public interface IProductPort {
    ProductEntity queryProductByProductId(String productId);

    MarketPayDiscountEntity lockMarketPayOrder(String userId, String teamId, Long activityId, String productId, String orderId);

}
