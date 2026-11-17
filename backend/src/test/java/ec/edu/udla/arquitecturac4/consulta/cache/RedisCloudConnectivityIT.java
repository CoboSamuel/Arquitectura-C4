package ec.edu.udla.arquitecturac4.consulta.cache;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RedisCloudConnectivityIT {

    @Test
    void writesAndReadsSyntheticHmacKeyWithShortExpiry() {
        String redisUrl = System.getenv("REDIS_URL");
        String secret = System.getenv("ANT_CACHE_KEY_SECRET");
        assumeTrue(redisUrl != null && !redisUrl.isBlank()
                        && secret != null && secret.getBytes(StandardCharsets.UTF_8).length >= 32,
                "Define REDIS_URL y ANT_CACHE_KEY_SECRET localmente para ejecutar este smoke test.");

        CacheKeyFactory keyFactory = new CacheKeyFactory(new LicenciaCacheProperties(
                secret, Duration.ofMinutes(1), Duration.ofMinutes(2)));
        String syntheticNonce = UUID.randomUUID().toString();
        String key = keyFactory.create("synthetic-id-" + syntheticNonce, "synthetic-plate-" + syntheticNonce);
        RedisClient redisClient = null;
        try {
            redisClient = RedisClient.create(RedisURI.create(redisUrl));
            try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
                String marker = "synthetic-license-cache-smoke-test";
                assertEquals("OK", connection.sync().setex(key, 60, marker));
                assertEquals(marker, connection.sync().get(key));
                long ttl = connection.sync().ttl(key);
                assertTrue(ttl > 0 && ttl <= 60, "La clave de prueba debe expirar en un minuto.");
            }
        } catch (RuntimeException exception) {
            throw new AssertionError(
                    "No se pudo verificar Redis Cloud; revisa REDIS_URL localmente sin compartirla.");
        } finally {
            if (redisClient != null) {
                redisClient.shutdown();
            }
        }
    }
}
