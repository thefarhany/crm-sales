package com.thefarhany.crmsales.model.dto.request;

import com.thefarhany.crmsales.model.enums.EmailTone;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualReminderRequest {

    @NotNull(message = "Contract ID is required")
    private Long contractId;

    private String recipientEmail;

    private String customSubject;

    private String customMessage;

    private EmailTone tone;
}
