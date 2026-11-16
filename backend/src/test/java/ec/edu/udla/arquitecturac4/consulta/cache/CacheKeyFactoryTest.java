package ec.edu.udla.arquitecturac4.consulta.cache;

import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CacheKeyFactoryTest {

    private static final String SECRET = "synthetic-test-secret-with-more-than-32-bytes";

    @Test
    void createsDeterministicKeyForNormalizedEquivalentInputs() {
        CacheKeyFactory factory = factory(SECRET);

        assertEquals(factory.create(" 0000000000 ", "zzz9999 "),
                factory.create("0000000000", "ZZZ9999"));
    }

    @Test
    void differentIdentifiersCreateDifferentKeys() {
        CacheKeyFactory factory = factory(SECRET);

        assertNotEquals(factory.create("0000000000", "ZZZ9999"),
                factory.create("0000000001", "ZZZ9999"));
        assertNotEquals(factory.create("0000000000", "ZZZ9999"),
                factory.create("0000000000", "ZZZ9998"));
    }

    @Test
    void generatedKeyDoesNotRevealIdentifiers() {
        String key = factory(SECRET).create("0000000000", "ZZZ9999");

        assertFalse(key.contains("0000000000"));
        assertFalse(key.contains("ZZZ9999"));
    }

    @Test
    void rejectsMissingOrShortHmacSecretWhenCreatingKey() {
        assertThrows(IllegalStateException.class, () -> factory(null).create("0000000000", "ZZZ9999"));
        assertThrows(IllegalStateException.class, () -> factory("short").create("0000000000", "ZZZ9999"));
    }

    private CacheKeyFactory factory(String secret) {
        return new CacheKeyFactory(new LicenciaCacheProperties(
                secret, Duration.ofMinutes(15), Duration.ofDays(7)));
    }
}
