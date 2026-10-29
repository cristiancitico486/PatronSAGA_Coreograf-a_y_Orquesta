package co.edu.unicauca.saga.notificaciones;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService servicio;

    public NotificacionController(NotificacionService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public Notificacion enviar(@Valid @RequestBody SolicitudNotificacion solicitud) {
        return servicio.enviar(solicitud);
    }

    /** Endpoint de pruebas: los próximos N envíos responderán 503. */
    @PostMapping("/simular-fallos")
    public Map<String, Integer> simularFallos(@RequestParam("veces") int veces) {
        return Map.of("fallosPendientes", servicio.simularFallos(veces));
    }

    @GetMapping
    public List<Notificacion> listar() {
        return servicio.listar();
    }
}
