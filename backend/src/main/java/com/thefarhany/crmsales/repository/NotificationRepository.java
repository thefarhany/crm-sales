package com.thefarhany.crmsales.repository;

import com.thefarhany.crmsales.model.entity.Notification;
import com.thefarhany.crmsales.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    long countByUserAndIsReadFalse(User user);
    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);
}
