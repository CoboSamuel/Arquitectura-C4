package ec.edu.udla.arquitecturac4.consulta.cache;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LicenciaCacheProperties.class)
public class LicenciaCacheConfiguration {

    @Bean
    Clock utcClock() {
        return Clock.systemUTC();
    }
}
