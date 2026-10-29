package co.edu.unicauca.saga.vuelos;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/vuelos/reservas")
public class ReservaVueloController {

    private final ReservaVueloService servicio;

    public ReservaVueloController(ReservaVueloService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ReservaVuelo reservar(@Valid @RequestBody SolicitudReservaVuelo solicitud) {
        return servicio.reservar(solicitud);
    }

    @PostMapping("/{sagaId}/cancelar")
    public ReservaVuelo cancelar(@PathVariable String sagaId) {
        return servicio.cancelar(sagaId);
    }

    @GetMapping
    public List<ReservaVuelo> listar() {
        return servicio.listar();
    }
}
