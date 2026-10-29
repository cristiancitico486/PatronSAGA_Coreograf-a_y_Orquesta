package co.edu.unicauca.saga.notificaciones;

import jakarta.validation.constraints.NotBlank;

public record SolicitudNotificacion(@NotBlank String sagaId, @NotBlank String tipo, @NotBlank String email,
        String asunto, String mensaje) {
}
