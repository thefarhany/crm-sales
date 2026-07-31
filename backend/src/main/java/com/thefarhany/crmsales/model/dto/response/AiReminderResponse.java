package com.thefarhany.crmsales.model.dto.response;

import com.thefarhany.crmsales.model.enums.ReminderStatus;
import com.thefarhany.crmsales.model.enums.ReminderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiReminderResponse {

    private Long id;

    private Long contractId;
    private String contractNumber;
    private String clientName;
    private String contractEndDate;

    private ReminderType reminderType;
    private String reminderTypeDescription;
    private int daysBeforeExpiry;

    private String recipientEmail;
    private Long recipientUserId;
    private String recipientUserName;

    private String subject;
    private String message;

    private ReminderStatus status;
    private String statusDescription;
    private LocalDateTime sentAt;
    private String errorMessage;

    private LocalDateTime createdAt;
}
