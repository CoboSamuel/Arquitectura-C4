package ec.edu.udla.arquitecturac4.api;

import java.util.List;

public record ApiErrorResponse(int status, String code, String message, List<ApiFieldError> fieldErrors) {

    public ApiErrorResponse {
        fieldErrors = List.copyOf(fieldErrors);
    }
}
