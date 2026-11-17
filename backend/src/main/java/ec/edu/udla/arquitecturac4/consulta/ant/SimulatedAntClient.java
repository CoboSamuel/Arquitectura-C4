package ec.edu.udla.arquitecturac4.consulta.ant;

import ec.edu.udla.arquitecturac4.consulta.spi.PuntosLicenciaProvider;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("live & !ant-real")
public class SimulatedAntClient implements PuntosLicenciaProvider {

    private final AntSimulationProperties properties;

    public SimulatedAntClient(AntSimulationProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<BigDecimal> consultarPuntos(String cedula, String placa) {
        if (!properties.available()) {
            throw new AntNoDisponibleException();
        }
        return Optional.of(properties.points());
    }

    @Override
    public Fuente fuente() {
        return Fuente.SIMULADA;
    }
}
