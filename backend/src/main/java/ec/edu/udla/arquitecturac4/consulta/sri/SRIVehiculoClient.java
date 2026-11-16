package ec.edu.udla.arquitecturac4.consulta.sri;

import com.fasterxml.jackson.databind.JsonNode;
import ec.edu.udla.arquitecturac4.consulta.SriNoDisponibleException;
import ec.edu.udla.arquitecturac4.consulta.SriRespuestaInvalidaException;
import ec.edu.udla.arquitecturac4.consulta.sri.dto.SriVehiculoData;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.List;
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
public class SRIVehiculoClient {

    private final RestClient restClient;
    private final SriProperties properties;

    public SRIVehiculoClient(@Qualifier("sriRestClient") RestClient sriRestClient, SriProperties properties) {
        this.restClient = sriRestClient;
        this.properties = properties;
    }

    public Optional<SriVehiculoData> buscar(String placa) {
        try {
            ResponseEntity<JsonNode> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(properties.vehiclePath())
                            .queryParam("numeroPlacaCampvCpn", placa)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(JsonNode.class);

            if (response.getStatusCode().value() == HttpStatus.NO_CONTENT.value()) {
                return Optional.empty();
            }
            if (response.getStatusCode().value() != HttpStatus.OK.value()) {
                throw new SriRespuestaInvalidaException();
            }

            return parseEnvelope(response.getBody(), placa);
        } catch (RestClientResponseException exception) {
            throw mapUpstreamStatus(exception);
        } catch (RestClientException exception) {
            throw mapClientFailure(exception);
        }
    }

    private Optional<SriVehiculoData> parseEnvelope(JsonNode body, String requestedPlate) {
        if (body == null || !body.isObject()) {
            throw new SriRespuestaInvalidaException();
        }

        JsonNode object = body.get("objeto");
        if (object != null && !object.isNull()) {
            if (!object.isObject()) {
                throw new SriRespuestaInvalidaException();
            }
            return Optional.of(toVehicle(object, requestedPlate));
        }

        JsonNode data = body.get("data");
        if (data == null || !data.isArray()) {
            throw new SriRespuestaInvalidaException();
        }
        if (data.isEmpty()) {
            return Optional.empty();
        }

        List<JsonNode> records = new ArrayList<>();
        data.forEach(record -> {
            if (!record.isObject()) {
                throw new SriRespuestaInvalidaException();
            }
            records.add(record);
        });

        if (records.size() == 1) {
            return Optional.of(toVehicle(records.getFirst(), requestedPlate));
        }

        List<JsonNode> matches = records.stream()
                .filter(record -> requestedPlate.equalsIgnoreCase(textValue(record.get("numeroPlaca"))))
                .toList();
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        if (matches.size() > 1) {
            throw new SriRespuestaInvalidaException();
        }
        return Optional.of(toVehicle(matches.getFirst(), requestedPlate));
    }

    private SriVehiculoData toVehicle(JsonNode record, String requestedPlate) {
        String responsePlate = optionalText(record, "numeroPlaca");
        if (responsePlate != null && !requestedPlate.equalsIgnoreCase(responsePlate)) {
            throw new SriRespuestaInvalidaException();
        }

        return new SriVehiculoData(
                responsePlate == null ? requestedPlate : responsePlate,
                requiredText(record, "marca"),
                requiredText(record, "modelo"),
                requiredText(record, "anioModelo"),
                requiredText(record, "claseVehiculo"),
                optionalText(record, "color"),
                optionalText(record, "cilindraje"));
    }

    private String requiredText(JsonNode record, String field) {
        String value = optionalText(record, field);
        if (value == null || value.isBlank()) {
            throw new SriRespuestaInvalidaException();
        }
        return value;
    }

    private String optionalText(JsonNode record, String field) {
        JsonNode value = record.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isValueNode() || value.isBoolean()) {
            throw new SriRespuestaInvalidaException();
        }
        String text = value.asText().trim();
        return text.isEmpty() ? null : text;
    }

    private String textValue(JsonNode value) {
        if (value == null || value.isNull() || !value.isValueNode() || value.isBoolean()) {
            return null;
        }
        return value.asText().trim();
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
