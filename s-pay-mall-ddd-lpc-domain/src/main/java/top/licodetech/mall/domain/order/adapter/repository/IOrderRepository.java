package top.licodetech.mall.domain.order.adapter.repository;

import top.licodetech.mall.domain.order.model.aggregate.CreateOrderAggregate;
import top.licodetech.mall.domain.order.model.entity.OrderEntity;
import top.licodetech.mall.domain.order.model.entity.PayOrderEntity;
import top.licodetech.mall.domain.order.model.entity.ShopCartEntity;

public interface IOrderRepository {
    OrderEntity queryUnPayOrder(ShopCartEntity shopCartEntity);

    void doSaveOrder(CreateOrderAggregate orderAggregate);

    void updataOrderPayInfo(PayOrderEntity payOrderEntity);
}
