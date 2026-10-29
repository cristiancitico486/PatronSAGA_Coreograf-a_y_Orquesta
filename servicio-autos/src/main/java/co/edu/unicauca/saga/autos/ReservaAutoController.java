package co.edu.unicauca.saga.autos;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/autos/reservas")
public class ReservaAutoController {

    private final ReservaAutoService servicio;

    public ReservaAutoController(ReservaAutoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ReservaAuto reservar(@Valid @RequestBody SolicitudReservaAuto solicitud) {
        return servicio.reservar(solicitud);
    }

    @PostMapping("/{sagaId}/cancelar")
    public ReservaAuto cancelar(@PathVariable String sagaId) {
        return servicio.cancelar(sagaId);
    }

    @GetMapping
    public List<ReservaAuto> listar() {
        return servicio.listar();
    }
}
