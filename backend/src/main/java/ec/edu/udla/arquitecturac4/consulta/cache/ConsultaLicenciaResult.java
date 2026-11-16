package ec.edu.udla.arquitecturac4.consulta.cache;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record ConsultaLicenciaResult(
        BigDecimal puntosDisponibles,
        Instant actualizadoEn,
        Origen origen,
        boolean desactualizado) {

    public ConsultaLicenciaResult {
        Objects.requireNonNull(puntosDisponibles, "puntosDisponibles es obligatorio");
        Objects.requireNonNull(actualizadoEn, "actualizadoEn es obligatorio");
        Objects.requireNonNull(origen, "origen es obligatorio");
        if (puntosDisponibles.signum() < 0) {
            throw new IllegalArgumentException("puntosDisponibles no puede ser negativo");
        }
    }

    public enum Origen {
        ANT,
        CACHE
    }
}
