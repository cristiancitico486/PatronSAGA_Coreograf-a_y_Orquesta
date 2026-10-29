package co.edu.unicauca.saga.vuelos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaVueloRepository extends JpaRepository<ReservaVuelo, Long> {

    Optional<ReservaVuelo> findBySagaId(String sagaId);
}
