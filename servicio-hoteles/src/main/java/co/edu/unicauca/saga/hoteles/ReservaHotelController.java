package co.edu.unicauca.saga.hoteles;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/hoteles")
public class ReservaHotelController {

    private final ReservaHotelService servicio;

    public ReservaHotelController(ReservaHotelService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/reservas")
    public ReservaHotel reservar(@Valid @RequestBody SolicitudReservaHotel solicitud) {
        return servicio.reservar(solicitud);
    }

    @PostMapping("/reservas/{sagaId}/cancelar")
    public ReservaHotel cancelar(@PathVariable String sagaId) {
        return servicio.cancelar(sagaId);
    }

    @GetMapping("/reservas")
    public List<ReservaHotel> listar() {
        return servicio.listar();
    }

    @GetMapping
    public List<Hotel> hoteles() {
        return servicio.hoteles();
    }
}
