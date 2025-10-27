package top.licodetech.mall.domain.order.service;

import lombok.extern.slf4j.Slf4j;
import top.licodetech.mall.domain.order.adapter.port.IProductPort;
import top.licodetech.mall.domain.order.adapter.repository.IOrderRepository;
import top.licodetech.mall.domain.order.model.aggregate.CreateOrderAggregate;
import top.licodetech.mall.domain.order.model.entity.OrderEntity;
import top.licodetech.mall.domain.order.model.entity.PayOrderEntity;
import top.licodetech.mall.domain.order.model.entity.ProductEntity;
import top.licodetech.mall.domain.order.model.entity.ShopCartEntity;
import top.licodetech.mall.domain.order.model.valobj.OrderStatusVO;

@Slf4j
public abstract class AbstractOrderService implements IOrderService{

    protected final IOrderRepository repository;
    protected final IProductPort port;

    public AbstractOrderService(IOrderRepository repository, IProductPort port) {
        this.repository = repository;
        this.port = port;
    }

    @Override
    public PayOrderEntity createOrder(ShopCartEntity shopCartEntity) throws Exception {

        // 1.查询当前用户是否存在未支付订单或掉单订单
        OrderEntity unpaidOrderEntity = repository.queryUnPayOrder(shopCartEntity);

        if (null != unpaidOrderEntity && OrderStatusVO.PAY_WAIT.equals(unpaidOrderEntity.getOrderStatusVO())) {
            log.info("创建订单-存在，已存在未支付订单。userId:{} productId:{} orderId:{}", shopCartEntity.getUserId(), shopCartEntity.getProductId(), unpaidOrderEntity.getOrderId());
            return PayOrderEntity.builder()
                    .orderId(unpaidOrderEntity.getOrderId())
                    .payUrl(unpaidOrderEntity.getPayUrl())
                    .build();
        } else if (null != unpaidOrderEntity && OrderStatusVO.CREATE.equals(unpaidOrderEntity.getOrderStatusVO())) {
//            log.info("创建订单-存在，存在未创建支付单订单，创建支付单开始 userId:{} productId:{} orderId:{}", shopCartEntity.getUserId(), shopCartEntity.getProductId(), unpaidOrderEntity.getOrderId());
//            PayOrder payOrder = doPrepayOrder(unpaidOrderEntity.getProductId(), unpaidOrderEntity.getProductName(), unpaidOrderEntity.getOrderId(), unpaidOrderEntity.getTotalAmount());
//            return PayOrderEntity.builder()
//                    .orderId(payOrder.getOrderId())
//                    .payUrl(payOrder.getPayUrl())
//                    .build();
        }

        ProductEntity productEntity = port.queryProductByProductId(shopCartEntity.getProductId());

        OrderEntity orderEntity = CreateOrderAggregate.buildOrderEntity(productEntity.getProductId(), productEntity.getProductName());

        CreateOrderAggregate orderAggregate = CreateOrderAggregate.builder()
                .userId(shopCartEntity.getUserId())
                .productEntity(productEntity)
                .orderEntity(orderEntity)
                .build();

        this.doSaveOrder(orderAggregate);

        return PayOrderEntity.builder()
                .orderId(orderEntity.getProductId())
                .payUrl("暂无")
                .build();
    }

    protected abstract void doSaveOrder(CreateOrderAggregate orderAggregate);

}
