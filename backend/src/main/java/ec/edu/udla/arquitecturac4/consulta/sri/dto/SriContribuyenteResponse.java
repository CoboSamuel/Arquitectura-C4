package ec.edu.udla.arquitecturac4.consulta.sri.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SriContribuyenteResponse(
        String numeroRuc,
        String razonSocial,
        String estadoContribuyenteRuc,
        String actividadEconomicaPrincipal,
        String tipoContribuyente,
        String regimen,
        SriFechasContribuyente informacionFechasContribuyente) {
}
