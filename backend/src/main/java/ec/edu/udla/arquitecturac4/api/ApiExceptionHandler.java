package ec.edu.udla.arquitecturac4.api;

import ec.edu.udla.arquitecturac4.consulta.ContribuyenteNoEncontradoException;
import ec.edu.udla.arquitecturac4.consulta.IntegracionPendienteException;
import ec.edu.udla.arquitecturac4.consulta.NoEsPersonaNaturalException;
import ec.edu.udla.arquitecturac4.consulta.SriNoDisponibleException;
import ec.edu.udla.arquitecturac4.consulta.SriRespuestaInvalidaException;
import ec.edu.udla.arquitecturac4.consulta.VehiculoNoEncontradoException;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<ApiFieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiFieldError(error.getField(), error.getDefaultMessage()))
                .distinct()
                .sorted(Comparator.comparing(ApiFieldError::field).thenComparing(ApiFieldError::message))
                .toList();

        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "La solicitud contiene campos inválidos.",
                fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_JSON",
                "El cuerpo debe ser JSON válido y compatible con la solicitud.",
                List.of()));
    }

    @ExceptionHandler(IntegracionPendienteException.class)
    public ResponseEntity<ApiErrorResponse> handleIntegrationPending(IntegracionPendienteException exception) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new ApiErrorResponse(
                HttpStatus.NOT_IMPLEMENTED.value(),
                "INTEGRACION_PENDIENTE",
                exception.getMessage(),
                List.of()));
    }

    @ExceptionHandler(ContribuyenteNoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> handleContributorNotFound(ContribuyenteNoEncontradoException exception) {
        return error(HttpStatus.NOT_FOUND, "CONTRIBUYENTE_NO_ENCONTRADO",
                "No se encontró un contribuyente con los datos ingresados.");
    }

    @ExceptionHandler(VehiculoNoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> handleVehicleNotFound(VehiculoNoEncontradoException exception) {
        return error(HttpStatus.NOT_FOUND, "VEHICULO_NO_ENCONTRADO",
                "No se encontró un vehículo con la placa ingresada.");
    }

    @ExceptionHandler(NoEsPersonaNaturalException.class)
    public ResponseEntity<ApiErrorResponse> handleNotNaturalPerson(NoEsPersonaNaturalException exception) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "NO_ES_PERSONA_NATURAL",
                "El RUC consultado no corresponde a una persona natural.");
    }

    @ExceptionHandler(SriNoDisponibleException.class)
    public ResponseEntity<ApiErrorResponse> handleSriUnavailable(SriNoDisponibleException exception) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "SRI_NO_DISPONIBLE",
                "El servicio del SRI no está disponible temporalmente.");
    }

    @ExceptionHandler(SriRespuestaInvalidaException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidSriResponse(SriRespuestaInvalidaException exception) {
        return error(HttpStatus.BAD_GATEWAY, "SRI_RESPUESTA_INVALIDA",
                "El servicio del SRI devolvió una respuesta no válida.");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Void> handleNotAcceptable(HttpMediaTypeNotAcceptableException exception) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
        if (exception instanceof ErrorResponse frameworkError) {
            HttpStatusCode status = frameworkError.getStatusCode();
            return ResponseEntity.status(status).body(new ApiErrorResponse(
                    status.value(),
                    frameworkErrorCode(status),
                    frameworkErrorMessage(status),
                    List.of()));
        }

        return ResponseEntity.internalServerError().body(new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "Ocurrió un error interno.",
                List.of()));
    }

    private String frameworkErrorCode(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            default -> status.is4xxClientError() ? "CLIENT_ERROR" : "INTERNAL_ERROR";
        };
    }

    private String frameworkErrorMessage(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> "La ruta solicitada no existe.";
            case 405 -> "El método HTTP no está permitido para esta ruta.";
            case 415 -> "El tipo de contenido de la solicitud no es compatible.";
            default -> status.is4xxClientError()
                    ? "La solicitud no puede procesarse."
                    : "Ocurrió un error interno.";
        };
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                status.value(), code, message, List.of()));
    }
}
