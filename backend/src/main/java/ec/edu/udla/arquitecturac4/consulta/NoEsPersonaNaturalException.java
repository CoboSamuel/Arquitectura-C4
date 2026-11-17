package ec.edu.udla.arquitecturac4.consulta;

public class NoEsPersonaNaturalException extends RuntimeException {

    public NoEsPersonaNaturalException() {
        super("El RUC consultado no corresponde a una persona natural.");
    }
}
