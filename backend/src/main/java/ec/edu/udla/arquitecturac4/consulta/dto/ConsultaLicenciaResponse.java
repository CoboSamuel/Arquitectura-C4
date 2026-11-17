package ec.edu.udla.arquitecturac4.consulta.dto;

import ec.edu.udla.arquitecturac4.consulta.cache.ConsultaLicenciaResult;
import java.math.BigDecimal;
import java.time.Instant;

public record ConsultaLicenciaResponse(
        BigDecimal puntosDisponibles,
        Instant actualizadoEn,
        ConsultaLicenciaResult.Origen origen,
        boolean desactualizado) {
}
