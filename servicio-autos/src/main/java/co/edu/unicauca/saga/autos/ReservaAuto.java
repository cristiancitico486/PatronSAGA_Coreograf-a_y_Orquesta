package co.edu.unicauca.saga.autos;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class ReservaAuto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador de la saga: clave de idempotencia. */
    @Column(nullable = false, unique = true)
    private String sagaId;

    private String ciudad;

    private String fechaInicio;

    private String fechaFin;

    @Enumerated(EnumType.STRING)
    private EstadoReserva estado;

    private Instant actualizada;

    protected ReservaAuto() {
    }

    public ReservaAuto(String sagaId, String ciudad, String fechaInicio, String fechaFin, EstadoReserva estado) {
        this.sagaId = sagaId;
        this.ciudad = ciudad;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
        this.actualizada = Instant.now();
    }

    public void cancelar() {
        this.estado = EstadoReserva.CANCELADA;
        this.actualizada = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getSagaId() {
        return sagaId;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getFechaInicio() {
        return fechaInicio;
    }

    public String getFechaFin() {
        return fechaFin;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public Instant getActualizada() {
        return actualizada;
    }
}
