package ec.edu.udla.arquitecturac4.consulta;

public class ContribuyenteNoEncontradoException extends RuntimeException {

    public ContribuyenteNoEncontradoException() {
        super("No se encontró un contribuyente con el RUC indicado.");
    }
}
