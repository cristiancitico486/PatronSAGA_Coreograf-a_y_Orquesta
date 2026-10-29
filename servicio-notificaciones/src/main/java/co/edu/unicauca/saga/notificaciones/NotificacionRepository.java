package co.edu.unicauca.saga.notificaciones;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Optional<Notificacion> findBySagaIdAndTipo(String sagaId, String tipo);
}
