package iuh.fit.chatservice.persistence.dynamodb;

import iuh.fit.chatservice.config.MyChatAppDynamoDbProperties;
import iuh.fit.chatservice.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepository {

    private final DynamoDbClient dynamoDbClient;
    private final MyChatAppDynamoDbProperties properties;
    private final ChatMessageItemFactory itemFactory;

    public ChatMessage save(ChatMessage message) {
        String table = properties.getTableName();
        ChatMessageDynamoKeys keys = itemFactory.keysFor(message);
        Map<String, AttributeValue> messageItem = itemFactory.createMessageItem(message, keys);
        Map<String, AttributeValue> metaItem = itemFactory.createMetaItem(message, keys);

        dynamoDbClient.transactWriteItems(TransactWriteItemsRequest.builder()
                .transactItems(
                        TransactWriteItem.builder()
                                .put(Put.builder().tableName(table).item(messageItem).build())
                                .build(),
                        TransactWriteItem.builder()
                                .put(Put.builder().tableName(table).item(metaItem).build())
                                .build()
                )
                .build());

        return message;
    }

    /**
     * Scans recent timeline pages for messages whose content contains the query (case-insensitive).
     * Bounded by maxPages to avoid unbounded DynamoDB reads on large conversations.
     */
    public List<ChatMessage> searchByConversationId(String conversationId, String query, int limit) {
        String q = query == null ? "" : query.trim().toLowerCase();
        if (q.length() < 2) {
            return List.of();
        }

        String table = properties.getTableName();
        List<ChatMessage> results = new ArrayList<>();
        Map<String, AttributeValue> lastKey = null;
        int pageSize = 50;
        int maxPages = 20;

        for (int page = 0; page < maxPages && results.size() < limit; page++) {
            QueryRequest.Builder builder = QueryRequest.builder()
                    .tableName(table)
                    .keyConditionExpression("PK = :pk AND begins_with(SK, :skPrefix)")
                    .filterExpression("entityType = :entityType")
                    .expressionAttributeValues(Map.of(
                            ":pk", AttributeValue.builder().s(ChatMessageItemFactory.conversationPk(conversationId)).build(),
                            ":skPrefix", AttributeValue.builder().s("MSG#").build(),
                            ":entityType", AttributeValue.builder().s("MESSAGE").build()
                    ))
                    .scanIndexForward(false)
                    .limit(pageSize);

            if (lastKey != null && !lastKey.isEmpty()) {
                builder.exclusiveStartKey(lastKey);
            }

            QueryResponse response = dynamoDbClient.query(builder.build());
            for (Map<String, AttributeValue> item : response.items()) {
                if (!"MESSAGE".equals(getEntityType(item))) {
                    continue;
                }
                ChatMessage message = itemFactory.fromTimelineItem(item);
                if (message.isDeleted()) {
                    continue;
                }
                String content = message.getContent();
                if (content == null || !content.toLowerCase().contains(q)) {
                    continue;
                }
                results.add(message);
                if (results.size() >= limit) {
                    return results;
                }
            }

            lastKey = response.lastEvaluatedKey();
            if (lastKey == null || lastKey.isEmpty()) {
                break;
            }
        }

        return results;
    }

    public List<ChatMessage> findByConversationId(String conversationId, int limit) {
        String table = properties.getTableName();
        QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(table)
                .keyConditionExpression("PK = :pk AND begins_with(SK, :skPrefix)")
                .expressionAttributeValues(Map.of(
                        ":pk", AttributeValue.builder().s(ChatMessageItemFactory.conversationPk(conversationId)).build(),
                        ":skPrefix", AttributeValue.builder().s("MSG#").build()
                ))
                .scanIndexForward(false)
                .limit(limit)
                .build());

        return response.items().stream()
                .map(itemFactory::fromTimelineItem)
                .collect(Collectors.toList());
    }

    public List<ChatMessage> findByConversationIdBefore(
            String conversationId, Instant beforeCreatedAt, String beforeMessageId, int limit) {
        if (beforeCreatedAt == null || beforeMessageId == null) {
            return findByConversationId(conversationId, limit);
        }
        String table = properties.getTableName();
        String beforeSk = ChatMessageItemFactory.messageSk(
                ChatMessageItemFactory.formatInstant(beforeCreatedAt), beforeMessageId);
        QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(table)
                .keyConditionExpression("PK = :pk AND SK < :beforeSk")
                .expressionAttributeValues(Map.of(
                        ":pk", AttributeValue.builder().s(ChatMessageItemFactory.conversationPk(conversationId)).build(),
                        ":beforeSk", AttributeValue.builder().s(beforeSk).build()
                ))
                .scanIndexForward(false)
                .limit(limit)
                .build());

        List<ChatMessage> messages = response.items().stream()
                .filter(item -> "MESSAGE".equals(getEntityType(item)))
                .map(itemFactory::fromTimelineItem)
                .collect(Collectors.toList());
        Collections.reverse(messages);
        return messages;
    }

    public Optional<ChatMessage> findByMessageId(String messageId) {
        String table = properties.getTableName();
        GetItemResponse metaResponse = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(table)
                .key(Map.of(
                        "PK", AttributeValue.builder().s("MSG#" + messageId).build(),
                        "SK", AttributeValue.builder().s("META").build()
                ))
                .build());
        if (!metaResponse.hasItem() || metaResponse.item().isEmpty()) {
            return Optional.empty();
        }
        String timelinePk = getS(metaResponse.item(), "timelinePk");
        String timelineSk = getS(metaResponse.item(), "timelineSk");
        if (timelinePk == null || timelineSk == null) {
            return Optional.empty();
        }
        GetItemResponse msgResponse = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(table)
                .key(Map.of(
                        "PK", AttributeValue.builder().s(timelinePk).build(),
                        "SK", AttributeValue.builder().s(timelineSk).build()
                ))
                .build());
        if (!msgResponse.hasItem() || msgResponse.item().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(itemFactory.fromTimelineItem(msgResponse.item()));
    }

    public boolean existsByMessageId(String messageId) {
        String table = properties.getTableName();
        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(table)
                .key(Map.of(
                        "PK", AttributeValue.builder().s("MSG#" + messageId).build(),
                        "SK", AttributeValue.builder().s("META").build()
                ))
                .build());
        return response.hasItem() && !response.item().isEmpty();
    }

    public void updateMessageContent(ChatMessage message) {
        String table = properties.getTableName();
        ChatMessageDynamoKeys keys = itemFactory.keysFor(message);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":content", AttributeValue.builder().s(
                message.getContent() != null ? message.getContent() : "").build());
        expressionValues.put(":edited", AttributeValue.builder().bool(true).build());
        expressionValues.put(":editedAt", AttributeValue.builder().s(
                ChatMessageItemFactory.formatInstant(message.getEditedAt())).build());

        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(table)
                .key(Map.of(
                        "PK", AttributeValue.builder().s(keys.timelinePk()).build(),
                        "SK", AttributeValue.builder().s(keys.timelineSk()).build()
                ))
                .updateExpression("SET content = :content, edited = :edited, editedAt = :editedAt")
                .expressionAttributeValues(expressionValues)
                .build());
    }

    private static String getEntityType(Map<String, AttributeValue> item) {
        AttributeValue v = item.get("entityType");
        return v != null ? v.s() : null;
    }

    private static String getS(Map<String, AttributeValue> item, String key) {
        AttributeValue v = item.get(key);
        return v != null ? v.s() : null;
    }
}
