package iuh.fit.chatservice.service;

import iuh.fit.chatservice.dto.request.CreateConversationRequest;
import iuh.fit.chatservice.dto.request.UpdateConversationRequest;
import iuh.fit.chatservice.entity.Conversation;
import iuh.fit.chatservice.entity.ConversationMember;
import iuh.fit.chatservice.entity.enums.MemberRole;
import iuh.fit.chatservice.entity.enums.TypeRoom;
import iuh.fit.chatservice.exception.ResourceNotFoundException;
import iuh.fit.chatservice.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ChatInboxBroadcastService inboxBroadcastService;
    private final ConversationAccessService conversationAccessService;
    private final ConversationInboxNotifier conversationInboxNotifier;

    @Transactional(readOnly = true)
    public List<Conversation> findAll() {
        return conversationRepository.findAllByDeletedFalse();
    }

    @Transactional(readOnly = true)
    public Conversation getById(UUID id) {
        return conversationRepository.findByIdWithMembers(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + id));
    }

    @Transactional(readOnly = true)
    public Conversation getByIdForUser(UUID id, UUID userId) {
        conversationAccessService.requireActiveMember(id, userId);
        return getById(id);
    }

    @Transactional
    public Conversation create(CreateConversationRequest req) {
        TypeRoom type = TypeRoom.valueOf(req.getType());

        if (type == TypeRoom.PRIVATE && req.getMemberIds().size() == 2) {
            UUID u1 = UUID.fromString(req.getMemberIds().get(0));
            UUID u2 = UUID.fromString(req.getMemberIds().get(1));
            return conversationRepository.findPrivateBetweenUsers(u1, u2)
                    .orElseGet(() -> createNew(req, type));
        }

        return createNew(req, type);
    }

    private Conversation createNew(CreateConversationRequest req, TypeRoom type) {
        UUID createdBy = UUID.fromString(req.getMemberIds().get(0));

        Conversation conversation = Conversation.builder()
                .title(req.getTitle())
                .type(type)
                .createdBy(createdBy)
                .members(new ArrayList<>())
                .build();

        List<ConversationMember> members = req.getMemberIds().stream()
                .map(id -> {
                    UUID memberId = UUID.fromString(id);
                    MemberRole role = type == TypeRoom.GROUP && memberId.equals(createdBy)
                            ? MemberRole.OWNER
                            : MemberRole.MEMBER;
                    return ConversationMember.of(conversation, memberId, role);
                })
                .toList();

        conversation.getMembers().addAll(members);
        Conversation saved = conversationRepository.save(conversation);
        notifyMembersConversationCreated(saved);
        return saved;
    }

    private void notifyMembersConversationCreated(Conversation conversation) {
        if (conversation.getMembers() == null) {
            return;
        }
        for (ConversationMember member : conversation.getMembers()) {
            if (member.getUserId() != null) {
                inboxBroadcastService.notifyConversationCreated(
                        member.getUserId().toString(), conversation);
            }
        }
    }

    @Transactional
    public Conversation update(UUID id, UUID actorUserId, UpdateConversationRequest request) {
        conversationAccessService.requireGroupOwner(id, actorUserId);
        Conversation conversation = getById(id);

        if (request.getTitle() != null) {
            conversation.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            conversation.setDescription(request.getDescription());
        }
        if (request.getAvatarUrl() != null) {
            conversation.setAvatarUrl(request.getAvatarUrl());
        }

        Conversation saved = conversationRepository.save(conversation);
        conversationInboxNotifier.broadcastConversationUpdated(saved.getId());
        return getById(saved.getId());
    }

    @Transactional
    public void delete(UUID id, UUID actorUserId) {
        conversationAccessService.requireGroupOwner(id, actorUserId);
        Conversation conversation = getById(id);
        conversationInboxNotifier.broadcastConversationDeleted(id);
        conversation.setDeleted(true);
        conversation.setDeletedAt(Instant.now());
        conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public List<Conversation> listForUser(String userId) {
        return conversationRepository.findByMemberUserId(UUID.fromString(userId));
    }
}
