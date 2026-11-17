package ec.edu.udla.arquitecturac4.consulta.ant;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integrations.ant")
public record AntProperties(
        String baseUrl,
        String path,
        Duration connectTimeout,
        Duration readTimeout) {

    public AntProperties {
        validateBaseUrl(baseUrl);
        if (path == null || path.isBlank() || !path.startsWith("/")) {
            throw new IllegalArgumentException("path debe ser una ruta absoluta del servicio ANT");
        }
        validateTimeout(connectTimeout, "connectTimeout");
        validateTimeout(readTimeout, "readTimeout");
    }

    private static void validateBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl es obligatorio");
        }
        final URI uri;
        try {
            uri = URI.create(baseUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("baseUrl debe ser una URL HTTPS válida", exception);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("baseUrl debe ser una URL HTTPS válida sin credenciales");
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
