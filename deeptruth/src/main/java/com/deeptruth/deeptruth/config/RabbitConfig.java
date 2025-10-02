package com.deeptruth.deeptruth.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "deeptruth.direct";
    public static final String REQ_QUEUE = "deepfake.request.q";
    public static final String RES_QUEUE = "deepfake.result.q";
    public static final String REQ_ROUTING = "deepfake.request";
    public static final String RES_ROUTING = "deepfake.result";

    private static final String DLX = "deeptruth.dlx";
    private static final String DLQ = "deepfake.request.dlq";

    @Bean
    DirectExchange deeptruthExchange() { return new DirectExchange(EXCHANGE, true, false); }

    @Bean
    DirectExchange dlx() { return new DirectExchange(DLX, true, false); }

    @Bean
    public Queue requestQueue() {
        return QueueBuilder.durable(REQ_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", "deepfake.request.dlq")
                .withArgument("x-message-ttl", 600_000) // 10분 처리 제한
                .build();
    }

    @Bean
    public Queue resultQueue() {
        return QueueBuilder.durable(RES_QUEUE).build();
    }

    @Bean
    public Queue deadLetterQueue() { return QueueBuilder.durable(DLQ).build(); }

    @Bean
    public Binding bindReq() { return BindingBuilder.bind(requestQueue()).to(deeptruthExchange()).with(REQ_ROUTING); }

    @Bean
    public Binding bindRes() { return BindingBuilder.bind(resultQueue()).to(deeptruthExchange()).with(RES_ROUTING); }

    @Bean
    public Binding bindDlq() { return BindingBuilder.bind(deadLetterQueue()).to(dlx()).with("deepfake.request.dlq"); }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter mc) {
        RabbitTemplate rt = new RabbitTemplate(cf);
        rt.setMessageConverter(mc);
        return rt;
    }
}
