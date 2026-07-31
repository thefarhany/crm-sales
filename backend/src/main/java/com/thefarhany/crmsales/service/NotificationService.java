package com.thefarhany.crmsales.service;

import com.thefarhany.crmsales.exception.ResourceNotFoundException;
import com.thefarhany.crmsales.model.entity.Notification;
import com.thefarhany.crmsales.model.entity.User;
import com.thefarhany.crmsales.model.enums.NotificationType;
import com.thefarhany.crmsales.repository.NotificationRepository;
import com.thefarhany.crmsales.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createNotification(User user, NotificationType type, String title, String message, String linkUrl) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLinkUrl(linkUrl);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    @Transactional
    public void createNotificationForAllManagers(NotificationType type, String title, String message, String linkUrl) {
        createNotificationForRole(com.thefarhany.crmsales.model.enums.Role.MARKETING_MANAGER, type, title, message, linkUrl);
    }

    @Transactional
    public void createNotificationForRole(com.thefarhany.crmsales.model.enums.Role role, NotificationType type, String title, String message, String linkUrl) {
        List<User> users = userRepository.findByRoleAndStatus(role, com.thefarhany.crmsales.model.enums.UserStatus.ACTIVE);
        users.forEach(user -> createNotification(user, type, title, message, linkUrl));
    }

    public List<Notification> getMyNotifications(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public long getUnreadCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        List<Notification> unread = notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
        unread.forEach(n -> {
            n.setIsRead(true);
            n.setReadAt(LocalDateTime.now());
        });
        notificationRepository.saveAll(unread);
    }
}
