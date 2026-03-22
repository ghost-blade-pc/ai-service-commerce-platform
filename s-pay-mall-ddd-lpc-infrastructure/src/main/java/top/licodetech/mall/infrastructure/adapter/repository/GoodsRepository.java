package top.licodetech.mall.infrastructure.adapter.repository;

import org.springframework.stereotype.Repository;
import top.licodetech.mall.domain.goods.adapter.repository.IGoodsRepository;
import top.licodetech.mall.infrastructure.dao.IOrderDao;

import javax.annotation.Resource;

/**
 * @author LiPC
 * @description
 * @create 2026-03-22 16:08
 */
@Repository
public class GoodsRepository implements IGoodsRepository {

    @Resource
    private IOrderDao orderDao;

    @Override
    public void changeOrderDealDone(String orderId) {
        orderDao.changeOrderDealDone(orderId);
    }
}
