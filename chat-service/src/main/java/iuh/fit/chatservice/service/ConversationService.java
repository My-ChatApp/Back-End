package iuh.fit.chatservice.service;

import iuh.fit.chatservice.dto.request.CreateConversationRequest;
import iuh.fit.chatservice.entity.Conversation;
import iuh.fit.chatservice.entity.ConversationMember;
import iuh.fit.chatservice.entity.enums.MemberRole;
import iuh.fit.chatservice.entity.enums.TypeRoom;
import iuh.fit.chatservice.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public Conversation create(CreateConversationRequest req) {
        List<ConversationMember> members = req.getMemberIds().stream()
                .map(id -> ConversationMember.builder()
                        .userId(id)
                        .role(MemberRole.MEMBER)
                        .joinedAt(Instant.now())
                        .build())
                .toList();

        Conversation conversation = Conversation.builder()
                .title(req.getTitle())
                .type(TypeRoom.valueOf(req.getType()))
                .createdBy(req.getMemberIds().get(0))
                .members(members)
                .build();

        return conversationRepository.save(conversation);
    }
}