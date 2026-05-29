package iuh.fit.chatservice.service;

import iuh.fit.chatservice.entity.Conversation;
import iuh.fit.chatservice.entity.ConversationMember;
import iuh.fit.chatservice.entity.ConversationMemberId;
import iuh.fit.chatservice.entity.enums.MemberRole;
import iuh.fit.chatservice.entity.enums.TypeRoom;
import iuh.fit.chatservice.exception.ForbiddenException;
import iuh.fit.chatservice.exception.ResourceNotFoundException;
import iuh.fit.chatservice.repository.ConversationMemberRepository;
import iuh.fit.chatservice.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationAccessService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Conversation requireConversation(UUID conversationId) {
        return conversationRepository.findByIdAndDeletedFalse(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
    }

    @Transactional(readOnly = true)
    public ConversationMember requireActiveMember(UUID conversationId, UUID userId) {
        return memberRepository
                .findById(new ConversationMemberId(conversationId, userId))
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new ForbiddenException("Bạn không phải thành viên của hội thoại này"));
    }

    @Transactional(readOnly = true)
    public ConversationMember requireOwner(UUID conversationId, UUID userId) {
        ConversationMember member = requireActiveMember(conversationId, userId);
        if (member.getRole() != MemberRole.OWNER) {
            throw new ForbiddenException("Chỉ chủ nhóm mới có quyền thực hiện thao tác này");
        }
        return member;
    }

    @Transactional(readOnly = true)
    public void requireGroupOwner(UUID conversationId, UUID userId) {
        Conversation conversation = requireConversation(conversationId);
        if (conversation.getType() != TypeRoom.GROUP) {
            throw new ForbiddenException("Thao tác chỉ áp dụng cho nhóm");
        }
        requireOwner(conversationId, userId);
    }

    public boolean isOwner(ConversationMember member) {
        return member.getRole() == MemberRole.OWNER;
    }
}
