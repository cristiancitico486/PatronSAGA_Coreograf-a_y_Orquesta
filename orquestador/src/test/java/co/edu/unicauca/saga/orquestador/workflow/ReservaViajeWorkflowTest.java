package co.edu.unicauca.saga.orquestador.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.unicauca.saga.orquestador.actividades.NotificacionActivities;
import co.edu.unicauca.saga.orquestador.actividades.ServiciosViajeActivities;
import co.edu.unicauca.saga.orquestador.modelo.ResultadoReserva;
import co.edu.unicauca.saga.orquestador.modelo.SolicitudReserva;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.WorkflowReplayer;
import io.temporal.worker.Worker;

/** Camino feliz y los cuatro escenarios de fallo pedidos, sobre un servidor Temporal en memoria. */
class ReservaViajeWorkflowTest {

    private static final String COLA = "COLA_PRUEBAS";

    private TestWorkflowEnvironment entorno;
    private ServiciosFalsos servicios;
    private NotificacionesFalsas notificaciones;

    @BeforeEach
    void iniciar() {
        entorno = TestWorkflowEnvironment.newInstance();
        servicios = new ServiciosFalsos();
        notificaciones = new NotificacionesFalsas();
        registrar(entorno.newWorker(COLA), servicios, notificaciones);
        entorno.start();
    }

    @AfterEach
    void detener() {
        entorno.close();
    }

    @Test
    void caminoFeliz() {
        ResultadoReserva r = ejecutar("saga-ok", solicitud("HOTEL-CENTRO", "4111111111111111", 0));

        assertEquals("CONFIRMADA", r.estado());
        assertTrue(r.notificacionEnviada());
        assertEquals(List.of("reservarVuelo", "reservarHotel", "reservarAuto", "cobrarPago"), servicios.llamadas);
    }

    @Test
    void hotelSinCupoCompensaElVuelo() {
        ResultadoReserva r = ejecutar("saga-hotel", solicitud("HOTEL-LLENO", "4111111111111111", 0));

        assertEquals("CANCELADA", r.estado());
        assertTrue(r.motivo().contains("HOTEL_SIN_CUPO"));
        // Error de negocio: un solo intento, luego compensación en orden inverso
        assertEquals(List.of("reservarVuelo", "reservarHotel", "cancelarHotel", "cancelarVuelo"), servicios.llamadas);
        assertFalse(servicios.llamadas.contains("cobrarPago"), "nunca se cobra");
        assertEquals(List.of("CANCELACION"), notificaciones.enviadas);
    }

    @Test
    void pagoRechazadoNoSeReintentaYCompensaTodo() {
        ResultadoReserva r = ejecutar("saga-pago", solicitud("HOTEL-CENTRO", ServiciosFalsos.TARJETA_RECHAZADA, 0));

        assertEquals("CANCELADA", r.estado());
        assertTrue(r.motivo().contains("PAGO_RECHAZADO"));
        assertEquals(1, servicios.intentosCobro.get(), "el rechazo no se reintenta");
        assertEquals(List.of("reservarVuelo", "reservarHotel", "reservarAuto", "cobrarPago",
                "reembolsarPago", "cancelarAuto", "cancelarHotel", "cancelarVuelo"), servicios.llamadas);
    }

    @Test
    void falloTemporalDelCorreoSeReintentaSinCompensar() {
        notificaciones.fallosPendientes.set(3);

        ResultadoReserva r = ejecutar("saga-correo", solicitud("HOTEL-CENTRO", "4111111111111111", 0));

        assertEquals("CONFIRMADA", r.estado());
        assertTrue(r.notificacionEnviada());
        assertEquals(4, notificaciones.intentos.get(), "3 fallos + 1 éxito");
        assertFalse(servicios.llamadas.stream().anyMatch(l -> l.startsWith("cancelar")), "no se compensa");
    }

