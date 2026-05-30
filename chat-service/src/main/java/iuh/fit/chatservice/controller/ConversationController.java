package iuh.fit.chatservice.controller;

import iuh.fit.chatservice.dto.request.CreateConversationRequest;
import iuh.fit.chatservice.dto.request.UpdateConversationRequest;
import iuh.fit.chatservice.entity.Conversation;
import iuh.fit.chatservice.config.ChatSpaceProperties;
import iuh.fit.chatservice.dto.response.MessagesPageResponse;
import iuh.fit.chatservice.service.ChatService;
import iuh.fit.chatservice.service.ConversationMemberService;
import iuh.fit.chatservice.service.ConversationService;
import iuh.fit.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import iuh.fit.chatservice.dto.response.MessageSearchResult;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationMemberService conversationMemberService;
    private final ChatService chatService;
    private final ChatSpaceProperties chatSpaceProperties;

    @GetMapping
    public ApiResponse<List<Conversation>> listAll() {
        return new ApiResponse<>(true, "OK", conversationService.findAll());
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<Conversation>> listByUser(@PathVariable String userId) {
        return new ApiResponse<>(true, "OK", conversationService.listForUser(userId));
    }

    @PostMapping("/{conversationId}/read")
    public ApiResponse<Void> markConversationRead(
            @PathVariable UUID conversationId,
            @RequestParam String userId) {
        conversationMemberService.markConversationRead(conversationId, UUID.fromString(userId));
        return new ApiResponse<>(true, "OK", null);
    }

    @GetMapping("/{conversationId}/messages")
    public ApiResponse<MessagesPageResponse> getMessages(
            @PathVariable String conversationId,
            @RequestParam String userId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String before) {
        int pageSize = limit != null && limit > 0 ? limit : chatSpaceProperties.getDefaultPageSize();
        return new ApiResponse<>(true, "OK", chatService.getMessages(conversationId, userId, pageSize, before));
    }

    @GetMapping("/{id}")
    public ApiResponse<Conversation> getById(
            @PathVariable UUID id,
            @RequestParam String userId) {
        return new ApiResponse<>(true, "OK", conversationService.getByIdForUser(id, UUID.fromString(userId)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Conversation> create(@RequestBody CreateConversationRequest request) {
        return new ApiResponse<>(true, "Conversation created successfully", conversationService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Conversation> update(
            @PathVariable UUID id,
            @RequestParam String userId,
            @RequestBody UpdateConversationRequest request) {
        return new ApiResponse<>(
                true,
                "Conversation updated",
                conversationService.update(id, UUID.fromString(userId), request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable UUID id,
            @RequestParam String userId) {
        conversationService.delete(id, UUID.fromString(userId));
        return new ApiResponse<>(true, "Conversation deleted", null);
    }

    @GetMapping("/{conversationId}/messages/search")
    public ApiResponse<List<MessageSearchResult>> searchMessages(
            @PathVariable String conversationId,
            @RequestParam String userId,
            @RequestParam String q,
            @RequestParam(required = false) Integer limit) {
        int pageSize = limit != null && limit > 0 ? Math.min(limit, 50) : 30;
        return new ApiResponse<>(
                true,
                "OK",
                chatService.searchMessages(conversationId, userId, q, pageSize));
    }
}
