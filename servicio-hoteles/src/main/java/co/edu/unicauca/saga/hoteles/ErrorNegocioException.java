package co.edu.unicauca.saga.hoteles;

import org.springframework.http.HttpStatus;

/**
 * Error con código HTTP. El orquestador trata los 4xx como errores de negocio (no reintentables)
 * y los 5xx como fallos temporales (reintentables).
 */
public class ErrorNegocioException extends RuntimeException {

    private final HttpStatus estado;
    private final String codigo;

    public ErrorNegocioException(HttpStatus estado, String codigo, String mensaje) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    public HttpStatus getEstado() {
        return estado;
    }

    public String getCodigo() {
        return codigo;
    }
}
