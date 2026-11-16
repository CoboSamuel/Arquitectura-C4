package ec.edu.udla.arquitecturac4.api;

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
}
