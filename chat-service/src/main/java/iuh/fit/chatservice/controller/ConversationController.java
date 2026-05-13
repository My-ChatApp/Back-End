package iuh.fit.chatservice.controller;

import iuh.fit.chatservice.dto.request.CreateConversationRequest;
import iuh.fit.chatservice.entity.Conversation;
import iuh.fit.chatservice.service.ConversationService;
import iuh.fit.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    public ApiResponse<Conversation> create(@RequestBody CreateConversationRequest request) {
        return new ApiResponse<>(true, "Conversation created successfully", conversationService.create(request));
    }
}