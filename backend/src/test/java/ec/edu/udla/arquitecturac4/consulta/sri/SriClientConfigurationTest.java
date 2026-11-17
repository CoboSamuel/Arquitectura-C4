package ec.edu.udla.arquitecturac4.consulta.sri;

import com.sun.net.httpserver.HttpServer;
import ec.edu.udla.arquitecturac4.consulta.SriRespuestaInvalidaException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SriClientConfigurationTest {

    @Test
    void doesNotFollowRedirectsFromTheSri() throws IOException {
        AtomicInteger redirectedRequests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/exists", exchange -> {
            exchange.getResponseHeaders().add("Location", "/redirect-target");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/redirect-target", exchange -> {
            redirectedRequests.incrementAndGet();
            byte[] body = "true".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            SriProperties properties = new SriProperties(baseUrl, "/exists", "/details", "/vehicle",
                    Duration.ofSeconds(2), Duration.ofSeconds(2));
            RestClient restClient = new SriClientConfiguration().sriRestClient(RestClient.builder(), properties);
            SRIContribuyenteClient client = new SRIContribuyenteClient(restClient, properties);

            assertThrows(SriRespuestaInvalidaException.class, () -> client.existe("9999999999999"));
            assertEquals(0, redirectedRequests.get());
        } finally {
            server.stop(0);
        }
    }
}
