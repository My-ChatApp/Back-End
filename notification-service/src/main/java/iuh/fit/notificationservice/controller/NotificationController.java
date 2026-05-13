package iuh.fit.notificationservice.controller;

import iuh.fit.common.dto.response.ApiResponse;
import iuh.fit.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Lấy tất cả thông báo của user
    @GetMapping("/user/{userId}")
    public ApiResponse<?> getByUser(@PathVariable String userId) {
        return new ApiResponse<>(
                true,
                "Lấy thông báo thành công",
                notificationService.getByUserId(userId)
        );
    }

    // Đếm thông báo chưa đọc
    @GetMapping("/user/{userId}/unread-count")
    public ApiResponse<?> unreadCount(@PathVariable String userId) {
        return new ApiResponse<>(
                true,
                "Đếm thông báo chưa đọc thành công",
                notificationService.countUnread(userId)
        );
    }

    // Đánh dấu đã đọc
    @PutMapping("/{notificationId}/read")
    public ApiResponse<?> markRead(@PathVariable String notificationId) {
        notificationService.markAsRead(notificationId);
        return new ApiResponse<>(
                true,
                "Đánh dấu đã đọc thành công",
                null
        );
    }
}