package co.edu.unicauca.saga.orquestador.api;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unicauca.saga.orquestador.modelo.EstadoSaga;
import co.edu.unicauca.saga.orquestador.modelo.ResultadoReserva;
import co.edu.unicauca.saga.orquestador.modelo.SolicitudReserva;
import co.edu.unicauca.saga.orquestador.workflow.ReservaViajeWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final WorkflowClient cliente;
    private final String taskQueue;

    public ReservaController(WorkflowClient cliente, @Value("${temporal.task-queue}") String taskQueue) {
        this.cliente = cliente;
        this.taskQueue = taskQueue;
    }

    /** Inicia la saga de forma asíncrona y devuelve su identificador. */
    @PostMapping
    public ResponseEntity<Map<String, String>> reservar(@RequestBody SolicitudReserva solicitud) {
        String sagaId = "reserva-" + UUID.randomUUID().toString().substring(0, 8);
        ReservaViajeWorkflow saga = cliente.newWorkflowStub(ReservaViajeWorkflow.class,
                WorkflowOptions.newBuilder().setWorkflowId(sagaId).setTaskQueue(taskQueue).build());
        WorkflowClient.start(saga::reservar, solicitud);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "sagaId", sagaId,
                "estado", "/api/reservas/" + sagaId,
                "resultado", "/api/reservas/" + sagaId + "/resultado"));
    }

    /** Estado actual (query al workflow). Requiere un worker activo. */
    @GetMapping("/{sagaId}")
    public EstadoSaga estado(@PathVariable String sagaId) {
        return cliente.newWorkflowStub(ReservaViajeWorkflow.class, sagaId).estado();
    }

    /** Espera el resultado final hasta {@code segundos}; 202 si la saga aún no termina. */
    @GetMapping("/{sagaId}/resultado")
    public ResponseEntity<?> resultado(@PathVariable String sagaId,
            @RequestParam(name = "segundos", defaultValue = "30") long segundos) {
        try {
            ResultadoReserva resultado = cliente.newUntypedWorkflowStub(sagaId)
                    .getResult(segundos, TimeUnit.SECONDS, ResultadoReserva.class);
            return ResponseEntity.ok(resultado);
        } catch (TimeoutException e) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("sagaId", sagaId, "estado", "EN_CURSO"));
        }
    }
}
