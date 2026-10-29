package co.edu.unicauca.saga.orquestador.modelo;

/**
 * Resultado final de la saga.
 *
 * @param estado CONFIRMADA o CANCELADA (compensada)
 */
public record ResultadoReserva(
        String sagaId,
        String estado,
        String reservaVuelo,
        String reservaHotel,
        String reservaAuto,
        String pago,
        boolean notificacionEnviada,
        String motivo) {
}
