package ec.edu.udla.arquitecturac4.consulta.cache;

import ec.edu.udla.arquitecturac4.consulta.LicenciaNoEncontradaException;
import ec.edu.udla.arquitecturac4.consulta.ant.ANTClient;
import ec.edu.udla.arquitecturac4.consulta.ant.AntNoDisponibleException;
import ec.edu.udla.arquitecturac4.consulta.ant.AntRespuestaInvalidaException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LicenciaCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LicenciaCacheService.class);

    private final ANTClient antClient;
    private final LicenciaCache cache;
    private final CacheKeyFactory keyFactory;
    private final LicenciaCacheProperties properties;
    private final Clock clock;

    public LicenciaCacheService(
            ANTClient antClient,
            LicenciaCache cache,
            CacheKeyFactory keyFactory,
            LicenciaCacheProperties properties,
            Clock clock) {
        this.antClient = antClient;
        this.cache = cache;
        this.keyFactory = keyFactory;
        this.properties = properties;
        this.clock = clock;
    }

    public ConsultaLicenciaResult consultar(String cedula, String placa) {
        String key = keyFactory.create(cedula, placa);
        Optional<CachedLicensePoints> cached = readCache(key);
        Instant now = clock.instant();

        Optional<CachedLicensePoints> fresh = cached.filter(value -> isWithin(value, now, properties.freshness()));
        if (fresh.isPresent()) {
            return fromCache(fresh.orElseThrow(), false);
        }

        try {
            Optional<BigDecimal> points = antClient.consultarPuntos(cedula, placa);
            if (points.isEmpty()) {
                evictCache(key);
                throw new LicenciaNoEncontradaException();
            }

            Instant fetchedAt = clock.instant();
            CachedLicensePoints latest = new CachedLicensePoints(points.orElseThrow(), fetchedAt);
            writeCache(key, latest);
            return new ConsultaLicenciaResult(
                    latest.puntosDisponibles(), latest.actualizadoEn(), ConsultaLicenciaResult.Origen.ANT, false);
        } catch (LicenciaNoEncontradaException exception) {
            throw exception;
        } catch (AntNoDisponibleException | AntRespuestaInvalidaException exception) {
            Optional<CachedLicensePoints> stale = cached.filter(
                    value -> isWithin(value, clock.instant(), properties.maxStaleTtl()));
            if (stale.isPresent()) {
                return fromCache(stale.orElseThrow(), true);
            }
            throw exception;
        }
    }

    private Optional<CachedLicensePoints> readCache(String key) {
        try {
            return cache.get(key);
        } catch (RuntimeException exception) {
            LOGGER.warn("No fue posible leer Redis; se continuará con la consulta ANT.");
            return Optional.empty();
        }
    }

    private void writeCache(String key, CachedLicensePoints value) {
        try {
            cache.put(key, value, properties.maxStaleTtl());
        } catch (RuntimeException exception) {
            LOGGER.warn("No fue posible escribir Redis; se conserva la respuesta validada de ANT.");
        }
    }

    private void evictCache(String key) {
        try {
            cache.evict(key);
        } catch (RuntimeException exception) {
            LOGGER.warn("No fue posible invalidar Redis tras una respuesta ANT de ausencia.");
        }
    }

    private boolean isWithin(CachedLicensePoints value, Instant now, java.time.Duration window) {
        Instant updatedAt = value.actualizadoEn();
        return !updatedAt.isAfter(now) && !updatedAt.isBefore(now.minus(window));
    }

    private ConsultaLicenciaResult fromCache(CachedLicensePoints value, boolean stale) {
        return new ConsultaLicenciaResult(
                value.puntosDisponibles(), value.actualizadoEn(), ConsultaLicenciaResult.Origen.CACHE, stale);
    }
}
