package co.edu.unicauca.saga.orquestador.workflow;

import java.time.Duration;

import org.slf4j.Logger;

import co.edu.unicauca.saga.orquestador.actividades.NotificacionActivities;
import co.edu.unicauca.saga.orquestador.actividades.ServiciosViajeActivities;
import co.edu.unicauca.saga.orquestador.modelo.EstadoSaga;
import co.edu.unicauca.saga.orquestador.modelo.ResultadoReserva;
import co.edu.unicauca.saga.orquestador.modelo.SolicitudReserva;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;

/**
 * Saga orquestada: vuelo -> hotel -> auto -> pago (transacción pivote) -> notificación.
 *
 * <p>
 * Antes del pago, cualquier fallo dispara las compensaciones en orden inverso. Después del pago la
 * reserva ya no se deshace: la notificación es un paso "reintentable" que no compensa la saga.
 */
public class ReservaViajeWorkflowImpl implements ReservaViajeWorkflow {

    private static final Logger log = Workflow.getLogger(ReservaViajeWorkflowImpl.class);

    /** Pasos hacia adelante: pocos reintentos; si el servicio sigue fallando se compensa. */
    private final ServiciosViajeActivities servicios = Workflow.newActivityStub(ServiciosViajeActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setBackoffCoefficient(2)
                            .setMaximumInterval(Duration.ofSeconds(10))
                            .setMaximumAttempts(5)
                            .build())
                    .build());

    /** Compensaciones: se reintentan sin límite, una compensación nunca puede "rendirse". */
    private final ServiciosViajeActivities compensaciones = Workflow.newActivityStub(
            ServiciosViajeActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setBackoffCoefficient(2)
                            .setMaximumInterval(Duration.ofSeconds(30))
                            .setMaximumAttempts(0)
                            .build())
                    .build());

    /** Correo: fallos temporales del SMTP se absorben con reintentos y backoff. */
    private final NotificacionActivities notificaciones = Workflow.newActivityStub(NotificacionActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(2))
                            .setBackoffCoefficient(2)
                            .setMaximumInterval(Duration.ofSeconds(20))
                            .setMaximumAttempts(10)
                            .build())
                    .build());

    private EstadoSaga estado;

    @Override
    public ResultadoReserva reservar(SolicitudReserva s) {
        String sagaId = Workflow.getInfo().getWorkflowId();
        estado = new EstadoSaga(sagaId);
        Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());

        String vuelo = null;
        String hotel = null;
        String auto = null;
        String pago = null;
        try {
            // La compensación se registra ANTES de invocar el paso: si la llamada llega al servicio
            // pero la respuesta se pierde (timeout), igual se compensa. Las cancelaciones son idempotentes.
            saga.addCompensation(compensaciones::cancelarVuelo, sagaId);
            vuelo = servicios.reservarVuelo(sagaId, s);
            estado.registrar("VUELO_RESERVADO", vuelo);
            pausa(s);

            saga.addCompensation(compensaciones::cancelarHotel, sagaId);
            hotel = servicios.reservarHotel(sagaId, s);
            estado.registrar("HOTEL_RESERVADO", hotel);
            pausa(s);

            saga.addCompensation(compensaciones::cancelarAuto, sagaId);
            auto = servicios.reservarAuto(sagaId, s);
            estado.registrar("AUTO_RESERVADO", auto);
            pausa(s);

            saga.addCompensation(compensaciones::reembolsarPago, sagaId);
            pago = servicios.cobrarPago(sagaId, s);
            estado.registrar("PAGO_APROBADO", pago);
        } catch (ActivityFailure e) {
            String motivo = motivo(e);
            log.warn("Saga {} falló: {}. Compensando...", sagaId, motivo);
            estado.registrar("COMPENSANDO", motivo);
            saga.compensate();
            estado.registrar("COMPENSADA", "pasos previos revertidos en orden inverso");

            boolean avisado = notificar(sagaId, "CANCELACION", s.email(), "Reserva no realizada",
                    "Hola " + s.cliente() + ", no fue posible reservar tu paquete: " + motivo
                            + ". No se realizó ningún cobro.");
            return terminar(new ResultadoReserva(sagaId, "CANCELADA", null, null, null, null, avisado, motivo));
        }

        // Pivote superado: la reserva queda confirmada aunque el correo falle definitivamente.
        boolean avisado = notificar(sagaId, "CONFIRMACION", s.email(), "Reserva confirmada",
                "Hola " + s.cliente() + ", tu paquete a " + s.destino() + " está confirmado. Vuelo " + vuelo
                        + ", hotel " + hotel + ", auto " + auto + ", pago " + pago + ".");
        return terminar(new ResultadoReserva(sagaId, "CONFIRMADA", vuelo, hotel, auto, pago, avisado,
                avisado ? null : "Reserva confirmada; notificación pendiente"));
    }

    @Override
    public EstadoSaga estado() {
        return estado;
    }

    private boolean notificar(String sagaId, String tipo, String email, String asunto, String mensaje) {
        try {
            notificaciones.enviarNotificacion(sagaId, tipo, email, asunto, mensaje);
            estado.registrar("NOTIFICACION_ENVIADA", tipo);
            return true;
        } catch (ActivityFailure e) {
            log.error("Saga {}: no se pudo enviar la notificación {}: {}", sagaId, tipo, motivo(e));
            estado.registrar("NOTIFICACION_FALLIDA", motivo(e));
            return false;
        }
    }

    private ResultadoReserva terminar(ResultadoReserva resultado) {
        estado.setResultado(resultado);
        estado.registrar(resultado.estado(), resultado.motivo());
        return resultado;
    }

    /** Timer durable: si el worker cae durante la espera, otro worker la retoma al reiniciar. */
    private void pausa(SolicitudReserva s) {
        if (s.pausaEntrePasosSegundos() > 0) {
            Workflow.sleep(Duration.ofSeconds(s.pausaEntrePasosSegundos()));
        }
    }

    private static String motivo(ActivityFailure e) {
        Throwable causa = e.getCause();
        if (causa instanceof ApplicationFailure af) {
            return af.getType() + ": " + af.getOriginalMessage();
        }
        return causa != null ? causa.getMessage() : e.getMessage();
    }
}
