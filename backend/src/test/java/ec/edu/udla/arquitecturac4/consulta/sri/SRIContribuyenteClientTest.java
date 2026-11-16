package ec.edu.udla.arquitecturac4.consulta.sri;

import ec.edu.udla.arquitecturac4.consulta.SriNoDisponibleException;
import ec.edu.udla.arquitecturac4.consulta.SriRespuestaInvalidaException;
import ec.edu.udla.arquitecturac4.consulta.sri.dto.SriContribuyenteResponse;
import java.io.InputStream;
import java.io.IOException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SRIContribuyenteClientTest {

    private static final String BASE_URL = "https://sri.test";
    private static final String TEST_RUC = "9999999999999";
    private static final String EXISTS_URL = BASE_URL
            + "/sri-catastro-sujeto-servicio-internet/rest/ConsolidadoContribuyente/existePorNumeroRuc?numeroRuc="
            + TEST_RUC;
    private static final String DETAILS_URL = BASE_URL
            + "/sri-catastro-sujeto-servicio-internet/rest/ConsolidadoContribuyente/obtenerPorNumerosRuc?ruc="
            + TEST_RUC;

    private MockRestServiceServer server;
    private SRIContribuyenteClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new SRIContribuyenteClient(builder.build(), defaultProperties());
    }

    @Test
    void existenceEndpointParsesJsonBoolean() {
        server.expect(requestTo(EXISTS_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("false", MediaType.APPLICATION_JSON));

        assertFalse(client.existe(TEST_RUC));
        server.verify();
    }

    @Test
    void detailsEndpointMapsContributorAndIgnoresUnknownFields() {
        server.expect(requestTo(DETAILS_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{
                          "numeroRuc":"9999999999999",
                          "razonSocial":"PERSONA DE PRUEBA",
                          "estadoContribuyenteRuc":"ACTIVO",
                          "actividadEconomicaPrincipal":"ACTIVIDAD DE PRUEBA",
                          "tipoContribuyente":"PERSONA NATURAL",
                          "regimen":"GENERAL",
                          "informacionFechasContribuyente":{"fechaInicioActividades":"2020-01-02 00:00:00.0"},
                          "campoFuturo":"se ignora"
                        }]
                        """, MediaType.APPLICATION_JSON));

        List<SriContribuyenteResponse> result = client.obtener(TEST_RUC);

        assertEquals(1, result.size());
        assertEquals("PERSONA NATURAL", result.getFirst().tipoContribuyente());
        assertEquals("PERSONA DE PRUEBA", result.getFirst().razonSocial());
        assertEquals("2020-01-02 00:00:00.0", result.getFirst().informacionFechasContribuyente()
                .fechaInicioActividades());
        server.verify();
    }

    @Test
    void noContentFromDetailsEndpointReturnsEmptyList() {
        server.expect(requestTo(DETAILS_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertTrue(client.obtener(TEST_RUC).isEmpty());
        server.verify();
    }

    @Test
    void malformedExistencePayloadRaisesSanitizedResponseException() {
        server.expect(requestTo(EXISTS_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("not-a-boolean", MediaType.APPLICATION_JSON));

        assertThrows(SriRespuestaInvalidaException.class, () -> client.existe(TEST_RUC));
        server.verify();
    }

    @Test
    void existenceEndpointRejectsStringAndNumberInsteadOfJsonBoolean() {
        server.expect(requestTo(EXISTS_URL))
                .andRespond(withSuccess("\"false\"", MediaType.APPLICATION_JSON));
        server.expect(requestTo(EXISTS_URL))
                .andRespond(withSuccess("0", MediaType.APPLICATION_JSON));

        assertThrows(SriRespuestaInvalidaException.class, () -> client.existe(TEST_RUC));
        assertThrows(SriRespuestaInvalidaException.class, () -> client.existe(TEST_RUC));
        server.verify();
    }

    @Test
    void detailsEndpointRejectsNullArrayEntriesAsInvalidUpstreamData() {
        server.expect(requestTo(DETAILS_URL))
                .andRespond(withSuccess("[null]", MediaType.APPLICATION_JSON));

        assertThrows(SriRespuestaInvalidaException.class, () -> client.obtener(TEST_RUC));
        server.verify();
    }

    @Test
    void responseBodyTimeoutMapsToUnavailable() {
        server.expect(requestTo(EXISTS_URL)).andRespond(responseWithReadFailure(new SocketTimeoutException()));

        assertThrows(SriNoDisponibleException.class, () -> client.existe(TEST_RUC));
        server.verify();
    }

    @Test
    void protocolFailureWhileReadingResponseMapsToInvalidUpstreamData() {
        server.expect(requestTo(EXISTS_URL)).andRespond(responseWithReadFailure(new ProtocolException()));

        assertThrows(SriRespuestaInvalidaException.class, () -> client.existe(TEST_RUC));
        server.verify();
    }

    @Test
    void tooManyRequestsMapsToUnavailable() {
        server.expect(requestTo(EXISTS_URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(SriNoDisponibleException.class, () -> client.existe(TEST_RUC));
        server.verify();
    }

    @Test
    void configuredEndpointPathsAreUsed() {
        SriProperties custom = new SriProperties(BASE_URL, "/custom/exists", "/custom/details", "/custom/vehicle",
                Duration.ofSeconds(3), Duration.ofSeconds(8));
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer customServer = MockRestServiceServer.bindTo(builder).build();
        SRIContribuyenteClient customClient = new SRIContribuyenteClient(builder.build(), custom);
        customServer.expect(requestTo(BASE_URL + "/custom/exists?numeroRuc=" + TEST_RUC))
                .andRespond(withSuccess("true", MediaType.APPLICATION_JSON));
        customServer.expect(requestTo(BASE_URL + "/custom/details?ruc=" + TEST_RUC))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(customClient.existe(TEST_RUC));
        assertTrue(customClient.obtener(TEST_RUC).isEmpty());
        customServer.verify();
    }

    @Test
    void upstreamServerErrorRaisesUnavailableException() {
        server.expect(requestTo(EXISTS_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(SriNoDisponibleException.class, () -> client.existe(TEST_RUC));
        server.verify();
    }

    @Test
    void transportFailureRaisesUnavailableException() {
        server.expect(requestTo(EXISTS_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new IOException("simulated transport failure")));

        assertThrows(SriNoDisponibleException.class, () -> client.existe(TEST_RUC));
        server.verify();
    }

    private SriProperties defaultProperties() {
        return new SriProperties(BASE_URL,
                "/sri-catastro-sujeto-servicio-internet/rest/ConsolidadoContribuyente/existePorNumeroRuc",
                "/sri-catastro-sujeto-servicio-internet/rest/ConsolidadoContribuyente/obtenerPorNumerosRuc",
                "/sri-matriculacion-vehicular-recaudacion-servicio-internet/rest/BaseVehiculo/"
                        + "obtenerPorNumeroPlacaOPorNumeroCampvOPorNumeroCpn",
                Duration.ofSeconds(3), Duration.ofSeconds(8));
    }

    private ResponseCreator responseWithReadFailure(IOException failure) {
        return request -> {
            InputStream input = new InputStream() {
                @Override
                public int read() throws IOException {
                    throw failure;
                }
            };
            MockClientHttpResponse response = new MockClientHttpResponse(input, HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return response;
        };
    }
}
