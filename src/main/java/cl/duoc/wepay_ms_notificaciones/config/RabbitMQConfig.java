package cl.duoc.wepay_ms_notificaciones.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología RabbitMQ de WePay (3 colas principales + 3 DLQ).
 *
 * ms-pedidos publica en el exchange "ex.pedidos" con routing key "pedido.estado.<estado>".
 * Cada cola se suscribe solo a los estados que le interesan:
 *   - q.cmd.email        ← pedido.estado.*          (todos los cambios)
 *   - q.cmd.sms          ← pedido.estado.despachado / pedido.estado.cancelado  (TTL 60 s)
 *   - q.cmd.facturacion  ← pedido.estado.entregado  (máx. 100 mensajes)
 * Lo que falla, expira o desborda va a "ex.pedidos.dlx" y de ahí a su DLQ.
 *
 * IMPORTANTE: esta misma clase existe en ms-pedidos. Declarar dos veces es
 * idempotente siempre que los argumentos sean idénticos.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EX_PEDIDOS = "ex.pedidos";
    public static final String EX_DLX = "ex.pedidos.dlx";

    public static final String Q_EMAIL = "q.cmd.email";
    public static final String Q_SMS = "q.cmd.sms";
    public static final String Q_FACTURACION = "q.cmd.facturacion";

    public static final String RK_ESTADO = "pedido.estado.";

    // ---------------- Exchanges ----------------

    @Bean
    public TopicExchange pedidosExchange() {
        return new TopicExchange(EX_PEDIDOS, true, false);
    }

    /** Topic (y no direct) para que el Rabbit de respaldo pueda copiar todo con "#". */
    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(EX_DLX, true, false);
    }

    // ---------------- Colas principales ----------------

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(Q_EMAIL)
                .deadLetterExchange(EX_DLX)
                .deadLetterRoutingKey(Q_EMAIL + ".dlq")
                .build();
    }

    /** Un SMS atrasado no sirve: si nadie lo consume en 60 s, expira y va a la DLQ. */
    @Bean
    public Queue smsQueue() {
        return QueueBuilder.durable(Q_SMS)
                .deadLetterExchange(EX_DLX)
                .deadLetterRoutingKey(Q_SMS + ".dlq")
                .ttl(60_000)
                .build();
    }

    /** Protege a facturación de una avalancha: sobre 100 mensajes, el nuevo se rechaza a la DLQ. */
    @Bean
    public Queue facturacionQueue() {
        return QueueBuilder.durable(Q_FACTURACION)
                .deadLetterExchange(EX_DLX)
                .deadLetterRoutingKey(Q_FACTURACION + ".dlq")
                .maxLength(100)
                .withArgument("x-overflow", "reject-publish-dlx")
                .build();
    }

    // ---------------- DLQs ----------------

    @Bean
    public Queue emailDlq() {
        return QueueBuilder.durable(Q_EMAIL + ".dlq").build();
    }

    @Bean
    public Queue smsDlq() {
        return QueueBuilder.durable(Q_SMS + ".dlq").build();
    }

    @Bean
    public Queue facturacionDlq() {
        return QueueBuilder.durable(Q_FACTURACION + ".dlq").build();
    }

    // ---------------- Bindings principales ----------------

    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue()).to(pedidosExchange()).with(RK_ESTADO + "*");
    }

    @Bean
    public Binding smsDespachadoBinding() {
        return BindingBuilder.bind(smsQueue()).to(pedidosExchange()).with(RK_ESTADO + "despachado");
    }

    @Bean
    public Binding smsCanceladoBinding() {
        return BindingBuilder.bind(smsQueue()).to(pedidosExchange()).with(RK_ESTADO + "cancelado");
    }

    @Bean
    public Binding facturacionBinding() {
        return BindingBuilder.bind(facturacionQueue()).to(pedidosExchange()).with(RK_ESTADO + "entregado");
    }

    // ---------------- Bindings DLQ ----------------

    @Bean
    public Binding emailDlqBinding() {
        return BindingBuilder.bind(emailDlq()).to(dlxExchange()).with(Q_EMAIL + ".dlq");
    }

    @Bean
    public Binding smsDlqBinding() {
        return BindingBuilder.bind(smsDlq()).to(dlxExchange()).with(Q_SMS + ".dlq");
    }

    @Bean
    public Binding facturacionDlqBinding() {
        return BindingBuilder.bind(facturacionDlq()).to(dlxExchange()).with(Q_FACTURACION + ".dlq");
    }

    // ---------------- Conversión JSON ----------------

    /**
     * Spring Boot inyecta este converter en la fábrica de listeners automática.
     * No se define una fábrica propia para que apliquen los reintentos, concurrencia
     * y prefetch configurados en application.yml (spring.rabbitmq.listener.simple.*).
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
