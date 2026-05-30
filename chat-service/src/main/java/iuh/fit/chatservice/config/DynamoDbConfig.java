package iuh.fit.chatservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(MyChatAppDynamoDbProperties.class)
@Slf4j
public class DynamoDbConfig {

    @Bean
    DynamoDbClient dynamoDbClient(
            MyChatAppDynamoDbProperties properties,
            @Value("${aws.region:us-east-1}") String region,
            @Value("${aws.access-key-id:}") String accessKeyId,
            @Value("${aws.secret-access-key:}") String secretAccessKey) {

        var builder = DynamoDbClient.builder().region(Region.of(region));

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
            builder.credentialsProvider(localCredentials(accessKeyId, secretAccessKey));
            log.info("[DynamoDB] Local endpoint: {} table={}", properties.getEndpoint(), properties.getTableName());
        } else {
            builder.credentialsProvider(cloudCredentials(accessKeyId, secretAccessKey));
            log.info("[DynamoDB] AWS cloud region={} table={}", region, properties.getTableName());
        }

        return builder.build();
    }

    private static AwsCredentialsProvider localCredentials(String accessKeyId, String secretAccessKey) {
        String key = accessKeyId == null || accessKeyId.isBlank() ? "local" : accessKeyId;
        String secret = secretAccessKey == null || secretAccessKey.isBlank() ? "local" : secretAccessKey;
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(key, secret));
    }

    private static AwsCredentialsProvider cloudCredentials(String accessKeyId, String secretAccessKey) {
        if (accessKeyId != null && !accessKeyId.isBlank()
                && secretAccessKey != null && !secretAccessKey.isBlank()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey));
        }
        return DefaultCredentialsProvider.create();
    }
}
