package ec.edu.udla.arquitecturac4.consulta.sri;

import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SriPropertiesTest {

    private static final String BASE_URL = "https://sri.test";
    private static final String EXISTS_PATH = "/exists";
    private static final String DETAILS_PATH = "/details";

    @Test
    void acceptsPositiveTimeoutsWithMillisecondPrecision() {
        assertDoesNotThrow(() -> properties(Duration.ofMillis(1), Duration.ofMillis(2)));
    }

    @Test
    void rejectsZeroAndSubMillisecondTimeouts() {
        assertThrows(IllegalArgumentException.class,
                () -> properties(Duration.ZERO, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> properties(Duration.ofSeconds(1), Duration.ofNanos(1)));
    }

    @Test
    void rejectsTimeoutsBeyondSupportedMillisecondRange() {
        assertThrows(IllegalArgumentException.class,
                () -> properties(Duration.ofMillis((long) Integer.MAX_VALUE + 1), Duration.ofSeconds(1)));
    }

    private SriProperties properties(Duration connectTimeout, Duration readTimeout) {
        return new SriProperties(BASE_URL, EXISTS_PATH, DETAILS_PATH, connectTimeout, readTimeout);
    }
}
