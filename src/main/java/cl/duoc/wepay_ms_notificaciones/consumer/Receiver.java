package cl.duoc.wepay_ms_notificaciones.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class Receiver {

    /**
     * Se ejecuta automáticamente cada vez que llega un mensaje a 'q.cmd.email'.
     * Spring AMQP maneja conexión, deserialización, ack y excepciones.
     */
    @RabbitListener(queues = "q.cmd.email")
        public void receiveMessage(Map<String, Object> message) {
        try {
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            
            System.out.println("=" .repeat(60));
            System.out.println("[" + timestamp + "] [OK] Nuevo correo recibido para procesar:");
            System.out.println("To: " + message.get("to"));
            System.out.println("Subject: " + message.get("subject"));
            System.out.println("Body: " + message.get("body"));
            System.out.println("=" .repeat(60));
            
            // Aquí iría tu lógica real (ej: JavaMailSender, envío con SMTP, etc.)
            // Ejemplo:
            // emailService.sendEmail(message);
            
        } catch (Exception e) {
            System.err.println("[ERROR] Error procesando mensaje: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e); // Spring AMQP manejará el reintento según la configuración
        }
    }
}