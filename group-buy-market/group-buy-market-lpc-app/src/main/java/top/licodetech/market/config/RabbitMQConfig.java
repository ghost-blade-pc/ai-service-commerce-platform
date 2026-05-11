package top.licodetech.market.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author LiPC
 * @description
 * @create 2026-04-01 15:55
 */
@Configuration
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.config.producer.exchange}")
    private String exchangeName;

    /**
     * 专属交换机
     */
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    /**
     * 绑定队列到交换机
     */
    @Bean
    public Binding topicTeamSuccessBinding(
            @Value("${spring.rabbitmq.config.producer.topic_team_success.routing_key}") String routingKey,
            @Value("${spring.rabbitmq.config.producer.topic_team_success.queue}") String queue) {
        return BindingBuilder.bind(new Queue(queue, true))
                .to(topicExchange())
                .with(routingKey);
    }

    @Bean
    public Binding topicTeamRefundBinding(
            @Value("${spring.rabbitmq.config.producer.topic_team_refund.routing_key}") String routingKey,
            @Value("${spring.rabbitmq.config.producer.topic_team_refund.queue}") String queue) {
        return BindingBuilder.bind(new Queue(queue, true))
                .to(topicExchange())
                .with(routingKey);
    }
}
