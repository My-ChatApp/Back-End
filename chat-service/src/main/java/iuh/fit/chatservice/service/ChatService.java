package iuh.fit.chatservice.service;

import iuh.fit.chatservice.dto.request.SendMessageRequest;
import iuh.fit.chatservice.entity.Message;
import iuh.fit.chatservice.entity.Conversation;
import iuh.fit.chatservice.event.payload.MessageSentEvent;
import iuh.fit.chatservice.event.publisher.MessagePublisher;
import iuh.fit.chatservice.repository.MessageRepository;
import iuh.fit.chatservice.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final MessagePublisher messagePublisher;


    public Message sendMessage(SendMessageRequest req) {

        Conversation conversation = conversationRepository
                .findById(req.getConversationId())
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        Message message = Message.builder()
                .conversation(conversation)
                .senderId(req.getSenderId())
                .content(req.getContent())
                .type(Enum.valueOf(iuh.fit.chatservice.entity.enums.MessageType.class, req.getType()))
                .createdAt(Instant.now())
                .build();

        Message saved = messageRepository.save(message);



        // update last message
        conversation.setLastMessageId(saved.getId());
        conversation.setLastMessageAt(saved.getCreatedAt());

        conversationRepository.save(conversation);

//        //Publish RabbitMQ
//        messagePublisher.publishMessageSent(saved, conversation.getId());
        return saved;
    }
}