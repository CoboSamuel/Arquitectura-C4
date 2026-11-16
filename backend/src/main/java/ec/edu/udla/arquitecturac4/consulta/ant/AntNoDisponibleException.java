package ec.edu.udla.arquitecturac4.consulta.ant;

public final class AntNoDisponibleException extends RuntimeException {

    public AntNoDisponibleException() {
        super("El servicio ANT no está disponible temporalmente.");
    }
}
