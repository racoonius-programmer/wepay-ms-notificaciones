package cl.duoc.wepay_ms_notificaciones.consumer;

import cl.duoc.wepay_ms_notificaciones.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Consumidor de las 3 colas principales.
 * Si un listener lanza una excepción, Spring reintenta según application.yml
 * (3 intentos con backoff) y luego rechaza el mensaje sin reencolar: RabbitMQ
 * lo envía al exchange "ex.pedidos.dlx" y queda en la DLQ correspondiente.
 */
@Component
public class Receiver {

    /** Permite simular una caída del servicio de facturación (APP_FALLA_FACTURACION=true). */
    @Value("${app.falla-facturacion:false}")
    private boolean fallaFacturacion;

    @RabbitListener(queues = RabbitMQConfig.Q_EMAIL)
    public void receiveEmail(Map<String, Object> message) throws InterruptedException {
        // Simula la latencia de un servidor SMTP (útil para ver la cola crecer en la prueba de carga)
        Thread.sleep(300);

        System.out.println("=".repeat(60));
        System.out.println("[" + timestamp() + "] [EMAIL] Nuevo correo recibido para procesar:");
        System.out.println("To: " + message.get("to"));
        System.out.println("Subject: " + message.get("subject"));
        System.out.println("Body: " + message.get("body"));
        System.out.println("=".repeat(60));
    }

    @RabbitListener(queues = RabbitMQConfig.Q_SMS)
    public void receiveSms(Map<String, Object> message) {
        System.out.println("[" + timestamp() + "] [SMS] Pedido #" + message.get("pedidoId")
                + " está " + message.get("estado") + " -> SMS enviado al cliente");
    }

    @RabbitListener(queues = RabbitMQConfig.Q_FACTURACION)
    public void receiveFacturacion(Map<String, Object> message) {
        System.out.println("[" + timestamp() + "] [FACTURACION] Procesando pedido #" + message.get("pedidoId"));
        if (fallaFacturacion) {
            throw new IllegalStateException("Servicio de facturación no disponible (simulado)");
        }
        System.out.println("[" + timestamp() + "] [FACTURACION] Boleta generada para pedido #"
                + message.get("pedidoId") + " por $" + message.get("total"));
    }

    private static String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}
