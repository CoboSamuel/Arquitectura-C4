package ec.edu.udla.arquitecturac4.consulta.sri.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SriFechasContribuyente(String fechaInicioActividades) {
}
