package co.edu.unicauca.saga.hoteles;

import jakarta.validation.constraints.NotBlank;

public record SolicitudReservaHotel(@NotBlank String sagaId, @NotBlank String codigoHotel,
        String fechaEntrada, String fechaSalida) {
}
