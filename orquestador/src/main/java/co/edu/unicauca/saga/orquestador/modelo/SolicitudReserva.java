package co.edu.unicauca.saga.orquestador.modelo;

import java.math.BigDecimal;

/**
 * Paquete de viaje solicitado por el cliente.
 *
 * @param pausaEntrePasosSegundos solo para pruebas: espera durable (timer de Temporal) después de
 *                                cada paso, para tener tiempo de tumbar el worker a mitad de la saga.
 */
public record SolicitudReserva(
        String cliente,
        String email,
        String origen,
        String destino,
        String fechaIda,
        String fechaRegreso,
        String codigoHotel,
        String numeroTarjeta,
        BigDecimal monto,
        int pausaEntrePasosSegundos) {
}
