package cl.duoc.wepay_ms_notificaciones.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EmailConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmailConsumer.class);

    // Escucha automáticamente la cola que configuramos en RabbitMQConfig
    @RabbitListener(queues = "q.cmd.email")
    public void procesarComandoCorreo(Map<String, Object> mensaje) {
        log.info("📩 Comando de correo recibido desde RabbitMQ...");
        
        try {
            // Extraemos los datos dinámicamente del JSON que enviará ms-pedidos
            String destinatario = (String) mensaje.getOrDefault("to", "cliente@wepay.cl");
            String asunto = (String) mensaje.getOrDefault("subject", "Actualización de Pedido");
            String cuerpo = (String) mensaje.getOrDefault("body", "Tu pedido ha cambiado de estado.");

            log.info("Destinatario: {}", destinatario);
            log.info("Asunto: {}", asunto);
            log.info("Cuerpo: {}", cuerpo);
            
            // Simulación del envío de correo para el MVP
            log.info("✅ Simulación: Correo enviado exitosamente por el worker de notificaciones.");
            
        } catch (Exception e) {
            log.error("❌ Error procesando el correo. El mensaje será enviado a la DLQ (Dead Letter Queue).", e);
            // Al lanzar una excepción, RabbitMQ detecta el fallo y mueve el mensaje a q.cmd.email.dlq
            throw new RuntimeException("Fallo en el envío del correo", e);
        }
    }
}