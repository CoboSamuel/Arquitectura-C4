package ec.edu.udla.arquitecturac4.consulta.live;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ec.edu.udla.arquitecturac4.consulta.ant.ANTClient;
import ec.edu.udla.arquitecturac4.consulta.ant.SimulatedAntClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"live", "ant-real"})
class AntRealApplicationContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void antRealClientCanBeEnabledWithoutTheSimulator() {
        assertEquals(1, context.getBeansOfType(ANTClient.class).size());
        assertEquals(0, context.getBeansOfType(SimulatedAntClient.class).size());
    }
}
