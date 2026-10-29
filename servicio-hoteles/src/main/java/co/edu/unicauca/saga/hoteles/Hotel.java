package co.edu.unicauca.saga.hoteles;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Hotel {

    @Id
    private String codigo;

    private String nombre;

    private int cuposDisponibles;

    protected Hotel() {
    }

    public Hotel(String codigo, String nombre, int cuposDisponibles) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.cuposDisponibles = cuposDisponibles;
    }

    public void ocuparCupo() {
        cuposDisponibles--;
    }

    public void liberarCupo() {
        cuposDisponibles++;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCuposDisponibles() {
        return cuposDisponibles;
    }
}
