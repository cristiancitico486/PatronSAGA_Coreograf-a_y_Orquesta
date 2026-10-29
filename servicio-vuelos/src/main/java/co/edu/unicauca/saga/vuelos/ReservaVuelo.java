package co.edu.unicauca.saga.vuelos;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class ReservaVuelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador de la saga: clave de idempotencia. */
    @Column(nullable = false, unique = true)
    private String sagaId;

    private String origen;

    private String destino;

    private String fecha;

    @Enumerated(EnumType.STRING)
    private EstadoReserva estado;

    private Instant actualizada;

    protected ReservaVuelo() {
    }

    public ReservaVuelo(String sagaId, String origen, String destino, String fecha, EstadoReserva estado) {
        this.sagaId = sagaId;
        this.origen = origen;
        this.destino = destino;
        this.fecha = fecha;
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

    public String getOrigen() {
        return origen;
    }

    public String getDestino() {
        return destino;
    }

    public String getFecha() {
        return fecha;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public Instant getActualizada() {
        return actualizada;
    }
}
