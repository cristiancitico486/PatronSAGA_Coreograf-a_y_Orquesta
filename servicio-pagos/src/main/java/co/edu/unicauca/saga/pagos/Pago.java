package co.edu.unicauca.saga.pagos;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador de la saga: clave de idempotencia (un único cobro por saga). */
    @Column(nullable = false, unique = true)
    private String sagaId;

    private String tarjetaEnmascarada;

    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    private EstadoPago estado;

    private String motivo;

    private Instant actualizado;

    protected Pago() {
    }

    public Pago(String sagaId, String tarjetaEnmascarada, BigDecimal monto, EstadoPago estado, String motivo) {
        this.sagaId = sagaId;
        this.tarjetaEnmascarada = tarjetaEnmascarada;
        this.monto = monto;
        this.estado = estado;
        this.motivo = motivo;
        this.actualizado = Instant.now();
    }

    public void cambiarEstado(EstadoPago estado, String motivo) {
        this.estado = estado;
        this.motivo = motivo;
        this.actualizado = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getSagaId() {
        return sagaId;
    }

    public String getTarjetaEnmascarada() {
        return tarjetaEnmascarada;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public String getMotivo() {
        return motivo;
    }

    public Instant getActualizado() {
        return actualizado;
    }
}
