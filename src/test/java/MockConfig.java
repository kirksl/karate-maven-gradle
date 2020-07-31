import org.company.OrderController;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableAutoConfiguration
@PropertySource(factory = YamlPropertySourceFactory.class, value = "classpath:application.yml")
public class MockConfig {

    // Global Exception Handler ...

    // Services ...

    // Controllers ...
    @Bean
    public OrderController getOrderController()
    {
        return new OrderController();
    }
}
