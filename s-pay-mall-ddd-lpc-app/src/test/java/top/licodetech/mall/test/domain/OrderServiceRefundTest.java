package top.licodetech.mall.test.domain;

import org.junit.Test;
import top.licodetech.mall.domain.order.adapter.port.IProductPort;
import top.licodetech.mall.domain.order.adapter.repository.IOrderRepository;
import top.licodetech.mall.domain.order.model.entity.OrderEntity;
import top.licodetech.mall.domain.order.model.valobj.MarketTypeVO;
import top.licodetech.mall.domain.order.model.valobj.OrderStatusVO;
import top.licodetech.mall.domain.order.service.OrderService;
import top.licodetech.mall.types.exception.AppException;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrderServiceRefundTest {

    @Test
    public void test_refundOrder_noMarket_payWait_success() {
        IOrderRepository repository = mock(IOrderRepository.class);
        IProductPort productPort = mock(IProductPort.class);
        OrderService orderService = new OrderService(repository, productPort);

        when(repository.queryOrderByOrderId("100001")).thenReturn(OrderEntity.builder()
                        .userId("xiaofuge")
                        .orderId("100001")
                        .orderStatusVO(OrderStatusVO.PAY_WAIT)
                        .marketType(MarketTypeVO.NO_MARKET.getCode())
                        .payAmount(BigDecimal.TEN)
                        .build(),
                OrderEntity.builder()
                        .userId("xiaofuge")
                        .orderId("100001")
                        .orderStatusVO(OrderStatusVO.REFUNDING)
                        .marketType(MarketTypeVO.NO_MARKET.getCode())
                        .payAmount(BigDecimal.TEN)
                        .build());
        when(repository.changeOrderRefunding("xiaofuge", "100001")).thenReturn(1);
        when(repository.changeOrderRefunded("100001")).thenReturn(1);

        OrderEntity orderEntity = orderService.refundOrder("xiaofuge", "100001");

        assertEquals(OrderStatusVO.REFUNDED, orderEntity.getOrderStatusVO());
        verify(repository).changeOrderRefunding("xiaofuge", "100001");
        verify(repository).changeOrderRefunded("100001");
    }

    @Test
    public void test_refundOrder_groupBuy_paySuccess_refunding() {
        IOrderRepository repository = mock(IOrderRepository.class);
        IProductPort productPort = mock(IProductPort.class);
        OrderService orderService = new OrderService(repository, productPort);

        when(repository.queryOrderByOrderId("100001")).thenReturn(OrderEntity.builder()
                .userId("xiaofuge")
                .orderId("100001")
                .orderStatusVO(OrderStatusVO.PAY_SUCCESS)
                .marketType(MarketTypeVO.GROUP_BUY_MARKET.getCode())
                .payAmount(BigDecimal.TEN)
                .build());
        when(productPort.refundMarketPayOrder("xiaofuge", "100001")).thenReturn(true);
        when(repository.changeOrderRefunding("xiaofuge", "100001")).thenReturn(1);

        OrderEntity orderEntity = orderService.refundOrder("xiaofuge", "100001");

        assertEquals(OrderStatusVO.REFUNDING, orderEntity.getOrderStatusVO());
        verify(productPort).refundMarketPayOrder("xiaofuge", "100001");
        verify(repository).changeOrderRefunding("xiaofuge", "100001");
    }

    @Test
    public void test_changeOrderRefundSuccess_refunding_success() {
        IOrderRepository repository = mock(IOrderRepository.class);
        IProductPort productPort = mock(IProductPort.class);
        OrderService orderService = new OrderService(repository, productPort);

        when(repository.queryOrderByOrderId("100001")).thenReturn(OrderEntity.builder()
                .userId("xiaofuge")
                .orderId("100001")
                .orderStatusVO(OrderStatusVO.REFUNDING)
                .payAmount(BigDecimal.TEN)
                .build());
        when(repository.changeOrderRefunded("100001")).thenReturn(1);

        OrderEntity orderEntity = orderService.changeOrderRefundSuccess("100001");

        assertEquals(OrderStatusVO.REFUNDED, orderEntity.getOrderStatusVO());
        verify(repository).changeOrderRefunded("100001");
    }

    @Test
    public void test_changeOrderRefundSuccess_refunded_repeat() {
        IOrderRepository repository = mock(IOrderRepository.class);
        IProductPort productPort = mock(IProductPort.class);
        OrderService orderService = new OrderService(repository, productPort);

        when(repository.queryOrderByOrderId("100001")).thenReturn(OrderEntity.builder()
                .userId("xiaofuge")
                .orderId("100001")
                .orderStatusVO(OrderStatusVO.REFUNDED)
                .payAmount(BigDecimal.TEN)
                .build());

        OrderEntity orderEntity = orderService.changeOrderRefundSuccess("100001");

        assertEquals(OrderStatusVO.REFUNDED, orderEntity.getOrderStatusVO());
    }

    @Test
    public void test_refundOrder_notOwner_fail() {
        IOrderRepository repository = mock(IOrderRepository.class);
        IProductPort productPort = mock(IProductPort.class);
        OrderService orderService = new OrderService(repository, productPort);

        when(repository.queryOrderByOrderId("100001")).thenReturn(OrderEntity.builder()
                .userId("other")
                .orderId("100001")
                .orderStatusVO(OrderStatusVO.PAY_SUCCESS)
                .build());

        try {
            orderService.refundOrder("xiaofuge", "100001");
            fail("未抛出订单归属异常");
        } catch (AppException e) {
            assertEquals("订单不属于当前用户", e.getInfo());
        }
    }

    @Test
    public void test_refundOrder_closeStatus_fail() {
        IOrderRepository repository = mock(IOrderRepository.class);
        IProductPort productPort = mock(IProductPort.class);
        OrderService orderService = new OrderService(repository, productPort);

        when(repository.queryOrderByOrderId("100001")).thenReturn(OrderEntity.builder()
                .userId("xiaofuge")
                .orderId("100001")
                .orderStatusVO(OrderStatusVO.CLOSE)
                .build());

        try {
            orderService.refundOrder("xiaofuge", "100001");
            fail("未抛出状态不允许异常");
        } catch (AppException e) {
            assertEquals("当前订单状态不允许退单", e.getInfo());
        }
    }

    @Test
    public void test_queryUserOrderList_limitPageSize() {
        IOrderRepository repository = mock(IOrderRepository.class);
        IProductPort productPort = mock(IProductPort.class);
        OrderService orderService = new OrderService(repository, productPort);

        when(repository.queryUserOrderList("xiaofuge", null, 51)).thenReturn(Collections.emptyList());

        orderService.queryUserOrderList("xiaofuge", null, 100);

        verify(repository).queryUserOrderList("xiaofuge", null, 51);
    }

}
