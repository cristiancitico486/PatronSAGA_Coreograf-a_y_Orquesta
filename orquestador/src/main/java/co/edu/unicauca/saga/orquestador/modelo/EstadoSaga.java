package co.edu.unicauca.saga.orquestador.modelo;

import java.util.ArrayList;
import java.util.List;

/** Estado consultable (query) de la saga mientras se ejecuta y después de terminar. */
public class EstadoSaga {

    private String sagaId;
    private String paso = "INICIADA";
    private List<String> historial = new ArrayList<>();
    private ResultadoReserva resultado;

    public EstadoSaga() {
    }

    public EstadoSaga(String sagaId) {
        this.sagaId = sagaId;
    }

    public void registrar(String paso, String detalle) {
        this.paso = paso;
        historial.add(paso + (detalle == null ? "" : " - " + detalle));
    }

    public String getSagaId() {
        return sagaId;
    }

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
    }

    public String getPaso() {
        return paso;
    }

    public void setPaso(String paso) {
        this.paso = paso;
    }

    public List<String> getHistorial() {
        return historial;
    }

    public void setHistorial(List<String> historial) {
        this.historial = historial;
    }

    public ResultadoReserva getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoReserva resultado) {
        this.resultado = resultado;
    }
}
