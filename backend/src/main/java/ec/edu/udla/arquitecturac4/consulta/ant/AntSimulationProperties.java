package ec.edu.udla.arquitecturac4.consulta.ant;

import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integrations.ant.simulation")
public record AntSimulationProperties(BigDecimal points, boolean available) {

    public AntSimulationProperties {
        Objects.requireNonNull(points, "points es obligatorio");
        if (points.signum() < 0) {
            throw new IllegalArgumentException("points no puede ser negativo");
        }
    }
}
