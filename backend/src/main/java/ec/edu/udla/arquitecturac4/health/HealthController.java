package ec.edu.udla.arquitecturac4.health;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final String applicationName;

    public HealthController(
            @Value("${spring.application.name:sri-ant-backend}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping
    public Map<String, String> health() {
        return Map.of("status", "UP", "application", applicationName);
    }
}
