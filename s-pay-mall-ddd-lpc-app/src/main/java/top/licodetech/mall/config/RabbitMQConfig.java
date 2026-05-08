package top.licodetech.mall.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

/**
 * @author LiPC
 * @description
 * @create 2026-04-01 16:41
 */
@Configuration
public class RabbitMQConfig {

    private static final String DEAD_LETTER_EXCHANGE_SUFFIX = ".dlx";
    private static final String DEAD_LETTER_QUEUE_SUFFIX = ".dlq";
    private static final String DEAD_LETTER_ROUTING_KEY_SUFFIX = ".dead";

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setPrefetchCount(1);
        factory.setAdviceChain(retryOperationsInterceptor());
        return factory;
    }

    @Bean
    public RetryOperationsInterceptor retryOperationsInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000L, 2.0, 10000L)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    public TopicExchange topicTeamSuccessExchange(
            @Value("${spring.rabbitmq.config.consumer.topic_team_success.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue topicTeamSuccessQueue(
            @Value("${spring.rabbitmq.config.consumer.topic_team_success.queue}") String queue,
            @Value("${spring.rabbitmq.config.consumer.topic_team_success.routing_key}") String routingKey) {
        return buildQueueWithDeadLetter(queue, routingKey);
    }

    @Bean
    public Binding topicTeamSuccessBinding(
            TopicExchange topicTeamSuccessExchange,
            Queue topicTeamSuccessQueue,
            @Value("${spring.rabbitmq.config.consumer.topic_team_success.routing_key}") String routingKey) {
        return BindingBuilder.bind(topicTeamSuccessQueue)
                .to(topicTeamSuccessExchange)
                .with(routingKey);
    }

    @Bean
    public DirectExchange topicTeamSuccessDeadLetterExchange(
            @Value("${spring.rabbitmq.config.consumer.topic_team_success.queue}") String queue) {
        return new DirectExchange(deadLetterExchange(queue), true, false);
    }

    @Bean
    public Queue topicTeamSuccessDeadLetterQueue(
            @Value("${spring.rabbitmq.config.consumer.topic_team_success.queue}") String queue) {
        return QueueBuilder.durable(deadLetterQueue(queue)).build();
    }

    @Bean
    public Binding topicTeamSuccessDeadLetterBinding(
            DirectExchange topicTeamSuccessDeadLetterExchange,
            Queue topicTeamSuccessDeadLetterQueue,
            @Value("${spring.rabbitmq.config.consumer.topic_team_success.routing_key}") String routingKey) {
        return BindingBuilder.bind(topicTeamSuccessDeadLetterQueue)
                .to(topicTeamSuccessDeadLetterExchange)
                .with(deadLetterRoutingKey(routingKey));
    }

    @Bean
    public TopicExchange topicTeamRefundExchange(
            @Value("${spring.rabbitmq.config.consumer.topic_team_refund.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue topicTeamRefundQueue(
            @Value("${spring.rabbitmq.config.consumer.topic_team_refund.queue}") String queue,
            @Value("${spring.rabbitmq.config.consumer.topic_team_refund.routing_key}") String routingKey) {
        return buildQueueWithDeadLetter(queue, routingKey);
    }

    @Bean
    public Binding topicTeamRefundBinding(
            TopicExchange topicTeamRefundExchange,
            Queue topicTeamRefundQueue,
            @Value("${spring.rabbitmq.config.consumer.topic_team_refund.routing_key}") String routingKey) {
        return BindingBuilder.bind(topicTeamRefundQueue)
                .to(topicTeamRefundExchange)
                .with(routingKey);
    }

    @Bean
    public DirectExchange topicTeamRefundDeadLetterExchange(
            @Value("${spring.rabbitmq.config.consumer.topic_team_refund.queue}") String queue) {
        return new DirectExchange(deadLetterExchange(queue), true, false);
    }

    @Bean
    public Queue topicTeamRefundDeadLetterQueue(
            @Value("${spring.rabbitmq.config.consumer.topic_team_refund.queue}") String queue) {
        return QueueBuilder.durable(deadLetterQueue(queue)).build();
    }

    @Bean
    public Binding topicTeamRefundDeadLetterBinding(
            DirectExchange topicTeamRefundDeadLetterExchange,
            Queue topicTeamRefundDeadLetterQueue,
            @Value("${spring.rabbitmq.config.consumer.topic_team_refund.routing_key}") String routingKey) {
        return BindingBuilder.bind(topicTeamRefundDeadLetterQueue)
                .to(topicTeamRefundDeadLetterExchange)
                .with(deadLetterRoutingKey(routingKey));
    }

    @Bean
    public TopicExchange topicOrderPaySuccessExchange(
            @Value("${spring.rabbitmq.config.consumer.topic_order_pay_success.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue topicOrderPaySuccessQueue(
            @Value("${spring.rabbitmq.config.consumer.topic_order_pay_success.queue}") String queue,
            @Value("${spring.rabbitmq.config.consumer.topic_order_pay_success.routing_key}") String routingKey) {
        return buildQueueWithDeadLetter(queue, routingKey);
    }

    @Bean
    public Binding topicOrderPaySuccessBinding(
            TopicExchange topicOrderPaySuccessExchange,
            Queue topicOrderPaySuccessQueue,
            @Value("${spring.rabbitmq.config.consumer.topic_order_pay_success.routing_key}") String routingKey) {
        return BindingBuilder.bind(topicOrderPaySuccessQueue)
                .to(topicOrderPaySuccessExchange)
                .with(routingKey);
    }

    @Bean
    public DirectExchange topicOrderPaySuccessDeadLetterExchange(
            @Value("${spring.rabbitmq.config.consumer.topic_order_pay_success.queue}") String queue) {
        return new DirectExchange(deadLetterExchange(queue), true, false);
    }

    @Bean
    public Queue topicOrderPaySuccessDeadLetterQueue(
            @Value("${spring.rabbitmq.config.consumer.topic_order_pay_success.queue}") String queue) {
        return QueueBuilder.durable(deadLetterQueue(queue)).build();
    }

    @Bean
    public Binding topicOrderPaySuccessDeadLetterBinding(
            DirectExchange topicOrderPaySuccessDeadLetterExchange,
            Queue topicOrderPaySuccessDeadLetterQueue,
            @Value("${spring.rabbitmq.config.consumer.topic_order_pay_success.routing_key}") String routingKey) {
        return BindingBuilder.bind(topicOrderPaySuccessDeadLetterQueue)
                .to(topicOrderPaySuccessDeadLetterExchange)
                .with(deadLetterRoutingKey(routingKey));
    }

    private Queue buildQueueWithDeadLetter(String queue, String routingKey) {
        return QueueBuilder.durable(queue)
                .withArgument("x-dead-letter-exchange", deadLetterExchange(queue))
                .withArgument("x-dead-letter-routing-key", deadLetterRoutingKey(routingKey))
                .build();
    }

    private String deadLetterExchange(String queue) {
        return queue + DEAD_LETTER_EXCHANGE_SUFFIX;
    }

    private String deadLetterQueue(String queue) {
        return queue + DEAD_LETTER_QUEUE_SUFFIX;
    }

    private String deadLetterRoutingKey(String routingKey) {
        return routingKey + DEAD_LETTER_ROUTING_KEY_SUFFIX;
    }

}
