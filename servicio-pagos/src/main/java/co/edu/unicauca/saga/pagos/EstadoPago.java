package co.edu.unicauca.saga.pagos;

public enum EstadoPago {
    APROBADO,
    RECHAZADO,
    REEMBOLSADO,
    /** Lápida: se pidió reembolso de un cobro que nunca llegó. */
    ANULADO
}
