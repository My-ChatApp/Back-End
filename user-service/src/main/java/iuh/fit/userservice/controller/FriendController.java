package iuh.fit.userservice.controller;

import iuh.fit.common.dto.response.ApiResponse;
import iuh.fit.userservice.dto.request.CreateFriendRequestRequest;
import iuh.fit.userservice.dto.request.SendFriendRequestRequest;
import iuh.fit.userservice.dto.request.UpdateFriendRequestRequest;
import iuh.fit.userservice.entity.friend.FriendRequest;
import iuh.fit.userservice.service.FriendRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendRequestService friendRequestService;

    @GetMapping("/requests")
    public ApiResponse<List<FriendRequest>> listAll() {
        return new ApiResponse<>(true, "OK", friendRequestService.findAll());
    }

    @GetMapping("/requests/{id}")
    public ApiResponse<FriendRequest> getById(@PathVariable UUID id) {
        return new ApiResponse<>(true, "OK", friendRequestService.findById(id));
    }

    @GetMapping("/requests/incoming/{userId}")
    public ApiResponse<List<FriendRequest>> incoming(@PathVariable UUID userId) {
        return new ApiResponse<>(true, "OK", friendRequestService.findIncoming(userId));
    }

    @GetMapping("/requests/outgoing/{userId}")
    public ApiResponse<List<FriendRequest>> outgoing(@PathVariable UUID userId) {
        return new ApiResponse<>(true, "OK", friendRequestService.findOutgoing(userId));
    }

    @GetMapping
    public ApiResponse<List<FriendRequest>> listFriends(@RequestParam UUID userId) {
        return new ApiResponse<>(true, "OK", friendRequestService.findFriends(userId));
    }

    @PostMapping("/request")
    public ApiResponse<FriendRequest> sendRequest(@RequestBody SendFriendRequestRequest req) {
        return new ApiResponse<>(
                true,
                "Friend request sent successfully",
                friendRequestService.sendRequest(req.getSenderId(), req.getReceiverId())
        );
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FriendRequest> create(@RequestBody CreateFriendRequestRequest req) {
        return new ApiResponse<>(true, "Friend request created", friendRequestService.sendRequest(req));
    }

    @PutMapping("/requests/{id}")
    public ApiResponse<FriendRequest> update(
            @PathVariable UUID id,
            @RequestBody UpdateFriendRequestRequest request) {
        return new ApiResponse<>(true, "Friend request updated", friendRequestService.update(id, request));
    }

    @PutMapping("/request/{requestId}/accept")
    public ApiResponse<FriendRequest> acceptRequest(
            @PathVariable String requestId,
            @RequestParam String receiverId) {
        return new ApiResponse<>(
                true,
                "Friend request accepted successfully",
                friendRequestService.acceptRequest(requestId, receiverId)
        );
    }

    @PutMapping("/requests/{id}/accept")
    public ApiResponse<FriendRequest> acceptById(
            @PathVariable UUID id,
            @RequestParam UUID receiverId) {
        return new ApiResponse<>(
                true,
                "Friend request accepted successfully",
                friendRequestService.acceptRequest(id, receiverId)
        );
    }

    @PutMapping("/requests/{id}/reject")
    public ApiResponse<FriendRequest> reject(
            @PathVariable UUID id,
            @RequestParam UUID receiverId) {
        return new ApiResponse<>(
                true,
                "Friend request rejected",
                friendRequestService.rejectRequest(id, receiverId)
        );
    }

    @DeleteMapping("/requests/{id}")
    public ApiResponse<Void> cancelOutgoing(
            @PathVariable UUID id,
            @RequestParam UUID senderId) {
        friendRequestService.cancelOutgoing(id, senderId);
        return new ApiResponse<>(true, "Friend request cancelled", null);
    }

    @DeleteMapping("/{friendUserId}")
    public ApiResponse<Void> unfriend(
            @PathVariable UUID friendUserId,
            @RequestParam UUID userId) {
        friendRequestService.unfriend(userId, friendUserId);
        return new ApiResponse<>(true, "Friend removed", null);
    }
}
