package ec.edu.udla.arquitecturac4.consulta;

import ec.edu.udla.arquitecturac4.api.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
    void validContributorRequestReturnsIntegrationPending() throws Exception {
        mockMvc.perform(post("/api/v1/consultas/contribuyentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"persona@example.com","ruc":"0999999999001"}
                                """))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.code").value("INTEGRACION_PENDIENTE"));
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
