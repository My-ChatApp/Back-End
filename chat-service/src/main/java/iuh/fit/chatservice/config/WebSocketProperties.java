package iuh.fit.chatservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "mychatapp.websocket")
public class WebSocketProperties {

    /** SockJS/STOMP endpoint, e.g. /ws */
    private String endpoint = "/ws";

    private boolean sockJsEnabled = true;

    private List<String> allowedOriginPatterns = List.of("*");

    private Broker broker = new Broker();

    @Getter
    @Setter
    public static class Broker {
        /** Simple broker destinations, e.g. /topic */
        private String topicPrefix = "/topic";
        /** Client send prefix, e.g. /app */
        private String applicationPrefix = "/app";
    }
}
