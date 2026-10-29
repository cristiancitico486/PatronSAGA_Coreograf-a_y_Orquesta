package co.edu.unicauca.saga.hoteles;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservaHotelService {

    private static final Logger log = LoggerFactory.getLogger(ReservaHotelService.class);

    private final ReservaHotelRepository reservas;
    private final HotelRepository hoteles;

    public ReservaHotelService(ReservaHotelRepository reservas, HotelRepository hoteles) {
        this.reservas = reservas;
        this.hoteles = hoteles;
    }

    /** Idempotente: si la saga ya reservó, devuelve la misma reserva sin ocupar otro cupo. */
    @Transactional
    public ReservaHotel reservar(SolicitudReservaHotel s) {
        var existente = reservas.findBySagaId(s.sagaId());
        if (existente.isPresent()) {
            if (existente.get().getEstado() == EstadoReserva.CANCELADA) {
                throw new ErrorNegocioException(HttpStatus.CONFLICT, "RESERVA_CANCELADA",
                        "La saga " + s.sagaId() + " ya fue compensada");
            }
            log.info("Reserva repetida para saga {} (idempotencia)", s.sagaId());
            return existente.get();
        }

        Hotel hotel = hoteles.bloquear(s.codigoHotel())
                .orElseThrow(() -> new ErrorNegocioException(HttpStatus.NOT_FOUND, "HOTEL_NO_EXISTE",
                        "No existe el hotel " + s.codigoHotel()));
        if (hotel.getCuposDisponibles() <= 0) {
            log.warn("Hotel {} sin cupo para saga {}", hotel.getCodigo(), s.sagaId());
            throw new ErrorNegocioException(HttpStatus.CONFLICT, "HOTEL_SIN_CUPO",
                    "El hotel " + hotel.getNombre() + " no tiene cupo disponible");
        }
        hotel.ocuparCupo();
        log.info("Reservando hotel {} para saga {} (quedan {} cupos)", hotel.getCodigo(), s.sagaId(),
                hotel.getCuposDisponibles());
        return reservas.save(new ReservaHotel(s.sagaId(), s.codigoHotel(), s.fechaEntrada(), s.fechaSalida(),
                EstadoReserva.CONFIRMADA));
    }

    /**
     * Compensación idempotente: libera el cupo solo la primera vez. Si la reserva no existe
     * se deja una "lápida" CANCELADA para rechazar una reserva tardía.
     */
    @Transactional
    public ReservaHotel cancelar(String sagaId) {
        var existente = reservas.findBySagaId(sagaId);
        if (existente.isEmpty()) {
            log.info("Compensación sin reserva previa para saga {}: se registra lápida", sagaId);
            return reservas.save(new ReservaHotel(sagaId, null, null, null, EstadoReserva.CANCELADA));
        }
        ReservaHotel reserva = existente.get();
        if (reserva.getEstado() == EstadoReserva.CONFIRMADA) {
            hoteles.bloquear(reserva.getCodigoHotel()).ifPresent(Hotel::liberarCupo);
            reserva.cancelar();
            log.info("Compensación: reserva de hotel de saga {} CANCELADA, cupo liberado", sagaId);
        }
        return reserva;
    }

    public List<ReservaHotel> listar() {
        return reservas.findAll();
    }

    public List<Hotel> hoteles() {
        return hoteles.findAll();
    }
}
