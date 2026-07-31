package com.thefarhany.crmsales.model.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderSettingResponse {

    private Long id;
    private String settingKey;
    private String settingValue;
    private String description;
    private String updatedByName;
    private LocalDateTime updatedAt;
}
