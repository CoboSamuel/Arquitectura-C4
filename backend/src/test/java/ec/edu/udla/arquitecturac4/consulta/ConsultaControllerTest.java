package ec.edu.udla.arquitecturac4.consulta;

import ec.edu.udla.arquitecturac4.api.ApiExceptionHandler;
import ec.edu.udla.arquitecturac4.consulta.sri.SRIContribuyenteClient;
import ec.edu.udla.arquitecturac4.consulta.sri.dto.SriContribuyenteResponse;
import ec.edu.udla.arquitecturac4.consulta.sri.dto.SriFechasContribuyente;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConsultaController.class)
@Import({ConsultaService.class, ApiExceptionHandler.class})
class ConsultaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SRIContribuyenteClient sriContribuyenteClient;

    @Test
    void invalidContributorFieldsReturn400WithoutEchoingSubmittedValues() throws Exception {
        String invalidEmail = "not-an-email";

        mockMvc.perform(post("/api/v1/consultas/contribuyentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"not-an-email","ruc":"12"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("correo"))
                .andExpect(jsonPath("$.fieldErrors[1].field").value("ruc"))
                .andExpect(content().string(not(containsString(invalidEmail))));
    }

    @Test
    void invalidPlateReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/consultas/vehiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"placa":"??"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("placa"));
    }

    @Test
    void invalidNationalIdReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/consultas/licencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cedula":"1234","placa":"ABC1234"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("cedula"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/consultas/contribuyentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{malformed"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void unknownRequestPropertiesAreRejectedByTheContract() throws Exception {
        mockMvc.perform(post("/api/v1/consultas/vehiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"placa":"ABC1234","campoDesconocido":"valor"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void unknownContributorRequestPropertiesAreRejectedByTheContract() throws Exception {
        mockMvc.perform(post("/api/v1/consultas/contribuyentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"persona@example.com","ruc":"9999999999999","extra":"valor"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));
    }

    @Test
    void unsupportedContentTypeReturns415InsteadOf500() throws Exception {
        mockMvc.perform(post("/api/v1/consultas/vehiculos")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("ABC1234"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void unsupportedMethodReturns405InsteadOf500() throws Exception {
        mockMvc.perform(get("/api/v1/consultas/contribuyentes"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void unacceptableResponseTypeReturns406InsteadOf500() throws Exception {
        mockMvc.perform(post("/api/v1/consultas/vehiculos")
                        .accept(MediaType.TEXT_PLAIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"placa":"ABC1234"}
                                """))
                .andExpect(status().isNotAcceptable())
                .andExpect(content().string(""));
    }

    @Test
    void naturalPersonRequestReturnsContributorSummary() throws Exception {
        when(sriContribuyenteClient.existe("9999999999999")).thenReturn(true);
        when(sriContribuyenteClient.obtener("9999999999999")).thenReturn(List.of(
                new SriContribuyenteResponse(
                        "9999999999999",
                        "PERSONA DE PRUEBA",
                        "ACTIVO",
                        "ACTIVIDAD DE PRUEBA",
                        "PERSONA NATURAL",
                        "GENERAL",
                        new SriFechasContribuyente("2020-01-02 00:00:00.0"))));

        mockMvc.perform(post("/api/v1/consultas/contribuyentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"persona@example.com","ruc":"9999999999999"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruc").value("9999999999999"))
                .andExpect(jsonPath("$.nombre").value("PERSONA DE PRUEBA"))
                .andExpect(jsonPath("$.tipoContribuyente").value("PERSONA NATURAL"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.actividadEconomicaPrincipal").value("ACTIVIDAD DE PRUEBA"))
                .andExpect(jsonPath("$.regimen").value("GENERAL"))
                .andExpect(jsonPath("$.fechaInicioActividades").value("2020-01-02 00:00:00.0"))
                .andExpect(jsonPath("$.representantesLegales").doesNotExist());
    }

    @Test
    void unknownContributorReturns404WithoutEchoingRequestValues() throws Exception {
        when(sriContribuyenteClient.existe("9999999999999")).thenReturn(false);

        mockMvc.perform(post("/api/v1/consultas/contribuyentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"persona@example.com","ruc":"9999999999999"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONTRIBUYENTE_NO_ENCONTRADO"))
                .andExpect(content().string(not(containsString("9999999999999"))))
                .andExpect(content().string(not(containsString("persona@example.com"))));

        verify(sriContribuyenteClient, never()).obtener(anyString());
    }

    @Test
    void nonNaturalContributorReturns422() throws Exception {
        when(sriContribuyenteClient.existe("9999999999999")).thenReturn(true);
        when(sriContribuyenteClient.obtener("9999999999999")).thenReturn(List.of(
                new SriContribuyenteResponse("9999999999999", "ENTIDAD DE PRUEBA", "ACTIVO",
                        "ACTIVIDAD DE PRUEBA", "SOCIEDAD", "GENERAL", null)));

        mockMvc.perform(post("/api/v1/consultas/contribuyentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"persona@example.com","ruc":"9999999999999"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NO_ES_PERSONA_NATURAL"))
                .andExpect(content().string(not(containsString("ENTIDAD DE PRUEBA"))))
                .andExpect(content().string(not(containsString("9999999999999"))))
                .andExpect(content().string(not(containsString("persona@example.com"))));
    }

    @Test
    void unavailableSriReturns503WithoutEchoingRequestValues() throws Exception {
        when(sriContribuyenteClient.existe("9999999999999"))
                .thenThrow(new SriNoDisponibleException());

        mockMvc.perform(post("/api/v1/consultas/contribuyentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"persona@example.com","ruc":"9999999999999"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SRI_NO_DISPONIBLE"))
                .andExpect(content().string(not(containsString("9999999999999"))))
                .andExpect(content().string(not(containsString("persona@example.com"))));
    }

    @Test
    void invalidSriResponseReturns502WithoutEchoingRequestValues() throws Exception {
        when(sriContribuyenteClient.existe("9999999999999"))
                .thenThrow(new SriRespuestaInvalidaException());

        mockMvc.perform(post("/api/v1/consultas/contribuyentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"persona@example.com","ruc":"9999999999999"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("SRI_RESPUESTA_INVALIDA"))
                .andExpect(content().string(not(containsString("9999999999999"))))
                .andExpect(content().string(not(containsString("persona@example.com"))));
    }

    @Test
    void validVehicleRequestReturnsIntegrationPending() throws Exception {
        mockMvc.perform(post("/api/v1/consultas/vehiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"placa":"ABC1234"}
                                """))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.code").value("INTEGRACION_PENDIENTE"));
    }

    @Test
    void validLicenseRequestReturnsIntegrationPending() throws Exception {
        mockMvc.perform(post("/api/v1/consultas/licencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cedula":"1710034065","placa":"ABC1234"}
                                """))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.code").value("INTEGRACION_PENDIENTE"));
    }
}
