package com.thefarhany.crmsales.controller;

import com.thefarhany.crmsales.model.dto.ApiResponse;
import com.thefarhany.crmsales.model.entity.Notification;
import com.thefarhany.crmsales.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getMyNotifications() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Notification> notifications = notificationService.getMyNotifications(username);
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched successfully", notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        long count = notificationService.getUnreadCount(username);
        return ResponseEntity.ok(ApiResponse.success("Unread count fetched successfully", count));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        notificationService.markAllAsRead(username);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read"));
    }
}
