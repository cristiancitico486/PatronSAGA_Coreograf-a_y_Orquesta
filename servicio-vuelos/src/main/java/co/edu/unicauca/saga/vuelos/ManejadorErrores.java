package co.edu.unicauca.saga.vuelos;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ErrorNegocioException.class)
    public ResponseEntity<Map<String, String>> negocio(ErrorNegocioException e) {
        return ResponseEntity.status(e.getEstado())
                .body(Map.of("codigo", e.getCodigo(), "mensaje", e.getMessage()));
    }
}
