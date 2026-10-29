package co.edu.unicauca.saga.orquestador.actividades;

import java.time.Duration;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import io.temporal.failure.ApplicationFailure;

/**
 * Cliente HTTP de los microservicios. Traduce las respuestas a fallos de Temporal:
 * <ul>
 * <li>4xx (hotel sin cupo, pago rechazado...): error de negocio, NO reintentable.</li>
 * <li>5xx o servicio caído: fallo temporal, reintentable según la RetryPolicy.</li>
 * </ul>
 */
@Component
public class ClienteServicios {

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() {
    };

    private final RestClient rest;

    public ClienteServicios(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(2));
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        this.rest = builder.requestFactory(fabrica).build();
    }

    public Map<String, Object> post(String servicio, String url, Object cuerpo) {
        try {
            RestClient.RequestBodySpec peticion = rest.post().uri(url);
            if (cuerpo != null) {
                peticion.body(cuerpo);
            }
            return peticion.retrieve().body(MAPA);
        } catch (HttpClientErrorException e) {
            Map<String, Object> error = leerError(e);
            throw ApplicationFailure.newNonRetryableFailure(servicio + ": " + error.get("mensaje"),
                    String.valueOf(error.get("codigo")));
        } catch (HttpServerErrorException e) {
            Map<String, Object> error = leerError(e);
            throw ApplicationFailure.newFailure(servicio + " respondió " + e.getStatusCode().value() + ": "
                    + error.get("mensaje"), String.valueOf(error.get("codigo")));
        } catch (ResourceAccessException e) {
            throw ApplicationFailure.newFailure(servicio + " no disponible: " + e.getMessage(),
                    "SERVICIO_NO_DISPONIBLE");
        }
    }

    private static Map<String, Object> leerError(RestClientResponseException e) {
        try {
            Map<String, Object> error = e.getResponseBodyAs(MAPA);
            if (error != null && error.containsKey("codigo")) {
                return error;
            }
        } catch (RuntimeException ignorada) {
            // cuerpo no JSON: se usa el estado HTTP
        }
        return Map.of("codigo", "HTTP_" + e.getStatusCode().value(), "mensaje", e.getStatusText());
    }
}
