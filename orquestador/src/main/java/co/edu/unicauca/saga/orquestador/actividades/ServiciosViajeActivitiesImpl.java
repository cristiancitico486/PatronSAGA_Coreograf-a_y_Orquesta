package co.edu.unicauca.saga.orquestador.actividades;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import co.edu.unicauca.saga.orquestador.modelo.SolicitudReserva;
import io.temporal.activity.Activity;

@Component
public class ServiciosViajeActivitiesImpl implements ServiciosViajeActivities {

    private static final Logger log = LoggerFactory.getLogger(ServiciosViajeActivitiesImpl.class);

    private final ClienteServicios cliente;
    private final String urlVuelos;
    private final String urlHoteles;
    private final String urlAutos;
    private final String urlPagos;

    public ServiciosViajeActivitiesImpl(ClienteServicios cliente,
            @Value("${servicios.vuelos}") String urlVuelos,
            @Value("${servicios.hoteles}") String urlHoteles,
            @Value("${servicios.autos}") String urlAutos,
            @Value("${servicios.pagos}") String urlPagos) {
        this.cliente = cliente;
        this.urlVuelos = urlVuelos;
        this.urlHoteles = urlHoteles;
        this.urlAutos = urlAutos;
        this.urlPagos = urlPagos;
    }

    @Override
    public String reservarVuelo(String sagaId, SolicitudReserva s) {
        traza("reservarVuelo", sagaId);
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("sagaId", sagaId);
        cuerpo.put("origen", s.origen());
        cuerpo.put("destino", s.destino());
        cuerpo.put("fecha", s.fechaIda());
        return "VUELO-" + cliente.post("Vuelos", urlVuelos + "/reservas", cuerpo).get("id");
    }

    @Override
    public void cancelarVuelo(String sagaId) {
        traza("cancelarVuelo", sagaId);
        cliente.post("Vuelos", urlVuelos + "/reservas/" + sagaId + "/cancelar", null);
    }

    @Override
    public String reservarHotel(String sagaId, SolicitudReserva s) {
        traza("reservarHotel", sagaId);
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("sagaId", sagaId);
        cuerpo.put("codigoHotel", s.codigoHotel());
        cuerpo.put("fechaEntrada", s.fechaIda());
        cuerpo.put("fechaSalida", s.fechaRegreso());
        return "HOTEL-" + cliente.post("Hoteles", urlHoteles + "/reservas", cuerpo).get("id");
    }

    @Override
    public void cancelarHotel(String sagaId) {
        traza("cancelarHotel", sagaId);
        cliente.post("Hoteles", urlHoteles + "/reservas/" + sagaId + "/cancelar", null);
    }

    @Override
    public String reservarAuto(String sagaId, SolicitudReserva s) {
        traza("reservarAuto", sagaId);
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("sagaId", sagaId);
        cuerpo.put("ciudad", s.destino());
        cuerpo.put("fechaInicio", s.fechaIda());
        cuerpo.put("fechaFin", s.fechaRegreso());
        return "AUTO-" + cliente.post("Autos", urlAutos + "/reservas", cuerpo).get("id");
    }

    @Override
    public void cancelarAuto(String sagaId) {
        traza("cancelarAuto", sagaId);
        cliente.post("Autos", urlAutos + "/reservas/" + sagaId + "/cancelar", null);
    }

    @Override
    public String cobrarPago(String sagaId, SolicitudReserva s) {
        traza("cobrarPago", sagaId);
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("sagaId", sagaId);
        cuerpo.put("numeroTarjeta", s.numeroTarjeta());
        cuerpo.put("monto", s.monto());
        return "PAGO-" + cliente.post("Pagos", urlPagos, cuerpo).get("id");
    }

    @Override
    public void reembolsarPago(String sagaId) {
        traza("reembolsarPago", sagaId);
        cliente.post("Pagos", urlPagos + "/" + sagaId + "/reembolsar", null);
    }

    private static void traza(String operacion, String sagaId) {
        log.info("[{}] {} (intento {})", sagaId, operacion, Activity.getExecutionContext().getInfo().getAttempt());
    }
}
