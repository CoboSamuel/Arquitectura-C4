package ec.edu.udla.arquitecturac4.consulta.dto;

public record ConsultaContribuyenteResponse(
        String ruc,
        String nombre,
        String tipoContribuyente,
        String estado,
        String actividadEconomicaPrincipal,
        String regimen,
        String fechaInicioActividades) {
}
