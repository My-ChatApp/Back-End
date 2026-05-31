package iuh.fit.chatservice.service;

import iuh.fit.chatservice.dto.request.AddConversationMemberRequest;
import iuh.fit.chatservice.dto.request.UpdateConversationMemberRequest;
import iuh.fit.chatservice.entity.Conversation;
import iuh.fit.chatservice.entity.ConversationMember;
import iuh.fit.chatservice.entity.ConversationMemberId;
import iuh.fit.chatservice.entity.enums.MemberRole;
import iuh.fit.chatservice.entity.enums.TypeRoom;
import iuh.fit.chatservice.exception.ForbiddenException;
import iuh.fit.chatservice.exception.ResourceNotFoundException;
import iuh.fit.chatservice.event.payload.ChatRealtimeEnvelope;
import iuh.fit.chatservice.repository.ConversationMemberRepository;
import iuh.fit.chatservice.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationMemberService {

    private final ConversationMemberRepository memberRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationAccessService conversationAccessService;
    private final ConversationInboxNotifier conversationInboxNotifier;
    private final ChatRealtimeBroadcastService realtimeBroadcastService;

    @Transactional(readOnly = true)
    public List<ConversationMember> listByConversation(UUID conversationId, UUID actorUserId) {
        conversationAccessService.requireActiveMember(conversationId, actorUserId);
        ensureConversationExists(conversationId);
        return memberRepository.findById_ConversationIdAndDeletedFalse(conversationId);
    }

    @Transactional(readOnly = true)
    public ConversationMember getById(UUID conversationId, UUID userId, UUID actorUserId) {
        conversationAccessService.requireActiveMember(conversationId, actorUserId);
        return getById(conversationId, userId);
    }

    @Transactional(readOnly = true)
    public ConversationMember getById(UUID conversationId, UUID userId) {
        return memberRepository.findById(new ConversationMemberId(conversationId, userId))
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member not found: conversation=" + conversationId + ", user=" + userId));
    }

    @Transactional
    public ConversationMember addMember(UUID conversationId, UUID actorUserId, AddConversationMemberRequest request) {
        Conversation conversation = ensureConversationExists(conversationId);
        if (conversation.getType() != TypeRoom.GROUP) {
            throw new ForbiddenException("Chỉ có thể thêm thành viên vào nhóm");
        }
        conversationAccessService.requireOwner(conversationId, actorUserId);

        ConversationMemberId memberId = new ConversationMemberId(conversationId, request.getUserId());
        if (memberRepository.existsById(memberId)) {
            ConversationMember existing = memberRepository.findById(memberId).orElseThrow();
            if (!existing.isDeleted()) {
                throw new IllegalStateException("User đã là thành viên");
            }
            existing.setDeleted(false);
            existing.setLeftAt(null);
            existing.setJoinedAt(Instant.now());
            existing.setUpdatedAt(Instant.now());
            if (request.getRole() != null) {
                existing.setRole(request.getRole());
            }
            ConversationMember saved = memberRepository.save(existing);
            conversationInboxNotifier.broadcastConversationUpdated(conversationId);
            return saved;
        }

        ConversationMember member = ConversationMember.builder()
                .id(memberId)
                .conversation(conversation)
                .role(request.getRole() != null ? request.getRole() : MemberRole.MEMBER)
                .nickname(request.getNickname())
                .build();

        ConversationMember saved = memberRepository.save(member);
        conversationInboxNotifier.broadcastConversationUpdated(conversationId);
        return saved;
    }

    @Transactional
    public ConversationMember updateMember(
            UUID conversationId,
            UUID userId,
            UUID actorUserId,
            UpdateConversationMemberRequest request) {
        conversationAccessService.requireActiveMember(conversationId, actorUserId);
        if (!userId.equals(actorUserId)) {
            throw new ForbiddenException("Chỉ có thể cập nhật cài đặt của chính bạn");
        }
        return updateMember(conversationId, userId, request);
    }

    @Transactional
    public ConversationMember updateMember(
            UUID conversationId,
            UUID userId,
            UpdateConversationMemberRequest request) {
        ConversationMember member = getById(conversationId, userId);

        if (request.getNickname() != null) {
            member.setNickname(request.getNickname());
        }
        if (request.getMuted() != null) {
            member.setMuted(request.getMuted());
        }
        if (request.getPinned() != null) {
            member.setPinned(request.getPinned());
        }
        if (request.getNotificationsEnabled() != null) {
            member.setNotificationsEnabled(request.getNotificationsEnabled());
        }
        if (request.getArchived() != null) {
            member.setArchived(request.getArchived());
        }
        if (request.getLastReadMessageId() != null) {
            member.setLastReadMessageId(request.getLastReadMessageId());
            member.setLastReadAt(Instant.now());
        }
        if (request.getUnreadCount() != null) {
            member.setUnreadCount(request.getUnreadCount());
        }

        return memberRepository.save(member);
    }

    @Transactional
    public ConversationMember markConversationRead(UUID conversationId, UUID userId) {
        ConversationMember member = getById(conversationId, userId);
        Conversation conversation = ensureConversationExists(conversationId);
        member.setUnreadCount(0);
        member.setLastReadAt(Instant.now());
        if (conversation.getLastMessageId() != null) {
            member.setLastReadMessageId(conversation.getLastMessageId());
        }
        ConversationMember saved = memberRepository.save(member);
        broadcastReadReceipt(conversationId, saved);
        return saved;
    }

    private void broadcastReadReceipt(UUID conversationId, ConversationMember member) {
        String lastReadMessageId = member.getLastReadMessageId() != null
                ? member.getLastReadMessageId().toString()
                : null;
        realtimeBroadcastService.broadcast(
                conversationId.toString(),
                ChatRealtimeEnvelope.builder()
                        .eventType(ChatRealtimeEnvelope.EventType.READ_RECEIPT)
                        .conversationId(conversationId.toString())
                        .userId(member.getUserId().toString())
                        .lastReadMessageId(lastReadMessageId)
                        .lastReadAt(member.getLastReadAt())
                        .build());
    }

    @Transactional
    public void removeMember(UUID conversationId, UUID targetUserId, UUID actorUserId) {
        Conversation conversation = ensureConversationExists(conversationId);
        if (conversation.getType() != TypeRoom.GROUP) {
            throw new ForbiddenException("Không thể rời hội thoại riêng tư theo cách này");
        }

        if (targetUserId.equals(actorUserId)) {
            ConversationMember self = conversationAccessService.requireActiveMember(conversationId, actorUserId);
            if (self.getRole() == MemberRole.OWNER) {
                long activeOwners = memberRepository.findById_ConversationIdAndDeletedFalse(conversationId).stream()
                        .filter(m -> m.getRole() == MemberRole.OWNER)
                        .count();
                if (activeOwners <= 1) {
                    long activeMembers = memberRepository.findById_ConversationIdAndDeletedFalse(conversationId)
                            .size();
                    if (activeMembers > 1) {
                        throw new ForbiddenException(
                                "Cần chuyển quyền chủ nhóm trước khi rời nhóm (hoặc giải tán nhóm)");
                    }
                }
            }
            softRemove(getById(conversationId, targetUserId));
            conversationInboxNotifier.broadcastMemberRemoved(conversationId, targetUserId);
            return;
        }

        conversationAccessService.requireOwner(conversationId, actorUserId);
        ConversationMember target = getById(conversationId, targetUserId);
        if (target.getRole() == MemberRole.OWNER) {
            throw new ForbiddenException("Không thể xóa chủ nhóm khỏi nhóm");
        }
        softRemove(target);
        conversationInboxNotifier.broadcastMemberRemoved(conversationId, targetUserId);
    }

    private void softRemove(ConversationMember member) {
        member.setDeleted(true);
        member.setLeftAt(Instant.now());
        memberRepository.save(member);
    }

    private Conversation ensureConversationExists(UUID conversationId) {
        return conversationRepository.findByIdAndDeletedFalse(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
    }
}
