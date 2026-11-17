package ec.edu.udla.arquitecturac4.consulta.ant;

import ec.edu.udla.arquitecturac4.consulta.ant.ANTClient;
import ec.edu.udla.arquitecturac4.consulta.ant.AntHtmlParser;
import ec.edu.udla.arquitecturac4.consulta.ant.AntProperties;
import ec.edu.udla.arquitecturac4.consulta.ant.AntNoDisponibleException;
import ec.edu.udla.arquitecturac4.consulta.ant.AntRespuestaInvalidaException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ANTClientTest {

    private static final String BASE_URL = "https://ant.test";
    private static final String PATH = "/PortalWEB/paginas/clientes/clp_grid_citaciones.jsp";

    private MockRestServiceServer server;
    private ANTClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        AntProperties properties = new AntProperties(
                BASE_URL, PATH, Duration.ofSeconds(3), Duration.ofSeconds(5));
        client = new ANTClient(builder.build(), properties, new AntHtmlParser());
    }

    @Test
    void sendsEncodedGetParametersAndParsesPoints() {
        server.expect(request -> {
                    assertEquals(HttpMethod.GET, request.getMethod());
                    assertEquals(PATH, request.getURI().getPath());
                    String query = request.getURI().getRawQuery();
                    assertTrue(query.contains("ps_tipo_identificacion=CED"));
                    assertTrue(query.contains("ps_identificacion=0000000000%2B%20TEST"), query);
                    assertTrue(query.contains("ps_placa=ZZZ9999%2FTEST"), query);
                })
                .andRespond(withSuccess("<html><p>Puntos: 25</p></html>", MediaType.TEXT_HTML));

        assertEquals(Optional.of(new java.math.BigDecimal("25")),
                client.consultarPuntos("0000000000+ TEST", "ZZZ9999/TEST"));
        server.verify();
    }

    @Test
    void mapsTemporaryUpstreamStatusesToUnavailable() {
        HttpStatus[] statuses = {
                HttpStatus.REQUEST_TIMEOUT, HttpStatus.TOO_MANY_REQUESTS, HttpStatus.BAD_GATEWAY,
                HttpStatus.SERVICE_UNAVAILABLE};
        for (HttpStatus status : statuses) {
            server.expect(method(HttpMethod.GET)).andRespond(withStatus(status));
        }
        for (HttpStatus status : statuses) {
            assertThrows(AntNoDisponibleException.class,
                    () -> client.consultarPuntos("0000000000", "ZZZ9999"), status.toString());
        }
        server.verify();
    }

    @Test
    void mapsClientErrorToInvalidUpstreamResponse() {
        server.expect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(AntRespuestaInvalidaException.class,
                () -> client.consultarPuntos("0000000000", "ZZZ9999"));
        server.verify();
    }

    @Test
    void malformedHtmlIsInvalid() {
        server.expect(method(HttpMethod.GET))
                .andRespond(withSuccess("<html><p>Portal temporalmente bloqueado</p></html>", MediaType.TEXT_HTML));

        assertThrows(AntRespuestaInvalidaException.class,
                () -> client.consultarPuntos("0000000000", "ZZZ9999"));
        server.verify();
    }

    @Test
    void redirectIsRejectedWithoutFollowingIt() {
        server.expect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FOUND).location(java.net.URI.create("https://ant.test/login")));

        assertThrows(AntRespuestaInvalidaException.class,
                () -> client.consultarPuntos("0000000000", "ZZZ9999"));
        server.verify();
    }

    @Test
    void transportTimeoutMapsToUnavailable() {
        server.expect(method(HttpMethod.GET))
                .andRespond(withException(new SocketTimeoutException("synthetic timeout")));

        assertThrows(AntNoDisponibleException.class,
                () -> client.consultarPuntos("0000000000", "ZZZ9999"));
        server.verify();
    }
}
