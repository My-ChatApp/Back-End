package iuh.fit.chatservice.entity.enums;

public enum OutboxEventType {
    MESSAGE_CREATED,
    MESSAGE_UPDATED,
    MESSAGE_DELETED,
    MESSAGE_REACTIONS_UPDATED,
    HISTORY_LOAD_REQUESTED
}
