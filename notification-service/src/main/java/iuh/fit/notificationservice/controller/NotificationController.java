package iuh.fit.notificationservice.controller;

import iuh.fit.common.dto.response.ApiResponse;
import iuh.fit.notificationservice.dto.request.CreateNotificationRequest;
import iuh.fit.notificationservice.dto.request.UpdateNotificationRequest;
import iuh.fit.notificationservice.entity.Notification;
import iuh.fit.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    public ApiResponse<List<Notification>> getByUser(@PathVariable String userId) {
        return new ApiResponse<>(
                true,
                "Lấy thông báo thành công",
                notificationService.getByUserId(userId)
        );
    }

    @GetMapping("/user/{userId}/unread-count")
    public ApiResponse<Long> unreadCount(@PathVariable String userId) {
        return new ApiResponse<>(
                true,
                "Đếm thông báo chưa đọc thành công",
                notificationService.countUnread(userId)
        );
    }

    @PutMapping("/{notificationId}/read")
    public ApiResponse<Void> markRead(@PathVariable String notificationId) {
        notificationService.markAsRead(notificationId);
        return new ApiResponse<>(true, "Đánh dấu đã đọc thành công", null);
    }

    @PutMapping("/user/{userId}/read-all")
    public ApiResponse<Integer> markAllRead(@PathVariable String userId) {
        int updated = notificationService.markAllSystemAsRead(userId);
        return new ApiResponse<>(true, "Đánh dấu tất cả đã đọc thành công", updated);
    }

    @GetMapping
    public ApiResponse<List<Notification>> listAll() {
        return new ApiResponse<>(true, "OK", notificationService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Notification> getById(@PathVariable UUID id) {
        return new ApiResponse<>(true, "OK", notificationService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Notification> create(@RequestBody CreateNotificationRequest request) {
        return new ApiResponse<>(true, "Notification created", notificationService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Notification> update(
            @PathVariable UUID id,
            @RequestBody UpdateNotificationRequest request) {
        return new ApiResponse<>(true, "Notification updated", notificationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        notificationService.delete(id);
        return new ApiResponse<>(true, "Notification deleted", null);
    }
}
