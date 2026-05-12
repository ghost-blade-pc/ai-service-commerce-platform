package top.licodetech.mall.domain.order.adapter.port;

import top.licodetech.mall.domain.order.model.entity.MarketPayDiscountEntity;
import top.licodetech.mall.domain.order.model.entity.ProductEntity;

import java.util.Date;

/**
 * @author LiPC
 */
public interface IProductPort {
    ProductEntity queryProductByProductId(String productId);

    ProductEntity queryProductByProductId(String userId, String productId);

    MarketPayDiscountEntity lockMarketPayOrder(String userId, String teamId, Long activityId, String productId, String orderId);

    void settlementMarketPayOrder(String userId, String orderId, Date payTime);

    boolean refundMarketPayOrder(String userId, String orderId);
}
