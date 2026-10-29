package co.edu.unicauca.saga.hoteles;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaHotelRepository extends JpaRepository<ReservaHotel, Long> {

    Optional<ReservaHotel> findBySagaId(String sagaId);
}
