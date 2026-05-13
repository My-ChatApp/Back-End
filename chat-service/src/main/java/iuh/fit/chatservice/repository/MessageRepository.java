package iuh.fit.chatservice.repository;

import iuh.fit.chatservice.entity.Message;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId);
}