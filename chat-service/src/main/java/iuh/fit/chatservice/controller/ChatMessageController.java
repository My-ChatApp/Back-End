package iuh.fit.chatservice.controller;

import iuh.fit.chatservice.dto.request.SetMessageReactionRequest;
import iuh.fit.chatservice.dto.request.UpdateMessageRequest;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.service.ChatService;
import iuh.fit.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatService chatService;

    @PatchMapping("/{messageId}")
    public ApiResponse<ChatMessage> updateMessage(
            @PathVariable String conversationId,
            @PathVariable String messageId,
            @RequestParam String userId,
            @RequestBody UpdateMessageRequest request) {
        return new ApiResponse<>(
                true,
                "OK",
                chatService.updateMessage(conversationId, messageId, userId, request));
    }

    @DeleteMapping("/{messageId}")
    public ApiResponse<ChatMessage> deleteMessage(
            @PathVariable String conversationId,
            @PathVariable String messageId,
            @RequestParam String userId) {
        return new ApiResponse<>(
                true,
                "OK",
                chatService.deleteMessage(conversationId, messageId, userId));
    }

    @PutMapping("/{messageId}/reactions")
    public ApiResponse<ChatMessage> setReaction(
            @PathVariable String conversationId,
            @PathVariable String messageId,
            @RequestParam String userId,
            @RequestBody SetMessageReactionRequest request) {
        return new ApiResponse<>(
                true,
                "OK",
                chatService.setMessageReaction(
                        conversationId, messageId, userId, request.getReactionType()));
    }

    @DeleteMapping("/{messageId}/reactions")
    public ApiResponse<ChatMessage> removeReaction(
            @PathVariable String conversationId,
            @PathVariable String messageId,
            @RequestParam String userId) {
        return new ApiResponse<>(
                true,
                "OK",
                chatService.removeMessageReaction(conversationId, messageId, userId));
    }
}
