package ec.edu.udla.arquitecturac4.consulta;

import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaContribuyenteRequest;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaContribuyenteResponse;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaLicenciaRequest;
import ec.edu.udla.arquitecturac4.consulta.cache.ConsultaLicenciaResult;
import ec.edu.udla.arquitecturac4.consulta.cache.LicenciaCacheService;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaVehiculoRequest;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaVehiculoResponse;
import ec.edu.udla.arquitecturac4.consulta.sri.SRIContribuyenteClient;
import ec.edu.udla.arquitecturac4.consulta.sri.SRIVehiculoClient;
import ec.edu.udla.arquitecturac4.consulta.sri.dto.SriContribuyenteResponse;
import ec.edu.udla.arquitecturac4.consulta.sri.dto.SriFechasContribuyente;
import ec.edu.udla.arquitecturac4.consulta.sri.dto.SriVehiculoData;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class ConsultaServiceTest {

    private static final String TEST_RUC = "9999999999999";

    @Mock
    private SRIContribuyenteClient sriContribuyenteClient;

    @Mock
    private SRIVehiculoClient sriVehiculoClient;

    @Mock
    private LicenciaCacheService licenciaCacheService;

    @InjectMocks
    private ConsultaService consultaService;

    @Test
    void nonexistentContributorReturnsNotFoundWithoutRequestingDetails() {
        when(sriContribuyenteClient.existe(TEST_RUC)).thenReturn(false);

        assertThrows(ContribuyenteNoEncontradoException.class,
                () -> consultaService.consultarContribuyente(request()));

        verify(sriContribuyenteClient, never()).obtener(anyString());
    }

    @Test
    void naturalPersonDetailsMapToLocalResponse() {
        when(sriContribuyenteClient.existe(TEST_RUC)).thenReturn(true);
        when(sriContribuyenteClient.obtener(TEST_RUC)).thenReturn(List.of(contributor(
                "PERSONA NATURAL", "PERSONA DE PRUEBA")));

        ConsultaContribuyenteResponse result = consultaService.consultarContribuyente(request());

        assertEquals(TEST_RUC, result.ruc());
        assertEquals("PERSONA DE PRUEBA", result.nombre());
        assertEquals("PERSONA NATURAL", result.tipoContribuyente());
        assertEquals("ACTIVO", result.estado());
        assertEquals("ACTIVIDAD DE PRUEBA", result.actividadEconomicaPrincipal());
        assertEquals("GENERAL", result.regimen());
        assertEquals("2020-01-02 00:00:00.0", result.fechaInicioActividades());
        verify(sriContribuyenteClient).obtener(TEST_RUC);
    }

    @Test
    void naturalPersonTypeIsCanonicalizedInResponse() {
        when(sriContribuyenteClient.existe(TEST_RUC)).thenReturn(true);
        when(sriContribuyenteClient.obtener(TEST_RUC)).thenReturn(List.of(contributor(
                "  persona natural ", "PERSONA DE PRUEBA")));

        ConsultaContribuyenteResponse result = consultaService.consultarContribuyente(request());

        assertEquals("PERSONA NATURAL", result.tipoContribuyente());
    }

    @Test
    void incompleteNaturalPersonProfileIsRejectedAsInvalidSriResponse() {
        when(sriContribuyenteClient.existe(TEST_RUC)).thenReturn(true);
        when(sriContribuyenteClient.obtener(TEST_RUC)).thenReturn(List.of(new SriContribuyenteResponse(
                TEST_RUC, "  ", "ACTIVO", "ACTIVIDAD DE PRUEBA", "PERSONA NATURAL", "GENERAL", null)));

        assertThrows(SriRespuestaInvalidaException.class,
                () -> consultaService.consultarContribuyente(request()));
    }

    @Test
    void emptyDetailsReturnNotFound() {
        when(sriContribuyenteClient.existe(TEST_RUC)).thenReturn(true);
        when(sriContribuyenteClient.obtener(TEST_RUC)).thenReturn(List.of());

        assertThrows(ContribuyenteNoEncontradoException.class,
                () -> consultaService.consultarContribuyente(request()));
    }

    @Test
    void nonNaturalContributorIsRejected() {
        when(sriContribuyenteClient.existe(TEST_RUC)).thenReturn(true);
        when(sriContribuyenteClient.obtener(TEST_RUC)).thenReturn(List.of(contributor(
                "SOCIEDAD", "ENTIDAD DE PRUEBA")));

        assertThrows(NoEsPersonaNaturalException.class,
                () -> consultaService.consultarContribuyente(request()));
    }

    @Test
    void vehicleNotFoundIsMappedToNotFoundException() {
        when(sriVehiculoClient.buscar("ABC1234")).thenReturn(Optional.empty());

        assertThrows(VehiculoNoEncontradoException.class,
                () -> consultaService.consultarVehiculo(new ConsultaVehiculoRequest("ABC1234")));
    }

    @Test
    void syntheticVehicleDataMapsToLocalResponse() {
        when(sriVehiculoClient.buscar("ABC1234")).thenReturn(Optional.of(new SriVehiculoData(
                "ABC1234", "MARCA DE PRUEBA", "MODELO DE PRUEBA", "2022",
                "AUTOMOVIL", "AZUL", "1600")));

        ConsultaVehiculoResponse result = consultaService.consultarVehiculo(
                new ConsultaVehiculoRequest("ABC1234"));

        assertEquals("ABC1234", result.placa());
        assertEquals("MARCA DE PRUEBA", result.marca());
        assertEquals("MODELO DE PRUEBA", result.modelo());
        assertEquals("2022", result.anioModelo());
        assertEquals("AUTOMOVIL", result.claseVehiculo());
        assertEquals("AZUL", result.color());
        assertEquals("1600", result.cilindraje());
    }

    @Test
    void incompleteSyntheticVehicleDataIsRejected() {
        when(sriVehiculoClient.buscar("ABC1234")).thenReturn(Optional.of(new SriVehiculoData(
                "ABC1234", "", "MODELO DE PRUEBA", "2022", "AUTOMOVIL", null, null)));

        assertThrows(SriRespuestaInvalidaException.class,
                () -> consultaService.consultarVehiculo(new ConsultaVehiculoRequest("ABC1234")));
    }

    @Test
    void licenseResultMapsToPublicResponseWithoutIdentifiers() {
        when(licenciaCacheService.consultar("0000000000", "ZZZ9999"))
                .thenReturn(new ConsultaLicenciaResult(
                        new BigDecimal("19.5"), Instant.parse("2026-11-15T12:00:00Z"),
                        ConsultaLicenciaResult.Origen.CACHE, true));

        var result = consultaService.consultarLicencia(new ConsultaLicenciaRequest("0000000000", "ZZZ9999"));

        assertEquals(new BigDecimal("19.5"), result.puntosDisponibles());
        assertEquals(Instant.parse("2026-11-15T12:00:00Z"), result.actualizadoEn());
        assertEquals(ConsultaLicenciaResult.Origen.CACHE, result.origen());
        assertTrue(result.desactualizado());
        verify(licenciaCacheService).consultar("0000000000", "ZZZ9999");
    }

    private ConsultaContribuyenteRequest request() {
        return new ConsultaContribuyenteRequest("persona@example.com", TEST_RUC);
    }

    private SriContribuyenteResponse contributor(String type, String name) {
        return new SriContribuyenteResponse(
                TEST_RUC,
                name,
                "ACTIVO",
                "ACTIVIDAD DE PRUEBA",
                type,
                "GENERAL",
                new SriFechasContribuyente("2020-01-02 00:00:00.0"));
    }
}
