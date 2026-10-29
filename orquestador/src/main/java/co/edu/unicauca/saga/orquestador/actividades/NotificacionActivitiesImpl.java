package co.edu.unicauca.saga.orquestador.actividades;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.temporal.activity.Activity;

@Component
public class NotificacionActivitiesImpl implements NotificacionActivities {

    private static final Logger log = LoggerFactory.getLogger(NotificacionActivitiesImpl.class);

    private final ClienteServicios cliente;
    private final String urlNotificaciones;

    public NotificacionActivitiesImpl(ClienteServicios cliente,
            @Value("${servicios.notificaciones}") String urlNotificaciones) {
        this.cliente = cliente;
        this.urlNotificaciones = urlNotificaciones;
    }

    @Override
    public void enviarNotificacion(String sagaId, String tipo, String email, String asunto, String mensaje) {
        log.info("[{}] enviarNotificacion {} (intento {})", sagaId, tipo,
                Activity.getExecutionContext().getInfo().getAttempt());
        cliente.post("Notificaciones", urlNotificaciones, Map.of(
                "sagaId", sagaId,
                "tipo", tipo,
                "email", email,
                "asunto", asunto,
                "mensaje", mensaje));
    }
}
