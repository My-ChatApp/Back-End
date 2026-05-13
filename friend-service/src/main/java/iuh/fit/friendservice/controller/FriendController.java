package iuh.fit.friendservice.controller;

import iuh.fit.common.dto.response.ApiResponse;
import iuh.fit.friendservice.dto.request.SendFriendRequestRequest;
import iuh.fit.friendservice.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @PostMapping("/request")
    public ApiResponse<?> sendRequest(@RequestBody SendFriendRequestRequest req) {
        return new ApiResponse<>(
                true,
                "Friend request sent successfully",
                friendService.sendRequest(req.getSenderId(), req.getReceiverId())
        );
    }

    @PutMapping("/request/{requestId}/accept")
    public ApiResponse<?> acceptRequest(
            @PathVariable String requestId,
            @RequestParam String receiverId) {
        return new ApiResponse<>(
                true,
                "Friend request accepted successfully",
                friendService.acceptRequest(requestId, receiverId)
        );
    }
}