package ec.edu.udla.arquitecturac4.consulta;

public class SriRespuestaInvalidaException extends RuntimeException {

    public SriRespuestaInvalidaException() {
        super("El SRI devolvió una respuesta no válida.");
    }
}
