package ec.edu.udla.arquitecturac4.consulta.ant;

import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AntPropertiesTest {

    @Test
    void acceptsHttpsEndpointAndPositiveTimeouts() {
        assertDoesNotThrow(() -> new AntProperties(
                "https://ant.test", "/consulta", Duration.ofSeconds(3), Duration.ofSeconds(5)));
    }

    @Test
    void rejectsNonAbsolutePath() {
        assertThrows(IllegalArgumentException.class, () -> new AntProperties(
                "https://ant.test", "consulta", Duration.ofSeconds(3), Duration.ofSeconds(5)));
    }

    @Test
    void rejectsZeroOrSubMillisecondTimeout() {
        assertThrows(IllegalArgumentException.class, () -> new AntProperties(
                "https://ant.test", "/consulta", Duration.ZERO, Duration.ofSeconds(5)));
        assertThrows(IllegalArgumentException.class, () -> new AntProperties(
                "https://ant.test", "/consulta", Duration.ofSeconds(3), Duration.ofNanos(1)));
    }

    @Test
    void rejectsEndpointWithoutHttps() {
        assertThrows(IllegalArgumentException.class, () -> new AntProperties(
                "http://ant.test", "/consulta", Duration.ofSeconds(3), Duration.ofSeconds(5)));
    }
}
