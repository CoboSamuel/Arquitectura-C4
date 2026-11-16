package ec.edu.udla.arquitecturac4.consulta.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConsultaContribuyenteRequest(
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        @Size(max = 254, message = "El correo no puede superar 254 caracteres.")
        String correo,

        @NotBlank(message = "El RUC es obligatorio.")
        @Pattern(regexp = "[0-9]{13}", message = "El RUC debe contener exactamente 13 dígitos.")
        String ruc) {
}
