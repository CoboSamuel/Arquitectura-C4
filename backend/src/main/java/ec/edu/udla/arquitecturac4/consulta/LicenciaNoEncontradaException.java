package ec.edu.udla.arquitecturac4.consulta;

public final class LicenciaNoEncontradaException extends RuntimeException {

    public LicenciaNoEncontradaException() {
        super("No se encontró una licencia para la consulta realizada.");
    }
}
