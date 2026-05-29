package iuh.fit.chatservice.service;

import iuh.fit.chatservice.entity.Conversation;
import iuh.fit.chatservice.entity.ConversationMember;
import iuh.fit.chatservice.repository.ConversationMemberRepository;
import iuh.fit.chatservice.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Pushes group conversation mutations to each member's inbox topic (/topic/inbox/{userId}).
 */
@Service
@RequiredArgsConstructor
public class ConversationInboxNotifier {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final ChatInboxBroadcastService inboxBroadcastService;

    @Transactional(readOnly = true)
    public void broadcastConversationUpdated(UUID conversationId) {
        conversationRepository.findByIdWithMembers(conversationId).ifPresent(this::broadcastConversationUpdated);
    }

    @Transactional(readOnly = true)
    public void broadcastConversationUpdated(Conversation conversation) {
        if (conversation == null || conversation.getId() == null || conversation.isDeleted()) {
            return;
        }
        UUID conversationId = conversation.getId();
        for (ConversationMember member : activeMembers(conversationId)) {
            UUID userId = member.getUserId();
            if (userId != null) {
                inboxBroadcastService.notifyConversationUpdated(userId.toString(), conversation);
            }
        }
    }

    @Transactional(readOnly = true)
    public void broadcastConversationDeleted(UUID conversationId) {
        conversationRepository.findByIdWithMembers(conversationId).ifPresent(conv -> {
            List<ConversationMember> members = memberRepository.findById_ConversationIdAndDeletedFalse(conversationId);
            if (members.isEmpty() && conv.getMembers() != null) {
                members = conv.getMembers().stream().filter(m -> !m.isDeleted()).toList();
            }
            for (ConversationMember member : members) {
                UUID userId = member.getUserId();
                if (userId != null) {
                    inboxBroadcastService.notifyConversationDeleted(userId.toString(), conv);
                }
            }
        });
    }

  /**
   * Member left or was removed: removed user drops the conversation; others receive updated member list.
   */
    @Transactional(readOnly = true)
    public void broadcastMemberRemoved(UUID conversationId, UUID removedUserId) {
        Conversation conversation = conversationRepository.findByIdWithMembers(conversationId).orElse(null);
        if (conversation == null) {
            return;
        }

        inboxBroadcastService.notifyConversationDeleted(removedUserId.toString(), conversation);

        Conversation refreshed = conversationRepository.findByIdWithMembers(conversationId).orElse(conversation);
        for (ConversationMember member : activeMembers(conversationId)) {
            UUID userId = member.getUserId();
            if (userId != null && !userId.equals(removedUserId)) {
                inboxBroadcastService.notifyConversationUpdated(userId.toString(), refreshed);
            }
        }
    }

    private List<ConversationMember> activeMembers(UUID conversationId) {
        return memberRepository.findById_ConversationIdAndDeletedFalse(conversationId);
    }
}
