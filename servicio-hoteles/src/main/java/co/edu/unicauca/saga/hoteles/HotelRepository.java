package co.edu.unicauca.saga.hoteles;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface HotelRepository extends JpaRepository<Hotel, String> {

    /** Bloqueo pesimista para que dos sagas no tomen el último cupo a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Hotel h where h.codigo = :codigo")
    Optional<Hotel> bloquear(@Param("codigo") String codigo);
}
