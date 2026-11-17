package ec.edu.udla.arquitecturac4.consulta.cache;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integrations.ant.cache")
public record LicenciaCacheProperties(String keySecret, Duration freshness, Duration maxStaleTtl) {

    public LicenciaCacheProperties {
        validateDuration(freshness, "freshness");
        validateDuration(maxStaleTtl, "maxStaleTtl");
        if (maxStaleTtl.compareTo(freshness) < 0) {
            throw new IllegalArgumentException("maxStaleTtl no puede ser menor que freshness");
        }
    }

    private static void validateDuration(Duration duration, String name) {
        Objects.requireNonNull(duration, name + " es obligatorio");
        long millis;
        try {
            millis = duration.toMillis();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(name + " excede el rango soportado", exception);
        }
        if (millis < 1 || millis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(name + " debe estar entre 1 y 2147483647 milisegundos");
        }
    }
}
