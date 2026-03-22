package top.licodetech.mall.domain.goods.service;

import org.springframework.stereotype.Service;
import top.licodetech.mall.domain.goods.adapter.repository.IGoodsRepository;

import javax.annotation.Resource;

/**
 * @author LiPC
 * @description
 * @create 2026-03-22 16:04
 */
@Service
public class GoodsService implements IGoodsService {

    @Resource
    private IGoodsRepository repository;

    @Override
    public void changeOrderDealDone(String orderId) {
        repository.changeOrderDealDone(orderId);
    }
}
