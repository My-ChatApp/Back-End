package iuh.fit.chatservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.chatservice.security.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(WebSocketProperties.class)
@RequiredArgsConstructor
public class WebsocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketProperties properties;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final ObjectMapper chatSpaceObjectMapper;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        var registration = registry
                .addEndpoint(properties.getEndpoint())
                .setAllowedOriginPatterns(
                        properties.getAllowedOriginPatterns().toArray(String[]::new));

        if (properties.isSockJsEnabled()) {
            registration.withSockJS();
        }
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        String topicPrefix = properties.getBroker().getTopicPrefix();
        config.enableSimpleBroker(topicPrefix, "/queue");
        config.setApplicationDestinationPrefixes(properties.getBroker().getApplicationPrefix());
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(chatSpaceObjectMapper);
        messageConverters.add(converter);
        return false;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
