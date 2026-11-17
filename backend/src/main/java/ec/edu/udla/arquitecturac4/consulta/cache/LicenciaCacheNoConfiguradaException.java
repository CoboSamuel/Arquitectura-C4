package ec.edu.udla.arquitecturac4.consulta.cache;

public class LicenciaCacheNoConfiguradaException extends IllegalStateException {

    public LicenciaCacheNoConfiguradaException() {
        super("La caché segura de licencias no está configurada.");
    }
}
