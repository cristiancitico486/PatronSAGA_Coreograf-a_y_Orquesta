package co.edu.unicauca.saga.orquestador.workflow;

import co.edu.unicauca.saga.orquestador.modelo.EstadoSaga;
import co.edu.unicauca.saga.orquestador.modelo.ResultadoReserva;
import co.edu.unicauca.saga.orquestador.modelo.SolicitudReserva;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ReservaViajeWorkflow {

    @WorkflowMethod
    ResultadoReserva reservar(SolicitudReserva solicitud);

    @QueryMethod
    EstadoSaga estado();
}
