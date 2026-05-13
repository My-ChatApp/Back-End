package iuh.fit.chatservice.repository;

import iuh.fit.chatservice.entity.Conversation;
import iuh.fit.chatservice.entity.enums.TypeRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {
    List<Conversation> findByMembers_UserId(String userId);
    @Query("{ 'type': ?0, 'members.userId': { $all: [?1, ?2] } }")
    Optional<Conversation> findPrivateConversation(TypeRoom type, String userId1, String userId2);
}