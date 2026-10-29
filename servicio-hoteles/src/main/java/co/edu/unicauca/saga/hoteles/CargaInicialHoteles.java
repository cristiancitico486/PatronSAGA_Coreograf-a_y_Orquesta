package co.edu.unicauca.saga.hoteles;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Catálogo de prueba: HOTEL-LLENO no tiene cupo para provocar la compensación. */
@Component
public class CargaInicialHoteles implements CommandLineRunner {

    private final HotelRepository hoteles;

    public CargaInicialHoteles(HotelRepository hoteles) {
        this.hoteles = hoteles;
    }

    @Override
    public void run(String... args) {
        if (hoteles.count() == 0) {
            hoteles.save(new Hotel("HOTEL-CENTRO", "Hotel Centro Popayán", 10));
            hoteles.save(new Hotel("HOTEL-PLAYA", "Hotel Playa Cartagena", 5));
            hoteles.save(new Hotel("HOTEL-LLENO", "Hotel Lleno Bogotá", 0));
        }
    }
}
