package co.edu.unicauca.saga.hoteles;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class ReservaHotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador de la saga: clave de idempotencia. */
    @Column(nullable = false, unique = true)
    private String sagaId;

    private String codigoHotel;

    private String fechaEntrada;

    private String fechaSalida;

    @Enumerated(EnumType.STRING)
    private EstadoReserva estado;

    private Instant actualizada;

    protected ReservaHotel() {
    }

    public ReservaHotel(String sagaId, String codigoHotel, String fechaEntrada, String fechaSalida,
            EstadoReserva estado) {
        this.sagaId = sagaId;
        this.codigoHotel = codigoHotel;
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
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

    public String getCodigoHotel() {
        return codigoHotel;
    }

    public String getFechaEntrada() {
        return fechaEntrada;
    }

    public String getFechaSalida() {
        return fechaSalida;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public Instant getActualizada() {
        return actualizada;
    }
}
