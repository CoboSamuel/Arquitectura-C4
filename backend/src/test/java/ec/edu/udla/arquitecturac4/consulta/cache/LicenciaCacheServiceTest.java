package ec.edu.udla.arquitecturac4.consulta.cache;

import ec.edu.udla.arquitecturac4.consulta.LicenciaNoEncontradaException;
import ec.edu.udla.arquitecturac4.consulta.ant.ANTClient;
import ec.edu.udla.arquitecturac4.consulta.ant.AntNoDisponibleException;
import ec.edu.udla.arquitecturac4.consulta.ant.AntRespuestaInvalidaException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LicenciaCacheServiceTest {

    private static final String CEDULA = "0000000000";
    private static final String PLATE = "ZZZ9999";
    private static final String CACHE_KEY = "ant:license:v1:synthetic-key";
    private static final Instant NOW = Instant.parse("2026-11-15T12:30:00Z");
    private static final Duration FRESHNESS = Duration.ofMinutes(15);
    private static final Duration RETENTION = Duration.ofDays(7);

    private ANTClient antClient;
    private LicenciaCache cache;
    private CacheKeyFactory keyFactory;
    private LicenciaCacheService service;

    @BeforeEach
    void setUp() {
        antClient = mock(ANTClient.class);
        cache = mock(LicenciaCache.class);
        keyFactory = mock(CacheKeyFactory.class);
        when(keyFactory.create(CEDULA, PLATE)).thenReturn(CACHE_KEY);
        LicenciaCacheProperties properties = new LicenciaCacheProperties("synthetic-test-secret-long-enough",
                FRESHNESS, RETENTION);
        service = new LicenciaCacheService(
                antClient, cache, keyFactory, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void freshCacheReturnsWithoutCallingAnt() {
        CachedLicensePoints cached = cached(new BigDecimal("18"), NOW.minus(Duration.ofMinutes(5)));
        when(cache.get(CACHE_KEY)).thenReturn(Optional.of(cached));

        ConsultaLicenciaResult result = service.consultar(CEDULA, PLATE);

        assertEquals(cached.puntosDisponibles(), result.puntosDisponibles());
        assertEquals(cached.actualizadoEn(), result.actualizadoEn());
        assertEquals(ConsultaLicenciaResult.Origen.CACHE, result.origen());
        assertFalse(result.desactualizado());
        verifyNoInteractions(antClient);
    }

    @Test
    void cacheMissReturnsAntResultAndStoresSummary() {
        when(cache.get(CACHE_KEY)).thenReturn(Optional.empty());
        when(antClient.consultarPuntos(CEDULA, PLATE)).thenReturn(Optional.of(new BigDecimal("25")));

        ConsultaLicenciaResult result = service.consultar(CEDULA, PLATE);

        assertEquals(new BigDecimal("25"), result.puntosDisponibles());
        assertEquals(NOW, result.actualizadoEn());
        assertEquals(ConsultaLicenciaResult.Origen.ANT, result.origen());
        assertFalse(result.desactualizado());
        verify(cache).put(CACHE_KEY, cached(new BigDecimal("25"), NOW), RETENTION);
    }

    @Test
    void staleCacheIsReturnedWhenAntIsUnavailableWithoutReplacingIt() {
        CachedLicensePoints cached = cached(new BigDecimal("20"), NOW.minus(Duration.ofHours(1)));
        when(cache.get(CACHE_KEY)).thenReturn(Optional.of(cached));
        when(antClient.consultarPuntos(CEDULA, PLATE)).thenThrow(new AntNoDisponibleException());

        ConsultaLicenciaResult result = service.consultar(CEDULA, PLATE);

        assertEquals(cached.puntosDisponibles(), result.puntosDisponibles());
        assertEquals(ConsultaLicenciaResult.Origen.CACHE, result.origen());
        assertTrue(result.desactualizado());
        verify(cache, never()).put(anyString(), any(), any());
    }

    @Test
    void staleCacheIsReturnedWhenAntHtmlIsInvalid() {
        CachedLicensePoints cached = cached(new BigDecimal("20"), NOW.minus(Duration.ofHours(1)));
        when(cache.get(CACHE_KEY)).thenReturn(Optional.of(cached));
        when(antClient.consultarPuntos(CEDULA, PLATE)).thenThrow(new AntRespuestaInvalidaException());

        ConsultaLicenciaResult result = service.consultar(CEDULA, PLATE);

        assertEquals(ConsultaLicenciaResult.Origen.CACHE, result.origen());
        assertTrue(result.desactualizado());
        assertEquals(cached.actualizadoEn(), result.actualizadoEn());
        verify(cache, never()).put(anyString(), any(), any());
    }

    @Test
    void explicitNoRecordsReturnsNotFoundAndIsNotCached() {
        when(cache.get(CACHE_KEY)).thenReturn(Optional.empty());
        when(antClient.consultarPuntos(CEDULA, PLATE)).thenReturn(Optional.empty());

        assertThrows(LicenciaNoEncontradaException.class, () -> service.consultar(CEDULA, PLATE));
        verify(cache, never()).put(anyString(), any(), any());
    }

    @Test
    void explicitNoRecordsDoesNotHideAValidNotFoundBehindStaleData() {
        when(cache.get(CACHE_KEY)).thenReturn(Optional.of(cached(
                new BigDecimal("20"), NOW.minus(Duration.ofHours(1)))));
        when(antClient.consultarPuntos(CEDULA, PLATE)).thenReturn(Optional.empty());

        assertThrows(LicenciaNoEncontradaException.class, () -> service.consultar(CEDULA, PLATE));
        verify(cache, never()).put(anyString(), any(), any());
    }

    @Test
    void explicitNoRecordsPreventsOldPositiveValueFromReturningAfterLaterAntOutage() {
        InMemoryLicenciaCache statefulCache = new InMemoryLicenciaCache();
        statefulCache.put(CACHE_KEY, cached(new BigDecimal("20"), NOW.minus(Duration.ofHours(1))), RETENTION);
        LicenciaCacheService statefulService = new LicenciaCacheService(
                antClient, statefulCache, keyFactory,
                new LicenciaCacheProperties("synthetic-test-secret-long-enough", FRESHNESS, RETENTION),
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(antClient.consultarPuntos(CEDULA, PLATE))
                .thenReturn(Optional.empty())
                .thenThrow(new AntNoDisponibleException());

        assertThrows(LicenciaNoEncontradaException.class, () -> statefulService.consultar(CEDULA, PLATE));

        assertThrows(AntNoDisponibleException.class, () -> statefulService.consultar(CEDULA, PLATE));
    }

    @Test
    void zeroPointsIsValidAndCached() {
        when(cache.get(CACHE_KEY)).thenReturn(Optional.empty());
        when(antClient.consultarPuntos(CEDULA, PLATE)).thenReturn(Optional.of(BigDecimal.ZERO));

        ConsultaLicenciaResult result = service.consultar(CEDULA, PLATE);

        assertEquals(BigDecimal.ZERO, result.puntosDisponibles());
        verify(cache).put(CACHE_KEY, cached(BigDecimal.ZERO, NOW), RETENTION);
    }

    @Test
    void noUsableCacheAndAntUnavailablePropagatesUnavailable() {
        when(cache.get(CACHE_KEY)).thenReturn(Optional.empty());
        when(antClient.consultarPuntos(CEDULA, PLATE)).thenThrow(new AntNoDisponibleException());

        assertThrows(AntNoDisponibleException.class, () -> service.consultar(CEDULA, PLATE));
    }

    @Test
    void redisReadFailureDoesNotDiscardLiveAntResult() {
        when(cache.get(CACHE_KEY)).thenThrow(new IllegalStateException("synthetic Redis failure"));
        when(antClient.consultarPuntos(CEDULA, PLATE)).thenReturn(Optional.of(new BigDecimal("15")));

        ConsultaLicenciaResult result = service.consultar(CEDULA, PLATE);

        assertEquals(ConsultaLicenciaResult.Origen.ANT, result.origen());
        assertEquals(new BigDecimal("15"), result.puntosDisponibles());
    }

    @Test
    void redisReadAndAntFailurePropagateUnavailable() {
        when(cache.get(CACHE_KEY)).thenThrow(new IllegalStateException("synthetic Redis failure"));
        when(antClient.consultarPuntos(CEDULA, PLATE)).thenThrow(new AntNoDisponibleException());

        assertThrows(AntNoDisponibleException.class, () -> service.consultar(CEDULA, PLATE));
    }

    @Test
    void redisWriteFailureDoesNotDiscardLiveAntResult() {
        when(cache.get(CACHE_KEY)).thenReturn(Optional.empty());
        when(antClient.consultarPuntos(CEDULA, PLATE)).thenReturn(Optional.of(new BigDecimal("15")));
        org.mockito.Mockito.doThrow(new IllegalStateException("synthetic Redis failure"))
                .when(cache).put(eq(CACHE_KEY), any(), eq(RETENTION));

        ConsultaLicenciaResult result = service.consultar(CEDULA, PLATE);

        assertEquals(ConsultaLicenciaResult.Origen.ANT, result.origen());
        assertEquals(new BigDecimal("15"), result.puntosDisponibles());
    }

    @Test
    void staleValueOlderThanMaximumRetentionIsNotUsed() {
        when(cache.get(CACHE_KEY)).thenReturn(Optional.of(cached(
                new BigDecimal("20"), NOW.minus(Duration.ofDays(8)))));
        when(antClient.consultarPuntos(CEDULA, PLATE)).thenThrow(new AntNoDisponibleException());

        assertThrows(AntNoDisponibleException.class, () -> service.consultar(CEDULA, PLATE));
    }

    @Test
    void missingCacheSecretFailsBeforeAntOrCacheAccess() {
        when(keyFactory.create(CEDULA, PLATE)).thenThrow(new IllegalStateException("missing secret"));

        assertThrows(IllegalStateException.class, () -> service.consultar(CEDULA, PLATE));
        verifyNoInteractions(antClient, cache);
    }

    private CachedLicensePoints cached(BigDecimal points, Instant updatedAt) {
        return new CachedLicensePoints(points, updatedAt);
    }

    private static final class InMemoryLicenciaCache implements LicenciaCache {

        private final Map<String, CachedLicensePoints> values = new HashMap<>();

        @Override
        public Optional<CachedLicensePoints> get(String key) {
            return Optional.ofNullable(values.get(key));
        }

        @Override
        public void put(String key, CachedLicensePoints value, Duration maxStaleTtl) {
            values.put(key, value);
        }

        @Override
        public void evict(String key) {
            values.remove(key);
        }
    }
}
