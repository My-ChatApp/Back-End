package iuh.fit.chatservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import iuh.fit.chatservice.entity.enums.MemberRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_members", schema = "app")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMember {

    @EmbeddedId
    private ConversationMemberId id;

    @MapsId("conversationId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    @JsonIgnore
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "member_role")
    @Builder.Default
    private MemberRole role = MemberRole.MEMBER;

    private String nickname;

    @Column(name = "last_read_message_id")
    private UUID lastReadMessageId;

    @Column(name = "last_read_at")
    private Instant lastReadAt;

    @Column(name = "unread_count", nullable = false)
    @Builder.Default
    private int unreadCount = 0;

    @Builder.Default
    private boolean muted = false;

    @Builder.Default
    private boolean pinned = false;

    @Column(name = "notifications_enabled", nullable = false)
    @Builder.Default
    private boolean notificationsEnabled = true;

    @Builder.Default
    private boolean archived = false;

    @Builder.Default
    private boolean deleted = false;

    @Column(name = "left_at")
    private Instant leftAt;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (joinedAt == null) {
            joinedAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public static ConversationMember of(Conversation conversation, UUID userId, MemberRole role) {
        ConversationMemberId memberId = new ConversationMemberId(conversation.getId(), userId);
        ConversationMember member = new ConversationMember();
        member.setId(memberId);
        member.setConversation(conversation);
        member.setUserId(userId);
        member.setRole(role);
        member.setJoinedAt(Instant.now());
        member.setUpdatedAt(Instant.now());
        return member;
    }

    @JsonProperty("userId")
    public UUID getUserId() {
        return id != null ? id.getUserId() : null;
    }

    public void setUserId(UUID userId) {
        if (id == null) {
            id = new ConversationMemberId();
        }
        id.setUserId(userId);
    }
}
