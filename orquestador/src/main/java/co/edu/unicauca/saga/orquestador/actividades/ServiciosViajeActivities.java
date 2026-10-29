package co.edu.unicauca.saga.orquestador.actividades;

import co.edu.unicauca.saga.orquestador.modelo.SolicitudReserva;
import io.temporal.activity.ActivityInterface;

/**
 * Pasos de la saga y sus compensaciones. Todas las operaciones son idempotentes en los
 * microservicios (clave = sagaId), por lo que Temporal puede reintentarlas con seguridad.
 */
@ActivityInterface
public interface ServiciosViajeActivities {

    String reservarVuelo(String sagaId, SolicitudReserva solicitud);

    void cancelarVuelo(String sagaId);

    String reservarHotel(String sagaId, SolicitudReserva solicitud);

    void cancelarHotel(String sagaId);

    String reservarAuto(String sagaId, SolicitudReserva solicitud);

    void cancelarAuto(String sagaId);

    String cobrarPago(String sagaId, SolicitudReserva solicitud);

    void reembolsarPago(String sagaId);
}
