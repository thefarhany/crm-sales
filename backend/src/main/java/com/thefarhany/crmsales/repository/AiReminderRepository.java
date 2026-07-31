package com.thefarhany.crmsales.repository;

import com.thefarhany.crmsales.model.entity.AiReminder;
import com.thefarhany.crmsales.model.enums.ReminderStatus;
import com.thefarhany.crmsales.model.enums.ReminderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AiReminderRepository extends JpaRepository<AiReminder, Long> {

    Page<AiReminder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<AiReminder> findByStatusOrderByCreatedAtDesc(ReminderStatus status);

    Page<AiReminder> findByStatusOrderByCreatedAtDesc(ReminderStatus status, Pageable pageable);

    List<AiReminder> findByContractIdOrderByCreatedAtDesc(Long contractId);

    boolean existsByContractIdAndReminderTypeAndStatus(
            Long contractId,
            ReminderType reminderType,
            ReminderStatus status
    );

    @Query("SELECT r FROM AiReminder r " +
            "WHERE r.status = 'SENT' " +
            "ORDER BY r.sentAt DESC")
    Page<AiReminder> findSentReminders(Pageable pageable);

    @Query("SELECT r FROM AiReminder r " +
            "WHERE r.status = 'PENDING' " +
            "ORDER BY r.createdAt ASC")
    List<AiReminder> findPendingReminders();

    @Query("SELECT r FROM AiReminder r " +
            "WHERE r.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY r.createdAt DESC")
    Page<AiReminder> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    long countByStatus(ReminderStatus status);

    @Query("SELECT r.reminderType, COUNT(r) FROM AiReminder r " +
            "WHERE r.status = :status " +
            "GROUP BY r.reminderType")
    List<Object[]> countByReminderTypeAndStatus(@Param("status") ReminderStatus status);

    Page<AiReminder> findByRecipientUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
