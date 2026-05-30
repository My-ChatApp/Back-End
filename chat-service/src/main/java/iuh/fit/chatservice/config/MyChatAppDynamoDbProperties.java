package iuh.fit.chatservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mychatapp.dynamodb")
public class MyChatAppDynamoDbProperties {
    private String tableName = "MyChatApp_Chat";
    private String endpoint;
}
