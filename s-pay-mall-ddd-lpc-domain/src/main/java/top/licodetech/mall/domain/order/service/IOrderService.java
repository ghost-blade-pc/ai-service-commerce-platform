package top.licodetech.mall.domain.order.service;

import top.licodetech.mall.domain.order.model.entity.PayOrderEntity;
import top.licodetech.mall.domain.order.model.entity.ShopCartEntity;

public interface IOrderService {

    PayOrderEntity createOrder(ShopCartEntity shopCartEntity) throws Exception;

}
