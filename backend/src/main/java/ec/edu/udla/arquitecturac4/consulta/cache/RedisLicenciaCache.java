package ec.edu.udla.arquitecturac4.consulta.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisLicenciaCache implements LicenciaCache {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisLicenciaCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<CachedLicensePoints> get(String key) {
        Objects.requireNonNull(key, "key es obligatorio");
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            CachedLicensePoints value = objectMapper.readValue(json, CachedLicensePoints.class);
            if (value == null) {
                redisTemplate.delete(key);
                return Optional.empty();
            }
            return Optional.of(value);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            redisTemplate.delete(key);
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, CachedLicensePoints value, Duration maxStaleTtl) {
        Objects.requireNonNull(key, "key es obligatorio");
        Objects.requireNonNull(value, "value es obligatorio");
        Objects.requireNonNull(maxStaleTtl, "maxStaleTtl es obligatorio");
        if (maxStaleTtl.isZero() || maxStaleTtl.isNegative()) {
            throw new IllegalArgumentException("maxStaleTtl debe ser positivo");
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, maxStaleTtl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar el resumen de puntos para la caché.");
        }
    }

    @Override
    public void evict(String key) {
        Objects.requireNonNull(key, "key es obligatorio");
        redisTemplate.delete(key);
    }
}
