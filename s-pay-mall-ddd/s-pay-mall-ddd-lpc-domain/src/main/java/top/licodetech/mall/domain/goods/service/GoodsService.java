package top.licodetech.mall.domain.goods.service;

import org.springframework.stereotype.Service;
import top.licodetech.mall.domain.subscription.service.ISubscriptionService;

import javax.annotation.Resource;

/**
 * @author LiPC
 * @description
 * @create 2026-03-22 16:04
 */
@Service
public class GoodsService implements IGoodsService {

    @Resource
    private ISubscriptionService subscriptionService;

    @Override
    public void changeOrderDealDone(String orderId) {
        subscriptionService.fulfillOrder(orderId);
    }
}
