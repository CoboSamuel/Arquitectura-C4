package ec.edu.udla.arquitecturac4.consulta.cache;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record CachedLicensePoints(BigDecimal puntosDisponibles, Instant actualizadoEn) {

    public CachedLicensePoints {
        Objects.requireNonNull(puntosDisponibles, "puntosDisponibles es obligatorio");
        Objects.requireNonNull(actualizadoEn, "actualizadoEn es obligatorio");
        if (puntosDisponibles.signum() < 0) {
            throw new IllegalArgumentException("puntosDisponibles no puede ser negativo");
        }
    }
}
