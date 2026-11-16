package ec.edu.udla.arquitecturac4.consulta.sri;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integrations.sri")
public record SriProperties(
        String baseUrl,
        String existsPath,
        String detailsPath,
        Duration connectTimeout,
        Duration readTimeout) {

    public SriProperties {
        Objects.requireNonNull(baseUrl, "baseUrl es obligatorio");
        validatePath(existsPath, "existsPath");
        validatePath(detailsPath, "detailsPath");
        validateTimeout(connectTimeout, "connectTimeout");
        validateTimeout(readTimeout, "readTimeout");
    }

    private static void validatePath(String path, String name) {
        if (path == null || path.isBlank() || !path.startsWith("/")) {
            throw new IllegalArgumentException(name + " debe ser una ruta absoluta del servicio");
        }
    }

    private static void validateTimeout(Duration timeout, String name) {
        Objects.requireNonNull(timeout, name + " es obligatorio");
        long millis;
        try {
            millis = timeout.toMillis();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(name + " excede el rango soportado", exception);
        }
        if (millis < 1 || millis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(name + " debe estar entre 1 y 2147483647 milisegundos");
        }
    }
}
