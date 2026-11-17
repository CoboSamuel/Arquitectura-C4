package ec.edu.udla.arquitecturac4.consulta.sri;

import com.fasterxml.jackson.databind.JsonNode;
import ec.edu.udla.arquitecturac4.consulta.SriNoDisponibleException;
import ec.edu.udla.arquitecturac4.consulta.SriRespuestaInvalidaException;
import ec.edu.udla.arquitecturac4.consulta.sri.dto.SriContribuyenteResponse;
import ec.edu.udla.arquitecturac4.consulta.spi.ContribuyenteProvider;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Profile("live")
public class SRIContribuyenteClient implements ContribuyenteProvider {

    private final RestClient restClient;
    private final SriProperties properties;

    public SRIContribuyenteClient(@Qualifier("sriRestClient") RestClient sriRestClient, SriProperties properties) {
        this.restClient = sriRestClient;
        this.properties = properties;
    }

    @Override
    public boolean existe(String ruc) {
        try {
            ResponseEntity<JsonNode> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(properties.existsPath())
                            .queryParam("numeroRuc", ruc)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(JsonNode.class);

            JsonNode body = response.getBody();
            if (response.getStatusCode().value() != HttpStatus.OK.value() || body == null || !body.isBoolean()) {
                throw new SriRespuestaInvalidaException();
            }
            return body.booleanValue();
        } catch (RestClientResponseException exception) {
            throw mapUpstreamStatus(exception);
        } catch (RestClientException exception) {
            throw mapClientFailure(exception);
        }
    }

    @Override
    public List<SriContribuyenteResponse> obtener(String ruc) {
        try {
            ResponseEntity<List<SriContribuyenteResponse>> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(properties.detailsPath())
                            .queryParam("ruc", ruc)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<>() {});

            if (response.getStatusCode().value() == HttpStatus.NO_CONTENT.value()) {
                return List.of();
            }
            if (response.getStatusCode().value() != HttpStatus.OK.value() || response.getBody() == null) {
                throw new SriRespuestaInvalidaException();
            }
            List<SriContribuyenteResponse> body = response.getBody();
            if (body.stream().anyMatch(Objects::isNull)) {
                throw new SriRespuestaInvalidaException();
            }
            return List.copyOf(body);
        } catch (RestClientResponseException exception) {
            throw mapUpstreamStatus(exception);
        } catch (RestClientException exception) {
            throw mapClientFailure(exception);
        }
    }

    private RuntimeException mapClientFailure(RestClientException exception) {
        if (hasCause(exception, ProtocolException.class)) {
            return new SriRespuestaInvalidaException();
        }
        if (hasCause(exception, SocketTimeoutException.class)
                || hasCause(exception, HttpTimeoutException.class)
                || exception instanceof ResourceAccessException) {
            return new SriNoDisponibleException();
        }
        return new SriRespuestaInvalidaException();
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
            return new SriNoDisponibleException();
        }
        return new SriRespuestaInvalidaException();
    }
}
