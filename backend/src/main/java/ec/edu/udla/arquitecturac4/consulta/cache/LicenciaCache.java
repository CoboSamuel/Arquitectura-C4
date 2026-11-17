package ec.edu.udla.arquitecturac4.consulta.cache;

import java.time.Duration;
import java.util.Optional;

public interface LicenciaCache {

    Optional<CachedLicensePoints> get(String key);

    void put(String key, CachedLicensePoints value, Duration maxStaleTtl);

    void evict(String key);
}
