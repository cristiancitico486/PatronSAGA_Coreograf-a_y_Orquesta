package co.edu.unicauca.saga.vuelos;

import jakarta.validation.constraints.NotBlank;

public record SolicitudReservaVuelo(@NotBlank String sagaId, String origen, String destino, String fecha) {
}
