package ec.edu.udla.arquitecturac4.consulta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConsultaLicenciaRequest(
        @NotBlank(message = "La cédula es obligatoria.")
        @Pattern(regexp = "[0-9]{10}", message = "La cédula debe contener exactamente 10 dígitos.")
        String cedula,

        @NotBlank(message = "La placa es obligatoria.")
        @Pattern(regexp = "[A-Za-z0-9-]{5,8}", message = "La placa debe tener de 5 a 8 letras, números o guiones.")
        String placa) {
}
