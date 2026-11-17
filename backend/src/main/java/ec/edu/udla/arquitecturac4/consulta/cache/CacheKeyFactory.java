package ec.edu.udla.arquitecturac4.consulta.cache;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class CacheKeyFactory {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String KEY_PREFIX = "ant:license:v1:";

    private final LicenciaCacheProperties properties;

    public CacheKeyFactory(LicenciaCacheProperties properties) {
        this.properties = properties;
    }

    public String create(String cedula, String placa) {
        String secret = properties.keySecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new LicenciaCacheNoConfiguradaException();
        }

        String normalizedCedula = normalize(cedula, "cédula");
        String normalizedPlate = normalize(placa, "placa");
        String material = "v1\u0000license-points\u0000CED\u0000" + normalizedCedula + "\u0000" + normalizedPlate;
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return KEY_PREFIX + HexFormat.of().formatHex(mac.doFinal(material.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("No se pudo generar la clave privada de caché.");
        }
    }

    private String normalize(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " es obligatorio para generar la clave de caché.");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
