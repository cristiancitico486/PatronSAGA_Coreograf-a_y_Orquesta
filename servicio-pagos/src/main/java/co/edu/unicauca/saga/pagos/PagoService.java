package co.edu.unicauca.saga.pagos;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    /** Tarjeta de prueba que la pasarela simulada siempre rechaza (fondos insuficientes). */
    static final String TARJETA_RECHAZADA = "4000000000000002";
    static final BigDecimal LIMITE = new BigDecimal("20000000");

    private final PagoRepository pagos;

    public PagoService(PagoRepository pagos) {
        this.pagos = pagos;
    }

    /** Idempotente: un único cobro por saga. El rechazo queda registrado (no hace rollback). */
    @Transactional(noRollbackFor = ErrorNegocioException.class)
    public Pago cobrar(SolicitudPago s) {
        var existente = pagos.findBySagaId(s.sagaId());
        if (existente.isPresent()) {
            Pago pago = existente.get();
            if (pago.getEstado() == EstadoPago.APROBADO) {
                log.info("Cobro repetido para saga {} (idempotencia): no se cobra dos veces", s.sagaId());
                return pago;
            }
            throw rechazo(pago);
        }

        String tarjeta = s.numeroTarjeta().replaceAll("[^0-9]", "");
        String mascara = "****" + tarjeta.substring(Math.max(0, tarjeta.length() - 4));
        String motivo = null;
        if (tarjeta.equals(TARJETA_RECHAZADA)) {
            motivo = "Fondos insuficientes";
        } else if (s.monto().compareTo(LIMITE) > 0) {
            motivo = "Monto supera el cupo de la tarjeta";
        }
        if (motivo != null) {
            log.warn("Pago RECHAZADO para saga {}: {}", s.sagaId(), motivo);
            throw rechazo(pagos.save(new Pago(s.sagaId(), mascara, s.monto(), EstadoPago.RECHAZADO, motivo)));
        }
        log.info("Pago APROBADO para saga {} por {}", s.sagaId(), s.monto());
        return pagos.save(new Pago(s.sagaId(), mascara, s.monto(), EstadoPago.APROBADO, null));
    }

    /** Compensación idempotente: reembolsa solo lo que efectivamente se cobró. */
    @Transactional
    public Pago reembolsar(String sagaId) {
        var existente = pagos.findBySagaId(sagaId);
        if (existente.isEmpty()) {
            log.info("Reembolso sin cobro previo para saga {}: se registra lápida", sagaId);
            return pagos.save(new Pago(sagaId, null, BigDecimal.ZERO, EstadoPago.ANULADO, "Cobro nunca realizado"));
        }
        Pago pago = existente.get();
        if (pago.getEstado() == EstadoPago.APROBADO) {
            pago.cambiarEstado(EstadoPago.REEMBOLSADO, "Compensación de la saga");
            log.info("Compensación: pago de saga {} REEMBOLSADO", sagaId);
        }
        return pago;
    }

    public List<Pago> listar() {
        return pagos.findAll();
    }

    private ErrorNegocioException rechazo(Pago pago) {
        String codigo = pago.getEstado() == EstadoPago.RECHAZADO ? "PAGO_RECHAZADO" : "PAGO_ANULADO";
        return new ErrorNegocioException(HttpStatus.PAYMENT_REQUIRED, codigo, pago.getMotivo());
    }
}
