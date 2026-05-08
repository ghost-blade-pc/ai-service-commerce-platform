package top.licodetech.mall.test.infrastructure;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.licodetech.mall.domain.order.adapter.event.PaySuccessMessageEvent;
import top.licodetech.mall.infrastructure.adapter.repository.OrderRepository;
import top.licodetech.mall.infrastructure.dao.IOrderDao;
import top.licodetech.mall.infrastructure.event.EventPublisher;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrderRepositoryMarketSettlementTest {

    @Test
    public void test_changeOrderMarketSettlement_onlyPublishChangedOrder() {
        IOrderDao orderDao = mock(IOrderDao.class);
        EventPublisher eventPublisher = mock(EventPublisher.class);
        PaySuccessMessageEvent paySuccessMessageEvent = new PaySuccessMessageEvent();
        ReflectionTestUtils.setField(paySuccessMessageEvent, "TOPIC_ORDER_PAY_SUCCESS", "topic.order_pay_success");

        OrderRepository orderRepository = new OrderRepository();
        ReflectionTestUtils.setField(orderRepository, "orderDao", orderDao);
        ReflectionTestUtils.setField(orderRepository, "paySuccessMessageEvent", paySuccessMessageEvent);
        ReflectionTestUtils.setField(orderRepository, "eventPublisher", eventPublisher);

        when(orderDao.changeOrderMarketSettlement("100001")).thenReturn(1);
        when(orderDao.changeOrderMarketSettlement("100002")).thenReturn(0);

        orderRepository.changeOrderMarketSettlement(Arrays.asList("100001", "100002"));

        verify(eventPublisher).publish(eq("topic.order_pay_success"), contains("\"tradeNo\":\"100001\""));
        verify(eventPublisher, never()).publish(eq("topic.order_pay_success"), contains("\"tradeNo\":\"100002\""));
    }

    @Test
    public void test_changeOrderMarketSettlement_emptyList_skip() {
        IOrderDao orderDao = mock(IOrderDao.class);
        EventPublisher eventPublisher = mock(EventPublisher.class);

        OrderRepository orderRepository = new OrderRepository();
        ReflectionTestUtils.setField(orderRepository, "orderDao", orderDao);
        ReflectionTestUtils.setField(orderRepository, "eventPublisher", eventPublisher);

        orderRepository.changeOrderMarketSettlement(null);

        verify(orderDao, never()).changeOrderMarketSettlement("100001");
        verify(eventPublisher, never()).publish(eq("topic.order_pay_success"), contains("tradeNo"));
    }

}
