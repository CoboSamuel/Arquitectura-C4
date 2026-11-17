package ec.edu.udla.arquitecturac4.consulta.sri;

import ec.edu.udla.arquitecturac4.consulta.SriNoDisponibleException;
import ec.edu.udla.arquitecturac4.consulta.SriRespuestaInvalidaException;
import ec.edu.udla.arquitecturac4.consulta.sri.dto.SriVehiculoData;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SRIVehiculoClientTest {

    private static final String BASE_URL = "https://sri.test";
    private static final String TEST_PLATE = "ABC1234";
    private static final String VEHICLE_URL = BASE_URL
            + "/sri-matriculacion-vehicular-recaudacion-servicio-internet/rest/BaseVehiculo/"
            + "obtenerPorNumeroPlacaOPorNumeroCampvOPorNumeroCpn?numeroPlacaCampvCpn=" + TEST_PLATE;

    private MockRestServiceServer server;
    private SRIVehiculoClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new SRIVehiculoClient(builder.build(), properties(
                "/sri-matriculacion-vehicular-recaudacion-servicio-internet/rest/BaseVehiculo/"
                        + "obtenerPorNumeroPlacaOPorNumeroCampvOPorNumeroCpn"));
    }

    @Test
    void emptySRIEnvelopeReturnsNoVehicle() {
        server.expect(requestTo(VEHICLE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"data": [], "objeto": null, "mensajeServidor": {"texto": "SIN RESULTADOS"}}
                        """, MediaType.APPLICATION_JSON));

        assertEquals(Optional.empty(), client.buscar(TEST_PLATE));
        server.verify();
    }

    @Test
    void syntheticObjectMapsOnlyAllowlistedVehicleFields() {
        server.expect(requestTo(VEHICLE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "data": [],
                          "objeto": {
                            "numeroPlaca": "ABC1234",
                            "marca": "MARCA DE PRUEBA",
                            "modelo": "MODELO DE PRUEBA",
                            "anioModelo": 2022,
                            "claseVehiculo": "AUTOMOVIL",
                            "color": "AZUL",
                            "cilindraje": 1600,
                            "propietario": "TITULAR FICTICIO",
                            "identificacionPropietario": "0000000000",
                            "numeroChasis": "CHASIS FICTICIO"
                          },
                          "mensajeServidor": {"texto": "OK"}
                        }
                        """, MediaType.APPLICATION_JSON));

        SriVehiculoData result = client.buscar(TEST_PLATE).orElseThrow();

        assertEquals(TEST_PLATE, result.numeroPlaca());
        assertEquals("MARCA DE PRUEBA", result.marca());
        assertEquals("MODELO DE PRUEBA", result.modelo());
        assertEquals("2022", result.anioModelo());
        assertEquals("AUTOMOVIL", result.claseVehiculo());
        assertEquals("AZUL", result.color());
        assertEquals("1600", result.cilindraje());
        server.verify();
    }

    @Test
    void syntheticArrayRecordCanBeSelectedByRequestedPlate() {
        server.expect(requestTo(VEHICLE_URL))
                .andRespond(withSuccess("""
                        {
                          "data": [
                            {"numeroPlaca":"XYZ1234","marca":"OTRA","modelo":"OTRO","anioModelo":2020,"claseVehiculo":"AUTO"},
                            {"numeroPlaca":"ABC1234","marca":"MARCA DE PRUEBA","modelo":"MODELO DE PRUEBA","anioModelo":2022,"claseVehiculo":"AUTOMOVIL"}
                          ],
                          "objeto": null,
                          "mensajeServidor": {"texto": "OK"}
                        }
                        """, MediaType.APPLICATION_JSON));

        SriVehiculoData result = client.buscar(TEST_PLATE).orElseThrow();

        assertEquals(TEST_PLATE, result.numeroPlaca());
        assertEquals("MARCA DE PRUEBA", result.marca());
        server.verify();
    }

    @Test
    void flatSriVehicleObjectMapsOfficialFieldNames() {
        server.expect(requestTo(VEHICLE_URL))
                .andRespond(withSuccess("""
                        {
                          "numeroPlaca": "ABC1234",
                          "descripcionMarca": "MARCA SRI",
                          "descripcionModelo": "MODELO SRI",
                          "anioAuto": 2022,
                          "nombreClase": "AUTOMOVIL",
                          "colorVehiculo1": "ROJO",
                          "cilindraje": 1600,
                          "codigoVehiculo": "IDENTIFICADOR INTERNO"
                        }
                        """, MediaType.APPLICATION_JSON));

        SriVehiculoData result = client.buscar(TEST_PLATE).orElseThrow();

        assertEquals(TEST_PLATE, result.numeroPlaca());
        assertEquals("MARCA SRI", result.marca());
        assertEquals("MODELO SRI", result.modelo());
        assertEquals("2022", result.anioModelo());
        assertEquals("AUTOMOVIL", result.claseVehiculo());
        assertEquals("ROJO", result.color());
        assertEquals("1600", result.cilindraje());
        server.verify();
    }

    @Test
    void malformedEnvelopeRaisesSanitizedInvalidResponseException() {
        server.expect(requestTo(VEHICLE_URL))
                .andRespond(withSuccess("{\"objeto\":null}", MediaType.APPLICATION_JSON));

        assertThrows(SriRespuestaInvalidaException.class, () -> client.buscar(TEST_PLATE));
        server.verify();
    }

    @Test
    void upstreamServerErrorMapsToUnavailable() {
        server.expect(requestTo(VEHICLE_URL))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(SriNoDisponibleException.class, () -> client.buscar(TEST_PLATE));
        server.verify();
    }

    @Test
    void transportTimeoutMapsToUnavailable() {
        server.expect(requestTo(VEHICLE_URL))
                .andRespond(withException(new SocketTimeoutException("simulated timeout")));

        assertThrows(SriNoDisponibleException.class, () -> client.buscar(TEST_PLATE));
        server.verify();
    }

    private SriProperties properties(String vehiclePath) {
        return new SriProperties(
                BASE_URL,
                "/exists",
                "/details",
                vehiclePath,
                Duration.ofSeconds(3),
                Duration.ofSeconds(8));
    }
}
