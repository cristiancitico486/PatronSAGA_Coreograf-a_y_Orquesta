package co.edu.unicauca.saga.orquestador.actividades;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface NotificacionActivities {

    /** @param tipo CONFIRMACION o CANCELACION */
    void enviarNotificacion(String sagaId, String tipo, String email, String asunto, String mensaje);
}
