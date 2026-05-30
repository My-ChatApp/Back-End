package iuh.fit.chatservice.controller;

import iuh.fit.chatservice.dto.request.AddConversationMemberRequest;
import iuh.fit.chatservice.dto.request.UpdateConversationMemberRequest;
import iuh.fit.chatservice.entity.ConversationMember;
import iuh.fit.chatservice.service.ConversationMemberService;
import iuh.fit.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations/{conversationId}/members")
@RequiredArgsConstructor
public class ConversationMemberController {

    private final ConversationMemberService memberService;

    @GetMapping
    public ApiResponse<List<ConversationMember>> list(
            @PathVariable UUID conversationId,
            @RequestParam String userId) {
        return new ApiResponse<>(
                true,
                "OK",
                memberService.listByConversation(conversationId, UUID.fromString(userId)));
    }

    @GetMapping("/{memberUserId}")
    public ApiResponse<ConversationMember> getById(
            @PathVariable UUID conversationId,
            @PathVariable UUID memberUserId,
            @RequestParam String userId) {
        return new ApiResponse<>(
                true,
                "OK",
                memberService.getById(conversationId, memberUserId, UUID.fromString(userId)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConversationMember> add(
            @PathVariable UUID conversationId,
            @RequestParam String userId,
            @RequestBody AddConversationMemberRequest request) {
        return new ApiResponse<>(
                true,
                "Member added",
                memberService.addMember(conversationId, UUID.fromString(userId), request));
    }

    @PutMapping("/{memberUserId}")
    public ApiResponse<ConversationMember> update(
            @PathVariable UUID conversationId,
            @PathVariable UUID memberUserId,
            @RequestParam String userId,
            @RequestBody UpdateConversationMemberRequest request) {
        return new ApiResponse<>(
                true,
                "Member updated",
                memberService.updateMember(
                        conversationId, memberUserId, UUID.fromString(userId), request));
    }

    @DeleteMapping("/{memberUserId}")
    public ApiResponse<Void> remove(
            @PathVariable UUID conversationId,
            @PathVariable UUID memberUserId,
            @RequestParam String userId) {
        memberService.removeMember(conversationId, memberUserId, UUID.fromString(userId));
        return new ApiResponse<>(true, "Member removed", null);
    }
}
