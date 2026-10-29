package co.edu.unicauca.saga.pagos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SolicitudPago(@NotBlank String sagaId, @NotBlank String numeroTarjeta,
        @NotNull @Positive BigDecimal monto) {
}
