package co.edu.unicauca.saga.autos;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservaAutoService {

    private static final Logger log = LoggerFactory.getLogger(ReservaAutoService.class);

    private final ReservaAutoRepository repositorio;

    public ReservaAutoService(ReservaAutoRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Idempotente: si la saga ya reservó, devuelve la misma reserva. */
    @Transactional
    public ReservaAuto reservar(SolicitudReservaAuto s) {
        return repositorio.findBySagaId(s.sagaId())
                .map(existente -> {
                    if (existente.getEstado() == EstadoReserva.CANCELADA) {
                        throw new ErrorNegocioException(HttpStatus.CONFLICT, "RESERVA_CANCELADA",
                                "La saga " + s.sagaId() + " ya fue compensada");
                    }
                    log.info("Reserva repetida para saga {} (idempotencia)", s.sagaId());
                    return existente;
                })
                .orElseGet(() -> {
                    log.info("Reservando para saga {}", s.sagaId());
                    return repositorio.save(new ReservaAuto(s.sagaId(), s.ciudad(), s.fechaInicio(), s.fechaFin(), EstadoReserva.CONFIRMADA));
                });
    }

    /**
     * Compensación idempotente. Si la reserva no existe (p. ej. la petición original
     * nunca llegó) se deja una "lápida" CANCELADA para rechazar una reserva tardía.
     */
    @Transactional
    public ReservaAuto cancelar(String sagaId) {
        ReservaAuto reserva = repositorio.findBySagaId(sagaId)
                .orElseGet(() -> new ReservaAuto(sagaId, null, null, null, EstadoReserva.CANCELADA));
        reserva.cancelar();
        log.info("Compensación: reserva de saga {} CANCELADA", sagaId);
        return repositorio.save(reserva);
    }

    public List<ReservaAuto> listar() {
        return repositorio.findAll();
    }
}
