package co.edu.unicauca.saga.notificaciones;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository notificaciones;

    /** Cantidad de envíos que fallarán con 503 (simula caída temporal del servidor SMTP). */
    private final AtomicInteger fallosPendientes = new AtomicInteger();

    public NotificacionService(NotificacionRepository notificaciones) {
        this.notificaciones = notificaciones;
    }

    /** Idempotente: un correo de cada tipo por saga. */
    @Transactional
    public Notificacion enviar(SolicitudNotificacion s) {
        if (fallosPendientes.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
            log.warn("Servidor de correo no disponible (fallo simulado) para saga {}", s.sagaId());
            throw new ErrorNegocioException(HttpStatus.SERVICE_UNAVAILABLE, "SMTP_NO_DISPONIBLE",
                    "Servidor de correo temporalmente no disponible");
        }
        return notificaciones.findBySagaIdAndTipo(s.sagaId(), s.tipo())
                .map(existente -> {
                    log.info("Correo {} ya enviado para saga {} (idempotencia)", s.tipo(), s.sagaId());
                    return existente;
                })
                .orElseGet(() -> {
                    log.info("Enviando correo {} a {} para saga {}", s.tipo(), s.email(), s.sagaId());
                    return notificaciones.save(
                            new Notificacion(s.sagaId(), s.tipo(), s.email(), s.asunto(), s.mensaje()));
                });
    }

    public int simularFallos(int veces) {
        fallosPendientes.set(veces);
        log.warn("Los próximos {} envíos de correo fallarán", veces);
        return veces;
    }

    public List<Notificacion> listar() {
        return notificaciones.findAll();
    }
}
