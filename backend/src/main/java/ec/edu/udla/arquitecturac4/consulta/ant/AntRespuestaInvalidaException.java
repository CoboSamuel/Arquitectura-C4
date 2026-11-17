package ec.edu.udla.arquitecturac4.consulta.ant;

public final class AntRespuestaInvalidaException extends RuntimeException {

    public AntRespuestaInvalidaException() {
        super("La respuesta del servicio ANT no tiene un formato reconocido.");
    }
}
