package co.edu.unicauca.saga.autos;

import jakarta.validation.constraints.NotBlank;

public record SolicitudReservaAuto(@NotBlank String sagaId, String ciudad, String fechaInicio, String fechaFin) {
}
