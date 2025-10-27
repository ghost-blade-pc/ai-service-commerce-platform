package top.licodetech.mall.domain.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.licodetech.mall.domain.order.adapter.port.IProductPort;
import top.licodetech.mall.domain.order.adapter.repository.IOrderRepository;
import top.licodetech.mall.domain.order.model.aggregate.CreateOrderAggregate;

@Slf4j
@Service
public class OrderService extends AbstractOrderService{


    public OrderService(IOrderRepository repository, IProductPort port) {
        super(repository, port);
    }

    @Override
    protected void doSaveOrder(CreateOrderAggregate orderAggregate) {
        repository.doSaveOrder(orderAggregate);
    }
}
