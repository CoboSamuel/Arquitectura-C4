package ec.edu.udla.arquitecturac4.consulta.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisLicenciaCacheTest {

    private static final String KEY = "ant:license:v1:synthetic-key";
    private static final Instant UPDATED_AT = Instant.parse("2026-11-15T12:00:00Z");

    private StringRedisTemplate template;
    private ValueOperations<String, String> valueOperations;
    private RedisLicenciaCache cache;
    private ObjectMapper objectMapper;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        template = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        when(template.opsForValue()).thenReturn(valueOperations);
        cache = new RedisLicenciaCache(template, objectMapper);
    }

    @Test
    void readsValidatedCachedPointsJson() throws Exception {
        CachedLicensePoints expected = new CachedLicensePoints(new BigDecimal("19.5"), UPDATED_AT);
        when(valueOperations.get(KEY)).thenReturn(objectMapper.writeValueAsString(expected));

        assertEquals(Optional.of(expected), cache.get(KEY));
    }

    @Test
    void writesOnlySummaryWithMaximumRetentionTtlAndRoundTripsIt() throws Exception {
        CachedLicensePoints value = new CachedLicensePoints(BigDecimal.ZERO, UPDATED_AT);

        cache.put(KEY, value, Duration.ofDays(7));

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(KEY), jsonCaptor.capture(), eq(Duration.ofDays(7)));
        Set<String> jsonFields = new HashSet<>();
        objectMapper.readTree(jsonCaptor.getValue()).fieldNames().forEachRemaining(jsonFields::add);
        assertEquals(Set.of("puntosDisponibles", "actualizadoEn"), jsonFields);
        when(valueOperations.get(KEY)).thenReturn(jsonCaptor.getValue());
        assertEquals(Optional.of(value), cache.get(KEY));
    }

    @Test
    void removesMalformedCacheValueAndReturnsMiss() {
        when(valueOperations.get(KEY)).thenReturn("<html>not cached data</html>");

        assertTrue(cache.get(KEY).isEmpty());
        verify(template).delete(KEY);
    }

    @Test
    void evictsAnExistingCachedLicenseSummary() {
        cache.evict(KEY);

        verify(template).delete(KEY);
    }
}
