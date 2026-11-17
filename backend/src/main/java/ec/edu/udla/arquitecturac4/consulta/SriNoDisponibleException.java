package ec.edu.udla.arquitecturac4.consulta;

public class SriNoDisponibleException extends RuntimeException {

    public SriNoDisponibleException() {
        super("El servicio del SRI no está disponible.");
    }
}
