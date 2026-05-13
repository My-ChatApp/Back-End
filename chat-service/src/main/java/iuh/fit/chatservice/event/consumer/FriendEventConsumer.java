package iuh.fit.chatservice.event.consumer;

import iuh.fit.chatservice.config.ChatRabbitMQConfig;
import iuh.fit.chatservice.entity.Conversation;
import iuh.fit.chatservice.entity.ConversationMember;
import iuh.fit.chatservice.entity.enums.MemberRole;
import iuh.fit.chatservice.entity.enums.TypeRoom;
import iuh.fit.chatservice.event.payload.FriendAcceptedEvent;
import iuh.fit.chatservice.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendEventConsumer {

    private final ConversationRepository conversationRepository;
    private final static String FRIEND_ACCEPTED_QUEUE = "friend.accepted.queue";

    @RabbitListener(queues = FRIEND_ACCEPTED_QUEUE)
    public void handleFriendAccepted(FriendAcceptedEvent event) {
        log.info("Tạo conversation giữa {} và {}", event.getSenderId(), event.getReceiverId());

        // Kiểm tra conversation đã tồn tại chưa
        boolean exists = conversationRepository
                .findPrivateConversation(TypeRoom.PRIVATE, event.getSenderId(), event.getReceiverId())
                .isPresent();

        if (exists) {
            log.info("Conversation đã tồn tại, bỏ qua");
            return;
        }

        List<ConversationMember> members = List.of(
            ConversationMember.builder()
                    .userId(event.getSenderId())
                    .role(MemberRole.MEMBER)
                    .joinedAt(Instant.now())
                    .build(),
            ConversationMember.builder()
                    .userId(event.getReceiverId())
                    .role(MemberRole.MEMBER)
                    .joinedAt(Instant.now())
                    .build()
        );

        Conversation conversation = Conversation.builder()
                .type(TypeRoom.PRIVATE)
                .createdBy(event.getSenderId())
                .members(members)
                .build();

        conversationRepository.save(conversation);
        log.info("Tạo conversation thành công");
    }
}