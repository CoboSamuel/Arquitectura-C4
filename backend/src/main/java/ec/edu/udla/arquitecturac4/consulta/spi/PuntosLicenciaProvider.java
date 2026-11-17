package ec.edu.udla.arquitecturac4.consulta.spi;

import java.math.BigDecimal;
import java.util.Optional;

public interface PuntosLicenciaProvider {

    Optional<BigDecimal> consultarPuntos(String cedula, String placa);

    default Fuente fuente() {
        return Fuente.REAL;
    }

    enum Fuente {
        REAL,
        SIMULADA
    }
}
