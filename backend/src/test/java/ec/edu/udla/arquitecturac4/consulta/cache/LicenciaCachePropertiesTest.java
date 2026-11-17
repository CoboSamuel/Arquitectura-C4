package ec.edu.udla.arquitecturac4.consulta.cache;

import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LicenciaCachePropertiesTest {

    @Test
    void acceptsPositiveFreshnessAndLongerRetention() {
        assertDoesNotThrow(() -> new LicenciaCacheProperties(
                "synthetic-test-secret-with-more-than-32-bytes", Duration.ofMinutes(15), Duration.ofDays(7)));
    }

    @Test
    void rejectsZeroOrSubMillisecondDurations() {
        assertThrows(IllegalArgumentException.class, () -> new LicenciaCacheProperties(
                "secret", Duration.ZERO, Duration.ofDays(7)));
        assertThrows(IllegalArgumentException.class, () -> new LicenciaCacheProperties(
                "secret", Duration.ofMinutes(1), Duration.ofNanos(1)));
    }

    @Test
    void rejectsRetentionShorterThanFreshness() {
        assertThrows(IllegalArgumentException.class, () -> new LicenciaCacheProperties(
                "secret", Duration.ofMinutes(15), Duration.ofMinutes(14)));
    }
}
