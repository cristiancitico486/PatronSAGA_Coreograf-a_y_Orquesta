package co.edu.unicauca.saga.orquestador.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import co.edu.unicauca.saga.orquestador.actividades.NotificacionActivities;
import co.edu.unicauca.saga.orquestador.actividades.ServiciosViajeActivities;
import co.edu.unicauca.saga.orquestador.workflow.ReservaViajeWorkflowImpl;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

/** Registra el workflow y las actividades y arranca el worker cuando Spring terminó de iniciar. */
@Component
public class IniciadorWorker {

    private static final Logger log = LoggerFactory.getLogger(IniciadorWorker.class);

    private final WorkerFactory factory;
    private final ServiciosViajeActivities servicios;
    private final NotificacionActivities notificaciones;
    private final String taskQueue;
    private final boolean habilitado;

    public IniciadorWorker(WorkerFactory factory, ServiciosViajeActivities servicios,
            NotificacionActivities notificaciones,
            @Value("${temporal.task-queue}") String taskQueue,
            @Value("${temporal.worker-habilitado}") boolean habilitado) {
        this.factory = factory;
        this.servicios = servicios;
        this.notificaciones = notificaciones;
        this.taskQueue = taskQueue;
        this.habilitado = habilitado;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void iniciar() {
        if (!habilitado) {
            log.info("Worker deshabilitado: esta instancia solo expone la API");
            return;
        }
        Worker worker = factory.newWorker(taskQueue);
        worker.registerWorkflowImplementationTypes(ReservaViajeWorkflowImpl.class);
        worker.registerActivitiesImplementations(servicios, notificaciones);
        factory.start();
        log.info("Worker de Temporal escuchando la cola {}", taskQueue);
    }
}
