package top.licodetech.mall.test.config;

import org.junit.Test;
import org.springframework.amqp.core.Queue;
import top.licodetech.mall.config.RabbitMQConfig;

import static org.junit.Assert.assertEquals;

public class RabbitMQConfigTest {

    @Test
    public void test_topicTeamSuccessQueue_hasDeadLetterArguments() {
        RabbitMQConfig rabbitMQConfig = new RabbitMQConfig();

        Queue queue = rabbitMQConfig.topicTeamSuccessQueue("s_pay_mall_queue_2_topic_team_success", "topic.team_success");

        assertEquals("s_pay_mall_queue_2_topic_team_success.dlx", queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals("topic.team_success.dead", queue.getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    public void test_retryOperationsInterceptor_created() {
        RabbitMQConfig rabbitMQConfig = new RabbitMQConfig();

        assertEquals("RetryOperationsInterceptor", rabbitMQConfig.retryOperationsInterceptor().getClass().getSimpleName());
    }

}
