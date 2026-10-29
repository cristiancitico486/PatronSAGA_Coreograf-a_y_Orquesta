package co.edu.unicauca.saga.autos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaAutoRepository extends JpaRepository<ReservaAuto, Long> {

    Optional<ReservaAuto> findBySagaId(String sagaId);
}
