package ec.edu.udla.arquitecturac4.consulta;

public class VehiculoNoEncontradoException extends RuntimeException {

    public VehiculoNoEncontradoException() {
        super("No se encontró un vehículo con la placa ingresada.");
    }
}
