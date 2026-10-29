package co.edu.unicauca.saga.notificaciones;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "sagaId", "tipo" }))
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sagaId;

    /** CONFIRMACION o CANCELACION: una de cada tipo por saga. */
    @Column(nullable = false)
    private String tipo;

    private String email;

    private String asunto;

    @Column(length = 2000)
    private String mensaje;

    private Instant enviada;

    protected Notificacion() {
    }

    public Notificacion(String sagaId, String tipo, String email, String asunto, String mensaje) {
        this.sagaId = sagaId;
        this.tipo = tipo;
        this.email = email;
        this.asunto = asunto;
        this.mensaje = mensaje;
        this.enviada = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getSagaId() {
        return sagaId;
    }

    public String getTipo() {
        return tipo;
    }

    public String getEmail() {
        return email;
    }

    public String getAsunto() {
        return asunto;
    }

    public String getMensaje() {
        return mensaje;
    }

    public Instant getEnviada() {
        return enviada;
    }
}
