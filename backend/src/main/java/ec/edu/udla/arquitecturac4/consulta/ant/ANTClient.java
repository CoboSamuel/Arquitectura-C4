package ec.edu.udla.arquitecturac4.consulta.ant;

import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ANTClient {

    private final RestClient restClient;
    private final AntProperties properties;
    private final AntHtmlParser parser;

    public ANTClient(
            @Qualifier("antRestClient") RestClient restClient,
            AntProperties properties,
            AntHtmlParser parser) {
        this.restClient = restClient;
        this.properties = properties;
        this.parser = parser;
    }

    public Optional<BigDecimal> consultarPuntos(String cedula, String placa) {
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(properties.path())
                            .queryParam("ps_tipo_identificacion", "CED")
                            .queryParam("ps_identificacion", "{cedula}")
                            .queryParam("ps_placa", "{placa}")
                            .build(Map.of("cedula", cedula, "placa", placa)))
                    .accept(MediaType.TEXT_HTML)
                    .retrieve()
                    .toEntity(String.class);

            if (response.getStatusCode().value() != HttpStatus.OK.value()) {
                throw new AntRespuestaInvalidaException();
            }
            return parser.parse(response.getBody());
        } catch (RestClientResponseException exception) {
            throw mapUpstreamStatus(exception);
        } catch (RestClientException exception) {
            throw mapClientFailure(exception);
        }
    }

    private RuntimeException mapClientFailure(RestClientException exception) {
        if (hasCause(exception, ProtocolException.class)) {
            return new AntRespuestaInvalidaException();
        }
        if (hasCause(exception, SocketTimeoutException.class)
                || hasCause(exception, HttpTimeoutException.class)
                || exception instanceof ResourceAccessException) {
            return new AntNoDisponibleException();
        }
        return new AntRespuestaInvalidaException();
    }

    private boolean hasCause(Throwable exception, Class<? extends Throwable> type) {
        Throwable current = exception;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private RuntimeException mapUpstreamStatus(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        if (status == HttpStatus.REQUEST_TIMEOUT.value()
                || status == HttpStatus.TOO_MANY_REQUESTS.value()
                || status >= 500) {
            return new AntNoDisponibleException();
        }
        return new AntRespuestaInvalidaException();
    }
}
