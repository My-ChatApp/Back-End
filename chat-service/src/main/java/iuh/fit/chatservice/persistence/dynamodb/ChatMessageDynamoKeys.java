package iuh.fit.chatservice.persistence.dynamodb;

/**
 * DynamoDB primary keys for a chat message timeline row and its META lookup row.
 */
public record ChatMessageDynamoKeys(
        String timelinePk,
        String timelineSk,
        String metaPk,
        String metaSk,
        String createdAtIso
) {
}
