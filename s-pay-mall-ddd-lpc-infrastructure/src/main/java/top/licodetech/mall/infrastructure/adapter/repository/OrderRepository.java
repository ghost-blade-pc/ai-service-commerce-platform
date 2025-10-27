package top.licodetech.mall.infrastructure.adapter.repository;

import org.springframework.stereotype.Repository;
import top.licodetech.mall.domain.order.adapter.repository.IOrderRepository;
import top.licodetech.mall.domain.order.model.aggregate.CreateOrderAggregate;
import top.licodetech.mall.domain.order.model.entity.OrderEntity;
import top.licodetech.mall.domain.order.model.entity.ProductEntity;
import top.licodetech.mall.domain.order.model.entity.ShopCartEntity;
import top.licodetech.mall.domain.order.model.valobj.OrderStatusVO;
import top.licodetech.mall.infrastructure.dao.IOrderDao;
import top.licodetech.mall.infrastructure.dao.po.PayOrder;

import javax.annotation.Resource;

@Repository
public class OrderRepository implements IOrderRepository {

    @Resource
    private IOrderDao orderDao;

    @Override
    public OrderEntity queryUnPayOrder(ShopCartEntity shopCartEntity) {

        // 1. 封装参数
        PayOrder orderReq = new PayOrder();
        orderReq.setUserId(shopCartEntity.getUserId());
        orderReq.setProductId(shopCartEntity.getProductId());

        // 2. 查询到订单
        PayOrder order = orderDao.queryUnPayOrder(orderReq);
        if (null == order) return null;

        // 3. 返回结果
        return OrderEntity.builder()
                .productId(order.getProductId())
                .productName(order.getProductName())
                .orderId(order.getOrderId())
                .orderStatusVO(OrderStatusVO.valueOf(order.getStatus()))
                .orderTime(order.getOrderTime())
                .totalAmount(order.getTotalAmount())
                .payUrl(order.getPayUrl())
                .build();

    }

    @Override
    public void doSaveOrder(CreateOrderAggregate orderAggregate) {
//        String userId = orderAggregate.getUserId();
//        ProductEntity productEntity = orderAggregate.getProductEntity();
//        OrderEntity orderEntity = orderAggregate.getOrderEntity();
//
//        PayOrder order = PayOrder.builder()
//                .userId(userId)
//                .productId(productEntity.getProductId())
//                .productName(productEntity.getProductName())
//                .orderId(orderEntity.getOrderId())
//                .totalAmount(productEntity.getPrice())
//                .orderTime(orderEntity.getOrderTime())
//                .status(orderEntity.getOrderStatusVO().getCode())
//                .build();
//
//        orderDao.insert(order);

        String userId = orderAggregate.getUserId();
        ProductEntity productEntity = orderAggregate.getProductEntity();
        OrderEntity orderEntity = orderAggregate.getOrderEntity();

        PayOrder order = new PayOrder();
        order.setUserId(userId);
        order.setProductId(productEntity.getProductId());
        order.setProductName(productEntity.getProductName());
        order.setOrderId(orderEntity.getOrderId());
        order.setOrderTime(orderEntity.getOrderTime());
        order.setTotalAmount(productEntity.getPrice());
        order.setStatus(orderEntity.getOrderStatusVO().getCode());

        orderDao.insert(order);

    }
}
