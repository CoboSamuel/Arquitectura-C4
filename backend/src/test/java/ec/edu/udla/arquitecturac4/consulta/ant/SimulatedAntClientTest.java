package ec.edu.udla.arquitecturac4.consulta.ant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ec.edu.udla.arquitecturac4.consulta.spi.PuntosLicenciaProvider;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SimulatedAntClientTest {

    @Test
    void returnsConfiguredPointsWithSimulationSource() {
        SimulatedAntClient client = new SimulatedAntClient(
                new AntSimulationProperties(new BigDecimal("30"), true));

        assertEquals(Optional.of(new BigDecimal("30")), client.consultarPuntos("0102030405", "ABC1234"));
        assertEquals(PuntosLicenciaProvider.Fuente.SIMULADA, client.fuente());
    }

    @Test
    void unavailableSimulationRaisesAntUnavailable() {
        SimulatedAntClient client = new SimulatedAntClient(
                new AntSimulationProperties(new BigDecimal("30"), false));

        assertThrows(
                AntNoDisponibleException.class,
                () -> client.consultarPuntos("0102030405", "ABC1234"));
    }
}
