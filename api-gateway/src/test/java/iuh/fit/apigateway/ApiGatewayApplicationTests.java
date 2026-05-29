package iuh.fit.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "gateway.ratelimit.enabled=false")
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }

}
