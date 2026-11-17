package ec.edu.udla.arquitecturac4.consulta.live;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.udla.arquitecturac4.consulta.ant.ANTClient;
import ec.edu.udla.arquitecturac4.consulta.ant.SimulatedAntClient;
import ec.edu.udla.arquitecturac4.consulta.cache.CacheKeyFactory;
import ec.edu.udla.arquitecturac4.consulta.sri.SRIContribuyenteClient;
import ec.edu.udla.arquitecturac4.consulta.sri.SRIVehiculoClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LiveApplicationContextTest {

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private CacheKeyFactory cacheKeyFactory;

    @Test
    void defaultsToLiveProfileAndRegistersRealSriAndSimulatedAntClients() {
        assertArrayEquals(new String[] {"live"}, environment.getDefaultProfiles());
        assertEquals(1, context.getBeansOfType(SRIContribuyenteClient.class).size());
        assertEquals(1, context.getBeansOfType(SRIVehiculoClient.class).size());
        assertEquals(0, context.getBeansOfType(ANTClient.class).size());
        assertEquals(1, context.getBeansOfType(SimulatedAntClient.class).size());
    }

    @Test
    void localLiveDefaultsAllowTheSimulatedLicenseFlowToGeneratePrivateKeys() {
        assertTrue(cacheKeyFactory.create("0000000000", "ZZZ9999").startsWith("ant:license:v1:"));
    }
}
