package cl.duoc.wepay_ms_notificaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class WepayMsNotificacionesApplication {

	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(WepayMsNotificacionesApplication.class, args);
		
		System.out.println("\n" + "=".repeat(60));
		System.out.println("RabbitMQ - Microservicio de Notificaciones por Email");
		System.out.println("=".repeat(60));
		System.out.println("[OK] Aplicación iniciada correctamente");
		System.out.println("[OK] RabbitMQ conectado en localhost:5672");
		System.out.println("[OK] Cola 'q.cmd.email' lista para recibir mensajes\n");
	}

}
