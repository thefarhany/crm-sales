package com.thefarhany.crmsales.model.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReminderTestRequest {

    private Long contractId;
    private Integer daysBeforeExpiry;
}
