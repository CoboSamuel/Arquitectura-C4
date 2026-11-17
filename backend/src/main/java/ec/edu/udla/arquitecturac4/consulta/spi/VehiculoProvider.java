package ec.edu.udla.arquitecturac4.consulta.spi;

import ec.edu.udla.arquitecturac4.consulta.sri.dto.SriVehiculoData;
import java.util.Optional;

public interface VehiculoProvider {

    Optional<SriVehiculoData> buscar(String placa);
}
