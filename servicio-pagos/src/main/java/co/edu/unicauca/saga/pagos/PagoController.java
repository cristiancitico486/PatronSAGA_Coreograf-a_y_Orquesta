package co.edu.unicauca.saga.pagos;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService servicio;

    public PagoController(PagoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public Pago cobrar(@Valid @RequestBody SolicitudPago solicitud) {
        return servicio.cobrar(solicitud);
    }

    @PostMapping("/{sagaId}/reembolsar")
    public Pago reembolsar(@PathVariable String sagaId) {
        return servicio.reembolsar(sagaId);
    }

    @GetMapping
    public List<Pago> listar() {
        return servicio.listar();
    }
}