    /**
     * Tras una caída, el worker nuevo no tiene nada en memoria: reconstruye la saga reproduciendo el
     * historial guardado en Temporal, sin volver a invocar las actividades ya completadas. Aquí se
     * verifica justamente eso con el replayer (la caída real del proceso se prueba con
     * scripts/prueba-4-caida-worker.ps1 contra el servidor de desarrollo).
     */
    @Test
    void caidaDelWorkerSeRecuperaReproduciendoElHistorial() throws Exception {
        ResultadoReserva r = ejecutar("saga-caida", solicitud("HOTEL-CENTRO", "4111111111111111", 30));
        assertEquals("CONFIRMADA", r.estado());
        int llamadasAntes = servicios.llamadas.size();

        WorkflowExecutionHistory historial = entorno.getWorkflowClient().fetchHistory("saga-caida");
        WorkflowReplayer.replayWorkflowExecution(historial, ReservaViajeWorkflowImpl.class);

        assertEquals(llamadasAntes, servicios.llamadas.size(), "el replay no repite llamadas a los servicios");
        assertEquals(List.of("reservarVuelo", "reservarHotel", "reservarAuto", "cobrarPago"), servicios.llamadas);
    }

    private ResultadoReserva ejecutar(String sagaId, SolicitudReserva s) {
        ReservaViajeWorkflow saga = entorno.getWorkflowClient().newWorkflowStub(ReservaViajeWorkflow.class,
                WorkflowOptions.newBuilder().setWorkflowId(sagaId).setTaskQueue(COLA).build());
        return saga.reservar(s);
    }

    private static void registrar(Worker worker, ServiciosViajeActivities s, NotificacionActivities n) {
        worker.registerWorkflowImplementationTypes(ReservaViajeWorkflowImpl.class);
        worker.registerActivitiesImplementations(s, n);
    }

    private static SolicitudReserva solicitud(String hotel, String tarjeta, int pausa) {
        return new SolicitudReserva("Ana", "ana@correo.com", "PPN", "CTG", "2026-12-01", "2026-12-05",
                hotel, tarjeta, new BigDecimal("2500000"), pausa);
    }

    /** Dobles de los microservicios con las mismas reglas de error que los reales. */
    static class ServiciosFalsos implements ServiciosViajeActivities {

        static final String TARJETA_RECHAZADA = "4000000000000002";

        final List<String> llamadas = new CopyOnWriteArrayList<>();
        final AtomicInteger intentosCobro = new AtomicInteger();

        @Override
        public String reservarVuelo(String sagaId, SolicitudReserva s) {
            llamadas.add("reservarVuelo");
            return "VUELO-1";
        }

        @Override
        public void cancelarVuelo(String sagaId) {
            llamadas.add("cancelarVuelo");
        }

        @Override
        public String reservarHotel(String sagaId, SolicitudReserva s) {
            llamadas.add("reservarHotel");
            if (s.codigoHotel().equals("HOTEL-LLENO")) {
                throw ApplicationFailure.newNonRetryableFailure("Hoteles: sin cupo", "HOTEL_SIN_CUPO");
            }
            return "HOTEL-1";
        }

        @Override
        public void cancelarHotel(String sagaId) {
            llamadas.add("cancelarHotel");
        }

        @Override
        public String reservarAuto(String sagaId, SolicitudReserva s) {
            llamadas.add("reservarAuto");
            return "AUTO-1";
        }

        @Override
        public void cancelarAuto(String sagaId) {
            llamadas.add("cancelarAuto");
        }

        @Override
        public String cobrarPago(String sagaId, SolicitudReserva s) {
            intentosCobro.incrementAndGet();
            llamadas.add("cobrarPago");
            if (s.numeroTarjeta().equals(TARJETA_RECHAZADA)) {
                throw ApplicationFailure.newNonRetryableFailure("Pagos: Fondos insuficientes", "PAGO_RECHAZADO");
            }
            return "PAGO-1";
        }

        @Override
        public void reembolsarPago(String sagaId) {
            llamadas.add("reembolsarPago");
        }
    }

    static class NotificacionesFalsas implements NotificacionActivities {

        final List<String> enviadas = new CopyOnWriteArrayList<>();
        final AtomicInteger fallosPendientes = new AtomicInteger();
        final AtomicInteger intentos = new AtomicInteger();

        @Override
        public void enviarNotificacion(String sagaId, String tipo, String email, String asunto, String mensaje) {
            intentos.incrementAndGet();
            if (fallosPendientes.getAndDecrement() > 0) {
                throw ApplicationFailure.newFailure("SMTP no disponible", "SMTP_NO_DISPONIBLE");
            }
            enviadas.add(tipo);
        }
    }
}
