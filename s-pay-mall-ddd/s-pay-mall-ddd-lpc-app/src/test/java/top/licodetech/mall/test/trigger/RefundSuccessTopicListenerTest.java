package top.licodetech.mall.test.trigger;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.licodetech.mall.domain.order.model.valobj.RefundTypeVO;
import top.licodetech.mall.domain.order.service.IOrderService;
import top.licodetech.mall.trigger.listener.RefundSuccessTopicListener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RefundSuccessTopicListenerTest {

    @Test
    public void test_listener_unpaidUnlock_receiveSuccessMessage() {
        IOrderService orderService = mock(IOrderService.class);
        RefundSuccessTopicListener listener = new RefundSuccessTopicListener();
        ReflectionTestUtils.setField(listener, "orderService", orderService);
        String message = "{\"type\":\"unpaid_unlock\",\"orderId\":\"100001\",\"outTradeNo\":\"100001\"}";

        when(orderService.receiveRefundSuccessMessage("100001", RefundTypeVO.UNPAID_UNLOCK, message)).thenReturn(true);

        listener.listener(message);

        verify(orderService).receiveRefundSuccessMessage("100001", RefundTypeVO.UNPAID_UNLOCK, message);
    }

    @Test
    public void test_listener_missingType_ackAndSkipTask() {
        IOrderService orderService = mock(IOrderService.class);
        RefundSuccessTopicListener listener = new RefundSuccessTopicListener();
        ReflectionTestUtils.setField(listener, "orderService", orderService);
        String message = "{\"orderId\":\"100001\",\"outTradeNo\":\"100001\"}";

        listener.listener(message);

        verify(orderService, never()).receiveRefundSuccessMessage("100001", RefundTypeVO.PAID_FORMED, message);
    }

    @Test
    public void test_listener_illegalType_ackAndSkipTask() {
        IOrderService orderService = mock(IOrderService.class);
        RefundSuccessTopicListener listener = new RefundSuccessTopicListener();
        ReflectionTestUtils.setField(listener, "orderService", orderService);
        String message = "{\"type\":\"unknown\",\"orderId\":\"100001\",\"outTradeNo\":\"100001\"}";

        listener.listener(message);

        verify(orderService, never()).receiveRefundSuccessMessage("100001", RefundTypeVO.PAID_FORMED, message);
    }

}
