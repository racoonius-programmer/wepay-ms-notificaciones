package cl.duoc.wepay_ms_notificaciones.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 1. Declaración de Exchanges
    @Bean
    public DirectExchange cmdDirectExchange() {
        return new DirectExchange("cmd.direct");
    }

    @Bean
    public DirectExchange dlqExchange() {
        return new DirectExchange("cmd.dead.dlx");
    }

    // 2. Declaración de la Cola Principal para Emails con argumentos para DLQ
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable("q.cmd.email")
                .withArgument("x-dead-letter-exchange", "cmd.dead.dlx")
                .withArgument("x-dead-letter-routing-key", "email.send.dlq")
                .build();
    }

    // 3. Declaración de la Dead Letter Queue (DLQ) para Emails
    @Bean
    public Queue emailDlq() {
        return QueueBuilder.durable("q.cmd.email.dlq").build();
    }

    // 4. Bindings (Unir Colas con Exchanges)
    @Bean
    public Binding emailBinding(Queue emailQueue, DirectExchange cmdDirectExchange) {
        return BindingBuilder.bind(emailQueue).to(cmdDirectExchange).with("email.send");
    }

    @Bean
    public Binding emailDlqBinding(Queue emailDlq, DirectExchange dlqExchange) {
        return BindingBuilder.bind(emailDlq).to(dlqExchange).with("email.send.dlq");
    }

// 5. Conversor JSON (Forma actualizada)
    @Bean
    public MessageConverter jsonMessageConverter(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}