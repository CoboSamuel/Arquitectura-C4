package ec.edu.udla.arquitecturac4.api;

import ec.edu.udla.arquitecturac4.consulta.LicenciaNoEncontradaException;
import ec.edu.udla.arquitecturac4.consulta.ant.AntNoDisponibleException;
import ec.edu.udla.arquitecturac4.consulta.ant.AntRespuestaInvalidaException;
import ec.edu.udla.arquitecturac4.consulta.cache.LicenciaCacheNoConfiguradaException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    @Test
    void unexpectedExceptionReturnsGeneric500WithoutInternalDetails() {
        ApiExceptionHandler handler = new ApiExceptionHandler();

        var response = handler.handleUnexpectedException(
                new IllegalStateException("private implementation detail"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).doesNotContain("private implementation detail");
    }

    @Test
    void missingLicenseReturnsGeneric404() {
        var response = new ApiExceptionHandler().handleLicenseNotFound(new LicenciaNoEncontradaException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("LICENCIA_NO_ENCONTRADA");
    }

    @Test
    void invalidAntResponseReturnsGeneric502() {
        var response = new ApiExceptionHandler().handleInvalidAntResponse(new AntRespuestaInvalidaException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("ANT_RESPUESTA_INVALIDA");
    }

    @Test
    void unavailableAntReturnsGeneric503() {
        var response = new ApiExceptionHandler().handleAntUnavailable(new AntNoDisponibleException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("ANT_NO_DISPONIBLE");
    }

    @Test
    void missingLicenseCacheConfigurationReturnsGeneric503() {
        var response = new ApiExceptionHandler().handleLicenseCacheNotConfigured(
                new LicenciaCacheNoConfiguradaException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("CACHE_NO_CONFIGURADA");
        assertThat(response.getBody().message()).doesNotContain("ANT_CACHE_KEY_SECRET");
    }
}
