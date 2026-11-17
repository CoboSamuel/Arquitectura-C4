package ec.edu.udla.arquitecturac4.consulta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConsultaVehiculoRequest(
        @NotBlank(message = "La placa es obligatoria.")
        @Pattern(regexp = "[A-Za-z0-9-]{5,8}", message = "La placa debe tener de 5 a 8 letras, números o guiones.")
        String placa) {
}
